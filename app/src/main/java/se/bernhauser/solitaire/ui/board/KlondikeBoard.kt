package se.bernhauser.solitaire.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import se.bernhauser.solitaire.game.FoundationMoveSource
import se.bernhauser.solitaire.game.GameState
import se.bernhauser.solitaire.game.Suit
import se.bernhauser.solitaire.game.TableauMoveSource
import se.bernhauser.solitaire.game.moveToFoundation
import se.bernhauser.solitaire.game.moveToTableau
import se.bernhauser.solitaire.game.nextAutoCompleteSource
import se.bernhauser.solitaire.ui.cards.PlayingCard
import se.bernhauser.solitaire.ui.theme.FeltGreen

private const val AutoCompleteMoveMs: Int = 120

private data class StartupRevealUiState(
  val coveredVisibleCount: Int,
  val activeCoveredIndex: Int?,
  val showFaceUpCards: Boolean,
  val faceUpVisibleCount: Int,
  val activeFaceUpIndex: Int?,
)

@Composable
fun KlondikeBoard(
  modifier: Modifier = Modifier,
  state: GameState,
  dealId: Int = 0,
  animateReveal: Boolean = true,
  canRecycle: Boolean = true,
  onStockTap: () -> Unit = {},
  onWasteTap: () -> Unit = {},
  onTableauTopTap: (Int) -> Unit = {},
  onDropOnTableau: (TableauMoveSource, Int) -> Unit = { _, _ -> },
  onDropOnFoundation: (FoundationMoveSource) -> Unit = {},
  autoComplete: Boolean = false,
  onAutoCompleteDone: () -> Unit = {},
) {
  val revealPlan = remember(dealId) { buildStartupRevealPlan(state) }
  var coveredVisibleCount by remember(dealId, animateReveal) { mutableIntStateOf(0) }
  var faceUpPhaseStarted by remember(dealId, animateReveal) { mutableStateOf(false) }
  var faceUpVisibleCount by remember(dealId, animateReveal) { mutableIntStateOf(0) }
  LaunchedEffect(dealId, animateReveal) {
    coveredVisibleCount = if (animateReveal) minOf(1, revealPlan.coveredTotal) else revealPlan.coveredTotal
    faceUpPhaseStarted = revealPlan.coveredTotal == 0 && revealPlan.faceUpTotal > 0
    faceUpVisibleCount = if (faceUpPhaseStarted) 1 else 0
  }

  fun startFaceUpPhase() {
    if (faceUpPhaseStarted || revealPlan.faceUpTotal == 0) return
    faceUpPhaseStarted = true
    faceUpVisibleCount = 1
  }

  fun onCoveredReady(index: Int) {
    if (!animateReveal || index != coveredVisibleCount - 1) return
    if (index == revealPlan.coveredTotal - 1) {
      startFaceUpPhase()
    } else {
      coveredVisibleCount++
    }
  }

  fun onFirstCoveredReady() {
    if (animateReveal || faceUpPhaseStarted) return
    startFaceUpPhase()
  }

  fun onFaceUpReady(index: Int) {
    if (!faceUpPhaseStarted || index != faceUpVisibleCount - 1) return
    faceUpVisibleCount = if (index == revealPlan.faceUpTotal - 1) Int.MAX_VALUE else faceUpVisibleCount + 1
  }

  val revealState = StartupRevealUiState(
    coveredVisibleCount = if (revealPlan.coveredTotal == 0) Int.MAX_VALUE else coveredVisibleCount,
    activeCoveredIndex = if (animateReveal && coveredVisibleCount in 1..revealPlan.coveredTotal) {
      coveredVisibleCount - 1
    } else {
      null
    },
    showFaceUpCards = faceUpPhaseStarted || revealPlan.faceUpTotal == 0,
    faceUpVisibleCount = if (faceUpPhaseStarted) faceUpVisibleCount else 0,
    activeFaceUpIndex = if (faceUpPhaseStarted && faceUpVisibleCount in 1..revealPlan.faceUpTotal) {
      faceUpVisibleCount - 1
    } else {
      null
    },
  )

  val dragState = rememberBoardDragState()
  val scope = rememberCoroutineScope()
  val density = LocalDensity.current
  val onDrop: (DragSource, DropTarget?) -> DropResult? = { source, target ->
    handleDrop(state, source, target, dragState, density, onDropOnTableau, onDropOnFoundation)
  }

  suspend fun animateWasteToFoundation(snapshot: GameState, durationMs: Int): Boolean {
    val card = snapshot.waste.lastOrNull() ?: return false
    if (snapshot.moveToFoundation(FoundationMoveSource.WasteTop) == null) return false
    val fromRect = dragState.anchorRect(Anchor.WasteTop) ?: return false
    val toRect = dragState.anchorRect(
      Anchor.FoundationDisplayedAt(foundationDisplayedIndexAfter(snapshot, card.suit))
    ) ?: return false
    dragState.runTapMove(
      source = DragSource.Waste,
      cards = listOf(card),
      fromRect = fromRect,
      toTopLeft = toRect.topLeft,
      applyMove = onWasteTap,
      durationMs = durationMs,
    )
    return true
  }

  suspend fun animateTableauTopToFoundation(snapshot: GameState, col: Int, durationMs: Int): Boolean {
    val pile = snapshot.tableau.getOrNull(col) ?: return false
    val card = pile.faceUp.lastOrNull() ?: return false
    if (snapshot.moveToFoundation(FoundationMoveSource.TableauTop(col)) == null) return false
    val fromRect = dragState.anchorRect(Anchor.TableauTop(col)) ?: return false
    val toRect = dragState.anchorRect(
      Anchor.FoundationDisplayedAt(foundationDisplayedIndexAfter(snapshot, card.suit))
    ) ?: return false
    dragState.runTapMove(
      source = DragSource.TableauRun(col, pile.faceUp.lastIndex),
      cards = listOf(card),
      fromRect = fromRect,
      toTopLeft = toRect.topLeft,
      applyMove = { onTableauTopTap(col) },
      durationMs = durationMs,
    )
    return true
  }

  val animatedWasteTap: () -> Unit = {
    scope.launch {
      if (!animateWasteToFoundation(state, TapMoveDurationMs)) onWasteTap()
    }
  }

  val animatedTableauTopTap: (Int) -> Unit = { col ->
    scope.launch {
      if (!animateTableauTopToFoundation(state, col, TapMoveDurationMs)) onTableauTopTap(col)
    }
  }

  val animatedStockTap: () -> Unit = {
    if (dragState.stockAnimMode != null) {
      // already animating; ignore
    } else if (state.stock.isNotEmpty()) {
      val drawn = drawnCardsPreview(state)
      val stockRect = dragState.anchorRect(Anchor.Stock)
      val wasteRect = dragState.anchorRect(Anchor.WasteTop)
      if (drawn.isEmpty() || stockRect == null || wasteRect == null) {
        onStockTap()
      } else {
        val step = wasteRect.width * WasteFanOffsetFraction
        val moves = drawn.mapIndexed { idx, card ->
          val slotFromTop = (drawn.size - 1) - idx
          val targetX = wasteRect.left - slotFromTop * step
          StockAnimMove(
            card = card,
            from = stockRect.topLeft,
            to = Offset(targetX, wasteRect.top),
            flipFromFaceUp = false,
            flipToFaceUp = true,
          )
        }
        val cardSize = IntSize(wasteRect.width.toInt(), wasteRect.height.toInt())
        scope.launch {
          dragState.runStockAnim(StockAnimMode.Draw, moves, cardSize, onStockTap)
        }
      }
    } else if (state.waste.isNotEmpty() && canRecycle) {
      val wasteRect = dragState.anchorRect(Anchor.WasteTop)
      val stockRect = dragState.anchorRect(Anchor.Stock)
      if (wasteRect == null || stockRect == null) {
        onStockTap()
      } else {
        val visible = state.waste.takeLast(WasteVisibleCount)
        val step = wasteRect.width * WasteFanOffsetFraction
        // visible[lastIndex] is on top (rightmost = wasteRect.left).
        val moves = visible.mapIndexed { i, card ->
          val slotFromTop = visible.lastIndex - i
          val fromX = wasteRect.left - slotFromTop * step
          StockAnimMove(
            card = card,
            from = Offset(fromX, wasteRect.top),
            to = stockRect.topLeft,
            flipFromFaceUp = true,
            flipToFaceUp = false,
          )
        }.asReversed() // animate top card first
        val cardSize = IntSize(wasteRect.width.toInt(), wasteRect.height.toInt())
        scope.launch {
          dragState.runStockAnim(StockAnimMode.Recycle, moves, cardSize, onStockTap)
        }
      }
    } else {
      onStockTap()
    }
  }

  val currentState = rememberUpdatedState(state)
  LaunchedEffect(autoComplete) {
    if (!autoComplete) return@LaunchedEffect
    while (true) {
      val snapshot = currentState.value
      val next = snapshot.nextAutoCompleteSource() ?: break
      val animated = when (next) {
        FoundationMoveSource.WasteTop ->
          animateWasteToFoundation(snapshot, AutoCompleteMoveMs)
        is FoundationMoveSource.TableauTop ->
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
        revealPlan = revealPlan,
        revealState = revealState,
        dragState = dragState,
        onCoveredReady = ::onCoveredReady,
        onFirstCoveredReady = ::onFirstCoveredReady,
        onFaceUpReady = ::onFaceUpReady,
        onStockTap = animatedStockTap,
        onWasteTap = animatedWasteTap,
        onDrop = onDrop,
      )
      TableauRowFilled(
        state = state,
        revealPlan = revealPlan,
        revealState = revealState,
        dragState = dragState,
        onCoveredReady = ::onCoveredReady,
        onFirstCoveredReady = ::onFirstCoveredReady,
        onFaceUpReady = ::onFaceUpReady,
        onTopTap = animatedTableauTopTap,
        onDrop = onDrop,
      )
    }
    DragOverlay(state = dragState)
    StockAnimationOverlay(state = dragState)
  }
}

private fun drawnCardsPreview(state: GameState): List<se.bernhauser.solitaire.game.Card> {
  if (state.stock.isEmpty()) return emptyList()
  val take = minOf(3, state.stock.size)
  return state.stock.takeLast(take).asReversed()
}

@Composable
private fun TopRow(
  state: GameState,
  revealPlan: StartupRevealPlan,
  revealState: StartupRevealUiState,
  dragState: BoardDragState,
  onCoveredReady: (Int) -> Unit,
  onFirstCoveredReady: () -> Unit,
  onFaceUpReady: (Int) -> Unit,
  onStockTap: () -> Unit,
  onWasteTap: () -> Unit,
  onDrop: (DragSource, DropTarget?) -> DropResult?,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    FoundationsBlock(
      modifier = Modifier.weight(4f),
      state = state,
      revealPlan = revealPlan,
      revealState = revealState,
      dragState = dragState,
      onFaceUpReady = onFaceUpReady,
      onDrop = onDrop,
    )
    Box(modifier = Modifier.weight(1f))
    WastePile(
      modifier = Modifier
        .weight(1f)
        .clickable(enabled = state.waste.isNotEmpty()) { onWasteTap() },
      cards = state.waste,
      revealPlan = revealPlan,
      dragState = dragState,
      faceUpVisibleCount = revealState.faceUpVisibleCount,
      activeFaceUpIndex = revealState.activeFaceUpIndex,
      showFaceUpCards = revealState.showFaceUpCards,
      onFaceUpReady = onFaceUpReady,
      onDrop = onDrop,
    )
    StockBox(
      modifier = Modifier
        .weight(1f)
        .clickable { onStockTap() },
      empty = state.stock.isEmpty(),
      revealPlan = revealPlan,
      dragState = dragState,
      coveredVisibleCount = revealState.coveredVisibleCount,
      activeCoveredIndex = revealState.activeCoveredIndex,
      onCoveredReady = onCoveredReady,
      onFirstCoveredReady = if (!revealState.showFaceUpCards) onFirstCoveredReady else null,
    )
  }
}

@Composable
private fun FoundationsBlock(
  modifier: Modifier,
  state: GameState,
  revealPlan: StartupRevealPlan,
  revealState: StartupRevealUiState,
  dragState: BoardDragState,
  onFaceUpReady: (Int) -> Unit,
  onDrop: (DragSource, DropTarget?) -> DropResult?,
) {
  Row(
    modifier = modifier.dropTarget(dragState, DropTarget.Foundation),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    val displayed = state.foundations.sortedBy { it.isEmpty() }
    displayed.forEachIndexed { displayedIndex, pile ->
      val slotAnchor = Modifier
        .weight(1f)
        .anchor(dragState, Anchor.FoundationDisplayedAt(displayedIndex))
      if (pile.isEmpty()) {
        EmptySlot(modifier = slotAnchor)
      } else {
        val faceUpIndex = revealPlan.foundationFaceUpIndex(displayedIndex)
        val top = pile.last()
        val dragging = dragState.active?.source == DragSource.Foundation(top.suit)
        val below = pile.dropLast(1).lastOrNull()
        Box(modifier = slotAnchor) {
          if (dragging) {
            if (below != null) {
              PlayingCard(
                card = below,
                revealed = revealState.showFaceUpCards &&
                  (faceUpIndex == null || faceUpIndex < revealState.faceUpVisibleCount),
              )
            } else {
              EmptySlot()
            }
          }
          PlayingCard(
            modifier = Modifier
              .then(if (dragging) Modifier.alpha(0f) else Modifier)
              .dragSource(
                state = dragState,
                source = DragSource.Foundation(top.suit),
                cards = listOf(top),
                onDrop = onDrop,
              ),
            card = top,
            revealed = revealState.showFaceUpCards &&
              (faceUpIndex == null || faceUpIndex < revealState.faceUpVisibleCount),
            onImageReady = if (faceUpIndex != null && revealState.activeFaceUpIndex == faceUpIndex) {
              { onFaceUpReady(faceUpIndex) }
            } else {
              null
            },
          )
        }
      }
    }
  }
}

@Composable
private fun ColumnScope.TableauRowFilled(
  state: GameState,
  revealPlan: StartupRevealPlan,
  revealState: StartupRevealUiState,
  dragState: BoardDragState,
  onCoveredReady: (Int) -> Unit,
  onFirstCoveredReady: () -> Unit,
  onFaceUpReady: (Int) -> Unit,
  onTopTap: (Int) -> Unit,
  onDrop: (DragSource, DropTarget?) -> DropResult?,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .weight(1f),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    state.tableau.forEachIndexed { col, pile ->
      TableauSlot(
        modifier = Modifier.weight(1f),
        pile = pile,
        revealPlan = revealPlan,
        revealState = revealState,
        col = col,
        dragState = dragState,
        onCoveredReady = onCoveredReady,
        onFirstCoveredReady = onFirstCoveredReady,
        onFaceUpReady = onFaceUpReady,
        onTopTap = onTopTap,
        onDrop = onDrop,
      )
    }
  }
}

@Composable
private fun RowScope.TableauSlot(
  modifier: Modifier,
  pile: se.bernhauser.solitaire.game.TableauPile,
  revealPlan: StartupRevealPlan,
  revealState: StartupRevealUiState,
  col: Int,
  dragState: BoardDragState,
  onCoveredReady: (Int) -> Unit,
  onFirstCoveredReady: () -> Unit,
  onFaceUpReady: (Int) -> Unit,
  onTopTap: (Int) -> Unit,
  onDrop: (DragSource, DropTarget?) -> DropResult?,
) {
  val interactionSource = remember { MutableInteractionSource() }
  Box(
    modifier = modifier
      .fillMaxHeight()
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        enabled = pile.faceUp.isNotEmpty(),
      ) { onTopTap(col) }
      .dropTarget(dragState, DropTarget.Tableau(col)),
  ) {
    TableauColumn(
      faceDownCount = pile.faceDown.size,
      faceUp = pile.faceUp,
      revealPlan = revealPlan,
      col = col,
      dragState = dragState,
      coveredVisibleCount = revealState.coveredVisibleCount,
      activeCoveredIndex = revealState.activeCoveredIndex,
      showFaceUpCards = revealState.showFaceUpCards,
      faceUpVisibleCount = revealState.faceUpVisibleCount,
      activeFaceUpIndex = revealState.activeFaceUpIndex,
      onCoveredReady = onCoveredReady,
      onFirstCoveredReady = if (!revealState.showFaceUpCards) onFirstCoveredReady else null,
      onFaceUpReady = onFaceUpReady,
      onDrop = onDrop,
    )
  }
}

private fun handleDrop(
  state: GameState,
  source: DragSource,
  target: DropTarget?,
  dragState: BoardDragState,
  density: Density,
  onDropOnTableau: (TableauMoveSource, Int) -> Unit,
  onDropOnFoundation: (FoundationMoveSource) -> Unit,
): DropResult? = when (target) {
  null -> null
  is DropTarget.Tableau -> {
    val move = source.asTableauMove()
    if (move == null || state.moveToTableau(move, target.column) == null) null
    else {
      val dest = tableauSettleTopLeft(state, target.column, dragState, density)
      if (dest == null) null
      else DropResult(dest) { onDropOnTableau(move, target.column) }
    }
  }
  DropTarget.Foundation -> {
    val move = source.asFoundationMove(state)
    val suit = source.movedCardSuit(state)
    if (move == null || suit == null || state.moveToFoundation(move) == null) null
    else {
      val dest = dragState.anchorRect(
        Anchor.FoundationDisplayedAt(foundationDisplayedIndexAfter(state, suit))
      )?.topLeft
      if (dest == null) null
      else DropResult(dest) { onDropOnFoundation(move) }
    }
  }
}

private fun tableauSettleTopLeft(
  state: GameState,
  col: Int,
  dragState: BoardDragState,
  density: Density,
): Offset? {
  val anchor = dragState.anchorRect(Anchor.TableauTop(col)) ?: return null
  val pile = state.tableau.getOrNull(col) ?: return null
  return if (pile.faceUp.isEmpty() && pile.faceDown.isEmpty()) {
    anchor.topLeft
  } else {
    val stepPx = with(density) { TableauCardOffset.toPx() }
    Offset(anchor.left, anchor.top + stepPx)
  }
}

private fun DragSource.movedCardSuit(state: GameState): Suit? = when (this) {
  DragSource.Waste -> state.waste.lastOrNull()?.suit
  is DragSource.Foundation -> state.foundations[suit.ordinal].lastOrNull()?.suit
  is DragSource.TableauRun -> state.tableau.getOrNull(column)?.faceUp?.getOrNull(fromIndex)?.suit
}

private fun DragSource.asTableauMove(): TableauMoveSource? = when (this) {
  DragSource.Waste -> TableauMoveSource.WasteTop
  is DragSource.Foundation -> TableauMoveSource.FoundationTop(suit)
  is DragSource.TableauRun -> TableauMoveSource.TableauRun(column, fromIndex)
}

private fun DragSource.asFoundationMove(state: GameState): FoundationMoveSource? = when (this) {
  DragSource.Waste -> FoundationMoveSource.WasteTop
  is DragSource.Foundation -> null
  is DragSource.TableauRun -> {
    val pile = state.tableau.getOrNull(column)
    if (pile != null && fromIndex == pile.faceUp.lastIndex) FoundationMoveSource.TableauTop(column)
    else null
  }
}

private fun foundationDisplayedIndexAfter(state: GameState, suit: Suit): Int =
  (0 until suit.ordinal).count { state.foundations[it].isNotEmpty() }

private fun totalCardCount(state: GameState): Int {
  val tableau = state.tableau.sumOf { it.faceDown.size + it.faceUp.size }
  val foundations = state.foundations.count { it.isNotEmpty() }
  val waste = state.waste.takeLast(3).size
  val stock = if (state.stock.isEmpty()) 0 else 1
  return tableau + foundations + waste + stock
}
