package com.pnotes.ui.plans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pnotes.ui.theme.LocalPalette

object TrustIcons {
    private fun strokeIcon(strokeWidth: Float = 1.5f, vararg pathData: String): ImageVector {
        val nodes = PathParser().parsePathString(pathData.joinToString(" ")).toNodes()
        return ImageVector.Builder(
            name = "trust_stroke",
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

    val lock = strokeIcon(
        1.5f,
        "M19 11H5a2 2 0 0 0-2 2v7a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7a2 2 0 0 0-2-2z",
        "M7 11V7a5 5 0 0 1 10 0v4",
    )

    val shield = strokeIcon(
        1.5f,
        "M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z",
        "M9 12l2 2 4-4",
    )

    val rotate = strokeIcon(
        1.5f,
        "M21.5 2v6h-6",
        "M21.34 15.57a10 10 0 1 1-.57-8.38l.73-.73",
    )
}

@Composable
fun TrustFooter(
    modifier: Modifier = Modifier,
) {
    val isDark = LocalPalette.current.isDark
    val textMuted = if (isDark) PlansTokens.DarkTextMuted else PlansTokens.LightTextMuted

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrustItem(TrustIcons.lock, "Secure Payment", textMuted)
        Spacer(Modifier.width(22.dp))
        VerticalDivider(modifier = Modifier.height(14.dp), color = textMuted.copy(alpha = 0.25f))
        Spacer(Modifier.width(22.dp))
        TrustItem(TrustIcons.shield, "100% Safe & Private", textMuted)
        Spacer(Modifier.width(22.dp))
        VerticalDivider(modifier = Modifier.height(14.dp), color = textMuted.copy(alpha = 0.25f))
        Spacer(Modifier.width(22.dp))
        TrustItem(TrustIcons.rotate, "Cancel Anytime", textMuted)
    }
}

@Composable
private fun TrustItem(icon: ImageVector, text: String, textColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PlansTokens.CoralAccent,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
        )
    }
}
