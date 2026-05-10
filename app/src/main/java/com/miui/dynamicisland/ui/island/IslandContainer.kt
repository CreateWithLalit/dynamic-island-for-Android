// app/src/main/kotlin/com/miui/dynamicisland/ui/island/IslandContainer.kt
package com.miui.dynamicisland.ui.island

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun IslandContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .wrapContentWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.TopCenter
    ) {
        content()
    }
}