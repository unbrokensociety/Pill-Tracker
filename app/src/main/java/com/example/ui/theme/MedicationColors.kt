package com.example.ui.theme

import androidx.compose.ui.graphics.Color

object MedicationColors {
    val predefinedColors = listOf(
        Color(0xFF0284C7), // Sky Blue (Default #0)
        Color(0xFF0D9488), // Emerald Teal
        Color(0xFF16A34A), // Fresh Green
        Color(0xFF7C3AED), // Royal Purple
        Color(0xFFD97706), // Amber Orange
        Color(0xFFDB2777), // Rose Pink
        Color(0xFF2563EB), // Indigo Blue
        Color(0xFFEF4444)  // Coral Red
    )

    fun getColor(index: Int, fallbackName: String = ""): Color {
        return if (index in predefinedColors.indices) {
            predefinedColors[index]
        } else if (fallbackName.isNotBlank()) {
            val idx = Math.abs(fallbackName.hashCode()) % predefinedColors.size
            predefinedColors[idx]
        } else {
            predefinedColors[0]
        }
    }
}
