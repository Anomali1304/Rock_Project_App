package com.rockproject.mt6789.root

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

data class ShellResult(
    val code: Int,
    val stdout: String,
    val stderr: String
) {
    val isSuccess: Boolean get() = code == 0
}

object ShellManager {

    private const val DEFAULT_TIMEOUT_MS = 8000L

    // Global mutex for writes to prevent concurrency conflicts
    private val writeMutex = Mutex()

    // Dedicated queue for CPU OC target application
    private val cpuOcApplyMutex = Mutex()

    // Per-CPU-policy serial queues (e.g. policy0, policy6)
    private val policyQueues = ConcurrentHashMap<String, Mutex>()

    fun getPolicyMutex(policyPath: String): Mutex {
        return policyQueues.computeIfAbsent(policyPath) { Mutex() }
    }

    suspend fun isRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val shell = Shell.getShell()
            shell.isRoot
        } catch (_: Exception) {
            false
        }
    }

    suspend fun exec(cmd: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): ShellResult = withContext(Dispatchers.IO) {
        val result = withTimeoutOrNull(timeoutMs) {
            try {
                val shellResult = Shell.cmd(cmd).exec()
                ShellResult(
                    code = shellResult.code,
                    stdout = shellResult.out.joinToString("\n").trim(),
                    stderr = shellResult.err.joinToString("\n").trim()
                )
            } catch (e: Exception) {
                ShellResult(code = -1, stdout = "", stderr = e.message ?: "Execution error")
            }
        }
        result ?: ShellResult(code = -1, stdout = "", stderr = "Command timed out after ${timeoutMs}ms")
    }

    suspend fun <T> runSerializedWrite(block: suspend () -> T): T {
        return writeMutex.withLock {
            block()
        }
    }

    suspend fun <T> runCpuOcApply(block: suspend () -> T): T {
        return cpuOcApplyMutex.withLock {
            block()
        }
    }

    suspend fun <T> runPolicyTask(policyPath: String, block: suspend () -> T): T {
        val mutex = getPolicyMutex(policyPath)
        return mutex.withLock {
            block()
        }
    }
}
