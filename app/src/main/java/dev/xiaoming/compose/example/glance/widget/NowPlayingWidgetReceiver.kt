package dev.xiaoming.compose.example.glance.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dev.xiaoming.compose.example.R
import dev.xiaoming.compose.example.glance.GlanceWidgetReceiver
import dev.xiaoming.compose.example.glance.TrackInfo
import dev.xiaoming.compose.example.glance.Tracks
import kotlinx.serialization.Serializable

class NowPlayingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NowPlayingWidget()
}

@Serializable
data class NowPlayingTrack(val info: TrackInfo? = null, val isPlaying: Boolean = false)


class NowPlayingWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override var stateDefinition = JsonStateDefinition(
        initialValue = NowPlayingTrack(),
        serializer = NowPlayingTrack.serializer()
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val initialState = initializeState(context = context, id = id)
        provideContent {
            val state: NowPlayingTrack = currentState<NowPlayingTrack>().let {
                if (it.info == null) initialState else it
            }
            NowPlayingWidget(state = state)
        }
    }

    private suspend fun initializeState(context: Context, id: GlanceId): NowPlayingTrack {
        val state =
            getAppWidgetState(context = context, definition = stateDefinition, glanceId = id)
        return if (state.info == null) {
            // Load the initial state and store it in the widget's data store, here we
            // just use some sample data. In a real application, you might want to load data
            // from the server or a local database.
            val track = Tracks.tracks.first()
            val newState = NowPlayingTrack(info = track, isPlaying = false)

            // Also persist the update to the widget's data store
            updateAppWidgetState(context = context, definition = stateDefinition, glanceId = id) {
                newState
            }
            newState
        } else {
            state
        }
    }
}

@Composable
private fun NowPlayingWidget(state: NowPlayingTrack) {
    GlanceTheme {
        RoundedCornerBox(
            cornerRadius = 16.dp,
            backgroundColor = GlanceTheme.colors.widgetBackground,
            modifier = GlanceModifier.fillMaxSize()
        ) {
            val trackInfo = state.info
            if (trackInfo != null) {
                NowPlayingWidgetUi(isPlaying = state.isPlaying, trackInfo = trackInfo)
            } else {
                Text(text = "Nothing is playing")
            }
        }
    }
}

@Composable
private fun NowPlayingWidgetUi(isPlaying: Boolean, trackInfo: TrackInfo) {
    val size = LocalSize.current
    Row(modifier = GlanceModifier.fillMaxSize().padding(8.dp)) {
        val imageSize = size.height - 16.dp
        GlanceImage(
            src = trackInfo.coverUrl,
            modifier = GlanceModifier.size(imageSize),
            cornerRadius = 8.dp
        )
        Column(modifier = GlanceModifier.defaultWeight().padding(start = 8.dp)) {
            Text(
                text = trackInfo.title,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium

                )
            )

            Row(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SeekButton(next = false)

                Spacer(modifier = GlanceModifier.width(8.dp).defaultWeight())

                PlayPauseButton(isPlaying = isPlaying)

                Spacer(modifier = GlanceModifier.width(8.dp).defaultWeight())

                SeekButton(next = true)

            }
        }
    }
}

@Composable
private fun SeekButton(next: Boolean, modifier: GlanceModifier = GlanceModifier) {
    val icon = if (next) R.drawable.ic_skip_next else R.drawable.ic_skip_previous
    CircleIconButton(
        imageProvider = ImageProvider(resId = icon),
        contentDescription = null,
        onClick = actionRunCallback<SeekAction>(
            parameters = actionParametersOf(
                SeekAction.PARAMETER_NEXT to next
            )
        ),
        modifier = modifier,
        backgroundColor = GlanceTheme.colors.secondaryContainer,
        contentColor = GlanceTheme.colors.onSecondaryContainer
    )
}

@Composable
private fun PlayPauseButton(isPlaying: Boolean, modifier: GlanceModifier = GlanceModifier) {
    val icon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
    CircleIconButton(
        imageProvider = ImageProvider(resId = icon),
        contentDescription = null,
        backgroundColor = GlanceTheme.colors.primary,
        contentColor = GlanceTheme.colors.onPrimary,
        onClick = actionRunCallback<PlayPauseAction>(),
        modifier = modifier
    )
}

class PlayPauseAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = Intent(context, GlanceWidgetReceiver::class.java).apply {
            action = GlanceWidgetReceiver.ACTION_PLAY_PAUSE
        }
        context.sendBroadcast(intent)
    }
}

class SeekAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val next = parameters[PARAMETER_NEXT] ?: false
        val intent = Intent(context, GlanceWidgetReceiver::class.java).apply {
            action =
                if (next) GlanceWidgetReceiver.ACTION_SEEK_NEXT else GlanceWidgetReceiver.ACTION_SEEK_PREVIOUS
        }
        context.sendBroadcast(intent)
    }

    companion object {
        val PARAMETER_NEXT = ActionParameters.Key<Boolean>("next")
    }
}


@OptIn(ExperimentalGlancePreviewApi::class)
@Preview
@Composable
private fun NowPlayingWidgetPreview() {
    GlancePreview(sizes = listOf(4 to 1, 5 to 1)) {
        NowPlayingWidget(
            state = NowPlayingTrack(
                isPlaying = false,
                info = TrackInfo(
                    id = 1,
                    title = "Song Title",
                    coverUrl = "https://picsum.photos/400"
                )
            ),
        )
    }
}
