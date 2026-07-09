package se.bernhauser.solitaire.game.pyramid

import kotlinx.serialization.Serializable
import se.bernhauser.solitaire.game.Card
import se.bernhauser.solitaire.game.shuffledDeck

/** Number of times the waste may be turned back into the stock (3 passes total). */
const val PyramidRedeals: Int = 2

@Serializable
data class PyramidState(
  /** Fixed 28-slot pyramid following [PyramidSlots]; null = removed. */
  val board: List<Card?>,
  val stock: List<Card>,
  val waste: List<Card>,
  val redealsLeft: Int,
)

fun dealNewPyramid(seed: Long): PyramidState {
  val deck = shuffledDeck(seed)
  return PyramidState(
    board = deck.subList(0, PyramidSlotCount),
    stock = deck.subList(PyramidSlotCount, deck.size),
    waste = emptyList(),
    redealsLeft = PyramidRedeals,
  )
}
