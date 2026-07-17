package se.bernhauser.solitaire.ui.spider

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import se.bernhauser.solitaire.SolitaireApp
import se.bernhauser.solitaire.game.spider.SpiderDifficulty
import se.bernhauser.solitaire.game.spider.SpiderViewModel
import se.bernhauser.solitaire.ui.common.NewGameConfirmDialog
import se.bernhauser.solitaire.ui.rules.GameRules
import se.bernhauser.solitaire.ui.rules.RulesDialog
import se.bernhauser.solitaire.ui.rules.RulesHelpButton
import se.bernhauser.solitaire.ui.theme.FeltGreen
import se.bernhauser.solitaire.ui.win.GameOverOverlay
import se.bernhauser.solitaire.ui.win.WinOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpiderScreen(modifier: Modifier = Modifier, onBack: () -> Unit = {}) {
  val app = LocalContext.current.applicationContext as SolitaireApp
  val vm: SpiderViewModel = viewModel(factory = SpiderViewModel.factory(app.repositorySupplier.spiderRepo))
  val state by vm.state.collectAsState()
  val difficulty by vm.difficulty.collectAsState()
  val canUndo by vm.canUndo.collectAsState()
  val isWon by vm.isWon.collectAsState()
  val gameOver by vm.gameOver.collectAsState()
  val needsNewGame by vm.needsNewGame.collectAsState()
  var showNewGameConfirm by remember { mutableStateOf(false) }
  var showDifficultyPicker by remember { mutableStateOf(false) }
  var showRules by remember { mutableStateOf(false) }

  BackHandler { onBack() }

  Scaffold(
    modifier = modifier.fillMaxSize().background(FeltGreen),
    containerColor = FeltGreen,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = difficulty?.label.orEmpty(),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 16.sp,
          )
        },
        navigationIcon = {
          TextButton(onClick = onBack) {
            Text("‹", color = Color.White, fontSize = 28.sp)
          }
        },
        actions = {
          TextButton(onClick = vm::undo, enabled = canUndo) {
            Text("Undo", color = if (canUndo) Color.White else Color.White.copy(alpha = 0.4f))
          }
          TextButton(onClick = { showNewGameConfirm = true }) {
            Text("New game", color = Color.White)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = FeltGreen,
          actionIconContentColor = Color.White,
        ),
      )
    },
  ) { innerPadding ->
    Box(modifier = Modifier.fillMaxSize().padding(innerPadding).background(FeltGreen)) {
      state?.let { current ->
        SpiderBoard(
          state = current,
          onDealTap = vm::onDealTap,
          onMoveRun = vm::onMoveRun,
        )
      }
      RulesHelpButton(
        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        onClick = { showRules = true },
      )
      if (isWon) {
        WinOverlay(onNewGame = { showDifficultyPicker = true })
      } else if (gameOver) {
        GameOverOverlay(
          onNewGame = { showDifficultyPicker = true },
          onUndo = vm::undo,
          canUndo = canUndo,
          subtitle = "No more moves are possible.",
        )
      }
    }
  }

  if (needsNewGame || showDifficultyPicker) {
    SpiderDifficultyDialog(
      onPick = { picked ->
        showDifficultyPicker = false
        vm.newGame(picked)
      },
      onDismiss = {
        showDifficultyPicker = false
        if (needsNewGame) onBack()
      },
    )
  }

  if (showNewGameConfirm) {
    NewGameConfirmDialog(
      onConfirm = {
        showNewGameConfirm = false
        showDifficultyPicker = true
      },
      onDismiss = { showNewGameConfirm = false },
    )
  }

  if (showRules) {
    RulesDialog(
      title = "Spider",
      rules = GameRules.spider,
      onDismiss = { showRules = false },
    )
  }
}

@Composable
private fun SpiderDifficultyDialog(
  modifier: Modifier = Modifier,
  onPick: (SpiderDifficulty) -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    modifier = modifier,
    onDismissRequest = onDismiss,
    title = { Text("Spider") },
    text = {
      Column {
        DifficultyOption("Easy", SpiderDifficulty.OneSuit, onPick)
        DifficultyOption("Medium", SpiderDifficulty.TwoSuits, onPick)
        DifficultyOption("Hard", SpiderDifficulty.FourSuits, onPick)
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@Composable
private fun DifficultyOption(
  name: String,
  difficulty: SpiderDifficulty,
  onPick: (SpiderDifficulty) -> Unit,
) {
  TextButton(
    modifier = Modifier.fillMaxWidth(),
    onClick = { onPick(difficulty) },
  ) {
    Text("$name — ${difficulty.label}", fontSize = 18.sp)
  }
}
