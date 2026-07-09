package se.bernhauser.solitaire.ui.freecell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import se.bernhauser.solitaire.game.Card
import se.bernhauser.solitaire.game.Suit
import se.bernhauser.solitaire.game.freecell.FreeCellCardSource
import se.bernhauser.solitaire.game.freecell.FreeCellState
import se.bernhauser.solitaire.game.freecell.FreeCellTableauSource
import se.bernhauser.solitaire.game.freecell.firstEmptyCell
import se.bernhauser.solitaire.game.freecell.moveToCell
import se.bernhauser.solitaire.game.freecell.moveToFoundation
import se.bernhauser.solitaire.game.freecell.moveToTableau
import se.bernhauser.solitaire.game.freecell.nextAutoCompleteSource
import se.bernhauser.solitaire.ui.board.Anchor
import se.bernhauser.solitaire.ui.board.BoardDragState
import se.bernhauser.solitaire.ui.board.DragOverlay
import se.bernhauser.solitaire.ui.board.DragSource
import se.bernhauser.solitaire.ui.board.DropResult
import se.bernhauser.solitaire.ui.board.DropTarget
import se.bernhauser.solitaire.ui.board.EmptySlot
import se.bernhauser.solitaire.ui.board.TableauColumn
import se.bernhauser.solitaire.ui.board.TableauFaceUpOverlapFraction
import se.bernhauser.solitaire.ui.board.TapMoveDurationMs
import se.bernhauser.solitaire.ui.board.anchor
import se.bernhauser.solitaire.ui.board.dragSource
import se.bernhauser.solitaire.ui.board.dropTarget
import se.bernhauser.solitaire.ui.board.rememberBoardDragState
import se.bernhauser.solitaire.ui.cards.PlayingCard
import se.bernhauser.solitaire.ui.theme.FeltGreen

private const val AutoCompleteMoveMs: Int = 120

@Composable
fun FreeCellBoard(
  modifier: Modifier = Modifier,
  state: FreeCellState,
  onMoveToTableau: (FreeCellTableauSource, Int) -> Unit = { _, _ -> },
  onMoveToFoundation: (FreeCellCardSource) -> Unit = {},
  onMoveToCell: (FreeCellCardSource, Int) -> Unit = { _, _ -> },
  autoComplete: Boolean = false,
  onAutoCompleteDone: () -> Unit = {},
) {
  val dragState = rememberBoardDragState()
  val scope = rememberCoroutineScope()

  val onDrop: (DragSource, DropTarget?) -> DropResult? = { source, target ->
    handleDrop(state, source, target, dragState, onMoveToTableau, onMoveToFoundation, onMoveToCell)
  }

  suspend fun animateTableauTopToFoundation(
    snapshot: FreeCellState,
    col: Int,
    durationMs: Int,
  ): Boolean {
    val pile = snapshot.tableau.getOrNull(col) ?: return false
    val card = pile.faceUp.lastOrNull() ?: return false
    if (snapshot.moveToFoundation(FreeCellCardSource.TableauTop(col)) == null) return false
    val fromRect = dragState.anchorRect(Anchor.TableauTop(col)) ?: return false
    val toRect = dragState.anchorRect(Anchor.FoundationDisplayedAt(card.suit.ordinal)) ?: return false
    dragState.runTapMove(
      source = DragSource.TableauRun(col, pile.faceUp.lastIndex),
      cards = listOf(card),
      fromRect = fromRect,
      toTopLeft = toRect.topLeft,
      applyMove = { onMoveToFoundation(FreeCellCardSource.TableauTop(col)) },
      durationMs = durationMs,
    )
    return true
  }

  suspend fun animateCellToFoundation(snapshot: FreeCellState, index: Int, durationMs: Int): Boolean {
    val card = snapshot.cells.getOrNull(index) ?: return false
    if (snapshot.moveToFoundation(FreeCellCardSource.Cell(index)) == null) return false
    val fromRect = dragState.anchorRect(Anchor.FreeCellAt(index)) ?: return false
    val toRect = dragState.anchorRect(Anchor.FoundationDisplayedAt(card.suit.ordinal)) ?: return false
    dragState.runTapMove(
      source = DragSource.FreeCell(index),
      cards = listOf(card),
      fromRect = fromRect,
      toTopLeft = toRect.topLeft,
      applyMove = { onMoveToFoundation(FreeCellCardSource.Cell(index)) },
      durationMs = durationMs,
    )
    return true
  }

  suspend fun animateTableauTopToCell(snapshot: FreeCellState, col: Int): Boolean {
    val pile = snapshot.tableau.getOrNull(col) ?: return false
    val card = pile.faceUp.lastOrNull() ?: return false
    val cellIndex = snapshot.firstEmptyCell() ?: return false
    if (snapshot.moveToCell(FreeCellCardSource.TableauTop(col), cellIndex) == null) return false
    val fromRect = dragState.anchorRect(Anchor.TableauTop(col)) ?: return false
    val toRect = dragState.anchorRect(Anchor.FreeCellAt(cellIndex)) ?: return false
    dragState.runTapMove(
      source = DragSource.TableauRun(col, pile.faceUp.lastIndex),
      cards = listOf(card),
      fromRect = fromRect,
      toTopLeft = toRect.topLeft,
      applyMove = { onMoveToCell(FreeCellCardSource.TableauTop(col), cellIndex) },
      durationMs = TapMoveDurationMs,
    )
    return true
  }

  val onCardTap: (Int, Int) -> Unit = tap@{ col, index ->
    if (dragState.active != null) return@tap
    if (index != state.tableau[col].faceUp.lastIndex) return@tap
    scope.launch {
      if (!animateTableauTopToFoundation(state, col, TapMoveDurationMs)) {
        animateTableauTopToCell(state, col)
      }
    }
  }

  val onCellTap: (Int) -> Unit = tap@{ index ->
    if (dragState.active != null) return@tap
    scope.launch { animateCellToFoundation(state, index, TapMoveDurationMs) }
  }

  val currentState = rememberUpdatedState(state)
  LaunchedEffect(autoComplete) {
    if (!autoComplete) return@LaunchedEffect
    while (true) {
      val snapshot = currentState.value
      val next = snapshot.nextAutoCompleteSource() ?: break
      val animated = when (next) {
        is FreeCellCardSource.Cell ->
          animateCellToFoundation(snapshot, next.index, AutoCompleteMoveMs)
        is FreeCellCardSource.TableauTop ->
          animateTableauTopToFoundation(snapshot, next.column, AutoCompleteMoveMs)
      }
      if (!animated) break
      snapshotFlow { currentState.value }.filter { it !== snapshot }.first()
    }
    onAutoCompleteDone()
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(FeltGreen)
      .onGloballyPositioned { dragState.boardCoords = it },
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      TopRow(
        state = state,
        dragState = dragState,
        onCellTap = onCellTap,
        onDrop = onDrop,
      )
      Row(
        modifier = Modifier.fillMaxWidth().weight(1f),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        state.tableau.forEachIndexed { col, pile ->
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .dropTarget(dragState, DropTarget.Tableau(col)),
          ) {
            TableauColumn(
              faceDownCount = 0,
              faceUp = pile.faceUp,
              col = col,
              dragState = dragState,
              onDrop = onDrop,
              onCardTap = { index -> onCardTap(col, index) },
            )
          }
        }
      }
    }
    DragOverlay(state = dragState)
  }
}

@Composable
private fun TopRow(
  modifier: Modifier = Modifier,
  state: FreeCellState,
  dragState: BoardDragState,
  onCellTap: (Int) -> Unit,
  onDrop: (DragSource, DropTarget?) -> DropResult?,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Row(
      modifier = Modifier.weight(4f),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      state.cells.forEachIndexed { i, card ->
        CellSlot(
          modifier = Modifier.weight(1f),
          card = card,
          index = i,
          dragState = dragState,
          onTap = { onCellTap(i) },
          onDrop = onDrop,
        )
      }
    }
    Row(
      modifier = Modifier
        .weight(4f)
        .dropTarget(dragState, DropTarget.Foundation),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Suit.entries.forEach { suit ->
        val top = state.foundations[suit.ordinal].lastOrNull()
        Box(
          modifier = Modifier
            .weight(1f)
            .anchor(dragState, Anchor.FoundationDisplayedAt(suit.ordinal)),
        ) {
          if (top == null) {
            EmptySlot()
          } else {
            PlayingCard(card = top)
          }
        }
      }
    }
  }
}

@Composable
private fun CellSlot(
  modifier: Modifier = Modifier,
  card: Card?,
  index: Int,
  dragState: BoardDragState,
  onTap: () -> Unit,
  onDrop: (DragSource, DropTarget?) -> DropResult?,
) {
  Box(
    modifier = modifier
      .dropTarget(dragState, DropTarget.FreeCell(index))
      .anchor(dragState, Anchor.FreeCellAt(index)),
  ) {
    if (card == null) {
      EmptySlot()
    } else {
      // The card stays composed (alpha 0) while dragged so the gesture node survives.
      val dragging = dragState.active?.source == DragSource.FreeCell(index)
      if (dragging) {
        EmptySlot()
      }
      PlayingCard(
        modifier = Modifier
          .then(if (dragging) Modifier.alpha(0f) else Modifier)
          .clickable(
            interactionSource = remember(index) { MutableInteractionSource() },
            indication = null,
          ) { onTap() }
          .dragSource(
            state = dragState,
            source = DragSource.FreeCell(index),
            cards = listOf(card),
            onDrop = onDrop,
          ),
        card = card,
      )
    }
  }
}

private fun handleDrop(
  state: FreeCellState,
  source: DragSource,
  target: DropTarget?,
  dragState: BoardDragState,
  onMoveToTableau: (FreeCellTableauSource, Int) -> Unit,
  onMoveToFoundation: (FreeCellCardSource) -> Unit,
  onMoveToCell: (FreeCellCardSource, Int) -> Unit,
): DropResult? = when (target) {
  null -> null
  is DropTarget.Tableau -> {
    val move = source.asTableauSource()
    if (move == null || state.moveToTableau(move, target.column) == null) null
    else {
      val dest = tableauSettleTopLeft(state, target.column, dragState)
      if (dest == null) null
      else DropResult(dest) { onMoveToTableau(move, target.column) }
    }
  }
  DropTarget.Foundation -> {
    val move = source.asCardSource(state)
    val card = source.movedCard(state)
    if (move == null || card == null || state.moveToFoundation(move) == null) null
    else {
      val dest = dragState.anchorRect(Anchor.FoundationDisplayedAt(card.suit.ordinal))?.topLeft
      if (dest == null) null
      else DropResult(dest) { onMoveToFoundation(move) }
    }
  }
  is DropTarget.FreeCell -> {
    val move = source.asCardSource(state)
    if (move == null || state.moveToCell(move, target.index) == null) null
    else {
      val dest = dragState.anchorRect(Anchor.FreeCellAt(target.index))?.topLeft
      if (dest == null) null
      else DropResult(dest) { onMoveToCell(move, target.index) }
    }
  }
}

private fun tableauSettleTopLeft(
  state: FreeCellState,
  col: Int,
  dragState: BoardDragState,
): Offset? {
  val anchor = dragState.anchorRect(Anchor.TableauTop(col)) ?: return null
  val pile = state.tableau.getOrNull(col) ?: return null
  return if (pile.faceUp.isEmpty()) {
    anchor.topLeft
  } else {
    Offset(anchor.left, anchor.top + anchor.height * TableauFaceUpOverlapFraction)
  }
}

private fun DragSource.movedCard(state: FreeCellState): Card? = when (this) {
  is DragSource.FreeCell -> state.cells.getOrNull(index)
  is DragSource.TableauRun -> state.tableau.getOrNull(column)?.faceUp?.getOrNull(fromIndex)
  else -> null
}

private fun DragSource.asTableauSource(): FreeCellTableauSource? = when (this) {
  is DragSource.FreeCell -> FreeCellTableauSource.Cell(index)
  is DragSource.TableauRun -> FreeCellTableauSource.TableauRun(column, fromIndex)
  else -> null
}

private fun DragSource.asCardSource(state: FreeCellState): FreeCellCardSource? = when (this) {
  is DragSource.FreeCell -> FreeCellCardSource.Cell(index)
  is DragSource.TableauRun -> {
    if (fromIndex == state.tableau.getOrNull(column)?.faceUp?.lastIndex) {
      FreeCellCardSource.TableauTop(column)
    } else {
      null
    }
  }
  else -> null
}
