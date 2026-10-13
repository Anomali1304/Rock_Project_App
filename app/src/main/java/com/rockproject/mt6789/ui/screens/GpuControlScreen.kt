package com.rockproject.mt6789.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rockproject.mt6789.model.GpuControlState
import com.rockproject.mt6789.ui.components.BottomSheetPicker
import com.rockproject.mt6789.ui.components.CardWithBadge
import com.rockproject.mt6789.ui.components.ControlFieldItem

private enum class GpuPickerType {
    NONE, GOVERNOR, MAX_FREQ, MIN_FREQ
}

@Composable
fun GpuControlScreen(
    gpuControl: GpuControlState,
    onSetGpuGov: (String) -> Unit,
    onSetGpuMin: (Int) -> Unit,
    onSetGpuMax: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var activePicker by remember { mutableStateOf(GpuPickerType.NONE) }

    val oppList = remember(gpuControl.oppTable) {
        gpuControl.oppTable.values.sortedBy { it.index }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        CardWithBadge(
            badgeText = "GPU",
            badgeIcon = Icons.Default.DeveloperBoard
        ) {
            ControlFieldItem(
                label = "Governor",
                currentValue = gpuControl.currentGovernor,
                onClick = { activePicker = GpuPickerType.GOVERNOR }
            )

            val maxFreqStr = gpuControl.currentMaxOppIdx?.let { idx ->
                gpuControl.oppTable[idx]?.let { "${it.freqMhz.toInt()} MHz" } ?: "Index $idx"
            } ?: "—"
            ControlFieldItem(
                label = "Max frequency",
                currentValue = maxFreqStr,
                onClick = { activePicker = GpuPickerType.MAX_FREQ }
            )

            val minFreqStr = gpuControl.currentMinOppIdx?.let { idx ->
                gpuControl.oppTable[idx]?.let { "${it.freqMhz.toInt()} MHz" } ?: "Index $idx"
            } ?: "—"
            ControlFieldItem(
                label = "Min frequency",
                currentValue = minFreqStr,
                onClick = { activePicker = GpuPickerType.MIN_FREQ },
                showDivider = false
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    when (activePicker) {
        GpuPickerType.GOVERNOR -> {
            BottomSheetPicker(
                title = "Governor",
                items = gpuControl.availableGovernors,
                selectedItem = gpuControl.currentGovernor,
                itemLabel = { it },
                onItemSelected = onSetGpuGov,
                onDismiss = { activePicker = GpuPickerType.NONE }
            )
        }
        GpuPickerType.MAX_FREQ -> {
            BottomSheetPicker(
                title = "Max frequency",
                items = oppList,
                selectedItem = gpuControl.oppTable[gpuControl.currentMaxOppIdx],
                itemLabel = { "${it.freqMhz.toInt()} MHz (${it.voltMv.toInt()} mV)" },
                onItemSelected = { onSetGpuMax(it.index) },
                onDismiss = { activePicker = GpuPickerType.NONE }
            )
        }
        GpuPickerType.MIN_FREQ -> {
            BottomSheetPicker(
                title = "Min frequency",
                items = oppList,
                selectedItem = gpuControl.oppTable[gpuControl.currentMinOppIdx],
                itemLabel = { "${it.freqMhz.toInt()} MHz (${it.voltMv.toInt()} mV)" },
                onItemSelected = { onSetGpuMin(it.index) },
                onDismiss = { activePicker = GpuPickerType.NONE }
            )
        }
        GpuPickerType.NONE -> {}
    }
}
