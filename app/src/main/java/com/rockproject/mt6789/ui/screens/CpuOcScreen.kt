package com.rockproject.mt6789.ui.screens

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
import com.rockproject.mt6789.ui.components.OcActionButtons
import com.rockproject.mt6789.ui.theme.RockTheme

@Composable
fun CpuOcScreen(
    cpuLittleValue: Int,
    cpuBigValue: Int,
    cpuPrimeValue: Int = 0,
    stockFloors: StockFloors,
    isBusy: Boolean,
    onCpuLittleChanged: (Int) -> Unit,
    onCpuBigChanged: (Int) -> Unit,
    onCpuPrimeChanged: (Int) -> Unit = {},
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

    val littleMin = stockFloors.cpuLittleMin ?: cpuLittleValue.coerceAtMost(KernelConstants.CPU_LITTLE_MAX_MHZ)
    val bigMin = stockFloors.cpuBigMin ?: cpuBigValue.coerceAtMost(KernelConstants.CPU_BIG_MAX_MHZ)
    val primeMin = stockFloors.cpuPrimeMin ?: cpuPrimeValue.coerceAtMost(KernelConstants.CPU_PRIME_MAX_MHZ)
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
                value = cpuLittleValue,
                rangeMin = littleMin,
                rangeMax = KernelConstants.CPU_LITTLE_MAX_MHZ,
                step = 5,
                unit = "MHz",
                onValueChange = onCpuLittleChanged,
                onValueChangeFinished = onSliderDragEnd
            )

            HorizontalDivider(
                color = colors.separator,
                thickness = 0.5.dp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            CustomSliderField(
                label = "Big Cluster",
                value = cpuBigValue,
                rangeMin = bigMin,
                rangeMax = KernelConstants.CPU_BIG_MAX_MHZ,
                step = 5,
                unit = "MHz",
                onValueChange = onCpuBigChanged,
                onValueChangeFinished = onSliderDragEnd
            )

            if (stockFloors.isPrimeActive && stockFloors.cpuPrimeMin != null) {
                HorizontalDivider(
                    color = colors.separator,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                CustomSliderField(
                    label = "Prime Cluster",
                    value = cpuPrimeValue,
                    rangeMin = primeMin,
                    rangeMax = KernelConstants.CPU_PRIME_MAX_MHZ,
                    step = 5,
                    unit = "MHz",
                    onValueChange = onCpuPrimeChanged,
                    onValueChangeFinished = onSliderDragEnd
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "No explicit voltage - SVS handles voltage autonomously. Kernel enforces <= stock +60%.",
                color = colors.labelTertiary,
                fontSize = 12.5.sp,
                lineHeight = 17.sp
            )

            OcActionButtons(
                isRestoreVisible = isRestoreVisible,
                isApplyVisible = isDirty,
                canApply = canApply,
                onRestore = onRestore,
                onApply = onApply
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
