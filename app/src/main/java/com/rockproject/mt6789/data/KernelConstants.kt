package com.rockproject.mt6789.data

object KernelConstants {
    const val MODULE_NAME = "overclock_mt6789"
    const val PARAMS_DIR = "/sys/module/overclock_mt6789/parameters"
    const val BASE_DIR = "/data/adb/mt6789"
    const val BASELINE_DIR = "/data/adb/mt6789/baseline"
    const val STOCK_BASELINE_JSON = "/data/adb/mt6789/baseline/stock.json"
    const val BOOT_SCRIPT_PATH = "/data/adb/mt6789/apply_on_boot.sh"
    const val BOOT_CONFIG_JSON = "/data/adb/mt6789/boot_config.json"
    const val BOOT_RESULT_JSON = "/data/adb/mt6789/boot_result.json"
    const val BOOT_LOG_PATH = "/data/adb/mt6789/boot.log"

    const val GPU_WORKING_OPP = "/proc/gpufreqv2/gpu_working_opp_table"
    const val GPU_UPBOUND = "/sys/kernel/ged/hal/custom_upbound_gpu_freq"
    const val GPU_BOOST = "/sys/kernel/ged/hal/custom_boost_gpu_freq"
    const val GPU_CUR_FREQ = "/sys/kernel/ged/hal/current_freqency"

    const val CPU_POLICY_DIR = "/sys/devices/system/cpu/cpufreq"

    // Absolute caps (max OC limits - explicitly permitted exceptions)
    const val CPU_OC_ABS_CAP_KHZ = 2600000L
    const val CPU_LL_MAX_MHZ = 2600
    const val CPU_B_MAX_MHZ = 2600
    const val CPU_PRIME_MAX_MHZ = 2600
    const val GPU_FREQ_MAX_MHZ = 1750
    const val GPU_VOLT_MAX_MV = 1000
}
