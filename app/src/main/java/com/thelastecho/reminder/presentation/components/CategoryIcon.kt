package com.thelastecho.reminder.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.StarBorder
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
        "pets" -> Icons.Outlined.Pets
        "fitness" -> Icons.Outlined.FitnessCenter
        "music" -> Icons.Outlined.MusicNote
        "book" -> Icons.AutoMirrored.Outlined.MenuBook
        "event" -> Icons.Outlined.Event
        "car" -> Icons.Outlined.DirectionsCar
        "money" -> Icons.Outlined.Payments
        "restaurant" -> Icons.Outlined.Restaurant
        "cleaning" -> Icons.Outlined.CleaningServices
        "gift" -> Icons.Outlined.CardGiftcard
        "nature" -> Icons.Outlined.Park
        "computer" -> Icons.Outlined.Computer
        "phone" -> Icons.Outlined.PhoneAndroid
        "game" -> Icons.Outlined.SportsEsports
        "beach" -> Icons.Outlined.BeachAccess
        "weekend" -> Icons.Outlined.Weekend
        "lock" -> Icons.Outlined.Lock
        "star" -> Icons.Outlined.StarBorder
        else -> Icons.AutoMirrored.Outlined.Label
    }
    Icon(icon, contentDescription = null, modifier = modifier, tint = tint)
}
