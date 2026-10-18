package com.example.ui.game

import android.app.Application
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.MatchHistoryEntity
import com.example.data.preferences.SettingsRepository
import com.example.data.preferences.UserSettings
import com.example.game.ai.AiPlayer
import com.example.game.engine.DotBattleEngine
import com.example.game.model.AiDifficulty
import com.example.game.model.BoardSize
import com.example.game.model.Edge
import com.example.game.model.GameMode
import com.example.game.model.GameState
import com.example.game.model.MatchStatus
import com.example.game.model.Player
import com.example.network.MultiplayerManager
import com.example.network.NetworkState
import com.example.util.AudioHapticsHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    val engine = DotBattleEngine()
    val database = AppDatabase.getInstance(application)
    val settingsRepo = SettingsRepository(application)
    val audioHaptics = AudioHapticsHelper(application)
    val multiplayerManager = MultiplayerManager(engine, viewModelScope)

    val settings: StateFlow<UserSettings> = settingsRepo.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserSettings()
    )

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _gameMode = MutableStateFlow(GameMode.VS_AI)
    val gameMode: StateFlow<GameMode> = _gameMode.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _isAiPaused = MutableStateFlow(false)
    val isAiPaused: StateFlow<Boolean> = _isAiPaused.asStateFlow()

    private val _recentComboText = MutableStateFlow<String?>(null)
    val recentComboText: StateFlow<String?> = _recentComboText.asStateFlow()

    private var aiTurnJob: Job? = null

    init {
        // Listen to incoming network moves
        viewModelScope.launch {
            multiplayerManager.incomingMoves.collect { payload ->
                val edge = payload.toEdge()
                applyMove(edge, payload.playerId, isRemote = true)
            }
        }
    }

    fun startNewGame(
        mode: GameMode,
        boardSize: BoardSize,
        players: List<Player>
    ) {
        aiTurnJob?.cancel()
        _gameMode.value = mode
        _isAiPaused.value = false
        _recentComboText.value = null

        val initialState = engine.createInitialState(boardSize, players)
        _gameState.value = initialState

        checkAndTriggerAiTurn(initialState)
    }

    fun onUserSelectEdge(edge: Edge, view: View? = null) {
        val state = _gameState.value
        if (state.status != MatchStatus.IN_PROGRESS) return

        val activePlayer = state.currentPlayer ?: return
        if (activePlayer.isAi) return // Don't allow user moves on AI turns

        // In online mode, verify local player is active
        if (_gameMode.value == GameMode.ONLINE) {
            val isLocalTurn = activePlayer.id == 1 // Local player is id 1
            if (!isLocalTurn) return
        }

        applyMove(edge, activePlayer.id, isRemote = false, view = view)
    }

    private fun applyMove(edge: Edge, requestingPlayerId: Int, isRemote: Boolean, view: View? = null) {
        val currentState = _gameState.value
        val result = engine.makeMove(currentState, edge, requestingPlayerId)

        result.onSuccess { nextState ->
            _gameState.value = nextState
            val currentSettings = settings.value

            // Haptics & Sound
            if (nextState.lastCompletedBoxes.isNotEmpty()) {
                audioHaptics.playBoxCaptured(
                    soundEnabled = currentSettings.soundEffectsEnabled,
                    vibrationEnabled = currentSettings.vibrationEnabled,
                    view = view
                )
                val combo = nextState.consecutiveTurns
                if (combo > 1) {
                    _recentComboText.value = "Combo ×$combo! Extra Turn"
                } else {
                    _recentComboText.value = "Box Captured! Bonus Turn"
                }
            } else {
                audioHaptics.playEdgeDrawn(
                    soundEnabled = currentSettings.soundEffectsEnabled,
                    vibrationEnabled = currentSettings.vibrationEnabled,
                    view = view
                )
                _recentComboText.value = null
            }

            // Sync move over network if online and local move
            if (_gameMode.value == GameMode.ONLINE && !isRemote) {
                multiplayerManager.sendMove(edge, requestingPlayerId, nextState.sequenceNumber)
            }

            if (nextState.status == MatchStatus.FINISHED) {
                audioHaptics.playGameWon(
                    soundEnabled = currentSettings.soundEffectsEnabled,
                    vibrationEnabled = currentSettings.vibrationEnabled
                )
                recordMatchResult(nextState)
            } else {
                checkAndTriggerAiTurn(nextState)
            }
        }
    }

    private fun checkAndTriggerAiTurn(state: GameState) {
        if (state.status != MatchStatus.IN_PROGRESS) return
        val activePlayer = state.currentPlayer ?: return

        if (activePlayer.isAi) {
            if (_isAiPaused.value) return

            aiTurnJob?.cancel()
            aiTurnJob = viewModelScope.launch {
                _isAiThinking.value = true
                // Human-like deliberation pause
                delay(420)

                val difficulty = activePlayer.aiDifficulty ?: AiDifficulty.MEDIUM
                val edge = AiPlayer.decideMove(_gameState.value, engine, difficulty)

                _isAiThinking.value = false
                if (edge != null && _gameState.value.status == MatchStatus.IN_PROGRESS) {
                    applyMove(edge, activePlayer.id, isRemote = false)
                }
            }
        }
    }

    fun toggleAiPause() {
        val newPaused = !_isAiPaused.value
        _isAiPaused.value = newPaused
        if (!newPaused) {
            checkAndTriggerAiTurn(_gameState.value)
        } else {
            aiTurnJob?.cancel()
            _isAiThinking.value = false
        }
    }

    fun stepAiTurn() {
        if (!_isAiPaused.value) return
        val state = _gameState.value
        val activePlayer = state.currentPlayer ?: return
        if (activePlayer.isAi) {
            viewModelScope.launch {
                val difficulty = activePlayer.aiDifficulty ?: AiDifficulty.MEDIUM
                val edge = AiPlayer.decideMove(state, engine, difficulty)
                if (edge != null) {
                    applyMove(edge, activePlayer.id, isRemote = false)
                }
            }
        }
    }

    fun restartMatch() {
        val current = _gameState.value
        if (current.players.isNotEmpty()) {
            startNewGame(_gameMode.value, current.boardSize, current.players)
        }
    }

    private fun recordMatchResult(state: GameState) {
        viewModelScope.launch {
            val winnerName = when {
                state.isDraw -> "Draw"
                state.winnerPlayerId != null -> state.players.find { it.id == state.winnerPlayerId }?.name ?: "Unknown"
                else -> "None"
            }
            val summary = state.players.joinToString(", ") { p ->
                "${p.name}: ${state.scores[p.id] ?: 0}"
            }
            val entity = MatchHistoryEntity(
                mode = _gameMode.value.label,
                boardSize = state.boardSize.label,
                winnerName = winnerName,
                scoresSummary = summary,
                movesCount = state.moveHistory.size,
                isDraw = state.isDraw
            )
            database.gameDao().insertMatchHistory(entity)
        }
    }

    override fun onCleared() {
        super.onCleared()
        aiTurnJob?.cancel()
        audioHaptics.release()
    }
}
