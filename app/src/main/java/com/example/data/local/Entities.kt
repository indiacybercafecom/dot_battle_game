package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "match_history")
data class MatchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mode: String,
    val boardSize: String,
    val winnerName: String,
    val scoresSummary: String,
    val movesCount: Int,
    val isDraw: Boolean,
    val playedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_game")
data class SavedGameEntity(
    @PrimaryKey
    val key: String = "active_saved_game",
    val gameStateJson: String,
    val savedAt: Long = System.currentTimeMillis()
)
