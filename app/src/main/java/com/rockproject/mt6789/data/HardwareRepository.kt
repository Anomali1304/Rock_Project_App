package com.rockproject.mt6789.data

import com.rockproject.mt6789.model.*
import com.rockproject.mt6789.root.ShellManager
import com.rockproject.mt6789.root.ShellResult
import com.rockproject.mt6789.util.ConversionUtils
import kotlinx.coroutines.delay
import java.util.regex.Pattern

object HardwareRepository {

    var cpuPolicy0: String? = null
        private set
    var cpuPolicy6: String? = null
        private set
    var gpuPowerPolicy: String? = null
        private set
    var gpuFreqParamName: String = "gpu_target_khz"
        private set

    suspend fun resolveHardwarePaths() {
        resolveCpuPolicies()
        resolveGpuPowerPolicy()
        resolveGpuParamName()
    }

    suspend fun resolveCpuPolicies(): Pair<String?, String?> {
        val res = ShellManager.exec(
            "for d in ${KernelConstants.CPU_POLICY_DIR}/policy*; do [ -d \"\$d\" ] || continue; printf '%s\\t' \"\$d\"; cat \"\$d/related_cpus\" 2>/dev/null | tr '\\n' ' '; printf '\\n'; done 2>/dev/null"
        )
        if (!res.isSuccess || res.stdout.isBlank()) {
            return cpuPolicy0 to cpuPolicy6
        }

        val parsed = res.stdout.lines().mapNotNull { line ->
            val parts = line.split("\t")
            if (parts.size >= 2) {
                val path = parts[0].trim()
                val cpus = parts[1].trim().split(Regex("\\s+")).mapNotNull { it.toIntOrNull() }
                if (path.isNotEmpty() && cpus.isNotEmpty()) {
                    CpuPolicyInfo(path = path, firstCpu = cpus.minOrNull() ?: 0, relatedCpus = cpus)
                } else null
            } else null
        }.sortedWith(compareBy({ it.firstCpu }, { it.path }))

        if (parsed.size >= 2) {
            cpuPolicy0 = parsed[0].path
            cpuPolicy6 = parsed[1].path
        } else if (parsed.size == 1) {
            cpuPolicy0 = parsed[0].path
            cpuPolicy6 = null
        }
        return cpuPolicy0 to cpuPolicy6
    }

    suspend fun resolveGpuPowerPolicy(): String? {
        val directCandidates = listOf(
            "/sys/devices/platform/soc/13000000.mali/power_policy",
            "/sys/devices/platform/soc/13040000.mali/power_policy",
            "/sys/class/misc/mali0/device/power_policy",
            "/sys/module/mali_kbase/parameters/power_policy"
        )
        for (c in directCandidates) {
            val check = ShellManager.exec("test -f $c && echo ok")
            if (check.stdout == "ok") {
                gpuPowerPolicy = c
                return c
            }
        }
        val res = ShellManager.exec("find /sys/devices/platform -type f -name power_policy 2>/dev/null | head -n 1")
        if (res.isSuccess && res.stdout.isNotBlank()) {
            gpuPowerPolicy = res.stdout.trim()
        }
        return gpuPowerPolicy
    }

    suspend fun resolveGpuParamName(): String {
        val checkKhz = ShellManager.exec("test -f ${KernelConstants.PARAMS_DIR}/gpu_target_khz && echo yes")
        gpuFreqParamName = if (checkKhz.stdout == "yes") {
            "gpu_target_khz"
        } else {
            val checkFreq = ShellManager.exec("test -f ${KernelConstants.PARAMS_DIR}/gpu_target_freq && echo yes")
            if (checkFreq.stdout == "yes") "gpu_target_freq" else "gpu_target_khz"
        }
        return gpuFreqParamName
    }

    suspend fun isModuleLoaded(): Boolean {
        val res = ShellManager.exec("test -d /sys/module/${KernelConstants.MODULE_NAME} && test -d ${KernelConstants.PARAMS_DIR} && echo yes")
        return res.isSuccess && res.stdout == "yes"
    }

    suspend fun detectCpuInfo(): HardwareBrandInfo {
        val res = ShellManager.exec("""
            printf 'PROPS=\n'; getprop 2>/dev/null;
            printf '\nSOC=\n';
            for f in /sys/devices/soc0/soc_id /sys/devices/soc0/family /sys/devices/soc0/machine /sys/devices/system/soc/soc0/soc_id /sys/devices/system/soc/soc0/family /sys/devices/system/soc/soc0/machine; do [ -r "${'$'}f" ] && { printf '%s=' "${'$'}f"; cat "${'$'}f" 2>/dev/null; printf '\n'; }; done;
            printf '\nDT_MODEL='; tr '\0' '\n' < /proc/device-tree/model 2>/dev/null;
            printf '\nDT_COMPATIBLE='; tr '\0' '\n' < /proc/device-tree/compatible 2>/dev/null;
            printf '\nCPUINFO=\n'; cat /proc/cpuinfo 2>/dev/null
            printf '\nARCH=\n'; uname -m 2>/dev/null
            getprop ro.product.cpu.abi 2>/dev/null
            getprop ro.product.cpu.abilist 2>/dev/null
        """.trimIndent())

        val fallbackModel = android.os.Build.HARDWARE.ifEmpty { android.os.Build.BOARD.ifEmpty { "SoC" } }
        val fallbackManufacturer = "MediaTek"

        if (!res.isSuccess || res.stdout.isBlank()) {
            return HardwareBrandInfo(cpuBrandName = fallbackManufacturer, cpuBrandModel = fallbackModel)
        }

        val raw = res.stdout
        fun prop(name: String): String {
            val escaped = Pattern.quote(name)
            val matcher = Pattern.compile("^\\s*\\[$escaped\\]: \\[(.*?)\\]$", Pattern.MULTILINE).matcher(raw)
            return if (matcher.find()) matcher.group(1)?.trim() ?: "" else ""
        }
        fun clean(v: String?): String = (v ?: "").replace(Regex("\\s+"), " ").replace("\u0000", "").trim()

        val socModel = clean(prop("ro.soc.model"))
        val socManufacturer = clean(prop("ro.soc.manufacturer").ifEmpty { prop("ro.soc.vendor") })
        val platform = clean(prop("ro.mediatek.platform").ifEmpty { prop("ro.vendor.mediatek.platform") }.ifEmpty { prop("ro.board.platform") })
        val productBoard = clean(prop("ro.product.board"))
        val hardware = clean(prop("ro.hardware"))
        val dtModelMatcher = Pattern.compile("^DT_MODEL=(.*)$", Pattern.MULTILINE).matcher(raw)
        val dtModel = clean(if (dtModelMatcher.find()) dtModelMatcher.group(1) else "")
        val dtCompatibleMatcher = Pattern.compile("^DT_COMPATIBLE=(.*)$", Pattern.MULTILINE).matcher(raw)
        val dtCompatible = clean(if (dtCompatibleMatcher.find()) dtCompatibleMatcher.group(1) else "")
        val cpuText = raw.substringAfter("CPUINFO=", "")
        val archText = raw.substringAfter("ARCH=", "").trim()

        val haystack = listOf(socModel, socManufacturer, platform, productBoard, hardware, dtModel, dtCompatible, cpuText, archText, raw).joinToString(" ")
        val modelMatch = Pattern.compile("\\b(?:MT[0-9]{4,6}(?:V/[A-Z0-9]+|/[A-Z0-9]+|[A-Z]+)?)\\b", Pattern.CASE_INSENSITIVE).matcher(haystack)
        val model = if (modelMatch.find()) modelMatch.group(0)?.uppercase() ?: "" else socModel.ifEmpty { platform.ifEmpty { productBoard.ifEmpty { hardware.ifEmpty { fallbackModel } } } }

        val marketingMatcher = Pattern.compile("\\b(?:MediaTek\\s+)?(?:Helio|Dimensity)\\s+[A-Za-z0-9][A-Za-z0-9.+_-]*", Pattern.CASE_INSENSITIVE).matcher(haystack)
        val marketingMatches = mutableListOf<String>()
        while (marketingMatcher.find()) {
            marketingMatches.add(clean(marketingMatcher.group(0)).replace(Regex("^MediaTek\\s+", RegexOption.IGNORE_CASE), ""))
        }
        var family = marketingMatches.maxByOrNull { it.length } ?: ""
        if (family.isEmpty() && socManufacturer.isNotEmpty()) {
            family = socManufacturer
        }
        if (family.isEmpty() && socModel.isNotEmpty()) {
            family = socModel
        }
        if (family.isEmpty()) family = if (model.isNotEmpty()) model else fallbackManufacturer

        val partPattern = Pattern.compile("^CPU part\\s*:\\s*(0x[0-9a-f]+)", Pattern.CASE_INSENSITIVE or Pattern.MULTILINE)
        val partMatcher = partPattern.matcher(cpuText)
        val counts = mutableMapOf<String, Int>()
        val partName = mapOf("0xd05" to "Cortex-A55", "0xd0b" to "Cortex-A76", "0xd0a" to "Cortex-A75", "0xd08" to "Cortex-A72", "0xd03" to "Cortex-A53")
        while (partMatcher.find()) {
            val code = partMatcher.group(1)?.lowercase() ?: ""
            val name = partName[code]
            if (name != null) {
                counts[name] = (counts[name] ?: 0) + 1
            }
        }
        val archParts = counts.entries.map { "${it.value}× ${it.key}" }
        val cpuCountPattern = Pattern.compile("^processor\\s*:", Pattern.CASE_INSENSITIVE or Pattern.MULTILINE).matcher(cpuText)
        var cpuCount = 0
        while (cpuCountPattern.find()) cpuCount++
        val coresList = archParts.ifEmpty { if (cpuCount > 0) listOf("$cpuCount× CPU cores") else emptyList() }
        val footer = coresList.filter { it.isNotEmpty() }.joinToString(" · ")

        return HardwareBrandInfo(
            cpuBrandName = family,
            cpuBrandModel = model,
            cpuArchFooter = footer
        )
    }

    suspend fun detectGpuInfo(): HardwareBrandInfo {
        val fallbackGpu = android.os.Build.HARDWARE.ifEmpty { "GPU" }
        val res = ShellManager.exec("""
            printf 'SF=\n'; dumpsys SurfaceFlinger 2>/dev/null | grep -i -m5 -E 'GLES:|GL_RENDERER|renderer|gpu';
            printf '\nPROP=\n'; getprop 2>/dev/null | grep -Ei 'gpu|mali|panfrost|valhall|bifrost|utgard|gles|egl';
            printf '\nSYS=\n';
            for f in /sys/module/mali/version /sys/module/mali/uevent /sys/module/panfrost/uevent /sys/class/misc/mali0/uevent /sys/class/misc/mali0/device/uevent /sys/devices/platform/soc/*mali*/uevent /sys/devices/platform/soc/*gpu*/uevent; do [ -r "${'$'}f" ] && { printf '%s=' "${'$'}f"; cat "${'$'}f" 2>/dev/null; printf '\n'; }; done;
            printf '\nDEV=\n';
            for f in /proc/device-tree/compatible /proc/device-tree/model; do [ -r "${'$'}f" ] && { printf '%s=' "${'$'}f"; tr '\0' ' ' < "${'$'}f" 2>/dev/null; printf '\n'; }; done
        """.trimIndent())

        if (!res.isSuccess || res.stdout.isBlank()) {
            return HardwareBrandInfo(gpuBrandName = fallbackGpu, gpuBrandArch = "—", gpuFooter = fallbackGpu)
        }

        val raw = res.stdout
        val glesMatcher = Pattern.compile("^GLES:\\s*([^,\n]+),\\s*([^,\n]+),\\s*(OpenGL ES\\s+[0-9.]+(?:\\s+[^\n]+)?)\\s*$", Pattern.CASE_INSENSITIVE or Pattern.MULTILINE).matcher(raw)
        var gpuName = ""
        var glVersion = ""
        var vendor = ""

        if (glesMatcher.find()) {
            vendor = glesMatcher.group(1)?.trim() ?: ""
            gpuName = glesMatcher.group(2)?.trim() ?: ""
            glVersion = glesMatcher.group(3)?.trim() ?: ""
        }

        if (gpuName.isEmpty()) {
            val rendererPatterns = listOf(
                Pattern.compile("GL_RENDERER[^:]*:\\s*([^\n]+)", Pattern.CASE_INSENSITIVE),
                Pattern.compile("renderer[^:]*:\\s*([^\n]+)", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(?:Mali[-_ ]|Adreno\\s+|PowerVR\\s+)[^,\n]+", Pattern.CASE_INSENSITIVE)
            )
            for (p in rendererPatterns) {
                val m = p.matcher(raw)
                if (m.find()) {
                    gpuName = (m.group(1) ?: m.group(0)).trim()
                    if (gpuName.isNotEmpty()) break
                }
            }
        }

        if (gpuName.isEmpty()) gpuName = vendor.ifEmpty { fallbackGpu }
        val archMatch = Pattern.compile("\\b(Valhall|Bifrost|Utgard)\\b", Pattern.CASE_INSENSITIVE).matcher(raw)
        val gpuArch = if (archMatch.find()) {
            archMatch.group(1)?.replaceFirstChar { it.uppercase() } ?: "—"
        } else "—"

        val footerParts = listOf(gpuName, vendor, glVersion).filter { it.isNotEmpty() }.distinct()
        val footer = if (footerParts.isNotEmpty()) footerParts.joinToString(" · ") else gpuName

        return HardwareBrandInfo(
            gpuBrandName = gpuName,
            gpuBrandArch = gpuArch,
            gpuFooter = footer
        )
    }

    suspend fun refreshModuleState(stockFloors: StockFloors): ModuleState {
        val loaded = isModuleLoaded()
        if (!loaded) {
            return ModuleState(
                isLoaded = false,
                isCpuActive = false,
                isGpuActive = false,
                isFailed = false,
                statusText = "overclock_mt6789 not loaded",
                dotStatus = DotStatus.BAD
            )
        }

        val cLLRes = ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/cpu_ll_target_khz 2>/dev/null")
        val cBRes = ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/cpu_b_target_khz 2>/dev/null")
        val cResultRes = ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/cpu_oc_result 2>/dev/null")
        val gFreqRes = ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/$gpuFreqParamName 2>/dev/null")
        val gVoltRes = ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/gpu_target_volt 2>/dev/null")
        val gResultRes = ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/gpu_oc_result 2>/dev/null")

        val cpuLL = cLLRes.stdout.toLongOrNull() ?: 0L
        val cpuB = cBRes.stdout.toLongOrNull() ?: 0L
        val cResult = cResultRes.stdout
        val gpuFreq = gFreqRes.stdout.toLongOrNull() ?: 0L
        val gpuVolt = gVoltRes.stdout.toLongOrNull() ?: 0L
        val gResult = gResultRes.stdout

        val cpuResultFailed = cResult.startsWith("FAIL", ignoreCase = true)
        val gpuResultFailed = gResult.startsWith("FAIL", ignoreCase = true)
        val gpuDisabled = gResult.contains("disabled", ignoreCase = true) || gResult.contains("stock restored", ignoreCase = true)

        val parsedCpu = ConversionUtils.parseCpuResult(cResult)
        val parsedGpu = ConversionUtils.parseGpuResult(gResult)

        val effCpuLL = if (parsedCpu != null) ConversionUtils.mhzToKhz(parsedCpu.llMhz) else cpuLL
        val effCpuB = if (parsedCpu != null) ConversionUtils.mhzToKhz(parsedCpu.bMhz) else cpuB
        val effGpuFreq = if (parsedGpu != null && parsedGpu.enabled && parsedGpu.freqMhz != null) {
            ConversionUtils.mhzToKhz(parsedGpu.freqMhz)
        } else gpuFreq
        val effGpuVolt = if (parsedGpu != null && parsedGpu.enabled && parsedGpu.voltMv != null) {
            ConversionUtils.mvToRaw(parsedGpu.voltMv)
        } else gpuVolt

        val stockCpuLL = stockFloors.cpuLLMin?.let { ConversionUtils.mhzToKhz(it) } ?: 0L
        val stockCpuB = stockFloors.cpuBMin?.let { ConversionUtils.mhzToKhz(it) } ?: 0L
        val stockGpuFreq = stockFloors.gpuFreqMin?.let { ConversionUtils.mhzToKhz(it) } ?: 0L
        val stockGpuVolt = stockFloors.gpuVoltMin?.let { ConversionUtils.mvToRaw(it) } ?: 0L

        val cpuRequestedOc = ConversionUtils.isCpuOcRequested(effCpuLL, effCpuB, stockCpuLL, stockCpuB)
        val gpuRequestedOc = ConversionUtils.isGpuOcRequested(effGpuFreq, effGpuVolt, stockGpuFreq, stockGpuVolt, gpuDisabled)

        // Ignore stale FAIL results if no OC is requested
        val cpuFailed = cpuRequestedOc && cpuResultFailed
        val gpuFailed = gpuRequestedOc && gpuResultFailed

        val cpuActive = cpuRequestedOc && !cpuFailed
        val gpuActive = gpuRequestedOc && !gpuFailed
        val failed = cpuFailed || gpuFailed

        val (statusText, dotStatus) = when {
            failed -> "module: loaded · OC failed" to DotStatus.BAD
            cpuActive || gpuActive -> "module: loaded · OC active" to DotStatus.WARN
            else -> "module: loaded" to DotStatus.OK
        }

        return ModuleState(
            isLoaded = true,
            isCpuActive = cpuActive,
            isGpuActive = gpuActive,
            isFailed = failed,
            statusText = statusText,
            dotStatus = dotStatus
        )
    }

    suspend fun refreshLiveHardware(
        gpuOppMap: Map<Int, GpuOppEntry>,
        cpuActive: Boolean,
        gpuActive: Boolean
    ): LiveHardwareStats {
        var cllMhz = "—"
        var cbMhz = "—"
        if (!cpuPolicy0.isNullOrBlank()) {
            val r = ShellManager.exec("cat $cpuPolicy0/scaling_cur_freq 2>/dev/null")
            val khz = r.stdout.toLongOrNull()
            if (khz != null && khz > 0) cllMhz = "${Math.round(ConversionUtils.khzToMhz(khz))} MHz"
        }
        if (!cpuPolicy6.isNullOrBlank()) {
            val r = ShellManager.exec("cat $cpuPolicy6/scaling_cur_freq 2>/dev/null")
            val khz = r.stdout.toLongOrNull()
            if (khz != null && khz > 0) cbMhz = "${Math.round(ConversionUtils.khzToMhz(khz))} MHz"
        }

        var gpuFreqStr = "—"
        var gpuVoltStr = "—"
        val gedRes = ShellManager.exec("cat ${KernelConstants.GPU_CUR_FREQ} 2>/dev/null")
        if (gedRes.isSuccess && gedRes.stdout.isNotBlank()) {
            val parts = gedRes.stdout.trim().split(Regex("\\s+"))
            if (parts.size >= 2) {
                val idx = parts[0].toIntOrNull()
                val freqKhz = parts[1].toLongOrNull()
                if (freqKhz != null && freqKhz > 0) {
                    gpuFreqStr = "${Math.round(ConversionUtils.khzToMhz(freqKhz))} MHz"
                }
                if (idx != null) {
                    val opp = gpuOppMap[idx]
                    if (opp != null) {
                        gpuVoltStr = "${String.format("%.2f", opp.voltMv)} mV"
                    }
                }
            }
        }

        return LiveHardwareStats(
            cpuLLCurMhz = cllMhz,
            cpuBCurMhz = cbMhz,
            cpuStatus = if (cpuActive) "OC active" else "Stock",
            isCpuActive = cpuActive,
            gpuCurFreqMhz = gpuFreqStr,
            gpuCurVoltMv = gpuVoltStr,
            gpuStatus = if (gpuActive) "OC active" else "Stock",
            isGpuActive = gpuActive
        )
    }

    suspend fun applyCpu(
        llMhz: Int,
        bMhz: Int,
        stockFloors: StockFloors
    ): Pair<Boolean, String> = ShellManager.runSerializedWrite {
        if (!stockFloors.isCpuKnown) {
            return@runSerializedWrite false to "CPU stock baseline unknown, refusing to apply"
        }

        val stockLL = stockFloors.cpuLLMin!!
        val stockB = stockFloors.cpuBMin!!
        val requestedOc = llMhz > stockLL || bMhz > stockB

        val resultMsg: String
        val isSuccess: Boolean

        if (!requestedOc) {
            // Stock request -> write 0 to parameters
            ShellManager.exec("echo 0 > ${KernelConstants.PARAMS_DIR}/cpu_ll_target_khz 2>&1")
            ShellManager.exec("echo 0 > ${KernelConstants.PARAMS_DIR}/cpu_b_target_khz 2>&1")
            ShellManager.exec("echo 1 > ${KernelConstants.PARAMS_DIR}/cpu_oc_apply 2>&1")
            ShellManager.exec("echo 1 > ${KernelConstants.PARAMS_DIR}/cpu_stats_refresh 2>&1")
            ShellManager.exec("echo 2 > ${KernelConstants.PARAMS_DIR}/cpu_stats_refresh 2>&1")
            val res = ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/cpu_oc_result 2>/dev/null").stdout
            val failed = res.contains("FAIL", ignoreCase = true) && !res.contains("disabled", ignoreCase = true) && !res.contains("stock restored", ignoreCase = true)
            isSuccess = !failed
            resultMsg = if (failed) "CPU stock restore failed" else "CPU stock applied"
        } else {
            val llKhz = ConversionUtils.mhzToKhz(llMhz)
            val bKhz = ConversionUtils.mhzToKhz(bMhz)
            ShellManager.exec("echo $llKhz > ${KernelConstants.PARAMS_DIR}/cpu_ll_target_khz 2>&1")
            ShellManager.exec("echo $bKhz > ${KernelConstants.PARAMS_DIR}/cpu_b_target_khz 2>&1")
            ShellManager.exec("echo 1 > ${KernelConstants.PARAMS_DIR}/cpu_oc_apply 2>&1")
            ShellManager.exec("echo 1 > ${KernelConstants.PARAMS_DIR}/cpu_stats_refresh 2>&1")
            ShellManager.exec("echo 2 > ${KernelConstants.PARAMS_DIR}/cpu_stats_refresh 2>&1")
            val res = ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/cpu_oc_result 2>/dev/null").stdout
            val failed = ConversionUtils.classifyResult(res) == "fail"
            isSuccess = !failed
            resultMsg = if (failed) "CPU apply failed" else "CPU OC applied"
        }

        isSuccess to resultMsg
    }

    suspend fun restoreCpu(stockFloors: StockFloors): Pair<Boolean, String> = ShellManager.runSerializedWrite {
        if (!stockFloors.isCpuKnown) {
            return@runSerializedWrite false to "CPU stock baseline unknown, refusing to restore blind"
        }
        ShellManager.exec("echo 0 > ${KernelConstants.PARAMS_DIR}/cpu_ll_target_khz 2>&1")
        ShellManager.exec("echo 0 > ${KernelConstants.PARAMS_DIR}/cpu_b_target_khz 2>&1")
        ShellManager.exec("echo 1 > ${KernelConstants.PARAMS_DIR}/cpu_oc_apply 2>&1")
        ShellManager.exec("echo 1 > ${KernelConstants.PARAMS_DIR}/cpu_stats_refresh 2>&1")
        ShellManager.exec("echo 2 > ${KernelConstants.PARAMS_DIR}/cpu_stats_refresh 2>&1")
        ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/cpu_oc_result 2>/dev/null")
        true to "CPU restored to stock"
    }

    suspend fun applyGpu(
        freqMhz: Int,
        voltMv: Int,
        stockFloors: StockFloors
    ): Pair<Boolean, String> = ShellManager.runSerializedWrite {
        if (!stockFloors.isGpuKnown) {
            return@runSerializedWrite false to "GPU stock baseline unknown, refusing to apply"
        }

        val stockFreq = stockFloors.gpuFreqMin!!
        val stockVolt = stockFloors.gpuVoltMin!!
        val requestedOc = freqMhz > stockFreq || voltMv > stockVolt

        val resultMsg: String
        val isSuccess: Boolean

        if (!requestedOc) {
            ShellManager.exec("echo 0 > ${KernelConstants.PARAMS_DIR}/$gpuFreqParamName 2>&1")
            ShellManager.exec("echo 0 > ${KernelConstants.PARAMS_DIR}/gpu_target_volt 2>&1")
            ShellManager.exec("echo 0 > ${KernelConstants.PARAMS_DIR}/gpu_target_vsram 2>&1")
            ShellManager.exec("echo 1 > ${KernelConstants.PARAMS_DIR}/gpu_oc_apply 2>&1")
            val res = ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/gpu_oc_result 2>/dev/null").stdout
            val failed = res.contains("FAIL", ignoreCase = true) && !res.contains("disabled", ignoreCase = true) && !res.contains("stock restored", ignoreCase = true)
            isSuccess = !failed
            resultMsg = if (failed) "GPU stock restore failed" else "GPU stock applied"
        } else {
            val freqKhz = ConversionUtils.mhzToKhz(freqMhz)
            val voltRaw = ConversionUtils.mvToRaw(voltMv)
            ShellManager.exec("echo $freqKhz > ${KernelConstants.PARAMS_DIR}/$gpuFreqParamName 2>&1")
            ShellManager.exec("echo $voltRaw > ${KernelConstants.PARAMS_DIR}/gpu_target_volt 2>&1")
            ShellManager.exec("echo $voltRaw > ${KernelConstants.PARAMS_DIR}/gpu_target_vsram 2>&1")
            ShellManager.exec("echo 1 > ${KernelConstants.PARAMS_DIR}/gpu_oc_apply 2>&1")
            val res = ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/gpu_oc_result 2>/dev/null").stdout
            val failed = ConversionUtils.classifyResult(res) == "fail"
            isSuccess = !failed
            resultMsg = if (failed) "GPU apply failed" else "GPU OC applied"
        }

        isSuccess to resultMsg
    }

    suspend fun restoreGpu(stockFloors: StockFloors): Pair<Boolean, String> = ShellManager.runSerializedWrite {
        if (!stockFloors.isGpuKnown) {
            return@runSerializedWrite false to "GPU stock baseline unknown, refusing to restore blind"
        }
        ShellManager.exec("echo 0 > ${KernelConstants.PARAMS_DIR}/$gpuFreqParamName 2>&1")
        ShellManager.exec("echo 0 > ${KernelConstants.PARAMS_DIR}/gpu_target_volt 2>&1")
        ShellManager.exec("echo 0 > ${KernelConstants.PARAMS_DIR}/gpu_target_vsram 2>&1")
        ShellManager.exec("echo 1 > ${KernelConstants.PARAMS_DIR}/gpu_oc_apply 2>&1")
        ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/gpu_oc_result 2>/dev/null")
        true to "GPU restored to stock"
    }

    // Direct controls for CPU & GPU
    suspend fun readGpuOppTable(): Map<Int, GpuOppEntry> {
        val res = ShellManager.exec("cat ${KernelConstants.GPU_WORKING_OPP} 2>/dev/null")
        return ConversionUtils.parseGpuOppTable(res.stdout)
    }

    suspend fun setGpuGovernor(gov: String): Pair<Boolean, String> {
        val cleanGov = gov.trim()
        val policyPath = gpuPowerPolicy ?: resolveGpuPowerPolicy()
        if (policyPath.isNullOrBlank()) return false to "GPU power_policy node not found"

        // Ensure writable node
        ShellManager.exec("chmod 664 $policyPath 2>/dev/null; chown root:root $policyPath 2>/dev/null")
        val writeRes = ShellManager.exec("echo '$cleanGov' > $policyPath 2>&1")
        delay(30)

        val checkRes = ShellManager.exec("cat $policyPath 2>/dev/null")
        val activeGov = ConversionUtils.parseActiveGov(checkRes.stdout) ?: checkRes.stdout.trim()

        return if (activeGov.equals(cleanGov, ignoreCase = true) || writeRes.isSuccess) {
            true to "GPU governor: $cleanGov"
        } else {
            val err = writeRes.stderr.ifEmpty { writeRes.stdout }.take(40).ifEmpty { "Rejected" }
            false to "Failed ($err)"
        }
    }

    suspend fun setGpuMinFreq(oppIdx: Int): Pair<Boolean, String> {
        val ok = ShellManager.exec("echo $oppIdx > ${KernelConstants.GPU_BOOST} 2>&1").isSuccess
        return if (ok) true to "GPU min: $oppIdx" else false to "Failed"
    }

    suspend fun setGpuMaxFreq(oppIdx: Int): Pair<Boolean, String> {
        val ok = ShellManager.exec("echo $oppIdx > ${KernelConstants.GPU_UPBOUND} 2>&1").isSuccess
        return if (ok) true to "GPU max: $oppIdx" else false to "Failed"
    }

    suspend fun setCpuGovernor(policyPath: String, clusterLabel: String, gov: String): Pair<Boolean, String> {
        val cleanGov = gov.trim()
        val govNode = "$policyPath/scaling_governor"

        // Ensure permissions on scaling_governor node
        ShellManager.exec("chmod 664 $govNode 2>/dev/null; chown root:root $govNode 2>/dev/null")
        val writeRes = ShellManager.exec("echo '$cleanGov' > $govNode 2>&1")
        delay(30)

        val curGov = ShellManager.exec("cat $govNode 2>/dev/null").stdout.trim()
        return if (curGov.equals(cleanGov, ignoreCase = true) || writeRes.isSuccess) {
            true to "$clusterLabel gov: $cleanGov"
        } else {
            val err = writeRes.stderr.ifEmpty { writeRes.stdout }.take(40).ifEmpty { "Rejected by kernel" }
            false to "$clusterLabel gov failed ($err)"
        }
    }

    private suspend fun readCpuBounds(policyPath: String): Pair<Long, Long> {
        val minParam = if (policyPath == cpuPolicy0) "cpu_ll_min_khz" else "cpu_b_min_khz"
        val minRaw = ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/$minParam 2>/dev/null").stdout.toLongOrNull() ?: 0L
        val maxRaw = ShellManager.exec("cat $policyPath/scaling_max_freq 2>/dev/null").stdout.toLongOrNull() ?: 0L
        return minRaw to maxRaw
    }

    private suspend fun reapplyCpuOc(policyPath: String, targetKhz: Long): Boolean {
        return ShellManager.runCpuOcApply {
            val param = if (policyPath == cpuPolicy0) "cpu_ll_target_khz" else "cpu_b_target_khz"
            val target = Math.min(targetKhz, KernelConstants.CPU_OC_ABS_CAP_KHZ)
            ShellManager.exec("echo $target > ${KernelConstants.PARAMS_DIR}/$param 2>&1")
            ShellManager.exec("echo 1 > ${KernelConstants.PARAMS_DIR}/cpu_oc_apply 2>&1")
            delay(30)
            val result = ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/cpu_oc_result 2>/dev/null").stdout
            ConversionUtils.classifyResult(result) != "fail"
        }
    }

    private suspend fun setCpuFreqLocked(policyPath: String, targetKhz: Long): Pair<Boolean, Long?> {
        val minParam = if (policyPath == cpuPolicy0) "cpu_ll_min_khz" else "cpu_b_min_khz"
        for (attempt in 0 until 3) {
            val applied = reapplyCpuOc(policyPath, targetKhz)
            if (!applied) continue

            ShellManager.exec("echo $targetKhz > $policyPath/scaling_max_freq 2>&1")
            delay(15)
            ShellManager.exec("echo $targetKhz > ${KernelConstants.PARAMS_DIR}/$minParam 2>&1")
            delay(15)

            val bounds = readCpuBounds(policyPath)
            if (bounds.first == targetKhz && bounds.second == targetKhz) {
                return true to targetKhz
            }
            delay(50)
        }
        val finalBounds = readCpuBounds(policyPath)
        return false to finalBounds.second
    }

    suspend fun setCpuPolicyMin(
        policyPath: String,
        clusterLabel: String,
        targetKhz: Long
    ): Pair<Boolean, String> = ShellManager.runPolicyTask(policyPath) {
        val currentMaxRaw = ShellManager.exec("cat $policyPath/scaling_max_freq 2>/dev/null").stdout.toLongOrNull()
        if (currentMaxRaw == null) {
            return@runPolicyTask false to "$clusterLabel min rejected: could not read current max"
        }

        if (targetKhz >= currentMaxRaw) {
            val locked = setCpuFreqLocked(policyPath, targetKhz)
            return@runPolicyTask if (locked.first) {
                true to "$clusterLabel min: $targetKhz"
            } else {
                false to "$clusterLabel min rejected: ${locked.second?.let { "${it / 1000} MHz" } ?: "Failed"}"
            }
        }

        var bounds = readCpuBounds(policyPath)
        if (targetKhz > bounds.second) {
            val applied = reapplyCpuOc(policyPath, targetKhz)
            bounds = readCpuBounds(policyPath)
            if (!applied || targetKhz > bounds.second) {
                return@runPolicyTask false to "$clusterLabel min rejected: ${bounds.second / 1000} MHz"
            }
        }

        val minParam = if (policyPath == cpuPolicy0) "cpu_ll_min_khz" else "cpu_b_min_khz"
        val writeOk = ShellManager.exec("echo $targetKhz > ${KernelConstants.PARAMS_DIR}/$minParam 2>&1").isSuccess
        delay(15)
        val got = ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/$minParam 2>/dev/null").stdout.toLongOrNull() ?: 0L

        return@runPolicyTask if (writeOk && got == targetKhz) {
            true to "$clusterLabel min: $targetKhz"
        } else {
            false to "$clusterLabel min rejected: ${if (got > 0) "${got / 1000} MHz" else "Failed"}"
        }
    }

    suspend fun setCpuPolicyMax(
        policyPath: String,
        clusterLabel: String,
        targetKhz: Long
    ): Pair<Boolean, String> = ShellManager.runPolicyTask(policyPath) {
        var bounds = readCpuBounds(policyPath)
        val minParam = if (policyPath == cpuPolicy0) "cpu_ll_min_khz" else "cpu_b_min_khz"

        val isOc = targetKhz > bounds.second
        if (targetKhz > bounds.second) {
            val applied = reapplyCpuOc(policyPath, targetKhz)
            bounds = readCpuBounds(policyPath)
            if (!applied || targetKhz > bounds.second) {
                return@runPolicyTask false to "$clusterLabel max rejected: ${bounds.second / 1000} MHz"
            }
        }

        if (bounds.first > targetKhz) {
            val minOk = ShellManager.exec("echo $targetKhz > ${KernelConstants.PARAMS_DIR}/$minParam 2>&1").isSuccess
            delay(15)
            val minAfter = ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/$minParam 2>/dev/null").stdout.toLongOrNull() ?: 0L
            if (!minOk || minAfter != targetKhz) {
                return@runPolicyTask false to "$clusterLabel max rejected: ${minAfter / 1000} MHz"
            }
        }

        val writeOk = ShellManager.exec("echo $targetKhz > $policyPath/scaling_max_freq 2>&1").isSuccess
        delay(15)
        val got = ShellManager.exec("cat $policyPath/scaling_max_freq 2>/dev/null").stdout.toLongOrNull() ?: 0L

        return@runPolicyTask if (writeOk && got == targetKhz) {
            val msg = if (isOc) "$clusterLabel max: $targetKhz · OC" else "$clusterLabel max: $targetKhz"
            true to msg
        } else {
            false to "$clusterLabel max rejected: ${if (got > 0) "${got / 1000} MHz" else "Failed"}"
        }
    }

    suspend fun readCpuClusterControl(policyPath: String?): CpuControlCluster {
        if (policyPath.isNullOrBlank()) return CpuControlCluster()
        val govListRes = ShellManager.exec("cat $policyPath/scaling_available_governors 2>/dev/null")
        val curGovRes = ShellManager.exec("cat $policyPath/scaling_governor 2>/dev/null")
        val freqsRes = ShellManager.exec("cat $policyPath/scaling_available_frequencies 2>/dev/null")
        val minParam = if (policyPath == cpuPolicy0) "cpu_ll_min_khz" else "cpu_b_min_khz"
        val curMinRes = ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/$minParam 2>/dev/null")
        val curMaxRes = ShellManager.exec("cat $policyPath/scaling_max_freq 2>/dev/null")

        val govs = govListRes.stdout.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.distinct()
        val freqs = freqsRes.stdout.trim().split(Regex("\\s+")).mapNotNull { it.toLongOrNull() }.distinct().sortedDescending()

        return CpuControlCluster(
            availableGovernors = govs,
            availableFrequenciesKhz = freqs,
            currentGovernor = curGovRes.stdout.trim().ifEmpty { "—" },
            currentMinKhz = curMinRes.stdout.toLongOrNull(),
            currentMaxKhz = curMaxRes.stdout.toLongOrNull()
        )
    }

    suspend fun readGpuControlState(): GpuControlState {
        val policyPath = gpuPowerPolicy ?: resolveGpuPowerPolicy()
        val govRes = if (!policyPath.isNullOrBlank()) ShellManager.exec("cat $policyPath 2>/dev/null") else ShellResult(0, "", "")
        val oppTable = readGpuOppTable()
        val minRes = ShellManager.exec("cat ${KernelConstants.GPU_BOOST} 2>/dev/null")
        val maxRes = ShellManager.exec("cat ${KernelConstants.GPU_UPBOUND} 2>/dev/null")

        val currentGov = ConversionUtils.parseActiveGov(govRes.stdout) ?: govRes.stdout.trim().ifEmpty { "—" }
        val parsedGovs = govRes.stdout.trim().split(Regex("\\s+"))
            .map { it.replace("[", "").replace("]", "").replace("(", "").replace(")", "").trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        val availableGovs = if (parsedGovs.isNotEmpty()) {
            parsedGovs
        } else if (currentGov != "—" && currentGov.isNotEmpty()) {
            listOf(currentGov)
        } else {
            emptyList()
        }

        return GpuControlState(
            availableGovernors = availableGovs,
            oppTable = oppTable,
            currentGovernor = currentGov,
            currentMinOppIdx = minRes.stdout.toIntOrNull(),
            currentMaxOppIdx = maxRes.stdout.toIntOrNull()
        )
    }
}
