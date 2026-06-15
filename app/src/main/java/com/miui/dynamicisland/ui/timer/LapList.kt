package com.miui.dynamicisland.ui.timer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun LapList(
    laps: List<Long>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 120.dp)
    ) {
        itemsIndexed(laps.reversed()) { index, lapTime ->
            val lapNumber = laps.size - index
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Lap $lapNumber",
                    color = Color(0xFF8E8E93),
                    fontSize = 14.sp
                )
                Text(
                    text = formatLapTime(lapTime),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            if (index < laps.size - 1) {
                HorizontalDivider(color = Color(0xFF2C2C2E), thickness = 0.5.dp)
            }
        }
    }
}

private fun formatLapTime(ms: Long): String {
    val centiseconds = (ms / 10) % 100
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    return String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, centiseconds)
}
