package com.thelastecho.reminder.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalGroceryStore
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun CategoryIcon(name: String, modifier: Modifier = Modifier, tint: Color = Color.Unspecified) {
    val icon = when (name) {
        "person" -> Icons.Outlined.Person
        "work" -> Icons.Outlined.Work
        "shopping_cart" -> Icons.Outlined.ShoppingCart
        "home" -> Icons.Outlined.Home
        "school" -> Icons.Outlined.School
        "favorite" -> Icons.Outlined.FavoriteBorder
        "flight" -> Icons.Outlined.Flight
        "cafe" -> Icons.Outlined.LocalCafe
        "grocery" -> Icons.Outlined.LocalGroceryStore
        "health" -> Icons.Outlined.MedicalServices
        "build" -> Icons.Outlined.Build
        else -> Icons.AutoMirrored.Outlined.Label
    }
    Icon(icon, contentDescription = null, modifier = modifier, tint = tint)
}
