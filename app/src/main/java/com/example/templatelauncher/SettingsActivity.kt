package com.example.templatelauncher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.ComponentActivity

class SettingsActivity : ComponentActivity() {

    private lateinit var lblColumns: TextView
    private lateinit var seekColumns: SeekBar
    private lateinit var lblRows: TextView
    private lateinit var seekRows: SeekBar
    private lateinit var groupIconSize: RadioGroup
    private lateinit var btnSystemLogs: Button
    private lateinit var btnDone: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        (application as? LauncherApplication)?.logLifecycleEvent("SettingsActivity OnCreate")

        lblColumns = findViewById(R.id.lbl_columns)
        seekColumns = findViewById(R.id.seek_columns)
        lblRows = findViewById(R.id.lbl_rows)
        seekRows = findViewById(R.id.seek_rows)
        groupIconSize = findViewById(R.id.group_icon_size)
        btnSystemLogs = findViewById(R.id.btn_system_logs)
        btnDone = findViewById(R.id.btn_done)

        // Load current configurations
        val prefs = getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
        val columns = prefs.getInt("grid_columns", 4)
        val rows = prefs.getInt("grid_rows", 5)
        val iconSize = prefs.getString("icon_size", "medium") ?: "medium"

        // Set seek column state
        seekColumns.progress = (columns - 3).coerceIn(0, 3)
        lblColumns.text = "Columns: $columns"

        // Set seek row state
        seekRows.progress = (rows - 4).coerceIn(0, 4)
        lblRows.text = "Rows: $rows"

        // Set radio button state
        when (iconSize) {
            "small" -> findViewById<RadioButton>(R.id.radio_small).isChecked = true
            "large" -> findViewById<RadioButton>(R.id.radio_large).isChecked = true
            else -> findViewById<RadioButton>(R.id.radio_medium).isChecked = true
        }

        // SeekBar listeners
        seekColumns.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val actualVal = progress + 3
                lblColumns.text = "Columns: $actualVal"
                prefs.edit().putInt("grid_columns", actualVal).apply()
                (application as? LauncherApplication)?.logLifecycleEvent("Settings: Columns updated to $actualVal")
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekRows.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val actualVal = progress + 4
                lblRows.text = "Rows: $actualVal"
                prefs.edit().putInt("grid_rows", actualVal).apply()
                (application as? LauncherApplication)?.logLifecycleEvent("Settings: Rows updated to $actualVal")
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // RadioGroup listener
        groupIconSize.setOnCheckedChangeListener { _, checkedId ->
            val sizeStr = when (checkedId) {
                R.id.radio_small -> "small"
                R.id.radio_large -> "large"
                else -> "medium"
            }
            prefs.edit().putString("icon_size", sizeStr).apply()
            (application as? LauncherApplication)?.logLifecycleEvent("Settings: Icon size updated to $sizeStr")
        }

        // Logs Viewer Button
        btnSystemLogs.setOnClickListener {
            startActivity(Intent(this, LogViewerActivity::class.java))
        }

        // Done / Save Button
        btnDone.setOnClickListener {
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        (application as? LauncherApplication)?.logLifecycleEvent("SettingsActivity OnStart")
    }

    override fun onResume() {
        super.onResume()
        (application as? LauncherApplication)?.logLifecycleEvent("SettingsActivity OnResume")
    }

    override fun onPause() {
        super.onPause()
        (application as? LauncherApplication)?.logLifecycleEvent("SettingsActivity OnPause")
    }

    override fun onStop() {
        super.onStop()
        (application as? LauncherApplication)?.logLifecycleEvent("SettingsActivity OnStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as? LauncherApplication)?.logLifecycleEvent("SettingsActivity OnDestroy")
    }
}
