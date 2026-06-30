package se.bernhauser.solitaire.game

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeadGameDetectionTest {

  /**
   * Exact board pulled off the device: stock empty, the only legal moves are
   * tableau run-to-run shuffles that flip no face-down card, empty no column
   * and feed no foundation, and the waste can never surface a playable card.
   * It is a dead game and must be reported as having no immediate move.
   */
  @Test
  fun `a board whose only moves are sterile run shuffles has no immediate move`() {
    assertFalse(deadGameState().hasAnyImmediateMove())
  }

  /** A run move that uncovers a face-down card is real progress and still counts. */
  @Test
  fun `a run move that flips a face-down card counts as a move`() {
    val state = GameState(
      stock = emptyList(),
      waste = emptyList(),
      foundations = List(4) { emptyList() },
      tableau = listOf(
        TableauPile(faceDown = listOf(Card(Rank.Ace, Suit.Clubs)), faceUp = listOf(Card(Rank.Six, Suit.Hearts))),
        TableauPile(faceDown = emptyList(), faceUp = listOf(Card(Rank.Seven, Suit.Spades))),
        TableauPile.Empty, TableauPile.Empty, TableauPile.Empty, TableauPile.Empty, TableauPile.Empty,
      ),
    )
    assertTrue(state.hasAnyImmediateMove())
  }

  /** A run move that empties a column (freeing it for a King) still counts. */
  @Test
  fun `a run move that empties a column counts as a move`() {
    val state = GameState(
      stock = emptyList(),
      waste = emptyList(),
      foundations = List(4) { emptyList() },
      tableau = listOf(
        TableauPile(faceDown = emptyList(), faceUp = listOf(Card(Rank.Queen, Suit.Hearts))),
        TableauPile(faceDown = emptyList(), faceUp = listOf(Card(Rank.King, Suit.Spades))),
        TableauPile.Empty, TableauPile.Empty, TableauPile.Empty, TableauPile.Empty, TableauPile.Empty,
      ),
    )
    assertTrue(state.hasAnyImmediateMove())
  }
}

private fun deadGameState(): GameState = GameState(
  stock = emptyList(),
  waste = listOf(Card(Rank.Four, Suit.Spades), Card(Rank.Four, Suit.Clubs), Card(Rank.Eight, Suit.Clubs), Card(Rank.King, Suit.Clubs), Card(Rank.Seven, Suit.Diamonds), Card(Rank.Six, Suit.Spades), Card(Rank.Five, Suit.Diamonds), Card(Rank.Ace, Suit.Clubs), Card(Rank.Seven, Suit.Clubs), Card(Rank.Nine, Suit.Hearts)),
  foundations = listOf(
    emptyList(),
    listOf(Card(Rank.Ace, Suit.Diamonds), Card(Rank.Two, Suit.Diamonds)),
    listOf(Card(Rank.Ace, Suit.Hearts), Card(Rank.Two, Suit.Hearts), Card(Rank.Three, Suit.Hearts)),
    listOf(Card(Rank.Ace, Suit.Spades), Card(Rank.Two, Suit.Spades)),
  ),
  tableau = listOf(
    TableauPile(faceDown = emptyList(), faceUp = listOf(Card(Rank.King, Suit.Diamonds), Card(Rank.Queen, Suit.Clubs), Card(Rank.Jack, Suit.Diamonds), Card(Rank.Ten, Suit.Clubs), Card(Rank.Nine, Suit.Diamonds), Card(Rank.Eight, Suit.Spades), Card(Rank.Seven, Suit.Hearts), Card(Rank.Six, Suit.Clubs), Card(Rank.Five, Suit.Hearts))),
    TableauPile(faceDown = emptyList(), faceUp = listOf(Card(Rank.Three, Suit.Diamonds), Card(Rank.Two, Suit.Clubs))),
    TableauPile(faceDown = emptyList(), faceUp = listOf(Card(Rank.King, Suit.Spades), Card(Rank.Queen, Suit.Hearts), Card(Rank.Jack, Suit.Clubs), Card(Rank.Ten, Suit.Hearts), Card(Rank.Nine, Suit.Spades), Card(Rank.Eight, Suit.Diamonds), Card(Rank.Seven, Suit.Spades), Card(Rank.Six, Suit.Hearts))),
    TableauPile(faceDown = listOf(Card(Rank.Ten, Suit.Diamonds), Card(Rank.Queen, Suit.Diamonds)), faceUp = listOf(Card(Rank.Six, Suit.Diamonds), Card(Rank.Five, Suit.Spades), Card(Rank.Four, Suit.Hearts), Card(Rank.Three, Suit.Clubs))),
    TableauPile(faceDown = listOf(Card(Rank.Eight, Suit.Hearts), Card(Rank.Ten, Suit.Spades), Card(Rank.Five, Suit.Clubs)), faceUp = listOf(Card(Rank.Nine, Suit.Clubs))),
    TableauPile(faceDown = listOf(Card(Rank.Three, Suit.Spades)), faceUp = listOf(Card(Rank.King, Suit.Hearts), Card(Rank.Queen, Suit.Spades), Card(Rank.Jack, Suit.Hearts))),
    TableauPile(faceDown = listOf(Card(Rank.Four, Suit.Diamonds)), faceUp = listOf(Card(Rank.Jack, Suit.Spades))),
  ),
)
