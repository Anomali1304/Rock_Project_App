package com.rockproject.mt6789.util

import com.rockproject.mt6789.model.CpuParsedResult
import com.rockproject.mt6789.model.GpuOppEntry
import com.rockproject.mt6789.model.GpuParsedResult
import java.util.regex.Pattern

object ConversionUtils {

    fun mhzToKhz(mhz: Number): Long = Math.round(mhz.toDouble() * 1000.0)

    fun khzToMhz(khz: Number): Double = khz.toDouble() / 1000.0

    fun mvToRaw(mv: Number): Long = Math.round(mv.toDouble() * 100.0)

    fun rawToMv(raw: Number): Double = raw.toDouble() / 100.0

    fun getMaxCpuMhzForStock(stockMhz: Int?): Int {
        if (stockMhz == null) return 2600
        return if (stockMhz <= 1540) 3000 else 2600
    }

    fun roundToStep(value: Double, step: Int): Int {
        if (step <= 0) return Math.round(value).toInt()
        return (Math.round(value / step.toDouble()) * step).toInt()
    }

    fun classifyResult(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val trimmed = text.trim()
        return when {
            trimmed.startsWith("OK", ignoreCase = true) -> "ok"
            trimmed.startsWith("FAIL", ignoreCase = true) -> "fail"
            else -> null
        }
    }

    fun parseCpuResult(text: String?): CpuParsedResult? {
        if (text.isNullOrBlank()) return null
        val littlePattern = Pattern.compile("(?:LL|little)(?:_idx\\d+)?=(\\d+)KHz", Pattern.CASE_INSENSITIVE)
        val bigPattern = Pattern.compile("(?:b|big)(?:_idx\\d+)?=(\\d+)KHz", Pattern.CASE_INSENSITIVE)
        val primePattern = Pattern.compile("prime(?:_idx\\d+)?=(\\d+)KHz", Pattern.CASE_INSENSITIVE)

        val littleMatcher = littlePattern.matcher(text)
        val bigMatcher = bigPattern.matcher(text)
        val primeMatcher = primePattern.matcher(text)

        if (littleMatcher.find() && bigMatcher.find()) {
            val littleKhz = littleMatcher.group(1)?.toLongOrNull() ?: return null
            val bigKhz = bigMatcher.group(1)?.toLongOrNull() ?: return null
            val primeKhz = if (primeMatcher.find()) primeMatcher.group(1)?.toLongOrNull() else null
            return CpuParsedResult(
                littleMhz = khzToMhz(littleKhz),
                bigMhz = khzToMhz(bigKhz),
                primeMhz = primeKhz?.takeIf { it > 0 }?.let { khzToMhz(it) }
            )
        }
        return null
    }

    fun parseGpuResult(text: String?): GpuParsedResult? {
        if (text.isNullOrBlank()) return null
        if (text.contains("disabled", ignoreCase = true) || text.contains("stock restored", ignoreCase = true)) {
            return GpuParsedResult(enabled = false)
        }
        val freqPattern = Pattern.compile("freq=(\\d+)", Pattern.CASE_INSENSITIVE)
        val voltPattern = Pattern.compile("volt=(\\d+)", Pattern.CASE_INSENSITIVE)

        val freqMatcher = freqPattern.matcher(text)
        val voltMatcher = voltPattern.matcher(text)

        if (freqMatcher.find()) {
            val freqKhz = freqMatcher.group(1)?.toLongOrNull() ?: return null
            val voltRaw = if (voltMatcher.find()) voltMatcher.group(1)?.toLongOrNull() else null
            return GpuParsedResult(
                enabled = true,
                freqMhz = khzToMhz(freqKhz),
                voltMv = voltRaw?.let { rawToMv(it) }
            )
        }
        return null
    }

    fun parseGpuOppTable(content: String?): Map<Int, GpuOppEntry> {
        val map = mutableMapOf<Int, GpuOppEntry>()
        if (content.isNullOrBlank()) return map

        val linePattern = Pattern.compile("\\[(\\d+)\\]\\s*freq:\\s*(\\d+),\\s*volt:\\s*(\\d+)")
        for (line in content.lines()) {
            val matcher = linePattern.matcher(line)
            if (matcher.find()) {
                val idx = matcher.group(1)?.toIntOrNull() ?: continue
                val freqKhz = matcher.group(2)?.toLongOrNull() ?: continue
                val voltRaw = matcher.group(3)?.toLongOrNull() ?: continue
                map[idx] = GpuOppEntry(
                    index = idx,
                    freqMhz = khzToMhz(freqKhz),
                    voltMv = rawToMv(voltRaw)
                )
            }
        }
        return map
    }

    fun getGpuStockMax(oppMap: Map<Int, GpuOppEntry>): GpuOppEntry? {
        if (oppMap.isEmpty()) return null
        return oppMap.values.maxByOrNull { it.freqMhz }
    }

    fun parseActiveGov(content: String?): String? {
        if (content.isNullOrBlank()) return null
        val bracketMatch = Pattern.compile("[\\[(](\\w+)[)\\]]").matcher(content)
        if (bracketMatch.find()) {
            return bracketMatch.group(1)
        }
        val tokens = content.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return if (tokens.size == 1) tokens[0] else null
    }

    fun isCpuOcRequested(
        effLittleKhz: Long,
        effBigKhz: Long,
        stockLittleKhz: Long,
        stockBigKhz: Long,
        effPrimeKhz: Long = 0L,
        stockPrimeKhz: Long = 0L
    ): Boolean {
        val known = stockLittleKhz > 0 && stockBigKhz > 0
        val primeOc = stockPrimeKhz > 0 && effPrimeKhz > stockPrimeKhz
        return known && (effLittleKhz > stockLittleKhz || effBigKhz > stockBigKhz || primeOc)
    }

    fun isGpuOcRequested(
        effFreqKhz: Long,
        effVoltRaw: Long,
        stockFreqKhz: Long,
        stockVoltRaw: Long,
        disabled: Boolean
    ): Boolean {
        val known = stockFreqKhz > 0 && stockVoltRaw > 0
        return !disabled && known && (effFreqKhz > stockFreqKhz || effVoltRaw > stockVoltRaw)
    }

    fun parseCpuRangeList(rangeText: String?): List<Int> {
        if (rangeText.isNullOrBlank()) return emptyList()
        val result = sortedSetOf<Int>()
        for (token in rangeText.trim().split(",")) {
            val part = token.trim()
            if (part.isEmpty()) continue
            if (part.contains("-")) {
                val bounds = part.split("-")
                if (bounds.size == 2) {
                    val start = bounds[0].trim().toIntOrNull()
                    val end = bounds[1].trim().toIntOrNull()
                    if (start != null && end != null && start <= end && end - start <= 1024) {
                        for (i in start..end) result.add(i)
                    }
                }
            } else {
                part.toIntOrNull()?.let { result.add(it) }
            }
        }
        return result.toList()
    }

    fun resolveCpuCompatName(compatText: String?): String? {
        if (compatText.isNullOrBlank()) return null
        val tokens = compatText.split(",", "\u0000", "\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        for (token in tokens) {
            val lower = token.lowercase()
            if (lower == "arm,armv8" || lower == "arm,armv9" || lower == "arm,arm-v8" || lower == "arm,arm-v9" || lower == "linux,cpu") {
                continue
            }
            val cortexMatch = Pattern.compile("^arm,cortex-([a-z0-9-]+)$", Pattern.CASE_INSENSITIVE).matcher(lower)
            if (cortexMatch.find()) {
                val suffix = cortexMatch.group(1)?.uppercase() ?: continue
                return "Cortex-$suffix"
            }
            val neoverseMatch = Pattern.compile("^arm,neoverse-([a-z0-9-]+)$", Pattern.CASE_INSENSITIVE).matcher(lower)
            if (neoverseMatch.find()) {
                val suffix = neoverseMatch.group(1)?.uppercase() ?: continue
                return "Neoverse-$suffix"
            }
            if (lower.startsWith("qcom,")) {
                val model = token.substringAfter(",").replace("-", " ")
                return model.replaceFirstChar { it.uppercase() }
            }
        }
        return null
    }

    fun resolveArmPartName(implementer: Int?, part: Int?): String? {
        if (part == null) return null
        if (implementer == null || implementer == 0x41) {
            return when (part) {
                0xd03 -> "Cortex-A53"
                0xd04 -> "Cortex-A35"
                0xd05 -> "Cortex-A55"
                0xd06 -> "Cortex-A65"
                0xd07 -> "Cortex-A57"
                0xd08 -> "Cortex-A72"
                0xd09 -> "Cortex-A73"
                0xd0a -> "Cortex-A75"
                0xd0b -> "Cortex-A76"
                0xd0c -> "Neoverse-N1"
                0xd0d -> "Cortex-A77"
                0xd0e -> "Cortex-A76AE"
                0xd40 -> "Neoverse-V1"
                0xd41 -> "Cortex-A78"
                0xd42 -> "Cortex-A78AE"
                0xd43 -> "Cortex-A65AE"
                0xd44 -> "Cortex-X1"
                0xd46 -> "Cortex-A510"
                0xd47 -> "Cortex-A710"
                0xd48 -> "Cortex-X2"
                0xd49 -> "Neoverse-N2"
                0xd4b -> "Cortex-A78C"
                0xd4c -> "Cortex-X1C"
                0xd4d -> "Cortex-A715"
                0xd4e -> "Cortex-X3"
                0xd4f -> "Neoverse-V2"
                0xd80 -> "Cortex-A520"
                0xd81 -> "Cortex-A720"
                0xd82 -> "Cortex-X4"
                0xd84 -> "Neoverse-V3"
                0xd85 -> "Cortex-X925"
                0xd87 -> "Cortex-A725"
                else -> "ARM 0x${part.toString(16).lowercase()}"
            }
        }
        return when (implementer) {
            0x51 -> "Kryo 0x${part.toString(16).lowercase()}"
            0x53 -> "Samsung 0x${part.toString(16).lowercase()}"
            0x48 -> "HiSilicon 0x${part.toString(16).lowercase()}"
            else -> "CPU 0x${part.toString(16).lowercase()}"
        }
    }

    fun decodeMidr(midrText: String?): String? {
        if (midrText.isNullOrBlank()) return null
        val cleaned = midrText.trim().removePrefix("0x").removePrefix("0X")
        val midr = cleaned.toLongOrNull(16) ?: return null
        if (midr == 0L) return null
        val implementer = ((midr ushr 24) and 0xFFL).toInt()
        val part = ((midr ushr 4) and 0xFFFL).toInt()
        return resolveArmPartName(implementer, part)
    }

    fun parseCpuArchFooter(
        presentText: String?,
        sysCpuText: String?,
        dtCpusText: String?,
        policiesText: String?,
        cpuInfoText: String?
    ): String {
        val coreArch = mutableMapOf<Int, String>()
        val allCpuIds = sortedSetOf<Int>()

        allCpuIds.addAll(parseCpuRangeList(presentText))

        // 1) Parse per-CPU sysfs info (midr_el1 & of_node/compatible)
        if (!sysCpuText.isNullOrBlank()) {
            val sysLinePattern = Pattern.compile("^cpu=(\\d+)\\s+midr=(\\S*)\\s+compat=(.*)$")
            for (line in sysCpuText.lines()) {
                val m = sysLinePattern.matcher(line.trim())
                if (m.find()) {
                    val idx = m.group(1)?.toIntOrNull() ?: continue
                    val midr = m.group(2)?.trim()
                    val compat = m.group(3)?.trim()
                    allCpuIds.add(idx)
                    val resolved = resolveCpuCompatName(compat) ?: decodeMidr(midr)
                    if (!resolved.isNullOrBlank()) {
                        coreArch[idx] = resolved
                    }
                }
            }
        }

        // 2) Parse /proc/cpuinfo per-processor blocks
        if (!cpuInfoText.isNullOrBlank()) {
            var currentProc: Int? = null
            var currentImpl: Int? = null
            var currentPart: Int? = null
            var currentModel: String? = null

            fun flushBlock() {
                val proc = currentProc ?: return
                allCpuIds.add(proc)
                if (!coreArch.containsKey(proc)) {
                    val resolved = resolveArmPartName(currentImpl, currentPart)
                        ?: currentModel?.takeIf { it.isNotBlank() }
                    if (!resolved.isNullOrBlank()) {
                        coreArch[proc] = resolved
                    }
                }
            }

            for (rawLine in cpuInfoText.lines()) {
                val line = rawLine.trim()
                if (line.isEmpty()) {
                    flushBlock()
                    currentProc = null
                    currentImpl = null
                    currentPart = null
                    currentModel = null
                    continue
                }
                val colonIdx = line.indexOf(':')
                if (colonIdx <= 0) continue
                val key = line.substring(0, colonIdx).trim().lowercase()
                val value = line.substring(colonIdx + 1).trim()
                when (key) {
                    "processor" -> {
                        val nextProc = value.toIntOrNull()
                        if (nextProc != null) {
                            if (currentProc != null) {
                                flushBlock()
                                currentImpl = null
                                currentPart = null
                                currentModel = null
                            }
                            currentProc = nextProc
                        }
                    }
                    "cpu implementer" -> {
                        currentImpl = value.removePrefix("0x").removePrefix("0X").toIntOrNull(16)
                    }
                    "cpu part" -> {
                        currentPart = value.removePrefix("0x").removePrefix("0X").toIntOrNull(16)
                    }
                    "model name" -> {
                        if (currentModel == null) currentModel = value
                    }
                }
            }
            flushBlock()
        }

        // 3) Parse cpufreq policies (related_cpus)
        val policies = mutableListOf<List<Int>>()
        if (!policiesText.isNullOrBlank()) {
            for (line in policiesText.lines()) {
                val trimmed = line.trim()
                if (trimmed.isEmpty() || !trimmed.contains(":")) continue
                val cpusStr = trimmed.substringAfter(":").trim()
                val cpus = cpusStr.split(Regex("\\s+")).mapNotNull { it.toIntOrNull() }.distinct().sorted()
                if (cpus.isNotEmpty()) {
                    policies.add(cpus)
                    allCpuIds.addAll(cpus)
                }
            }
        }
        val sortedPolicies = policies.sortedBy { it.firstOrNull() ?: Int.MAX_VALUE }

        // Propagate arch name to offline siblings within the same cpufreq policy
        for (policyCpus in sortedPolicies) {
            val knownArch = policyCpus.firstNotNullOfOrNull { coreArch[it] }
            if (knownArch != null) {
                for (cpu in policyCpus) {
                    if (!coreArch.containsKey(cpu)) {
                        coreArch[cpu] = knownArch
                    }
                }
            }
        }

        // 4) Fallback to /proc/device-tree/cpus/cpu@* if count matches and some cores remain unresolved
        if (!dtCpusText.isNullOrBlank() && allCpuIds.isNotEmpty() && coreArch.size < allCpuIds.size) {
            val dtEntries = dtCpusText.lines()
                .mapNotNull { line ->
                    val trimmed = line.trim()
                    if (!trimmed.contains("compat=")) return@mapNotNull null
                    val node = trimmed.substringBefore(" compat=").trim()
                    val compat = trimmed.substringAfter("compat=").trim()
                    val unitAddr = node.substringAfter("@", "0").toIntOrNull(16) ?: Int.MAX_VALUE
                    unitAddr to resolveCpuCompatName(compat)
                }
                .sortedBy { it.first }
                .map { it.second }

            val orderedCpuIds = allCpuIds.toList()
            if (dtEntries.size == orderedCpuIds.size) {
                for (i in orderedCpuIds.indices) {
                    val cpuId = orderedCpuIds[i]
                    val name = dtEntries[i]
                    if (!coreArch.containsKey(cpuId) && !name.isNullOrBlank()) {
                        coreArch[cpuId] = name
                    }
                }
            }
        }

        // 5) Build footer grouped by policy (cluster) when policies exist, or across all CPUs
        val parts = mutableListOf<String>()
        if (sortedPolicies.isNotEmpty()) {
            val seenCpus = mutableSetOf<Int>()
            for (policyCpus in sortedPolicies) {
                seenCpus.addAll(policyCpus)
                val counts = linkedMapOf<String, Int>()
                var unresolvedCount = 0
                for (cpu in policyCpus) {
                    val arch = coreArch[cpu]
                    if (arch != null) {
                        counts[arch] = (counts[arch] ?: 0) + 1
                    } else {
                        unresolvedCount++
                    }
                }
                for ((arch, count) in counts) {
                    parts.add("${count}× $arch")
                }
                if (unresolvedCount > 0) {
                    parts.add("${unresolvedCount}× CPU cores")
                }
            }
            val remaining = allCpuIds.filter { !seenCpus.contains(it) }
            if (remaining.isNotEmpty()) {
                val counts = linkedMapOf<String, Int>()
                var unresolvedCount = 0
                for (cpu in remaining) {
                    val arch = coreArch[cpu]
                    if (arch != null) {
                        counts[arch] = (counts[arch] ?: 0) + 1
                    } else {
                        unresolvedCount++
                    }
                }
                for ((arch, count) in counts) {
                    parts.add("${count}× $arch")
                }
                if (unresolvedCount > 0) {
                    parts.add("${unresolvedCount}× CPU cores")
                }
            }
        } else if (allCpuIds.isNotEmpty()) {
            val counts = linkedMapOf<String, Int>()
            var unresolvedCount = 0
            for (cpu in allCpuIds) {
                val arch = coreArch[cpu]
                if (arch != null) {
                    counts[arch] = (counts[arch] ?: 0) + 1
                } else {
                    unresolvedCount++
                }
            }
            for ((arch, count) in counts) {
                parts.add("${count}× $arch")
            }
            if (unresolvedCount > 0) {
                parts.add("${unresolvedCount}× CPU cores")
            }
        }

        return parts.joinToString(" · ")
    }
}
