package com.pnotes.ui

import androidx.compose.runtime.Composable
import com.pnotes.ui.plans.PlansPane

/**
 * Backstage pane for Plans & Support (formerly About).
 * Reuses the existing About destination in Backstage routing, rendering the new Plans experience.
 */
@Composable
fun AboutPane() {
    PlansPane()
}
