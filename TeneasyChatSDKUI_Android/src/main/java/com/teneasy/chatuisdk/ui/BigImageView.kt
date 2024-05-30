package com.teneasy.chatuisdk.ui

import android.content.Context
import androidx.annotation.OptIn
import androidx.appcompat.widget.AppCompatImageView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.LegacyPlayerControlView
import androidx.media3.ui.PlayerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.lxj.xpopup.core.CenterPopupView
import com.teneasy.chatuisdk.R


class BigImageView(context: Context, url: String): CenterPopupView (context){

    private var url: String? = url

    override fun getImplLayoutId(): Int {
        return R.layout.fragment_image_full
    }

    override fun onCreate() {
        super.onCreate()

        val ivBig = findViewById<AppCompatImageView>(R.id.ivBig)
                        Glide.with(context).load(url).dontAnimate()
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .into(ivBig)
    }

}