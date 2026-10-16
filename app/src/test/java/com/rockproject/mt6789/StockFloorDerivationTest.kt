package com.rockproject.mt6789

import com.rockproject.mt6789.data.BaselineRepository
import com.rockproject.mt6789.model.StockFloors
import com.rockproject.mt6789.util.ConversionUtils
import org.junit.Assert.*
import org.junit.Test

class StockFloorDerivationTest {

    @Test
    fun `test baseline json parsing`() {
        val json = """
            {
              "captured_at": 1697150000,
              "gpu": { "freq_khz": 1000000, "volt_raw": 73500, "vsram_raw": 73500 },
              "cpu": { "ll_khz": 2000000, "b_khz": 2200000 }
            }
        """.trimIndent()

        val parsed = BaselineRepository.parseBaselineJson(json)
        assertNotNull(parsed)
        assertEquals(1697150000L, parsed!!.captured_at)
        assertEquals(1000000L, parsed.gpu.freq_khz)
        assertEquals(73500L, parsed.gpu.volt_raw)
        assertEquals(2000000L, parsed.cpu.ll_khz)
        assertEquals(2200000L, parsed.cpu.b_khz)
    }

    @Test
    fun `test stock floor derivation values`() {
        val gpuFreqKhz = 1000000L
        val gpuVoltRaw = 73500L
        val cpuLLKhz = 2000000L
        val cpuBKhz = 2200000L

        val stockGpuFreq = ConversionUtils.roundToStep(ConversionUtils.khzToMhz(gpuFreqKhz), 5)
        val stockGpuVolt = Math.round(gpuVoltRaw / 100.0).toInt()
        val stockLL = ConversionUtils.roundToStep(ConversionUtils.khzToMhz(cpuLLKhz), 10)
        val stockB = ConversionUtils.roundToStep(ConversionUtils.khzToMhz(cpuBKhz), 10)

        assertEquals(1000, stockGpuFreq)
        assertEquals(735, stockGpuVolt)
        assertEquals(2000, stockLL)
        assertEquals(2200, stockB)

        val floors = StockFloors(
            cpuLLMin = stockLL,
            cpuBMin = stockB,
            gpuFreqMin = stockGpuFreq,
            gpuVoltMin = stockGpuVolt
        )

        assertTrue(floors.isCpuKnown)
        assertTrue(floors.isGpuKnown)
        assertTrue(floors.isFullyKnown)
    }

    @Test
    fun `test baseline json parsing with prime`() {
        val json = """
            {
              "captured_at": 1697150000,
              "gpu": { "freq_khz": 1000000, "volt_raw": 73500, "vsram_raw": 73500 },
              "cpu": { "ll_khz": 2000000, "b_khz": 2200000, "prime_khz": 2400000 }
            }
        """.trimIndent()

        val parsed = BaselineRepository.parseBaselineJson(json)
        assertNotNull(parsed)
        assertEquals(2400000L, parsed!!.cpu.prime_khz)
    }

    @Test
    fun `test stock floor derivation values with prime`() {
        val floors2p = StockFloors(
            cpuLLMin = 2000,
            cpuBMin = 2200,
            cpuPrimeMin = null,
            isPrimeActive = false,
            gpuFreqMin = 1000,
            gpuVoltMin = 735
        )
        assertTrue(floors2p.isCpuKnown)
        assertTrue(floors2p.isFullyKnown)

        val floors3pMissingPrime = StockFloors(
            cpuLLMin = 2000,
            cpuBMin = 2200,
            cpuPrimeMin = null,
            isPrimeActive = true,
            gpuFreqMin = 1000,
            gpuVoltMin = 735
        )
        assertFalse(floors3pMissingPrime.isCpuKnown)
        assertFalse(floors3pMissingPrime.isFullyKnown)

        val floors3pComplete = StockFloors(
            cpuLLMin = 2000,
            cpuBMin = 2200,
            cpuPrimeMin = 2400,
            isPrimeActive = true,
            gpuFreqMin = 1000,
            gpuVoltMin = 735
        )
        assertTrue(floors3pComplete.isCpuKnown)
        assertTrue(floors3pComplete.isFullyKnown)
    }

    @Test
    fun `test unknown baseline safety check`() {
        val unknownFloors = StockFloors()
        assertFalse(unknownFloors.isCpuKnown)
        assertFalse(unknownFloors.isGpuKnown)
        assertFalse(unknownFloors.isFullyKnown)
    }
}
