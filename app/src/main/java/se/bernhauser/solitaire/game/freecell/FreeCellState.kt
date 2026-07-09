package se.bernhauser.solitaire.game.freecell

import kotlinx.serialization.Serializable
import se.bernhauser.solitaire.game.Card
import se.bernhauser.solitaire.game.TableauPile
import se.bernhauser.solitaire.game.shuffledDeck

const val FreeCellCount: Int = 4
const val FreeCellColumnCount: Int = 8

@Serializable
data class FreeCellState(
  val cells: List<Card?>,
  val foundations: List<List<Card>>,
  val tableau: List<TableauPile>,
)

fun dealNewFreeCell(seed: Long): FreeCellState {
  val deck = shuffledDeck(seed)
  var offset = 0
  val tableau = List(FreeCellColumnCount) { col ->
    val size = if (col < 4) 7 else 6
    val cards = deck.subList(offset, offset + size)
    offset += size
    TableauPile(faceDown = emptyList(), faceUp = cards.toList())
  }
  return FreeCellState(
    cells = List(FreeCellCount) { null },
    foundations = List(4) { emptyList() },
    tableau = tableau,
  )
}
