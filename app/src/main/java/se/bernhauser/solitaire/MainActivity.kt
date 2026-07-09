package se.bernhauser.solitaire

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import se.bernhauser.solitaire.game.Card
import se.bernhauser.solitaire.game.Rank
import se.bernhauser.solitaire.game.Suit
import se.bernhauser.solitaire.ui.freecell.FreeCellScreen
import se.bernhauser.solitaire.ui.klondike.KlondikeScreen
import se.bernhauser.solitaire.ui.landing.GameMenuItem
import se.bernhauser.solitaire.ui.landing.LandingScreen
import se.bernhauser.solitaire.ui.spider.SpiderScreen
import se.bernhauser.solitaire.ui.theme.SolitaireTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      SolitaireTheme {
        AppRoot()
      }
    }
  }
}

private enum class AppScreen { Landing, Klondike, Spider, FreeCell }

@Composable
private fun AppRoot() {
  var screen by rememberSaveable { mutableStateOf(AppScreen.Landing) }
  when (screen) {
    AppScreen.Landing -> Landing(
      onPlayKlondike = { screen = AppScreen.Klondike },
      onPlaySpider = { screen = AppScreen.Spider },
      onPlayFreeCell = { screen = AppScreen.FreeCell },
    )
    AppScreen.Klondike -> KlondikeScreen(onBack = { screen = AppScreen.Landing })
    AppScreen.Spider -> SpiderScreen(onBack = { screen = AppScreen.Landing })
    AppScreen.FreeCell -> FreeCellScreen(onBack = { screen = AppScreen.Landing })
  }
}

@Composable
private fun Landing(
  onPlayKlondike: () -> Unit,
  onPlaySpider: () -> Unit,
  onPlayFreeCell: () -> Unit,
) {
  val app = LocalContext.current.applicationContext as SolitaireApp
  val hasKlondikeSave by produceState(initialValue = false) {
    value = app.repositorySupplier.klondikeRepo.load() != null
  }
  val hasSpiderSave by produceState(initialValue = false) {
    value = app.repositorySupplier.spiderRepo.load() != null
  }
  val hasFreeCellSave by produceState(initialValue = false) {
    value = app.repositorySupplier.freeCellRepo.load() != null
  }
  LandingScreen(
    games = listOf(
      GameMenuItem(
        title = "Klondike",
        description = "The classic solitaire",
        previewCards = listOf(
          Card(Rank.Ace, Suit.Spades),
          Card(Rank.King, Suit.Hearts),
          Card(Rank.Queen, Suit.Clubs),
        ),
        inProgress = hasKlondikeSave,
        onPlay = onPlayKlondike,
      ),
      GameMenuItem(
        title = "Spider",
        description = "Build runs from King to Ace",
        previewCards = listOf(
          Card(Rank.King, Suit.Spades),
          Card(Rank.Queen, Suit.Spades),
          Card(Rank.Jack, Suit.Spades),
        ),
        inProgress = hasSpiderSave,
        onPlay = onPlaySpider,
      ),
      GameMenuItem(
        title = "FreeCell",
        description = "All cards open — nearly every deal is winnable",
        previewCards = listOf(
          Card(Rank.Ace, Suit.Diamonds),
          Card(Rank.Eight, Suit.Clubs),
          Card(Rank.Five, Suit.Hearts),
        ),
        inProgress = hasFreeCellSave,
        onPlay = onPlayFreeCell,
      ),
    ),
  )
}
