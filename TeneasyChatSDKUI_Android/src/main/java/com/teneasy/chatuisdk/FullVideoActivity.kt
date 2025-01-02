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
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
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
            val player = ExoPlayer.Builder(this).build()

// Prepare the media item
            val mediaItem = MediaItem.Builder().setMediaId("ddd").setTag(991).setUri(videoUrl).build()

// Create a data source factory
            val dataSourceFactory = DefaultHttpDataSource.Factory()

// Check the URL extension to determine the correct media source
            if (videoUrl!!.endsWith("m3u8")) {
                // Use HLS Media Source for .m3u8 URLs (HLS streams)
                val hlsMediaSource = HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
                player.setMediaSource(hlsMediaSource)
            } // Check the URL extension to determine the correct media source
            else {
                // Use Progressive Media Source for MP4 files
                val progressiveMediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
                player.setMediaSource(progressiveMediaSource)
            }
//            else {
//                val cacheDataSourceFactory: DataSource.Factory =
//                CacheDataSource.Factory()
//                  //  .setCache(simpleCache)
//                    .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory())
//                // Use Dash Media Source for other video formats
//                val dashMediaSource = DashMediaSource.Factory(cacheDataSourceFactory)
//                    .createMediaSource(mediaItem)
//                player.setMediaSource(dashMediaSource)
//            }

// Prepare the player with the media source and start playback
            player.prepare()
            player.play()

            // Start playing the video
            player.playWhenReady = true
            it.player = player
            //player.setMediaItem(mediaItem)
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