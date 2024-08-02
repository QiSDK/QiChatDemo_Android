package com.teneasy.chatuisdk

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.annotation.OptIn
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.teneasy.chatuisdk.databinding.FragmentVideoBinding

 //const val ARG_IMAGEURL = "IMAGEURL"

class FullVideoActivity : FragmentActivity() {
    private var videoUrl: String? = ""
    private var kefuName: String? = ""
    var binding: FragmentVideoBinding? = null

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        videoUrl = intent.getStringExtra(ARG_VIDEOURL)
            kefuName = intent.getStringExtra(ARG_KEFUNAME)

        binding = FragmentVideoBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        // Inflate the layout for this fragment
        binding?.tvTitle?.text = kefuName
        binding?.ivBack?.setOnClickListener {
           finish()
        }
        binding?.playerView?.let {
            val mediaItem = MediaItem.Builder().setMediaId("ddd").setTag(991).setUri(videoUrl).build()
            val player = ExoPlayer.Builder(this).build()

            it.player = player
            player.setMediaItem(mediaItem)
            it.setShowPreviousButton(false)
            it.setShowNextButton(false)
            it.controllerHideOnTouch = true

            player.addListener(
                object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (isPlaying) {
                            // Active playback.
                            it.hideController()
                        } else {
                            // Not playing because playback is paused, ended, suppressed, or the player
                            // is buffering, stopped or failed. Check player.playWhenReady,
                            // player.playbackState, player.playbackSuppressionReason and
                            // player.playerError for details.
                            it.showController()
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

    override fun onPause() {
        super.onPause()

        binding?.playerView?.let {
            val player = it.player
            player?.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}