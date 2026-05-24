package se.bernhauser.solitaire.game

import kotlinx.serialization.Serializable

@Serializable
data class TableauPile(
  val faceDown: List<Card>,
  val faceUp: List<Card>,
) {
  companion object {
    val Empty: TableauPile = TableauPile(faceDown = emptyList(), faceUp = emptyList())
  }
}

@Serializable
data class GameState(
  val stock: List<Card>,
  val waste: List<Card>,
  val foundations: List<List<Card>>,
  val tableau: List<TableauPile>,
)

fun dealNewGame(seed: Long): GameState {
  val deck = shuffledDeck(seed)
  var offset = 0
  val tableau = (0..6).map { col ->
    val size = col + 1
    val cards = deck.subList(offset, offset + size)
    offset += size
    TableauPile(faceDown = cards.dropLast(1), faceUp = listOf(cards.last()))
  }
  return GameState(
    stock = deck.subList(offset, deck.size),
    waste = emptyList(),
    foundations = List(4) { emptyList() },
    tableau = tableau,
  )
}
