package com.teneasy.chatuisdk.ui

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.LegacyPlayerControlView
import androidx.media3.ui.PlayerView
import com.lxj.xpopup.core.CenterPopupView
import com.teneasy.chatuisdk.R


class VideoPlayView(context: Context, videoUrl: String): CenterPopupView (context){

    private var videoUrl: String? = videoUrl
    //lateinit var binding: FragmentVideoFullBinding

    override fun getImplLayoutId(): Int {
        return R.layout.fragment_video_full
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        print(videoUrl)
        Log.d("chatlibVideo", "onCreate: $videoUrl")
        val playerView = findViewById<PlayerView>(R.id.player_view)
        val mediaItem = MediaItem.Builder().setMediaId("ddd").setTag(991).setUri(videoUrl).build()
        val player = ExoPlayer.Builder(context).build()
        //binding.playerView.player = player
        playerView.player = player
        player.setMediaItem(mediaItem)
        playerView.setShowPreviousButton(false)
        playerView.setShowNextButton(false)
        playerView.controllerHideOnTouch = true

        player.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        // Active playback.
                        playerView.hideController()
                    } else {
                        // Not playing because playback is paused, ended, suppressed, or the player
                        // is buffering, stopped or failed. Check player.playWhenReady,
                        // player.playbackState, player.playbackSuppressionReason and
                        // player.playerError for details.
                        playerView.showController()
                    }
                }
            }
        )

        //playerView.useController = false
        // Prepare the player.
        player.prepare()
        // Start the playback.
        player.play()
    }

}