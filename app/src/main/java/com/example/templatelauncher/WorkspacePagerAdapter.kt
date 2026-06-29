package com.example.templatelauncher

import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.templatelauncher.db.WorkspaceItem

class WorkspacePagerAdapter(
    private val activity: HomeActivity,
    private val pageCount: Int,
    private val columns: Int,
    private val rows: Int,
    private val iconSize: String,
    private val workspaceItems: List<WorkspaceItem>,
    private val installedApps: List<AppItem>
) : RecyclerView.Adapter<WorkspacePagerAdapter.PageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val recyclerView = RecyclerView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            val paddingPx = (8 * resources.displayMetrics.density).toInt()
            setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            clipToPadding = false
        }
        return PageViewHolder(recyclerView)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val gridLayoutManager = GridLayoutManager(holder.itemView.context, columns)
        holder.recyclerView.layoutManager = gridLayoutManager

        // Generate static grid cells list
        val totalCells = columns * rows
        val pageItems = ArrayList<AppItem?>(totalCells)
        for (i in 0 until totalCells) {
            pageItems.add(null)
        }

        // Map workspace items from Room to exact cell indexes on this page
        val pageWorkspace = workspaceItems.filter { it.page == position }
        for (wItem in pageWorkspace) {
            val cellIndex = wItem.row * columns + wItem.col
            if (cellIndex in 0 until totalCells) {
                val app = installedApps.find { it.packageName == wItem.packageName }
                if (app != null) {
                    pageItems[cellIndex] = app
                }
            }
        }

        // Initialize standalone AppGridAdapter
        val adapter = AppGridAdapter(
            context = holder.itemView.context,
            mode = AppGridAdapter.MODE_WORKSPACE,
            items = pageItems,
            iconSize = iconSize,
            onItemClick = { clickedApp ->
                activity.launchApp(clickedApp.packageName)
            },
            onEmptyLongClick = { cellPos ->
                val col = cellPos % columns
                val row = cellPos / columns
                activity.showAppPickerDialog(position, col, row)
            },
            onItemLongClick = { clickedApp, cellPos ->
                val col = cellPos % columns
                val row = cellPos / columns
                activity.showRemoveAppDialog(position, col, row, clickedApp.label)
            }
        )
        holder.recyclerView.adapter = adapter
    }

    override fun getItemCount(): Int = pageCount

    class PageViewHolder(val recyclerView: RecyclerView) : RecyclerView.ViewHolder(recyclerView)
}
