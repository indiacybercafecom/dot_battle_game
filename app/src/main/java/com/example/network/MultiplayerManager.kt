package com.example.network

import com.example.game.engine.DotBattleEngine
import com.example.game.model.BoardSize
import com.example.game.model.Edge
import com.example.game.model.GameState
import com.example.game.model.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

class MultiplayerManager(
    private val engine: DotBattleEngine,
    private val scope: CoroutineScope
) {

    private val _networkState = MutableStateFlow(NetworkState.IDLE)
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

    private val _currentRoom = MutableStateFlow<RoomInfo?>(null)
    val currentRoom: StateFlow<RoomInfo?> = _currentRoom.asStateFlow()

    private val _incomingMoves = MutableSharedFlow<NetworkMovePayload>()
    val incomingMoves: SharedFlow<NetworkMovePayload> = _incomingMoves.asSharedFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var matchmakingJob: Job? = null
    private var simulatedOpponentJob: Job? = null

    val localUserId: String = "usr_" + UUID.randomUUID().toString().substring(0, 8)

    // In-memory private room directory for offline/local-network peer play
    private val localRoomDirectory = mutableMapOf<String, RoomInfo>()

    fun createRoom(hostName: String, boardSize: BoardSize): String {
        cancelMatchmaking()
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val roomCode = (1..6).map { chars.random() }.joinToString("")

        val room = RoomInfo(
            roomId = roomCode,
            hostPlayerId = localUserId,
            hostPlayerName = hostName,
            boardSize = boardSize,
            hostReady = true
        )
        localRoomDirectory[roomCode] = room
        _currentRoom.value = room
        _networkState.value = NetworkState.IN_LOBBY

        return roomCode
    }

    fun joinRoom(roomCode: String, guestName: String, onJoined: (RoomInfo) -> Unit, onError: (String) -> Unit) {
        cancelMatchmaking()
        val cleanCode = roomCode.trim().uppercase()
        val existingRoom = localRoomDirectory[cleanCode]

        if (existingRoom == null) {
            // For testing online play seamlessly, if code is 6 characters we can create a joinable mock room
            if (cleanCode.length == 6) {
                val newRoom = RoomInfo(
                    roomId = cleanCode,
                    hostPlayerId = "remote_host",
                    hostPlayerName = "Host Player",
                    guestPlayerId = localUserId,
                    guestPlayerName = guestName,
                    guestReady = true,
                    hostReady = true,
                    isGameStarted = true
                )
                localRoomDirectory[cleanCode] = newRoom
                _currentRoom.value = newRoom
                _networkState.value = NetworkState.IN_LOBBY
                onJoined(newRoom)
                return
            }
            val error = "Room '$cleanCode' not found. Verify code or create a new room."
            _errorMessage.value = error
            onError(error)
            return
        }

        if (existingRoom.guestPlayerId != null && existingRoom.guestPlayerId != localUserId) {
            val error = "Room '$cleanCode' is already full."
            _errorMessage.value = error
            onError(error)
            return
        }

        val updated = existingRoom.copy(
            guestPlayerId = localUserId,
            guestPlayerName = guestName,
            guestReady = true,
            isGameStarted = true
        )
        localRoomDirectory[cleanCode] = updated
        _currentRoom.value = updated
        _networkState.value = NetworkState.IN_LOBBY
        onJoined(updated)
    }

    fun startQuickMatch(playerName: String, boardSize: BoardSize, onMatched: (RoomInfo) -> Unit) {
        cancelMatchmaking()
        _networkState.value = NetworkState.QUEUED

        matchmakingJob = scope.launch(Dispatchers.Default) {
            // Simulate queue search delay
            delay(1500)
            if (_networkState.value != NetworkState.QUEUED) return@launch

            val roomId = "QM" + Random.nextInt(1000, 9999)
            val matchedRoom = RoomInfo(
                roomId = roomId,
                hostPlayerId = localUserId,
                hostPlayerName = playerName,
                guestPlayerId = "opponent_" + UUID.randomUUID().toString().substring(0, 4),
                guestPlayerName = listOf("NovaTactician", "GridMaster", "DotNinja", "PixelPulse").random(),
                boardSize = boardSize,
                hostReady = true,
                guestReady = true,
                isGameStarted = true
            )
            _currentRoom.value = matchedRoom
            _networkState.value = NetworkState.IN_LOBBY
            onMatched(matchedRoom)
        }
    }

    fun cancelMatchmaking() {
        matchmakingJob?.cancel()
        matchmakingJob = null
        if (_networkState.value == NetworkState.QUEUED) {
            _networkState.value = NetworkState.IDLE
        }
    }

    fun sendMove(edge: Edge, playerId: Int, sequenceNumber: Int) {
        // Broadcast to current room
        scope.launch(Dispatchers.Default) {
            val payload = NetworkMovePayload.fromEdge(edge, playerId, sequenceNumber)
            // If the opponent is a remote peer or simulated online player, trigger their response
            val room = _currentRoom.value ?: return@launch
            if (room.guestPlayerId?.startsWith("opponent_") == true || room.hostPlayerId == "remote_host") {
                // If it becomes the remote player's turn, simulate network move
                triggerRemoteOpponentTurn(playerId)
            }
        }
    }

    private fun triggerRemoteOpponentTurn(lastPlayerId: Int) {
        simulatedOpponentJob?.cancel()
        simulatedOpponentJob = scope.launch(Dispatchers.Default) {
            // Simulate network latency (400 - 800ms)
            delay(Random.nextLong(400, 800))
        }
    }

    fun leaveRoom() {
        cancelMatchmaking()
        simulatedOpponentJob?.cancel()
        val room = _currentRoom.value
        if (room != null) {
            localRoomDirectory.remove(room.roomId)
        }
        _currentRoom.value = null
        _networkState.value = NetworkState.IDLE
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
