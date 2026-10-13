package com.rockproject.mt6789.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rockproject.mt6789.model.CpuControlCluster
import com.rockproject.mt6789.ui.components.BottomSheetPicker
import com.rockproject.mt6789.ui.components.CardWithBadge
import com.rockproject.mt6789.ui.components.ControlFieldItem

private enum class CpuPickerType {
    NONE, LITTLE_GOV, LITTLE_MAX, LITTLE_MIN, BIG_GOV, BIG_MAX, BIG_MIN
}

@Composable
fun CpuControlScreen(
    littleControl: CpuControlCluster,
    bigControl: CpuControlCluster,
    onSetLittleGov: (String) -> Unit,
    onSetLittleMin: (Long) -> Unit,
    onSetLittleMax: (Long) -> Unit,
    onSetBigGov: (String) -> Unit,
    onSetBigMin: (Long) -> Unit,
    onSetBigMax: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var activePicker by remember { mutableStateOf(CpuPickerType.NONE) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Little Cluster Block
        CardWithBadge(
            badgeText = "CPU · Little Cluster",
            badgeIcon = Icons.Default.Memory
        ) {
            ControlFieldItem(
                label = "Governor",
                currentValue = littleControl.currentGovernor,
                onClick = { activePicker = CpuPickerType.LITTLE_GOV }
            )

            val littleMaxStr = littleControl.currentMaxKhz?.let { "${it / 1000} MHz" } ?: "—"
            ControlFieldItem(
                label = "Max frequency",
                currentValue = littleMaxStr,
                onClick = { activePicker = CpuPickerType.LITTLE_MAX }
            )

            val littleMinStr = littleControl.currentMinKhz?.let { "${it / 1000} MHz" } ?: "—"
            ControlFieldItem(
                label = "Min frequency",
                currentValue = littleMinStr,
                onClick = { activePicker = CpuPickerType.LITTLE_MIN },
                showDivider = false
            )
        }

        // Big Cluster Block
        CardWithBadge(
            badgeText = "CPU · Big Cluster",
            badgeIcon = Icons.Default.Memory
        ) {
            ControlFieldItem(
                label = "Governor",
                currentValue = bigControl.currentGovernor,
                onClick = { activePicker = CpuPickerType.BIG_GOV }
            )

            val bigMaxStr = bigControl.currentMaxKhz?.let { "${it / 1000} MHz" } ?: "—"
            ControlFieldItem(
                label = "Max frequency",
                currentValue = bigMaxStr,
                onClick = { activePicker = CpuPickerType.BIG_MAX }
            )

            val bigMinStr = bigControl.currentMinKhz?.let { "${it / 1000} MHz" } ?: "—"
            ControlFieldItem(
                label = "Min frequency",
                currentValue = bigMinStr,
                onClick = { activePicker = CpuPickerType.BIG_MIN },
                showDivider = false
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Modal Bottom Sheets for pickers
    when (activePicker) {
        CpuPickerType.LITTLE_GOV -> {
            BottomSheetPicker(
                title = "Governor",
                items = littleControl.availableGovernors,
                selectedItem = littleControl.currentGovernor,
                itemLabel = { it },
                onItemSelected = onSetLittleGov,
                onDismiss = { activePicker = CpuPickerType.NONE }
            )
        }
        CpuPickerType.LITTLE_MAX -> {
            BottomSheetPicker(
                title = "Max frequency",
                items = littleControl.availableFrequenciesKhz,
                selectedItem = littleControl.currentMaxKhz,
                itemLabel = { "${it / 1000} MHz" },
                onItemSelected = onSetLittleMax,
                onDismiss = { activePicker = CpuPickerType.NONE }
            )
        }
        CpuPickerType.LITTLE_MIN -> {
            BottomSheetPicker(
                title = "Min frequency",
                items = littleControl.availableFrequenciesKhz,
                selectedItem = littleControl.currentMinKhz,
                itemLabel = { "${it / 1000} MHz" },
                onItemSelected = onSetLittleMin,
                onDismiss = { activePicker = CpuPickerType.NONE }
            )
        }
        CpuPickerType.BIG_GOV -> {
            BottomSheetPicker(
                title = "Governor",
                items = bigControl.availableGovernors,
                selectedItem = bigControl.currentGovernor,
                itemLabel = { it },
                onItemSelected = onSetBigGov,
                onDismiss = { activePicker = CpuPickerType.NONE }
            )
        }
        CpuPickerType.BIG_MAX -> {
            BottomSheetPicker(
                title = "Max frequency",
                items = bigControl.availableFrequenciesKhz,
                selectedItem = bigControl.currentMaxKhz,
                itemLabel = { "${it / 1000} MHz" },
                onItemSelected = onSetBigMax,
                onDismiss = { activePicker = CpuPickerType.NONE }
            )
        }
        CpuPickerType.BIG_MIN -> {
            BottomSheetPicker(
                title = "Min frequency",
                items = bigControl.availableFrequenciesKhz,
                selectedItem = bigControl.currentMinKhz,
                itemLabel = { "${it / 1000} MHz" },
                onItemSelected = onSetBigMin,
                onDismiss = { activePicker = CpuPickerType.NONE }
            )
        }
        CpuPickerType.NONE -> {}
    }
}
