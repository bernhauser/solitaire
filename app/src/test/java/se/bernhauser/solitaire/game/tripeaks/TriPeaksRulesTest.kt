package se.bernhauser.solitaire.game.tripeaks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import se.bernhauser.solitaire.game.Card
import se.bernhauser.solitaire.game.Rank
import se.bernhauser.solitaire.game.Suit

private fun card(rank: Rank, suit: Suit = Suit.Spades) = Card(rank, suit)

/** A full board of unplayable sevens with specific slots overridden. */
private fun fullBoard(vararg overrides: Pair<Int, Card?>): List<Card?> {
  val board = MutableList<Card?>(TriPeaksSlotCount) { card(Rank.Seven, Suit.Hearts) }
  overrides.forEach { (index, c) -> board[index] = c }
  return board
}

private fun state(
  board: List<Card?>,
  stock: List<Card> = emptyList(),
  waste: List<Card> = listOf(card(Rank.Ten)),
) = TriPeaksState(board = board, stock = stock, waste = waste)

class TriPeaksRulesTest {
  @Test
  fun `one rank above or below the waste top is playable`() {
    val s = state(fullBoard(26 to card(Rank.Jack), 27 to card(Rank.Nine)))
    assertTrue(s.canPlay(26))
    assertTrue(s.canPlay(27))
  }

  @Test
  fun `equal or distant ranks are not playable`() {
    val s = state(fullBoard(26 to card(Rank.Ten, Suit.Hearts), 27 to card(Rank.Seven)))
    assertFalse(s.canPlay(26))
    assertFalse(s.canPlay(27))
  }

  @Test
  fun `king and ace wrap around`() {
    val onKing = state(fullBoard(27 to card(Rank.Ace)), waste = listOf(card(Rank.King)))
    assertTrue(onKing.canPlay(27))
    val onAce = state(fullBoard(27 to card(Rank.King)), waste = listOf(card(Rank.Ace)))
    assertTrue(onAce.canPlay(27))
  }

  @Test
  fun `playing moves the card to the waste`() {
    val jack = card(Rank.Jack)
    val s = state(fullBoard(27 to jack))
    val next = s.play(27)
    assertNotNull(next)
    assertNull(next!!.board[27])
    assertEquals(jack, next.waste.last())
  }

  @Test
  fun `covered cards are not playable even with a matching rank`() {
    // Slot 9 sits on base cards 18 and 19.
    val s = state(fullBoard(9 to card(Rank.Jack)))
    assertFalse(s.canPlay(9))
    assertNull(s.play(9))
  }

  @Test
  fun `clearing both children uncovers the parent, one is not enough`() {
    val oneGone = state(fullBoard(9 to card(Rank.Jack), 18 to null))
    assertFalse(oneGone.isUncovered(9))
    assertFalse(oneGone.canPlay(9))
    val bothGone = state(fullBoard(9 to card(Rank.Jack), 18 to null, 19 to null))
    assertTrue(bothGone.isUncovered(9))
    assertTrue(bothGone.canPlay(9))
  }

  @Test
  fun `cleared and out-of-range slots are never playable`() {
    val s = state(fullBoard(27 to null))
    assertFalse(s.canPlay(27))
    assertNull(s.play(27))
    assertFalse(s.canPlay(99))
  }

  @Test
  fun `draw moves the top stock card to the waste`() {
    val bottom = card(Rank.Two)
    val top = card(Rank.Three)
    val s = state(fullBoard(), stock = listOf(bottom, top))
    val next = s.drawFromStock()
    assertNotNull(next)
    assertEquals(listOf(bottom), next!!.stock)
    assertEquals(top, next.waste.last())
    assertNull(state(fullBoard()).drawFromStock())
  }

  @Test
  fun `won when the board is empty`() {
    val s = state(List(TriPeaksSlotCount) { null }, stock = listOf(card(Rank.Two)))
    assertTrue(s.isWon())
    assertFalse(s.isStuck())
  }

  @Test
  fun `stuck only when the stock is empty and nothing is playable`() {
    val dead = state(fullBoard())
    assertTrue(dead.isStuck())
    val withStock = state(fullBoard(), stock = listOf(card(Rank.Two)))
    assertFalse(withStock.isStuck())
    val withPlay = state(fullBoard(27 to card(Rank.Jack)))
    assertFalse(withPlay.isStuck())
  }
}
