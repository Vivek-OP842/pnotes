package com.pnotes.ui.plans

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pnotes.ui.theme.LocalPalette

@Composable
fun UnlimitedReassurancePanel(
    modifier: Modifier = Modifier,
) {
    val isDark = LocalPalette.current.isDark
    val bg = if (isDark) PlansTokens.DarkReassuranceBg else PlansTokens.LightReassuranceBg
    val textPrimary = if (isDark) PlansTokens.DarkTextPrimary else PlansTokens.LightTextPrimary
    val textSecondary = if (isDark) PlansTokens.DarkTextSecondary else PlansTokens.LightTextSecondary
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .border(1.2.dp, PlansTokens.CoralAccent.copy(alpha = 0.28f), shape)
            .padding(horizontal = 24.dp, vertical = 18.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // Shield Icon with Check in Coral
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(PlansTokens.CoralAccent.copy(alpha = 0.15f))
                    .border(1.dp, PlansTokens.CoralAccent.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PlanIcons.shieldCheck,
                    contentDescription = null,
                    tint = PlansTokens.CoralAccent,
                    modifier = Modifier.size(24.dp),
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = "Your creativity has no limits. Neither do we.",
                    color = textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "All plans give you unlimited access. Upgrade only if you love PNotes and want to support us. ❤️",
                    color = textSecondary,
                    fontSize = 13.5.sp,
                )
            }
        }
    }
}
