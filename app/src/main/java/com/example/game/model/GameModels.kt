package com.example.game.model

enum class Orientation {
    HORIZONTAL,
    VERTICAL
}

enum class AiDifficulty {
    EASY,
    MEDIUM,
    HARD
}

enum class BoardSize(val boxes: Int, val label: String) {
    SMALL(4, "4 × 4"),
    MEDIUM(6, "6 × 6"),
    LARGE(8, "8 × 8")
}

enum class GameMode(val label: String) {
    VS_AI("Player vs AI"),
    MULTI_AI("Multi-AI Battle"),
    ONLINE("Play Online"),
    PASS_AND_PLAY("Pass & Play")
}

enum class MatchStatus {
    NOT_STARTED,
    IN_PROGRESS,
    PAUSED,
    FINISHED
}

data class Edge(
    val orientation: Orientation,
    val row: Int,
    val col: Int
)

data class Box(
    val row: Int,
    val col: Int,
    val claimedByPlayerId: Int? = null
)

data class Player(
    val id: Int,
    val name: String,
    val colorIndex: Int,
    val isAi: Boolean = false,
    val aiDifficulty: AiDifficulty? = null
)

data class Move(
    val edge: Edge,
    val playerId: Int,
    val sequenceNumber: Int,
    val completedBoxes: List<Box> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class GameState(
    val boardSize: BoardSize = BoardSize.MEDIUM,
    val players: List<Player> = emptyList(),
    val currentPlayerIndex: Int = 0,
    val claimedEdges: Map<Edge, Int> = emptyMap(), // Edge -> PlayerId
    val boxes: List<Box> = emptyList(),
    val scores: Map<Int, Int> = emptyMap(), // PlayerId -> Score
    val status: MatchStatus = MatchStatus.NOT_STARTED,
    val winnerPlayerId: Int? = null,
    val isDraw: Boolean = false,
    val moveHistory: List<Move> = emptyList(),
    val lastCompletedBoxes: List<Box> = emptyList(),
    val consecutiveTurns: Int = 0,
    val sequenceNumber: Int = 0
) {
    val totalBoxes: Int get() = boardSize.boxes * boardSize.boxes
    val claimedBoxCount: Int get() = boxes.count { it.claimedByPlayerId != null }
    val currentPlayer: Player? get() = players.getOrNull(currentPlayerIndex)
    val remainingEdgesCount: Int
        get() {
            val total = (boardSize.boxes + 1) * boardSize.boxes * 2
            return total - claimedEdges.size
        }
}
