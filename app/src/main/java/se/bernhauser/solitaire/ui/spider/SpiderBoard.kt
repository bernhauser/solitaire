package se.bernhauser.solitaire.ui.spider

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import se.bernhauser.solitaire.game.Card
import se.bernhauser.solitaire.game.Rank
import se.bernhauser.solitaire.game.Suit
import se.bernhauser.solitaire.game.spider.SpiderDealSize
import se.bernhauser.solitaire.game.spider.SpiderRunCount
import se.bernhauser.solitaire.game.spider.SpiderState
import se.bernhauser.solitaire.game.spider.bestDestinationFor
import se.bernhauser.solitaire.game.spider.canDealFromStock
import se.bernhauser.solitaire.game.spider.moveRun
import se.bernhauser.solitaire.ui.board.Anchor
import se.bernhauser.solitaire.ui.board.BoardDragState
import se.bernhauser.solitaire.ui.board.DragOverlay
import se.bernhauser.solitaire.ui.board.DragSource
import se.bernhauser.solitaire.ui.board.DropResult
import se.bernhauser.solitaire.ui.board.DropTarget
import se.bernhauser.solitaire.ui.board.EmptySlot
import se.bernhauser.solitaire.ui.board.TableauColumn
import se.bernhauser.solitaire.ui.board.TableauFaceUpOverlapFraction
import se.bernhauser.solitaire.ui.board.dropTarget
import se.bernhauser.solitaire.ui.board.rememberBoardDragState
import se.bernhauser.solitaire.ui.cards.PlayingCard
import se.bernhauser.solitaire.ui.theme.FeltGreen

@Composable
fun SpiderBoard(
  modifier: Modifier = Modifier,
  state: SpiderState,
  onDealTap: () -> Unit = {},
  onMoveRun: (fromColumn: Int, fromIndex: Int, toColumn: Int) -> Unit = { _, _, _ -> },
) {
  val dragState = rememberBoardDragState()
  val scope = rememberCoroutineScope()

  val onDrop: (DragSource, DropTarget?) -> DropResult? = onDrop@{ source, target ->
    val run = source as? DragSource.TableauRun ?: return@onDrop null
    val dest = (target as? DropTarget.Tableau)?.column ?: return@onDrop null
    if (state.moveRun(run.column, run.fromIndex, dest) == null) return@onDrop null
    val settle = settleTopLeft(state, dest, dragState) ?: return@onDrop null
    DropResult(settle) { onMoveRun(run.column, run.fromIndex, dest) }
  }

  val onCardTap: (Int, Int) -> Unit = tap@{ col, index ->
    if (dragState.active != null) return@tap
    val dest = state.bestDestinationFor(col, index) ?: return@tap
    val pile = state.tableau[col]
    val cards = pile.faceUp.subList(index, pile.faceUp.size).toList()
    val topAnchor = dragState.anchorRect(Anchor.TableauTop(col)) ?: return@tap
    val stepPx = topAnchor.height * TableauFaceUpOverlapFraction
    val fromRect = topAnchor.translate(0f, -(pile.faceUp.lastIndex - index) * stepPx)
    val toTopLeft = settleTopLeft(state, dest, dragState) ?: return@tap
    scope.launch {
      dragState.runTapMove(
        source = DragSource.TableauRun(col, index),
        cards = cards,
        fromRect = fromRect,
        toTopLeft = toTopLeft,
        applyMove = { onMoveRun(col, index, dest) },
      )
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(FeltGreen)
      .onGloballyPositioned { dragState.boardCoords = it },
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      SpiderTopRow(
        completedRuns = state.completedRuns,
        dealsLeft = (state.stock.size + SpiderDealSize - 1) / SpiderDealSize,
        canDeal = state.canDealFromStock(),
        stockEmpty = state.stock.isEmpty(),
        onDealTap = onDealTap,
      )
      Row(
        modifier = Modifier.fillMaxWidth().weight(1f),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        state.tableau.forEachIndexed { col, pile ->
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .dropTarget(dragState, DropTarget.Tableau(col)),
          ) {
            TableauColumn(
              faceDownCount = pile.faceDown.size,
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
private fun SpiderTopRow(
  modifier: Modifier = Modifier,
  completedRuns: List<Suit>,
  dealsLeft: Int,
  canDeal: Boolean,
  stockEmpty: Boolean,
  onDealTap: () -> Unit,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    repeat(SpiderRunCount) { i ->
      val suit = completedRuns.getOrNull(i)
      if (suit == null) {
        EmptySlot(modifier = Modifier.weight(1f))
      } else {
        PlayingCard(modifier = Modifier.weight(1f), card = Card(Rank.King, suit))
      }
    }
    Spacer(modifier = Modifier.weight(1f))
    Box(modifier = Modifier.weight(1f)) {
      if (stockEmpty) {
        EmptySlot()
      } else {
        Box(
          modifier = Modifier
            .clickable(enabled = canDeal) { onDealTap() }
            .alpha(if (canDeal) 1f else 0.5f),
        ) {
          PlayingCard(card = StockPlaceholder, faceUp = false)
          Surface(
            modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp),
            shape = RoundedCornerShape(50),
            color = Color.White,
          ) {
            Text(
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
              text = "$dealsLeft",
              color = FeltGreen,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }
  }
}

private fun settleTopLeft(state: SpiderState, col: Int, dragState: BoardDragState): Offset? {
  val anchor = dragState.anchorRect(Anchor.TableauTop(col)) ?: return null
  val pile = state.tableau.getOrNull(col) ?: return null
  return if (pile.faceUp.isEmpty() && pile.faceDown.isEmpty()) {
    anchor.topLeft
  } else {
    Offset(anchor.left, anchor.top + anchor.height * TableauFaceUpOverlapFraction)
  }
}

private val StockPlaceholder: Card = Card(Rank.Ace, Suit.Spades)
