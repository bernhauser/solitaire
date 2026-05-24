package se.bernhauser.solitaire.ui.board

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import se.bernhauser.solitaire.ui.cards.CardBackImage
import se.bernhauser.solitaire.ui.cards.CardFace

@Composable
fun StockAnimationOverlay(state: BoardDragState) {
  if (state.stockAnimMode == null) return
  val density = LocalDensity.current
  val cameraDistPx = with(density) { 12.dp.toPx() }

  state.stockAnimItems.forEach { item ->
    val widthDp = with(density) { item.cardSize.width.toDp() }
    val heightDp = with(density) { item.cardSize.height.toDp() }
    val rotation = (1f - item.flipProgress) * 180f
    val faceUp = item.flipProgress > 0.5f

    Box(
      modifier = Modifier
        .offset {
          IntOffset(
            x = item.pos.x.toInt(),
            y = item.pos.y.toInt(),
          )
        }
        .requiredSize(width = widthDp, height = heightDp)
        .graphicsLayer {
          rotationY = rotation
          cameraDistance = cameraDistPx
        },
    ) {
      if (faceUp) {
        CardFace(modifier = Modifier, card = item.card)
      } else {
        CardBackImage(
          modifier = Modifier.graphicsLayer { rotationY = 180f },
        )
      }
    }
  }
}
