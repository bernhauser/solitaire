package se.bernhauser.solitaire.ui.win

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GameOverOverlay(
  modifier: Modifier = Modifier,
  onNewGame: () -> Unit,
  onUndo: () -> Unit,
  canUndo: Boolean,
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.55f)),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.padding(24.dp),
    ) {
      Text(
        text = "No moves left",
        color = Color.White,
        fontSize = 40.sp,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = "You dealt through the stock without a single playable card.",
        color = Color.White.copy(alpha = 0.85f),
        fontSize = 16.sp,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (canUndo) {
          OutlinedButton(onClick = onUndo) { Text("Undo") }
        }
        Button(onClick = onNewGame) { Text("New game") }
      }
    }
  }
}
