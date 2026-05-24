package se.bernhauser.solitaire.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoCompleteTest {
  private val emptyTableau = List(7) { TableauPile.Empty }

  private fun fullFoundationFor(suit: Suit): List<Card> =
    Rank.entries.map { Card(it, suit) }

  @Test
  fun `canAutoComplete is false on freshly dealt game`() {
    assertFalse(dealNewGame(seed = 1L).canAutoComplete())
  }

  @Test
  fun `canAutoComplete is false when already won`() {
    val state = GameState(
      stock = emptyList(),
      waste = emptyList(),
      foundations = Suit.entries.map { fullFoundationFor(it) },
      tableau = emptyTableau,
    )
    assertFalse(state.canAutoComplete())
  }

  @Test
  fun `canAutoComplete is false when stock has cards`() {
    val state = GameState(
      stock = listOf(Card(Rank.Two, Suit.Hearts)),
      waste = emptyList(),
      foundations = List(4) { emptyList() },
      tableau = List(7) {
        TableauPile(faceDown = emptyList(), faceUp = listOf(Card(Rank.Ace, Suit.Spades)))
      },
    )
    assertFalse(state.canAutoComplete())
  }

  @Test
  fun `canAutoComplete is false when any tableau pile has face-down cards`() {
    val state = GameState(
      stock = emptyList(),
      waste = emptyList(),
      foundations = List(4) { emptyList() },
      tableau = List(7) { col ->
        if (col == 0) TableauPile(
          faceDown = listOf(Card(Rank.Five, Suit.Hearts)),
          faceUp = listOf(Card(Rank.Ace, Suit.Spades)),
        ) else TableauPile.Empty
      },
    )
    assertFalse(state.canAutoComplete())
  }

  @Test
  fun `canAutoComplete is true when stock empty, no face-downs, and a foundation move exists`() {
    val state = GameState(
      stock = emptyList(),
      waste = listOf(Card(Rank.Ace, Suit.Spades), Card(Rank.Ace, Suit.Hearts)),
      foundations = List(4) { emptyList() },
      tableau = emptyTableau,
    )
    assertTrue(state.canAutoComplete())
  }

  @Test
  fun `canAutoComplete is false when only one playable card remains`() {
    val state = GameState(
      stock = emptyList(),
      waste = listOf(Card(Rank.Ace, Suit.Hearts)),
      foundations = List(4) { emptyList() },
      tableau = emptyTableau,
    )
    assertFalse(state.canAutoComplete())
  }

  @Test
  fun `nextAutoCompleteSource picks waste top when legal`() {
    val state = GameState(
      stock = emptyList(),
      waste = listOf(Card(Rank.Ace, Suit.Hearts)),
      foundations = List(4) { emptyList() },
      tableau = List(7) { col ->
        if (col == 0) TableauPile(faceDown = emptyList(), faceUp = listOf(Card(Rank.Ace, Suit.Clubs)))
        else TableauPile.Empty
      },
    )
    assertEquals(FoundationMoveSource.WasteTop, state.nextAutoCompleteSource())
  }

  @Test
  fun `nextAutoCompleteSource falls back to tableau when waste cannot move`() {
    val state = GameState(
      stock = emptyList(),
      waste = listOf(Card(Rank.Five, Suit.Hearts)),
      foundations = List(4) { emptyList() },
      tableau = List(7) { col ->
        if (col == 3) TableauPile(faceDown = emptyList(), faceUp = listOf(Card(Rank.Ace, Suit.Clubs)))
        else TableauPile.Empty
      },
    )
    assertEquals(FoundationMoveSource.TableauTop(3), state.nextAutoCompleteSource())
  }

  @Test
  fun `nextAutoCompleteSource is null when no foundation move exists`() {
    val state = GameState(
      stock = emptyList(),
      waste = listOf(Card(Rank.Five, Suit.Hearts)),
      foundations = List(4) { emptyList() },
      tableau = emptyTableau,
    )
    assertNull(state.nextAutoCompleteSource())
  }

  @Test
  fun `applying nextAutoCompleteSource repeatedly wins a solvable end-game`() {
    var state = GameState(
      stock = emptyList(),
      waste = emptyList(),
      foundations = List(4) { emptyList() },
      tableau = List(7) { col ->
        if (col < 4) {
          val suit = Suit.entries[col]
          TableauPile(faceDown = emptyList(), faceUp = Rank.entries.reversed().map { Card(it, suit) })
        } else TableauPile.Empty
      },
    )
    var steps = 0
    while (true) {
      val src = state.nextAutoCompleteSource() ?: break
      state = state.moveToFoundation(src) ?: error("expected legal move")
      steps++
      if (steps > 200) error("auto-complete loop did not terminate")
    }
    assertTrue(state.isWon())
  }
}
