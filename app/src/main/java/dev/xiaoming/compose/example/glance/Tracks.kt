package dev.xiaoming.compose.example.glance

import kotlinx.serialization.Serializable

@Serializable
data class TrackInfo(val id: Int, val title: String, val coverUrl: String)

object Tracks {
    val tracks = List(10) { index ->
        TrackInfo(
            id = index,
            title = "Song Title ${index + 1}",
            coverUrl = "https://picsum.photos/id/${120 + index}/200/300"
        )
    }
}