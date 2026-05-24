package se.bernhauser.solitaire

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import se.bernhauser.solitaire.game.GameViewModel
import se.bernhauser.solitaire.ui.board.KlondikeBoard
import se.bernhauser.solitaire.ui.settings.SettingsDialog
import se.bernhauser.solitaire.ui.theme.FeltGreen
import se.bernhauser.solitaire.ui.theme.SolitaireTheme
import se.bernhauser.solitaire.ui.win.GameOverOverlay
import se.bernhauser.solitaire.ui.win.WinOverlay

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      SolitaireTheme {
        GameScreen()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameScreen(modifier: Modifier = Modifier) {
  val app = LocalContext.current.applicationContext as SolitaireApp
  val vm: GameViewModel = viewModel(factory = GameViewModel.factory(app.repositorySupplier.gameRepo))
  val state by vm.state.collectAsState()
  val canUndo by vm.canUndo.collectAsState()
  val canAutoComplete by vm.canAutoComplete.collectAsState()
  val canRecycle by vm.canRecycle.collectAsState()
  val isWon by vm.isWon.collectAsState()
  val gameOver by vm.gameOver.collectAsState()
  val dealId by vm.dealId.collectAsState()
  val isRestored by vm.isRestored.collectAsState()
  var autoCompleting by remember { mutableStateOf(false) }
  var showSettings by remember { mutableStateOf(false) }

  Scaffold(
    modifier = modifier.fillMaxSize().background(FeltGreen),
    containerColor = FeltGreen,
    topBar = {
      TopAppBar(
        title = {},
        actions = {
          TextButton(onClick = vm::undo, enabled = canUndo && !autoCompleting) {
            val enabled = canUndo && !autoCompleting
            Text("Undo", color = if (enabled) Color.White else Color.White.copy(alpha = 0.4f))
          }
          if (canAutoComplete || autoCompleting) {
            TextButton(
              onClick = { autoCompleting = true },
              enabled = !autoCompleting,
            ) {
              Text("Finish", color = if (autoCompleting) Color.White.copy(alpha = 0.4f) else Color.White)
            }
          }
          TextButton(onClick = vm::newGame, enabled = !autoCompleting) {
            Text(
              "New game",
              color = if (autoCompleting) Color.White.copy(alpha = 0.4f) else Color.White,
            )
          }
          TextButton(onClick = { showSettings = true }, enabled = !autoCompleting) {
            Text(
              "☰",
              color = if (autoCompleting) Color.White.copy(alpha = 0.4f) else Color.White,
            )
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
      val current = state ?: return@Box
      val boardModifier = if (autoCompleting) {
        Modifier.pointerInput(Unit) {
          awaitPointerEventScope {
            while (true) {
              awaitPointerEvent().changes.forEach { it.consume() }
            }
          }
        }
      } else Modifier
      Box(modifier = boardModifier) {
        KlondikeBoard(
          state = current,
          dealId = dealId,
          animateReveal = !isRestored,
          canRecycle = canRecycle,
          onStockTap = vm::onStockTap,
          onWasteTap = vm::onWasteTap,
          onTableauTopTap = vm::onTableauTopTap,
          onDropOnTableau = vm::onDropOnTableau,
          onDropOnFoundation = vm::onDropOnFoundation,
          autoComplete = autoCompleting,
          onAutoCompleteDone = { autoCompleting = false },
        )
      }
      if (isWon) {
        WinOverlay(onNewGame = vm::newGame)
      } else if (gameOver) {
        GameOverOverlay(
          onNewGame = vm::newGame,
          onUndo = vm::undo,
          canUndo = canUndo,
        )
      }
    }
  }

  if (showSettings) {
    SettingsDialog(
      onDebugWin = {
        showSettings = false
        vm.debugWin()
      },
      onDismiss = { showSettings = false },
    )
  }
}
