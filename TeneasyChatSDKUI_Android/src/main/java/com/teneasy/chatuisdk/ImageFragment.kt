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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.teneasy.chatuisdk.databinding.FragmentImageFullBinding
import com.teneasy.chatuisdk.databinding.FragmentSettingsBinding
import com.teneasy.chatuisdk.databinding.FragmentVideoBinding

 const val ARG_IMAGEURL = "IMAGEURL"

class ImageFragment : Fragment() {
    private var imageUrl: String? = ""
    private var kefuName: String? = ""
    var binding: FragmentImageFullBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            imageUrl = it.getString(ARG_IMAGEURL)
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
        binding = FragmentImageFullBinding.inflate(inflater, container, false)

        binding?.let {
            it.tvTitle.text = kefuName
            it.ivBack.setOnClickListener {
                findNavController().popBackStack()
            }
            Glide.with(requireContext()).load(imageUrl).dontAnimate()
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(it.ivImage)
        }


        return binding?.root
    }

    companion object {
    }
}