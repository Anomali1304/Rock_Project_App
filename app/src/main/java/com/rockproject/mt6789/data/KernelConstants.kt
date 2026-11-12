package com.rockproject.mt6789.data

object KernelConstants {
    const val MODULE_NAME = "overclock_mt6789"
    const val PARAMS_DIR = "/sys/module/overclock_mt6789/parameters"
    const val CPU_ALLOW_3GHZ_PARAM = "cpu_oc_allow_3ghz"
    const val CPU_VOLT_MAX_DELTA_PARAM = "cpu_volt_max_delta_raw"
    const val CPU_VOLT_ABS_MAX_PARAM = "cpu_volt_abs_max_raw"
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
    const val CPU_OC_ABS_CAP_KHZ = 3000000L
    const val CPU_LITTLE_MAX_MHZ = 3000
    const val CPU_BIG_MAX_MHZ = 3000
    const val CPU_PRIME_MAX_MHZ = 3000
    const val GPU_FREQ_MAX_MHZ = 1750
    const val GPU_VOLT_MAX_MV = 1000
}
