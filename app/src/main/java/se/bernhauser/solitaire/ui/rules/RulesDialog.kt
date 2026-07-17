package se.bernhauser.solitaire.ui.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RulesDialog(
  modifier: Modifier = Modifier,
  title: String,
  rules: List<String>,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    modifier = modifier,
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        rules.forEach { rule ->
          Text("•  $rule")
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text("Got it") }
    },
  )
}

@Composable
fun RulesHelpButton(
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  Surface(
    modifier = modifier.size(40.dp),
    onClick = onClick,
    shape = CircleShape,
    color = Color.White.copy(alpha = 0.15f),
    contentColor = Color.White.copy(alpha = 0.8f),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text("?", fontSize = 20.sp)
    }
  }
}

object GameRules {
  val spider: List<String> = listOf(
    "Played with two decks — 1, 2, or 4 suits depending on difficulty.",
    "Build columns downward in rank; suits may mix.",
    "Only a same-suit descending run can be moved as a group.",
    "An empty column takes any card or run.",
    "Tap the stock to deal one card onto every column — no column may be empty.",
    "A complete King-to-Ace run in one suit is removed from play.",
    "Win by completing all 8 runs.",
  )

  val freeCell: List<String> = listOf(
    "All 52 cards are dealt face up in 8 columns.",
    "Build columns downward in alternating colors.",
    "Each of the 4 free cells holds a single card.",
    "An empty column takes any card or run.",
    "Free cells and empty columns increase how many cards you can move at once.",
    "Win by moving all cards to the foundations, built up by suit from Ace to King.",
  )

  val pyramid: List<String> = listOf(
    "Remove pairs of cards that add up to 13.",
    "Jack = 11, Queen = 12, King = 13 — Kings are removed alone.",
    "Only uncovered cards can be played: nothing may overlap them from below.",
    "Tap the stock to draw; the top waste card can be paired too.",
    "When the stock runs out, the waste can be recycled twice.",
    "Win by clearing the pyramid — leftover stock and waste cards don't matter.",
  )

  val triPeaks: List<String> = listOf(
    "Play any uncovered card that is one rank above or below the waste card.",
    "Ranks wrap around: Ace and King connect.",
    "A card is uncovered once the cards overlapping it are gone.",
    "Tap the stock to flip a new card onto the waste — there is no redeal.",
    "Win by clearing all three peaks before the stock and moves run out.",
  )
}
