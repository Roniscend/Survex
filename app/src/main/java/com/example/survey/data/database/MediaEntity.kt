package com.example.survey.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "media_items",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["name"],
            childColumns = ["sessionName"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionName"])]
)
data class MediaEntity(
    @PrimaryKey
    val id: String = "",
    val sessionName: String, // Ensure this matches exactly with SessionEntity
    val filePath: String,
    val isVideo: Boolean,
    val timestamp: String,
    val location: String,
    val createdAt: Long = System.currentTimeMillis()
)
