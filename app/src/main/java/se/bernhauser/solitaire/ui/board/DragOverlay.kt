package se.bernhauser.solitaire.ui.board

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import se.bernhauser.solitaire.ui.cards.PlayingCard

@Composable
fun DragOverlay(state: BoardDragState) {
  val active = state.active ?: return
  val density = LocalDensity.current
  val topLeft = active.pointer - active.anchor
  val stepPx = with(density) { TableauCardOffset.toPx() }
  val widthDp = with(density) { active.cardSize.width.toDp() }
  val heightDp = with(density) { active.cardSize.height.toDp() }

  active.cards.forEachIndexed { i, card ->
    Box(
      modifier = Modifier
        .offset {
          IntOffset(
            x = topLeft.x.toInt(),
            y = (topLeft.y + i * stepPx).toInt(),
          )
        }
        .requiredSize(width = widthDp, height = heightDp),
    ) {
      PlayingCard(card = card)
    }
  }
}
