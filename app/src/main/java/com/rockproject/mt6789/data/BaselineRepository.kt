package com.rockproject.mt6789.data

import com.rockproject.mt6789.model.CpuBaseline
import com.rockproject.mt6789.model.GpuBaseline
import com.rockproject.mt6789.model.StockBaseline
import com.rockproject.mt6789.model.StockFloors
import com.rockproject.mt6789.root.ShellManager
import com.rockproject.mt6789.util.ConversionUtils
import org.json.JSONObject
import java.util.regex.Pattern

object BaselineRepository {

    suspend fun readBaseline(): StockBaseline? {
        val res = ShellManager.exec("cat ${KernelConstants.STOCK_BASELINE_JSON} 2>/dev/null")
        if (!res.isSuccess || res.stdout.isBlank()) return null
        return parseBaselineJson(res.stdout)
    }

    fun parseBaselineJson(jsonStr: String): StockBaseline? {
        try {
            val root = JSONObject(jsonStr)
            val capturedAt = root.optLong("captured_at", 0L)
            val gpuObj = root.optJSONObject("gpu")
            val cpuObj = root.optJSONObject("cpu")

            val gpu = GpuBaseline(
                freq_khz = gpuObj?.optLong("freq_khz", 0L) ?: 0L,
                volt_raw = gpuObj?.optLong("volt_raw", 0L) ?: 0L,
                vsram_raw = gpuObj?.optLong("vsram_raw", 0L) ?: 0L
            )
            val cpu = CpuBaseline(
                little_khz = cpuObj?.optLong("little_khz", 0L) ?: cpuObj?.optLong("ll_khz", 0L) ?: 0L,
                big_khz = cpuObj?.optLong("big_khz", 0L) ?: cpuObj?.optLong("b_khz", 0L) ?: 0L,
                prime_khz = cpuObj?.optLong("prime_khz", 0L) ?: 0L
            )
            if (gpu.freq_khz > 0 || cpu.little_khz > 0) {
                return StockBaseline(captured_at = capturedAt, gpu = gpu, cpu = cpu)
            }
        } catch (_: Throwable) {
            // JSONObject may be stubbed in plain JVM tests
        }

        return try {
            fun findLong(key: String): Long {
                val m = Pattern.compile("\"$key\"\\s*:\\s*(\\d+)").matcher(jsonStr)
                return if (m.find()) m.group(1)?.toLongOrNull() ?: 0L else 0L
            }
            val captured = findLong("captured_at")
            val gFreq = findLong("freq_khz")
            val gVolt = findLong("volt_raw")
            val gVsram = findLong("vsram_raw")
            val cLittle = findLong("little_khz").ifEmpty { findLong("ll_khz") }
            val cBig = findLong("big_khz").ifEmpty { findLong("b_khz") }
            val cPrime = findLong("prime_khz")
            if (gFreq > 0 || cLittle > 0) {
                StockBaseline(
                    captured_at = captured,
                    gpu = GpuBaseline(freq_khz = gFreq, volt_raw = gVolt, vsram_raw = gVsram),
                    cpu = CpuBaseline(little_khz = cLittle, big_khz = cBig, prime_khz = cPrime)
                )
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun Long.ifEmpty(fallback: () -> Long): Long = if (this > 0) this else fallback()

    suspend fun captureBaselineIfStock(
        policyLittlePath: String?,
        policyBigPath: String?,
        policyPrimePath: String? = null,
        gpuFreqParamName: String
    ): Boolean {
        // Never overwrite an existing valid baseline
        val existing = readBaseline()
        if (existing != null && existing.gpu.freq_khz > 0 && existing.cpu.little_khz > 0) {
            return true
        }

        // Verify module parameters exist
        val testMod = ShellManager.exec("test -d ${KernelConstants.PARAMS_DIR} && echo ok")
        if (testMod.stdout != "ok") return false

        // Check if any OC is currently active
        val clittle = ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/cpu_little_target_khz 2>/dev/null").stdout.toLongOrNull()
            ?: ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/cpu_ll_target_khz 2>/dev/null").stdout.toLongOrNull() ?: 0L
        val cbig = ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/cpu_big_target_khz 2>/dev/null").stdout.toLongOrNull()
            ?: ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/cpu_b_target_khz 2>/dev/null").stdout.toLongOrNull() ?: 0L
        val cPrime = ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/cpu_prime_target_khz 2>/dev/null").stdout.toLongOrNull() ?: 0L
        val gFreq = ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/$gpuFreqParamName 2>/dev/null").stdout.toLongOrNull() ?: 0L

        // If any target is non-zero, OC is active -> do NOT capture baseline blind!
        if (clittle > 0L || cbig > 0L || cPrime > 0L || gFreq > 0L) {
            return false
        }

        // Read OPP table for GPU stock max
        val oppRes = ShellManager.exec("head -n 1 ${KernelConstants.GPU_WORKING_OPP} 2>/dev/null")
        var gpuFreqKhz = 0L
        var gpuVoltRaw = 0L
        var gpuVsramRaw = 0L
        if (oppRes.isSuccess && oppRes.stdout.isNotBlank()) {
            val fMatch = Pattern.compile("freq:\\s*(\\d+)").matcher(oppRes.stdout)
            val vMatch = Pattern.compile("volt:\\s*(\\d+)").matcher(oppRes.stdout)
            val vsMatch = Pattern.compile("vsram:\\s*(\\d+)").matcher(oppRes.stdout)
            if (fMatch.find()) gpuFreqKhz = fMatch.group(1)?.toLongOrNull() ?: 0L
            if (vMatch.find()) gpuVoltRaw = vMatch.group(1)?.toLongOrNull() ?: 0L
            if (vsMatch.find()) gpuVsramRaw = vsMatch.group(1)?.toLongOrNull() ?: 0L
        }

        var cpuLittleKhz = 0L
        var cpuBigKhz = 0L
        var cpuPrimeKhz = 0L
        if (!policyLittlePath.isNullOrBlank()) {
            val r = ShellManager.exec("cat $policyLittlePath/cpuinfo_max_freq 2>/dev/null")
            cpuLittleKhz = r.stdout.toLongOrNull() ?: 0L
        }
        if (!policyBigPath.isNullOrBlank()) {
            val r = ShellManager.exec("cat $policyBigPath/cpuinfo_max_freq 2>/dev/null")
            cpuBigKhz = r.stdout.toLongOrNull() ?: 0L
        }
        if (!policyPrimePath.isNullOrBlank()) {
            val r = ShellManager.exec("cat $policyPrimePath/cpuinfo_max_freq 2>/dev/null")
            cpuPrimeKhz = r.stdout.toLongOrNull() ?: 0L
        }

        if (gpuFreqKhz <= 0 || cpuLittleKhz <= 0 || cpuBigKhz <= 0) {
            return false
        }

        val capturedAt = System.currentTimeMillis() / 1000L
        val json = """
            {
              "captured_at": $capturedAt,
              "gpu": { "freq_khz": $gpuFreqKhz, "volt_raw": $gpuVoltRaw, "vsram_raw": $gpuVsramRaw },
              "cpu": { "little_khz": $cpuLittleKhz, "big_khz": $cpuBigKhz, "prime_khz": $cpuPrimeKhz }
            }
        """.trimIndent()

        ShellManager.runSerializedWrite {
            ShellManager.exec("mkdir -p ${KernelConstants.BASELINE_DIR} 2>/dev/null")
            ShellManager.exec("cat << 'EOF' > ${KernelConstants.STOCK_BASELINE_JSON}\n$json\nEOF")
        }

        return true
    }

    suspend fun deriveStockFloors(
        policyLittlePath: String?,
        policyBigPath: String?,
        policyPrimePath: String? = null,
        primeStatus: String = "inactive"
    ): StockFloors {
        var gpuFreqMin: Int? = null
        var gpuVoltMin: Int? = null
        var cpuLittleMin: Int? = null
        var cpuBigMin: Int? = null
        var cpuPrimeMin: Int? = null
        val isPrimeActive = primeStatus.equals("active", ignoreCase = true) && !policyPrimePath.isNullOrBlank()

        val baseline = readBaseline()
        if (baseline != null) {
            if (baseline.gpu.freq_khz > 0) {
                val stockFreq = ConversionUtils.roundToStep(ConversionUtils.khzToMhz(baseline.gpu.freq_khz), 5)
                if (stockFreq <= KernelConstants.GPU_FREQ_MAX_MHZ) {
                    gpuFreqMin = stockFreq
                }
            }
            if (baseline.gpu.volt_raw > 0) {
                val stockVolt = Math.round(baseline.gpu.volt_raw / 100.0).toInt()
                if (stockVolt <= KernelConstants.GPU_VOLT_MAX_MV) {
                    gpuVoltMin = stockVolt
                }
            }
            if (baseline.cpu.little_khz > 0) {
                val stockLittle = ConversionUtils.roundToStep(ConversionUtils.khzToMhz(baseline.cpu.little_khz), 10)
                if (stockLittle <= KernelConstants.CPU_LITTLE_MAX_MHZ) {
                    cpuLittleMin = stockLittle
                }
            }
            if (baseline.cpu.big_khz > 0) {
                val stockBig = ConversionUtils.roundToStep(ConversionUtils.khzToMhz(baseline.cpu.big_khz), 10)
                if (stockBig <= KernelConstants.CPU_BIG_MAX_MHZ) {
                    cpuBigMin = stockBig
                }
            }
            if (isPrimeActive && baseline.cpu.prime_khz > 0) {
                val stockPrime = ConversionUtils.roundToStep(ConversionUtils.khzToMhz(baseline.cpu.prime_khz), 10)
                if (stockPrime <= KernelConstants.CPU_PRIME_MAX_MHZ) {
                    cpuPrimeMin = stockPrime
                }
            }
        }

        val clittleTarget = ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/cpu_little_target_khz 2>/dev/null").stdout.toLongOrNull()
            ?: ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/cpu_ll_target_khz 2>/dev/null").stdout.toLongOrNull() ?: 0L
        val cbigTarget = ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/cpu_big_target_khz 2>/dev/null").stdout.toLongOrNull()
            ?: ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/cpu_b_target_khz 2>/dev/null").stdout.toLongOrNull() ?: 0L
        val cPrimeTarget = ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/cpu_prime_target_khz 2>/dev/null").stdout.toLongOrNull() ?: 0L

        // Secondary fallback to sysfs if baseline was missing
        if (cpuLittleMin == null && !policyLittlePath.isNullOrBlank()) {
            if (clittleTarget == 0L) {
                val r = ShellManager.exec("cat $policyLittlePath/cpuinfo_max_freq 2>/dev/null")
                val khz = r.stdout.toLongOrNull()
                if (khz != null && khz > 0) {
                    val stock = ConversionUtils.roundToStep(ConversionUtils.khzToMhz(khz), 10)
                    if (stock <= KernelConstants.CPU_LITTLE_MAX_MHZ) {
                        cpuLittleMin = stock
                    }
                }
            } else {
                val r = ShellManager.exec("cat $policyLittlePath/cpuinfo_min_freq 2>/dev/null")
                val khz = r.stdout.toLongOrNull()
                if (khz != null && khz > 0) {
                    cpuLittleMin = ConversionUtils.roundToStep(ConversionUtils.khzToMhz(khz), 10)
                }
            }
        }
        if (cpuBigMin == null && !policyBigPath.isNullOrBlank()) {
            if (cbigTarget == 0L) {
                val r = ShellManager.exec("cat $policyBigPath/cpuinfo_max_freq 2>/dev/null")
                val khz = r.stdout.toLongOrNull()
                if (khz != null && khz > 0) {
                    val stock = ConversionUtils.roundToStep(ConversionUtils.khzToMhz(khz), 10)
                    if (stock <= KernelConstants.CPU_BIG_MAX_MHZ) {
                        cpuBigMin = stock
                    }
                }
            } else {
                val r = ShellManager.exec("cat $policyBigPath/cpuinfo_min_freq 2>/dev/null")
                val khz = r.stdout.toLongOrNull()
                if (khz != null && khz > 0) {
                    cpuBigMin = ConversionUtils.roundToStep(ConversionUtils.khzToMhz(khz), 10)
                }
            }
        }
        if (isPrimeActive && cpuPrimeMin == null && !policyPrimePath.isNullOrBlank()) {
            if (cPrimeTarget == 0L) {
                val r = ShellManager.exec("cat $policyPrimePath/cpuinfo_max_freq 2>/dev/null")
                val khz = r.stdout.toLongOrNull()
                if (khz != null && khz > 0) {
                    val stock = ConversionUtils.roundToStep(ConversionUtils.khzToMhz(khz), 10)
                    if (stock <= KernelConstants.CPU_PRIME_MAX_MHZ) {
                        cpuPrimeMin = stock
                    }
                }
            } else {
                val r = ShellManager.exec("cat $policyPrimePath/cpuinfo_min_freq 2>/dev/null")
                val khz = r.stdout.toLongOrNull()
                if (khz != null && khz > 0) {
                    cpuPrimeMin = ConversionUtils.roundToStep(ConversionUtils.khzToMhz(khz), 10)
                }
            }
        }
        if (gpuFreqMin == null || gpuVoltMin == null) {
            val oppContent = ShellManager.exec("cat ${KernelConstants.GPU_WORKING_OPP} 2>/dev/null").stdout
            if (oppContent.isNotBlank()) {
                val oppMap = ConversionUtils.parseGpuOppTable(oppContent)
                val stockMax = ConversionUtils.getGpuStockMax(oppMap)
                if (stockMax != null) {
                    if (gpuFreqMin == null) {
                        val stockFreq = ConversionUtils.roundToStep(stockMax.freqMhz, 5)
                        if (stockFreq <= KernelConstants.GPU_FREQ_MAX_MHZ) {
                            gpuFreqMin = stockFreq
                        }
                    }
                    if (gpuVoltMin == null) {
                        val stockVolt = Math.round(stockMax.voltMv).toInt()
                        if (stockVolt <= KernelConstants.GPU_VOLT_MAX_MV) {
                            gpuVoltMin = stockVolt
                        }
                    }
                }
            }
        }

        return StockFloors(
            cpuLittleMin = cpuLittleMin,
            cpuBigMin = cpuBigMin,
            cpuPrimeMin = if (isPrimeActive) cpuPrimeMin else null,
            isPrimeActive = isPrimeActive,
            gpuFreqMin = gpuFreqMin,
            gpuVoltMin = gpuVoltMin
        )
    }
}
