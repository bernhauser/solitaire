package se.bernhauser.solitaire.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameStateTest {
  @Test
  fun `dealNewGame deals 28 tableau cards and 24 stock cards`() {
    val state = dealNewGame(seed = 7L)
    val tableauCount = state.tableau.sumOf { it.faceDown.size + it.faceUp.size }
    assertEquals(28, tableauCount)
    assertEquals(24, state.stock.size)
    assertEquals(emptyList<Card>(), state.waste)
  }

  @Test
  fun `dealNewGame tableau columns have sizes 1 through 7`() {
    val state = dealNewGame(seed = 7L)
    val sizes = state.tableau.map { it.faceDown.size + it.faceUp.size }
    assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), sizes)
  }

  @Test
  fun `dealNewGame tableau columns have exactly one face-up card on top`() {
    val state = dealNewGame(seed = 7L)
    state.tableau.forEachIndexed { i, pile ->
      assertEquals("column $i face-up size", 1, pile.faceUp.size)
      assertEquals("column $i face-down size", i, pile.faceDown.size)
    }
  }

  @Test
  fun `dealNewGame foundations start empty`() {
    val state = dealNewGame(seed = 7L)
    assertEquals(4, state.foundations.size)
    assertTrue(state.foundations.all { it.isEmpty() })
  }

  @Test
  fun `dealNewGame uses all 52 unique cards`() {
    val state = dealNewGame(seed = 7L)
    val all = buildList {
      addAll(state.stock)
      addAll(state.waste)
      state.foundations.forEach { addAll(it) }
      state.tableau.forEach {
        addAll(it.faceDown)
        addAll(it.faceUp)
      }
    }
    assertEquals(52, all.size)
    assertEquals(FullDeck.toSet(), all.toSet())
  }

  @Test
  fun `dealNewGame is deterministic for same seed`() {
    assertEquals(dealNewGame(seed = 99L), dealNewGame(seed = 99L))
  }
}
