package se.bernhauser.solitaire.ui.tripeaks

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import se.bernhauser.solitaire.game.tripeaks.TriPeaksViewModel
import se.bernhauser.solitaire.ui.common.NewGameConfirmDialog
import se.bernhauser.solitaire.ui.rules.GameRules
import se.bernhauser.solitaire.ui.rules.RulesDialog
import se.bernhauser.solitaire.ui.rules.RulesHelpButton
import se.bernhauser.solitaire.ui.theme.FeltGreen
import se.bernhauser.solitaire.ui.win.GameOverOverlay
import se.bernhauser.solitaire.ui.win.WinOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriPeaksScreen(modifier: Modifier = Modifier, onBack: () -> Unit = {}) {
  val app = LocalContext.current.applicationContext as SolitaireApp
  val vm: TriPeaksViewModel =
    viewModel(factory = TriPeaksViewModel.factory(app.repositorySupplier.triPeaksRepo))
  val state by vm.state.collectAsState()
  val canUndo by vm.canUndo.collectAsState()
  val isWon by vm.isWon.collectAsState()
  val gameOver by vm.gameOver.collectAsState()
  var showNewGameConfirm by remember { mutableStateOf(false) }
  var showRules by remember { mutableStateOf(false) }

  BackHandler { onBack() }

  Scaffold(
    modifier = modifier.fillMaxSize().background(FeltGreen),
    containerColor = FeltGreen,
    topBar = {
      TopAppBar(
        title = {},
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
        TriPeaksBoard(
          state = current,
          onPlay = vm::onPlay,
          onDrawTap = vm::onDrawTap,
        )
      }
      RulesHelpButton(
        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        onClick = { showRules = true },
      )
      if (isWon) {
        WinOverlay(onNewGame = vm::newGame)
      } else if (gameOver) {
        GameOverOverlay(
          onNewGame = vm::newGame,
          onUndo = vm::undo,
          canUndo = canUndo,
          subtitle = "No more moves are possible.",
        )
      }
    }
  }

  if (showNewGameConfirm) {
    NewGameConfirmDialog(
      onConfirm = {
        showNewGameConfirm = false
        vm.newGame()
      },
      onDismiss = { showNewGameConfirm = false },
    )
  }

  if (showRules) {
    RulesDialog(
      title = "TriPeaks",
      rules = GameRules.triPeaks,
      onDismiss = { showRules = false },
    )
  }
}
