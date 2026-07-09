package se.bernhauser.solitaire.game.freecell

import se.bernhauser.solitaire.game.Card
import se.bernhauser.solitaire.game.Rank
import se.bernhauser.solitaire.game.TableauPile

/**
 * Largest run that may move in one gesture: (empty cells + 1) doubled once per empty
 * column. The destination column is excluded — moving onto an empty column can't use
 * that column as a waypoint.
 */
fun FreeCellState.maxMovableRun(destColumn: Int): Int {
  val freeCells = cells.count { it == null }
  val emptyColumns = tableau.indices.count { it != destColumn && tableau[it].faceUp.isEmpty() }
  return (freeCells + 1) shl emptyColumns
}

fun FreeCellState.moveToTableau(source: FreeCellTableauSource, destColumn: Int): FreeCellState? {
  if (destColumn !in tableau.indices) return null
  val dest = tableau[destColumn]
  return when (source) {
    is FreeCellTableauSource.Cell -> {
      val card = cells.getOrNull(source.index) ?: return null
      if (!canPlaceOnTableau(card, dest)) return null
      val newCells = cells.toMutableList().also { it[source.index] = null }
      val newTableau = tableau.toMutableList()
      newTableau[destColumn] = dest.copy(faceUp = dest.faceUp + card)
      copy(cells = newCells, tableau = newTableau)
    }
    is FreeCellTableauSource.TableauRun -> {
      if (source.column == destColumn) return null
      if (source.column !in tableau.indices) return null
      val srcUp = tableau[source.column].faceUp
      if (source.fromIndex !in srcUp.indices) return null
      val run = srcUp.subList(source.fromIndex, srcUp.size)
      if (!isValidRun(run)) return null
      if (run.size > maxMovableRun(destColumn)) return null
      if (!canPlaceOnTableau(run.first(), dest)) return null
      val newTableau = tableau.toMutableList()
      newTableau[source.column] =
        tableau[source.column].copy(faceUp = srcUp.subList(0, source.fromIndex).toList())
      newTableau[destColumn] = dest.copy(faceUp = dest.faceUp + run.toList())
      copy(tableau = newTableau)
    }
  }
}

fun FreeCellState.moveToFoundation(source: FreeCellCardSource): FreeCellState? {
  val card = cardAt(source) ?: return null
  val foundation = foundations[card.suit.ordinal]
  if (!canPlaceOnFoundation(card, foundation)) return null
  val newFoundations = foundations.toMutableList()
  newFoundations[card.suit.ordinal] = foundation + card
  return removeCard(source).copy(foundations = newFoundations)
}

fun FreeCellState.moveToCell(source: FreeCellCardSource, cellIndex: Int): FreeCellState? {
  if (cellIndex !in cells.indices || cells[cellIndex] != null) return null
  if (source is FreeCellCardSource.Cell && source.index == cellIndex) return null
  val card = cardAt(source) ?: return null
  val removed = removeCard(source)
  return removed.copy(cells = removed.cells.toMutableList().also { it[cellIndex] = card })
}

fun FreeCellState.firstEmptyCell(): Int? = cells.indexOfFirst { it == null }.takeIf { it >= 0 }

fun FreeCellState.isWon(): Boolean = foundations.all { it.size == Rank.entries.size }

/**
 * Strict availability check, like Spider's: the game is only dead when literally nothing
 * can be played. Single-card checks are exhaustive — with no empty cell and no empty
 * column the supermove capacity is 1, so run moves can't exist where no single-card
 * tableau move does.
 */
fun FreeCellState.hasAnyMove(): Boolean {
  if (cells.any { it == null } && tableau.any { it.faceUp.isNotEmpty() }) return true
  for (card in cells) {
    if (card == null) continue
    if (canPlaceOnFoundation(card, foundations[card.suit.ordinal])) return true
    if (tableau.any { canPlaceOnTableau(card, it) }) return true
  }
  for (src in tableau.indices) {
    val top = tableau[src].faceUp.lastOrNull() ?: continue
    if (canPlaceOnFoundation(top, foundations[top.suit.ordinal])) return true
    for (dst in tableau.indices) {
      if (dst == src) continue
      if (canPlaceOnTableau(top, tableau[dst])) return true
    }
  }
  return false
}

fun FreeCellState.isStuck(): Boolean = !isWon() && !hasAnyMove()

/**
 * Finishing is mechanical once every column is ordered by descending rank: the lowest
 * unplayed card of any suit is then always exposed, so foundation moves alone win.
 */
fun FreeCellState.canAutoComplete(): Boolean {
  if (isWon()) return false
  if (tableau.any { !isDescendingByRank(it.faceUp) }) return false
  val remaining = cells.count { it != null } + tableau.sumOf { it.faceUp.size }
  if (remaining <= 1) return false
  return nextAutoCompleteSource() != null
}

fun FreeCellState.nextAutoCompleteSource(): FreeCellCardSource? {
  for (i in cells.indices) {
    val card = cells[i] ?: continue
    if (canPlaceOnFoundation(card, foundations[card.suit.ordinal])) {
      return FreeCellCardSource.Cell(i)
    }
  }
  for (col in tableau.indices) {
    val top = tableau[col].faceUp.lastOrNull() ?: continue
    if (canPlaceOnFoundation(top, foundations[top.suit.ordinal])) {
      return FreeCellCardSource.TableauTop(col)
    }
  }
  return null
}

private fun FreeCellState.cardAt(source: FreeCellCardSource): Card? = when (source) {
  is FreeCellCardSource.Cell -> cells.getOrNull(source.index)
  is FreeCellCardSource.TableauTop -> tableau.getOrNull(source.column)?.faceUp?.lastOrNull()
}

private fun FreeCellState.removeCard(source: FreeCellCardSource): FreeCellState = when (source) {
  is FreeCellCardSource.Cell ->
    copy(cells = cells.toMutableList().also { it[source.index] = null })
  is FreeCellCardSource.TableauTop -> {
    val pile = tableau[source.column]
    val newTableau = tableau.toMutableList()
    newTableau[source.column] = pile.copy(faceUp = pile.faceUp.dropLast(1))
    copy(tableau = newTableau)
  }
}

private fun canPlaceOnTableau(head: Card, dest: TableauPile): Boolean {
  val top = dest.faceUp.lastOrNull() ?: return true
  return head.rank.value == top.rank.value - 1 && head.suit.color != top.suit.color
}

private fun canPlaceOnFoundation(card: Card, foundation: List<Card>): Boolean {
  val top = foundation.lastOrNull() ?: return card.rank == Rank.Ace
  return top.suit == card.suit && card.rank.value == top.rank.value + 1
}

private fun isValidRun(cards: List<Card>): Boolean {
  for (i in 0 until cards.size - 1) {
    val a = cards[i]
    val b = cards[i + 1]
    if (b.rank.value != a.rank.value - 1) return false
    if (b.suit.color == a.suit.color) return false
  }
  return true
}

private fun isDescendingByRank(cards: List<Card>): Boolean {
  for (i in 0 until cards.size - 1) {
    if (cards[i + 1].rank.value > cards[i].rank.value) return false
  }
  return true
}
