package com.rockproject.mt6789.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rockproject.mt6789.model.HardwareBrandInfo
import com.rockproject.mt6789.model.LiveHardwareStats
import com.rockproject.mt6789.ui.components.*
import com.rockproject.mt6789.ui.theme.RockTheme

@Composable
fun HomeScreen(
    cpuInfo: HardwareBrandInfo,
    gpuInfo: HardwareBrandInfo,
    liveStats: LiveHardwareStats,
    onNavigateCpuMenu: () -> Unit,
    onNavigateGpuMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = RockTheme.colors
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // CPU Home Card
        GlassCard(onClick = onNavigateCpuMenu) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.width(76.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MediaTekLogo()
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cpuInfo.cpuBrandName,
                        color = colors.label,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = cpuInfo.cpuBrandModel,
                        color = colors.mint,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            StatRow(
                label = "Little Cluster",
                value = liveStats.cpuLLCurMhz
            )
            StatRow(
                label = "Big Cluster",
                value = liveStats.cpuBCurMhz
            )
            if (liveStats.isPrimeActive) {
                StatRow(
                    label = "Prime Cluster",
                    value = liveStats.cpuPrimeCurMhz
                )
            }
            StatRow(
                label = "Status",
                value = liveStats.cpuStatus,
                isActive = liveStats.isCpuActive,
                showDivider = false
            )

            if (cpuInfo.cpuArchFooter.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = cpuInfo.cpuArchFooter,
                    color = colors.labelTertiary,
                    fontSize = 12.5.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp
                )
            }
        }

        // GPU Home Card
        GlassCard(onClick = onNavigateGpuMenu) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.width(76.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ArmLogo()
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = gpuInfo.gpuBrandName,
                        color = colors.label,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = gpuInfo.gpuBrandArch,
                        color = colors.mint,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            StatRow(
                label = "Frequency",
                value = liveStats.gpuCurFreqMhz
            )
            StatRow(
                label = "Voltage",
                value = liveStats.gpuCurVoltMv
            )
            StatRow(
                label = "Status",
                value = liveStats.gpuStatus,
                isActive = liveStats.isGpuActive,
                showDivider = false
            )

            if (gpuInfo.gpuFooter.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = gpuInfo.gpuFooter,
                    color = colors.labelTertiary,
                    fontSize = 12.5.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
