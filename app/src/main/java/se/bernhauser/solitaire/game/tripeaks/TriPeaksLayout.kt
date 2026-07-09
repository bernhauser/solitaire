package se.bernhauser.solitaire.game.tripeaks

import kotlin.math.abs

const val TriPeaksSlotCount: Int = 28
const val TriPeaksRowCount: Int = 4

/** Base row is 10 cards wide; horizontal positions are measured in half-card widths. */
const val TriPeaksBaseWidthInCards: Int = 10

/**
 * One board position. [xHalf] is the left edge in half-card widths (base row spans 0..18).
 * [children] are the two slots in the row below that must be cleared before this one
 * is uncovered; empty for the base row.
 */
data class TriPeaksSlot(val row: Int, val xHalf: Int, val children: List<Int>)

val TriPeaksSlots: List<TriPeaksSlot> = run {
  val positions = buildList {
    listOf(3, 9, 15).forEach { add(0 to it) }
    listOf(2, 4, 8, 10, 14, 16).forEach { add(1 to it) }
    repeat(9) { add(2 to (1 + 2 * it)) }
    repeat(10) { add(3 to (2 * it)) }
  }
  positions.map { (row, x) ->
    val children = positions.withIndex()
      .filter { (_, pos) -> pos.first == row + 1 && abs(pos.second - x) == 1 }
      .map { it.index }
    TriPeaksSlot(row = row, xHalf = x, children = children)
  }
}
