package se.bernhauser.solitaire.game.klondike

import kotlinx.serialization.Serializable
import se.bernhauser.solitaire.game.Card
import se.bernhauser.solitaire.game.TableauPile
import se.bernhauser.solitaire.game.shuffledDeck

@Serializable
data class KlondikeState(
  val stock: List<Card>,
  val waste: List<Card>,
  val foundations: List<List<Card>>,
  val tableau: List<TableauPile>,
)

fun dealNewGame(seed: Long): KlondikeState {
  val deck = shuffledDeck(seed)
  var offset = 0
  val tableau = (0..6).map { col ->
    val size = col + 1
    val cards = deck.subList(offset, offset + size)
    offset += size
    TableauPile(faceDown = cards.dropLast(1), faceUp = listOf(cards.last()))
  }
  return KlondikeState(
    stock = deck.subList(offset, deck.size),
    waste = emptyList(),
    foundations = List(4) { emptyList() },
    tableau = tableau,
  )
}
