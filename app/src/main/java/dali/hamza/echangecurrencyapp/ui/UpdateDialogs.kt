package dali.hamza.echangecurrencyapp.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

/**
 * Update prompts for [AppUpdateChecker]. Self-styled (not LedgerColors)
 * on purpose: the shared theme lives *inside* [ExchangeCurrencyApp], so
 * these dialogs — composed as its siblings in the app module — re-use
 * the same canvas colors MainActivity already knows for the window
 * background. The colors match; the coupling stays zero.
 */
@Composable
fun UpdateDialogs(
    state: AppUpdateChecker.UiState,
    dark: Boolean,
    onUpdate: () -> Unit,
    onDecline: () -> Unit,
    onRestart: () -> Unit,
) {
    // Sovereign Ledger canvas tones (same values MainActivity paints the
    // window with pre-Compose).
    val canvas = if (dark) Color(0xFF0E0E0E) else Color(0xFFF5F7F2)
    val onCanvas = if (dark) Color(0xFFF4F2EC) else Color(0xFF191C1A)
    val accent = Color(0xFF1F93FF) // Play blue — signals "store action"

    when (state) {
        is AppUpdateChecker.UiState.Available -> AlertDialog(
            onDismissRequest = onDecline,
            containerColor = canvas,
            titleContentColor = onCanvas,
            textContentColor = onCanvas,
            title = { Text("Update available", fontSize = 18.sp) },
            text = {
                Text(
                    "Sovereign Ledger ${state.versionCode} is out. " +
                        "Update now — you can keep using the app while it downloads."
                )
            },
            confirmButton = {
                TextButton(onClick = onUpdate) {
                    Text("Update", color = accent)
                }
            },
            dismissButton = {
                TextButton(onClick = onDecline) { Text("Later", color = onCanvas.copy(alpha = 0.7f)) }
            },
        )

        AppUpdateChecker.UiState.Downloaded -> AlertDialog(
            onDismissRequest = onDecline,
            containerColor = canvas,
            titleContentColor = onCanvas,
            textContentColor = onCanvas,
            title = { Text("Update ready", fontSize = 18.sp) },
            text = { Text("The new version is downloaded. Restart to finish installing.") },
            confirmButton = {
                TextButton(onClick = onRestart) {
                    Text("Restart", color = accent)
                }
            },
            dismissButton = {
                TextButton(onClick = onDecline) { Text("Later", color = onCanvas.copy(alpha = 0.7f)) }
            },
        )

        AppUpdateChecker.UiState.Idle -> Unit
    }
}
