package dev.xiaoming.compose.example.glance.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

@Composable
fun GlancePreview(content: @Composable () -> Unit) {
    GlanceTheme {
        CompositionLocalProvider(
            LocalInspectionMode provides true
        ) {
            content()
        }
    }
}

@Composable
fun GlancePreview(sizes: List<Pair<Int, Int>>, content: @Composable () -> Unit) {
    GlanceTheme {
        CompositionLocalProvider(
            LocalInspectionMode provides true
        ) {
            Column {
                sizes.forEach { (cellWidth, cellHeight) ->
                    GlancePreviewWithSize(
                        cellWidth = cellWidth,
                        cellHeight = cellHeight,
                        content = content
                    )
                }
            }
        }
    }
}

@Composable
private fun GlancePreviewWithSize(
    cellWidth: Int,
    cellHeight: Int,
    content: @Composable () -> Unit
) {
    Column(
        modifier = GlanceModifier.padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$cellWidth x $cellHeight",
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = GlanceModifier.padding(bottom = 16.dp)
        )

        val size = DpSize(width = cellWidth.dp * 73 - 16.dp, height = cellHeight.dp * 118 - 16.dp)
        Box(modifier = GlanceModifier.size(size.width, size.height)) {
            CompositionLocalProvider(LocalSize provides size) {
                content()
            }
        }
    }
}