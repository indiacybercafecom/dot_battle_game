package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.game.model.AiDifficulty
import com.example.game.model.BoardSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dot_battle_prefs")

data class UserSettings(
    val soundEffectsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val defaultBoardSize: BoardSize = BoardSize.MEDIUM,
    val defaultAiDifficulty: AiDifficulty = AiDifficulty.MEDIUM,
    val playerName: String = "Player 1",
    val tutorialCompleted: Boolean = false
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val SOUND_EFFECTS = booleanPreferencesKey("sound_effects")
        val VIBRATION = booleanPreferencesKey("vibration")
        val DEFAULT_BOARD_SIZE = stringPreferencesKey("default_board_size")
        val DEFAULT_AI_DIFFICULTY = stringPreferencesKey("default_ai_difficulty")
        val PLAYER_NAME = stringPreferencesKey("player_name")
        val TUTORIAL_COMPLETED = booleanPreferencesKey("tutorial_completed")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val boardSizeStr = prefs[Keys.DEFAULT_BOARD_SIZE] ?: BoardSize.MEDIUM.name
        val difficultyStr = prefs[Keys.DEFAULT_AI_DIFFICULTY] ?: AiDifficulty.MEDIUM.name

        UserSettings(
            soundEffectsEnabled = prefs[Keys.SOUND_EFFECTS] ?: true,
            vibrationEnabled = prefs[Keys.VIBRATION] ?: true,
            defaultBoardSize = runCatching { BoardSize.valueOf(boardSizeStr) }.getOrDefault(BoardSize.MEDIUM),
            defaultAiDifficulty = runCatching { AiDifficulty.valueOf(difficultyStr) }.getOrDefault(AiDifficulty.MEDIUM),
            playerName = prefs[Keys.PLAYER_NAME] ?: "Player 1",
            tutorialCompleted = prefs[Keys.TUTORIAL_COMPLETED] ?: false
        )
    }

    suspend fun updateSound(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_EFFECTS] = enabled }
    }

    suspend fun updateVibration(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VIBRATION] = enabled }
    }

    suspend fun updateDefaultBoardSize(size: BoardSize) {
        context.dataStore.edit { it[Keys.DEFAULT_BOARD_SIZE] = size.name }
    }

    suspend fun updateDefaultAiDifficulty(diff: AiDifficulty) {
        context.dataStore.edit { it[Keys.DEFAULT_AI_DIFFICULTY] = diff.name }
    }

    suspend fun updatePlayerName(name: String) {
        context.dataStore.edit { it[Keys.PLAYER_NAME] = name.trim().ifEmpty { "Player 1" } }
    }

    suspend fun setTutorialCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.TUTORIAL_COMPLETED] = completed }
    }
}
