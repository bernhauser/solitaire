package se.bernhauser.solitaire.ui.tripeaks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import se.bernhauser.solitaire.game.Card
import se.bernhauser.solitaire.game.Rank
import se.bernhauser.solitaire.game.Suit
import se.bernhauser.solitaire.game.tripeaks.TriPeaksBaseWidthInCards
import se.bernhauser.solitaire.game.tripeaks.TriPeaksRowCount
import se.bernhauser.solitaire.game.tripeaks.TriPeaksSlots
import se.bernhauser.solitaire.game.tripeaks.TriPeaksState
import se.bernhauser.solitaire.game.tripeaks.isUncovered
import se.bernhauser.solitaire.game.tripeaks.play
import se.bernhauser.solitaire.ui.board.Anchor
import se.bernhauser.solitaire.ui.board.BoardDragState
import se.bernhauser.solitaire.ui.board.DragOverlay
import se.bernhauser.solitaire.ui.board.DragSource
import se.bernhauser.solitaire.ui.board.EmptySlot
import se.bernhauser.solitaire.ui.board.StockAnimMode
import se.bernhauser.solitaire.ui.board.StockAnimMove
import se.bernhauser.solitaire.ui.board.StockAnimationOverlay
import se.bernhauser.solitaire.ui.board.anchor
import se.bernhauser.solitaire.ui.board.rememberBoardDragState
import se.bernhauser.solitaire.ui.cards.CardAspectRatio
import se.bernhauser.solitaire.ui.cards.FlipCard
import se.bernhauser.solitaire.ui.cards.PlayingCard
import se.bernhauser.solitaire.ui.theme.FeltGreen

/** Vertical distance between pyramid rows as a fraction of card height. */
private const val PeaksRowStepFraction = 0.55f

@Composable
fun TriPeaksBoard(
  modifier: Modifier = Modifier,
  state: TriPeaksState,
  onPlay: (Int) -> Unit = {},
  onDrawTap: () -> Unit = {},
) {
  val dragState = rememberBoardDragState()
  val scope = rememberCoroutineScope()

  val onSlotTap: (Int) -> Unit = tap@{ index ->
    if (dragState.active != null || dragState.stockAnimMode != null) return@tap
    val card = state.board.getOrNull(index) ?: return@tap
    if (state.play(index) == null) return@tap
    val fromRect = dragState.anchorRect(Anchor.BoardSlotAt(index)) ?: return@tap
    val toRect = dragState.anchorRect(Anchor.WasteTop) ?: return@tap
    scope.launch {
      dragState.runTapMove(
        source = DragSource.BoardSlot(index),
        cards = listOf(card),
        fromRect = fromRect,
        toTopLeft = toRect.topLeft,
        applyMove = { onPlay(index) },
      )
    }
  }

  val onStockTap: () -> Unit = stock@{
    if (dragState.active != null || dragState.stockAnimMode != null) return@stock
    val card = state.stock.lastOrNull() ?: return@stock
    val stockRect = dragState.anchorRect(Anchor.Stock)
    val wasteRect = dragState.anchorRect(Anchor.WasteTop)
    if (stockRect == null || wasteRect == null) {
      onDrawTap()
      return@stock
    }
    val move = StockAnimMove(
      card = card,
      from = stockRect.topLeft,
      to = wasteRect.topLeft,
      flipFromFaceUp = false,
      flipToFaceUp = true,
    )
    val cardSize = IntSize(wasteRect.width.toInt(), wasteRect.height.toInt())
    scope.launch {
      dragState.runStockAnim(StockAnimMode.Draw, listOf(move), cardSize, onDrawTap)
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(FeltGreen)
      .onGloballyPositioned { dragState.boardCoords = it },
  ) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 12.dp)) {
      PeaksLayout(
        state = state,
        dragState = dragState,
        onSlotTap = onSlotTap,
      )
      Spacer(modifier = Modifier.height(32.dp))
      StockWasteRow(
        stockCount = state.stock.size,
        wasteTop = state.waste.lastOrNull(),
        dragState = dragState,
        onStockTap = onStockTap,
      )
    }
    DragOverlay(state = dragState)
    StockAnimationOverlay(state = dragState)
  }
}

@Composable
private fun PeaksLayout(
  modifier: Modifier = Modifier,
  state: TriPeaksState,
  dragState: BoardDragState,
  onSlotTap: (Int) -> Unit,
) {
  Layout(
    modifier = modifier.fillMaxWidth(),
    content = {
      TriPeaksSlots.forEachIndexed { index, _ ->
        PeakSlot(
          index = index,
          card = state.board[index],
          uncovered = state.isUncovered(index),
          dragState = dragState,
          onTap = { onSlotTap(index) },
        )
      }
    },
  ) { measurables, constraints ->
    val cardWidth = constraints.maxWidth / TriPeaksBaseWidthInCards
    val cardHeight = (cardWidth / CardAspectRatio).toInt()
    val step = (cardHeight * PeaksRowStepFraction).toInt()
    val cardConstraints = Constraints.fixed(cardWidth, cardHeight)
    val placeables = measurables.map { it.measure(cardConstraints) }
    val height = (TriPeaksRowCount - 1) * step + cardHeight
    layout(constraints.maxWidth, height) {
      // Index order places lower rows later, so they draw on top of the row above.
      placeables.forEachIndexed { index, placeable ->
        val slot = TriPeaksSlots[index]
        placeable.placeRelative(x = slot.xHalf * cardWidth / 2, y = slot.row * step)
      }
    }
  }
}

@Composable
private fun PeakSlot(
  modifier: Modifier = Modifier,
  index: Int,
  card: Card?,
  uncovered: Boolean,
  dragState: BoardDragState,
  onTap: () -> Unit,
) {
  if (card == null) {
    Box(modifier)
    return
  }
  var flipping by remember(index) { mutableStateOf(false) }
  var prevUncovered by remember(index) { mutableStateOf(uncovered) }
  LaunchedEffect(uncovered) {
    if (uncovered && !prevUncovered) flipping = true
    prevUncovered = uncovered
  }
  val hidden = (dragState.active?.source as? DragSource.BoardSlot)?.index == index
  val slotModifier = modifier
    .anchor(dragState, Anchor.BoardSlotAt(index))
    .then(if (hidden) Modifier.alpha(0f) else Modifier)
  val tappable = slotModifier.clickable(
    interactionSource = remember(index) { MutableInteractionSource() },
    indication = null,
  ) { onTap() }
  when {
    !uncovered -> PlayingCard(modifier = slotModifier, card = Placeholder, faceUp = false)
    flipping -> FlipCard(modifier = tappable, card = card, onComplete = { flipping = false })
    else -> PlayingCard(modifier = tappable, card = card)
  }
}

@Composable
private fun StockWasteRow(
  modifier: Modifier = Modifier,
  stockCount: Int,
  wasteTop: Card?,
  dragState: BoardDragState,
  onStockTap: () -> Unit,
) {
  Row(modifier = modifier.fillMaxWidth()) {
    repeat(3) { Spacer(modifier = Modifier.weight(1f)) }
    Box(modifier = Modifier.weight(1f).anchor(dragState, Anchor.Stock)) {
      if (stockCount == 0) {
        EmptySlot()
      } else {
        Box(
          modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
          ) { onStockTap() },
        ) {
          PlayingCard(card = Placeholder, faceUp = false)
          Surface(
            modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp),
            shape = RoundedCornerShape(50),
            color = Color.White,
          ) {
            Text(
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
              text = "$stockCount",
              color = FeltGreen,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }
    Spacer(modifier = Modifier.weight(1f))
    Box(modifier = Modifier.weight(1f).anchor(dragState, Anchor.WasteTop)) {
      if (wasteTop == null) {
        EmptySlot()
      } else {
        PlayingCard(card = wasteTop)
      }
    }
    repeat(4) { Spacer(modifier = Modifier.weight(1f)) }
  }
}

private val Placeholder: Card = Card(Rank.Ace, Suit.Spades)
