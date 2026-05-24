package se.bernhauser.solitaire.ui.board

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import se.bernhauser.solitaire.game.Card
import se.bernhauser.solitaire.game.Rank
import se.bernhauser.solitaire.game.Suit
import se.bernhauser.solitaire.ui.cards.CardAspectRatio
import se.bernhauser.solitaire.ui.cards.PlayingCard

internal const val WasteFanOffsetFraction = 0.35f
internal const val WasteVisibleCount = 3

@Composable
fun WastePile(
  modifier: Modifier = Modifier,
  cards: List<Card>,
  dragState: BoardDragState? = null,
  revealedCount: Int = Int.MAX_VALUE,
  startIndex: Int = 0,
  onDrop: (DragSource, DropTarget?) -> DropResult? = { _, _ -> null },
) {
  if (cards.isEmpty()) {
    val emptyModifier = if (dragState != null) modifier.anchor(dragState, Anchor.WasteTop) else modifier
    EmptySlot(modifier = emptyModifier)
    return
  }

  val dragging = dragState?.active?.source == DragSource.Waste
  val hideForRecycle = dragState?.stockAnimMode == StockAnimMode.Recycle
  val visible = cards.takeLast(WasteVisibleCount)

  Layout(
    modifier = modifier,
    content = {
      val topIndex = visible.lastIndex
      visible.forEachIndexed { i, card ->
        val isTop = i == topIndex
        val cardModifier = when {
          isTop && dragState != null -> Modifier
            .then(if (dragging || hideForRecycle) Modifier.alpha(0f) else Modifier)
            .anchor(dragState, Anchor.WasteTop)
            .dragSource(
              state = dragState,
              source = DragSource.Waste,
              cards = listOf(card),
              onDrop = onDrop,
            )
          hideForRecycle -> Modifier.alpha(0f)
          else -> Modifier
        }
        PlayingCard(
          modifier = cardModifier,
          card = card,
          revealed = revealedCount > startIndex + i,
        )
      }
    },
  ) { measurables, constraints ->
    val slotWidth = constraints.maxWidth
    val cardHeight = (slotWidth / CardAspectRatio).toInt()
    val cardConstraints = Constraints.fixed(slotWidth, cardHeight)
    val placeables = measurables.map { it.measure(cardConstraints) }
    val step = (slotWidth * WasteFanOffsetFraction).toInt()

    layout(slotWidth, cardHeight) {
      placeables.forEachIndexed { index, placeable ->
        val offsetFromTop = placeables.size - 1 - index
        placeable.placeRelative(x = -offsetFromTop * step, y = 0)
      }
    }
  }
}

@Composable
fun StockBox(
  modifier: Modifier = Modifier,
  empty: Boolean,
  dragState: BoardDragState? = null,
  revealedCount: Int = Int.MAX_VALUE,
  startIndex: Int = 0,
) {
  val anchored = if (dragState != null) modifier.anchor(dragState, Anchor.Stock) else modifier
  if (empty) {
    EmptySlot(modifier = anchored)
  } else {
    PlayingCard(
      modifier = anchored,
      card = StockTopPlaceholder,
      faceUp = false,
      revealed = revealedCount > startIndex,
    )
  }
}

private val StockTopPlaceholder: Card = Card(Rank.Ace, Suit.Spades)
