package se.bernhauser.solitaire.game.freecell

/** A single-card source: a free cell or the top card of a tableau column. */
sealed interface FreeCellCardSource {
  data class Cell(val index: Int) : FreeCellCardSource
  data class TableauTop(val column: Int) : FreeCellCardSource
}

/** Sources for tableau placement, which additionally allows multi-card runs. */
sealed interface FreeCellTableauSource {
  data class Cell(val index: Int) : FreeCellTableauSource
  data class TableauRun(val column: Int, val fromIndex: Int) : FreeCellTableauSource
}
