package se.bernhauser.solitaire.game.tripeaks

import kotlinx.serialization.Serializable
import se.bernhauser.solitaire.game.Card
import se.bernhauser.solitaire.game.shuffledDeck

@Serializable
data class TriPeaksState(
  /** Fixed 28-slot board following [TriPeaksSlots]; null = cleared. */
  val board: List<Card?>,
  val stock: List<Card>,
  val waste: List<Card>,
)

fun dealNewTriPeaks(seed: Long): TriPeaksState {
  val deck = shuffledDeck(seed)
  return TriPeaksState(
    board = deck.subList(0, TriPeaksSlotCount),
    stock = deck.subList(TriPeaksSlotCount, deck.size - 1),
    waste = listOf(deck.last()),
  )
}
