package com.example.network

import com.example.game.model.BoardSize
import com.example.game.model.Edge
import com.example.game.model.GameState
import com.example.game.model.Orientation

enum class NetworkState {
    IDLE,
    CONNECTING,
    QUEUED,
    IN_LOBBY,
    SYNCING,
    DISCONNECTED,
    ERROR
}

data class RoomInfo(
    val roomId: String,
    val hostPlayerId: String,
    val hostPlayerName: String,
    val guestPlayerId: String? = null,
    val guestPlayerName: String? = null,
    val boardSize: BoardSize = BoardSize.MEDIUM,
    val isGameStarted: Boolean = false,
    val hostReady: Boolean = false,
    val guestReady: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class NetworkMovePayload(
    val orientation: String,
    val row: Int,
    val col: Int,
    val playerId: Int,
    val sequenceNumber: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toEdge(): Edge = Edge(
        orientation = Orientation.valueOf(orientation),
        row = row,
        col = col
    )

    companion object {
        fun fromEdge(edge: Edge, playerId: Int, seq: Int): NetworkMovePayload =
            NetworkMovePayload(
                orientation = edge.orientation.name,
                row = edge.row,
                col = edge.col,
                playerId = playerId,
                sequenceNumber = seq
            )
    }
}
