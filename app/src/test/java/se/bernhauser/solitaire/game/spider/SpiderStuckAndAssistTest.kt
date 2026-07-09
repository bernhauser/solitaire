package se.bernhauser.solitaire.game.spider

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import se.bernhauser.solitaire.game.Card
import se.bernhauser.solitaire.game.Rank
import se.bernhauser.solitaire.game.Suit
import se.bernhauser.solitaire.game.TableauPile

private fun card(rank: Rank, suit: Suit = Suit.Spades) = Card(rank, suit)

private fun pile(vararg faceUp: Card, faceDown: List<Card> = emptyList()) =
  TableauPile(faceDown = faceDown, faceUp = faceUp.toList())

private fun fullState(piles: List<TableauPile>, stock: List<Card> = emptyList()) = SpiderState(
  stock = stock,
  completedRuns = emptyList(),
  tableau = piles + List(SpiderColumnCount - piles.size) { TableauPile.Empty },
)

class SpiderStuckAndAssistTest {
  @Test
  fun `fresh deal always has a move`() {
    assertTrue(dealNewSpider(SpiderDifficulty.OneSuit, seed = 5L).hasAnyMove())
  }

  @Test
  fun `empty column means a move is available`() {
    val s = fullState(listOf(pile(card(Rank.Five))))
    assertTrue(s.hasAnyMove())
    assertFalse(s.isStuck())
  }

  @Test
  fun `equal exposed ranks with empty stock and no empty columns is stuck`() {
    // All ten columns expose a five; nothing can move anywhere.
    val piles = List(SpiderColumnCount) {
      pile(card(Rank.Five, Suit.Hearts), faceDown = listOf(card(Rank.King)))
    }
    val s = fullState(piles)
    assertFalse(s.hasAnyMove())
    assertTrue(s.isStuck())
  }

  @Test
  fun `same board with stock remaining is not stuck`() {
    val piles = List(SpiderColumnCount) {
      pile(card(Rank.Five, Suit.Hearts), faceDown = listOf(card(Rank.King)))
    }
    val s = fullState(piles, stock = List(10) { card(Rank.Two, Suit.Hearts) })
    assertTrue(s.hasAnyMove())
    assertFalse(s.isStuck())
  }

  @Test
  fun `won game is not stuck`() {
    val s = SpiderState(
      stock = emptyList(),
      completedRuns = List(SpiderRunCount) { Suit.Spades },
      tableau = List(SpiderColumnCount) { TableauPile.Empty },
    )
    assertTrue(s.isWon())
    assertFalse(s.isStuck())
  }

  @Test
  fun `tap assist prefers same suit destination`() {
    val s = fullState(
      listOf(
        pile(card(Rank.Five, Suit.Spades)),
        pile(card(Rank.Six, Suit.Hearts)),
        pile(card(Rank.Six, Suit.Spades)),
      ),
    )
    assertEquals(2, s.bestDestinationFor(column = 0, fromIndex = 0))
  }

  @Test
  fun `tap assist falls back to any rank match then empty pile`() {
    val offSuitOnly = fullState(
      listOf(
        pile(card(Rank.Five, Suit.Spades), faceDown = listOf(card(Rank.King))),
        pile(card(Rank.Six, Suit.Hearts)),
      ),
    )
    assertEquals(1, offSuitOnly.bestDestinationFor(0, 0))

    val emptyOnly = fullState(
      listOf(
        pile(card(Rank.Five, Suit.Spades), faceDown = listOf(card(Rank.King))),
        pile(card(Rank.Nine, Suit.Hearts)),
      ),
    )
    assertEquals(2, emptyOnly.bestDestinationFor(0, 0))
  }

  @Test
  fun `tap assist refuses to shuffle a whole pile between empty columns`() {
    val s = fullState(listOf(pile(card(Rank.King))))
    assertNull(s.bestDestinationFor(0, 0))
    assertNull(s.bestTapMove(0))
  }

  @Test
  fun `tap move picks the longest movable run`() {
    val s = fullState(
      listOf(
        pile(card(Rank.Seven), card(Rank.Six), card(Rank.Five)),
        pile(card(Rank.Eight, Suit.Hearts)),
      ),
    )
    assertEquals(0 to 1, s.bestTapMove(0))
  }
}
