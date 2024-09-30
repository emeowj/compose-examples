package dev.xiaoming.compose.example.glance

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.unit.ColorProvider
import dev.xiaoming.compose.example.R

@Composable
fun RoundedCornerBox(
  modifier: GlanceModifier = GlanceModifier,
  cornerRadius: Dp = 8.dp,
  backgroundColor: ColorProvider = GlanceTheme.colors.widgetBackground,
  content: @Composable () -> Unit
) {
  Box(
    modifier = modifier.cornerRadiusCompat(cornerRadius, backgroundColor),
    contentAlignment = Alignment.Center,
    content = content
  )
}

private fun GlanceModifier.cornerRadiusCompat(radius: Dp, backgroundColor: ColorProvider) =
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    this.cornerRadius(radius).background(backgroundColor)
  } else if (radius > 0.dp) {
    val backgroundDrawable = when (radius) {
      4.dp -> R.drawable.widget_shape_small
      8.dp -> R.drawable.widget_shape_medium
      16.dp -> R.drawable.widget_shape_large
      else -> throw IllegalArgumentException("Unsupported radius $radius")
    }
    this.background(
      imageProvider = ImageProvider(resId = backgroundDrawable),
      colorFilter = ColorFilter.tint(backgroundColor)
    )
  } else {
    this.background(backgroundColor)
  }