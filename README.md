# Rock Project • Overclock MT6789

**Rock Project • Overclock MT6789** is a root-based native Android controller for the POCO M5 / MediaTek Helio G99 (MT6789). It interfaces directly with the kernel module `overclock_mt6789.ko` via `/sys/module/overclock_mt6789/parameters/` and provides hardware telemetry, CPU & GPU overclocking, cpufreq & power policy controls, and an automated Apply on Boot mechanism.

---

## Features

- **Live Hardware Telemetry**: Real-time Little Cluster, Big Cluster, GPU frequency, and GPU voltage stats.
- **CPU Overclocking**:
  - Independent Little Cluster (2000–2600 MHz, step 10) and Big Cluster (2200–2600 MHz, step 10) targets.
  - Runtime stock-ceiling detection with safe floor constraints.
  - SVS autonomous voltage handling (`kernel enforces <= stock +60%`).
- **GPU Overclocking**:
  - GPU frequency slider (1100–1750 MHz, step 5).
  - GPU voltage slider (775–1000 mV, step 1) with linked vsram.
  - Auto-detection and fallback between `gpu_target_khz` and `gpu_target_freq`.
- **CPU & GPU Control**:
  - Per-cluster governor selection (`scaling_governor`).
  - Min/Max frequency locking with serial queue synchronization.
  - GPU power policy governor selection and OPP table indexing.
- **Robust Apply on Boot**:
  - Standalone stock baseline creation (`/data/adb/mt6789/baseline/stock.json`).
  - Generated boot script (`/data/adb/mt6789/apply_on_boot.sh`) executed on `BOOT_COMPLETED`.
  - Loop timeouts, 3 retry attempts, and result logging to `/data/adb/mt6789/boot_result.json`.
  - One-time MIUI / HyperOS Autostart and battery restriction guidance.

---

## How to Build

Build debug APK:
```bash
gradle :app:assembleDebug
```

Build optimized release APK with R8 shrinking:
```bash
gradle :app:assembleRelease
```

Run local unit tests:
```bash
gradle :app:testDebugUnitTest
```

---

## How to Test Boot-Apply Over ADB

1. **Verify Boot Result**:
   ```bash
   adb shell su -c "cat /data/adb/mt6789/boot_result.json"
   ```
   Expected output format when successful:
   ```json
   {"time":1697150000,"status":"ok","message":"applied"}
   ```

2. **Inspect Boot Logs**:
   ```bash
   adb shell su -c "tail -n 30 /data/adb/mt6789/boot.log"
   ```

3. **Manually Trigger Boot Script**:
   ```bash
   adb shell su -c "sh /data/adb/mt6789/apply_on_boot.sh"
   ```

4. **Verify Applied Parameters in Sysfs**:
   ```bash
   adb shell su -c "cat /sys/module/overclock_mt6789/parameters/cpu_oc_result"
   adb shell su -c "cat /sys/module/overclock_mt6789/parameters/gpu_oc_result"
   ```

---

## Troubleshooting

1. **Module Not Loaded**:
   - Status will indicate `overclock_mt6789 not loaded`.
   - Ensure `overclock_mt6789.ko` is placed and loaded by the kernel / vendor image at boot. The app communicates with the loaded module and does not load `.ko` files itself.

2. **MIUI / HyperOS Autostart**:
   - Xiaomi MIUI and HyperOS block `BOOT_COMPLETED` broadcasts for user-installed applications by default.
   - Go to **Security > Manage Apps > Permissions > Autostart** and enable **Rock Project**.
   - Set **Battery Saver** for the app to **No Restrictions**.

3. **Stock Baseline Unknown**:
   - If the baseline is unknown, the app disables Apply/Restore buttons for safety.
   - Click **Restore stock** to return all frequencies to vendor defaults, allowing the app to capture a clean baseline in `/data/adb/mt6789/baseline/stock.json`.
