package se.bernhauser.solitaire.ui.board

import org.junit.Assert.assertEquals
import org.junit.Test
import se.bernhauser.solitaire.game.Card
import se.bernhauser.solitaire.game.GameState
import se.bernhauser.solitaire.game.Rank
import se.bernhauser.solitaire.game.Suit
import se.bernhauser.solitaire.game.TableauPile
import se.bernhauser.solitaire.game.dealNewGame

class StartupRevealPlanTest {
  @Test
  fun `new game reveal order is stock then tableau rows`() {
    val plan = buildStartupRevealPlan(dealNewGame(seed = 7L))

    assertEquals(22, plan.coveredTotal)
    assertEquals(7, plan.faceUpTotal)
    assertEquals(0, plan.stockCoveredIndex)
    assertEquals(1, plan.tableauCoveredIndex(column = 1, row = 0))
    assertEquals(6, plan.tableauCoveredIndex(column = 6, row = 0))
    assertEquals(7, plan.tableauCoveredIndex(column = 2, row = 1))
    assertEquals(21, plan.tableauCoveredIndex(column = 6, row = 5))
    assertEquals(0, plan.tableauFaceUpIndex(column = 0, row = 0))
    assertEquals(6, plan.tableauFaceUpIndex(column = 6, row = 0))
  }

  @Test
  fun `restore reveal order is top row face ups then tableau face up rows`() {
    val state = GameState(
      stock = listOf(card(Rank.Ace, Suit.Spades)),
      waste = listOf(card(Rank.Two, Suit.Clubs), card(Rank.Three, Suit.Clubs), card(Rank.Four, Suit.Clubs), card(Rank.Five, Suit.Clubs)),
      foundations = listOf(
        listOf(card(Rank.Ace, Suit.Hearts)),
        emptyList(),
        listOf(card(Rank.Ace, Suit.Spades)),
        emptyList(),
      ),
      tableau = listOf(
        TableauPile(faceDown = listOf(card(Rank.Six, Suit.Hearts)), faceUp = listOf(card(Rank.Seven, Suit.Clubs), card(Rank.Eight, Suit.Diamonds))),
        TableauPile(faceDown = emptyList(), faceUp = listOf(card(Rank.Nine, Suit.Spades))),
        TableauPile(faceDown = listOf(card(Rank.Ten, Suit.Hearts), card(Rank.Jack, Suit.Clubs)), faceUp = listOf(card(Rank.Queen, Suit.Diamonds), card(Rank.King, Suit.Spades))),
        TableauPile.Empty,
        TableauPile.Empty,
        TableauPile.Empty,
        TableauPile.Empty,
      ),
    )

    val plan = buildStartupRevealPlan(state)

    assertEquals(4, plan.coveredTotal)
    assertEquals(10, plan.faceUpTotal)
    assertEquals(0, plan.stockCoveredIndex)
    assertEquals(1, plan.tableauCoveredIndex(column = 0, row = 0))
    assertEquals(2, plan.tableauCoveredIndex(column = 2, row = 0))
    assertEquals(3, plan.tableauCoveredIndex(column = 2, row = 1))
    assertEquals(0, plan.foundationFaceUpIndex(displayedIndex = 0))
    assertEquals(1, plan.foundationFaceUpIndex(displayedIndex = 1))
    assertEquals(2, plan.wasteFaceUpIndex(visibleIndex = 0))
    assertEquals(4, plan.wasteFaceUpIndex(visibleIndex = 2))
    assertEquals(5, plan.tableauFaceUpIndex(column = 0, row = 0))
    assertEquals(6, plan.tableauFaceUpIndex(column = 1, row = 0))
    assertEquals(7, plan.tableauFaceUpIndex(column = 2, row = 0))
    assertEquals(8, plan.tableauFaceUpIndex(column = 0, row = 1))
    assertEquals(9, plan.tableauFaceUpIndex(column = 2, row = 1))
  }

  private fun card(rank: Rank, suit: Suit): Card = Card(rank, suit)
}
