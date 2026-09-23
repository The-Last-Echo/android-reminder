package com.thelastecho.reminder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.thelastecho.reminder.domain.model.Category

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorArgb: Long,
    val iconName: String
) {
    fun toDomain(): Category = Category(
        id = id,
        name = name,
        colorArgb = colorArgb,
        iconName = iconName
    )

    companion object {
        fun fromDomain(domain: Category): CategoryEntity = CategoryEntity(
            id = domain.id,
            name = domain.name,
            colorArgb = domain.colorArgb,
            iconName = domain.iconName
        )
    }
}
