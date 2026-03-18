package com.datainterview.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val activationId: Long,
    val interactionType: String,  // "deverrouillage", "application", "widget"
    val time: String,             // HH:MM format
    val appOrWidgetName: String?, // app name or widget name
    val closeTime: String?,       // HH:MM for app close, null for unlocks
    val widgetLocation: String?,  // "ecran_verrouillage", "ecran_accueil", null
    val timestampMillis: Long = System.currentTimeMillis() // for ordering
)
