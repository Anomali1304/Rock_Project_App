package com.rockproject.mt6789.model

enum class DotStatus {
    OK, WARN, BAD
}

data class GpuBaseline(
    val freq_khz: Long = 0,
    val volt_raw: Long = 0,
    val vsram_raw: Long = 0
)

data class CpuBaseline(
    val ll_khz: Long = 0,
    val b_khz: Long = 0,
    val prime_khz: Long = 0
)

data class StockBaseline(
    val captured_at: Long = 0,
    val gpu: GpuBaseline = GpuBaseline(),
    val cpu: CpuBaseline = CpuBaseline()
)

data class StockFloors(
    val cpuLLMin: Int? = null,
    val cpuBMin: Int? = null,
    val cpuPrimeMin: Int? = null,
    val isPrimeActive: Boolean = false,
    val gpuFreqMin: Int? = null,
    val gpuVoltMin: Int? = null
) {
    val isCpuKnown: Boolean get() = cpuLLMin != null && cpuBMin != null && (!isPrimeActive || cpuPrimeMin != null)
    val isGpuKnown: Boolean get() = gpuFreqMin != null && gpuVoltMin != null
    val isFullyKnown: Boolean get() = isCpuKnown && isGpuKnown
}

data class CpuParsedResult(
    val llMhz: Double,
    val bMhz: Double,
    val primeMhz: Double? = null
)

data class GpuParsedResult(
    val enabled: Boolean,
    val freqMhz: Double? = null,
    val voltMv: Double? = null
)

data class GpuOppEntry(
    val index: Int,
    val freqMhz: Double,
    val voltMv: Double
)

data class CpuPolicyInfo(
    val path: String,
    val firstCpu: Int,
    val relatedCpus: List<Int>
)

data class BootResult(
    val time: Long = 0,
    val status: String = "never",
    val message: String = ""
) {
    val isOk: Boolean get() = status.equals("ok", ignoreCase = true)
    val isFail: Boolean get() = status.equals("fail", ignoreCase = true)
    val isNever: Boolean get() = status.equals("never", ignoreCase = true) || time == 0L
}

data class ModuleState(
    val isLoaded: Boolean = false,
    val isCpuActive: Boolean = false,
    val isGpuActive: Boolean = false,
    val isFailed: Boolean = false,
    val statusText: String = "Checking…",
    val dotStatus: DotStatus = DotStatus.BAD
)

data class LiveHardwareStats(
    val cpuLLCurMhz: String = "—",
    val cpuBCurMhz: String = "—",
    val cpuPrimeCurMhz: String = "—",
    val isPrimeActive: Boolean = false,
    val cpuStatus: String = "Stock",
    val isCpuActive: Boolean = false,
    val gpuCurFreqMhz: String = "—",
    val gpuCurVoltMv: String = "—",
    val gpuStatus: String = "Stock",
    val isGpuActive: Boolean = false
)

data class HardwareBrandInfo(
    val cpuBrandName: String = "CPU",
    val cpuBrandModel: String = "—",
    val cpuArchFooter: String = "",
    val gpuBrandName: String = "GPU",
    val gpuBrandArch: String = "—",
    val gpuFooter: String = ""
)

data class CpuControlCluster(
    val availableGovernors: List<String> = emptyList(),
    val availableFrequenciesKhz: List<Long> = emptyList(),
    val currentGovernor: String = "—",
    val currentMinKhz: Long? = null,
    val currentMaxKhz: Long? = null
)

data class GpuControlState(
    val availableGovernors: List<String> = emptyList(),
    val oppTable: Map<Int, GpuOppEntry> = emptyMap(),
    val currentGovernor: String = "—",
    val currentMinOppIdx: Int? = null,
    val currentMaxOppIdx: Int? = null
)
