package com.example.templatelauncher

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import android.util.LruCache
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LauncherApplication : Application() {

    // LruCache for 60 icons
    val iconCache = object : LruCache<String, Bitmap>(60) {
        override fun sizeOf(key: String?, value: Bitmap?): Int {
            return 1 // count by item
        }
    }

    override fun onCreate() {
        super.onCreate()
        setupCrashHandler()
        logLifecycleEvent("Application OnCreate")
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                saveCrashLog(throwable)
            } catch (e: Exception) {
                Log.e("LauncherApplication", "Failed to save crash log", e)
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun saveCrashLog(throwable: Throwable) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTrace = sw.toString()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val dir = getLogDirectory(this)
        
        // Save timestamped crash file
        val crashFile = File(dir, "launcher_crash_$timestamp.txt")
        FileOutputStream(crashFile).use { out ->
            out.write(stackTrace.toByteArray())
        }

        // Save latest crash file
        val latestFile = File(dir, "launcher_crash_latest.txt")
        FileOutputStream(latestFile).use { out ->
            out.write(stackTrace.toByteArray())
        }
    }

    fun logLifecycleEvent(event: String) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
            val logLine = "[$timestamp] $event\n"
            val dir = getLogDirectory(this)
            val logFile = File(dir, "launcher_log.txt")

            // Check file size (max 1MB)
            if (logFile.exists() && logFile.length() > 1024 * 1024) {
                val content = logFile.readText()
                val halfLength = content.length / 2
                val splitIndex = content.indexOf('\n', halfLength)
                val newContent = if (splitIndex != -1) {
                    content.substring(splitIndex + 1)
                } else {
                    content.substring(halfLength)
                }
                logFile.writeText("[LOG PRUNED]\n" + newContent)
            }

            FileOutputStream(logFile, true).use { out ->
                out.write(logLine.toByteArray())
            }
        } catch (e: Exception) {
            Log.e("LauncherApplication", "Failed to write lifecycle log", e)
        }
    }

    companion object {
        fun getLogDirectory(context: Context): File {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }
    }
}
