package se.bernhauser.solitaire.ui.board

import se.bernhauser.solitaire.game.GameState

internal data class StartupRevealPlan(
  val coveredTotal: Int,
  val faceUpTotal: Int,
  val stockCoveredIndex: Int?,
  val tableauCoveredIndices: List<IntArray>,
  val foundationFaceUpCount: Int,
  val visibleWasteCount: Int,
  val tableauFaceUpStart: Int,
  val tableauFaceUpOffsets: List<IntArray>,
) {
  fun tableauCoveredIndex(column: Int, row: Int): Int? =
    tableauCoveredIndices.getOrNull(column)?.let { if (row in it.indices) it[row] else null }

  fun foundationFaceUpIndex(displayedIndex: Int): Int? =
    if (displayedIndex in 0 until foundationFaceUpCount) displayedIndex else null

  fun wasteFaceUpIndex(visibleIndex: Int): Int? =
    if (visibleIndex in 0 until visibleWasteCount) foundationFaceUpCount + visibleIndex else null

  fun tableauFaceUpIndex(column: Int, row: Int): Int? =
    tableauFaceUpOffsets.getOrNull(column)
      ?.let { if (row in it.indices) tableauFaceUpStart + it[row] else null }
}

internal fun buildStartupRevealPlan(state: GameState): StartupRevealPlan {
  var nextCovered = 0
  val stockCoveredIndex = if (state.stock.isNotEmpty()) nextCovered++ else null
  val tableauCoveredIndices = List(state.tableau.size) { col ->
    IntArray(state.tableau[col].faceDown.size) { -1 }
  }
  val maxFaceDown = state.tableau.maxOfOrNull { it.faceDown.size } ?: 0
  repeat(maxFaceDown) { row ->
    state.tableau.indices.forEach { col ->
      val indices = tableauCoveredIndices[col]
      if (row in indices.indices) {
        indices[row] = nextCovered++
      }
    }
  }

  val foundationFaceUpCount = state.foundations.count { it.isNotEmpty() }
  val visibleWasteCount = state.waste.takeLast(WasteVisibleCount).size
  val tableauFaceUpStart = foundationFaceUpCount + visibleWasteCount
  val tableauFaceUpOffsets = List(state.tableau.size) { col ->
    IntArray(state.tableau[col].faceUp.size) { -1 }
  }
  var nextTableauFaceUp = 0
  val maxFaceUp = state.tableau.maxOfOrNull { it.faceUp.size } ?: 0
  repeat(maxFaceUp) { row ->
    state.tableau.indices.forEach { col ->
      val offsets = tableauFaceUpOffsets[col]
      if (row in offsets.indices) {
        offsets[row] = nextTableauFaceUp++
      }
    }
  }

  return StartupRevealPlan(
    coveredTotal = nextCovered,
    faceUpTotal = tableauFaceUpStart + nextTableauFaceUp,
    stockCoveredIndex = stockCoveredIndex,
    tableauCoveredIndices = tableauCoveredIndices,
    foundationFaceUpCount = foundationFaceUpCount,
    visibleWasteCount = visibleWasteCount,
    tableauFaceUpStart = tableauFaceUpStart,
    tableauFaceUpOffsets = tableauFaceUpOffsets,
  )
}
