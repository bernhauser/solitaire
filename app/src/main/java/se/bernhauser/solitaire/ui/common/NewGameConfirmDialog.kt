package se.bernhauser.solitaire.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun NewGameConfirmDialog(
  modifier: Modifier = Modifier,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    modifier = modifier,
    onDismissRequest = onDismiss,
    title = { Text("Start a new game?") },
    text = { Text("Your current progress will be lost.") },
    confirmButton = {
      TextButton(onClick = onConfirm) { Text("New game") }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}
