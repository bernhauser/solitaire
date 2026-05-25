package se.bernhauser.solitaire.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import se.bernhauser.solitaire.BuildConfig

@Composable
fun SettingsDialog(
  modifier: Modifier = Modifier,
  onDebugWin: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    modifier = modifier,
    onDismissRequest = onDismiss,
    title = { Text("Settings") },
    text = {
      Column {
        if (BuildConfig.DEBUG) {
          TextButton(onClick = onDebugWin) { Text("Set to Win state") }
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
