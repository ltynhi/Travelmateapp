package com.example.travelmate.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelmate.navigation.NavRoutes
import com.example.travelmate.ui.theme.SkyBlue40

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String,
    val emoji: String
)

@Composable
fun BottomNavBar(
    currentRoute: String,
    onItemClick: (String) -> Unit
) {
    val items = listOf(
        BottomNavItem("Khám phá", Icons.Filled.Explore,      NavRoutes.HOME,     "🌍"),
        BottomNavItem("Yêu thích", Icons.Filled.Favorite,    NavRoutes.FAVORITES,"❤️"),
        BottomNavItem("Chuyến đi", Icons.Filled.Map,         NavRoutes.TRIPS,    "✈️"),
        BottomNavItem("Timeline",  Icons.Filled.PhotoLibrary, NavRoutes.TIMELINE, "📸"),
        BottomNavItem("Hồ sơ",    Icons.Filled.Person,       NavRoutes.PROFILE,  "👤")
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                NavBarItem(
                    item = item,
                    isSelected = isSelected,
                    onClick = { onItemClick(item.route) }
                )
            }
        }
    }
}

@Composable
private fun NavBarItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected)
            SkyBlue40.copy(alpha = 0.15f)
        else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "navBg"
    )
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) SkyBlue40
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "navTint"
    )
    val itemWidth by animateDpAsState(
        targetValue = if (isSelected) 80.dp else 56.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "navWidth"
    )

    Column(
        modifier = Modifier
            .width(itemWidth)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        if (isSelected) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = SkyBlue40,
                maxLines = 1
            )
        }
    }
}
