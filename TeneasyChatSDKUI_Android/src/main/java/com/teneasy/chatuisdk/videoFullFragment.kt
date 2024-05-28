package com.teneasy.chatuisdk

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.findNavController
import com.teneasy.chatuisdk.databinding.FragmentVideoFullBinding
import com.teneasy.chatuisdk.ui.base.Constants


private const val ARG_PARAM1 = "param1"
 const val ARG_VIDEOURL = "VideoUrl"

/**
 * A simple [Fragment] subclass.
 * Use the [videoFullFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class videoFullFragment : Fragment() {
    private var videoUrl: String? = null
    lateinit var binding: FragmentVideoFullBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            videoUrl = it.getString(ARG_VIDEOURL)
        }

        //硬返回按钮点点击之后
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            //返回到上个页面
            findNavController().popBackStack()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentVideoFullBinding.inflate(inflater, container, false)
        val mediaItem = MediaItem.Builder().setMediaId("ddd").setTag(991).setUri(videoUrl).build()
        val player = ExoPlayer.Builder(requireContext()).build()
        binding.playerView.player = player
        player.setMediaItem(mediaItem)
        // Prepare the player.
        player.prepare()
        // Start the playback.
        player.play()
        return binding.root
    }

    /*companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment videoFullFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            videoFullFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }*/
}