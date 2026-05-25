package com.jongheon.myreadle.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.SportsBaseball
import androidx.compose.ui.graphics.vector.ImageVector

fun categoryIcon(category: String): ImageVector = when (category) {
    "tech" -> Icons.Outlined.Memory
    "tech_business" -> Icons.Outlined.Business
    "world" -> Icons.Outlined.Public
    "culture" -> Icons.Outlined.Brush
    "sports" -> Icons.Outlined.SportsBaseball
    "energy" -> Icons.Outlined.Bolt
    else -> Icons.Outlined.Apartment
}

fun categoryLabel(category: String): String = when (category) {
    "tech" -> "Tech"
    "tech_business" -> "Tech Business"
    "world" -> "World"
    "culture" -> "Culture"
    else -> category.replace('_', ' ').replaceFirstChar { it.uppercase() }
}
