package com.thelastecho.reminder.domain.model

/**
 * Category for organizing reminders (e.g., Work, Personal, Shopping, etc.).
 */
data class Category(
    val id: Long = 0,
    val name: String,
    val colorArgb: Long = 0xFF6750A4,
    val iconName: String = "folder"
)
