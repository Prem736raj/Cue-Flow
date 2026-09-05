package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "scripts")
data class Script(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val scrollSpeed: Int = 5,
    val fontSize: Int = 24,
    val isMirrored: Boolean = false,
    val folderName: String? = null,
    val textColor: String = "#FFFFFF",
    val bgOpacity: Float = 0.4f,
    val textAlignment: String = "left",
    val lineSpacing: String = "normal",
    val textDirection: String = "auto"
) : Serializable
