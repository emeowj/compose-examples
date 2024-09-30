package dev.xiaoming.compose.example.glance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.xiaoming.compose.example.ExamplePreview
import dev.xiaoming.compose.example.R
import dev.xiaoming.compose.example.glance.widget.NowPlayingTrack
import dev.xiaoming.compose.example.glance.widget.NowPlayingWidget
import dev.xiaoming.compose.example.ui.theme.Padding


@Composable
fun GlanceWidget() {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items = Tracks.tracks) { track ->
            HorizontalDivider()
            ListItem(
                headlineContent = {
                    Row(
                        modifier = Modifier.padding(Padding.medium),
                        horizontalArrangement = Arrangement.spacedBy(Padding.medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NetworkImage(
                            src = track.coverUrl, modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(Padding.medium))
                        )
                        Text(text = track.title, modifier = Modifier.weight(1f))
                    }
                },
                trailingContent = {
                    FilledTonalIconButton(
                        onClick = {
                            val isPlaying =
                                if (track == NowPlayingState.currentPlaying) !NowPlayingState.isPlaying else true
                            NowPlayingState.updateNowPlaying(track = track, isPlaying = isPlaying)
                        }
                    ) {
                        val icon =
                            if (track == NowPlayingState.currentPlaying && NowPlayingState.isPlaying) {
                                R.drawable.ic_pause
                            } else {
                                R.drawable.ic_play
                            }
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = null
                        )
                    }
                }
            )
        }
    }

    val context = LocalContext.current
    LaunchedEffect(NowPlayingState.currentPlaying, NowPlayingState.isPlaying) {
        val manager = GlanceAppWidgetManager(context)
        val widget = NowPlayingWidget()
        manager.getGlanceIds(NowPlayingWidget::class.java).forEach { glanceId ->
            updateAppWidgetState(
                context = context,
                definition = widget.stateDefinition,
                glanceId = glanceId
            ) {
                NowPlayingTrack(
                    isPlaying = NowPlayingState.isPlaying,
                    info = NowPlayingState.currentPlaying
                )
            }
        }
        widget.updateAll(context = context)
    }
}

@Composable
private fun NetworkImage(src: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(src)
            .apply {
                if (isPreview) {
                    placeholder(R.drawable.image_placeholder)
                }
            }
            .build(),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}


@Composable
@Preview
private fun GlanceWidgetPreview() {
    ExamplePreview {
        GlanceWidget()
    }
}