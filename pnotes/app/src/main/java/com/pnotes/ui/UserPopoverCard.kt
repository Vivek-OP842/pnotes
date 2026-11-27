package com.pnotes.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.pnotes.account.Account
import com.pnotes.account.AccountState
import kotlinx.coroutines.launch

/**
 * Development preview mode switcher for testing all 3 user popover tiers:
 * - GUEST: Unauthenticated free tier
 * - SIGNED_IN_FREE: Free user signed in with Obsidian cloud ID
 * - LIFETIME_PRO: Pro user with verified lifetime entitlement
 */
enum class PopoverPreviewMode {
    GUEST,
    SIGNED_IN_FREE,
    LIFETIME_PRO
}

/**
 * Top-level Popover Anchor for the user avatar menu card.
 */
@Composable
fun UserPopoverMenu(
    expanded: Boolean,
    accountState: AccountState,
    notesCount: Int = 3,
    maxFreeNotes: Int = 10,
    onDismissRequest: () -> Unit,
    onUpgrade: () -> Unit = {},
    onSignIn: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onStylusStudio: () -> Unit = {},
    onPaperDefaults: () -> Unit = {},
    onCanvasPresentation: () -> Unit = {},
    onAppSettings: () -> Unit = {},
    onExportVault: () -> Unit = {},
    onManageAccount: () -> Unit = {},
    onRestorePurchase: () -> Unit = {},
) {
    if (!expanded) return

    val density = LocalDensity.current
    val yOffsetPx = with(density) { 56.dp.roundToPx() }
    val xOffsetPx = with(density) { (-12).dp.roundToPx() }

    Popup(
        alignment = Alignment.TopEnd,
        offset = IntOffset(xOffsetPx, yOffsetPx),
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        UserPopoverContent(
            accountState = accountState,
            notesCount = notesCount,
            maxFreeNotes = maxFreeNotes,
            onUpgrade = {
                onDismissRequest()
                onUpgrade()
            },
            onSignIn = {
                onDismissRequest()
                onSignIn()
            },
            onSignOut = {
                onDismissRequest()
                onSignOut()
            },
            onStylusStudio = {
                onDismissRequest()
                onStylusStudio()
            },
            onPaperDefaults = {
                onDismissRequest()
                onPaperDefaults()
            },
            onCanvasPresentation = {
                onDismissRequest()
                onCanvasPresentation()
            },
            onAppSettings = {
                onDismissRequest()
                onAppSettings()
            },
            onExportVault = {
                onDismissRequest()
                onExportVault()
            },
            onManageAccount = {
                onDismissRequest()
                onManageAccount()
            },
            onRestorePurchase = {
                onDismissRequest()
                onRestorePurchase()
            },
        )
    }
}

/**
 * Popover Content Card matching the prototype layout, geometry, typography, and vectors.
 */
@Composable
fun UserPopoverContent(
    accountState: AccountState,
    notesCount: Int = 3,
    maxFreeNotes: Int = 10,
    modifier: Modifier = Modifier,
    onUpgrade: () -> Unit = {},
    onSignIn: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onStylusStudio: () -> Unit = {},
    onPaperDefaults: () -> Unit = {},
    onCanvasPresentation: () -> Unit = {},
    onAppSettings: () -> Unit = {},
    onExportVault: () -> Unit = {},
    onManageAccount: () -> Unit = {},
    onRestorePurchase: () -> Unit = {},
) {
    val initialMode = when {
        accountState.premium -> PopoverPreviewMode.LIFETIME_PRO
        accountState.signedIn -> PopoverPreviewMode.SIGNED_IN_FREE
        else -> PopoverPreviewMode.GUEST
    }

    var previewMode by rememberSaveable { mutableStateOf(initialMode) }

    val isLifetime = previewMode == PopoverPreviewMode.LIFETIME_PRO
    val isSignedIn = previewMode != PopoverPreviewMode.GUEST

    val effectiveAccountState = when (previewMode) {
        PopoverPreviewMode.GUEST -> AccountState(account = null, premium = false)
        PopoverPreviewMode.SIGNED_IN_FREE -> AccountState(
            account = accountState.account ?: Account(
                id = "demo_id",
                email = "vivek.choudhury@gmail.com",
                displayName = "Vivek Choudhury",
                photoUrl = null,
            ),
            premium = false,
        )
        PopoverPreviewMode.LIFETIME_PRO -> AccountState(
            account = accountState.account ?: Account(
                id = "demo_id",
                email = "vivek.choudhury@gmail.com",
                displayName = "Vivek Choudhury",
                photoUrl = null,
            ),
            premium = true,
        )
    }

    val cardBorder = Color(0xFF20202A)
    val cardGlow = when {
        isLifetime -> Color(0x1A818CF8)
        isSignedIn -> Color(0x18818CF8)
        else -> Color(0x14F59E0B)
    }

    Box(
        modifier = modifier
            .width(350.dp)
            .shadow(
                elevation = 28.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0xCC000000),
                ambientColor = cardGlow,
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF131317), Color(0xFF0D0D10))
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color(0x2EFFFFFF), cardBorder)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            // DEV PREVIEW STATE SWITCHER (Segmented control)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF16161F))
                    .border(0.5.dp, Color(0xFF282838), RoundedCornerShape(8.dp))
                    .padding(2.5.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                DevPreviewPill(
                    label = "\uD83D\uDC64 Guest",
                    isSelected = previewMode == PopoverPreviewMode.GUEST,
                    modifier = Modifier.weight(1f)
                ) {
                    previewMode = PopoverPreviewMode.GUEST
                }
                DevPreviewPill(
                    label = "\u2709 Free",
                    isSelected = previewMode == PopoverPreviewMode.SIGNED_IN_FREE,
                    modifier = Modifier.weight(1f)
                ) {
                    previewMode = PopoverPreviewMode.SIGNED_IN_FREE
                }
                DevPreviewPill(
                    label = "\u2605 Pro",
                    isSelected = previewMode == PopoverPreviewMode.LIFETIME_PRO,
                    modifier = Modifier.weight(1f)
                ) {
                    previewMode = PopoverPreviewMode.LIFETIME_PRO
                }
            }

            // 1. Header (Identity, Avatar, Tier Badge)
            PopoverHeader(
                accountState = effectiveAccountState,
                isLifetime = isLifetime,
                isSignedIn = isSignedIn,
                onUpgrade = onUpgrade,
            )

            // 2. Unauthenticated Quick Sign-in Callout (Only in Guest mode)
            if (!isSignedIn) {
                GuestSignInCallout(onSignIn = onSignIn)
            }

            // 3. Cloud Sync & Quota / Storage Box
            CloudSyncQuotaBox(
                isLifetime = isLifetime,
                isSignedIn = isSignedIn,
                notesCount = notesCount,
                maxFreeNotes = maxFreeNotes,
            )

            // 4. Upgrade Pass / Lifetime Banner
            UpgradePassBanner(
                isLifetime = isLifetime,
                isSignedIn = isSignedIn,
                notesCount = notesCount,
                maxFreeNotes = maxFreeNotes,
                onUpgrade = onUpgrade,
                onRestorePurchase = onRestorePurchase,
            )

            // Divider
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF20202A))
            )

            // 5. Core Inking & Hardware Action List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                PopoverActionRow(
                    title = if (isLifetime) "Stylus & Inking Studio" else "Stylus & Pen Studio",
                    subtitle = "Pressure, Palm",
                    icon = { StylusPathIcon() },
                    onClick = onStylusStudio,
                )
                PopoverActionRow(
                    title = if (isLifetime) "Default Paper & Templates" else "Paper & Canvas Defaults",
                    subtitle = "Dot Grid, Dark",
                    icon = { PaperGridPathIcon() },
                    onClick = onPaperDefaults,
                )
                if (isLifetime) {
                    PopoverActionRow(
                        title = "Canvas Presentation",
                        trailingContent = {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF1B1B24))
                                    .border(0.5.dp, Color(0xFF282834), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Ready to cast",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                            Spacer(Modifier.width(5.dp))
                            RightChevronPathIcon()
                        },
                        icon = { CastPathIcon() },
                        onClick = onCanvasPresentation,
                    )
                }
                PopoverActionRow(
                    title = "App Settings & Theme",
                    trailingContent = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isSignedIn && !isLifetime) "Dark Obsidian" else "Dark",
                                fontSize = 11.sp,
                                color = Color(0xFFA1A1AA)
                            )
                            MoonPathIcon()
                        }
                    },
                    icon = { SlidersPathIcon() },
                    onClick = onAppSettings,
                )
            }

            // Divider
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF20202A))
            )

            // 6. Vault Export & Account Actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Export Vault Row
                PopoverActionRow(
                    title = if (isLifetime) "Export Full Vault (ZIP)" else if (isSignedIn) "Export Full Vault (.zip backup)" else "Export All Notes (.pnote backup)",
                    sublabel = if (isSignedIn || isLifetime) "Store offline copy of all .pnote files" else "Store offline copy on local disk",
                    icon = { VaultBoxPathIcon() },
                    trailingContent = {
                        DownloadArrowPathIcon()
                    },
                    onClick = onExportVault,
                )

                // Manage Account Row (Only for SignedIn Free)
                if (isSignedIn && !isLifetime) {
                    PopoverActionRow(
                        title = "Manage Account / Security",
                        subtitle = "Obsidian ID",
                        icon = { UserShieldPathIcon() },
                        onClick = onManageAccount,
                    )
                }

                // Sign Out Button (Only for SignedIn users)
                if (isSignedIn) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1F1316))
                            .border(0.5.dp, Color(0xFF3B181E), RoundedCornerShape(8.dp))
                            .clickable(onClick = onSignOut)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(Color(0xFF2E1318))
                                    .border(0.5.dp, Color(0xFF4F1A23), RoundedCornerShape(5.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                LogoutPathIcon()
                            }
                            Text(
                                text = "Sign Out",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF87171)
                            )
                        }
                    }
                }
            }

            // 7. Footer Watermark / Build Info
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                val footerText = when {
                    isLifetime -> "PNotes Obsidian Suite v2.4 \u00b7 1.8ms Low-Latency Engine"
                    isSignedIn -> "PNotes Suite v2.4 \u00b7 Signed in via Obsidian Cloud"
                    else -> "PNotes Free Edition v2.4 \u00b7 1.8ms Ink Latency"
                }
                Text(
                    text = footerText,
                    fontSize = 9.5.sp,
                    color = Color(0xFF52525B),
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}

// --- Header Sub-Component ---

@Composable
private fun PopoverHeader(
    accountState: AccountState,
    isLifetime: Boolean,
    isSignedIn: Boolean,
    onUpgrade: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Avatar + Names
        Row(
            modifier = Modifier.weight(1f, fill = false),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Circle (40x40dp) with status dot
            Box(modifier = Modifier.size(40.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                if (isSignedIn) listOf(Color(0xFF2E2E3A), Color(0xFF161620))
                                else listOf(Color(0xFF24242E), Color(0xFF14141A))
                            )
                        )
                        .border(
                            1.dp,
                            if (isSignedIn) Color(0x33FFFFFF) else Color(0x1FFFFFFF),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSignedIn) {
                        Text(
                            text = accountState.avatarLabel,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFAFAFA)
                        )
                    } else {
                        UserSilhouetteIcon()
                    }
                }
                // Status dot at bottom right
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(if (isSignedIn) Color(0xFF22C55E) else Color(0xFF71717A))
                        .border(1.dp, Color(0xFF131317), CircleShape)
                )
            }

            // Names
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                val displayName = if (isSignedIn) {
                    accountState.account?.displayName?.ifEmpty { "Vivek Choudhury" } ?: "Vivek Choudhury"
                } else {
                    "Guest User"
                }
                val subtext = if (isSignedIn) {
                    accountState.account?.email?.ifEmpty { "vivek.choudhury@gmail.com" } ?: "vivek.choudhury@gmail.com"
                } else {
                    "Saved locally on this device"
                }

                Text(
                    text = displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFAFAFA),
                    maxLines = 1
                )
                Text(
                    text = subtext,
                    fontSize = if (isSignedIn) 12.sp else 11.5.sp,
                    color = Color(0xFFA1A1AA),
                    maxLines = 1
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Right: Badge / Actions
        if (isLifetime) {
            // Gold Lifetime Badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF2D220E), Color(0xFF1B1406))
                        )
                    )
                    .border(0.5.dp, Color(0xFFD97706), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SparkleStarGoldIcon()
                Text(
                    text = "LIFETIME",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFBBF24),
                    letterSpacing = 0.5.sp,
                    maxLines = 1
                )
            }
        } else if (isSignedIn) {
            // Stidked Vertically: FREE badge on top, Upgrade ↗ button below (matches prototype exactly without line wrapping)
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1C1C24))
                        .border(0.5.dp, Color(0xFF383848), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "FREE",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA1A1AA),
                        letterSpacing = 0.5.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF4F46E5), Color(0xFF3730A3))
                            )
                        )
                        .clickable(onClick = onUpgrade)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Upgrade \u2197",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        } else {
            // Free Pill (Guest)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1C1C24))
                    .border(0.5.dp, Color(0xFF383848), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "LOCAL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA1A1AA),
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// --- Guest Quick Sign-in Callout ---

@Composable
private fun GuestSignInCallout(onSignIn: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF1C1B28), Color(0xFF13131C))
                )
            )
            .border(0.5.dp, Color(0xFF2E2C42), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SparkleIconPurple()
            Text(
                text = "Sign in to sync notes & prevent data loss",
                fontSize = 11.5.sp,
                color = Color(0xFFD4D4D8)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(7.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFFFFFFF), Color(0xFFE4E4E7))
                    )
                )
                .clickable(onClick = onSignIn)
                .padding(vertical = 7.dp, horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LoginArrowIcon()
                Text(
                    text = "Sign In / Create Obsidian ID",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F0F12)
                )
            }
        }
    }
}

// --- Cloud Sync & Quota Card ---

@Composable
private fun CloudSyncQuotaBox(
    isLifetime: Boolean,
    isSignedIn: Boolean,
    notesCount: Int,
    maxFreeNotes: Int,
) {
    val coroutineScope = rememberCoroutineScope()
    val rotation = remember { Animatable(0f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF181822))
            .border(0.5.dp, Color(0xFF262634), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        // Status Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dot
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (isLifetime || isSignedIn) Color(0xFF22C55E) else Color(0xFFF59E0B)
                        )
                )
                val statusText = when {
                    isLifetime -> "All notes synced \u00b7 Just now"
                    isSignedIn -> "Cloud Sync: Active (Synced 1m ago)"
                    else -> "Local Storage: Active (Offline)"
                }
                Text(
                    text = statusText,
                    fontSize = if (isSignedIn || isLifetime) 12.sp else 11.5.sp,
                    color = if (isLifetime || isSignedIn) Color(0xFFD4D4D8) else Color(0xFFA1A1AA)
                )
            }

            if (isLifetime || isSignedIn) {
                // Refresh Button
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFF20202C))
                        .border(0.5.dp, Color(0xFF303040), RoundedCornerShape(5.dp))
                        .clickable {
                            coroutineScope.launch {
                                rotation.snapTo(0f)
                                rotation.animateTo(
                                    targetValue = 360f,
                                    animationSpec = tween(durationMillis = 600, easing = LinearEasing)
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.rotate(rotation.value)) {
                        SyncRefreshIcon()
                    }
                }
            }
        }

        // Progress Bar
        val progressFraction = when {
            isLifetime -> 0.21f
            isSignedIn -> 0.60f
            else -> 0.30f
        }
        val barGradient = when {
            isLifetime -> Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF0EA5E9)))
            else -> Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFFB923C)))
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF23232C))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressFraction)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(barGradient)
                )
            }

            // Bar Label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val label = when {
                    isLifetime -> "420 MB of 10 GB used"
                    isSignedIn -> "Notes Quota: 6 of 10 free notes used"
                    else -> "Free Notes: $notesCount / $maxFreeNotes used"
                }
                val pct = when {
                    isLifetime -> "21%"
                    isSignedIn -> "60%"
                    else -> "30%"
                }
                val pctColor = if (isLifetime) Color(0xFF38BDF8) else Color(0xFFF59E0B)

                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = if (isLifetime) Color(0xFF71717A) else Color(0xFFD4D4D8)
                )
                Text(
                    text = pct,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = pctColor
                )
            }
        }
    }
}

// --- Upgrade / Lifetime Banner ---

@Composable
private fun UpgradePassBanner(
    isLifetime: Boolean,
    isSignedIn: Boolean,
    notesCount: Int,
    maxFreeNotes: Int,
    onUpgrade: () -> Unit,
    onRestorePurchase: () -> Unit,
) {
    if (isLifetime) {
        // Lifetime Pro Active State Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF1A1A28), Color(0xFF11111A))
                    )
                )
                .border(0.5.dp, Color(0xFF2E2C42), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        LightningBoltIcon()
                    }
                    Text(
                        text = "Active Lifetime License",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFAFAFA)
                    )
                }

                // Active badge for Pro user
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF14532D), Color(0xFF052E16))
                            )
                        )
                        .border(0.5.dp, Color(0xFF22C55E), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "VERIFIED \u2713",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4ADE80),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Text(
                text = "Unlimited cloud sync & all 50+ templates unlocked",
                fontSize = 11.sp,
                color = Color(0xFFA1A1AA),
                lineHeight = 15.sp
            )
        }
    } else if (isSignedIn) {
        // Signed-In Free Upgrade Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF1D1A2C), Color(0xFF12101D))
                    )
                )
                .border(0.5.dp, Color(0xFF3A3459), RoundedCornerShape(12.dp))
                .padding(horizontal = 13.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        LightningBoltIcon()
                    }
                    Text(
                        text = "Remove Limits with Lifetime Pass",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFAFAFA)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF2D220E), Color(0xFF1B1406))
                            )
                        )
                        .border(0.5.dp, Color(0xFFD97706), RoundedCornerShape(5.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "\u20b9249",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFBBF24)
                    )
                }
            }

            Text(
                text = "You have 4 free notes left. Get unlimited notes, 50+ paper templates & watermark-free exports.",
                fontSize = 11.sp,
                color = Color(0xFFA1A1AA),
                lineHeight = 15.sp
            )

            // Primary Upgrade Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(7.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF6366F1), Color(0xFF4F46E5))
                        )
                    )
                    .clickable(onClick = onUpgrade)
                    .padding(vertical = 7.dp, horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CrownPathIcon()
                    Text(
                        text = "Upgrade to Lifetime \u2014 \u20b9249",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            // Restore Purchase link
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onRestorePurchase),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Already purchased? Restore Purchase",
                    fontSize = 10.5.sp,
                    color = Color(0xFFA1A1AA)
                )
            }
        }
    } else {
        // Guest Free Upgrade Card with Checkmarks
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF1D1A2C), Color(0xFF12101D))
                    )
                )
                .border(0.5.dp, Color(0xFF3A3459), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        LightningBoltIcon()
                    }
                    Text(
                        text = "PNotes Lifetime Pass",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFAFAFA)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF2D220E), Color(0xFF1B1406))
                            )
                        )
                        .border(0.5.dp, Color(0xFFD97706), RoundedCornerShape(5.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "\u20b9249",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFBBF24)
                    )
                }
            }

            // 3 Value Checkmarks
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CheckmarkBulletRow("Unlimited notes & folders")
                CheckmarkBulletRow("50+ templates & watermark-free export")
                CheckmarkBulletRow("One-time payment \u00b7 No subscription")
            }

            // Primary Upgrade Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(7.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF6366F1), Color(0xFF4F46E5))
                        )
                    )
                    .clickable(onClick = onUpgrade)
                    .padding(vertical = 7.dp, horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CrownPathIcon()
                    Text(
                        text = "Get Lifetime Access \u2014 \u20b9249",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            // Restore Purchase link
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onRestorePurchase),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Already purchased? Restore Purchase",
                    fontSize = 10.5.sp,
                    color = Color(0xFFA1A1AA)
                )
            }
        }
    }
}

@Composable
private fun CheckmarkBulletRow(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CheckmarkCircleIcon()
        Text(
            text = text,
            fontSize = 11.sp,
            color = Color(0xFFD4D4D8)
        )
    }
}

// --- Menu Item Row ---

@Composable
private fun PopoverActionRow(
    title: String,
    subtitle: String? = null,
    sublabel: String? = null,
    icon: @Composable () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(if (isPressed) Color(0xFF1E1E2A) else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1C1C26))
                    .border(0.5.dp, Color(0xFF282836), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Column {
                Text(
                    text = title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE4E4E7)
                )
                if (sublabel != null) {
                    Text(
                        text = sublabel,
                        fontSize = 10.sp,
                        color = Color(0xFF71717A)
                    )
                }
            }
        }

        if (trailingContent != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                trailingContent()
            }
        } else if (subtitle != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color(0xFF71717A)
                )
                RightChevronPathIcon()
            }
        }
    }
}

// --- Helper for Precision SVG ViewBox Drawing ---

private fun DrawScope.drawSvgViewBox(
    viewBoxX: Float,
    viewBoxY: Float,
    viewBoxWidth: Float,
    viewBoxHeight: Float,
    block: DrawScope.() -> Unit
) {
    val scaleFactorX = size.width / viewBoxWidth
    val scaleFactorY = size.height / viewBoxHeight
    withTransform({
        scale(scaleFactorX, scaleFactorY, pivot = Offset.Zero)
        translate(-viewBoxX, -viewBoxY)
    }) {
        block()
    }
}

// --- Custom Vector Icons Matching Prototype SVGs Exactly ---

@Composable
fun StylusPathIcon() {
    Canvas(Modifier.size(13.dp)) {
        val path = PathParser().parsePathString(
            "M19.22,10.91 C19.4,11.09 19.5,11.33 19.5,11.59 C19.5,11.85 19.4,12.09 19.22,12.27 L17.51,13.99 L16.2,17.48 C16.08,17.8 15.79,18.03 15.45,18.09 L7.06,19.49 C7.03,19.49 7.01,19.5 6.98,19.5 C6.84,19.5 6.7,19.43 6.61,19.32 C6.52,19.21 6.49,19.07 6.51,18.93 L7.91,10.54 C7.97,10.2 8.2,9.92 8.52,9.8 L12.01,8.49 L13.72,6.78 C14.1,6.4 14.7,6.4 15.08,6.78 L19.22,10.91 Z M15.29,17.14 L8.39,18.29 L11.24,15.44 C11.98,15.84 12.89,15.64 13.39,14.97 C13.89,14.3 13.82,13.36 13.23,12.77 C12.64,12.18 11.7,12.11 11.03,12.61 C10.36,13.11 10.15,14.02 10.55,14.76 L7.7,17.61 L8.86,10.7 L12.16,9.46 L16.53,13.84 L15.29,17.14 Z M11.31,13.96 C11.31,13.56 11.64,13.24 12.04,13.24 C12.44,13.24 12.76,13.56 12.76,13.96 C12.76,14.36 12.44,14.68 12.04,14.68 C11.64,14.68 11.31,14.36 11.31,13.96 Z M17.09,13.04 L12.96,8.9 L14.4,7.46 L18.54,11.59 L17.09,13.04 Z"
        ).toPath()
        path.fillType = PathFillType.EvenOdd
        drawSvgViewBox(6.5f, 6.5f, 13f, 13f) {
            drawPath(path, color = Color(0xFFA1A1AA))
        }
    }
}

@Composable
fun PaperGridPathIcon() {
    Canvas(Modifier.size(13.dp)) {
        val path = PathParser().parsePathString(
            "M7.68,6.5 L18.32,6.5 C18.97,6.5 19.5,7.03 19.5,7.68 L19.5,18.32 C19.5,18.97 18.97,19.5 18.32,19.5 L7.68,19.5 C7.03,19.5 6.5,18.97 6.5,18.32 L6.5,7.68 C6.5,7.03 7.03,6.5 7.68,6.5 Z M18.32,12.41 L13.59,12.41 L13.59,7.68 L18.32,7.68 L18.32,12.41 Z M12.41,7.68 L12.41,12.41 L7.68,12.41 L7.68,7.68 L12.41,7.68 Z M7.68,13.59 L12.41,13.59 L12.41,18.32 L7.68,18.32 L7.68,13.59 Z M18.32,18.32 L13.59,18.32 L13.59,13.59 L18.32,13.59 L18.32,18.32 Z"
        ).toPath()
        path.fillType = PathFillType.EvenOdd
        drawSvgViewBox(6.5f, 6.5f, 13f, 13f) {
            drawPath(path, color = Color(0xFFA1A1AA))
        }
    }
}

@Composable
fun CastPathIcon() {
    Canvas(Modifier.size(width = 13.dp, height = 11.dp)) {
        val path1 = PathParser().parsePathString(
            "M19.5,8.5 L19.5,17.5 C19.5,18.05 19.05,18.5 18.5,18.5 L14,18.5 C13.72,18.5 13.5,18.28 13.5,18 C13.5,17.72 13.72,17.5 14,17.5 L18.5,17.5 L18.5,8.5 L7.5,8.5 L7.5,11 C7.5,11.28 7.28,11.5 7,11.5 C6.72,11.5 6.5,11.28 6.5,11 L6.5,8.5 C6.5,7.95 6.95,7.5 7.5,7.5 L18.5,7.5 C19.05,7.5 19.5,7.95 19.5,8.5 Z"
        ).toPath()
        val path2 = PathParser().parsePathString(
            "M6.5,17 C6.5,16.72 6.72,16.5 7,16.5 C7.83,16.5 8.5,17.17 8.5,18 C8.5,18.28 8.28,18.5 8,18.5 C7.72,18.5 7.5,18.28 7.5,18 C7.5,17.72 7.28,17.5 7,17.5 C6.72,17.5 6.5,17.28 6.5,17 Z"
        ).toPath()
        val path3 = PathParser().parsePathString(
            "M6.5,15 C6.5,14.72 6.72,14.5 7,14.5 C8.93,14.5 10.5,16.07 10.5,18 C10.5,18.28 10.28,18.5 10,18.5 C9.72,18.5 9.5,18.28 9.5,18 C9.5,16.62 8.38,15.5 7,15.5 C6.72,15.5 6.5,15.28 6.5,15 Z"
        ).toPath()
        val path4 = PathParser().parsePathString(
            "M6.5,13 C6.5,12.72 6.72,12.5 7,12.5 C10.04,12.5 12.5,14.96 12.5,18 C12.5,18.28 12.28,18.5 12,18.5 C11.72,18.5 11.5,18.28 11.5,18 C11.5,15.52 9.48,13.5 7,13.5 C6.72,13.5 6.5,13.28 6.5,13 Z"
        ).toPath()

        drawSvgViewBox(6.5f, 7.5f, 13f, 11f) {
            drawPath(path1, color = Color(0xFFA1A1AA))
            drawPath(path2, color = Color(0xFFA1A1AA))
            drawPath(path3, color = Color(0xFFA1A1AA))
            drawPath(path4, color = Color(0xFFA1A1AA))
        }
    }
}

@Composable
fun SlidersPathIcon() {
    Canvas(Modifier.size(width = 13.dp, height = 11.dp)) {
        val p1 = PathParser().parsePathString("M16.25,9.75 C16.25,9.45 16.49,9.21 16.79,9.21 L18.96,9.21 C19.26,9.21 19.5,9.45 19.5,9.75 C19.5,10.05 19.26,10.29 18.96,10.29 L16.79,10.29 C16.49,10.29 16.25,10.05 16.25,9.75 Z").toPath()
        val p2 = PathParser().parsePathString("M14.08,10.29 L7.04,10.29 C6.74,10.29 6.5,10.05 6.5,9.75 C6.5,9.45 6.74,9.21 7.04,9.21 L14.08,9.21 L14.08,8.12 C14.08,7.82 14.32,7.58 14.62,7.58 C14.92,7.58 15.17,7.82 15.17,8.12 L15.17,11.37 C15.17,11.67 14.92,11.92 14.62,11.92 C14.32,11.92 14.08,11.67 14.08,11.37 L14.08,10.29 Z").toPath()
        val p3 = PathParser().parsePathString("M12.46,15.71 L18.96,15.71 C19.26,15.71 19.5,15.95 19.5,16.25 C19.5,16.55 19.26,16.79 18.96,16.79 L12.46,16.79 C12.16,16.79 11.92,16.55 11.92,16.25 C11.92,15.95 12.16,15.71 12.46,15.71 Z").toPath()
        val p4 = PathParser().parsePathString("M9.75,14.62 C9.75,14.32 9.99,14.08 10.29,14.08 C10.59,14.08 10.83,14.32 10.83,14.62 L10.83,17.87 C10.83,18.17 10.59,18.42 10.29,18.42 C9.99,18.42 9.75,18.17 9.75,17.87 L9.75,16.79 L7.04,16.79 C6.74,16.79 6.5,16.55 6.5,16.25 C6.5,15.95 6.74,15.71 7.04,15.71 L9.75,15.71 L9.75,14.62 Z").toPath()

        drawSvgViewBox(6.5f, 7.58f, 13f, 10.84f) {
            drawPath(p1, color = Color(0xFFA1A1AA))
            drawPath(p2, color = Color(0xFFA1A1AA))
            drawPath(p3, color = Color(0xFFA1A1AA))
            drawPath(p4, color = Color(0xFFA1A1AA))
        }
    }
}

@Composable
fun MoonPathIcon() {
    Canvas(Modifier.size(width = 12.dp, height = 11.5.dp)) {
        val p1 = PathParser().parsePathString("M40.47,4.89 C40.47,5.15 40.27,5.35 40.01,5.35 L39.08,5.35 L39.08,6.27 C39.08,6.53 38.88,6.73 38.62,6.73 C38.36,6.73 38.16,6.53 38.16,6.27 L38.16,5.35 L37.24,5.35 C36.98,5.35 36.78,5.15 36.78,4.89 C36.78,4.63 36.98,4.42 37.24,4.42 L38.16,4.42 L38.16,3.5 C38.16,3.24 38.36,3.04 38.62,3.04 C38.88,3.04 39.08,3.24 39.08,3.5 L39.08,4.42 L40.01,4.42 C40.27,4.42 40.47,4.63 40.47,4.89 Z").toPath()
        val p2 = PathParser().parsePathString("M35.39,2.58 L34.93,2.58 C34.67,2.58 34.47,2.38 34.47,2.12 C34.47,1.86 34.67,1.65 34.93,1.65 L35.39,1.65 L35.39,1.19 C35.39,0.93 35.59,0.73 35.85,0.73 C36.11,0.73 36.31,0.93 36.31,1.19 L36.31,1.65 L36.78,1.65 C37.04,1.65 37.24,1.86 37.24,2.12 C37.24,2.38 37.04,2.58 36.78,2.58 L36.31,2.58 L36.31,3.04 C36.31,3.3 36.11,3.5 35.85,3.5 C35.59,3.5 35.39,3.3 35.39,3.04 L35.39,2.58 Z").toPath()
        val p3 = PathParser().parsePathString("M39.13,8.17 C39.24,8.3 39.27,8.48 39.21,8.64 C38.33,11.05 35.91,12.53 33.36,12.23 C30.81,11.93 28.81,9.92 28.51,7.37 C28.21,4.82 29.7,2.41 32.11,1.53 C32.27,1.47 32.44,1.5 32.57,1.61 C32.7,1.72 32.75,1.89 32.72,2.05 C32.39,3.71 32.91,5.43 34.11,6.63 C35.31,7.83 37.03,8.35 38.69,8.02 C38.85,7.99 39.02,8.04 39.13,8.17 Z M37.7,9.04 C37.8,9.04 37.91,9.04 38.01,9.03 L38.01,9.03 C37.04,10.71 35.12,11.62 33.21,11.28 C31.3,10.94 29.8,9.44 29.46,7.53 C29.12,5.62 30.03,3.7 31.71,2.73 C31.7,2.83 31.7,2.94 31.7,3.04 C31.7,6.35 34.39,9.04 37.7,9.04 Z").toPath()
        p3.fillType = PathFillType.EvenOdd

        drawSvgViewBox(28.47f, 0.73f, 12f, 11.54f) {
            drawPath(p1, color = Color(0xFF818CF8))
            drawPath(p2, color = Color(0xFF818CF8))
            drawPath(p3, color = Color(0xFF818CF8))
        }
    }
}

@Composable
fun VaultBoxPathIcon() {
    Canvas(Modifier.size(width = 13.dp, height = 9.3.dp)) {
        val p1 = PathParser().parsePathString("M7.43,8.36 L18.57,8.36 C19.08,8.36 19.5,8.78 19.5,9.29 L19.5,10.68 C19.5,11.19 19.08,11.61 18.57,11.61 L18.57,16.71 C18.57,17.22 18.15,17.64 17.64,17.64 L8.36,17.64 C7.85,17.64 7.43,17.22 7.43,16.71 L7.43,11.61 C6.92,11.61 6.5,11.19 6.5,10.68 L6.5,9.29 C6.5,8.78 6.92,8.36 7.43,8.36 Z M17.64,16.71 L8.36,16.71 L8.36,11.61 L17.64,11.61 L17.64,16.71 Z M18.57,10.68 L7.43,10.68 L7.43,9.29 L18.57,9.29 L18.57,10.68 Z").toPath()
        p1.fillType = PathFillType.EvenOdd
        val p2 = PathParser().parsePathString("M11.14,13.46 C11.14,13.2 11.35,13 11.61,13 L14.39,13 C14.65,13 14.86,13.2 14.86,13.46 C14.86,13.72 14.65,13.93 14.39,13.93 L11.61,13.93 C11.35,13.93 11.14,13.72 11.14,13.46 Z").toPath()

        drawSvgViewBox(6.5f, 8.36f, 13f, 9.28f) {
            drawPath(p1, color = Color(0xFFA1A1AA))
            drawPath(p2, color = Color(0xFFA1A1AA))
        }
    }
}

@Composable
fun DownloadArrowPathIcon() {
    Canvas(Modifier.size(13.dp)) {
        val p1 = PathParser().parsePathString("M314,21.12 L314,25.46 C314,25.76 313.76,26 313.46,26 L301.54,26 C301.24,26 301,25.76 301,25.46 L301,21.12 C301,20.82 301.24,20.58 301.54,20.58 C301.84,20.58 302.08,20.82 302.08,21.12 L302.08,24.92 L312.92,24.92 L312.92,21.12 C312.92,20.82 313.16,20.58 313.46,20.58 C313.76,20.58 314,20.82 314,21.12 Z").toPath()
        val p2 = PathParser().parsePathString("M307.5,21.67 C307.36,21.67 307.22,21.61 307.12,21.51 L304.41,18.8 C304.2,18.59 304.2,18.24 304.41,18.03 C304.62,17.82 304.96,17.82 305.17,18.03 L306.96,19.82 L306.96,13.54 C306.96,13.24 307.2,13 307.5,13 C307.8,13 308.04,13.24 308.04,13.54 L308.04,19.82 L309.83,18.03 C310.04,17.82 310.38,17.82 310.59,18.03 C310.8,18.24 310.8,18.59 310.59,18.8 L307.88,21.51 C307.78,21.61 307.64,21.67 307.5,21.67 Z").toPath()

        drawSvgViewBox(301f, 13f, 13f, 13f) {
            drawPath(p1, color = Color(0xFFA1A1AA))
            drawPath(p2, color = Color(0xFFA1A1AA))
        }
    }
}

@Composable
fun LogoutPathIcon() {
    Canvas(Modifier.size(13.dp)) {
        val p1 = PathParser().parsePathString("M9.92,16.96 C9.92,17.26 9.68,17.5 9.38,17.5 L5.05,17.5 C4.75,17.5 4.5,17.26 4.5,16.96 L4.5,5.04 C4.5,4.74 4.75,4.5 5.05,4.5 L9.38,4.5 C9.68,4.5 9.92,4.74 9.92,5.04 C9.92,5.34 9.68,5.59 9.38,5.59 L5.59,5.59 L5.59,16.41 L9.38,16.41 C9.68,16.41 9.92,16.66 9.92,16.96 Z").toPath()
        val p2 = PathParser().parsePathString("M14.64,7.91 L17.34,10.62 C17.44,10.72 17.5,10.86 17.5,11 C17.5,11.14 17.44,11.28 17.34,11.38 L14.64,14.09 C14.43,14.3 14.08,14.3 13.87,14.09 C13.66,13.88 13.66,13.53 13.87,13.32 L15.65,11.54 L9.38,11.54 C9.08,11.54 8.84,11.3 8.84,11 C8.84,10.7 9.08,10.46 9.38,10.46 L15.65,10.46 L13.87,8.68 C13.66,8.47 13.66,8.12 13.87,7.91 C14.08,7.7 14.43,7.7 14.64,7.91 Z").toPath()

        drawSvgViewBox(4.5f, 4.5f, 13f, 13f) {
            drawPath(p1, color = Color(0xFFF87171))
            drawPath(p2, color = Color(0xFFF87171))
        }
    }
}

@Composable
fun SyncRefreshIcon() {
    Canvas(Modifier.size(11.dp)) {
        val p1 = PathParser().parsePathString("M15.5,5.42 L15.5,8.16 C15.5,8.41 15.29,8.63 15.04,8.63 L12.29,8.63 C12.04,8.63 11.83,8.41 11.83,8.16 C11.83,7.91 12.04,7.71 12.29,7.71 L13.94,7.71 L13.1,6.88 C12.25,6.02 11.09,5.53 9.88,5.53 L9.86,5.53 C8.66,5.53 7.5,6 6.65,6.84 C6.47,7.01 6.18,7 6.01,6.82 C5.84,6.64 5.84,6.36 6.01,6.18 C8.17,4.07 11.62,4.1 13.75,6.23 L14.58,7.07 L14.58,5.42 C14.58,5.17 14.79,4.96 15.04,4.96 C15.29,4.96 15.5,5.17 15.5,5.42 Z").toPath()
        val p2 = PathParser().parsePathString("M6.9,13.14 C8.67,14.91 11.55,14.92 13.35,13.17 C13.53,13 13.82,13 13.99,13.18 C14.16,13.36 14.16,13.65 13.99,13.83 C12.97,14.84 11.58,15.39 10.14,15.39 L10.11,15.39 C8.66,15.39 7.27,14.8 6.25,13.77 L5.42,12.93 L5.42,14.59 C5.42,14.84 5.21,15.05 4.96,15.05 C4.71,15.05 4.5,14.84 4.5,14.59 L4.5,11.84 C4.5,11.59 4.71,11.38 4.96,11.38 L7.71,11.38 C7.96,11.38 8.17,11.59 8.17,11.84 C8.17,12.09 7.96,12.3 7.71,12.3 L6.06,12.3 L6.9,13.14 Z").toPath()

        drawSvgViewBox(4.5f, 4.62f, 11f, 10.77f) {
            drawPath(p1, color = Color(0xFFA1A1AA))
            drawPath(p2, color = Color(0xFFA1A1AA))
        }
    }
}

@Composable
fun LightningBoltIcon() {
    Canvas(Modifier.size(11.dp)) {
        val path = PathParser().parsePathString(
            "M15.18,9.29 C15.34,9.34 15.45,9.43 15.49,9.55 C15.53,9.67 15.46,9.79 15.35,9.88 L8.37,15.38 C8.22,15.5 7.97,15.53 7.77,15.46 C7.57,15.39 7.46,15.22 7.5,15.06 L8.42,11.7 L4.82,10.72 C4.66,10.67 4.55,10.57 4.51,10.45 C4.47,10.33 4.53,10.21 4.64,10.12 L11.63,4.62 C11.78,4.5 12.03,4.47 12.23,4.54 C12.43,4.61 12.53,4.78 12.49,4.94 L11.58,8.3 L15.18,9.29 Z M8.83,13.94 L9.48,11.54 C9.53,11.37 9.39,11.19 9.17,11.13 L5.89,10.22 L11.15,6.06 L10.51,8.46 C10.46,8.63 10.6,8.81 10.82,8.87 L14.13,9.78 L8.83,13.94 Z"
        ).toPath()
        path.fillType = PathFillType.EvenOdd
        drawSvgViewBox(4.5f, 4.5f, 11f, 11f) {
            drawPath(path, color = Color.White)
        }
    }
}

@Composable
fun CrownPathIcon() {
    Canvas(Modifier.size(width = 13.dp, height = 10.dp)) {
        val path = PathParser().parsePathString(
            "M69.55,11.11 C70.17,11.32 70.57,11.9 70.57,12.55 C70.57,13.31 70.03,13.97 69.27,14.06 L68.46,18.76 C68.39,19.18 68.02,19.48 67.6,19.48 L60.55,19.48 C60.13,19.48 59.77,19.18 59.7,18.76 L58.91,14.05 C58.24,13.97 57.7,13.44 57.6,12.77 C57.5,12.1 57.86,11.45 58.48,11.17 C59.1,10.89 59.82,11.05 60.26,11.57 C60.7,12.09 60.73,12.84 60.35,13.4 L61.8,15.19 L63.11,12.19 C62.62,11.78 62.44,11.11 62.66,10.51 C62.88,9.91 63.43,9.52 64.07,9.52 C64.71,9.52 65.3,9.91 65.52,10.51 C65.74,11.11 65.54,11.78 65.05,12.19 L66.35,15.19 L67.82,13.4 C67.45,12.86 67.46,12.15 67.85,11.63 C68.24,11.11 68.93,10.9 69.55,11.11 Z M64.07,10.38 C64.43,10.38 64.74,10.67 64.74,11.03 C64.74,11.39 64.43,11.68 64.07,11.68 C63.71,11.68 63.44,11.39 63.44,11.03 C63.44,10.67 63.71,10.38 64.07,10.38 Z M58.45,12.55 C58.45,12.19 58.74,11.9 59.1,11.9 C59.46,11.9 59.74,12.19 59.74,12.55 C59.74,12.91 59.46,13.2 59.1,13.2 C58.74,13.2 58.45,12.91 58.45,12.55 Z M67.6,18.62 L60.55,18.62 L59.8,14.1 L61.58,16.29 C61.66,16.39 61.79,16.45 61.92,16.45 C61.94,16.45 61.96,16.45 61.98,16.45 C62.13,16.43 62.26,16.33 62.32,16.19 L63.9,12.54 C64.02,12.56 64.15,12.56 64.27,12.54 L65.85,16.19 C65.91,16.33 66.04,16.43 66.19,16.45 C66.21,16.45 66.22,16.45 66.24,16.45 C66.37,16.45 66.49,16.39 66.57,16.29 L68.35,14.09 L67.6,18.62 Z M69.07,13.2 C68.71,13.2 68.41,12.91 68.41,12.55 C68.41,12.19 68.71,11.9 69.07,11.9 C69.43,11.9 69.71,12.19 69.71,12.55 C69.71,12.91 69.43,13.2 69.07,13.2 Z"
        ).toPath()
        path.fillType = PathFillType.EvenOdd
        drawSvgViewBox(57.58f, 9.52f, 12.99f, 9.96f) {
            drawPath(path, color = Color.White)
        }
    }
}

@Composable
fun SparkleStarGoldIcon() {
    Canvas(Modifier.size(10.dp)) {
        val p1 = PathParser().parsePathString("M13.6,8.4 L15.83,9.22 L15.83,9.22 C16.1,9.32 16.28,9.57 16.28,9.86 C16.28,10.15 16.1,10.41 15.83,10.51 L13.6,11.33 L12.78,13.55 C12.68,13.82 12.43,14 12.14,14 C11.85,14 11.59,13.82 11.49,13.55 L10.67,11.33 L8.45,10.5 C8.18,10.4 8,10.15 8,9.86 C8,9.57 8.18,9.31 8.45,9.21 L10.67,8.4 L11.5,6.17 C11.6,5.9 11.85,5.72 12.14,5.72 C12.43,5.72 12.69,5.9 12.79,6.17 L13.6,8.4 Z M13.22,10.73 C13.13,10.76 13.04,10.85 13.01,10.94 L12.14,13.3 L11.27,10.94 C11.24,10.84 11.16,10.76 11.06,10.73 L8.7,9.86 L11.06,8.99 C11.16,8.96 11.24,8.88 11.27,8.78 L12.14,6.42 L13.01,8.78 C13.04,8.87 13.12,8.96 13.21,8.99 L15.58,9.86 L13.22,10.73 Z").toPath()
        p1.fillType = PathFillType.EvenOdd
        val p2 = PathParser().parsePathString("M13.52,5.38 C13.52,5.19 13.67,5.03 13.86,5.03 L14.55,5.03 L14.55,4.34 C14.55,4.15 14.71,4 14.9,4 C15.09,4 15.24,4.15 15.24,4.34 L15.24,5.03 L15.93,5.03 C16.12,5.03 16.28,5.19 16.28,5.38 C16.28,5.57 16.12,5.72 15.93,5.72 L15.24,5.72 L15.24,6.41 C15.24,6.6 15.09,6.76 14.9,6.76 C14.71,6.76 14.55,6.6 14.55,6.41 L14.55,5.72 L13.86,5.72 C13.67,5.72 13.52,5.57 13.52,5.38 Z").toPath()
        val p3 = PathParser().parsePathString("M18,7.45 C18,7.64 17.85,7.79 17.66,7.79 L17.31,7.79 L17.31,8.14 C17.31,8.33 17.16,8.48 16.97,8.48 C16.78,8.48 16.62,8.33 16.62,8.14 L16.62,7.79 L16.28,7.79 C16.09,7.79 15.93,7.64 15.93,7.45 C15.93,7.26 16.09,7.1 16.28,7.1 L16.62,7.1 L16.62,6.76 C16.62,6.57 16.78,6.41 16.97,6.41 C17.16,6.41 17.31,6.57 17.31,6.76 L17.31,7.1 L17.66,7.1 C17.85,7.1 18,7.26 18,7.45 Z").toPath()

        drawSvgViewBox(8f, 4f, 10f, 10f) {
            drawPath(p1, color = Color(0xFFFBBF24))
            drawPath(p2, color = Color(0xFFFBBF24))
            drawPath(p3, color = Color(0xFFFBBF24))
        }
    }
}

@Composable
fun SparkleIconPurple() {
    Canvas(Modifier.size(12.dp)) {
        val p1 = PathParser().parsePathString("M6.73,6.27 L9.39,7.26 L9.39,7.26 C9.71,7.38 9.93,7.7 9.93,8.04 C9.93,8.38 9.71,8.69 9.39,8.81 L6.73,9.79 L5.74,12.46 C5.62,12.78 5.3,13 4.96,13 C4.62,13 4.31,12.78 4.19,12.46 L3.21,9.79 L0.54,8.8 C0.22,8.68 0,8.37 0,8.03 C0,7.69 0.22,7.38 0.54,7.26 L3.21,6.27 L4.2,3.61 C4.32,3.29 4.63,3.07 4.97,3.07 C5.31,3.07 5.62,3.29 5.74,3.61 L6.73,6.27 Z M6.26,9.08 C6.15,9.12 6.05,9.21 6.01,9.32 L4.97,12.16 L3.92,9.33 C3.88,9.22 3.78,9.12 3.67,9.08 L0.84,8.03 L3.67,6.99 C3.78,6.95 3.88,6.85 3.92,6.74 L4.97,3.9 L6.01,6.74 C6.05,6.85 6.15,6.95 6.26,6.99 L9.1,8.03 L6.26,9.08 Z").toPath()
        p1.fillType = PathFillType.EvenOdd
        val p2 = PathParser().parsePathString("M6.62,2.65 C6.62,2.42 6.81,2.24 7.04,2.24 L7.86,2.24 L7.86,1.41 C7.86,1.18 8.05,1 8.28,1 C8.51,1 8.69,1.18 8.69,1.41 L8.69,2.24 L9.52,2.24 C9.75,2.24 9.93,2.42 9.93,2.65 C9.93,2.88 9.75,3.07 9.52,3.07 L8.69,3.07 L8.69,3.9 C8.69,4.13 8.51,4.31 8.28,4.31 C8.05,4.31 7.86,4.13 7.86,3.9 L7.86,3.07 L7.04,3.07 C6.81,3.07 6.62,2.88 6.62,2.65 Z").toPath()
        val p3 = PathParser().parsePathString("M12,5.14 C12,5.37 11.82,5.55 11.59,5.55 L11.17,5.55 L11.17,5.96 C11.17,6.19 10.99,6.38 10.76,6.38 C10.53,6.38 10.35,6.19 10.35,5.96 L10.35,5.55 L9.93,5.55 C9.7,5.55 9.52,5.37 9.52,5.14 C9.52,4.91 9.7,4.72 9.93,4.72 L10.35,4.72 L10.35,4.31 C10.35,4.08 10.53,3.9 10.76,3.9 C10.99,3.9 11.17,4.08 11.17,4.31 L11.17,4.72 L11.59,4.72 C11.82,4.72 12,4.91 12,5.14 Z").toPath()

        drawSvgViewBox(0f, 1f, 12f, 12f) {
            drawPath(p1, color = Color(0xFF818CF8))
            drawPath(p2, color = Color(0xFF818CF8))
            drawPath(p3, color = Color(0xFF818CF8))
        }
    }
}

@Composable
fun LoginArrowIcon() {
    Canvas(Modifier.size(13.dp)) {
        val p1 = PathParser().parsePathString("M68.97,14.88 L66.26,17.59 C66.05,17.8 65.7,17.8 65.49,17.59 C65.28,17.38 65.28,17.04 65.49,16.83 L67.28,15.04 L61,15.04 C60.7,15.04 60.46,14.8 60.46,14.5 C60.46,14.2 60.7,13.96 61,13.96 L67.28,13.96 L65.49,12.17 C65.28,11.96 65.28,11.62 65.49,11.41 C65.7,11.2 66.05,11.2 66.26,11.41 L68.97,14.12 C69.07,14.22 69.13,14.36 69.13,14.5 C69.13,14.64 69.07,14.78 68.97,14.88 Z").toPath()
        val p2 = PathParser().parsePathString("M68.58,8 L72.92,8 C73.22,8 73.46,8.24 73.46,8.54 L73.46,20.46 C73.46,20.76 73.22,21 72.92,21 L68.58,21 C68.28,21 68.04,20.76 68.04,20.46 C68.04,20.16 68.28,19.92 68.58,19.92 L72.38,19.92 L72.38,9.08 L68.58,9.08 C68.28,9.08 68.04,8.84 68.04,8.54 C68.04,8.24 68.28,8 68.58,8 Z").toPath()

        drawSvgViewBox(60.46f, 8f, 13f, 13f) {
            drawPath(p1, color = Color(0xFF0F0F12))
            drawPath(p2, color = Color(0xFF0F0F12))
        }
    }
}

@Composable
fun UserShieldPathIcon() {
    Canvas(Modifier.size(width = 13.dp, height = 9.5.dp)) {
        val p1 = PathParser().parsePathString("M15.02,10.9 C15.47,12.48 14.82,14.17 13.43,15.04 C14.54,15.41 15.55,16.09 16.36,17.05 C16.51,17.23 16.48,17.51 16.3,17.66 C16.12,17.81 15.84,17.78 15.69,17.6 C14.6,16.3 13.11,15.59 11.48,15.59 C9.85,15.59 8.36,16.3 7.27,17.6 C7.17,17.72 7.01,17.79 6.86,17.76 C6.71,17.73 6.57,17.63 6.52,17.48 C6.47,17.33 6.51,17.17 6.61,17.05 C7.42,16.09 8.42,15.41 9.54,15.04 C8.15,14.17 7.5,12.48 7.95,10.9 C8.4,9.32 9.84,8.23 11.48,8.23 C13.12,8.23 14.57,9.32 15.02,10.9 Z M8.67,11.92 C8.67,10.37 9.93,9.1 11.48,9.1 C13.03,9.1 14.29,10.37 14.29,11.92 C14.29,13.47 13.03,14.73 11.48,14.73 C9.93,14.73 8.67,13.47 8.67,11.92 Z").toPath()
        p1.fillType = PathFillType.EvenOdd
        val p2 = PathParser().parsePathString("M19.03,14.1 L19.28,14.25 C19.45,14.35 19.54,14.54 19.49,14.73 C19.44,14.92 19.27,15.05 19.07,15.05 C18.99,15.05 18.92,15.04 18.85,15 L18.6,14.85 C18.48,14.95 18.34,15.04 18.19,15.09 L18.19,15.38 C18.19,15.62 18,15.81 17.76,15.81 C17.52,15.81 17.32,15.62 17.32,15.38 L17.32,15.09 C17.17,15.04 17.03,14.95 16.91,14.85 L16.66,15 C16.45,15.12 16.19,15.05 16.07,14.84 C15.95,14.63 16.02,14.37 16.23,14.25 L16.48,14.1 C16.45,13.94 16.45,13.79 16.48,13.63 L16.23,13.48 C16.02,13.36 15.95,13.1 16.07,12.89 C16.19,12.68 16.45,12.61 16.66,12.73 L16.91,12.88 C17.03,12.78 17.17,12.69 17.32,12.64 L17.32,12.35 C17.32,12.11 17.52,11.92 17.76,11.92 C18,11.92 18.19,12.11 18.19,12.35 L18.19,12.64 C18.34,12.69 18.48,12.78 18.6,12.88 L18.85,12.73 C19.06,12.61 19.32,12.68 19.44,12.89 C19.56,13.1 19.49,13.36 19.28,13.48 L19.03,13.63 C19.06,13.79 19.06,13.94 19.03,14.1 Z M17.32,13.86 C17.32,13.62 17.52,13.43 17.76,13.43 C18,13.43 18.19,13.62 18.19,13.86 C18.19,14.1 18,14.3 17.76,14.3 C17.52,14.3 17.32,14.1 17.32,13.86 Z").toPath()
        p2.fillType = PathFillType.EvenOdd

        drawSvgViewBox(6.5f, 8.23f, 13f, 9.54f) {
            drawPath(p1, color = Color(0xFFA1A1AA))
            drawPath(p2, color = Color(0xFFA1A1AA))
        }
    }
}

@Composable
fun UserSilhouetteIcon() {
    Canvas(Modifier.size(width = 18.dp, height = 17.3.dp)) {
        val path = PathParser().parsePathString(
            "M23.18,22.92 C25.55,23.69 27.58,25.32 28.9,27.6 C29.03,27.81 29.03,28.09 28.91,28.31 C28.79,28.53 28.55,28.66 28.3,28.66 C28.05,28.66 27.82,28.52 27.7,28.3 C26.07,25.49 23.19,23.8 20,23.8 C16.81,23.8 13.93,25.49 12.3,28.3 C12.18,28.52 11.95,28.66 11.7,28.66 C11.45,28.66 11.21,28.53 11.09,28.31 C10.97,28.09 10.97,27.81 11.1,27.6 C12.42,25.32 14.45,23.69 16.82,22.92 C14.41,21.49 13.26,18.62 14,15.92 C14.74,13.22 17.2,11.35 20,11.35 C22.8,11.35 25.26,13.22 26,15.92 C26.74,18.62 25.59,21.49 23.18,22.92 Z M15.16,17.56 C15.16,14.89 17.33,12.74 20,12.74 C22.67,12.74 24.84,14.89 24.84,17.56 C24.84,20.23 22.67,22.42 20,22.42 C17.33,22.42 15.16,20.23 15.16,17.56 Z"
        ).toPath()
        path.fillType = PathFillType.EvenOdd

        drawSvgViewBox(11f, 11.35f, 18f, 17.31f) {
            drawPath(path, color = Color(0xFFA1A1AA))
        }
    }
}

@Composable
fun CheckmarkCircleIcon() {
    Canvas(Modifier.size(12.dp)) {
        val p1 = PathParser().parsePathString("M8.63,4.79 C8.72,4.88 8.77,5 8.77,5.12 C8.77,5.24 8.72,5.35 8.63,5.44 L5.4,8.67 C5.31,8.76 5.2,8.81 5.08,8.81 C4.96,8.81 4.84,8.76 4.75,8.67 L3.37,7.29 C3.19,7.11 3.19,6.81 3.37,6.63 C3.55,6.45 3.84,6.45 4.02,6.63 L5.08,7.69 L7.98,4.79 C8.07,4.7 8.19,4.65 8.31,4.65 C8.43,4.65 8.54,4.7 8.63,4.79 Z").toPath()
        val p2 = PathParser().parsePathString("M12,6.5 C12,9.81 9.31,12.5 6,12.5 C2.69,12.5 0,9.81 0,6.5 C0,3.19 2.69,0.5 6,0.5 C9.31,0.5 12,3.19 12,6.5 Z M6,1.42 C8.8,1.42 11.08,3.7 11.08,6.5 C11.08,9.3 8.8,11.58 6,11.58 C3.2,11.58 0.92,9.3 0.92,6.5 C0.92,3.7 3.2,1.42 6,1.42 Z").toPath()
        p2.fillType = PathFillType.EvenOdd

        drawSvgViewBox(0f, 0.5f, 12f, 12f) {
            drawPath(p1, color = Color(0xFF818CF8))
            drawPath(p2, color = Color(0xFF818CF8))
        }
    }
}

@Composable
fun RightChevronPathIcon() {
    Canvas(Modifier.size(width = 3.5.dp, height = 7.dp)) {
        val path = Path().apply {
            moveTo(0.5f, 0.5f)
            lineTo(size.width - 0.5f, size.height / 2f)
            lineTo(0.5f, size.height - 0.5f)
        }
        drawPath(
            path,
            color = Color(0xFF71717A),
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun DevPreviewPill(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .then(
                if (isSelected) {
                    Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF2E2E40), Color(0xFF1E1E2C))
                            )
                        )
                        .border(0.5.dp, Color(0xFF4C4C66), RoundedCornerShape(6.dp))
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) Color(0xFFFAFAFA) else Color(0xFF71717A)
        )
    }
}
