package com.rockproject.mt6789.ui.screens

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rockproject.mt6789.model.BootResult
import com.rockproject.mt6789.ui.components.CardWithBadge
import com.rockproject.mt6789.ui.components.PillButton
import com.rockproject.mt6789.ui.theme.RockTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BootApplyScreen(
    bootApplyEnabled: Boolean,
    bootResult: BootResult,
    showMiuiCard: Boolean,
    isBusy: Boolean,
    onToggleBootApply: (Boolean) -> Unit,
    onTestBootApplyNow: () -> Unit,
    onDismissMiuiCard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = RockTheme.colors
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CardWithBadge(
            badgeText = "Apply on Boot",
            badgeIcon = Icons.Default.PowerSettingsNew
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Apply on boot",
                        color = colors.label,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Re-applies your last OC and control settings after every reboot",
                        color = colors.labelTertiary,
                        fontSize = 13.sp,
                        lineHeight = 17.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = bootApplyEnabled,
                    onCheckedChange = onToggleBootApply,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = colors.blue,
                        uncheckedThumbColor = colors.labelSecondary,
                        uncheckedTrackColor = colors.surfaceHigh
                    )
                )
            }

            HorizontalDivider(
                color = colors.separator,
                thickness = 0.5.dp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            val bootStatusText = formatBootResult(bootResult)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Last boot apply",
                    color = colors.labelSecondary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text = bootStatusText,
                    color = when {
                        bootResult.isOk -> colors.mint
                        bootResult.isFail -> colors.red
                        else -> colors.labelTertiary
                    },
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            PillButton(
                text = "Test script now",
                isPrimary = false,
                enabled = bootApplyEnabled && !isBusy,
                onClick = onTestBootApplyNow,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Simplified MIUI / HyperOS Autostart Card
        if (showMiuiCard || bootApplyEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surfaceRaised)
                    .border(0.5.dp, colors.orange.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MIUI / HyperOS Autostart",
                        color = colors.orange,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Allow autostart for reliable boot apply",
                        color = colors.labelSecondary,
                        fontSize = 12.5.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = { openMiuiAutostart(context) },
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.orange),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Text("Open", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun formatBootResult(result: BootResult): String {
    if (result.isNever) return "never"
    val dateStr = if (result.time > 0) {
        val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
        " · " + sdf.format(Date(result.time * 1000L))
    } else ""

    return when {
        result.isOk -> "OK (${result.message.ifEmpty { "applied" }})$dateStr"
        result.isFail -> "FAIL ${result.message}$dateStr"
        else -> result.status + dateStr
    }
}

private fun openMiuiAutostart(context: Context) {
    try {
        val intent = Intent().apply {
            component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        openAppDetails(context)
    }
}

private fun openAppDetails(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}
