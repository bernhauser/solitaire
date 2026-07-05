package se.bernhauser.solitaire.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private const val SHOW_DEBUG_SETTINGS = false

val hasSettings: Boolean = SHOW_DEBUG_SETTINGS

@Composable
fun SettingsDialog(
  modifier: Modifier = Modifier,
  onDebugWin: () -> Unit,
  onDebugStuck: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    modifier = modifier,
    onDismissRequest = onDismiss,
    title = { Text("Settings") },
    text = {
      Column {
        if (SHOW_DEBUG_SETTINGS) {
          TextButton(onClick = onDebugWin) { Text("Set to Win state") }
          TextButton(onClick = onDebugStuck) { Text("Set to stuck state") }
        } else {
          Text("No settings yet.")
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text("Done") }
    },
  )
}
