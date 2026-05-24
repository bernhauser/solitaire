package se.bernhauser.solitaire.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StockWasteTest {
  private val emptyTableau = List(7) { TableauPile.Empty }
  private val emptyFoundations = List(4) { emptyList<Card>() }

  private fun stateWith(stock: List<Card>, waste: List<Card> = emptyList()): GameState =
    GameState(stock = stock, waste = waste, foundations = emptyFoundations, tableau = emptyTableau)

  private val A = Card(Rank.Ace, Suit.Spades)
  private val B = Card(Rank.Two, Suit.Spades)
  private val C = Card(Rank.Three, Suit.Spades)
  private val D = Card(Rank.Four, Suit.Spades)
  private val E = Card(Rank.Five, Suit.Spades)

  @Test
  fun `drawFromStock flips top 3 cards onto waste, last drawn on top`() {
    // stock top = E. Drawing flips E, then D, then C: waste becomes [E, D, C], top = C.
    val state = stateWith(stock = listOf(A, B, C, D, E))
    val next = state.drawFromStock()!!
    assertEquals(listOf(A, B), next.stock)
    assertEquals(listOf(E, D, C), next.waste)
  }

  @Test
  fun `drawFromStock with 2 cards flips both`() {
    val state = stateWith(stock = listOf(A, B))
    val next = state.drawFromStock()!!
    assertEquals(emptyList<Card>(), next.stock)
    assertEquals(listOf(B, A), next.waste)
  }

  @Test
  fun `drawFromStock with 1 card flips one`() {
    val state = stateWith(stock = listOf(A))
    val next = state.drawFromStock()!!
    assertEquals(emptyList<Card>(), next.stock)
    assertEquals(listOf(A), next.waste)
  }

  @Test
  fun `drawFromStock with empty stock returns null`() {
    val state = stateWith(stock = emptyList(), waste = listOf(A))
    assertNull(state.drawFromStock())
  }

  @Test
  fun `drawFromStock appends to existing waste`() {
    val state = stateWith(stock = listOf(C, D, E), waste = listOf(A, B))
    val next = state.drawFromStock()!!
    assertEquals(emptyList<Card>(), next.stock)
    assertEquals(listOf(A, B, E, D, C), next.waste)
  }

  @Test
  fun `recycleWaste turns waste over into stock when stock is empty`() {
    // waste [A, B, C, D, E] (E on top). After recycle, top of stock = A (was bottom of waste).
    val state = stateWith(stock = emptyList(), waste = listOf(A, B, C, D, E))
    val next = state.recycleWaste()!!
    assertEquals(emptyList<Card>(), next.waste)
    assertEquals(listOf(E, D, C, B, A), next.stock)
  }

  @Test
  fun `recycleWaste returns null when stock is not empty`() {
    val state = stateWith(stock = listOf(A), waste = listOf(B))
    assertNull(state.recycleWaste())
  }

  @Test
  fun `recycleWaste returns null when waste is empty`() {
    val state = stateWith(stock = emptyList(), waste = emptyList())
    assertNull(state.recycleWaste())
  }
}
