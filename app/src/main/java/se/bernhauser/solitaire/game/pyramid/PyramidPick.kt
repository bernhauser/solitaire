package se.bernhauser.solitaire.game.pyramid

/** A selectable card: an uncovered pyramid slot or the top of the waste. */
sealed interface PyramidPick {
  data class Board(val index: Int) : PyramidPick
  data object Waste : PyramidPick
}
