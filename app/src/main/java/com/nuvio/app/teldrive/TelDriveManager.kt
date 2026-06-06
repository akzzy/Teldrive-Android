package com.nuvio.app.teldrive

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

object TelDriveManager {
    private const val TAG = "TelDriveManager"
    private var process: Process? = null

    private val _runningState = MutableStateFlow(false)
    val runningState: StateFlow<Boolean> = _runningState.asStateFlow()

    // Real-time log listener callback for UI console output
    var logListener: ((String) -> Unit)? = null

    /**
     * Starts the TelDrive server process
     */
    @Synchronized
    fun startServer(
        context: Context,
        supabaseDsn: String,
        jwtSecret: String,
        port: Int = 8080,
        tgAppId: String = "",
        tgAppHash: String = "",
        tgUploadsEncryptionKey: String = ""
    ): Boolean {
        if (process != null) {
            Log.i(TAG, "TelDrive is already running.")
            _runningState.value = true
            return true
        }

        val binary = File(context.applicationInfo.nativeLibraryDir, "libteldrive.so")
        if (!binary.exists()) {
            Log.e(TAG, "TelDrive binary not found in native library directory: ${binary.absolutePath}")
            return false
        }

        val sessionDbPath = File(context.filesDir, "session.db").absolutePath

        try {
            val processBuilder = ProcessBuilder(
                binary.absolutePath,
                "run"
            )

            // Setup configuration via environment variables
            val env = processBuilder.environment()
            env["TELDRIVE_DB_DATA_SOURCE"] = supabaseDsn
            env["TELDRIVE_SERVER_PORT"] = port.toString()
            env["TELDRIVE_JWT_SECRET"] = jwtSecret
            env["TELDRIVE_TG_SESSION_TYPE"] = "bolt"
            env["TELDRIVE_TG_SESSION_BOLT_PATH"] = sessionDbPath
            env["TELDRIVE_LOG_LEVEL"] = "info"

            // Telegram API Credentials configuration
            env["TELDRIVE_TG_APP_ID"] = tgAppId
            env["TELDRIVE_TG_APP_HASH"] = tgAppHash
            env["TELDRIVE_TG_UPLOADS_ENCRYPTION_KEY"] = tgUploadsEncryptionKey
            
            // Database-specific performance configurations from your config.toml
            env["TELDRIVE_DB_PREPARE_STMT"] = "false"
            env["TELDRIVE_DB_POOL_ENABLE"] = "false"

            // Redirect logs
            processBuilder.redirectErrorStream(true)

            Log.i(TAG, "Launching TelDrive server on port $port with Supabase connection...")
            process = processBuilder.start()
            _runningState.value = true

            // Stream logs to Android Logcat and UI in a background thread
            Thread {
                try {
                    process?.inputStream?.bufferedReader()?.use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            Log.d("TelDriveDaemon", line ?: "")
                            logListener?.invoke(line ?: "")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading daemon output", e)
                } finally {
                    synchronized(this) {
                        process = null
                        _runningState.value = false
                    }
                }
            }.start()

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start TelDrive process", e)
            process = null
            _runningState.value = false
            return false
        }
    }

    /**
     * Stops the TelDrive server process
     */
    @Synchronized
    fun stopServer() {
        process?.let {
            Log.i(TAG, "Stopping TelDrive server...")
            it.destroy()
            process = null
            _runningState.value = false
        }
    }

    fun isRunning(): Boolean {
        return process?.isAlive ?: false
    }
}

