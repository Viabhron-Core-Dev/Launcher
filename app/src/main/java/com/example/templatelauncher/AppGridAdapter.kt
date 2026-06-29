package com.example.templatelauncher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppGridAdapter(
    private val context: Context,
    private val mode: Int, // 0 = Workspace Grid, 1 = Drawer List
    private var items: List<AppItem?>,
    private val iconSize: String, // "small", "medium", "large"
    private val onItemClick: (AppItem) -> Unit,
    private val onEmptyLongClick: ((position: Int) -> Unit)? = null,
    private val onItemLongClick: ((AppItem, position: Int) -> Unit)? = null
) : RecyclerView.Adapter<AppGridAdapter.AppViewHolder>() {

    companion object {
        const val MODE_WORKSPACE = 0
        const val MODE_DRAWER = 1
    }

    private val inflater = LayoutInflater.from(context)
    private val appClass = context.applicationContext as LauncherApplication
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    // Cache pre-computed empty placeholder bitmap
    private val placeholderBitmap: Bitmap by lazy {
        val size = dpToPx(48)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint().apply {
            color = 0x44FFFFFF.toInt()
            style = Paint.Style.STROKE
            strokeWidth = dpToPx(1).toFloat()
            isAntiAlias = true
        }
        val center = size / 2f
        canvas.drawCircle(center, center, center - dpToPx(4), paint)

        // Draw a tiny plus
        val linePaint = Paint().apply {
            color = 0x66FFFFFF.toInt()
            strokeWidth = dpToPx(2).toFloat()
            isAntiAlias = true
        }
        val len = dpToPx(6).toFloat()
        canvas.drawLine(center - len, center, center + len, center, linePaint)
        canvas.drawLine(center, center - len, center, center + len, linePaint)
        bmp
    }

    fun updateItems(newItems: List<AppItem?>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = inflater.inflate(R.layout.item_app_cell, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val item = items.getOrNull(position)

        // Set dimensions based on icon size setting
        val pxSize = when (iconSize) {
            "small" -> dpToPx(36)
            "large" -> dpToPx(60)
            else -> dpToPx(48) // medium
        }

        val layoutParams = holder.imgIcon.layoutParams
        layoutParams.width = pxSize
        layoutParams.height = pxSize
        holder.imgIcon.layoutParams = layoutParams

        if (item == null) {
            // Empty Cell (Only possible in MODE_WORKSPACE)
            holder.txtLabel.text = ""
            holder.imgIcon.setImageBitmap(placeholderBitmap)
            holder.imgIcon.alpha = 0.5f

            holder.itemView.setOnClickListener(null)
            holder.itemView.setOnLongClickListener {
                onEmptyLongClick?.invoke(position)
                true
            }
        } else {
            // App Cell
            holder.txtLabel.text = item.label
            holder.imgIcon.alpha = 1.0f

            val packageName = item.packageName
            holder.imgIcon.tag = packageName

            // Try resolving icon via Cache
            val cachedBmp = appClass.iconCache.get(packageName)
            if (cachedBmp != null) {
                holder.imgIcon.setImageBitmap(cachedBmp)
            } else {
                // Set default system placeholder first
                holder.imgIcon.setImageResource(android.R.drawable.sym_def_app_icon)

                // Load on Dispatchers.IO asynchronously
                coroutineScope.launch {
                    val loadedBmp = withContext(Dispatchers.IO) {
                        try {
                            val pm = context.packageManager
                            val intent = pm.getLaunchIntentForPackage(packageName)
                            val drawable = if (intent != null) {
                                pm.getActivityIcon(intent)
                            } else {
                                pm.getApplicationIcon(packageName)
                            }
                            drawableToBitmap(drawable)
                        } catch (e: Exception) {
                            null
                        }
                    }

                    if (loadedBmp != null) {
                        // Store in cache
                        appClass.iconCache.put(packageName, loadedBmp)
                        // Verify tag before setting to handle recycling
                        if (holder.imgIcon.tag == packageName) {
                            holder.imgIcon.setImageBitmap(loadedBmp)
                        }
                    }
                }
            }

            holder.itemView.setOnClickListener {
                onItemClick(item)
            }

            holder.itemView.setOnLongClickListener {
                onItemLongClick?.invoke(item, position)
                true
            }
        }
    }

    override fun getItemCount(): Int = items.size

    private fun dpToPx(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else dpToPx(48)
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else dpToPx(48)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        return bitmap
    }

    class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgIcon: ImageView = view.findViewById(R.id.img_app_icon)
        val txtLabel: TextView = view.findViewById(R.id.txt_app_label)
    }
}
