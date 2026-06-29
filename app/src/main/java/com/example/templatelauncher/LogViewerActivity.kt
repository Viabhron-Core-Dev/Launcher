package com.example.templatelauncher

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import java.io.File

class LogViewerActivity : ComponentActivity() {

    private lateinit var btnRunningLog: Button
    private lateinit var btnCrashLog: Button
    private lateinit var txtLogContent: TextView
    private lateinit var btnShare: Button
    private lateinit var btnClear: Button

    private var currentLogFile: File? = null
    private var isViewingRunningLog = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_viewer)

        // Log lifecycle event
        (application as? LauncherApplication)?.logLifecycleEvent("LogViewerActivity OnCreate")

        // Bind Views
        btnRunningLog = findViewById(R.id.btn_running_log)
        btnCrashLog = findViewById(R.id.btn_crash_log)
        txtLogContent = findViewById(R.id.txt_log_content)
        btnShare = findViewById(R.id.btn_share)
        btnClear = findViewById(R.id.btn_clear)

        // Set Click Listeners
        btnRunningLog.setOnClickListener {
            switchLog(viewRunning = true)
        }

        btnCrashLog.setOnClickListener {
            switchLog(viewRunning = false)
        }

        btnShare.setOnClickListener {
            shareCurrentLog()
        }

        btnClear.setOnClickListener {
            clearCurrentLog()
        }

        // Default to running log
        switchLog(viewRunning = true)
    }

    override fun onStart() {
        super.onStart()
        (application as? LauncherApplication)?.logLifecycleEvent("LogViewerActivity OnStart")
    }

    override fun onResume() {
        super.onResume()
        (application as? LauncherApplication)?.logLifecycleEvent("LogViewerActivity OnResume")
        // Refresh currently viewed log
        loadLogFileContent()
    }

    override fun onPause() {
        super.onPause()
        (application as? LauncherApplication)?.logLifecycleEvent("LogViewerActivity OnPause")
    }

    override fun onStop() {
        super.onStop()
        (application as? LauncherApplication)?.logLifecycleEvent("LogViewerActivity OnStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as? LauncherApplication)?.logLifecycleEvent("LogViewerActivity OnDestroy")
    }

    private fun switchLog(viewRunning: Boolean) {
        isViewingRunningLog = viewRunning
        val dir = LauncherApplication.getLogDirectory(this)

        if (viewRunning) {
            currentLogFile = File(dir, "launcher_log.txt")
            btnRunningLog.setBackgroundColor(0xFF0A84FF.toInt())
            btnRunningLog.setTextColor(0xFFFFFFFF.toInt())
            btnCrashLog.setBackgroundColor(0xFF2C2C2E.toInt())
            btnCrashLog.setTextColor(0xFFE5E5EA.toInt())
        } else {
            currentLogFile = File(dir, "launcher_crash_latest.txt")
            btnCrashLog.setBackgroundColor(0xFF0A84FF.toInt())
            btnCrashLog.setTextColor(0xFFFFFFFF.toInt())
            btnRunningLog.setBackgroundColor(0xFF2C2C2E.toInt())
            btnRunningLog.setTextColor(0xFFE5E5EA.toInt())
        }

        loadLogFileContent()
    }

    private fun loadLogFileContent() {
        val file = currentLogFile
        if (file == null || !file.exists() || file.length() == 0L) {
            val type = if (isViewingRunningLog) "Running Log" else "Crash Log"
            txtLogContent.text = "$type is empty or does not exist."
            return
        }

        try {
            val content = file.readText()
            txtLogContent.text = content
        } catch (e: Exception) {
            txtLogContent.text = "Error reading log: ${e.localizedMessage}"
        }
    }

    private fun clearCurrentLog() {
        val file = currentLogFile
        if (file != null && file.exists()) {
            if (file.delete()) {
                Toast.makeText(this, "Log cleared successfully", Toast.LENGTH_SHORT).show()
            } else {
                // If deletion fails, try writing empty string
                try {
                    file.writeText("")
                    Toast.makeText(this, "Log cleared successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed to clear log: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Log file is already empty", Toast.LENGTH_SHORT).show()
        }
        loadLogFileContent()
    }

    private fun shareCurrentLog() {
        val file = currentLogFile
        if (file == null || !file.exists() || file.length() == 0L) {
            Toast.makeText(this, "Nothing to share", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri = FileProvider.getUriForFile(
                this,
                "com.example.templatelauncher.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Share Log File"))
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to share log: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
