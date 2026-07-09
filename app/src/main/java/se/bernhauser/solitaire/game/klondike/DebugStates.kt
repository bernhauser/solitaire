package se.bernhauser.solitaire.game.klondike

import se.bernhauser.solitaire.game.Card
import se.bernhauser.solitaire.game.Rank
import se.bernhauser.solitaire.game.Suit
import se.bernhauser.solitaire.game.TableauPile

fun nearWinState(): KlondikeState {
  val foundations = Suit.entries.map { suit ->
    Rank.entries.dropLast(1).map { Card(it, suit) }
  }
  val kingsOnTableau = Suit.entries.mapIndexed { col, suit ->
    TableauPile(faceDown = emptyList(), faceUp = listOf(Card(Rank.King, suit)))
  }
  val tableau = kingsOnTableau + List(7 - kingsOnTableau.size) { TableauPile.Empty }
  return KlondikeState(
    stock = emptyList(),
    waste = emptyList(),
    foundations = foundations,
    tableau = tableau,
  )
}

/**
 * Reproduction of a reported mid-game board where certain cards seemed
 * impossible to move. The face-up cards mirror the screenshot exactly; the
 * face-down piles and the stock are filled with the remaining cards so the
 * deck stays a valid, duplicate-free 52.
 *
 * Face-up board (exposed = last in each faceUp list):
 *   Foundations: Hearts -> 3, Spades -> 2, Clubs & Diamonds empty
 *   Waste (top last): Q-Hearts, 5-Diamonds, K-Spades
 *   Tableau:
 *     1: K-D Q-C J-H 10-S 9-D 8-S    (exposed 8-S)
 *     2: 7-H
 *     3: [2 down] 3-D
 *     4: [2 down] Q-S
 *     5: 7-C 6-D 5-S 4-H 3-C 2-D     (exposed 2-D)
 *     6: 6-H
 *     7: [6 down] 9-S
 *
 * NOTE: the two long runs (cols 1 & 5) are encoded as legal alternating-colour
 * runs, the only shape the engine can normally build. If the real screenshot
 * shows same-colour runs, that illegal build is itself the bug — adjust the
 * suits here to match.
 */
fun stuckState(): KlondikeState {
  val foundations = listOf(
    emptyList(), // Clubs
    emptyList(), // Diamonds
    listOf( // Hearts
      Card(Rank.Ace, Suit.Hearts),
      Card(Rank.Two, Suit.Hearts),
      Card(Rank.Three, Suit.Hearts),
    ),
    listOf( // Spades
      Card(Rank.Ace, Suit.Spades),
      Card(Rank.Two, Suit.Spades),
    ),
  )
  val waste = listOf(
    Card(Rank.Queen, Suit.Hearts),
    Card(Rank.Five, Suit.Diamonds),
    Card(Rank.King, Suit.Spades),
  )
  val tableau = listOf(
    TableauPile(
      faceDown = emptyList(),
      faceUp = listOf(
        Card(Rank.King, Suit.Diamonds),
        Card(Rank.Queen, Suit.Clubs),
        Card(Rank.Jack, Suit.Hearts),
        Card(Rank.Ten, Suit.Spades),
        Card(Rank.Nine, Suit.Diamonds),
        Card(Rank.Eight, Suit.Spades),
      ),
    ),
    TableauPile(faceDown = emptyList(), faceUp = listOf(Card(Rank.Seven, Suit.Hearts))),
    TableauPile(
      faceDown = listOf(Card(Rank.Ace, Suit.Clubs), Card(Rank.Two, Suit.Clubs)),
      faceUp = listOf(Card(Rank.Three, Suit.Diamonds)),
    ),
    TableauPile(
      faceDown = listOf(Card(Rank.Four, Suit.Clubs), Card(Rank.Five, Suit.Clubs)),
      faceUp = listOf(Card(Rank.Queen, Suit.Spades)),
    ),
    TableauPile(
      faceDown = emptyList(),
      faceUp = listOf(
        Card(Rank.Seven, Suit.Clubs),
        Card(Rank.Six, Suit.Diamonds),
        Card(Rank.Five, Suit.Spades),
        Card(Rank.Four, Suit.Hearts),
        Card(Rank.Three, Suit.Clubs),
        Card(Rank.Two, Suit.Diamonds),
      ),
    ),
    TableauPile(faceDown = emptyList(), faceUp = listOf(Card(Rank.Six, Suit.Hearts))),
    TableauPile(
      faceDown = listOf(
        Card(Rank.Six, Suit.Clubs),
        Card(Rank.Eight, Suit.Clubs),
        Card(Rank.Nine, Suit.Clubs),
        Card(Rank.Ten, Suit.Clubs),
        Card(Rank.Jack, Suit.Clubs),
        Card(Rank.King, Suit.Clubs),
      ),
      faceUp = listOf(Card(Rank.Nine, Suit.Spades)),
    ),
  )
  val stock = listOf(
    Card(Rank.Five, Suit.Hearts),
    Card(Rank.Eight, Suit.Hearts),
    Card(Rank.Nine, Suit.Hearts),
    Card(Rank.Ten, Suit.Hearts),
    Card(Rank.King, Suit.Hearts),
    Card(Rank.Three, Suit.Spades),
    Card(Rank.Four, Suit.Spades),
    Card(Rank.Six, Suit.Spades),
    Card(Rank.Seven, Suit.Spades),
    Card(Rank.Jack, Suit.Spades),
    Card(Rank.Ace, Suit.Diamonds),
    Card(Rank.Four, Suit.Diamonds),
    Card(Rank.Seven, Suit.Diamonds),
    Card(Rank.Eight, Suit.Diamonds),
    Card(Rank.Ten, Suit.Diamonds),
    Card(Rank.Jack, Suit.Diamonds),
    Card(Rank.Queen, Suit.Diamonds),
  )
  return KlondikeState(
    stock = stock,
    waste = waste,
    foundations = foundations,
    tableau = tableau,
  )
}
