package se.bernhauser.solitaire.game.freecell

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

class FreeCellMoveTest {
  @Test
  fun `card moves onto opposite color one rank higher`() {
    val s = state(
      pile(card(Rank.Five, Suit.Hearts)),
      pile(card(Rank.Six, Suit.Spades)),
    )
    val next = s.moveToTableau(FreeCellTableauSource.TableauRun(0, 0), 1)
    assertNotNull(next)
    assertEquals(
      listOf(card(Rank.Six, Suit.Spades), card(Rank.Five, Suit.Hearts)),
      next!!.tableau[1].faceUp,
    )
    assertTrue(next.tableau[0].faceUp.isEmpty())
  }

  @Test
  fun `card cannot land on same color`() {
    val s = state(
      pile(card(Rank.Five, Suit.Clubs)),
      pile(card(Rank.Six, Suit.Spades)),
    )
    assertNull(s.moveToTableau(FreeCellTableauSource.TableauRun(0, 0), 1))
  }

  @Test
  fun `card cannot land on wrong rank`() {
    val s = state(
      pile(card(Rank.Five, Suit.Hearts)),
      pile(card(Rank.Seven, Suit.Spades)),
    )
    assertNull(s.moveToTableau(FreeCellTableauSource.TableauRun(0, 0), 1))
  }

  @Test
  fun `any card may move to an empty column`() {
    val s = state(pile(card(Rank.Two, Suit.Hearts)))
    val next = s.moveToTableau(FreeCellTableauSource.TableauRun(0, 0), 5)
    assertNotNull(next)
    assertEquals(listOf(card(Rank.Two, Suit.Hearts)), next!!.tableau[5].faceUp)
  }

  @Test
  fun `top card moves to an empty cell`() {
    val s = state(pile(card(Rank.Nine), card(Rank.Four, Suit.Hearts)))
    val next = s.moveToCell(FreeCellCardSource.TableauTop(0), 2)
    assertNotNull(next)
    assertEquals(card(Rank.Four, Suit.Hearts), next!!.cells[2])
    assertEquals(listOf(card(Rank.Nine)), next.tableau[0].faceUp)
  }

  @Test
  fun `card cannot move to an occupied cell`() {
    val s = state(
      pile(card(Rank.Four, Suit.Hearts)),
      cells = listOf(card(Rank.King), null, null, null),
    )
    assertNull(s.moveToCell(FreeCellCardSource.TableauTop(0), 0))
  }

  @Test
  fun `cell card may move to another empty cell but not itself`() {
    val s = state(cells = listOf(card(Rank.King), null, null, null))
    assertNull(s.moveToCell(FreeCellCardSource.Cell(0), 0))
    val next = s.moveToCell(FreeCellCardSource.Cell(0), 3)
    assertNotNull(next)
    assertNull(next!!.cells[0])
    assertEquals(card(Rank.King), next.cells[3])
  }

  @Test
  fun `cell card moves back to the tableau`() {
    val s = state(
      pile(card(Rank.Six, Suit.Spades)),
      cells = listOf(card(Rank.Five, Suit.Hearts), null, null, null),
    )
    val next = s.moveToTableau(FreeCellTableauSource.Cell(0), 0)
    assertNotNull(next)
    assertNull(next!!.cells[0])
    assertEquals(
      listOf(card(Rank.Six, Suit.Spades), card(Rank.Five, Suit.Hearts)),
      next.tableau[0].faceUp,
    )
  }

  @Test
  fun `ace starts a foundation and suit builds up in order`() {
    val s = state(pile(card(Rank.Two), card(Rank.Ace)))
    val afterAce = s.moveToFoundation(FreeCellCardSource.TableauTop(0))
    assertNotNull(afterAce)
    assertEquals(listOf(card(Rank.Ace)), afterAce!!.foundations[Suit.Spades.ordinal])
    val afterTwo = afterAce.moveToFoundation(FreeCellCardSource.TableauTop(0))
    assertNotNull(afterTwo)
    assertEquals(
      listOf(card(Rank.Ace), card(Rank.Two)),
      afterTwo!!.foundations[Suit.Spades.ordinal],
    )
    assertTrue(afterTwo.tableau[0].faceUp.isEmpty())
  }

  @Test
  fun `non-ace cannot start a foundation and ranks cannot be skipped`() {
    val foundations = List(4) { i ->
      if (i == Suit.Spades.ordinal) listOf(card(Rank.Ace)) else emptyList<Card>()
    }
    val s = state(pile(card(Rank.Two)), pile(card(Rank.Three)), foundations = foundations)
    assertNotNull(s.moveToFoundation(FreeCellCardSource.TableauTop(0)))
    assertNull(s.moveToFoundation(FreeCellCardSource.TableauTop(1)))
  }

  @Test
  fun `cell card moves to its foundation`() {
    val foundations = List(4) { i ->
      if (i == Suit.Hearts.ordinal) listOf(card(Rank.Ace, Suit.Hearts)) else emptyList<Card>()
    }
    val s = state(
      cells = listOf(card(Rank.Two, Suit.Hearts), null, null, null),
      foundations = foundations,
    )
    val next = s.moveToFoundation(FreeCellCardSource.Cell(0))
    assertNotNull(next)
    assertNull(next!!.cells[0])
    assertEquals(2, next.foundations[Suit.Hearts.ordinal].size)
  }

  @Test
  fun `moves from empty sources are rejected`() {
    val s = state(pile(card(Rank.Five, Suit.Hearts)))
    assertNull(s.moveToFoundation(FreeCellCardSource.Cell(0)))
    assertNull(s.moveToFoundation(FreeCellCardSource.TableauTop(5)))
    assertNull(s.moveToCell(FreeCellCardSource.TableauTop(5), 0))
    assertNull(s.moveToTableau(FreeCellTableauSource.Cell(0), 0))
  }
}
