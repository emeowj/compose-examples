package dev.xiaoming.compose.example.glance

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class GlanceWidgetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        when(action) {
            ACTION_SEEK_NEXT -> NowPlayingState.seekNext()
            ACTION_SEEK_PREVIOUS -> NowPlayingState.seekPrevious()
            ACTION_PLAY_PAUSE -> NowPlayingState.togglePlaying()
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "dev.xiaoming.compose.example.ACTION_PLAY_PAUSE"
        const val ACTION_SEEK_NEXT = "dev.xiaoming.compose.example.ACTION_SEEK_NEXT"
        const val ACTION_SEEK_PREVIOUS = "dev.xiaoming.compose.example.ACTION_SEEK_PREVIOUS"
    }
}