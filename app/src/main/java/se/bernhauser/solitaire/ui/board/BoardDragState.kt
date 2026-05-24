package se.bernhauser.solitaire.ui.board

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import se.bernhauser.solitaire.game.Card
import se.bernhauser.solitaire.game.Suit

private const val SnapBackDurationMs: Int = 220
internal const val TapMoveDurationMs: Int = 240
private const val SettleDurationMs: Int = 160
internal const val StockMoveDurationMs: Int = 220
internal const val StockStaggerMs: Int = 70

class BoardDragState {
  var active: ActiveDrag? by mutableStateOf(null)
    private set

  var boardCoords: LayoutCoordinates? by mutableStateOf(null)

  val stockAnimItems: SnapshotStateList<StockAnimItem> = mutableStateListOf()
  var stockAnimMode: StockAnimMode? by mutableStateOf(null)
    private set

  private var restPointer: Offset = Offset.Zero

  private val targets = mutableStateMapOf<DropTarget, Rect>()
  private val anchors = mutableStateMapOf<Anchor, Rect>()

  fun begin(drag: ActiveDrag) {
    active = drag
    restPointer = drag.pointer
  }

  fun updatePointer(delta: Offset) {
    val current = active ?: return
    active = current.copy(pointer = current.pointer + delta)
  }

  fun hitTestCurrent(): DropTarget? {
    val current = active ?: return null
    val topLeft = current.pointer - current.anchor
    val dragRect = Rect(
      topLeft.x,
      topLeft.y,
      topLeft.x + current.cardSize.width,
      topLeft.y + current.cardSize.height,
    )
    var best: DropTarget? = null
    var bestArea = 0f
    for ((target, bounds) in targets) {
      val ix = maxOf(bounds.left, dragRect.left)
      val iy = maxOf(bounds.top, dragRect.top)
      val ir = minOf(bounds.right, dragRect.right)
      val ib = minOf(bounds.bottom, dragRect.bottom)
      if (ir <= ix || ib <= iy) continue
      val area = (ir - ix) * (ib - iy)
      if (area > bestArea) {
        bestArea = area
        best = target
      }
    }
    return best
  }

  fun clear() {
    active = null
  }

  suspend fun animateSnapBack() {
    val start = active?.pointer ?: return
    val target = restPointer
    val anim = Animatable(start, Offset.VectorConverter)
    anim.animateTo(target, animationSpec = tween(SnapBackDurationMs)) {
      active = active?.copy(pointer = value)
    }
    active = null
  }

  suspend fun animateSettle(targetTopLeft: Offset) {
    val current = active ?: return
    val anchor = current.anchor
    val startTopLeft = current.pointer - anchor
    val anim = Animatable(startTopLeft, Offset.VectorConverter)
    anim.animateTo(targetTopLeft, animationSpec = tween(SettleDurationMs)) {
      active = active?.copy(pointer = value + anchor)
    }
  }

  fun registerTarget(target: DropTarget, bounds: Rect) {
    targets[target] = bounds
  }

  fun unregisterTarget(target: DropTarget) {
    targets.remove(target)
  }

  fun registerAnchor(anchor: Anchor, bounds: Rect) {
    anchors[anchor] = bounds
  }

  fun unregisterAnchor(anchor: Anchor) {
    anchors.remove(anchor)
  }

  fun anchorRect(anchor: Anchor): Rect? = anchors[anchor]

  suspend fun runStockAnim(
    mode: StockAnimMode,
    moves: List<StockAnimMove>,
    cardSize: IntSize,
    applyMove: () -> Unit,
  ) {
    stockAnimItems.clear()
    moves.forEach { spec ->
      stockAnimItems.add(
        StockAnimItem(
          card = spec.card,
          pos = spec.from,
          cardSize = cardSize,
          flipProgress = if (spec.flipFromFaceUp) 1f else 0f,
        )
      )
    }
    stockAnimMode = mode
    coroutineScope {
      moves.forEachIndexed { idx, spec ->
        launch {
          delay(idx * StockStaggerMs.toLong())
          val flipFrom = if (spec.flipFromFaceUp) 1f else 0f
          val flipTo = if (spec.flipToFaceUp) 1f else 0f
          val anim = Animatable(0f)
          anim.animateTo(1f, animationSpec = tween(StockMoveDurationMs)) {
            val t = value
            val pos = Offset(
              x = spec.from.x + (spec.to.x - spec.from.x) * t,
              y = spec.from.y + (spec.to.y - spec.from.y) * t,
            )
            val flipProgress = flipFrom + (flipTo - flipFrom) * t
            if (idx < stockAnimItems.size) {
              stockAnimItems[idx] = stockAnimItems[idx].copy(
                pos = pos,
                flipProgress = flipProgress,
              )
            }
          }
        }
      }
    }
    applyMove()
    stockAnimItems.clear()
    stockAnimMode = null
  }

  suspend fun runTapMove(
    source: DragSource,
    cards: List<Card>,
    fromRect: Rect,
    toTopLeft: Offset,
    applyMove: () -> Unit,
    durationMs: Int = TapMoveDurationMs,
  ) {
    active = ActiveDrag(
      source = source,
      cards = cards,
      anchor = Offset.Zero,
      cardSize = IntSize(fromRect.width.toInt(), fromRect.height.toInt()),
      pointer = fromRect.topLeft,
    )
    val anim = Animatable(fromRect.topLeft, Offset.VectorConverter)
    anim.animateTo(toTopLeft, animationSpec = tween(durationMs)) {
      active = active?.copy(pointer = value)
    }
    applyMove()
    active = null
  }
}

data class ActiveDrag(
  val source: DragSource,
  val cards: List<Card>,
  val anchor: Offset,
  val cardSize: IntSize,
  val pointer: Offset,
)

sealed interface DragSource {
  data object Waste : DragSource
  data class Foundation(val suit: Suit) : DragSource
  data class TableauRun(val column: Int, val fromIndex: Int) : DragSource
}

sealed interface DropTarget {
  data class Tableau(val column: Int) : DropTarget
  data object Foundation : DropTarget
}

data class DropResult(val destinationTopLeft: Offset, val applyMove: () -> Unit)

sealed interface Anchor {
  data object WasteTop : Anchor
  data object Stock : Anchor
  data class TableauTop(val column: Int) : Anchor
  data class FoundationDisplayedAt(val index: Int) : Anchor
}

enum class StockAnimMode { Draw, Recycle }

data class StockAnimMove(
  val card: Card,
  val from: Offset,
  val to: Offset,
  val flipFromFaceUp: Boolean,
  val flipToFaceUp: Boolean,
)

data class StockAnimItem(
  val card: Card,
  val pos: Offset,
  val cardSize: IntSize,
  val flipProgress: Float,
)

@Composable
fun rememberBoardDragState(): BoardDragState = remember { BoardDragState() }
