package com.rockproject.mt6789.data

import android.content.Context
import com.rockproject.mt6789.model.BootResult
import com.rockproject.mt6789.root.ShellManager
import org.json.JSONObject
import java.io.InputStreamReader

object BootApplyManager {

    fun renderScript(
        templateContent: String,
        isCpuOc: Boolean,
        cpuLLKhz: Long,
        cpuBKhz: Long,
        cpuPrimeKhz: Long = 0L,
        isGpuOc: Boolean,
        gpuFreqParam: String,
        gpuFreqKhz: Long,
        gpuVoltRaw: Long,
        controlCommands: List<String>
    ): String {
        val primeLine = if (cpuPrimeKhz > 0L) {
            """
  if [ "${'$'}(cat "${'$'}P/cpu_prime_status" 2>/dev/null)" = "active" ]; then
    w cpu_prime_target_khz $cpuPrimeKhz
  fi
            """.trimIndent()
        } else {
            ""
        }

        val cpuBlock = if (isCpuOc) {
            """
cpu_ok=1
n=0
while [ "${'$'}n" -lt 3 ]; do
  if [ -f "${'$'}P/cpu_little_target_khz" ]; then
    w cpu_little_target_khz $cpuLLKhz
  else
    w cpu_ll_target_khz $cpuLLKhz
  fi
  if [ -f "${'$'}P/cpu_big_target_khz" ]; then
    w cpu_big_target_khz $cpuBKhz
  else
    w cpu_b_target_khz $cpuBKhz
  fi
$primeLine
  w cpu_oc_apply 1
  w cpu_stats_refresh 1
  w cpu_stats_refresh 2
  r=${'$'}(cat "${'$'}P/cpu_oc_result" 2>/dev/null)
  case "${'$'}r" in
    FAIL*) cpu_ok=0 ;;
    *)     cpu_ok=1; break ;;
  esac
  n=${'$'}((n+1)); sleep 2
done
            """.trimIndent()
        } else {
            "cpu_ok=1"
        }

        val gpuBlock = if (isGpuOc) {
            """
gpu_ok=1
n=0
while [ "${'$'}n" -lt 3 ]; do
  w $gpuFreqParam $gpuFreqKhz
  w gpu_target_volt $gpuVoltRaw
  w gpu_target_vsram $gpuVoltRaw
  w gpu_oc_apply 1
  r=${'$'}(cat "${'$'}P/gpu_oc_result" 2>/dev/null)
  case "${'$'}r" in
    FAIL*) gpu_ok=0 ;;
    *)     gpu_ok=1; break ;;
  esac
  n=${'$'}((n+1)); sleep 2
done
            """.trimIndent()
        } else {
            "gpu_ok=1"
        }

        val controlsBlock = if (controlCommands.isNotEmpty()) {
            controlCommands.joinToString("\n")
        } else {
            "# No custom controls"
        }

        var rendered = templateContent
            .replace("@CPU_OC_BLOCK@", cpuBlock)
            .replace("@GPU_OC_BLOCK@", gpuBlock)
            .replace("@CONTROLS_BLOCK@", controlsBlock)

        return rendered
    }

    suspend fun saveBootApplyScript(
        context: Context,
        isCpuOc: Boolean,
        cpuLLKhz: Long,
        cpuBKhz: Long,
        cpuPrimeKhz: Long = 0L,
        isGpuOc: Boolean,
        gpuFreqParam: String,
        gpuFreqKhz: Long,
        gpuVoltRaw: Long,
        controlCommands: List<String>
    ): Boolean {
        val template = try {
            context.assets.open("apply_on_boot.sh.tpl").use { stream ->
                InputStreamReader(stream, Charsets.UTF_8).readText()
            }
        } catch (_: Exception) {
            return false
        }

        val script = renderScript(
            templateContent = template,
            isCpuOc = isCpuOc,
            cpuLLKhz = cpuLLKhz,
            cpuBKhz = cpuBKhz,
            cpuPrimeKhz = cpuPrimeKhz,
            isGpuOc = isGpuOc,
            gpuFreqParam = gpuFreqParam,
            gpuFreqKhz = gpuFreqKhz,
            gpuVoltRaw = gpuVoltRaw,
            controlCommands = controlCommands
        )

        val configJson = JSONObject().apply {
            put("enabled", true)
            put("cpu_oc", isCpuOc)
            put("cpu_ll_khz", cpuLLKhz)
            put("cpu_b_khz", cpuBKhz)
            if (cpuPrimeKhz > 0L) {
                put("cpu_prime_khz", cpuPrimeKhz)
            }
            put("gpu_oc", isGpuOc)
            put("gpu_freq_param", gpuFreqParam)
            put("gpu_freq_khz", gpuFreqKhz)
            put("gpu_volt_raw", gpuVoltRaw)
            put("updated_at", System.currentTimeMillis() / 1000L)
        }.toString()

        return ShellManager.runSerializedWrite {
            ShellManager.exec("mkdir -p ${KernelConstants.BASE_DIR} 2>/dev/null")
            val writeScript = ShellManager.exec("cat << 'EOF' > ${KernelConstants.BOOT_SCRIPT_PATH}\n$script\nEOF")
            val chmod = ShellManager.exec("chmod 755 ${KernelConstants.BOOT_SCRIPT_PATH}")
            ShellManager.exec("cat << 'EOF' > ${KernelConstants.BOOT_CONFIG_JSON}\n$configJson\nEOF")
            writeScript.isSuccess && chmod.isSuccess
        }
    }

    suspend fun disableBootApply(): Boolean {
        return ShellManager.runSerializedWrite {
            ShellManager.exec("rm -f ${KernelConstants.BOOT_SCRIPT_PATH} ${KernelConstants.BOOT_CONFIG_JSON} 2>/dev/null")
            true
        }
    }

    suspend fun isBootApplyFilePresent(): Boolean {
        val res = ShellManager.exec("test -f ${KernelConstants.BOOT_SCRIPT_PATH} && echo yes")
        return res.stdout == "yes"
    }

    suspend fun readBootResult(): BootResult {
        val res = ShellManager.exec("cat ${KernelConstants.BOOT_RESULT_JSON} 2>/dev/null")
        if (!res.isSuccess || res.stdout.isBlank()) return BootResult()

        return try {
            val obj = JSONObject(res.stdout)
            BootResult(
                time = obj.optLong("time", 0L),
                status = obj.optString("status", "never"),
                message = obj.optString("message", "")
            )
        } catch (_: Exception) {
            BootResult()
        }
    }
}
