package com.rockproject.mt6789.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rockproject.mt6789.data.BootApplyManager
import com.rockproject.mt6789.data.KernelConstants
import com.rockproject.mt6789.root.ShellManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check if boot apply is active
                val filePresent = BootApplyManager.isBootApplyFilePresent()
                if (!filePresent) {
                    pendingResult.finish()
                    return@launch
                }

                // Execute the generated apply_on_boot.sh script in background
                ShellManager.exec("sh ${KernelConstants.BOOT_SCRIPT_PATH} &")

                // Wait up to ~8 seconds for boot_result.json to be populated or updated
                withTimeoutOrNull(8000L) {
                    for (i in 0 until 16) {
                        delay(500)
                        val res = BootApplyManager.readBootResult()
                        if (!res.isNever) {
                            break
                        }
                    }
                }
            } catch (_: Exception) {
                // Ensure finish() is called regardless of error
            } finally {
                pendingResult.finish()
            }
        }
    }
}
