package se.bernhauser.solitaire.ui.landing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.bernhauser.solitaire.game.Card
import se.bernhauser.solitaire.ui.cards.PlayingCard
import se.bernhauser.solitaire.ui.theme.FeltGreen
import se.bernhauser.solitaire.ui.theme.FeltGreenLight

data class GameMenuItem(
  val title: String,
  val description: String,
  val previewCards: List<Card>,
  val inProgress: Boolean,
  val onPlay: () -> Unit,
)

@Composable
fun LandingScreen(
  modifier: Modifier = Modifier,
  games: List<GameMenuItem>,
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(FeltGreen)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp, vertical = 32.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp),
  ) {
    Column {
      Text(
        text = "Solitaire",
        color = Color.White,
        fontSize = 44.sp,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = "Pick a game",
        color = Color.White.copy(alpha = 0.7f),
        fontSize = 18.sp,
      )
    }
    Spacer(modifier = Modifier.height(4.dp))
    games.forEach { game ->
      GameCard(game = game)
    }
  }
}

@Composable
private fun GameCard(modifier: Modifier = Modifier, game: GameMenuItem) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    color = FeltGreenLight,
  ) {
    Row(
      modifier = Modifier
        .clickable(onClick = game.onPlay)
        .padding(20.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      CardFan(cards = game.previewCards)
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = game.title,
          color = Color.White,
          fontSize = 28.sp,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = game.description,
          color = Color.White.copy(alpha = 0.8f),
          fontSize = 16.sp,
        )
        if (game.inProgress) {
          Surface(
            shape = RoundedCornerShape(50),
            color = Color.White.copy(alpha = 0.18f),
          ) {
            Text(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
              text = "Game in progress",
              color = Color.White,
              fontSize = 13.sp,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CardFan(modifier: Modifier = Modifier, cards: List<Card>) {
  Box(modifier = modifier.width(96.dp).height(96.dp), contentAlignment = Alignment.Center) {
    val angles = listOf(-14f, 0f, 14f)
    cards.take(3).forEachIndexed { i, card ->
      PlayingCard(
        modifier = Modifier
          .width(56.dp)
          .padding(start = (i * 18).dp)
          .rotate(angles.getOrElse(i) { 0f }),
        card = card,
      )
    }
  }
}
