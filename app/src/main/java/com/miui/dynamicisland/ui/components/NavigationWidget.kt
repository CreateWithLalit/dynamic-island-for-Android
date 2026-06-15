// File: app/src/main/java/com/miui/dynamicisland/ui/components/NavigationWidget.kt
package com.miui.dynamicisland.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miui.dynamicisland.ui.states.IslandState

import coil.compose.rememberAsyncImagePainter

@Composable
fun NavigationWidget(
    state: IslandState.Navigation,
    slot: NavigationSlot,
    isExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (isExpanded) {
        NavigationExpandedWidget(state, modifier)
    } else {
        when (slot) {
            NavigationSlot.LEFT -> NavigationLeftSlot(state, modifier)
            NavigationSlot.RIGHT -> NavigationRightSlot(state, modifier)
        }
    }
}

@Composable
private fun NavigationLeftSlot(state: IslandState.Navigation, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(start = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (state.appIcon != null) {
            Image(
                painter = rememberAsyncImagePainter(state.appIcon),
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                tint = if (state.isUrgent) Color.Red else Color(0xFF4285F4),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun NavigationRightSlot(state: IslandState.Navigation, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(end = 12.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Text(
            text = state.distance,
            color = if (state.isUrgent) Color(0xFF34C759) else Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun NavigationExpandedWidget(state: IslandState.Navigation, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.appIcon != null) {
                    Image(
                        painter = rememberAsyncImagePainter(state.appIcon),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = Color(0xFF4285F4),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = state.street.ifBlank { "Bloomington Hwy" },
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = if (state.toward.isNotBlank()) "toward ${state.toward}" else "toward 3106 Sports Arena Blvd",
                color = Color(0xFF8E8E93),
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 30.dp), // Align with text after icon
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Green Action Button
                Row(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(Color(0xFF34C759))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = getDirectionIcon(state.direction),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = state.distance,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (state.nextDirection != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Then",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = getDirectionIcon(state.nextDirection),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Speaker Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2C2C2E))
                        .clickable { /* Toggle Mute */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Exit Button
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(Color(0xFFFF3B30))
                        .clickable { /* Exit Nav */ }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Exit",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Map Snippet (Improved to look more like the reference)
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(Color(0xFF1C1C1E)),
            contentAlignment = Alignment.Center
        ) {
            if (state.mapSnippet != null) {
                Image(
                    bitmap = state.mapSnippet.asImageBitmap(),
                    contentDescription = "Map",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Overlay a subtle gradient to make it look more like a map UI
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.2f)),
                                center = androidx.compose.ui.geometry.Offset.Unspecified,
                                radius = Float.POSITIVE_INFINITY
                            )
                        )
                )
            } else {
                // Better placeholder for Map
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "MAP",
                        color = Color(0xFF8E8E93),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun getDirectionIcon(direction: IslandState.Navigation.Direction): ImageVector = when (direction) {
    IslandState.Navigation.Direction.LEFT         -> Icons.AutoMirrored.Filled.ArrowBack
    IslandState.Navigation.Direction.RIGHT        -> Icons.AutoMirrored.Filled.ArrowForward
    IslandState.Navigation.Direction.STRAIGHT     -> Icons.Default.ArrowUpward
    IslandState.Navigation.Direction.SLIGHT_LEFT  -> Icons.AutoMirrored.Filled.Reply // Placeholder
    IslandState.Navigation.Direction.SLIGHT_RIGHT -> Icons.AutoMirrored.Filled.Reply // Placeholder
    IslandState.Navigation.Direction.U_TURN       -> Icons.AutoMirrored.Filled.Reply
    IslandState.Navigation.Direction.MERGE        -> Icons.Default.Merge
    IslandState.Navigation.Direction.EXIT         -> Icons.AutoMirrored.Filled.ExitToApp
    IslandState.Navigation.Direction.ARRIVE       -> Icons.Default.CheckCircle
    else                                          -> Icons.Default.Navigation
}
