package se.bernhauser.solitaire.game

fun nearWinState(): GameState {
  val foundations = Suit.entries.map { suit ->
    Rank.entries.dropLast(1).map { Card(it, suit) }
  }
  val kingsOnTableau = Suit.entries.mapIndexed { col, suit ->
    TableauPile(faceDown = emptyList(), faceUp = listOf(Card(Rank.King, suit)))
  }
  val tableau = kingsOnTableau + List(7 - kingsOnTableau.size) { TableauPile.Empty }
  return GameState(
    stock = emptyList(),
    waste = emptyList(),
    foundations = foundations,
    tableau = tableau,
  )
}
