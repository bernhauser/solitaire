package se.bernhauser.solitaire.game.klondike

import se.bernhauser.solitaire.game.*

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class StuckStateTest {
  private fun allCards(s: KlondikeState): List<Card> =
    s.stock + s.waste + s.foundations.flatten() + s.tableau.flatMap { it.faceDown + it.faceUp }

  @Test
  fun `stuckState uses every card exactly once`() {
    val cards = allCards(stuckState())
    assertEquals(52, cards.size)
    assertEquals(FullDeck.toSet(), cards.toSet())
  }

  @Test
  fun `stuckState is not already won`() {
    assertFalse(stuckState().isWon())
  }

  @Test
  fun `stuckState has at least one legal move so it is not a dead game`() {
    // The board looks frozen, but 7-Hearts onto 8-Spades keeps it alive; this
    // guards against the scenario being unintentionally unwinnable on load.
    assertEquals(true, stuckState().hasAnyImmediateMove())
  }
}
