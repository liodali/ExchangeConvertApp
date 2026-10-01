package dali.hamza.shared.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Reviews
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dali.hamza.shared.ui.components.EmptyState
import dali.hamza.shared.ui.components.LedgerTopAppBar

/**
 * Placeholder screens for the Account cluster (Phase 3) and pushed routes.
 * They carry the correct top bars and navigation hooks so the app is fully
 * navigable today; real content lands in its own phase.
 */
@Composable
private fun StubScreen(
    title: String,
    message: String,
    icon: ImageVector,
    actions: (@Composable () -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LedgerTopAppBar(title = title, trailing = actions)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 101.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            EmptyState(title = title, message = message, icon = icon)
        }
    }
}

@Composable
fun AccountPlaceholder() = StubScreen(
    title = "Profile & Settings",
    message = "Your Sovereign Ledger profile arrives in the Account phase.",
    icon = Icons.Outlined.PersonOutline,
)

@Composable
fun FaqPlaceholder() = StubScreen(
    title = "Frequently Asked Questions",
    message = "Answers to common questions are on their way.",
    icon = Icons.AutoMirrored.Outlined.HelpOutline,
)

@Composable
fun SupportPlaceholder(
    onOpenContact: () -> Unit,
    onOpenFeedback: () -> Unit,
) = StubScreen(
    title = "Support & Information",
    message = "Contact and feedback entry points arrive with the Support phase.",
    icon = Icons.AutoMirrored.Outlined.Chat,
    actions = {
        IconButton(onClick = onOpenContact) {
            Icon(Icons.Outlined.MailOutline, contentDescription = "Contact support")
        }
        IconButton(onClick = onOpenFeedback) {
            Icon(Icons.Outlined.Reviews, contentDescription = "Send feedback")
        }
    }
)

@Composable
fun ContactPlaceholder() = StubScreen(
    title = "Contact Support",
    message = "The contact form is coming soon.",
    icon = Icons.Outlined.MailOutline,
)

@Composable
fun FeedbackPlaceholder() = StubScreen(
    title = "User Feedback",
    message = "Tell us how we are doing — form coming soon.",
    icon = Icons.Outlined.Reviews,
)
