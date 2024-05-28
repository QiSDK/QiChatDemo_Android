package com.teneasy.chatuisdk.ui

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.lxj.xpopup.core.CenterPopupView
import com.teneasy.chatuisdk.R
import com.teneasy.chatuisdk.databinding.FragmentVideoFullBinding
import com.teneasy.chatuisdk.ui.base.ApplicationExt.Companion.context

class VideoPlayView(context: Context, videoUrl: String): CenterPopupView (context){

    private var videoUrl: String? = videoUrl
    //lateinit var binding: FragmentVideoFullBinding

    override fun getImplLayoutId(): Int {
        return R.layout.fragment_video_full
    }

    override fun onCreate() {
        super.onCreate()

        val playerView = findViewById<PlayerView>(R.id.player_view)
        val mediaItem = MediaItem.Builder().setMediaId("ddd").setTag(991).setUri(videoUrl).build()
        val player = ExoPlayer.Builder(context).build()
        //binding.playerView.player = player
        playerView.player = player
        player.setMediaItem(mediaItem)
        // Prepare the player.
        player.prepare()
        // Start the playback.
        player.play()
    }

}