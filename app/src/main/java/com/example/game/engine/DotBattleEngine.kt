package com.example.game.engine

import com.example.game.model.BoardSize
import com.example.game.model.Box
import com.example.game.model.Edge
import com.example.game.model.GameState
import com.example.game.model.MatchStatus
import com.example.game.model.Move
import com.example.game.model.Orientation
import com.example.game.model.Player

class DotBattleEngine {

    fun createInitialState(boardSize: BoardSize, players: List<Player>): GameState {
        require(players.size in 2..4) { "Player count must be between 2 and 4" }
        val n = boardSize.boxes
        val initialBoxes = mutableListOf<Box>()
        for (r in 0 until n) {
            for (c in 0 until n) {
                initialBoxes.add(Box(row = r, col = c, claimedByPlayerId = null))
            }
        }
        val initialScores = players.associate { it.id to 0 }

        return GameState(
            boardSize = boardSize,
            players = players,
            currentPlayerIndex = 0,
            claimedEdges = emptyMap(),
            boxes = initialBoxes,
            scores = initialScores,
            status = MatchStatus.IN_PROGRESS,
            winnerPlayerId = null,
            isDraw = false,
            moveHistory = emptyList(),
            lastCompletedBoxes = emptyList(),
            consecutiveTurns = 0,
            sequenceNumber = 0
        )
    }

    fun getAllPossibleEdges(boardSize: BoardSize): List<Edge> {
        val n = boardSize.boxes
        val edges = mutableListOf<Edge>()
        // Horizontal edges: (n + 1) rows, n columns
        for (r in 0..n) {
            for (c in 0 until n) {
                edges.add(Edge(Orientation.HORIZONTAL, r, c))
            }
        }
        // Vertical edges: n rows, (n + 1) columns
        for (r in 0 until n) {
            for (c in 0..n) {
                edges.add(Edge(Orientation.VERTICAL, r, c))
            }
        }
        return edges
    }

    fun isEdgeValid(edge: Edge, boardSize: BoardSize): Boolean {
        val n = boardSize.boxes
        return when (edge.orientation) {
            Orientation.HORIZONTAL -> edge.row in 0..n && edge.col in 0 until n
            Orientation.VERTICAL -> edge.row in 0 until n && edge.col in 0..n
        }
    }

    fun getBoxesForEdge(edge: Edge, boardSize: BoardSize): List<Pair<Int, Int>> {
        val n = boardSize.boxes
        val adjacent = mutableListOf<Pair<Int, Int>>()
        when (edge.orientation) {
            Orientation.HORIZONTAL -> {
                // Box above: (row - 1, col)
                if (edge.row > 0) adjacent.add(Pair(edge.row - 1, edge.col))
                // Box below: (row, col)
                if (edge.row < n) adjacent.add(Pair(edge.row, edge.col))
            }
            Orientation.VERTICAL -> {
                // Box left: (row, col - 1)
                if (edge.col > 0) adjacent.add(Pair(edge.row, edge.col - 1))
                // Box right: (row, col)
                if (edge.col < n) adjacent.add(Pair(edge.row, edge.col))
            }
        }
        return adjacent
    }

    fun isBoxComplete(r: Int, c: Int, claimedEdges: Map<Edge, Int>): Boolean {
        val top = Edge(Orientation.HORIZONTAL, r, c)
        val bottom = Edge(Orientation.HORIZONTAL, r + 1, c)
        val left = Edge(Orientation.VERTICAL, r, c)
        val right = Edge(Orientation.VERTICAL, r, c + 1)

        return claimedEdges.containsKey(top) &&
                claimedEdges.containsKey(bottom) &&
                claimedEdges.containsKey(left) &&
                claimedEdges.containsKey(right)
    }

    fun countEdgesForBox(r: Int, c: Int, claimedEdges: Map<Edge, Int>): Int {
        val top = Edge(Orientation.HORIZONTAL, r, c)
        val bottom = Edge(Orientation.HORIZONTAL, r + 1, c)
        val left = Edge(Orientation.VERTICAL, r, c)
        val right = Edge(Orientation.VERTICAL, r, c + 1)

        var count = 0
        if (claimedEdges.containsKey(top)) count++
        if (claimedEdges.containsKey(bottom)) count++
        if (claimedEdges.containsKey(left)) count++
        if (claimedEdges.containsKey(right)) count++
        return count
    }

    fun makeMove(state: GameState, edge: Edge, requestingPlayerId: Int? = null): Result<GameState> {
        if (state.status != MatchStatus.IN_PROGRESS) {
            return Result.failure(IllegalStateException("Match is not in progress"))
        }

        if (!isEdgeValid(edge, state.boardSize)) {
            return Result.failure(IllegalArgumentException("Invalid edge coordinates: $edge"))
        }

        if (state.claimedEdges.containsKey(edge)) {
            return Result.failure(IllegalArgumentException("Edge is already claimed: $edge"))
        }

        val activePlayer = state.currentPlayer
            ?: return Result.failure(IllegalStateException("No active player"))

        if (requestingPlayerId != null && requestingPlayerId != activePlayer.id) {
            return Result.failure(IllegalStateException("It is not player $requestingPlayerId's turn"))
        }

        // Add edge
        val newClaimedEdges = state.claimedEdges.toMutableMap()
        newClaimedEdges[edge] = activePlayer.id

        // Check for completed boxes
        val adjacentBoxes = getBoxesForEdge(edge, state.boardSize)
        val newlyCompleted = mutableListOf<Box>()

        val newBoxes = state.boxes.map { box ->
            if (box.claimedByPlayerId == null && adjacentBoxes.contains(Pair(box.row, box.col))) {
                if (isBoxComplete(box.row, box.col, newClaimedEdges)) {
                    val completed = box.copy(claimedByPlayerId = activePlayer.id)
                    newlyCompleted.add(completed)
                    completed
                } else {
                    box
                }
            } else {
                box
            }
        }

        // Update scores
        val newScores = state.scores.toMutableMap()
        if (newlyCompleted.isNotEmpty()) {
            val currentScore = newScores[activePlayer.id] ?: 0
            newScores[activePlayer.id] = currentScore + newlyCompleted.size
        }

        val extraTurn = newlyCompleted.isNotEmpty()
        val nextPlayerIndex = if (extraTurn) {
            state.currentPlayerIndex
        } else {
            (state.currentPlayerIndex + 1) % state.players.size
        }

        val totalBoxes = state.boardSize.boxes * state.boardSize.boxes
        val totalClaimedBoxes = newBoxes.count { it.claimedByPlayerId != null }
        val isGameOver = totalClaimedBoxes >= totalBoxes

        var winnerId: Int? = null
        var isDraw = false

        if (isGameOver) {
            val maxScore = newScores.values.maxOrNull() ?: 0
            val topPlayers = newScores.filter { it.value == maxScore }.keys.toList()
            if (topPlayers.size == 1) {
                winnerId = topPlayers.first()
            } else {
                isDraw = true
            }
        }

        val moveNumber = state.sequenceNumber + 1
        val move = Move(
            edge = edge,
            playerId = activePlayer.id,
            sequenceNumber = moveNumber,
            completedBoxes = newlyCompleted
        )

        val nextState = state.copy(
            claimedEdges = newClaimedEdges,
            boxes = newBoxes,
            scores = newScores,
            currentPlayerIndex = nextPlayerIndex,
            status = if (isGameOver) MatchStatus.FINISHED else MatchStatus.IN_PROGRESS,
            winnerPlayerId = winnerId,
            isDraw = isDraw,
            moveHistory = state.moveHistory + move,
            lastCompletedBoxes = newlyCompleted,
            consecutiveTurns = if (extraTurn) state.consecutiveTurns + 1 else 0,
            sequenceNumber = moveNumber
        )

        return Result.success(nextState)
    }
}
