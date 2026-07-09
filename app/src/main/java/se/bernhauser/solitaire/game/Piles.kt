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
