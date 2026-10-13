package com.rockproject.mt6789.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rockproject.mt6789.data.KernelConstants
import com.rockproject.mt6789.model.StockFloors
import com.rockproject.mt6789.ui.components.CardWithBadge
import com.rockproject.mt6789.ui.components.CustomSliderField
import com.rockproject.mt6789.ui.components.PillButton
import com.rockproject.mt6789.ui.theme.RockTheme

@Composable
fun CpuOcScreen(
    cpuLLValue: Int,
    cpuBValue: Int,
    stockFloors: StockFloors,
    isBusy: Boolean,
    onCpuLLChanged: (Int) -> Unit,
    onCpuBChanged: (Int) -> Unit,
    onSliderDragStart: () -> Unit,
    onSliderDragEnd: () -> Unit,
    onApply: () -> Unit,
    onRestore: () -> Unit,
    isDirty: Boolean,
    isRestoreVisible: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = RockTheme.colors
    val scrollState = rememberScrollState()

    val llMin = stockFloors.cpuLLMin ?: cpuLLValue.coerceAtMost(KernelConstants.CPU_LL_MAX_MHZ)
    val bMin = stockFloors.cpuBMin ?: cpuBValue.coerceAtMost(KernelConstants.CPU_B_MAX_MHZ)
    val canApply = stockFloors.isCpuKnown && !isBusy

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CardWithBadge(
            badgeText = "CPU",
            badgeIcon = Icons.Default.Memory
        ) {
            CustomSliderField(
                label = "Little Cluster",
                value = cpuLLValue,
                rangeMin = llMin,
                rangeMax = KernelConstants.CPU_LL_MAX_MHZ,
                step = 5,
                unit = "MHz",
                onValueChange = onCpuLLChanged,
                onValueChangeFinished = onSliderDragEnd
            )

            HorizontalDivider(
                color = colors.separator,
                thickness = 0.5.dp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            CustomSliderField(
                label = "Big Cluster",
                value = cpuBValue,
                rangeMin = bMin,
                rangeMax = KernelConstants.CPU_B_MAX_MHZ,
                step = 5,
                unit = "MHz",
                onValueChange = onCpuBChanged,
                onValueChangeFinished = onSliderDragEnd
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "No explicit voltage - SVS handles voltage autonomously. Kernel enforces <= stock +60%.",
                color = colors.labelTertiary,
                fontSize = 12.5.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnimatedVisibility(
                    visible = isRestoreVisible,
                    enter = fadeIn(animationSpec = tween(200)),
                    exit = fadeOut(animationSpec = tween(200))
                ) {
                    PillButton(
                        text = "Restore stock",
                        isPrimary = false,
                        enabled = canApply,
                        onClick = onRestore,
                        modifier = Modifier.weight(1f)
                    )
                }
                AnimatedVisibility(
                    visible = isDirty,
                    enter = fadeIn(animationSpec = tween(200)),
                    exit = fadeOut(animationSpec = tween(200))
                ) {
                    PillButton(
                        text = "Apply",
                        isPrimary = true,
                        enabled = canApply,
                        onClick = onApply,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
