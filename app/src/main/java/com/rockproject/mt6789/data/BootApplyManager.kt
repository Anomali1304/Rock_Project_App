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
        cpuLittleKhz: Long,
        cpuBigKhz: Long,
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

        val allow3ghzLine = if (cpuLittleKhz > 2600000L || cpuBigKhz > 2600000L || cpuPrimeKhz > 2600000L) {
            """
  if [ -f "${'$'}P/${KernelConstants.CPU_VOLT_MAX_DELTA_PARAM}" ]; then
    w ${KernelConstants.CPU_VOLT_MAX_DELTA_PARAM} 50000
  fi
  if [ -f "${'$'}P/${KernelConstants.CPU_VOLT_ABS_MAX_PARAM}" ]; then
    w ${KernelConstants.CPU_VOLT_ABS_MAX_PARAM} 115000
  fi
  if [ -f "${'$'}P/${KernelConstants.CPU_ALLOW_3GHZ_PARAM}" ]; then
    w ${KernelConstants.CPU_ALLOW_3GHZ_PARAM} 1
  fi
            """.trimIndent()
        } else {
            ""
        }

        val cpuBlock = if (isCpuOc) {
            val lines = mutableListOf<String>()
            lines.add("cpu_ok=1")
            lines.add("n=0")
            lines.add("while [ \"${'$'}n\" -lt 3 ]; do")
            if (allow3ghzLine.isNotBlank()) {
                lines.add(allow3ghzLine)
            }
            lines.add("  if [ -f \"${'$'}P/cpu_little_target_khz\" ]; then")
            lines.add("    w cpu_little_target_khz $cpuLittleKhz")
            lines.add("  else")
            lines.add("    w cpu_ll_target_khz $cpuLittleKhz")
            lines.add("  fi")
            lines.add("  if [ -f \"${'$'}P/cpu_big_target_khz\" ]; then")
            lines.add("    w cpu_big_target_khz $cpuBigKhz")
            lines.add("  else")
            lines.add("    w cpu_b_target_khz $cpuBigKhz")
            lines.add("  fi")
            if (primeLine.isNotBlank()) {
                lines.add(primeLine)
            }
            lines.add("  w cpu_oc_apply 1")
            lines.add("  w cpu_stats_refresh 1")
            lines.add("  w cpu_stats_refresh 2")
            lines.add("  if [ \"${'$'}(cat \"${'$'}P/cpu_prime_status\" 2>/dev/null)\" = \"active\" ]; then")
            lines.add("    w cpu_stats_refresh 3")
            lines.add("  fi")
            lines.add("  r=${'$'}(cat \"${'$'}P/cpu_oc_result\" 2>/dev/null)")
            lines.add("  case \"${'$'}r\" in")
            lines.add("    FAIL*) cpu_ok=0 ;;")
            lines.add("    *)     cpu_ok=1; break ;;")
            lines.add("  esac")
            lines.add("  n=${'$'}((n+1)); sleep 2")
            lines.add("done")
            lines.joinToString("\n")
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
        cpuLittleKhz: Long,
        cpuBigKhz: Long,
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
            cpuLittleKhz = cpuLittleKhz,
            cpuBigKhz = cpuBigKhz,
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
            put("cpu_little_khz", cpuLittleKhz)
            put("cpu_big_khz", cpuBigKhz)
            if (cpuPrimeKhz > 0L) {
                put("cpu_prime_khz", cpuPrimeKhz)
            }
            if (cpuLittleKhz > 2600000L || cpuBigKhz > 2600000L || cpuPrimeKhz > 2600000L) {
                put("cpu_oc_allow_3ghz", 1)
                put("cpu_volt_max_delta_raw", 50000)
                put("cpu_volt_abs_max_raw", 115000)
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
