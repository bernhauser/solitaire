package se.bernhauser.solitaire.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import se.bernhauser.solitaire.game.FoundationMoveSource.TableauTop
import se.bernhauser.solitaire.game.FoundationMoveSource.WasteTop

class MoveToFoundationTest {
  private val emptyTableau = List(7) { TableauPile.Empty }
  private val emptyFoundations = List(4) { emptyList<Card>() }

  private fun card(rank: Rank, suit: Suit) = Card(rank, suit)

  private fun state(
    waste: List<Card> = emptyList(),
    foundations: List<List<Card>> = emptyFoundations,
    tableau: List<TableauPile> = emptyTableau,
  ) = GameState(stock = emptyList(), waste = waste, foundations = foundations, tableau = tableau)

  private fun withColumn(index: Int, pile: TableauPile): List<TableauPile> =
    emptyTableau.toMutableList().also { it[index] = pile }

  private fun withFoundation(suit: Suit, cards: List<Card>): List<List<Card>> =
    emptyFoundations.toMutableList().also { it[suit.ordinal] = cards }

  @Test
  fun `WasteTop ace goes to its suit's empty foundation`() {
    val ace = card(Rank.Ace, Suit.Hearts)
    val s = state(waste = listOf(card(Rank.Two, Suit.Clubs), ace))
    val next = s.moveToFoundation(WasteTop)!!
    assertEquals(listOf(ace), next.foundations[Suit.Hearts.ordinal])
    assertEquals(listOf(card(Rank.Two, Suit.Clubs)), next.waste)
  }

  @Test
  fun `WasteTop non-Ace onto empty foundation is rejected`() {
    val s = state(waste = listOf(card(Rank.Two, Suit.Hearts)))
    assertNull(s.moveToFoundation(WasteTop))
  }

  @Test
  fun `WasteTop one rank higher and same suit succeeds`() {
    val ace = card(Rank.Ace, Suit.Hearts)
    val two = card(Rank.Two, Suit.Hearts)
    val s = state(waste = listOf(two), foundations = withFoundation(Suit.Hearts, listOf(ace)))
    val next = s.moveToFoundation(WasteTop)!!
    assertEquals(listOf(ace, two), next.foundations[Suit.Hearts.ordinal])
  }

  @Test
  fun `WasteTop wrong suit is rejected`() {
    val ace = card(Rank.Ace, Suit.Hearts)
    val twoClubs = card(Rank.Two, Suit.Clubs)
    val s = state(waste = listOf(twoClubs), foundations = withFoundation(Suit.Hearts, listOf(ace)))
    assertNull(s.moveToFoundation(WasteTop))
  }

  @Test
  fun `WasteTop wrong rank is rejected`() {
    val ace = card(Rank.Ace, Suit.Hearts)
    val threeHearts = card(Rank.Three, Suit.Hearts) // skips 2
    val s = state(waste = listOf(threeHearts), foundations = withFoundation(Suit.Hearts, listOf(ace)))
    assertNull(s.moveToFoundation(WasteTop))
  }

  @Test
  fun `WasteTop with empty waste is rejected`() {
    assertNull(state().moveToFoundation(WasteTop))
  }

  @Test
  fun `TableauTop moves the column's top face-up to the foundation`() {
    val ace = card(Rank.Ace, Suit.Spades)
    val src = TableauPile(faceDown = emptyList(), faceUp = listOf(ace))
    val s = state(tableau = withColumn(0, src))
    val next = s.moveToFoundation(TableauTop(0))!!
    assertEquals(listOf(ace), next.foundations[Suit.Spades.ordinal])
    assertEquals(TableauPile.Empty, next.tableau[0])
  }

  @Test
  fun `TableauTop auto-flips a newly exposed face-down card`() {
    val flipped = card(Rank.Ten, Suit.Hearts)
    val ace = card(Rank.Ace, Suit.Spades)
    val src = TableauPile(faceDown = listOf(flipped), faceUp = listOf(ace))
    val s = state(tableau = withColumn(0, src))
    val next = s.moveToFoundation(TableauTop(0))!!
    assertEquals(TableauPile(faceDown = emptyList(), faceUp = listOf(flipped)), next.tableau[0])
  }

  @Test
  fun `TableauTop with empty face-up column is rejected`() {
    val s = state(tableau = withColumn(0, TableauPile.Empty))
    assertNull(s.moveToFoundation(TableauTop(0)))
  }

  @Test
  fun `TableauTop with out-of-range column is rejected`() {
    assertNull(state().moveToFoundation(TableauTop(99)))
  }
}
