package se.bernhauser.solitaire.ui.pyramid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import se.bernhauser.solitaire.game.pyramid.PyramidBaseWidthInCards
import se.bernhauser.solitaire.game.pyramid.PyramidPick
import se.bernhauser.solitaire.game.pyramid.PyramidRowCount
import se.bernhauser.solitaire.game.pyramid.PyramidSlots
import se.bernhauser.solitaire.game.pyramid.PyramidState
import se.bernhauser.solitaire.game.pyramid.canRecycleWaste
import se.bernhauser.solitaire.game.pyramid.isUncovered
import se.bernhauser.solitaire.ui.board.Anchor
import se.bernhauser.solitaire.ui.board.BoardDragState
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
private const val PyramidRowStepFraction = 0.55f

private val SelectionShape = RoundedCornerShape(8.dp)
private val SelectionColor = Color(0xFFFFC107)

@Composable
fun PyramidBoard(
  modifier: Modifier = Modifier,
  state: PyramidState,
  selected: PyramidPick? = null,
  onPick: (PyramidPick) -> Unit = {},
  onStockTap: () -> Unit = {},
) {
  val dragState = rememberBoardDragState()
  val scope = rememberCoroutineScope()

  val animatedStockTap: () -> Unit = stock@{
    if (dragState.stockAnimMode != null) return@stock
    val card = state.stock.lastOrNull()
    if (card == null) {
      // Empty stock: the tap recycles the waste (when allowed) without animation.
      onStockTap()
      return@stock
    }
    val stockRect = dragState.anchorRect(Anchor.Stock)
    val wasteRect = dragState.anchorRect(Anchor.WasteTop)
    if (stockRect == null || wasteRect == null) {
      onStockTap()
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
      dragState.runStockAnim(StockAnimMode.Draw, listOf(move), cardSize, onStockTap)
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(FeltGreen)
      .onGloballyPositioned { dragState.boardCoords = it },
  ) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 12.dp)) {
      PyramidRows(
        state = state,
        selected = selected,
        onPick = onPick,
      )
      Spacer(modifier = Modifier.height(32.dp))
      StockWasteRow(
        state = state,
        selected = selected,
        dragState = dragState,
        onStockTap = animatedStockTap,
        onWasteTap = { onPick(PyramidPick.Waste) },
      )
    }
    StockAnimationOverlay(state = dragState)
  }
}

@Composable
private fun PyramidRows(
  modifier: Modifier = Modifier,
  state: PyramidState,
  selected: PyramidPick?,
  onPick: (PyramidPick) -> Unit,
) {
  Layout(
    modifier = modifier.fillMaxWidth(),
    content = {
      PyramidSlots.forEachIndexed { index, _ ->
        PyramidSlotCard(
          index = index,
          card = state.board[index],
          uncovered = state.isUncovered(index),
          selected = selected == PyramidPick.Board(index),
          onTap = { onPick(PyramidPick.Board(index)) },
        )
      }
    },
  ) { measurables, constraints ->
    val cardWidth = constraints.maxWidth / PyramidBaseWidthInCards
    val cardHeight = (cardWidth / CardAspectRatio).toInt()
    val step = (cardHeight * PyramidRowStepFraction).toInt()
    val cardConstraints = Constraints.fixed(cardWidth, cardHeight)
    val placeables = measurables.map { it.measure(cardConstraints) }
    val height = (PyramidRowCount - 1) * step + cardHeight
    layout(constraints.maxWidth, height) {
      // Index order places lower rows later, so they draw on top of the row above.
      placeables.forEachIndexed { index, placeable ->
        val slot = PyramidSlots[index]
        placeable.placeRelative(x = slot.xHalf * cardWidth / 2, y = slot.row * step)
      }
    }
  }
}

@Composable
private fun PyramidSlotCard(
  modifier: Modifier = Modifier,
  index: Int,
  card: Card?,
  uncovered: Boolean,
  selected: Boolean,
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
  val slotModifier = modifier
    .then(if (selected) Modifier.border(3.dp, SelectionColor, SelectionShape) else Modifier)
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
  state: PyramidState,
  selected: PyramidPick?,
  dragState: BoardDragState,
  onStockTap: () -> Unit,
  onWasteTap: () -> Unit,
) {
  Row(modifier = modifier.fillMaxWidth()) {
    repeat(2) { Spacer(modifier = Modifier.weight(1f)) }
    Box(modifier = Modifier.weight(1f).anchor(dragState, Anchor.Stock)) {
      StockPile(
        stockCount = state.stock.size,
        canRecycle = state.canRecycleWaste(),
        redealsLeft = state.redealsLeft,
        onTap = onStockTap,
      )
    }
    Spacer(modifier = Modifier.weight(1f))
    Box(modifier = Modifier.weight(1f).anchor(dragState, Anchor.WasteTop)) {
      val top = state.waste.lastOrNull()
      if (top == null) {
        EmptySlot()
      } else {
        PlayingCard(
          modifier = Modifier
            .then(
              if (selected == PyramidPick.Waste) {
                Modifier.border(3.dp, SelectionColor, SelectionShape)
              } else {
                Modifier
              }
            )
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = null,
            ) { onWasteTap() },
          card = top,
        )
      }
    }
    repeat(2) { Spacer(modifier = Modifier.weight(1f)) }
  }
}

@Composable
private fun StockPile(
  modifier: Modifier = Modifier,
  stockCount: Int,
  canRecycle: Boolean,
  redealsLeft: Int,
  onTap: () -> Unit,
) {
  when {
    stockCount > 0 -> Box(
      modifier = modifier.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
      ) { onTap() },
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
    canRecycle -> Box(
      modifier = modifier.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
      ) { onTap() },
    ) {
      EmptySlot()
      Text(
        modifier = Modifier.align(Alignment.Center),
        text = "↻ $redealsLeft",
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
      )
    }
    else -> EmptySlot(modifier = modifier)
  }
}

private val Placeholder: Card = Card(Rank.Ace, Suit.Spades)
