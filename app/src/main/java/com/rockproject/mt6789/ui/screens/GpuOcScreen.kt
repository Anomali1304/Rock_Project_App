package com.rockproject.mt6789.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rockproject.mt6789.data.KernelConstants
import com.rockproject.mt6789.model.StockFloors
import com.rockproject.mt6789.ui.components.CardWithBadge
import com.rockproject.mt6789.ui.components.CustomSliderField
import com.rockproject.mt6789.ui.components.PillButton
import com.rockproject.mt6789.ui.theme.RockTheme

@Composable
fun GpuOcScreen(
    gpuFreqValue: Int,
    gpuVoltValue: Int,
    stockFloors: StockFloors,
    isBusy: Boolean,
    onGpuFreqChanged: (Int) -> Unit,
    onGpuVoltChanged: (Int) -> Unit,
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

    val freqMin = stockFloors.gpuFreqMin ?: gpuFreqValue.coerceAtMost(KernelConstants.GPU_FREQ_MAX_MHZ)
    val voltMin = stockFloors.gpuVoltMin ?: gpuVoltValue.coerceAtMost(KernelConstants.GPU_VOLT_MAX_MV)
    val canApply = stockFloors.isGpuKnown && !isBusy

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CardWithBadge(
            badgeText = "GPU",
            badgeIcon = Icons.Default.DeveloperBoard
        ) {
            CustomSliderField(
                label = "Target frequency",
                value = gpuFreqValue,
                rangeMin = freqMin,
                rangeMax = KernelConstants.GPU_FREQ_MAX_MHZ,
                step = 5,
                unit = "MHz",
                onValueChange = onGpuFreqChanged,
                onValueChangeFinished = onSliderDragEnd
            )

            HorizontalDivider(
                color = colors.separator,
                thickness = 0.5.dp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            CustomSliderField(
                label = "Target voltage",
                value = gpuVoltValue,
                rangeMin = voltMin,
                rangeMax = KernelConstants.GPU_VOLT_MAX_MV,
                step = 1,
                unit = "mV",
                onValueChange = onGpuVoltChanged,
                onValueChangeFinished = onSliderDragEnd
            )

            HorizontalDivider(
                color = colors.separator,
                thickness = 0.5.dp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Target vsram row linked to voltage
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Target vsram",
                        color = colors.label,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Linked to voltage",
                        color = colors.labelTertiary,
                        fontSize = 12.5.sp
                    )
                }
                Text(
                    text = "$gpuVoltValue.00 mV",
                    color = colors.blue,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

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
