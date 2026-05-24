package se.bernhauser.solitaire.ui.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import se.bernhauser.solitaire.game.Card
import se.bernhauser.solitaire.game.CardBack
import se.bernhauser.solitaire.game.Rank
import se.bernhauser.solitaire.game.Suit
import se.bernhauser.solitaire.ui.cards.CardAspectRatio
import se.bernhauser.solitaire.ui.cards.FlipCard
import se.bernhauser.solitaire.ui.cards.PlayingCard

val TableauCardOffset = 16.dp

@Composable
fun TableauColumn(
  modifier: Modifier = Modifier,
  faceDownCount: Int,
  faceUp: List<Card>,
  col: Int = -1,
  dragState: BoardDragState? = null,
  back: CardBack = CardBack.Red,
  revealedCount: Int = Int.MAX_VALUE,
  startIndex: Int = 0,
  onDrop: (DragSource, DropTarget?) -> DropResult? = { _, _ -> null },
) {
  val hideFromIndex = (dragState?.active?.source as? DragSource.TableauRun)
    ?.takeIf { it.column == col }
    ?.fromIndex

  var prevFaceDown by remember(col) { mutableIntStateOf(faceDownCount) }
  var flippingCard by remember(col) { mutableStateOf<Card?>(null) }
  val freshTop = faceUp.firstOrNull()
  LaunchedEffect(faceDownCount, freshTop) {
    if (faceDownCount < prevFaceDown && freshTop != null) {
      flippingCard = freshTop
    }
    prevFaceDown = faceDownCount
  }

  if (faceDownCount == 0 && faceUp.isEmpty()) {
    val emptyModifier = if (dragState != null && col >= 0) {
      modifier.anchor(dragState, Anchor.TableauTop(col))
    } else {
      modifier
    }
    EmptySlot(modifier = emptyModifier)
    return
  }

  Layout(
    modifier = modifier,
    content = {
      repeat(faceDownCount) { i ->
        PlayingCard(
          card = Placeholder,
          faceUp = false,
          back = back,
          revealed = revealedCount > startIndex + i,
        )
      }
      faceUp.forEachIndexed { i, card ->
        val hidden = hideFromIndex != null && i >= hideFromIndex
        val isTopFaceUp = i == faceUp.lastIndex
        val cardModifier = if (dragState != null && col >= 0) {
          Modifier
            .then(if (hidden) Modifier.alpha(0f) else Modifier)
            .then(if (isTopFaceUp) Modifier.anchor(dragState, Anchor.TableauTop(col)) else Modifier)
            .dragSource(
              state = dragState,
              source = DragSource.TableauRun(col, i),
              cards = faceUp.subList(i, faceUp.size),
              onDrop = onDrop,
            )
        } else {
          Modifier
        }
        if (i == 0 && card == flippingCard) {
          FlipCard(
            modifier = cardModifier,
            card = card,
            back = back,
            onComplete = { if (flippingCard == card) flippingCard = null },
          )
        } else {
          PlayingCard(
            modifier = cardModifier,
            card = card,
            revealed = revealedCount > startIndex + faceDownCount + i,
          )
        }
      }
    },
  ) { measurables, constraints ->
    val width = constraints.maxWidth
    val cardHeight = (width / CardAspectRatio).toInt()
    val faceDownStep = TableauCardOffset.toPx().toInt()
    val faceUpStep = TableauCardOffset.toPx().toInt()

    val cardConstraints = Constraints.fixed(width, cardHeight)
    val placeables = measurables.map { it.measure(cardConstraints) }

    val positions = IntArray(placeables.size)
    var y = 0
    placeables.forEachIndexed { index, _ ->
      positions[index] = y
      val step = if (index < faceDownCount) faceDownStep else faceUpStep
      y += step
    }
    val totalHeight = if (placeables.isEmpty()) 0
    else positions.last() + cardHeight

    layout(width, totalHeight) {
      placeables.forEachIndexed { index, placeable ->
        placeable.placeRelative(0, positions[index])
      }
    }
  }
}

private val Placeholder: Card = Card(Rank.Ace, Suit.Spades)
