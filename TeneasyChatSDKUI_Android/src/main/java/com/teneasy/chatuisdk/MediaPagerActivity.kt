package com.teneasy.chatuisdk

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.annotation.OptIn
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.github.chrisbanes.photoview.PhotoView
import com.teneasy.chatuisdk.databinding.ActivityMediaPagerBinding
import com.teneasy.chatuisdk.ui.base.ChatMediaItem

/**
 * 会话媒体浏览器：图片+视频混排左右滑浏览 + 下拉关闭（对齐 Flutter MediaPagerView）。
 *
 * 媒体列表通过 [mediaItemsProvider] 在打开时收集（对齐 Flutter
 * `widget.items ?? ChatPage.currentMediaItems()` 的静态获取方式），
 * 点击的缩略图经 [EXTRA_START_URL] 定位初始页；列表为空时回退为单项浏览。
 */
class MediaPagerActivity : FragmentActivity() {

    companion object {
        const val EXTRA_START_URL = "media_pager_start_url"
        const val EXTRA_START_IS_VIDEO = "media_pager_start_is_video"

        /** 由聊天页注册的会话媒体收集器（对齐 Flutter ChatPage.currentMediaItems） */
        var mediaItemsProvider: (() -> List<ChatMediaItem>)? = null

        fun start(context: Context, startUrl: String, isVideo: Boolean = false) {
            val intent = Intent(context, MediaPagerActivity::class.java)
            intent.putExtra(EXTRA_START_URL, ChatMediaItem.absUrl(startUrl))
            intent.putExtra(EXTRA_START_IS_VIDEO, isVideo)
            context.startActivity(intent)
        }
    }

    private var binding: ActivityMediaPagerBinding? = null
    private var items: List<ChatMediaItem> = emptyList()
    private var currentIndex = 0
    private lateinit var pagerAdapter: MediaPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val b = ActivityMediaPagerBinding.inflate(layoutInflater)
        binding = b
        setContentView(b.root)

        val startUrl = intent.getStringExtra(EXTRA_START_URL) ?: ""
        val startIsVideo = intent.getBooleanExtra(EXTRA_START_IS_VIDEO, false)

        // 解析媒体列表与初始下标（对齐 Flutter _MediaPagerViewState.initState）
        val resolved = mediaItemsProvider?.invoke() ?: emptyList()
        var startIndex = 0
        val matched = resolved.indexOfFirst { it.url == startUrl }
        if (matched >= 0) startIndex = matched
        items = if (resolved.isEmpty() && startUrl.isNotEmpty()) {
            listOf(ChatMediaItem(startUrl, startIsVideo))
        } else {
            resolved
        }
        if (startIndex >= items.size) startIndex = items.size - 1
        if (startIndex < 0) startIndex = 0
        currentIndex = startIndex

        b.ivBack.setColorFilter(Color.WHITE)
        b.ivBack.setOnClickListener { finish() }
        b.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE

        pagerAdapter = MediaPagerAdapter()
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        b.rvPager.layoutManager = layoutManager
        b.rvPager.adapter = pagerAdapter
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(b.rvPager)
        layoutManager.scrollToPosition(startIndex)
        updateCounter()

        // 翻页结束更新计数 + 只播当前页视频（对齐 Flutter onPageChanged + _syncPlayback）
        b.rvPager.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState != RecyclerView.SCROLL_STATE_IDLE) return
                val snapped = snapHelper.findSnapView(layoutManager) ?: return
                val pos = layoutManager.getPosition(snapped)
                if (pos != RecyclerView.NO_POSITION && pos != currentIndex) {
                    currentIndex = pos
                    updateCounter()
                    syncPlayback()
                }
            }
        })

        // 下拉关闭 + 背景淡出（对齐 Flutter _onVerticalDragUpdate/_onVerticalDragEnd）
        b.dragRoot.onDismiss = {
            finish()
            overridePendingTransition(0, 0)
        }
        b.dragRoot.onDragFraction = { fraction ->
            b.dragRoot.background?.mutate()?.alpha = ((1f - fraction) * 255).toInt()
        }
    }

    private fun updateCounter() {
        binding?.tvCounter?.text =
            if (items.isEmpty()) "" else "${currentIndex + 1} / ${items.size}"
    }

    private fun syncPlayback() {
        val rv = binding?.rvPager ?: return
        for (i in 0 until rv.childCount) {
            val holder = rv.getChildViewHolder(rv.getChildAt(i))
            if (holder is VideoPageHolder) {
                if (holder.bindingAdapterPosition == currentIndex) {
                    holder.player?.play()
                } else {
                    holder.player?.pause()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        forEachVideoHolder { it.player?.pause() }
    }

    override fun onDestroy() {
        super.onDestroy()
        forEachVideoHolder { it.releasePlayer() }
        // 置空 adapter 强制回收 RecyclerView 离屏缓存（mCachedViews）里的页，
        // 触发 onViewRecycled → releasePlayer，否则缓存页的 ExoPlayer 随 Activity 泄漏
        binding?.rvPager?.adapter = null
        binding = null
    }

    private fun forEachVideoHolder(action: (VideoPageHolder) -> Unit) {
        val rv = binding?.rvPager ?: return
        for (i in 0 until rv.childCount) {
            val holder = rv.getChildViewHolder(rv.getChildAt(i))
            if (holder is VideoPageHolder) action(holder)
        }
    }

    // ============================== 页面 Adapter ==============================

    private inner class MediaPagerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val TYPE_IMAGE = 0
        private val TYPE_VIDEO = 1

        override fun getItemViewType(position: Int) =
            if (items[position].isVideo) TYPE_VIDEO else TYPE_IMAGE

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == TYPE_VIDEO) {
                VideoPageHolder(inflater.inflate(R.layout.item_media_page_video, parent, false))
            } else {
                ImagePageHolder(inflater.inflate(R.layout.item_media_page_image, parent, false))
            }
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = items[position]
            when (holder) {
                is ImagePageHolder -> holder.bind(item)
                is VideoPageHolder -> holder.bind(item, position == currentIndex)
            }
        }

        override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
            if (holder is VideoPageHolder) holder.releasePlayer()
        }
    }

    /** 图片页：PhotoView 可缩放，点击关闭（对齐 Flutter _ImagePage onTap pop） */
    private inner class ImagePageHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val photoView: PhotoView = view.findViewById(R.id.iv_image)
        private val progress: ProgressBar = view.findViewById(R.id.progress)

        fun bind(item: ChatMediaItem) {
            progress.visibility = View.VISIBLE
            Glide.with(photoView).load(item.url)
                .dontAnimate()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: com.bumptech.glide.load.engine.GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        progress.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable,
                        model: Any,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        progress.visibility = View.GONE
                        return false
                    }
                })
                .into(photoView)
            photoView.setOnClickListener { finish() }
        }
    }

    /** 视频页：按页创建 ExoPlayer，仅当前页自动播放、循环（对齐 Flutter _VideoPage） */
    private inner class VideoPageHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val playerView: PlayerView = view.findViewById(R.id.player_view)
        private val progress: ProgressBar = view.findViewById(R.id.progress)
        var player: ExoPlayer? = null

        @OptIn(UnstableApi::class)
        fun bind(item: ChatMediaItem, isActive: Boolean) {
            releasePlayer()
            progress.visibility = View.VISIBLE
            val newPlayer = ExoPlayer.Builder(this@MediaPagerActivity).build()
            val mediaItem = MediaItem.fromUri(item.url)
            val dataSourceFactory = DefaultHttpDataSource.Factory()
            // m3u8 用 HLS，其余走 Progressive（与 FullVideoActivity 同逻辑）
            val source = if (item.url.endsWith("m3u8")) {
                HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            } else {
                ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            }
            newPlayer.setMediaSource(source)
            newPlayer.repeatMode = Player.REPEAT_MODE_ONE
            newPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        progress.visibility = View.GONE
                    }
                }
            })
            newPlayer.prepare()
            newPlayer.playWhenReady = isActive
            playerView.player = newPlayer
            playerView.setShowPreviousButton(false)
            playerView.setShowNextButton(false)
            player = newPlayer
        }

        fun releasePlayer() {
            playerView.player = null
            player?.release()
            player = null
        }
    }
}
