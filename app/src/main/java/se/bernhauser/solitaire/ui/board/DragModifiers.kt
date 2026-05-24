package se.bernhauser.solitaire.ui.board

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.launch
import se.bernhauser.solitaire.game.Card

fun Modifier.dropTarget(
  state: BoardDragState,
  target: DropTarget,
): Modifier = onGloballyPositioned { coords ->
  val board = state.boardCoords ?: return@onGloballyPositioned
  state.registerTarget(target, board.localBoundingBoxOf(coords))
}

fun Modifier.anchor(
  state: BoardDragState,
  anchor: Anchor,
): Modifier = onGloballyPositioned { coords ->
  val board = state.boardCoords ?: return@onGloballyPositioned
  state.registerAnchor(anchor, board.localBoundingBoxOf(coords))
}

fun Modifier.dragSource(
  state: BoardDragState,
  source: DragSource,
  cards: List<Card>,
  onDrop: (DragSource, DropTarget?) -> DropResult?,
): Modifier = composed {
  val scope = rememberCoroutineScope()
  var sourceCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
  this
    .onGloballyPositioned { sourceCoords = it }
    .pointerInput(source, cards) {
      detectDragGestures(
        onDragStart = { offsetInSource ->
          val board = state.boardCoords ?: return@detectDragGestures
          val src = sourceCoords ?: return@detectDragGestures
          val pointerInBoard = board.localPositionOf(src, offsetInSource)
          state.begin(
            ActiveDrag(
              source = source,
              cards = cards,
              anchor = offsetInSource,
              cardSize = src.size,
              pointer = pointerInBoard,
            )
          )
        },
        onDrag = { change, delta ->
          change.consume()
          state.updatePointer(delta)
        },
        onDragEnd = {
          val target = state.hitTestCurrent()
          val result = onDrop(source, target)
          if (result != null) {
            scope.launch {
              state.animateSettle(result.destinationTopLeft)
              result.applyMove()
              state.clear()
            }
          } else {
            scope.launch { state.animateSnapBack() }
          }
        },
        onDragCancel = {
          scope.launch { state.animateSnapBack() }
        },
      )
    }
}
