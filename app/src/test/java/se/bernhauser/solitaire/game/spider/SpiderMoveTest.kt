package se.bernhauser.solitaire.game.spider

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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

private fun state(vararg piles: TableauPile, stock: List<Card> = emptyList()) = SpiderState(
  stock = stock,
  completedRuns = emptyList(),
  tableau = piles.toList() + List(SpiderColumnCount - piles.size) { TableauPile.Empty },
)

class SpiderMoveTest {
  @Test
  fun `single card moves onto any suit one rank higher`() {
    val s = state(
      pile(card(Rank.Five, Suit.Spades)),
      pile(card(Rank.Six, Suit.Hearts)),
    )
    val next = s.moveRun(fromColumn = 0, fromIndex = 0, toColumn = 1)
    assertNotNull(next)
    assertEquals(
      listOf(card(Rank.Six, Suit.Hearts), card(Rank.Five, Suit.Spades)),
      next!!.tableau[1].faceUp,
    )
    assertTrue(next.tableau[0].faceUp.isEmpty())
  }

  @Test
  fun `same suit run moves as a unit`() {
    val s = state(
      pile(card(Rank.Eight), card(Rank.Seven), card(Rank.Six)),
      pile(card(Rank.Nine, Suit.Hearts)),
    )
    val next = s.moveRun(0, 0, 1)
    assertNotNull(next)
    assertEquals(4, next!!.tableau[1].faceUp.size)
  }

  @Test
  fun `mixed suit sequence cannot move as a unit`() {
    val s = state(
      pile(card(Rank.Eight, Suit.Spades), card(Rank.Seven, Suit.Hearts)),
      pile(card(Rank.Nine, Suit.Hearts)),
    )
    assertNull(s.moveRun(0, 0, 1))
    // The hearts tail alone is movable (to an empty column).
    assertNotNull(s.moveRun(0, 1, 5))
  }

  @Test
  fun `card cannot land on wrong rank`() {
    val s = state(
      pile(card(Rank.Five)),
      pile(card(Rank.Seven)),
    )
    assertNull(s.moveRun(0, 0, 1))
  }

  @Test
  fun `any card may move to an empty pile`() {
    val s = state(
      pile(card(Rank.Two), faceDown = listOf(card(Rank.King))),
    )
    val next = s.moveRun(0, 0, 5)
    assertNotNull(next)
    assertEquals(listOf(card(Rank.Two)), next!!.tableau[5].faceUp)
  }

  @Test
  fun `moving the last face-up card flips the next face-down card`() {
    val hidden = card(Rank.Queen, Suit.Hearts)
    val s = state(
      pile(card(Rank.Four), faceDown = listOf(hidden)),
      pile(card(Rank.Five, Suit.Hearts)),
    )
    val next = s.moveRun(0, 0, 1)!!
    assertEquals(listOf(hidden), next.tableau[0].faceUp)
    assertTrue(next.tableau[0].faceDown.isEmpty())
  }

  @Test
  fun `completing a king to ace run clears it and records the suit`() {
    val run = Rank.entries.reversed().map { card(it) } // King down to Ace of spades
    val s = state(
      pile(*run.dropLast(1).toTypedArray(), faceDown = listOf(card(Rank.Nine, Suit.Hearts))),
      pile(card(Rank.Ace)),
    )
    val next = s.moveRun(1, 0, 0)
    assertNotNull(next)
    assertEquals(listOf(Suit.Spades), next!!.completedRuns)
    // Run removed and hidden card flipped.
    assertEquals(listOf(card(Rank.Nine, Suit.Hearts)), next.tableau[0].faceUp)
    assertTrue(next.tableau[0].faceDown.isEmpty())
  }

  @Test
  fun `off-suit king to ace stack does not clear`() {
    val run = Rank.entries.reversed().map { card(it) }.toMutableList()
    run[12] = card(Rank.Ace, Suit.Hearts) // break suit purity at the ace
    val s = state(
      pile(*run.dropLast(1).toTypedArray()),
      pile(card(Rank.Ace, Suit.Hearts)),
    )
    val next = s.moveRun(1, 0, 0)
    assertNotNull(next)
    assertTrue(next!!.completedRuns.isEmpty())
    assertEquals(13, next.tableau[0].faceUp.size)
  }
}
