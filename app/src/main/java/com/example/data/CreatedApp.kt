package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "created_apps")
data class CreatedApp(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val version: String = "1.0.0",
    val credits: String = "",
    val type: String, // "HTML" or "WEB"
    val appIconUri: String? = null,
    val orientation: String = "AUTO", // "PORTRAIT", "LANDSCAPE", "AUTO"
    val swipeToRefresh: Boolean = false,
    val url: String? = null, // Used for WEB
    val htmlContent: String? = null, // Stored custom index.html for HTML app
    val cssContent: String? = null, // Stored style.css for HTML app
    val jsContent: String? = null, // Stored script.js for HTML app
    val localFilesCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
