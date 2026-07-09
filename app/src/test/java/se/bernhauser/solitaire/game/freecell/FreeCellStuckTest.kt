package se.bernhauser.solitaire.game.freecell

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import se.bernhauser.solitaire.game.Card
import se.bernhauser.solitaire.game.Rank
import se.bernhauser.solitaire.game.Suit
import se.bernhauser.solitaire.game.TableauPile

private fun card(rank: Rank, suit: Suit = Suit.Spades) = Card(rank, suit)

private fun pile(vararg faceUp: Card) = TableauPile(faceDown = emptyList(), faceUp = faceUp.toList())

/**
 * All cells full and every exposed card black with no adjacent ranks playable and no
 * aces reachable — no legal move of any kind.
 */
private fun stuckState(cells: List<Card?>) = FreeCellState(
  cells = cells,
  foundations = List(4) { emptyList() },
  tableau = listOf(
    pile(card(Rank.Two, Suit.Spades)),
    pile(card(Rank.Four, Suit.Spades)),
    pile(card(Rank.Six, Suit.Spades)),
    pile(card(Rank.Eight, Suit.Spades)),
    pile(card(Rank.Ten, Suit.Spades)),
    pile(card(Rank.Queen, Suit.Spades)),
    pile(card(Rank.Three, Suit.Clubs)),
    pile(card(Rank.Five, Suit.Clubs)),
  ),
)

private val blackCells: List<Card?> = listOf(
  card(Rank.Seven, Suit.Clubs),
  card(Rank.Nine, Suit.Clubs),
  card(Rank.Jack, Suit.Clubs),
  card(Rank.King, Suit.Clubs),
)

class FreeCellStuckTest {
  @Test
  fun `no legal move means stuck`() {
    val s = stuckState(blackCells)
    assertFalse(s.hasAnyMove())
    assertTrue(s.isStuck())
  }

  @Test
  fun `an empty cell always offers a move`() {
    val s = stuckState(blackCells.toMutableList().also { it[3] = null })
    assertTrue(s.hasAnyMove())
    assertFalse(s.isStuck())
  }

  @Test
  fun `a playable foundation card is a move`() {
    val s = stuckState(blackCells.toMutableList().also { it[3] = card(Rank.Ace, Suit.Hearts) })
    assertTrue(s.hasAnyMove())
  }

  @Test
  fun `a playable tableau placement is a move`() {
    // 4♥ in a cell can land on the 5♣ column top.
    val s = stuckState(blackCells.toMutableList().also { it[3] = card(Rank.Four, Suit.Hearts) })
    assertTrue(s.hasAnyMove())
  }

  @Test
  fun `an empty column always offers a move`() {
    val base = stuckState(blackCells)
    val s = base.copy(tableau = base.tableau.toMutableList().also { it[0] = TableauPile.Empty })
    assertTrue(s.hasAnyMove())
  }

  @Test
  fun `won game is not stuck`() {
    val s = FreeCellState(
      cells = List(FreeCellCount) { null },
      foundations = Suit.entries.map { suit -> Rank.entries.map { Card(it, suit) } },
      tableau = List(FreeCellColumnCount) { TableauPile.Empty },
    )
    assertTrue(s.isWon())
    assertFalse(s.isStuck())
  }

  @Test
  fun `fresh deal is not won`() {
    assertFalse(dealNewFreeCell(seed = 1L).isWon())
  }
}
