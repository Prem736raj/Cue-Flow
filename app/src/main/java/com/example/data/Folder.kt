package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey val name: String,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable
