package se.bernhauser.solitaire.game

private const val DrawCount: Int = 3

fun GameState.drawFromStock(): GameState? {
  if (stock.isEmpty()) return null
  val take = minOf(DrawCount, stock.size)
  val drawn = stock.takeLast(take).asReversed()
  return copy(
    stock = stock.dropLast(take),
    waste = waste + drawn,
  )
}

fun GameState.recycleWaste(): GameState? {
  if (stock.isNotEmpty() || waste.isEmpty()) return null
  return copy(
    stock = waste.asReversed(),
    waste = emptyList(),
  )
}

fun GameState.moveToTableau(source: TableauMoveSource, destColumn: Int): GameState? {
  if (destColumn !in tableau.indices) return null
  val moving: List<Card> = when (source) {
    TableauMoveSource.WasteTop -> {
      val top = waste.lastOrNull() ?: return null
      listOf(top)
    }
    is TableauMoveSource.FoundationTop -> {
      val top = foundations[source.suit.ordinal].lastOrNull() ?: return null
      listOf(top)
    }
    is TableauMoveSource.TableauRun -> {
      if (source.column == destColumn) return null
      if (source.column !in tableau.indices) return null
      val srcUp = tableau[source.column].faceUp
      if (source.fromIndex !in srcUp.indices) return null
      val run = srcUp.subList(source.fromIndex, srcUp.size)
      if (!isValidRun(run)) return null
      run.toList()
    }
  }
  val dest = tableau[destColumn]
  if (!canPlaceOnTableau(moving.first(), dest)) return null

  val newDest = dest.copy(faceUp = dest.faceUp + moving)
  val newTableau = tableau.toMutableList()
  newTableau[destColumn] = newDest

  return when (source) {
    TableauMoveSource.WasteTop -> copy(
      waste = waste.dropLast(1),
      tableau = newTableau,
    )
    is TableauMoveSource.FoundationTop -> {
      val newFoundations = foundations.toMutableList()
      newFoundations[source.suit.ordinal] = newFoundations[source.suit.ordinal].dropLast(1)
      copy(
        foundations = newFoundations,
        tableau = newTableau,
      )
    }
    is TableauMoveSource.TableauRun -> {
      val srcPile = tableau[source.column]
      val remainingUp = srcPile.faceUp.subList(0, source.fromIndex)
      newTableau[source.column] = flipIfNeeded(srcPile.copy(faceUp = remainingUp.toList()))
      copy(tableau = newTableau)
    }
  }
}

fun GameState.moveToFoundation(source: FoundationMoveSource): GameState? {
  val card: Card = when (source) {
    FoundationMoveSource.WasteTop -> waste.lastOrNull() ?: return null
    is FoundationMoveSource.TableauTop -> {
      if (source.column !in tableau.indices) return null
      tableau[source.column].faceUp.lastOrNull() ?: return null
    }
  }
  val foundation = foundations[card.suit.ordinal]
  if (!canPlaceOnFoundation(card, foundation)) return null

  val newFoundations = foundations.toMutableList()
  newFoundations[card.suit.ordinal] = foundation + card

  return when (source) {
    FoundationMoveSource.WasteTop -> copy(
      waste = waste.dropLast(1),
      foundations = newFoundations,
    )
    is FoundationMoveSource.TableauTop -> {
      val srcPile = tableau[source.column]
      val newSrc = flipIfNeeded(srcPile.copy(faceUp = srcPile.faceUp.dropLast(1)))
      val newTableau = tableau.toMutableList()
      newTableau[source.column] = newSrc
      copy(tableau = newTableau, foundations = newFoundations)
    }
  }
}

fun GameState.isWon(): Boolean =
  foundations.size == 4 && foundations.all { it.size == Rank.entries.size }

fun GameState.hasAnyImmediateMove(): Boolean {
  if (waste.isNotEmpty()) {
    if (moveToFoundation(FoundationMoveSource.WasteTop) != null) return true
    for (c in tableau.indices) {
      if (moveToTableau(TableauMoveSource.WasteTop, c) != null) return true
    }
  }
  for (c in tableau.indices) {
    if (moveToFoundation(FoundationMoveSource.TableauTop(c)) != null) return true
  }
  for (src in tableau.indices) {
    val pile = tableau[src]
    for (from in pile.faceUp.indices) {
      for (dst in tableau.indices) {
        if (moveToTableau(TableauMoveSource.TableauRun(src, from), dst) == null) continue
        if (isUsefulRunMove(pile, from, tableau[dst])) return true
      }
    }
  }
  return false
}

private fun isUsefulRunMove(srcPile: TableauPile, from: Int, dstPile: TableauPile): Boolean {
  val flipsCard = from == 0 && srcPile.faceDown.isNotEmpty()
  if (flipsCard) return true
  val dstEmpty = dstPile.faceUp.isEmpty() && dstPile.faceDown.isEmpty()
  // Moving the entire face-up portion from a pile with no face-downs onto an empty pile
  // is a pure no-op (e.g. King shuffling between empty columns).
  val srcEmptiesIntoEmpty = from == 0 && srcPile.faceDown.isEmpty() && dstEmpty
  return !srcEmptiesIntoEmpty
}

fun GameState.canAutoComplete(): Boolean {
  if (isWon()) return false
  if (stock.isNotEmpty()) return false
  if (tableau.any { it.faceDown.isNotEmpty() }) return false
  val remaining = waste.size + tableau.sumOf { it.faceUp.size }
  if (remaining <= 1) return false
  return nextAutoCompleteSource() != null
}

fun GameState.nextAutoCompleteSource(): FoundationMoveSource? {
  if (waste.isNotEmpty() && moveToFoundation(FoundationMoveSource.WasteTop) != null) {
    return FoundationMoveSource.WasteTop
  }
  for (col in tableau.indices) {
    val src = FoundationMoveSource.TableauTop(col)
    if (moveToFoundation(src) != null) return src
  }
  return null
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

private fun canPlaceOnTableau(head: Card, dest: TableauPile): Boolean {
  if (dest.faceUp.isEmpty() && dest.faceDown.isEmpty()) {
    return head.rank == Rank.King
  }
  val top = dest.faceUp.lastOrNull() ?: return false
  return head.rank.value == top.rank.value - 1 && head.suit.color != top.suit.color
}

private fun flipIfNeeded(pile: TableauPile): TableauPile {
  if (pile.faceUp.isNotEmpty() || pile.faceDown.isEmpty()) return pile
  return TableauPile(
    faceDown = pile.faceDown.dropLast(1),
    faceUp = listOf(pile.faceDown.last()),
  )
}
