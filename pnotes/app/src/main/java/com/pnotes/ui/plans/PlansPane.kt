package com.pnotes.ui.plans

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun PlansPane(
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val scrollState = rememberScrollState()

    fun onSupportClicked(planName: String) {
        Toast.makeText(ctx, "Thank you for supporting PNotes! ($planName)", Toast.LENGTH_SHORT).show()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        val isWide = maxWidth >= 760.dp

        Column(
            modifier = Modifier
                .widthIn(max = 1200.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            // Hero Section (Heading + Flare + Subtitle + Illustration)
            PlansHero(showIllustration = true)

            // Pricing Cards Section
            if (isWide) {
                // Landscape Tablet: 3 equal-width columns side by side
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(580.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    PricingCard(
                        tier = PlanTier.FREE,
                        modifier = Modifier.weight(1f),
                    )
                    PricingCard(
                        tier = PlanTier.PRO_3K,
                        modifier = Modifier.weight(1f),
                        onSelect = { onSupportClicked("Pro — 3K") },
                    )
                    PricingCard(
                        tier = PlanTier.PRO_5K,
                        modifier = Modifier.weight(1f),
                        onSelect = { onSupportClicked("Pro — 5K") },
                    )
                }
            } else {
                // Portrait Tablet / Phone: Vertically stacked cards
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    PricingCard(
                        tier = PlanTier.FREE,
                        modifier = Modifier.fillMaxWidth().height(540.dp),
                    )
                    PricingCard(
                        tier = PlanTier.PRO_3K,
                        modifier = Modifier.fillMaxWidth().height(540.dp),
                        onSelect = { onSupportClicked("Pro — 3K") },
                    )
                    PricingCard(
                        tier = PlanTier.PRO_5K,
                        modifier = Modifier.fillMaxWidth().height(540.dp),
                        onSelect = { onSupportClicked("Pro — 5K") },
                    )
                }
            }

            // Unlimited Reassurance Panel
            UnlimitedReassurancePanel(
                modifier = Modifier.widthIn(max = 940.dp),
            )

            // Trust Footer
            TrustFooter()

            Spacer(Modifier.height(24.dp))
        }
    }
}
