package se.bernhauser.solitaire.game.pyramid

const val PyramidSlotCount: Int = 28
const val PyramidRowCount: Int = 7

/** Base row is 7 cards wide; horizontal positions are measured in half-card widths. */
const val PyramidBaseWidthInCards: Int = 7

/**
 * One pyramid position. Row r holds r + 1 cards; [xHalf] is the left edge in half-card
 * widths. [children] are the two overlapping slots in the row below that must be cleared
 * before this one is uncovered; empty for the base row.
 */
data class PyramidSlot(val row: Int, val xHalf: Int, val children: List<Int>)

val PyramidSlots: List<PyramidSlot> = buildList {
  var rowStart = 0
  for (row in 0 until PyramidRowCount) {
    val nextRowStart = rowStart + row + 1
    repeat(row + 1) { k ->
      val children = if (row == PyramidRowCount - 1) {
        emptyList()
      } else {
        listOf(nextRowStart + k, nextRowStart + k + 1)
      }
      add(PyramidSlot(row = row, xHalf = (PyramidRowCount - 1 - row) + 2 * k, children = children))
    }
    rowStart = nextRowStart
  }
}
