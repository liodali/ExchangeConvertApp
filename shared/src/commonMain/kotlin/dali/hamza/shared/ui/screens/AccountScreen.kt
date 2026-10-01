package dali.hamza.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.window.Dialog
import dali.hamza.shared.ui.components.BentoCard
import dali.hamza.shared.ui.components.LedgerButton
import dali.hamza.shared.ui.components.LedgerButtonVariant
import dali.hamza.shared.ui.components.LedgerChip
import dali.hamza.shared.ui.components.LedgerInfoField
import dali.hamza.shared.ui.components.LedgerInput
import dali.hamza.shared.ui.components.LedgerListRow
import dali.hamza.shared.ui.components.LedgerTopAppBar
import dali.hamza.shared.ui.components.SectionHeader
import dali.hamza.shared.ui.theme.LedgerColors
import dali.hamza.shared.ui.theme.LedgerStrings
import dali.hamza.shared.ui.viewmodel.AccountViewModel

/**
 * Profile & Settings (design frame `qDCZE`) — Account tab.
 *
 * Sections follow the pen file: profile hero (avatar + tier badges + edit),
 * personal information, security health (clearance ring), security &
 * authentication, preferences (persisted notifications toggle), vault
 * management, support entry point and the node/version footer.
 */
@Composable
fun AccountScreen(
    viewModel: AccountViewModel,
    onOpenSupport: () -> Unit,
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var biometricUnlock by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        LedgerTopAppBar(
            title = "Sovereign Ledger",
            avatarInitials = viewModel.username,
            trailing = {
                IconButton(onClick = onOpenSupport) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = LedgerColors.TextMuted,
                    )
                }
            },
        )

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(24.dp))

            // ============ Profile hero =====================================
            ProfileHero(
                username = viewModel.username,
                email = viewModel.email,
                onEdit = { showEditDialog = true },
            )

            Spacer(Modifier.height(32.dp))

            // ============ Personal information =============================
            BentoCard {
                SectionHeader(
                    title = LedgerStrings.Account.PERSONAL_INFO,
                    icon = Icons.Outlined.PersonOutline,
                )
                Spacer(Modifier.height(24.dp))
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    LedgerInfoField(
                        label = LedgerStrings.Account.PHONE_LABEL,
                        value = LedgerStrings.Account.PHONE_VALUE,
                    )
                    LedgerInfoField(
                        label = LedgerStrings.Account.RESIDENCE_LABEL,
                        value = LedgerStrings.Account.RESIDENCE_VALUE,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ============ Security health ==================================
            BentoCard {
                SectionHeader(
                    title = LedgerStrings.Account.SECURITY_HEALTH,
                    icon = Icons.Outlined.SupportAgent,
                    iconTint = LedgerColors.Green,
                    titleColor = LedgerColors.Green,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = LedgerStrings.Account.SECURITY_HEALTH_DESC,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LedgerColors.TextSecondary,
                )
                Spacer(Modifier.height(24.dp))
                ClearanceRow()
            }

            Spacer(Modifier.height(24.dp))

            // ============ Security & authentication =======================
            BentoCard {
                SectionLabel(LedgerStrings.Account.SECURITY_SECTION)
                Spacer(Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    LedgerListRow(
                        label = LedgerStrings.Account.CHANGE_PASSWORD,
                        icon = Icons.Outlined.Password,
                    )
                    LedgerListRow(
                        label = LedgerStrings.Account.BIOMETRIC_UNLOCK,
                        icon = Icons.Outlined.Fingerprint,
                        trailing = {
                            Switch(
                                checked = biometricUnlock,
                                onCheckedChange = { biometricUnlock = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = LedgerColors.Green,
                                    checkedTrackColor = LedgerColors.Green.copy(alpha = 0.25f),
                                ),
                            )
                        },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ============ Preferences =====================================
            BentoCard {
                SectionLabel(LedgerStrings.Account.PREFERENCES)
                Spacer(Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    LedgerListRow(
                        label = LedgerStrings.Account.PUSH_NOTIFICATIONS,
                        icon = Icons.Outlined.Notifications,
                        onClick = viewModel::toggleNotifications,
                        trailing = {
                            LedgerChip(
                                text = if (viewModel.notificationsEnabled) {
                                    LedgerStrings.Account.ENABLED
                                } else {
                                    LedgerStrings.Account.DISABLED
                                },
                                tint = if (viewModel.notificationsEnabled) {
                                    LedgerColors.Green
                                } else {
                                    LedgerColors.TextTertiary
                                },
                                cornerRadius = 4.dp,
                            )
                        },
                    )
                    LedgerListRow(
                        label = LedgerStrings.Account.LANGUAGE,
                        icon = Icons.Outlined.Language,
                        value = LedgerStrings.Account.LANGUAGE_VALUE,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ============ Vault management ================================
            BentoCard {
                SectionLabel(LedgerStrings.Account.VAULT_SECTION)
                Spacer(Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    LedgerListRow(
                        label = LedgerStrings.Account.LOG_OUT,
                        icon = Icons.AutoMirrored.Outlined.Logout,
                        emphasize = true,
                    )
                    LedgerListRow(
                        label = LedgerStrings.Account.DELETE_ACCOUNT,
                        icon = Icons.Outlined.DeleteOutline,
                        emphasize = true,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ============ Support entry point =============================
            SectionLabel(LedgerStrings.Account.SUPPORT_SECTION)
            Spacer(Modifier.height(16.dp))
            BentoCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    IconTile(
                        icon = Icons.Outlined.SupportAgent,
                        tint = LedgerColors.Blue,
                        size = 57.dp,
                    )
                    Column {
                        Text(
                            text = LedgerStrings.Account.SUPPORT_CARD_TITLE,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = LedgerStrings.Account.SUPPORT_CARD_DESC,
                            style = MaterialTheme.typography.bodyMedium,
                            color = LedgerColors.TextSecondary,
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                LedgerButton(
                    text = LedgerStrings.Account.SUPPORT_CARD_CTA,
                    onClick = onOpenSupport,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(24.dp))

            // ============ Node footer =====================================
            Text(
                text = LedgerStrings.Account.footer(viewModel.lastSyncLabel()),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                ),
                color = LedgerColors.Steel,
            )

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showEditDialog) {
        EditNameDialog(
            initial = viewModel.username,
            onDismiss = { showEditDialog = false },
            onSave = { name ->
                viewModel.rename(name)
                showEditDialog = false
            },
        )
    }
}

/** Caps section label — Inter 600/12 secondary (design `Heading 3`). */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = LedgerColors.TextSecondary,
    )
}

/** Tinted icon tile used across the cluster (design `Overlay` recipe). */
@Composable
private fun IconTile(
    icon: ImageVector,
    tint: Color,
    size: androidx.compose.ui.unit.Dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(tint.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun ProfileHero(
    username: String,
    email: String,
    onEdit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Box(modifier = Modifier.size(128.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(LedgerColors.SurfaceRaised)
                    .border(1.dp, LedgerColors.Blue.copy(alpha = 0.45f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = username.take(2).uppercase(),
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                    color = LedgerColors.Blue,
                )
            }
            // camera badge (design: 41x39 blue rounded-12, navy mark)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(LedgerColors.Blue),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoCamera,
                    contentDescription = LedgerStrings.Account.EDIT_PROFILE,
                    tint = LedgerColors.NavyPanel,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Column {
            Text(
                text = username,
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                color = LedgerColors.TextPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = email,
                style = MaterialTheme.typography.bodyLarge,
                color = LedgerColors.TextSecondary,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LedgerChip(
                    text = LedgerStrings.Account.BADGE_PRIVATE,
                    tint = LedgerColors.Gold,
                )
                LedgerChip(
                    text = LedgerStrings.Account.BADGE_VERIFIED,
                    tint = LedgerColors.Green,
                )
            }
        }

        LedgerButton(
            text = LedgerStrings.Account.EDIT_PROFILE,
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 48dp progress ring (75%, green arc on steel track) + clearance label. */
@Composable
private fun ClearanceRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LedgerColors.CardAlt)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(48.dp)) {
                val stroke = 4.dp.toPx()
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(
                    color = LedgerColors.Steel,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke),
                )
                drawArc(
                    color = LedgerColors.Green,
                    startAngle = -90f,
                    sweepAngle = 360f * 0.75f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke),
                )
            }
            Text(
                text = LedgerStrings.Account.CLEARANCE_SCORE,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = LedgerColors.Green,
            )
        }
        Text(
            text = LedgerStrings.Account.CLEARANCE,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/** Edit Profile — renames the Sovereign account (persisted in session). */
@Composable
private fun EditNameDialog(
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    Dialog(onDismissRequest = onDismiss) {
        BentoCard(fill = LedgerColors.SurfaceElevated) {
            Text(
                text = LedgerStrings.Account.EDIT_PROFILE_TITLE,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = LedgerColors.TextPrimary,
            )
            Spacer(Modifier.height(16.dp))
            LedgerInput(
                value = name,
                onValueChange = { name = it },
                label = LedgerStrings.Account.EDIT_PROFILE_HINT,
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LedgerButton(
                    text = LedgerStrings.Account.DIALOG_CANCEL,
                    onClick = onDismiss,
                    variant = LedgerButtonVariant.TONAL,
                    modifier = Modifier.weight(1f),
                )
                LedgerButton(
                    text = LedgerStrings.Account.DIALOG_SAVE,
                    onClick = { onSave(name) },
                    modifier = Modifier.weight(1f),
                    enabled = name.isNotBlank(),
                )
            }
        }
    }
}
