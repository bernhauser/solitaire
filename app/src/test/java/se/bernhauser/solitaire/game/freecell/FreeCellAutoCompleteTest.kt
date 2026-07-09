package se.bernhauser.solitaire.game.freecell

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

private fun pile(vararg faceUp: Card) = TableauPile(faceDown = emptyList(), faceUp = faceUp.toList())

private fun state(
  vararg piles: TableauPile,
  cells: List<Card?> = List(FreeCellCount) { null },
  foundations: List<List<Card>> = List(4) { emptyList() },
) = FreeCellState(
  cells = cells,
  foundations = foundations,
  tableau = piles.toList() + List(FreeCellColumnCount - piles.size) { TableauPile.Empty },
)

private fun foundationsUpTo(rank: Rank): List<List<Card>> =
  Suit.entries.map { suit ->
    Rank.entries.takeWhile { it.value <= rank.value }.map { Card(it, suit) }
  }

class FreeCellAutoCompleteTest {
  @Test
  fun `finish is offered when every column is descending`() {
    val s = state(
      pile(card(Rank.Three, Suit.Spades), card(Rank.Two, Suit.Hearts)),
      pile(card(Rank.Two, Suit.Spades)),
      pile(card(Rank.Three, Suit.Hearts), card(Rank.Two, Suit.Diamonds), card(Rank.Two, Suit.Clubs)),
      foundations = foundationsUpTo(Rank.Ace),
      cells = listOf(card(Rank.Three, Suit.Clubs), null, null, card(Rank.Three, Suit.Diamonds)),
    )
    assertTrue(s.canAutoComplete())
  }

  @Test
  fun `finish is not offered while a column is unordered`() {
    val s = state(
      pile(card(Rank.Two, Suit.Hearts), card(Rank.Three, Suit.Spades)),
      pile(card(Rank.Two, Suit.Spades)),
      foundations = foundationsUpTo(Rank.Ace),
    )
    assertFalse(s.canAutoComplete())
  }

  @Test
  fun `finish is not offered without a playable card`() {
    // Columns are ordered but no ace is available, so nothing can be played.
    val s = state(
      pile(card(Rank.Three, Suit.Spades), card(Rank.Two, Suit.Spades)),
      pile(card(Rank.Five, Suit.Hearts), card(Rank.Four, Suit.Hearts)),
    )
    assertNull(s.nextAutoCompleteSource())
    assertFalse(s.canAutoComplete())
  }

  @Test
  fun `finish is not offered for the single last move`() {
    val foundations = Suit.entries.map { suit ->
      if (suit == Suit.Spades) {
        Rank.entries.dropLast(1).map { Card(it, suit) }
      } else {
        Rank.entries.map { Card(it, suit) }
      }
    }
    val s = state(pile(card(Rank.King, Suit.Spades)), foundations = foundations)
    assertFalse(s.canAutoComplete())
  }

  @Test
  fun `cells are drained before tableau tops`() {
    val s = state(
      pile(card(Rank.Two, Suit.Spades)),
      foundations = foundationsUpTo(Rank.Ace),
      cells = listOf(null, card(Rank.Two, Suit.Hearts), null, null),
    )
    assertEquals(FreeCellCardSource.Cell(1), s.nextAutoCompleteSource())
  }

  @Test
  fun `auto-completing an ordered endgame reaches the win`() {
    var s = state(
      pile(card(Rank.King, Suit.Spades), card(Rank.Queen, Suit.Hearts)),
      pile(card(Rank.King, Suit.Hearts), card(Rank.Queen, Suit.Spades)),
      pile(card(Rank.King, Suit.Diamonds), card(Rank.Queen, Suit.Clubs)),
      pile(card(Rank.King, Suit.Clubs)),
      foundations = foundationsUpTo(Rank.Jack),
      cells = listOf(card(Rank.Queen, Suit.Diamonds), null, null, null),
    )
    assertTrue(s.canAutoComplete())
    var steps = 0
    while (true) {
      val source = s.nextAutoCompleteSource() ?: break
      s = s.moveToFoundation(source) ?: error("unplayable auto-complete source")
      steps++
    }
    assertTrue(s.isWon())
    assertEquals(8, steps)
  }
}
