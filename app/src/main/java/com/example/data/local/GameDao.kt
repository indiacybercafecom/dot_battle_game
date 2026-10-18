package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatchHistory(match: MatchHistoryEntity): Long

    @Query("SELECT * FROM match_history ORDER BY playedAt DESC LIMIT 50")
    fun getAllMatchHistory(): Flow<List<MatchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGame(savedGame: SavedGameEntity)

    @Query("SELECT * FROM saved_game WHERE `key` = :key LIMIT 1")
    suspend fun getSavedGame(key: String = "active_saved_game"): SavedGameEntity?

    @Query("DELETE FROM saved_game WHERE `key` = :key")
    suspend fun clearSavedGame(key: String = "active_saved_game")
}
