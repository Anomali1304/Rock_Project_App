package com.rockproject.mt6789

import com.rockproject.mt6789.util.ConversionUtils
import org.junit.Assert.*
import org.junit.Test

class ConversionAndParsingTest {

    @Test
    fun `test mhz and khz conversions`() {
        assertEquals(2000000L, ConversionUtils.mhzToKhz(2000))
        assertEquals(2600000L, ConversionUtils.mhzToKhz(2600))
        assertEquals(2000.0, ConversionUtils.khzToMhz(2000000L), 0.001)
        assertEquals(2200.0, ConversionUtils.khzToMhz(2200000L), 0.001)
    }

    @Test
    fun `test mv and raw conversions`() {
        assertEquals(73500L, ConversionUtils.mvToRaw(735))
        assertEquals(100000L, ConversionUtils.mvToRaw(1000))
        assertEquals(735.0, ConversionUtils.rawToMv(73500L), 0.001)
        assertEquals(1000.0, ConversionUtils.rawToMv(100000L), 0.001)
    }

    @Test
    fun `test step rounding`() {
        assertEquals(1000, ConversionUtils.roundToStep(1002.0, 5))
        assertEquals(1005, ConversionUtils.roundToStep(1003.0, 5))
        assertEquals(2210, ConversionUtils.roundToStep(2208.0, 10))
        assertEquals(2200, ConversionUtils.roundToStep(2204.0, 10))
    }

    @Test
    fun `test result classification`() {
        assertEquals("ok", ConversionUtils.classifyResult("OK: applied targets"))
        assertEquals("ok", ConversionUtils.classifyResult("ok: success"))
        assertEquals("fail", ConversionUtils.classifyResult("FAIL: clamp rejected"))
        assertEquals("fail", ConversionUtils.classifyResult("fail: timeout"))
        assertNull(ConversionUtils.classifyResult(""))
        assertNull(ConversionUtils.classifyResult(null))
    }

    @Test
    fun `test cpu result parsing`() {
        val output = "OK LL=2000000KHz b=2200000KHz"
        val parsed = ConversionUtils.parseCpuResult(output)
        assertNotNull(parsed)
        assertEquals(2000.0, parsed!!.llMhz, 0.001)
        assertEquals(2200.0, parsed.bMhz, 0.001)

        val outputWithIdx = "OK LL_idx0=2100000KHz b_idx1=2300000KHz"
        val parsedIdx = ConversionUtils.parseCpuResult(outputWithIdx)
        assertNotNull(parsedIdx)
        assertEquals(2100.0, parsedIdx!!.llMhz, 0.001)
        assertEquals(2300.0, parsedIdx.bMhz, 0.001)

        assertNull(ConversionUtils.parseCpuResult("INVALID OUTPUT"))
    }

    @Test
    fun `test gpu result parsing`() {
        val enabledOutput = "OK freq=1100000 volt=77500"
        val parsed = ConversionUtils.parseGpuResult(enabledOutput)
        assertNotNull(parsed)
        assertTrue(parsed!!.enabled)
        assertEquals(1100.0, parsed.freqMhz!!, 0.001)
        assertEquals(775.0, parsed.voltMv!!, 0.001)

        val disabledOutput = "OK disabled"
        val parsedDisabled = ConversionUtils.parseGpuResult(disabledOutput)
        assertNotNull(parsedDisabled)
        assertFalse(parsedDisabled!!.enabled)

        val restoredOutput = "OK stock restored"
        val parsedRestored = ConversionUtils.parseGpuResult(restoredOutput)
        assertNotNull(parsedRestored)
        assertFalse(parsedRestored!!.enabled)
    }

    @Test
    fun `test gpu opp table parsing`() {
        val oppText = """
            [0] freq: 1000000, volt: 73500
            [1] freq: 800000, volt: 70000
            [2] freq: 500000, volt: 65000
        """.trimIndent()

        val oppMap = ConversionUtils.parseGpuOppTable(oppText)
        assertEquals(3, oppMap.size)
        assertEquals(1000.0, oppMap[0]?.freqMhz ?: 0.0, 0.001)
        assertEquals(735.0, oppMap[0]?.voltMv ?: 0.0, 0.001)

        val stockMax = ConversionUtils.getGpuStockMax(oppMap)
        assertNotNull(stockMax)
        assertEquals(1000.0, stockMax!!.freqMhz, 0.001)
        assertEquals(735.0, stockMax.voltMv, 0.001)
    }

    @Test
    fun `test active governor parsing`() {
        assertEquals("schedutil", ConversionUtils.parseActiveGov("[schedutil] performance powersave"))
        assertEquals("performance", ConversionUtils.parseActiveGov("schedutil (performance) powersave"))
        assertEquals("schedutil", ConversionUtils.parseActiveGov("schedutil"))
        assertNull(ConversionUtils.parseActiveGov(null))
    }

    @Test
    fun `test oc requested logic and stale fail handling`() {
        val stockLL = 2000000L
        val stockB = 2200000L

        // Stock frequency equal to ceiling is NOT OC
        assertFalse(ConversionUtils.isCpuOcRequested(2000000L, 2200000L, stockLL, stockB))

        // Target greater than stock IS OC
        assertTrue(ConversionUtils.isCpuOcRequested(2100000L, 2200000L, stockLL, stockB))
        assertTrue(ConversionUtils.isCpuOcRequested(2000000L, 2300000L, stockLL, stockB))

        // GPU oc requested logic
        val stockGpuFreq = 1000000L
        val stockGpuVolt = 73500L

        assertFalse(ConversionUtils.isGpuOcRequested(1000000L, 73500L, stockGpuFreq, stockGpuVolt, disabled = false))
        assertTrue(ConversionUtils.isGpuOcRequested(1100000L, 73500L, stockGpuFreq, stockGpuVolt, disabled = false))
        assertTrue(ConversionUtils.isGpuOcRequested(1000000L, 75000L, stockGpuFreq, stockGpuVolt, disabled = false))
        assertFalse(ConversionUtils.isGpuOcRequested(1100000L, 75000L, stockGpuFreq, stockGpuVolt, disabled = true))
    }
}
