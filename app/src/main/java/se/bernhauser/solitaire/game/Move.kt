package se.bernhauser.solitaire.game

sealed interface TableauMoveSource {
  data object WasteTop : TableauMoveSource
  data class FoundationTop(val suit: Suit) : TableauMoveSource
  data class TableauRun(val column: Int, val fromIndex: Int) : TableauMoveSource
}

sealed interface FoundationMoveSource {
  data object WasteTop : FoundationMoveSource
  data class TableauTop(val column: Int) : FoundationMoveSource
}
