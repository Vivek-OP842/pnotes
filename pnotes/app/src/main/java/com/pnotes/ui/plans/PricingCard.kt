package com.pnotes.ui.plans

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pnotes.ui.icons.PnotesIcons
import com.pnotes.ui.theme.LocalPalette

enum class PlanTier {
    FREE,
    PRO_3K,
    PRO_5K,
}

object PlanIcons {
    private fun strokeIcon(strokeWidth: Float = 1.5f, vararg pathData: String): ImageVector {
        val nodes = PathParser().parsePathString(pathData.joinToString(" ")).toNodes()
        return ImageVector.Builder(
            name = "plan_stroke",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(
            pathData = nodes,
            stroke = SolidColor(Color.White),
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ).build()
    }

    val gift = strokeIcon(
        1.5f,
        "M20 12v8a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-8",
        "M2 7h20v5H2z",
        "M12 22V7",
        "M12 7H7.5a2.5 2.5 0 0 1 0-5C11 2 12 7 12 7z",
        "M12 7h4.5a2.5 2.5 0 0 0 0-5C13 2 12 7 12 7z",
    )

    val star = strokeIcon(
        1.5f,
        "M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z",
    )

    val diamond = strokeIcon(
        1.5f,
        "M6 3h12l4 6-10 12L2 9z",
        "M2 9h20",
        "M10 3l2 6-4 12",
        "M14 3l-2 6 4 12",
    )

    val crown = strokeIcon(
        1.8f,
        "M2 4l3 12h14l3-12-5 4-5-6-5 6z",
        "M3 20h18",
    )

    val shieldCheck = strokeIcon(
        1.5f,
        "M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z",
        "M9 12l2 2 4-4",
    )
}

@Composable
fun PricingCard(
    tier: PlanTier,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit = {},
) {
    val isDark = LocalPalette.current.isDark
    val isFree = tier == PlanTier.FREE
    val accent = PlansTokens.CoralAccent

    val cardBg = if (isDark) PlansTokens.DarkCardBg else PlansTokens.LightCardBg
    val textPrimary = if (isDark) PlansTokens.DarkTextPrimary else PlansTokens.LightTextPrimary
    val textSecondary = if (isDark) PlansTokens.DarkTextSecondary else PlansTokens.LightTextSecondary
    val textMuted = if (isDark) PlansTokens.DarkTextMuted else PlansTokens.LightTextMuted
    val borderColor = if (isFree) accent else (if (isDark) PlansTokens.DarkCardBorder else PlansTokens.LightCardBorder)
    val borderWidth = if (isFree) 1.8.dp else 1.dp
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .then(
                if (isFree) {
                    Modifier.drawBehind {
                        // Soft ambient monochrome glow around the Free card
                        val glowAlpha = if (isDark) 0.18f else 0.08f
                        drawRoundRect(
                            color = accent.copy(alpha = glowAlpha),
                            topLeft = Offset(-6.dp.toPx(), -6.dp.toPx()),
                            size = Size(size.width + 12.dp.toPx(), size.height + 12.dp.toPx()),
                            cornerRadius = CornerRadius(28.dp.toPx()),
                        )
                    }
                } else if (!isDark) {
                    Modifier.shadow(elevation = 4.dp, shape = shape, spotColor = Color(0x14000000))
                } else Modifier
            )
            .clip(shape)
            .background(cardBg)
            .border(borderWidth, borderColor, shape)
            .padding(24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Header Row: Badge (Free) or Spacer + Top-Right Icon
            Row(
                modifier = Modifier.fillMaxWidth().height(36.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isFree) {
                    // "BEST CHOICE" Pill Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PlansTokens.CoralAccent,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                imageVector = PlanIcons.crown,
                                contentDescription = null,
                                tint = Color(0xFF181B20),
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = "BEST CHOICE",
                                color = Color(0xFF181B20),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp,
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                // Top-Right Icon Container
                val topIcon = when (tier) {
                    PlanTier.FREE -> PlanIcons.gift
                    PlanTier.PRO_3K -> PlanIcons.star
                    PlanTier.PRO_5K -> PlanIcons.diamond
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                        .border(
                            1.dp,
                            if (isFree) PlansTokens.CoralAccent.copy(alpha = 0.45f)
                            else (if (isDark) Color(0x18FFFFFF) else Color(0xFFE2E8F0)),
                            RoundedCornerShape(12.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = topIcon,
                        contentDescription = null,
                        tint = PlansTokens.CoralAccent,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Plan Title & Subtitle
            Text(
                text = when (tier) {
                    PlanTier.FREE -> "Free"
                    PlanTier.PRO_3K -> "Pro — 3K"
                    PlanTier.PRO_5K -> "Pro — 5K"
                },
                color = textPrimary,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = when (tier) {
                    PlanTier.FREE -> "Everything you need, always free."
                    PlanTier.PRO_3K -> "One-time payment"
                    PlanTier.PRO_5K -> "One-time payment"
                },
                color = if (isFree) textSecondary else PlansTokens.CoralAccent.copy(alpha = 0.90f),
                fontSize = 13.5.sp,
            )

            Spacer(Modifier.height(18.dp))

            // Price Row
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = when (tier) {
                        PlanTier.FREE -> "₹0"
                        PlanTier.PRO_3K -> "₹2999"
                        PlanTier.PRO_5K -> "₹4999"
                    },
                    color = textPrimary,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = when (tier) {
                        PlanTier.FREE -> "Forever"
                        PlanTier.PRO_3K -> "Lifetime Access"
                        PlanTier.PRO_5K -> "Lifetime Access"
                    },
                    color = textMuted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = if (isDark) Color(0x18FFFFFF) else Color(0xFFE2E8F0))
            Spacer(Modifier.height(16.dp))

            // Features List with Subtle Diagonal Strike for Paid Cards
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(13.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val features = listOf(
                        "Unlimited Canvas",
                        "Unlimited Notes",
                        "Unlimited Templates",
                        "Unlimited Pages",
                        "All Features",
                        "No Ads",
                        if (isFree) "No Limits, Ever!" else "Lifetime Access",
                    )

                    features.forEach { feature ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            // Monochrome Checkmark Circle Icon
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color.White else Color(0xFF09090B)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = PnotesIcons.check,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF09090B) else Color.White,
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                            Text(
                                text = feature,
                                color = textPrimary,
                                fontSize = 14.5.sp,
                                fontWeight = if (feature.startsWith("Unlimited")) FontWeight.Medium else FontWeight.Normal,
                            )
                        }
                    }
                }

                // Subtle diagonal cross-out line for paid tiers (communicating optional tier)
                if (!isFree) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawLine(
                            color = Color(0x458A92A0),
                            start = Offset(-4.dp.toPx(), size.height * 0.70f),
                            end = Offset(size.width + 4.dp.toPx(), size.height * 0.12f),
                            strokeWidth = 2.0f,
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Bottom Button
            if (isFree) {
                // Free Button: disabled look
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(if (isDark) PlansTokens.DarkButtonBg else PlansTokens.LightButtonBg)
                        .border(1.2.dp, if (isDark) PlansTokens.DarkButtonBorder else PlansTokens.LightButtonBorder, RoundedCornerShape(26.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "You're on Free plan 🎉",
                        color = PlansTokens.CoralAccent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            } else {
                // Paid Support button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(if (isDark) Color(0x18FFFFFF) else Color(0xFFF1F5F9))
                        .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFCBD5E1), RoundedCornerShape(26.dp))
                        .clickable(onClick = onSelect),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Currently not needed 😊",
                        color = textSecondary,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}
