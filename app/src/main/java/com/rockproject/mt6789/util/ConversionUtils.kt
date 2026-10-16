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
        val llPattern = Pattern.compile("(?:LL|little)(?:_idx\\d+)?=(\\d+)KHz", Pattern.CASE_INSENSITIVE)
        val bPattern = Pattern.compile("(?:b|big)(?:_idx\\d+)?=(\\d+)KHz", Pattern.CASE_INSENSITIVE)
        val primePattern = Pattern.compile("prime(?:_idx\\d+)?=(\\d+)KHz", Pattern.CASE_INSENSITIVE)

        val llMatcher = llPattern.matcher(text)
        val bMatcher = bPattern.matcher(text)
        val primeMatcher = primePattern.matcher(text)

        if (llMatcher.find() && bMatcher.find()) {
            val llKhz = llMatcher.group(1)?.toLongOrNull() ?: return null
            val bKhz = bMatcher.group(1)?.toLongOrNull() ?: return null
            val primeKhz = if (primeMatcher.find()) primeMatcher.group(1)?.toLongOrNull() else null
            return CpuParsedResult(
                llMhz = khzToMhz(llKhz),
                bMhz = khzToMhz(bKhz),
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
        effLLKhz: Long,
        effBKhz: Long,
        stockLLKhz: Long,
        stockBKhz: Long,
        effPrimeKhz: Long = 0L,
        stockPrimeKhz: Long = 0L
    ): Boolean {
        val known = stockLLKhz > 0 && stockBKhz > 0
        val primeOc = stockPrimeKhz > 0 && effPrimeKhz > stockPrimeKhz
        return known && (effLLKhz > stockLLKhz || effBKhz > stockBKhz || primeOc)
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
}
