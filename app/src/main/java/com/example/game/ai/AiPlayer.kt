package com.example.game.ai

import com.example.game.engine.DotBattleEngine
import com.example.game.model.AiDifficulty
import com.example.game.model.Edge
import com.example.game.model.GameState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

object AiPlayer {

    suspend fun decideMove(
        state: GameState,
        engine: DotBattleEngine,
        difficulty: AiDifficulty
    ): Edge? = withContext(Dispatchers.Default) {
        val allEdges = engine.getAllPossibleEdges(state.boardSize)
        val availableEdges = allEdges.filter { !state.claimedEdges.containsKey(it) }

        if (availableEdges.isEmpty()) return@withContext null

        when (difficulty) {
            AiDifficulty.EASY -> chooseEasyMove(state, engine, availableEdges)
            AiDifficulty.MEDIUM -> chooseMediumMove(state, engine, availableEdges)
            AiDifficulty.HARD -> chooseHardMove(state, engine, availableEdges)
        }
    }

    private fun chooseEasyMove(
        state: GameState,
        engine: DotBattleEngine,
        availableEdges: List<Edge>
    ): Edge {
        val completingEdges = findCompletingEdges(state, engine, availableEdges)
        // Easy AI takes a completing edge 65% of the time, otherwise picks randomly
        if (completingEdges.isNotEmpty() && Random.nextFloat() < 0.65f) {
            return completingEdges.random()
        }
        return availableEdges.random()
    }

    private fun chooseMediumMove(
        state: GameState,
        engine: DotBattleEngine,
        availableEdges: List<Edge>
    ): Edge {
        // 1. Always take completing moves first
        val completingEdges = findCompletingEdges(state, engine, availableEdges)
        if (completingEdges.isNotEmpty()) {
            // Prioritize move that completes 2 boxes over 1 box
            val doubleCompleter = completingEdges.firstOrNull { edge ->
                countBoxesCompletedBy(state, engine, edge) == 2
            }
            return doubleCompleter ?: completingEdges.random()
        }

        // 2. Play safe edges that do not give away any box (do not create 3rd side)
        val safeEdges = availableEdges.filter { edge ->
            !createsThirdEdge(state, engine, edge)
        }
        if (safeEdges.isNotEmpty()) {
            return safeEdges.random()
        }

        // 3. Forced sacrifice: pick edge giving away the fewest boxes
        return findMinDamageSacrifice(state, engine, availableEdges)
    }

    private fun chooseHardMove(
        state: GameState,
        engine: DotBattleEngine,
        availableEdges: List<Edge>
    ): Edge {
        // 1. If remaining edges are few (<= 10), run exact alpha-beta minimax search
        if (availableEdges.size <= 10 && state.players.size == 2) {
            val bestMove = runAlphaBeta(state, engine, depth = 6, timeLimitMs = 200)
            if (bestMove != null) return bestMove
        }

        // 2. Immediate capture evaluation
        val completingEdges = findCompletingEdges(state, engine, availableEdges)
        if (completingEdges.isNotEmpty()) {
            // Check double-box completion first
            val doubleCompleter = completingEdges.firstOrNull { edge ->
                countBoxesCompletedBy(state, engine, edge) == 2
            }
            if (doubleCompleter != null) return doubleCompleter

            return completingEdges.maxByOrNull { edge ->
                // Evaluate potential chain continuation
                val simulated = engine.makeMove(state, edge).getOrNull()
                if (simulated != null) {
                    val nextAvail = engine.getAllPossibleEdges(state.boardSize)
                        .filter { !simulated.claimedEdges.containsKey(it) }
                    findCompletingEdges(simulated, engine, nextAvail).size
                } else 0
            } ?: completingEdges.first()
        }

        // 3. Safe moves: Choose the safe move that keeps maximum future flexibility
        val safeEdges = availableEdges.filter { edge ->
            !createsThirdEdge(state, engine, edge)
        }
        if (safeEdges.isNotEmpty()) {
            // Heuristic: prefer perimeter or isolated edges that don't crowd boxes
            return safeEdges.maxByOrNull { edge ->
                evaluateSafeEdge(state, engine, edge)
            } ?: safeEdges.random()
        }

        // 4. Forced sacrifice: Minimize opponent chain capture
        return findMinDamageSacrifice(state, engine, availableEdges)
    }

    private fun findCompletingEdges(
        state: GameState,
        engine: DotBattleEngine,
        availableEdges: List<Edge>
    ): List<Edge> {
        return availableEdges.filter { edge ->
            val adj = engine.getBoxesForEdge(edge, state.boardSize)
            adj.any { (r, c) ->
                engine.countEdgesForBox(r, c, state.claimedEdges) == 3
            }
        }
    }

    private fun countBoxesCompletedBy(
        state: GameState,
        engine: DotBattleEngine,
        edge: Edge
    ): Int {
        val adj = engine.getBoxesForEdge(edge, state.boardSize)
        return adj.count { (r, c) ->
            engine.countEdgesForBox(r, c, state.claimedEdges) == 3
        }
    }

    private fun createsThirdEdge(
        state: GameState,
        engine: DotBattleEngine,
        edge: Edge
    ): Boolean {
        val adj = engine.getBoxesForEdge(edge, state.boardSize)
        return adj.any { (r, c) ->
            val count = engine.countEdgesForBox(r, c, state.claimedEdges)
            count == 2 // Adding this edge will make it 3
        }
    }

    private fun evaluateSafeEdge(
        state: GameState,
        engine: DotBattleEngine,
        edge: Edge
    ): Int {
        var score = 0
        val adj = engine.getBoxesForEdge(edge, state.boardSize)
        for ((r, c) in adj) {
            val count = engine.countEdgesForBox(r, c, state.claimedEdges)
            // Having 0 edges is safer than having 1 edge
            if (count == 0) score += 3
            else if (count == 1) score += 1
        }
        return score
    }

    private fun findMinDamageSacrifice(
        state: GameState,
        engine: DotBattleEngine,
        availableEdges: List<Edge>
    ): Edge {
        // Find the edge that exposes the minimum chain length to the opponent
        return availableEdges.minByOrNull { edge ->
            val adj = engine.getBoxesForEdge(edge, state.boardSize)
            var exposedBoxes = 0
            for ((r, c) in adj) {
                if (engine.countEdgesForBox(r, c, state.claimedEdges) == 2) {
                    exposedBoxes++
                }
            }
            exposedBoxes
        } ?: availableEdges.random()
    }

    private fun runAlphaBeta(
        state: GameState,
        engine: DotBattleEngine,
        depth: Int,
        timeLimitMs: Long
    ): Edge? {
        val startTime = System.currentTimeMillis()
        val availableEdges = engine.getAllPossibleEdges(state.boardSize)
            .filter { !state.claimedEdges.containsKey(it) }

        val activePlayerId = state.currentPlayer?.id ?: return null
        var bestScore = Int.MIN_VALUE
        var bestEdge: Edge? = null

        for (edge in availableEdges) {
            if (System.currentTimeMillis() - startTime > timeLimitMs) break
            val nextState = engine.makeMove(state, edge).getOrNull() ?: continue
            val score = minimax(
                nextState,
                engine,
                depth - 1,
                Int.MIN_VALUE,
                Int.MAX_VALUE,
                activePlayerId,
                startTime,
                timeLimitMs
            )
            if (score > bestScore) {
                bestScore = score
                bestEdge = edge
            }
        }
        return bestEdge
    }

    private fun minimax(
        state: GameState,
        engine: DotBattleEngine,
        depth: Int,
        alpha: Int,
        beta: Int,
        maximizingPlayerId: Int,
        startTime: Long,
        timeLimitMs: Long
    ): Int {
        if (depth <= 0 || state.status == com.example.game.model.MatchStatus.FINISHED ||
            System.currentTimeMillis() - startTime > timeLimitMs
        ) {
            val myScore = state.scores[maximizingPlayerId] ?: 0
            val otherScore = state.scores.filterKeys { it != maximizingPlayerId }.values.sum()
            return myScore - otherScore
        }

        val availableEdges = engine.getAllPossibleEdges(state.boardSize)
            .filter { !state.claimedEdges.containsKey(it) }

        if (availableEdges.isEmpty()) {
            val myScore = state.scores[maximizingPlayerId] ?: 0
            val otherScore = state.scores.filterKeys { it != maximizingPlayerId }.values.sum()
            return myScore - otherScore
        }

        val isMaxTurn = state.currentPlayer?.id == maximizingPlayerId
        var currentAlpha = alpha
        var currentBeta = beta

        if (isMaxTurn) {
            var maxEval = Int.MIN_VALUE
            for (edge in availableEdges) {
                val nextState = engine.makeMove(state, edge).getOrNull() ?: continue
                val eval = minimax(
                    nextState,
                    engine,
                    depth - 1,
                    currentAlpha,
                    currentBeta,
                    maximizingPlayerId,
                    startTime,
                    timeLimitMs
                )
                maxEval = maxOf(maxEval, eval)
                currentAlpha = maxOf(currentAlpha, eval)
                if (currentBeta <= currentAlpha) break
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            for (edge in availableEdges) {
                val nextState = engine.makeMove(state, edge).getOrNull() ?: continue
                val eval = minimax(
                    nextState,
                    engine,
                    depth - 1,
                    currentAlpha,
                    currentBeta,
                    maximizingPlayerId,
                    startTime,
                    timeLimitMs
                )
                minEval = minOf(minEval, eval)
                currentBeta = minOf(currentBeta, eval)
                if (currentBeta <= currentAlpha) break
            }
            return minEval
        }
    }
}
