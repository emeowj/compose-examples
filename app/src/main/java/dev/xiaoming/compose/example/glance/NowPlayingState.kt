package dev.xiaoming.compose.example.glance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// In real application this would be persisted somewhere instead of just being in memory.
object NowPlayingState {
    var currentPlaying by mutableStateOf<TrackInfo?>(null)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    fun updateNowPlaying(track: TrackInfo, isPlaying: Boolean) {
        this.currentPlaying = track
        this.isPlaying = isPlaying
    }

    fun togglePlaying() {
        this.isPlaying = !this.isPlaying
        if (currentPlaying == null) {
            currentPlaying = Tracks.tracks.first()
        }
    }

    fun seekNext() {
        val track = if (currentPlaying == null) {
            Tracks.tracks.first()
        } else {
            val index = Tracks.tracks.indexOf(currentPlaying)
            Tracks.tracks[(index + 1) % Tracks.tracks.size]
        }
        currentPlaying = track
    }

    fun seekPrevious() {
        val track = if (currentPlaying == null) {
            Tracks.tracks.last()
        } else {
            val index = Tracks.tracks.indexOf(currentPlaying)
            Tracks.tracks[(index - 1 + Tracks.tracks.size) % Tracks.tracks.size]
        }
        currentPlaying = track
    }
}