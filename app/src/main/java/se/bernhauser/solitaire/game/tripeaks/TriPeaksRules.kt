package se.bernhauser.solitaire.game.tripeaks

import kotlin.math.abs
import se.bernhauser.solitaire.game.Rank

/** A slot is uncovered once both overlapping cards in the row below are gone. */
fun TriPeaksState.isUncovered(index: Int): Boolean {
  if (board.getOrNull(index) == null) return false
  return TriPeaksSlots[index].children.all { board[it] == null }
}

/** Playable when uncovered and one rank above or below the waste top, wrapping King↔Ace. */
fun TriPeaksState.canPlay(index: Int): Boolean {
  val card = board.getOrNull(index) ?: return false
  if (!isUncovered(index)) return false
  val top = waste.lastOrNull() ?: return false
  val diff = abs(card.rank.value - top.rank.value)
  return diff == 1 || diff == Rank.entries.size - 1
}

fun TriPeaksState.play(index: Int): TriPeaksState? {
  if (!canPlay(index)) return null
  val card = board[index] ?: return null
  return copy(
    board = board.toMutableList().also { it[index] = null },
    waste = waste + card,
  )
}

fun TriPeaksState.drawFromStock(): TriPeaksState? {
  val card = stock.lastOrNull() ?: return null
  return copy(
    stock = stock.dropLast(1),
    waste = waste + card,
  )
}

fun TriPeaksState.isWon(): Boolean = board.all { it == null }

fun TriPeaksState.hasAnyMove(): Boolean =
  stock.isNotEmpty() || board.indices.any { canPlay(it) }

fun TriPeaksState.isStuck(): Boolean = !isWon() && !hasAnyMove()
