package se.bernhauser.solitaire.game.pyramid

import se.bernhauser.solitaire.game.Card
import se.bernhauser.solitaire.game.Rank

const val PyramidPairSum: Int = 13

/** A slot is uncovered once both overlapping cards in the row below are gone. */
fun PyramidState.isUncovered(index: Int): Boolean {
  if (board.getOrNull(index) == null) return false
  return PyramidSlots[index].children.all { board[it] == null }
}

fun PyramidState.isAvailable(pick: PyramidPick): Boolean = when (pick) {
  is PyramidPick.Board -> isUncovered(pick.index)
  PyramidPick.Waste -> waste.isNotEmpty()
}

fun PyramidState.cardAt(pick: PyramidPick): Card? = when (pick) {
  is PyramidPick.Board -> board.getOrNull(pick.index)
  PyramidPick.Waste -> waste.lastOrNull()
}

/** Kings count 13 on their own and are removed by a single tap. */
fun PyramidState.removeKing(pick: PyramidPick): PyramidState? {
  if (!isAvailable(pick)) return null
  if (cardAt(pick)?.rank != Rank.King) return null
  return remove(pick)
}

fun PyramidState.removePair(a: PyramidPick, b: PyramidPick): PyramidState? {
  if (a == b) return null
  if (!isAvailable(a) || !isAvailable(b)) return null
  val cardA = cardAt(a) ?: return null
  val cardB = cardAt(b) ?: return null
  if (cardA.rank.value + cardB.rank.value != PyramidPairSum) return null
  return remove(a).remove(b)
}

fun PyramidState.drawFromStock(): PyramidState? {
  val card = stock.lastOrNull() ?: return null
  return copy(
    stock = stock.dropLast(1),
    waste = waste + card,
  )
}

fun PyramidState.canRecycleWaste(): Boolean =
  stock.isEmpty() && waste.isNotEmpty() && redealsLeft > 0

fun PyramidState.recycleWaste(): PyramidState? {
  if (!canRecycleWaste()) return null
  return copy(
    stock = waste.asReversed(),
    waste = emptyList(),
    redealsLeft = redealsLeft - 1,
  )
}

/** The pyramid alone must be cleared; leftover stock or waste cards don't matter. */
fun PyramidState.isWon(): Boolean = board.all { it == null }

fun PyramidState.hasAnyMove(): Boolean {
  if (stock.isNotEmpty()) return true
  if (canRecycleWaste()) return true
  val available = buildList {
    board.indices.forEach { if (isUncovered(it)) add(board[it]!!) }
    waste.lastOrNull()?.let { add(it) }
  }
  if (available.any { it.rank == Rank.King }) return true
  for (i in available.indices) {
    for (j in i + 1 until available.size) {
      if (available[i].rank.value + available[j].rank.value == PyramidPairSum) return true
    }
  }
  return false
}

fun PyramidState.isStuck(): Boolean = !isWon() && !hasAnyMove()

private fun PyramidState.remove(pick: PyramidPick): PyramidState = when (pick) {
  is PyramidPick.Board ->
    copy(board = board.toMutableList().also { it[pick.index] = null })
  PyramidPick.Waste ->
    copy(waste = waste.dropLast(1))
}
