package com.rockproject.mt6789

import org.junit.Assert.assertEquals
import org.junit.Test

class GpuFallbackTest {

    fun determineGpuParam(khzExists: Boolean, freqExists: Boolean): String {
        return when {
            khzExists -> "gpu_target_khz"
            freqExists -> "gpu_target_freq"
            else -> "gpu_target_khz"
        }
    }

    @Test
    fun `test gpu target khz takes priority when present`() {
        val selected = determineGpuParam(khzExists = true, freqExists = true)
        assertEquals("gpu_target_khz", selected)
    }

    @Test
    fun `test gpu target freq fallback when khz is missing`() {
        val selected = determineGpuParam(khzExists = false, freqExists = true)
        assertEquals("gpu_target_freq", selected)
    }

    @Test
    fun `test default to gpu target khz when neither exists`() {
        val selected = determineGpuParam(khzExists = false, freqExists = false)
        assertEquals("gpu_target_khz", selected)
    }
}
