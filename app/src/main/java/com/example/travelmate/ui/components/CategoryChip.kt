package com.example.travelmate.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelmate.ui.theme.SkyBlue40
import com.example.travelmate.ui.theme.SkyBlue90
import com.example.travelmate.ui.theme.Mint40

private fun categoryEmoji(label: String): String = when (label) {
    "Biển"      -> "🏖️"
    "Núi"       -> "⛰️"
    "Cafe"      -> "☕"
    "Check-in"  -> "📸"
    "Di tích"   -> "🏛️"
    "Công viên" -> "🌿"
    "Quán ăn"   -> "🍜"
    else        -> "📍"
}

@Composable
fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "chipBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "chipText"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) Color.Transparent
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "chipBorder"
    )

    Box(
        modifier = Modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(50.dp))
            .background(
                if (selected) Brush.horizontalGradient(
                    colors = listOf(SkyBlue40, Mint40)
                ) else Brush.horizontalGradient(
                    colors = listOf(SkyBlue90, SkyBlue90)
                )
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = categoryEmoji(label),
                fontSize = 14.sp
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
