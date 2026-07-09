package se.bernhauser.solitaire.game.freecell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import se.bernhauser.solitaire.game.Card
import se.bernhauser.solitaire.game.Rank
import se.bernhauser.solitaire.game.Suit
import se.bernhauser.solitaire.game.TableauPile

private fun card(rank: Rank, suit: Suit = Suit.Spades) = Card(rank, suit)

private fun pile(vararg faceUp: Card) = TableauPile(faceDown = emptyList(), faceUp = faceUp.toList())

private fun fullState(
  vararg piles: TableauPile,
  cells: List<Card?> = List(FreeCellCount) { null },
): FreeCellState {
  // Pad the remaining columns with filler so no column is unintentionally empty.
  val filler = pile(card(Rank.King, Suit.Diamonds))
  return FreeCellState(
    cells = cells,
    foundations = List(4) { emptyList() },
    tableau = piles.toList() + List(FreeCellColumnCount - piles.size) { filler },
  )
}

private val fullCells: List<Card?> = listOf(
  card(Rank.King, Suit.Clubs),
  card(Rank.King, Suit.Spades),
  card(Rank.Queen, Suit.Clubs),
  card(Rank.Queen, Suit.Spades),
)

/** 6♠ 5♥ 4♣ — a valid alternating-color run. */
private val runOfThree = arrayOf(
  card(Rank.Six, Suit.Spades),
  card(Rank.Five, Suit.Hearts),
  card(Rank.Four, Suit.Clubs),
)

class FreeCellSupermoveTest {
  @Test
  fun `capacity is free cells plus one, doubled per empty column`() {
    val noneFree = fullState(cells = fullCells)
    assertEquals(1, noneFree.maxMovableRun(0))

    val allFree = fullState()
    assertEquals(5, allFree.maxMovableRun(0))

    val oneEmptyColumn = FreeCellState(
      cells = List(FreeCellCount) { null },
      foundations = List(4) { emptyList() },
      tableau = List(FreeCellColumnCount) { col ->
        if (col == 7) TableauPile.Empty else pile(card(Rank.King, Suit.Diamonds))
      },
    )
    assertEquals(10, oneEmptyColumn.maxMovableRun(0))
    // The empty destination itself doesn't double the capacity.
    assertEquals(5, oneEmptyColumn.maxMovableRun(7))
  }

  @Test
  fun `run within capacity moves as a unit`() {
    val s = fullState(
      pile(*runOfThree),
      pile(card(Rank.Seven, Suit.Hearts)),
    )
    val next = s.moveToTableau(FreeCellTableauSource.TableauRun(0, 0), 1)
    assertNotNull(next)
    assertEquals(4, next!!.tableau[1].faceUp.size)
    assertEquals(0, next.tableau[0].faceUp.size)
  }

  @Test
  fun `run larger than capacity is rejected even when placement fits`() {
    val s = fullState(
      pile(*runOfThree),
      pile(card(Rank.Seven, Suit.Hearts)),
      cells = fullCells,
    )
    assertNull(s.moveToTableau(FreeCellTableauSource.TableauRun(0, 0), 1))
    // The single top card still moves: 4♣ onto 5♦.
    val single = fullState(
      pile(*runOfThree),
      pile(card(Rank.Five, Suit.Diamonds)),
      cells = fullCells,
    )
    assertNotNull(single.moveToTableau(FreeCellTableauSource.TableauRun(0, 2), 1))
  }

  @Test
  fun `same color sequence is not a movable run`() {
    val s = fullState(
      pile(card(Rank.Six, Suit.Spades), card(Rank.Five, Suit.Clubs)),
      pile(card(Rank.Seven, Suit.Hearts)),
    )
    assertNull(s.moveToTableau(FreeCellTableauSource.TableauRun(0, 0), 1))
  }

  @Test
  fun `gapped sequence is not a movable run`() {
    val s = fullState(
      pile(card(Rank.Six, Suit.Spades), card(Rank.Four, Suit.Hearts)),
      pile(card(Rank.Seven, Suit.Hearts)),
    )
    assertNull(s.moveToTableau(FreeCellTableauSource.TableauRun(0, 0), 1))
  }
}
