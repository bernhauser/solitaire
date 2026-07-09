package se.bernhauser.solitaire.game.pyramid

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

/** A full pyramid of sevens (7+7=14, never a pair) with specific slots overridden. */
private fun fullBoard(vararg overrides: Pair<Int, Card?>): List<Card?> {
  val board = MutableList<Card?>(PyramidSlotCount) { card(Rank.Seven, Suit.Hearts) }
  overrides.forEach { (index, c) -> board[index] = c }
  return board
}

private fun state(
  board: List<Card?>,
  stock: List<Card> = emptyList(),
  waste: List<Card> = emptyList(),
  redealsLeft: Int = PyramidRedeals,
) = PyramidState(board = board, stock = stock, waste = waste, redealsLeft = redealsLeft)

class PyramidRulesTest {
  @Test
  fun `two uncovered cards summing to 13 are removed together`() {
    val s = state(fullBoard(21 to card(Rank.Queen), 22 to card(Rank.Ace, Suit.Hearts)))
    val next = s.removePair(PyramidPick.Board(21), PyramidPick.Board(22))
    assertNotNull(next)
    assertNull(next!!.board[21])
    assertNull(next.board[22])
  }

  @Test
  fun `pairs not summing to 13 are rejected`() {
    val s = state(fullBoard(21 to card(Rank.Queen), 22 to card(Rank.Two, Suit.Hearts)))
    assertNull(s.removePair(PyramidPick.Board(21), PyramidPick.Board(22)))
  }

  @Test
  fun `covered cards cannot pair`() {
    // Slot 15 (row 5) is covered by base slots 21 and 22.
    val s = state(fullBoard(15 to card(Rank.Ace), 23 to card(Rank.Queen)))
    assertNull(s.removePair(PyramidPick.Board(15), PyramidPick.Board(23)))
  }

  @Test
  fun `a king is removed alone and non-kings are not`() {
    val s = state(fullBoard(21 to card(Rank.King), 22 to card(Rank.Queen)))
    val next = s.removeKing(PyramidPick.Board(21))
    assertNotNull(next)
    assertNull(next!!.board[21])
    assertNull(s.removeKing(PyramidPick.Board(22)))
  }

  @Test
  fun `the waste top pairs with a pyramid card`() {
    val s = state(
      fullBoard(21 to card(Rank.Queen)),
      waste = listOf(card(Rank.Five), card(Rank.Ace, Suit.Diamonds)),
    )
    val next = s.removePair(PyramidPick.Waste, PyramidPick.Board(21))
    assertNotNull(next)
    assertNull(next!!.board[21])
    assertEquals(listOf(card(Rank.Five)), next.waste)
  }

  @Test
  fun `a card cannot pair with itself`() {
    val s = state(fullBoard(21 to card(Rank.Queen)), waste = listOf(card(Rank.Ace)))
    assertNull(s.removePair(PyramidPick.Board(21), PyramidPick.Board(21)))
    assertNull(s.removePair(PyramidPick.Waste, PyramidPick.Waste))
  }

  @Test
  fun `clearing both children uncovers the parent, one is not enough`() {
    val oneGone = state(fullBoard(21 to null))
    assertFalse(oneGone.isUncovered(15))
    val bothGone = state(fullBoard(21 to null, 22 to null))
    assertTrue(bothGone.isUncovered(15))
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
  fun `recycling turns the waste back into the stock and spends a redeal`() {
    val a = card(Rank.Two)
    val b = card(Rank.Three)
    val c = card(Rank.Four)
    val s = state(fullBoard(), waste = listOf(a, b, c), redealsLeft = 2)
    val next = s.recycleWaste()
    assertNotNull(next)
    assertEquals(listOf(c, b, a), next!!.stock)
    assertTrue(next.waste.isEmpty())
    assertEquals(1, next.redealsLeft)
    // Drawing again returns the cards in the original waste order.
    assertEquals(a, next.drawFromStock()!!.waste.last())
  }

  @Test
  fun `recycling is rejected without redeals, with stock left, or with an empty waste`() {
    assertNull(state(fullBoard(), waste = listOf(card(Rank.Two)), redealsLeft = 0).recycleWaste())
    assertNull(
      state(fullBoard(), stock = listOf(card(Rank.Three)), waste = listOf(card(Rank.Two)))
        .recycleWaste()
    )
    assertNull(state(fullBoard()).recycleWaste())
  }

  @Test
  fun `won when the pyramid is cleared regardless of leftovers`() {
    val s = state(List(PyramidSlotCount) { null }, stock = listOf(card(Rank.Two)), waste = listOf(card(Rank.Five)))
    assertTrue(s.isWon())
    assertFalse(s.isStuck())
  }

  @Test
  fun `stuck only when no pair, no king, no stock and no recycle remain`() {
    val dead = state(fullBoard())
    assertTrue(dead.isStuck())
    assertFalse(state(fullBoard(), stock = listOf(card(Rank.Two))).isStuck())
    // A waste six pairs with an exposed seven.
    assertFalse(state(fullBoard(), waste = listOf(card(Rank.Six)), redealsLeft = 0).isStuck())
    // An unplayable waste card still allows a recycle while redeals remain.
    assertFalse(state(fullBoard(), waste = listOf(card(Rank.Two))).isStuck())
    assertTrue(state(fullBoard(), waste = listOf(card(Rank.Two)), redealsLeft = 0).isStuck())
    // An exposed king is always a move.
    assertFalse(state(fullBoard(27 to card(Rank.King))).isStuck())
  }
}
