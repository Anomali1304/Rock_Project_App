package com.rockproject.mt6789.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rockproject.mt6789.ui.components.GlassCard
import com.rockproject.mt6789.ui.components.MenuListRow
import com.rockproject.mt6789.ui.theme.RockTheme

@Composable
fun CpuMenuScreen(
    onNavigateCpuOc: () -> Unit,
    onNavigateCpuControl: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = RockTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        GlassCard {
            MenuListRow(
                icon = Icons.Default.Bolt,
                iconBgColor = colors.blue,
                title = "Overclock",
                onClick = onNavigateCpuOc
            )
            MenuListRow(
                icon = Icons.Default.Tune,
                iconBgColor = colors.purple,
                title = "Control",
                onClick = onNavigateCpuControl,
                showDivider = false
            )
        }
    }
}
