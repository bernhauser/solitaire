package se.bernhauser.solitaire.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import se.bernhauser.solitaire.game.TableauMoveSource.FoundationTop
import se.bernhauser.solitaire.game.TableauMoveSource.TableauRun
import se.bernhauser.solitaire.game.TableauMoveSource.WasteTop

class MoveToTableauTest {
  private val emptyTableau = List(7) { TableauPile.Empty }
  private val emptyFoundations = List(4) { emptyList<Card>() }

  private fun card(rank: Rank, suit: Suit) = Card(rank, suit)

  private fun state(
    waste: List<Card> = emptyList(),
    foundations: List<List<Card>> = emptyFoundations,
    tableau: List<TableauPile> = emptyTableau,
  ) = GameState(stock = emptyList(), waste = waste, foundations = foundations, tableau = tableau)

  private fun withColumn(index: Int, pile: TableauPile, base: List<TableauPile> = emptyTableau): List<TableauPile> =
    base.toMutableList().also { it[index] = pile }

  private fun foundationFor(suit: Suit, cards: List<Card>): List<List<Card>> =
    emptyFoundations.toMutableList().also { it[suit.ordinal] = cards }

  // --- WasteTop source ---

  @Test
  fun `WasteTop to empty column requires a King`() {
    val king = card(Rank.King, Suit.Hearts)
    val s = state(waste = listOf(card(Rank.Two, Suit.Clubs), king))
    val next = s.moveToTableau(WasteTop, destColumn = 0)!!
    assertEquals(listOf(card(Rank.Two, Suit.Clubs)), next.waste)
    assertEquals(TableauPile(faceDown = emptyList(), faceUp = listOf(king)), next.tableau[0])
  }

  @Test
  fun `WasteTop to empty column with non-King is rejected`() {
    val s = state(waste = listOf(card(Rank.Queen, Suit.Hearts)))
    assertNull(s.moveToTableau(WasteTop, destColumn = 0))
  }

  @Test
  fun `WasteTop onto non-empty column requires opposite color one rank lower`() {
    val destTop = card(Rank.Eight, Suit.Spades) // black 8
    val moved = card(Rank.Seven, Suit.Hearts)   // red 7
    val tableau = withColumn(3, TableauPile(faceDown = emptyList(), faceUp = listOf(destTop)))
    val s = state(waste = listOf(moved), tableau = tableau)
    val next = s.moveToTableau(WasteTop, destColumn = 3)!!
    assertEquals(emptyList<Card>(), next.waste)
    assertEquals(listOf(destTop, moved), next.tableau[3].faceUp)
  }

  @Test
  fun `WasteTop onto same-color top is rejected`() {
    val destTop = card(Rank.Eight, Suit.Spades) // black
    val moved = card(Rank.Seven, Suit.Clubs)    // black
    val tableau = withColumn(0, TableauPile(faceDown = emptyList(), faceUp = listOf(destTop)))
    val s = state(waste = listOf(moved), tableau = tableau)
    assertNull(s.moveToTableau(WasteTop, destColumn = 0))
  }

  @Test
  fun `WasteTop with wrong rank is rejected`() {
    val destTop = card(Rank.Eight, Suit.Spades)
    val moved = card(Rank.Six, Suit.Hearts) // off-by-one
    val tableau = withColumn(0, TableauPile(faceDown = emptyList(), faceUp = listOf(destTop)))
    val s = state(waste = listOf(moved), tableau = tableau)
    assertNull(s.moveToTableau(WasteTop, destColumn = 0))
  }

  @Test
  fun `WasteTop with empty waste is rejected`() {
    val tableau = withColumn(0, TableauPile(faceDown = emptyList(), faceUp = listOf(card(Rank.King, Suit.Spades))))
    val s = state(tableau = tableau)
    assertNull(s.moveToTableau(WasteTop, destColumn = 0))
  }

  // --- TableauRun source ---

  @Test
  fun `TableauRun moves the entire substack from fromIndex`() {
    // src col 0: faceDown [X], faceUp [J♠, T♥, 9♣]
    // dest col 1: faceUp [Q♦]
    // move fromIndex=0 → take all 3 face-ups, place on Q♦.
    val xDown = card(Rank.Two, Suit.Diamonds)
    val jS = card(Rank.Jack, Suit.Spades)
    val tH = card(Rank.Ten, Suit.Hearts)
    val nC = card(Rank.Nine, Suit.Clubs)
    val qD = card(Rank.Queen, Suit.Diamonds)
    val src = TableauPile(faceDown = listOf(xDown), faceUp = listOf(jS, tH, nC))
    val dest = TableauPile(faceDown = emptyList(), faceUp = listOf(qD))
    val tableau = emptyTableau.toMutableList().also {
      it[0] = src
      it[1] = dest
    }
    val s = state(tableau = tableau)
    val next = s.moveToTableau(TableauRun(column = 0, fromIndex = 0), destColumn = 1)!!
    // src column: face-down [X] uncovered → top flipped face-up
    assertEquals(TableauPile(faceDown = emptyList(), faceUp = listOf(xDown)), next.tableau[0])
    assertEquals(TableauPile(faceDown = emptyList(), faceUp = listOf(qD, jS, tH, nC)), next.tableau[1])
  }

  @Test
  fun `TableauRun with partial substack still requires whole tail to be a valid run`() {
    val jS = card(Rank.Jack, Suit.Spades)
    val tH = card(Rank.Ten, Suit.Hearts)
    val nC = card(Rank.Nine, Suit.Clubs)
    val tS = card(Rank.Ten, Suit.Spades) // dest top: black 10
    val src = TableauPile(faceDown = emptyList(), faceUp = listOf(jS, tH, nC))
    val dest = TableauPile(faceDown = emptyList(), faceUp = listOf(tS))
    val tableau = emptyTableau.toMutableList().also {
      it[0] = src
      it[1] = dest
    }
    val s = state(tableau = tableau)
    // move fromIndex=1 → [tH, nC] starts with red T onto black T? rank mismatch (need 9). Rejected.
    assertNull(s.moveToTableau(TableauRun(column = 0, fromIndex = 1), destColumn = 1))
  }

  @Test
  fun `TableauRun rejects an invalid face-up sequence`() {
    val badRun = TableauPile(
      faceDown = emptyList(),
      faceUp = listOf(card(Rank.Jack, Suit.Spades), card(Rank.Ten, Suit.Clubs)), // both black
    )
    val dest = TableauPile(faceDown = emptyList(), faceUp = listOf(card(Rank.Queen, Suit.Hearts)))
    val tableau = emptyTableau.toMutableList().also {
      it[0] = badRun
      it[1] = dest
    }
    val s = state(tableau = tableau)
    assertNull(s.moveToTableau(TableauRun(column = 0, fromIndex = 0), destColumn = 1))
  }

  @Test
  fun `TableauRun leaving column with no face-down and no face-up gives an empty pile`() {
    val king = card(Rank.King, Suit.Spades)
    val q = card(Rank.Queen, Suit.Hearts)
    val src = TableauPile(faceDown = emptyList(), faceUp = listOf(king))
    val dest = TableauPile(faceDown = emptyList(), faceUp = listOf(q)) // red Q
    // king onto red Q? No — kings go on empty columns or… well, k needs to go on empty. Let me pick a valid scenario.
    // Use dest as an empty column instead.
    val tableau = emptyTableau.toMutableList().also {
      it[0] = src
      it[1] = TableauPile.Empty
    }
    val s = state(tableau = tableau)
    val next = s.moveToTableau(TableauRun(column = 0, fromIndex = 0), destColumn = 1)!!
    assertEquals(TableauPile.Empty, next.tableau[0])
    assertEquals(TableauPile(faceDown = emptyList(), faceUp = listOf(king)), next.tableau[1])
  }

  @Test
  fun `TableauRun cannot move onto its own column`() {
    val src = TableauPile(faceDown = emptyList(), faceUp = listOf(card(Rank.King, Suit.Spades)))
    val tableau = withColumn(0, src)
    val s = state(tableau = tableau)
    assertNull(s.moveToTableau(TableauRun(column = 0, fromIndex = 0), destColumn = 0))
  }

  // --- FoundationTop source ---

  @Test
  fun `FoundationTop moves the top foundation card back to a tableau column`() {
    val ace = card(Rank.Ace, Suit.Diamonds)
    val two = card(Rank.Two, Suit.Diamonds) // red 2 — moving back onto black 3
    val foundations = foundationFor(Suit.Diamonds, listOf(ace, two))
    val three = card(Rank.Three, Suit.Spades)
    val tableau = withColumn(0, TableauPile(faceDown = emptyList(), faceUp = listOf(three)))
    val s = state(foundations = foundations, tableau = tableau)
    val next = s.moveToTableau(FoundationTop(Suit.Diamonds), destColumn = 0)!!
    assertEquals(listOf(ace), next.foundations[Suit.Diamonds.ordinal])
    assertEquals(listOf(three, two), next.tableau[0].faceUp)
  }

  @Test
  fun `FoundationTop is rejected when foundation is empty`() {
    val tableau = withColumn(0, TableauPile(faceDown = emptyList(), faceUp = listOf(card(Rank.King, Suit.Spades))))
    val s = state(tableau = tableau)
    assertNull(s.moveToTableau(FoundationTop(Suit.Hearts), destColumn = 0))
  }
}
