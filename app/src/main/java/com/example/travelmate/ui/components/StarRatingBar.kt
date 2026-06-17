package com.example.travelmate.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun StarRatingBar(
    rating: Float,
    onRatingChanged: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        for (i in 1..5) {
            if (onRatingChanged != null) {
                IconButton(onClick = { onRatingChanged(i.toFloat()) }) {
                    Icon(
                        imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "$i sao",
                        tint = if (i <= rating) Color(0xFFFFC107) else Color.Gray
                    )
                }
            } else {
                Icon(
                    imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "$i sao",
                    tint = if (i <= rating) Color(0xFFFFC107) else Color.Gray
                )
            }
        }
    }
}
