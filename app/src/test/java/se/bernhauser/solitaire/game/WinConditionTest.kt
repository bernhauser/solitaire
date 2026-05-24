package se.bernhauser.solitaire.game

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WinConditionTest {
  private val emptyTableau = List(7) { TableauPile.Empty }

  private fun fullFoundationFor(suit: Suit): List<Card> =
    Rank.entries.map { Card(it, suit) }

  @Test
  fun `isWon is true when all four foundations are full A through K`() {
    val foundations = Suit.entries.map { fullFoundationFor(it) }
    val state = GameState(
      stock = emptyList(),
      waste = emptyList(),
      foundations = foundations,
      tableau = emptyTableau,
    )
    assertTrue(state.isWon())
  }

  @Test
  fun `isWon is false on a freshly dealt game`() {
    assertFalse(dealNewGame(seed = 1L).isWon())
  }

  @Test
  fun `isWon is false when one foundation is missing the king`() {
    val incomplete = Rank.entries.dropLast(1).map { Card(it, Suit.Hearts) }
    val foundations = Suit.entries.map { suit ->
      if (suit == Suit.Hearts) incomplete else fullFoundationFor(suit)
    }
    val state = GameState(
      stock = emptyList(),
      waste = emptyList(),
      foundations = foundations,
      tableau = emptyTableau,
    )
    assertFalse(state.isWon())
  }
}
