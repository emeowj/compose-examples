package dev.xiaoming.compose.example.glance

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.text.Text

class NowPlayingWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = NowPlayingWidget()
}

class NowPlayingWidget : GlanceAppWidget() {
  override suspend fun provideGlance(context: Context, id: GlanceId) {
    provideContent {
      NowPlayingWidgetContent()
    }
  }
}

@Composable
private fun NowPlayingWidgetContent() {
  Text(text = "Hello", modifier = GlanceModifier.fillMaxWidth().height(84.dp))
}
