package com.teneasy.chatuisdk

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.fragment.findNavController
import com.teneasy.chatuisdk.databinding.FragmentSettingsBinding
import com.teneasy.chatuisdk.databinding.FragmentVideoBinding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
 const val ARG_VIDEOURL = "videoUrl"
const val ARG_KEFUNAME = "KEFUNAME"
/**
 * A simple [Fragment] subclass.
 * Use the [VideoFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class VideoFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var videoUrl: String? = ""
    private var kefuName: String? = ""
    var binding: FragmentVideoBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            videoUrl = it.getString(ARG_VIDEOURL)
            kefuName = it.getString(ARG_KEFUNAME)
        }

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            findNavController().popBackStack()
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentVideoBinding.inflate(inflater, container, false)
        binding?.tvTitle?.text = kefuName
        binding?.ivBack?.setOnClickListener {
            findNavController().popBackStack()
        }
        binding?.playerView?.let {
            val mediaItem = MediaItem.Builder().setMediaId("ddd").setTag(991).setUri(videoUrl).build()
            val player = ExoPlayer.Builder(requireContext()).build()

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

        return binding?.root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment VideoFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(videoUrl: String) =
            VideoFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_VIDEOURL, videoUrl)
                }
            }
    }
}