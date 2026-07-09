package se.bernhauser.solitaire.game.spider

import se.bernhauser.solitaire.game.Card
import se.bernhauser.solitaire.game.Rank
import se.bernhauser.solitaire.game.TableauPile

/** True when the face-up cards from [fromIndex] to the end form a same-suit run descending by one. */
fun SpiderState.isMovableRun(column: Int, fromIndex: Int): Boolean {
  val up = tableau.getOrNull(column)?.faceUp ?: return false
  if (fromIndex !in up.indices) return false
  for (i in fromIndex until up.size - 1) {
    val a = up[i]
    val b = up[i + 1]
    if (b.suit != a.suit || b.rank.value != a.rank.value - 1) return false
  }
  return true
}

/** Any run head may go on a card one rank higher regardless of suit, or on an empty pile. */
fun SpiderState.canPlaceOn(head: Card, destColumn: Int): Boolean {
  val dest = tableau.getOrNull(destColumn) ?: return false
  val top = dest.faceUp.lastOrNull() ?: return dest.faceDown.isEmpty()
  return head.rank.value == top.rank.value - 1
}

fun SpiderState.moveRun(fromColumn: Int, fromIndex: Int, toColumn: Int): SpiderState? {
  if (fromColumn == toColumn) return null
  if (fromColumn !in tableau.indices || toColumn !in tableau.indices) return null
  if (!isMovableRun(fromColumn, fromIndex)) return null
  val srcPile = tableau[fromColumn]
  val moving = srcPile.faceUp.subList(fromIndex, srcPile.faceUp.size).toList()
  if (!canPlaceOn(moving.first(), toColumn)) return null

  val newTableau = tableau.toMutableList()
  newTableau[fromColumn] = flipIfNeeded(srcPile.copy(faceUp = srcPile.faceUp.subList(0, fromIndex).toList()))
  newTableau[toColumn] = newTableau[toColumn].copy(faceUp = newTableau[toColumn].faceUp + moving)
  return copy(tableau = newTableau).clearCompletedRun(toColumn)
}

fun SpiderState.canDealFromStock(): Boolean =
  stock.isNotEmpty() && tableau.none { it.faceUp.isEmpty() && it.faceDown.isEmpty() }

fun SpiderState.dealFromStock(): SpiderState? {
  if (!canDealFromStock()) return null
  val count = minOf(SpiderDealSize, stock.size)
  val dealt = stock.takeLast(count).asReversed()
  var next = copy(
    stock = stock.dropLast(count),
    tableau = tableau.mapIndexed { col, pile ->
      val card = dealt.getOrNull(col) ?: return@mapIndexed pile
      pile.copy(faceUp = pile.faceUp + card)
    },
  )
  for (col in next.tableau.indices) {
    next = next.clearCompletedRun(col)
  }
  return next
}

fun SpiderState.isWon(): Boolean = completedRuns.size == SpiderRunCount

/**
 * Strict availability check: any legal run move or stock deal. Deliberately conservative —
 * a game is only declared dead when literally nothing can be played.
 */
fun SpiderState.hasAnyMove(): Boolean {
  if (canDealFromStock()) return true
  for (src in tableau.indices) {
    val up = tableau[src].faceUp
    for (from in up.indices) {
      if (!isMovableRun(src, from)) continue
      for (dst in tableau.indices) {
        if (dst == src) continue
        if (canPlaceOn(up[from], dst)) return true
      }
    }
  }
  return false
}

fun SpiderState.isStuck(): Boolean = !isWon() && !hasAnyMove()

/**
 * Destination for a tap-to-move assist: prefer continuing a same-suit sequence, then any
 * rank-above card, then an empty pile (unless the move would just relocate a whole pile).
 */
fun SpiderState.bestDestinationFor(column: Int, fromIndex: Int): Int? {
  if (!isMovableRun(column, fromIndex)) return null
  val srcPile = tableau[column]
  val head = srcPile.faceUp[fromIndex]
  var rankMatch: Int? = null
  var emptyPile: Int? = null
  for (dst in tableau.indices) {
    if (dst == column) continue
    val dstPile = tableau[dst]
    val top = dstPile.faceUp.lastOrNull()
    if (top == null) {
      if (dstPile.faceDown.isEmpty() && emptyPile == null) emptyPile = dst
    } else if (top.rank.value == head.rank.value + 1) {
      if (top.suit == head.suit) return dst
      if (rankMatch == null) rankMatch = dst
    }
  }
  if (rankMatch != null) return rankMatch
  // Moving an entire pile to another empty pile achieves nothing; skip that.
  val wholePile = fromIndex == 0 && srcPile.faceDown.isEmpty()
  return if (wholePile) null else emptyPile
}

/** First tap-assist move for a column, preferring the longest movable run. */
fun SpiderState.bestTapMove(column: Int): Pair<Int, Int>? {
  val up = tableau.getOrNull(column)?.faceUp ?: return null
  for (from in up.indices) {
    if (!isMovableRun(column, from)) continue
    val dest = bestDestinationFor(column, from) ?: continue
    return from to dest
  }
  return null
}

private fun SpiderState.clearCompletedRun(column: Int): SpiderState {
  val pile = tableau[column]
  val up = pile.faceUp
  if (up.size < Rank.entries.size) return this
  val tail = up.subList(up.size - Rank.entries.size, up.size)
  val suit = tail.first().suit
  val isFullRun = tail.first().rank == Rank.King &&
    tail.withIndex().all { (i, card) -> card.suit == suit && card.rank.value == Rank.King.value - i }
  if (!isFullRun) return this
  val newTableau = tableau.toMutableList()
  newTableau[column] = flipIfNeeded(pile.copy(faceUp = up.subList(0, up.size - Rank.entries.size).toList()))
  return copy(tableau = newTableau, completedRuns = completedRuns + suit)
}

private fun flipIfNeeded(pile: TableauPile): TableauPile {
  if (pile.faceUp.isNotEmpty() || pile.faceDown.isEmpty()) return pile
  return TableauPile(
    faceDown = pile.faceDown.dropLast(1),
    faceUp = listOf(pile.faceDown.last()),
  )
}
