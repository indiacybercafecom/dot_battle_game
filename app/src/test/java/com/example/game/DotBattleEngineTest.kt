package com.example.game

import com.example.game.ai.AiPlayer
import com.example.game.engine.DotBattleEngine
import com.example.game.model.AiDifficulty
import com.example.game.model.BoardSize
import com.example.game.model.Edge
import com.example.game.model.MatchStatus
import com.example.game.model.Orientation
import com.example.game.model.Player
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DotBattleEngineTest {

    private lateinit var engine: DotBattleEngine
    private val player1 = Player(id = 1, name = "P1", colorIndex = 0, isAi = false)
    private val player2 = Player(id = 2, name = "P2", colorIndex = 1, isAi = true, aiDifficulty = AiDifficulty.MEDIUM)

    @Before
    fun setup() {
        engine = DotBattleEngine()
    }

    @Test
    fun testBoardInitialization() {
        BoardSize.values().forEach { size ->
            val state = engine.createInitialState(size, listOf(player1, player2))
            assertEquals(size.boxes * size.boxes, state.totalBoxes)
            assertEquals(0, state.claimedEdges.size)
            assertEquals(0, state.scores[1])
            assertEquals(0, state.scores[2])
            assertEquals(MatchStatus.IN_PROGRESS, state.status)
        }
    }

    @Test
    fun testLegalMoveAndTurnRotation() {
        val state = engine.createInitialState(BoardSize.SMALL, listOf(player1, player2))
        val edge = Edge(Orientation.HORIZONTAL, 0, 0)

        val result = engine.makeMove(state, edge)
        assertTrue(result.isSuccess)

        val nextState = result.getOrThrow()
        assertEquals(1, nextState.claimedEdges.size)
        assertEquals(1, nextState.claimedEdges[edge])
        // No box completed, so next turn belongs to player 2
        assertEquals(1, nextState.currentPlayerIndex)
        assertEquals(player2.id, nextState.currentPlayer?.id)
    }

    @Test
    fun testDuplicateEdgeRejection() {
        val state = engine.createInitialState(BoardSize.SMALL, listOf(player1, player2))
        val edge = Edge(Orientation.HORIZONTAL, 0, 0)
        val s1 = engine.makeMove(state, edge).getOrThrow()

        // Attempt same edge again
        val duplicateResult = engine.makeMove(s1, edge)
        assertFalse(duplicateResult.isSuccess)
    }

    @Test
    fun testBoxCompletionAndExtraTurn() {
        val state = engine.createInitialState(BoardSize.SMALL, listOf(player1, player2))

        // Create 3 walls of box (0, 0)
        val eTop = Edge(Orientation.HORIZONTAL, 0, 0)
        val eBottom = Edge(Orientation.HORIZONTAL, 1, 0)
        val eLeft = Edge(Orientation.VERTICAL, 0, 0)
        val eRight = Edge(Orientation.VERTICAL, 0, 1)

        val s1 = engine.makeMove(state, eTop).getOrThrow() // P1
        val s2 = engine.makeMove(s1, eBottom).getOrThrow() // P2
        val s3 = engine.makeMove(s2, eLeft).getOrThrow() // P1

        // P2 plays eRight, completing the box!
        val s4 = engine.makeMove(s3, eRight).getOrThrow()

        assertEquals(1, s4.scores[player2.id])
        assertEquals(1, s4.lastCompletedBoxes.size)
        assertEquals(0, s4.lastCompletedBoxes[0].row)
        assertEquals(0, s4.lastCompletedBoxes[0].col)
        // Player 2 gets an EXTRA turn, so currentPlayerIndex is still 1 (Player 2)
        assertEquals(1, s4.currentPlayerIndex)
        assertEquals(player2.id, s4.currentPlayer?.id)
    }

    @Test
    fun testDoubleBoxCompletion() {
        val state = engine.createInitialState(BoardSize.SMALL, listOf(player1, player2))

        // Set up box (0, 0) and box (0, 1) sharing vertical edge (0, 1)
        // Box (0, 0): top (0,0), bottom (1,0), left (0,0)
        // Box (0, 1): top (0,1), bottom (1,1), right (0,2)
        // The shared divider edge is vertical (0, 1)
        var s = engine.makeMove(state, Edge(Orientation.HORIZONTAL, 0, 0)).getOrThrow()
        s = engine.makeMove(s, Edge(Orientation.HORIZONTAL, 1, 0)).getOrThrow()
        s = engine.makeMove(s, Edge(Orientation.VERTICAL, 0, 0)).getOrThrow()

        s = engine.makeMove(s, Edge(Orientation.HORIZONTAL, 0, 1)).getOrThrow()
        s = engine.makeMove(s, Edge(Orientation.HORIZONTAL, 1, 1)).getOrThrow()
        s = engine.makeMove(s, Edge(Orientation.VERTICAL, 0, 2)).getOrThrow()

        // Now both boxes have 3 edges. Play the shared divider edge:
        val dividerEdge = Edge(Orientation.VERTICAL, 0, 1)
        val activePlayer = s.currentPlayer!!
        val beforeScore = s.scores[activePlayer.id] ?: 0

        val finishState = engine.makeMove(s, dividerEdge).getOrThrow()

        assertEquals(2, finishState.lastCompletedBoxes.size)
        assertEquals(beforeScore + 2, finishState.scores[activePlayer.id])
    }

    @Test
    fun testAiSelectsLegalMove() = runBlocking {
        val state = engine.createInitialState(BoardSize.SMALL, listOf(player1, player2))
        val easyMove = AiPlayer.decideMove(state, engine, AiDifficulty.EASY)
        val mediumMove = AiPlayer.decideMove(state, engine, AiDifficulty.MEDIUM)
        val hardMove = AiPlayer.decideMove(state, engine, AiDifficulty.HARD)

        assertNotNull(easyMove)
        assertNotNull(mediumMove)
        assertNotNull(hardMove)

        assertTrue(engine.isEdgeValid(easyMove!!, BoardSize.SMALL))
        assertTrue(engine.isEdgeValid(mediumMove!!, BoardSize.SMALL))
        assertTrue(engine.isEdgeValid(hardMove!!, BoardSize.SMALL))
    }

    @Test
    fun testMediumAiCapturesAvailableBox() = runBlocking {
        val state = engine.createInitialState(BoardSize.SMALL, listOf(player1, player2))
        // Prepare box (0,0) with 3 edges
        var s = engine.makeMove(state, Edge(Orientation.HORIZONTAL, 0, 0)).getOrThrow()
        s = engine.makeMove(s, Edge(Orientation.HORIZONTAL, 1, 0)).getOrThrow()
        s = engine.makeMove(s, Edge(Orientation.VERTICAL, 0, 0)).getOrThrow()

        // It is now Player 2's turn (AI).
        val expectedClosingEdge = Edge(Orientation.VERTICAL, 0, 1)
        val aiMove = AiPlayer.decideMove(s, engine, AiDifficulty.MEDIUM)

        assertEquals(expectedClosingEdge, aiMove)
    }
}
