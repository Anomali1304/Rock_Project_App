package com.rockproject.mt6789.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rockproject.mt6789.data.BaselineRepository
import com.rockproject.mt6789.data.BootApplyManager
import com.rockproject.mt6789.data.HardwareRepository
import com.rockproject.mt6789.data.KernelConstants
import com.rockproject.mt6789.model.*
import com.rockproject.mt6789.root.ShellManager
import com.rockproject.mt6789.util.ConversionUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _darkTheme = MutableStateFlow(true)
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    private val _hasRoot = MutableStateFlow(true)
    val hasRoot: StateFlow<Boolean> = _hasRoot.asStateFlow()

    private val _moduleState = MutableStateFlow(ModuleState())
    val moduleState: StateFlow<ModuleState> = _moduleState.asStateFlow()

    private val _stockFloors = MutableStateFlow(StockFloors())
    val stockFloors: StateFlow<StockFloors> = _stockFloors.asStateFlow()

    private val _liveStats = MutableStateFlow(LiveHardwareStats())
    val liveStats: StateFlow<LiveHardwareStats> = _liveStats.asStateFlow()

    private val _cpuBrandInfo = MutableStateFlow(HardwareBrandInfo(cpuBrandName = "CPU", cpuBrandModel = "—"))
    val cpuBrandInfo: StateFlow<HardwareBrandInfo> = _cpuBrandInfo.asStateFlow()

    private val _gpuBrandInfo = MutableStateFlow(HardwareBrandInfo(gpuBrandName = "GPU", gpuBrandArch = "—"))
    val gpuBrandInfo: StateFlow<HardwareBrandInfo> = _gpuBrandInfo.asStateFlow()

    // Slider states (initialized from runtime stock floors / sysfs targets)
    private val _cpuLLSlider = MutableStateFlow(0)
    val cpuLLSlider: StateFlow<Int> = _cpuLLSlider.asStateFlow()

    private val _cpuBSlider = MutableStateFlow(0)
    val cpuBSlider: StateFlow<Int> = _cpuBSlider.asStateFlow()

    private val _cpuPrimeSlider = MutableStateFlow(0)
    val cpuPrimeSlider: StateFlow<Int> = _cpuPrimeSlider.asStateFlow()

    private val _gpuFreqSlider = MutableStateFlow(0)
    val gpuFreqSlider: StateFlow<Int> = _gpuFreqSlider.asStateFlow()

    private val _gpuVoltSlider = MutableStateFlow(0)
    val gpuVoltSlider: StateFlow<Int> = _gpuVoltSlider.asStateFlow()

    // Control states
    private val _cpuLittleControl = MutableStateFlow(CpuControlCluster())
    val cpuLittleControl: StateFlow<CpuControlCluster> = _cpuLittleControl.asStateFlow()

    private val _cpuBigControl = MutableStateFlow(CpuControlCluster())
    val cpuBigControl: StateFlow<CpuControlCluster> = _cpuBigControl.asStateFlow()

    private val _cpuPrimeControl = MutableStateFlow(CpuControlCluster())
    val cpuPrimeControl: StateFlow<CpuControlCluster> = _cpuPrimeControl.asStateFlow()

    private val _gpuControl = MutableStateFlow(GpuControlState())
    val gpuControl: StateFlow<GpuControlState> = _gpuControl.asStateFlow()

    // Boot Apply
    private val _bootApplyEnabled = MutableStateFlow(false)
    val bootApplyEnabled: StateFlow<Boolean> = _bootApplyEnabled.asStateFlow()

    private val _bootResult = MutableStateFlow(BootResult())
    val bootResult: StateFlow<BootResult> = _bootResult.asStateFlow()

    private val _showMiuiCard = MutableStateFlow(false)
    val showMiuiCard: StateFlow<Boolean> = _showMiuiCard.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _isCpuDirty = MutableStateFlow(false)
    val isCpuDirty: StateFlow<Boolean> = _isCpuDirty.asStateFlow()

    private val _isGpuDirty = MutableStateFlow(false)
    val isGpuDirty: StateFlow<Boolean> = _isGpuDirty.asStateFlow()

    val isCpuRestoreVisible: StateFlow<Boolean> = combine(_isCpuDirty, _cpuLLSlider, _cpuBSlider, _cpuPrimeSlider, _stockFloors) { dirty, ll, b, prime, floors ->
        val primeExceeds = floors.isPrimeActive && floors.cpuPrimeMin != null && prime > floors.cpuPrimeMin
        dirty || ll > (floors.cpuLLMin ?: 0) || b > (floors.cpuBMin ?: 0) || primeExceeds
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isGpuRestoreVisible: StateFlow<Boolean> = combine(_isGpuDirty, _gpuFreqSlider, _gpuVoltSlider, _stockFloors) { dirty, freq, volt, floors ->
        dirty || freq > (floors.gpuFreqMin ?: 0) || volt > (floors.gpuVoltMin ?: 0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Polling Jobs
    private var statusPollJob: Job? = null
    private var liveStatsJob: Job? = null
    private var bootRegenJob: Job? = null
    private var isDraggingSlider = false

    private var gpuOppMap: Map<Int, GpuOppEntry> = emptyMap()

    init {
        initialize()
    }

    private fun initialize() {
        viewModelScope.launch {
            val rootOk = ShellManager.isRootAvailable()
            _hasRoot.value = rootOk
            if (!rootOk) {
                _moduleState.value = ModuleState(
                    isLoaded = false,
                    statusText = "no root access",
                    dotStatus = DotStatus.BAD
                )
                showToast("No root access found")
                return@launch
            }

            // Detect hardware brand and paths
            HardwareRepository.resolveHardwarePaths()
            val cpuInfo = HardwareRepository.detectCpuInfo()
            _cpuBrandInfo.value = cpuInfo
            val gpuInfo = HardwareRepository.detectGpuInfo()
            _gpuBrandInfo.value = gpuInfo

            val loaded = HardwareRepository.isModuleLoaded()
            if (!loaded) {
                _moduleState.value = ModuleState(
                    isLoaded = false,
                    statusText = "overclock_mt6789 not loaded",
                    dotStatus = DotStatus.BAD
                )
                return@launch
            }

            // Attempt stock baseline capture if not overclocked
            BaselineRepository.captureBaselineIfStock(
                policy0Path = HardwareRepository.cpuPolicy0,
                policy6Path = HardwareRepository.cpuPolicy6,
                policyPrimePath = HardwareRepository.cpuPolicyPrime,
                gpuFreqParamName = HardwareRepository.gpuFreqParamName
            )

            // Derive stock floors
            val floors = BaselineRepository.deriveStockFloors(
                policy0Path = HardwareRepository.cpuPolicy0,
                policy6Path = HardwareRepository.cpuPolicy6,
                policyPrimePath = HardwareRepository.cpuPolicyPrime,
                primeStatus = HardwareRepository.cpuPrimeStatus
            )
            _stockFloors.value = floors

            // Set initial slider values from kernel or floors
            val cLL = ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/${HardwareRepository.cpuLittleTargetParam} 2>/dev/null").stdout.toLongOrNull()
                ?: ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/cpu_ll_target_khz 2>/dev/null").stdout.toLongOrNull() ?: 0L
            val cB = ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/${HardwareRepository.cpuBigTargetParam} 2>/dev/null").stdout.toLongOrNull()
                ?: ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/cpu_b_target_khz 2>/dev/null").stdout.toLongOrNull() ?: 0L
            val cPrime = ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/${HardwareRepository.cpuPrimeTargetParam} 2>/dev/null").stdout.toLongOrNull() ?: 0L
            val cResult = ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/cpu_oc_result 2>/dev/null").stdout
            val parsedCpu = ConversionUtils.parseCpuResult(cResult)

            if (floors.cpuLLMin != null) {
                val initLL = if (parsedCpu != null) parsedCpu.llMhz.toInt() else if (cLL > 0) (cLL / 1000).toInt() else floors.cpuLLMin
                _cpuLLSlider.value = initLL.coerceIn(floors.cpuLLMin, KernelConstants.CPU_LL_MAX_MHZ)
            }
            if (floors.cpuBMin != null) {
                val initB = if (parsedCpu != null) parsedCpu.bMhz.toInt() else if (cB > 0) (cB / 1000).toInt() else floors.cpuBMin
                _cpuBSlider.value = initB.coerceIn(floors.cpuBMin, KernelConstants.CPU_B_MAX_MHZ)
            }
            if (floors.isPrimeActive && floors.cpuPrimeMin != null) {
                val initPrime = if (parsedCpu?.primeMhz != null) parsedCpu.primeMhz.toInt() else if (cPrime > 0) (cPrime / 1000).toInt() else floors.cpuPrimeMin
                _cpuPrimeSlider.value = initPrime.coerceIn(floors.cpuPrimeMin, KernelConstants.CPU_PRIME_MAX_MHZ)
            }

            val gFreq = ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/${HardwareRepository.gpuFreqParamName} 2>/dev/null").stdout.toLongOrNull() ?: 0L
            val gVolt = ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/gpu_target_volt 2>/dev/null").stdout.toLongOrNull() ?: 0L
            val gResult = ShellManager.exec("cat ${KernelConstants.PARAMS_DIR}/gpu_oc_result 2>/dev/null").stdout
            val parsedGpu = ConversionUtils.parseGpuResult(gResult)

            if (floors.gpuFreqMin != null) {
                val initGFreq = if (parsedGpu != null && parsedGpu.enabled && parsedGpu.freqMhz != null) {
                    parsedGpu.freqMhz.toInt()
                } else if (gFreq > 0) (gFreq / 1000).toInt() else floors.gpuFreqMin
                _gpuFreqSlider.value = initGFreq.coerceIn(floors.gpuFreqMin, KernelConstants.GPU_FREQ_MAX_MHZ)
            }
            if (floors.gpuVoltMin != null) {
                val initGVolt = if (parsedGpu != null && parsedGpu.enabled && parsedGpu.voltMv != null) {
                    parsedGpu.voltMv.toInt()
                } else if (gVolt > 0) (gVolt / 100).toInt() else floors.gpuVoltMin
                _gpuVoltSlider.value = initGVolt.coerceIn(floors.gpuVoltMin, KernelConstants.GPU_VOLT_MAX_MV)
            }

            // Check if boot apply is active
            val bootScriptPresent = BootApplyManager.isBootApplyFilePresent()
            _bootApplyEnabled.value = bootScriptPresent
            _bootResult.value = BootApplyManager.readBootResult()

            // OPP table and controls
            gpuOppMap = HardwareRepository.readGpuOppTable()
            refreshControls()

            // Start polling
            startPolling()
        }
    }

    fun startPolling() {
        stopPolling()
        statusPollJob = viewModelScope.launch {
            while (isActive) {
                if (!isDraggingSlider && _hasRoot.value) {
                    val modState = HardwareRepository.refreshModuleState(_stockFloors.value)
                    _moduleState.value = modState
                }
                delay(1000)
            }
        }
        liveStatsJob = viewModelScope.launch {
            while (isActive) {
                if (!isDraggingSlider && _hasRoot.value) {
                    val stats = HardwareRepository.refreshLiveHardware(
                        gpuOppMap = gpuOppMap,
                        cpuActive = _moduleState.value.isCpuActive,
                        gpuActive = _moduleState.value.isGpuActive,
                        isPrimeActive = _stockFloors.value.isPrimeActive
                    )
                    _liveStats.value = stats
                }
                delay(2500)
            }
        }
    }

    fun stopPolling() {
        statusPollJob?.cancel()
        statusPollJob = null
        liveStatsJob?.cancel()
        liveStatsJob = null
    }

    fun toggleTheme() {
        _darkTheme.value = !_darkTheme.value
    }

    fun onSliderDragStart() {
        isDraggingSlider = true
    }

    fun onSliderDragEnd() {
        isDraggingSlider = false
    }

    fun onCpuLLChanged(value: Int) {
        val min = _stockFloors.value.cpuLLMin ?: 0
        _cpuLLSlider.value = value.coerceIn(min, KernelConstants.CPU_LL_MAX_MHZ)
        _isCpuDirty.value = true
    }

    fun onCpuBChanged(value: Int) {
        val min = _stockFloors.value.cpuBMin ?: 0
        _cpuBSlider.value = value.coerceIn(min, KernelConstants.CPU_B_MAX_MHZ)
        _isCpuDirty.value = true
    }

    fun onCpuPrimeChanged(value: Int) {
        val min = _stockFloors.value.cpuPrimeMin ?: 0
        _cpuPrimeSlider.value = value.coerceIn(min, KernelConstants.CPU_PRIME_MAX_MHZ)
        _isCpuDirty.value = true
    }

    fun onGpuFreqChanged(value: Int) {
        val min = _stockFloors.value.gpuFreqMin ?: 0
        _gpuFreqSlider.value = value.coerceIn(min, KernelConstants.GPU_FREQ_MAX_MHZ)
        _isGpuDirty.value = true
    }

    fun onGpuVoltChanged(value: Int) {
        val min = _stockFloors.value.gpuVoltMin ?: 0
        _gpuVoltSlider.value = value.coerceIn(min, KernelConstants.GPU_VOLT_MAX_MV)
        _isGpuDirty.value = true
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun applyCpu() {
        if (_isBusy.value) return
        _isBusy.value = true
        viewModelScope.launch {
            try {
                val floors = _stockFloors.value
                val (ok, msg) = HardwareRepository.applyCpu(
                    llMhz = _cpuLLSlider.value,
                    bMhz = _cpuBSlider.value,
                    primeMhz = if (floors.isPrimeActive) _cpuPrimeSlider.value else null,
                    stockFloors = floors
                )
                showToast(msg)
                val state = HardwareRepository.refreshModuleState(_stockFloors.value)
                _moduleState.value = state
                _isCpuDirty.value = false
                scheduleBootApplyRegen()
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun restoreCpu() {
        if (_isBusy.value) return
        _isBusy.value = true
        viewModelScope.launch {
            try {
                val floors = _stockFloors.value
                if (floors.cpuLLMin == null || floors.cpuBMin == null || (floors.isPrimeActive && floors.cpuPrimeMin == null)) {
                    showToast("CPU stock baseline unknown, refusing to restore blind")
                    return@launch
                }
                _cpuLLSlider.value = floors.cpuLLMin
                _cpuBSlider.value = floors.cpuBMin
                if (floors.isPrimeActive && floors.cpuPrimeMin != null) {
                    _cpuPrimeSlider.value = floors.cpuPrimeMin
                }

                val (_, msg) = HardwareRepository.restoreCpu(floors)
                showToast(msg)
                val state = HardwareRepository.refreshModuleState(_stockFloors.value)
                _moduleState.value = state
                _isCpuDirty.value = false
                scheduleBootApplyRegen()
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun applyGpu() {
        if (_isBusy.value) return
        _isBusy.value = true
        viewModelScope.launch {
            try {
                val (ok, msg) = HardwareRepository.applyGpu(
                    freqMhz = _gpuFreqSlider.value,
                    voltMv = _gpuVoltSlider.value,
                    stockFloors = _stockFloors.value
                )
                showToast(msg)
                val state = HardwareRepository.refreshModuleState(_stockFloors.value)
                _moduleState.value = state
                _isGpuDirty.value = false
                scheduleBootApplyRegen()
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun restoreGpu() {
        if (_isBusy.value) return
        _isBusy.value = true
        viewModelScope.launch {
            try {
                val floors = _stockFloors.value
                if (floors.gpuFreqMin == null || floors.gpuVoltMin == null) {
                    showToast("GPU stock baseline unknown, refusing to restore blind")
                    return@launch
                }
                _gpuFreqSlider.value = floors.gpuFreqMin
                _gpuVoltSlider.value = floors.gpuVoltMin

                val (_, msg) = HardwareRepository.restoreGpu(floors)
                showToast(msg)
                val state = HardwareRepository.refreshModuleState(_stockFloors.value)
                _moduleState.value = state
                _isGpuDirty.value = false
                scheduleBootApplyRegen()
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun setBootApplyEnabled(enabled: Boolean) {
        if (enabled && !_stockFloors.value.isFullyKnown) {
            showToast("Stock baseline unknown, cannot enable Apply on boot")
            return
        }

        _bootApplyEnabled.value = enabled
        if (enabled) {
            _showMiuiCard.value = true
            scheduleBootApplyRegen()
        } else {
            viewModelScope.launch {
                BootApplyManager.disableBootApply()
                _bootResult.value = BootApplyManager.readBootResult()
            }
        }
    }

    fun dismissMiuiCard() {
        _showMiuiCard.value = false
    }

    fun testBootApplyNow() {
        if (_isBusy.value) return
        _isBusy.value = true
        viewModelScope.launch {
            try {
                showToast("Testing boot script...")
                ShellManager.exec("sh ${KernelConstants.BOOT_SCRIPT_PATH}")
                val res = BootApplyManager.readBootResult()
                _bootResult.value = res
                showToast("Boot apply result: ${res.status}")
                val state = HardwareRepository.refreshModuleState(_stockFloors.value)
                _moduleState.value = state
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            } finally {
                _isBusy.value = false
            }
        }
    }

    private fun scheduleBootApplyRegen() {
        if (!_bootApplyEnabled.value) return
        bootRegenJob?.cancel()
        bootRegenJob = viewModelScope.launch {
            delay(500)
            regenerateBootApplyScript()
        }
    }

    suspend fun regenerateBootApplyScript() {
        if (!_bootApplyEnabled.value) return

        val floors = _stockFloors.value
        val isCpuOc = _moduleState.value.isCpuActive
        val isGpuOc = _moduleState.value.isGpuActive

        val cpuLLKhz = ConversionUtils.mhzToKhz(_cpuLLSlider.value)
        val cpuBKhz = ConversionUtils.mhzToKhz(_cpuBSlider.value)
        val gpuFreqKhz = ConversionUtils.mhzToKhz(_gpuFreqSlider.value)
        val gpuVoltRaw = ConversionUtils.mvToRaw(_gpuVoltSlider.value)

        val controlCmds = mutableListOf<String>()
        val p0 = HardwareRepository.cpuPolicy0
        val p6 = HardwareRepository.cpuPolicy6

        val littleGov = _cpuLittleControl.value.currentGovernor.trim()
        if (!p0.isNullOrBlank() && littleGov.isNotBlank() && littleGov != "—") {
            controlCmds.add("""
f="$p0/scaling_governor"
if [ -f "${'$'}f" ]; then
  chmod 0644 "${'$'}f"
  echo '$littleGov' > "${'$'}f" 2>>"${'$'}LOG"
  chmod 0444 "${'$'}f"
  cur=${'$'}(cat "${'$'}f" 2>/dev/null)
  if [ "${'$'}cur" = '$littleGov' ]; then
    log "policy Little ($p0) gov set to $littleGov"
  else
    log "ERROR: policy Little ($p0) gov failed: expected '$littleGov', got '${'$'}cur'"
    cpu_ok=0
  fi
fi
            """.trimIndent())
        }

        val bigGov = _cpuBigControl.value.currentGovernor.trim()
        if (!p6.isNullOrBlank() && bigGov.isNotBlank() && bigGov != "—") {
            controlCmds.add("""
f="$p6/scaling_governor"
if [ -f "${'$'}f" ]; then
  chmod 0644 "${'$'}f"
  echo '$bigGov' > "${'$'}f" 2>>"${'$'}LOG"
  chmod 0444 "${'$'}f"
  cur=${'$'}(cat "${'$'}f" 2>/dev/null)
  if [ "${'$'}cur" = '$bigGov' ]; then
    log "policy Big ($p6) gov set to $bigGov"
  else
    log "ERROR: policy Big ($p6) gov failed: expected '$bigGov', got '${'$'}cur'"
    cpu_ok=0
  fi
fi
            """.trimIndent())
        }

        val cpuPrimeKhz = if (floors.isPrimeActive && floors.cpuPrimeMin != null) {
            ConversionUtils.mhzToKhz(_cpuPrimeSlider.value)
        } else 0L

        val pPrime = HardwareRepository.cpuPolicyPrime
        val primeGov = _cpuPrimeControl.value.currentGovernor.trim()
        if (floors.isPrimeActive && !pPrime.isNullOrBlank() && primeGov.isNotBlank() && primeGov != "—") {
            controlCmds.add("""
f="$pPrime/scaling_governor"
if [ -f "${'$'}f" ]; then
  chmod 0644 "${'$'}f"
  echo '$primeGov' > "${'$'}f" 2>>"${'$'}LOG"
  chmod 0444 "${'$'}f"
  cur=${'$'}(cat "${'$'}f" 2>/dev/null)
  if [ "${'$'}cur" = '$primeGov' ]; then
    log "policy Prime ($pPrime) gov set to $primeGov"
  else
    log "ERROR: policy Prime ($pPrime) gov failed: expected '$primeGov', got '${'$'}cur'"
    cpu_ok=0
  fi
fi
            """.trimIndent())
        }

        BootApplyManager.saveBootApplyScript(
            context = getApplication(),
            isCpuOc = isCpuOc,
            cpuLLKhz = cpuLLKhz,
            cpuBKhz = cpuBKhz,
            cpuPrimeKhz = cpuPrimeKhz,
            isGpuOc = isGpuOc,
            gpuFreqParam = HardwareRepository.gpuFreqParamName,
            gpuFreqKhz = gpuFreqKhz,
            gpuVoltRaw = gpuVoltRaw,
            controlCommands = controlCmds
        )
    }

    // Direct Control operations
    fun refreshControls() {
        viewModelScope.launch {
            val p0 = HardwareRepository.cpuPolicy0
            val p6 = HardwareRepository.cpuPolicy6
            val pPrime = HardwareRepository.cpuPolicyPrime
            val little = HardwareRepository.readCpuClusterControl(p0)
            val big = HardwareRepository.readCpuClusterControl(p6)
            val prime = if (_stockFloors.value.isPrimeActive && !pPrime.isNullOrBlank()) {
                HardwareRepository.readCpuClusterControl(pPrime)
            } else {
                CpuControlCluster()
            }
            val gpu = HardwareRepository.readGpuControlState()

            _cpuLittleControl.value = little
            _cpuBigControl.value = big
            _cpuPrimeControl.value = prime
            _gpuControl.value = gpu
        }
    }

    fun setCpuPrimeGov(gov: String) {
        val pPrime = HardwareRepository.cpuPolicyPrime ?: return
        viewModelScope.launch {
            val (ok, msg) = HardwareRepository.setCpuGovernor(pPrime, "Prime", gov)
            showToast(msg)
            if (ok) {
                refreshControls()
                scheduleBootApplyRegen()
            }
        }
    }

    fun setCpuPrimeMin(targetKhz: Long) {
        val pPrime = HardwareRepository.cpuPolicyPrime ?: return
        viewModelScope.launch {
            val (ok, msg) = HardwareRepository.setCpuPolicyMin(pPrime, "Prime", targetKhz)
            showToast(msg)
            refreshControls()
            if (ok) scheduleBootApplyRegen()
        }
    }

    fun setCpuPrimeMax(targetKhz: Long) {
        val pPrime = HardwareRepository.cpuPolicyPrime ?: return
        viewModelScope.launch {
            val (ok, msg) = HardwareRepository.setCpuPolicyMax(pPrime, "Prime", targetKhz)
            showToast(msg)
            refreshControls()
            if (ok) scheduleBootApplyRegen()
        }
    }

    fun setCpuLittleGov(gov: String) {
        val p0 = HardwareRepository.cpuPolicy0 ?: return
        viewModelScope.launch {
            val (ok, msg) = HardwareRepository.setCpuGovernor(p0, "Little", gov)
            showToast(msg)
            if (ok) {
                refreshControls()
                scheduleBootApplyRegen()
            }
        }
    }

    fun setCpuLittleMin(targetKhz: Long) {
        val p0 = HardwareRepository.cpuPolicy0 ?: return
        viewModelScope.launch {
            val (ok, msg) = HardwareRepository.setCpuPolicyMin(p0, "Little", targetKhz)
            showToast(msg)
            refreshControls()
            if (ok) scheduleBootApplyRegen()
        }
    }

    fun setCpuLittleMax(targetKhz: Long) {
        val p0 = HardwareRepository.cpuPolicy0 ?: return
        viewModelScope.launch {
            val (ok, msg) = HardwareRepository.setCpuPolicyMax(p0, "Little", targetKhz)
            showToast(msg)
            refreshControls()
            if (ok) scheduleBootApplyRegen()
        }
    }

    fun setCpuBigGov(gov: String) {
        val p6 = HardwareRepository.cpuPolicy6 ?: return
        viewModelScope.launch {
            val (ok, msg) = HardwareRepository.setCpuGovernor(p6, "Big", gov)
            showToast(msg)
            if (ok) {
                refreshControls()
                scheduleBootApplyRegen()
            }
        }
    }

    fun setCpuBigMin(targetKhz: Long) {
        val p6 = HardwareRepository.cpuPolicy6 ?: return
        viewModelScope.launch {
            val (ok, msg) = HardwareRepository.setCpuPolicyMin(p6, "Big", targetKhz)
            showToast(msg)
            refreshControls()
            if (ok) scheduleBootApplyRegen()
        }
    }

    fun setCpuBigMax(targetKhz: Long) {
        val p6 = HardwareRepository.cpuPolicy6 ?: return
        viewModelScope.launch {
            val (ok, msg) = HardwareRepository.setCpuPolicyMax(p6, "Big", targetKhz)
            showToast(msg)
            refreshControls()
            if (ok) scheduleBootApplyRegen()
        }
    }

    fun setGpuGov(gov: String) {
        viewModelScope.launch {
            val (ok, msg) = HardwareRepository.setGpuGovernor(gov)
            showToast(msg)
            if (ok) {
                refreshControls()
                scheduleBootApplyRegen()
            }
        }
    }

    fun setGpuMin(oppIdx: Int) {
        viewModelScope.launch {
            val (ok, msg) = HardwareRepository.setGpuMinFreq(oppIdx)
            showToast(msg)
            if (ok) {
                refreshControls()
                scheduleBootApplyRegen()
            }
        }
    }

    fun setGpuMax(oppIdx: Int) {
        viewModelScope.launch {
            val (ok, msg) = HardwareRepository.setGpuMaxFreq(oppIdx)
            showToast(msg)
            if (ok) {
                refreshControls()
                scheduleBootApplyRegen()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
