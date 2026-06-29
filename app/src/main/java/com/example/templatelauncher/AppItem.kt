package com.example.templatelauncher

import android.graphics.Bitmap

data class AppItem(
    val packageName: String,
    val label: String,
    val icon: Bitmap? = null
)
