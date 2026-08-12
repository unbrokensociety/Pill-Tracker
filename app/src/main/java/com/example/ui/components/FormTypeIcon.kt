package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

enum class FormType(val key: String, val stringRes: Int, val icon: ImageVector) {
    CAPSULE("capsule", R.string.form_capsule, Icons.Default.Medication),
    TABLET("tablet", R.string.form_tablet, Icons.Default.Circle),
    LIQUID("liquid", R.string.form_liquid, Icons.Default.WaterDrop),
    DROPS("drops", R.string.form_drops, Icons.Default.InvertColors),
    INJECTION("injection", R.string.form_injection, Icons.Default.Vaccines),
    SPRAY("spray", R.string.form_spray, Icons.Default.Air),
    PATCH("patch", R.string.form_patch, Icons.Default.Medication);

    companion object {
        fun fromKey(key: String): FormType {
            return values().find { it.key.equals(key, ignoreCase = true) } ?: CAPSULE
        }
    }
}

@Composable
fun FormTypeIcon(
    formKey: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = tint.copy(alpha = 0.15f),
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    modifier: Modifier = Modifier
) {
    val formType = FormType.fromKey(formKey)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = formType.icon,
            contentDescription = stringResource(formType.stringRes),
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}
