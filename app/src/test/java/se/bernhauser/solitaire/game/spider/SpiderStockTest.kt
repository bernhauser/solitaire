package se.bernhauser.solitaire.game.spider

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import se.bernhauser.solitaire.game.Card
import se.bernhauser.solitaire.game.Rank
import se.bernhauser.solitaire.game.Suit
import se.bernhauser.solitaire.game.TableauPile

private fun card(rank: Rank, suit: Suit = Suit.Spades) = Card(rank, suit)

class SpiderStockTest {
  @Test
  fun `dealing adds one face-up card to every column`() {
    val start = dealNewSpider(SpiderDifficulty.OneSuit, seed = 3L)
    val next = start.dealFromStock()
    assertNotNull(next)
    assertEquals(40, next!!.stock.size)
    next.tableau.forEachIndexed { col, pile ->
      assertEquals(start.tableau[col].faceUp.size + 1, pile.faceUp.size)
      assertEquals(start.tableau[col].faceDown.size, pile.faceDown.size)
    }
  }

  @Test
  fun `dealing is blocked while a column is empty`() {
    val start = dealNewSpider(SpiderDifficulty.OneSuit, seed = 3L)
    val withEmptyColumn = start.copy(
      tableau = start.tableau.mapIndexed { i, pile -> if (i == 0) TableauPile.Empty else pile },
    )
    assertFalse(withEmptyColumn.canDealFromStock())
    assertNull(withEmptyColumn.dealFromStock())
  }

  @Test
  fun `dealing is blocked when the stock is empty`() {
    val start = dealNewSpider(SpiderDifficulty.OneSuit, seed = 3L)
    val drained = start.copy(stock = emptyList())
    assertFalse(drained.canDealFromStock())
    assertNull(drained.dealFromStock())
  }

  @Test
  fun `five deals empty the stock`() {
    var state = dealNewSpider(SpiderDifficulty.TwoSuits, seed = 11L)
    repeat(5) {
      state = state.dealFromStock() ?: error("deal $it failed")
    }
    assertTrue(state.stock.isEmpty())
    assertEquals(104, state.tableau.sumOf { it.faceDown.size + it.faceUp.size } + state.completedRuns.size * 13)
  }

  @Test
  fun `a deal that completes a run clears it`() {
    // Column 0 holds King..Two of spades; dealing must drop the ace of spades there.
    val runWithoutAce = Rank.entries.reversed().dropLast(1).map { card(it) }
    val tableau = listOf(TableauPile(faceDown = emptyList(), faceUp = runWithoutAce)) +
      List(SpiderColumnCount - 1) { TableauPile(faceDown = emptyList(), faceUp = listOf(card(Rank.Eight, Suit.Hearts))) }
    // dealFromStock takes the last 10 stock cards, reversed: the stock's last card lands on column 0.
    val state = SpiderState(
      stock = List(10) { card(Rank.Seven, Suit.Hearts) } +
        List(9) { card(Rank.Four, Suit.Hearts) } +
        listOf(card(Rank.Ace)),
      completedRuns = emptyList(),
      tableau = tableau,
    )
    val next = state.dealFromStock()
    assertNotNull(next)
    assertEquals(listOf(Suit.Spades), next!!.completedRuns)
    assertTrue(next.tableau[0].faceUp.isEmpty())
  }
}
