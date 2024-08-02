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
import androidx.media3.ui.PlayerView
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.teneasy.chatuisdk.databinding.FragmentImageFullBinding
import com.teneasy.chatuisdk.databinding.FragmentSettingsBinding
import com.teneasy.chatuisdk.databinding.FragmentVideoBinding

 //const val ARG_IMAGEURL = "IMAGEURL"

class FullImageActivity : FragmentActivity() {
    private var imageUrl: String? = ""
    private var kefuName: String? = ""
    var binding: FragmentImageFullBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
            imageUrl = intent.getStringExtra(ARG_IMAGEURL)
            kefuName = intent.getStringExtra(ARG_KEFUNAME)

        binding = FragmentImageFullBinding.inflate(layoutInflater)
        setContentView(binding?.root)


        binding?.let {
            it.tvTitle.text = kefuName
            it.ivBack.setOnClickListener {
                finish()
            }
            Glide.with(this).load(imageUrl).dontAnimate()
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(it.ivImage)
        }
    }

}