package com.example.templatelauncher

import android.app.AlertDialog
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.templatelauncher.db.LauncherDatabase
import com.example.templatelauncher.db.WorkspaceItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeActivity : ComponentActivity() {

    private lateinit var imgWallpaper: ImageView
    private lateinit var pagerWorkspace: ViewPager2
    private lateinit var layoutDots: LinearLayout
    private lateinit var layoutDock: LinearLayout
    private lateinit var imgSettingsIcon: ImageView

    // Drawer Overlay views
    private lateinit var layoutDrawerOverlay: LinearLayout
    private lateinit var editSearch: EditText
    private lateinit var recyclerDrawerApps: RecyclerView

    // Preferences & Config
    private var gridColumns = 4
    private var gridRows = 5
    private var iconSizeSetting = "medium"

    // Data lists
    private var installedApps = listOf<AppItem>()
    private var filteredDrawerApps = listOf<AppItem>()
    private var workspaceItemsFromDb = listOf<WorkspaceItem>()

    // Adapter for drawer
    private lateinit var drawerAdapter: AppGridAdapter

    // Wallpaper cache
    companion object {
        private var cachedWallpaper: Bitmap? = null
    }

    // Back button drawer toggle
    private val onBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            hideAppDrawer()
        }
    }

    // Dynamic package receiver
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == Intent.ACTION_PACKAGE_ADDED ||
                action == Intent.ACTION_PACKAGE_REMOVED ||
                action == Intent.ACTION_PACKAGE_CHANGED
            ) {
                (application as? LauncherApplication)?.logLifecycleEvent("Package Broadcast received: $action")
                loadLauncherData()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Log lifecycle
        (application as? LauncherApplication)?.logLifecycleEvent("HomeActivity OnCreate")

        // Initialize views
        imgWallpaper = findViewById(R.id.img_wallpaper)
        pagerWorkspace = findViewById(R.id.pager_workspace)
        layoutDots = findViewById(R.id.layout_dots)
        layoutDock = findViewById(R.id.layout_dock)
        imgSettingsIcon = findViewById(R.id.img_settings_icon)

        layoutDrawerOverlay = findViewById(R.id.layout_drawer_overlay)
        editSearch = findViewById(R.id.edit_search)
        recyclerDrawerApps = findViewById(R.id.recycler_drawer_apps)

        // Add back-press listener for Drawer closing
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        // Bind quick settings click
        imgSettingsIcon.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Initialize background wallpaper load
        loadWallpaperAsync()

        // Configure drawer search TextWatcher
        editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterDrawerApps(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Initial launch configuration load
        loadLauncherData()
    }

    override fun onResume() {
        super.onResume()
        (application as? LauncherApplication)?.logLifecycleEvent("HomeActivity OnResume")

        // Register package events receiver
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        registerReceiver(packageReceiver, filter)

        // Reload user customization preferences & reconstruct desktop grids
        reloadPreferencesAndGrids()
    }

    override fun onPause() {
        super.onPause()
        (application as? LauncherApplication)?.logLifecycleEvent("HomeActivity OnPause")
        try {
            unregisterReceiver(packageReceiver)
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    override fun onStart() {
        super.onStart()
        (application as? LauncherApplication)?.logLifecycleEvent("HomeActivity OnStart")
    }

    override fun onStop() {
        super.onStop()
        (application as? LauncherApplication)?.logLifecycleEvent("HomeActivity OnStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as? LauncherApplication)?.logLifecycleEvent("HomeActivity OnDestroy")
    }

    private fun reloadPreferencesAndGrids() {
        val prefs = getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
        gridColumns = prefs.getInt("grid_columns", 4)
        gridRows = prefs.getInt("grid_rows", 5)
        iconSizeSetting = prefs.getString("icon_size", "medium") ?: "medium"

        // Reload data from Room & system to adapt grids
        loadLauncherData()
    }

    private fun loadLauncherData() {
        lifecycleScope.launch {
            // Fetch installed applications from package manager
            val systemApps = withContext(Dispatchers.IO) {
                val pm = packageManager
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = pm.queryIntentActivities(intent, 0)
                resolveInfos.map { info ->
                    AppItem(
                        packageName = info.activityInfo.packageName,
                        label = info.loadLabel(pm).toString(),
                        icon = null
                    )
                }.sortedBy { it.label.lowercase() }
            }

            installedApps = systemApps
            filteredDrawerApps = systemApps

            // Fetch workspace allocations from Room database
            val dbItems = withContext(Dispatchers.IO) {
                LauncherDatabase.getDatabase(this@HomeActivity).workspaceDao().getAllWorkspaceItems()
            }
            workspaceItemsFromDb = dbItems

            // Reconstruct Pager views
            setupWorkspacePager()

            // Repopulate Dock
            setupDockLayout()

            // Reconstruct Drawer grid view
            setupDrawerGrid()
        }
    }

    private fun loadWallpaperAsync() {
        val cached = cachedWallpaper
        if (cached != null) {
            imgWallpaper.setImageBitmap(cached)
            return
        }

        lifecycleScope.launch {
            val bmp = withContext(Dispatchers.IO) {
                try {
                    val wm = WallpaperManager.getInstance(this@HomeActivity)
                    val drawable = wm.drawable
                    if (drawable != null) {
                        drawableToBitmap(drawable)
                    } else {
                        null
                    }
                } catch (e: SecurityException) {
                    null
                } catch (e: Exception) {
                    null
                }
            }

            if (bmp != null) {
                cachedWallpaper = bmp
                imgWallpaper.setImageBitmap(bmp)
            } else {
                // Fallback to solid dark slate background if unable to load system wallpaper
                imgWallpaper.setBackgroundColor(0xFF1C1C1E.toInt())
            }
        }
    }

    private fun setupWorkspacePager() {
        val pageCount = 3
        val adapter = WorkspacePagerAdapter(
            activity = this,
            pageCount = pageCount,
            columns = gridColumns,
            rows = gridRows,
            iconSize = iconSizeSetting,
            workspaceItems = workspaceItemsFromDb,
            installedApps = installedApps
        )
        pagerWorkspace.adapter = adapter

        // Setup dots indicator layout
        setupPageDots(pageCount)

        pagerWorkspace.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updatePageDots(position)
            }
        })
    }

    private fun setupPageDots(count: Int) {
        layoutDots.removeAllViews()
        val density = resources.displayMetrics.density
        val size = (6 * density).toInt()
        val margin = (4 * density).toInt()

        for (i in 0 until count) {
            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(margin, 0, margin, 0)
                }
                setBackgroundColor(if (i == 0) 0xFF0A84FF.toInt() else 0x44FFFFFF)
            }
            layoutDots.addView(dot)
        }
    }

    private fun updatePageDots(selectedPos: Int) {
        for (i in 0 until layoutDots.childCount) {
            val dot = layoutDots.getChildAt(i)
            dot?.setBackgroundColor(if (i == selectedPos) 0xFF0A84FF.toInt() else 0x44FFFFFF)
        }
    }

    private fun setupDockLayout() {
        layoutDock.removeAllViews()

        // 5 slots total: [App][App][Drawer Center Button][App][App]
        for (slotIndex in 0..4) {
            val cellView = LayoutInflater.from(this).inflate(R.layout.item_app_cell, layoutDock, false)
            cellView.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                gravity = Gravity.CENTER_VERTICAL
            }

            val imgIcon = cellView.findViewById<ImageView>(R.id.img_app_icon)
            val txtLabel = cellView.findViewById<TextView>(R.id.txt_app_label)

            // Adjust dimensions for Dock items as well
            val pxSize = when (iconSizeSetting) {
                "small" -> (36 * resources.displayMetrics.density).toInt()
                "large" -> (60 * resources.displayMetrics.density).toInt()
                else -> (48 * resources.displayMetrics.density).toInt()
            }
            imgIcon.layoutParams.width = pxSize
            imgIcon.layoutParams.height = pxSize

            if (slotIndex == 2) {
                // Fixed Drawer Toggle in Center
                txtLabel.text = "Apps"
                txtLabel.setTextColor(0xFF8E8E93.toInt())
                imgIcon.setImageResource(android.R.drawable.ic_dialog_dialer)
                imgIcon.alpha = 1.0f

                cellView.setOnClickListener {
                    showAppDrawer()
                }
            } else {
                // Map slot index 0,1,3,4 to defaults (first 4 alphabetical apps)
                val defaultAppIndex = when (slotIndex) {
                    0 -> 0
                    1 -> 1
                    3 -> 2
                    4 -> 3
                    else -> 0
                }

                val app = installedApps.getOrNull(defaultAppIndex)
                if (app != null) {
                    txtLabel.text = app.label
                    txtLabel.setTextColor(0xFFFFFFFF.toInt())
                    imgIcon.alpha = 1.0f

                    // Attempt resolving cached icon or load asynchronously
                    val cachedBmp = (applicationContext as LauncherApplication).iconCache.get(app.packageName)
                    if (cachedBmp != null) {
                        imgIcon.setImageBitmap(cachedBmp)
                    } else {
                        imgIcon.setImageResource(android.R.drawable.sym_def_app_icon)
                        lifecycleScope.launch {
                            val bmp = withContext(Dispatchers.IO) {
                                try {
                                    val pm = packageManager
                                    val intent = pm.getLaunchIntentForPackage(app.packageName)
                                    val drawable = if (intent != null) pm.getActivityIcon(intent) else pm.getApplicationIcon(app.packageName)
                                    drawableToBitmap(drawable)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            if (bmp != null) {
                                (applicationContext as LauncherApplication).iconCache.put(app.packageName, bmp)
                                imgIcon.setImageBitmap(bmp)
                            }
                        }
                    }

                    cellView.setOnClickListener {
                        launchApp(app.packageName)
                    }
                } else {
                    // Empty dock slot placeholder
                    txtLabel.text = ""
                    imgIcon.setImageDrawable(null)
                }
            }

            layoutDock.addView(cellView)
        }
    }

    private fun setupDrawerGrid() {
        val columns = 4 // Drawer standard column count
        recyclerDrawerApps.layoutManager = GridLayoutManager(this, columns)

        drawerAdapter = AppGridAdapter(
            context = this,
            mode = AppGridAdapter.MODE_DRAWER,
            items = filteredDrawerApps,
            iconSize = iconSizeSetting,
            onItemClick = { clickedApp ->
                launchApp(clickedApp.packageName)
                hideAppDrawer()
            }
        )
        recyclerDrawerApps.adapter = drawerAdapter
    }

    private fun filterDrawerApps(query: String) {
        filteredDrawerApps = if (query.isEmpty()) {
            installedApps
        } else {
            installedApps.filter { it.label.contains(query, ignoreCase = true) }
        }
        drawerAdapter.updateItems(filteredDrawerApps)
    }

    private fun showAppDrawer() {
        (application as? LauncherApplication)?.logLifecycleEvent("Opening App Drawer")
        layoutDrawerOverlay.visibility = View.VISIBLE
        onBackPressedCallback.isEnabled = true
        editSearch.setText("")
        editSearch.requestFocus()
    }

    private fun hideAppDrawer() {
        (application as? LauncherApplication)?.logLifecycleEvent("Closing App Drawer")
        layoutDrawerOverlay.visibility = View.INVISIBLE
        onBackPressedCallback.isEnabled = false
        editSearch.clearFocus()
    }

    fun launchApp(packageName: String) {
        (application as? LauncherApplication)?.logLifecycleEvent("Launching package: $packageName")
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                startActivity(launchIntent)
            } else {
                Toast.makeText(this, "Unable to open application", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Launch error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun showAppPickerDialog(page: Int, col: Int, row: Int) {
        val appNames = installedApps.map { it.label }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Place App at ($col, $row)")
            .setItems(appNames) { _, index ->
                val selectedApp = installedApps[index]
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        val db = LauncherDatabase.getDatabase(this@HomeActivity)
                        // Clear any pre-existing entry in that exact slot first
                        db.workspaceDao().deleteWorkspaceItemAt(page, col, row)
                        // Insert new placement
                        db.workspaceDao().insertWorkspaceItem(
                            WorkspaceItem(
                                packageName = selectedApp.packageName,
                                page = page,
                                col = col,
                                row = row
                            )
                        )
                    }
                    Toast.makeText(this@HomeActivity, "Placed ${selectedApp.label}", Toast.LENGTH_SHORT).show()
                    loadLauncherData() // refresh
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun showRemoveAppDialog(page: Int, col: Int, row: Int, appName: String) {
        AlertDialog.Builder(this)
            .setTitle("Remove Application")
            .setMessage("Remove $appName from this home screen cell?")
            .setPositiveButton("Remove") { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        val db = LauncherDatabase.getDatabase(this@HomeActivity)
                        db.workspaceDao().deleteWorkspaceItemAt(page, col, row)
                    }
                    Toast.makeText(this@HomeActivity, "Cell cleared", Toast.LENGTH_SHORT).show()
                    loadLauncherData() // refresh
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 100
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 100
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        return bitmap
    }
}
