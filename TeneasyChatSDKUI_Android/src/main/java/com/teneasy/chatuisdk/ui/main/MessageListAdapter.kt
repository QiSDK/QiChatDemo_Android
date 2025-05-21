package com.teneasy.chatuisdk.ui.main;

//import com.teneasy.chatuisdk.ui.utils.emoji.EmoticonTextView

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.gson.Gson
import com.luck.picture.lib.utils.ToastUtils
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.interfaces.OnSelectListener
import com.teneasy.chatuisdk.R
import com.teneasy.chatuisdk.databinding.ItemFileMessageBinding
import com.teneasy.chatuisdk.databinding.ItemLastLineBinding
import com.teneasy.chatuisdk.databinding.ItemQaListBinding
import com.teneasy.chatuisdk.databinding.ItemTextImagesMessageBinding
import com.teneasy.chatuisdk.databinding.ItemTextMessageBinding
import com.teneasy.chatuisdk.databinding.ItemTipMsgBinding
import com.teneasy.chatuisdk.databinding.ItemVideoImageMessageBinding
import com.teneasy.chatuisdk.ui.base.Constants
import com.teneasy.chatuisdk.ui.base.Constants.Companion.withAutoReplyU
import com.teneasy.chatuisdk.ui.base.Utils
import com.teneasy.chatuisdk.ui.http.bean.AutoReplyItem
import com.teneasy.chatuisdk.ui.http.bean.QA
import com.teneasy.chatuisdk.ui.http.bean.TextBody
import com.teneasy.chatuisdk.ui.http.bean.TextImages
import com.teneasy.sdk.ui.CellType
import com.teneasy.sdk.ui.MessageItem
import com.teneasy.sdk.ui.MessageSendState
import com.teneasyChat.api.common.CMessage


interface MessageItemOperateListener {
    fun onDelete(position: Int)
    fun onCopy(position: Int)
    fun onReSend(position: Int)
    fun onQuote(position: Int)
    fun onSendLocalMsg(msg: String, isLeft: Boolean, msgType: String = "MSG_TEXT")
    fun onPlayVideo(url: String)
    fun onPlayImage(url: String)
    fun onDownload(position: Int)
    fun onShowOriginal(position: Int)
    fun onOpenFile(url: String)
}

data class QADisplayedEvent(val tag: Int)
/**
 * 聊天界面列表adapter
 */
class MessageListAdapter (myContext: Activity,  listener: MessageItemOperateListener?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var msgList: ArrayList<MessageItem>? = null
    val act: Activity = myContext
    private var listener: MessageItemOperateListener? = listener
    private var autoReplyItem: AutoReplyItem? = null
//    fun getList(): ArrayList<MessageItem>? {
//        return msgList
//    }

    fun setList(list: ArrayList<MessageItem>?) {
        this.msgList = list//ArrayList(list)
    }

    fun setAutoReply(auto: AutoReplyItem) {
        this.autoReplyItem = auto
        //notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == CellType.TYPE_QA.value) {
            val binding = ItemQaListBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false)
            return QAViewHolder(binding)
        } else  if (viewType == CellType.TYPE_Tip.value) {
            val binding = ItemTipMsgBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false)
            return TipMsgViewHolder(binding)
        }else  if (viewType == CellType.TYPE_Image.value || viewType == CellType.TYPE_VIDEO.value) {
            val binding = ItemVideoImageMessageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ItemVideoViewHolder(binding)
        }else  if (viewType == CellType.TYPE_LastLine.value) {
            val binding = ItemLastLineBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false)
            return ItemLastLineViewHolder(binding)
        }else if (viewType == CellType.TYPE_File.value) {
            val binding = ItemFileMessageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false)
            return ItemFileViewHolder(binding)
        }else if (viewType == CellType.TYPE_Text_Images.value) {
            val binding = ItemTextImagesMessageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false)
            return TextImagesViewHolder(binding)
        }else {

            val binding = ItemTextMessageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false)
            return TextMsgViewHolder(binding)
        }
    }

    @OptIn(UnstableApi::class)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (msgList == null) {
            return
        }
        if (holder is ItemLastLineViewHolder) {
            holder.tvTitle.text = ""
        }
        else if (holder is QAViewHolder) {
            if (autoReplyItem != null){
                holder.tvTitle.text = autoReplyItem?.title
                autoReplyItem?.qa?.let {
                    holder.qaAdapter.setDataList(it)
                    //holder.qaAdapter.expandGroup(0)
                    holder.tvTitle.visibility = View.VISIBLE

                    val localTime = Utils().timestampToDate(System.currentTimeMillis() + 700)
                    holder.tvLeftTime.text = localTime
                }
            }

            //客服头像
            val url = Constants.baseUrlImage + Constants.workerAvatar
            print("avatar:$url")
            Glide.with(act).load(url).dontAnimate()
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(holder.ivKefuImage)
        }
        else if (holder is ItemFileViewHolder) {
            val item = msgList!![position]
            if (item.cMsg == null) {
                return
            }
            var localTime = "Time error"
            item.cMsg?.let {
                localTime = Utils().timestampToString(it.msgTime)
            }
            if (!item.isLeft) {
                holder.tvRightTime.text = localTime
                holder.llLeftContent.visibility = View.GONE
                holder.llRightContent.visibility = View.VISIBLE

                if (item.sendStatus != MessageSendState.发送成功) {
                    holder.ivSendStatus.visibility = View.VISIBLE
                } else
                    holder.ivSendStatus.visibility = View.GONE

                holder.tvLeftTime.visibility = View.GONE
                holder.tvRightTime.visibility = View.VISIBLE

                holder.ivRightImg.visibility = View.VISIBLE
                val ext = item.cMsg?.file?.uri?.split(".")?.last()
                holder.ivRightImg.setImageResource(getFileThumbnail(ext?:"#"))
                holder.ivRightImg.scaleType = ImageView.ScaleType.CENTER_CROP
                holder.tvRightFileSize.text = Utils().formatSize(item.cMsg?.file?.size?: 0)
                holder.tvRightFileName.text = item.cMsg!!.file.fileName

                var meidaUrl = Constants.baseUrlImage + item.cMsg!!.file.uri
                holder.llRightContent.tag = position
                holder.llRightContent.setOnClickListener {
                    listener?.onOpenFile(meidaUrl)
                }
            } else {
                holder.tvLeftTime.text = localTime

                holder.llLeftContent.visibility = View.VISIBLE
                holder.tvLeftTime.visibility = View.VISIBLE
                holder.tvRightTime.visibility = View.GONE
                holder.llRightContent.visibility = View.GONE

                val ext = item.cMsg?.file?.uri?.split(".")?.last()
                holder.ivLeftImg.setImageResource(getFileThumbnail(ext?:"#"))
                holder.ivLeftImg.scaleType = ImageView.ScaleType.CENTER_CROP

                //((item.replyItem?.size?:0)  * 0.001).toString() + "K"
                holder.tvLeftFileSize.text = Utils().formatSize(item.cMsg?.file?.size?: 0)
                holder.tvLeftFileName.text = item.cMsg!!.file.fileName

                var meidaUrl = Constants.baseUrlImage + item.cMsg!!.file.uri
                holder.llLeftContent.tag = position
                holder.llLeftContent.setOnClickListener {
                    listener?.onOpenFile(meidaUrl)
                }
                //客服头像
                val url = Constants.baseUrlImage + Constants.workerAvatar
                print("avatar:$url")
                Glide.with(act).load(url).dontAnimate()
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .into(holder.civKefuImage)
            }
        }
       else if (holder is ItemVideoViewHolder) {
            val item = msgList!![position]
            if (item.cMsg == null) {
                return
            }

            var localTime = "Time error"

            item.cMsg?.let {
                // val msgDate = Date(it.msgTime.seconds * 1000L)
                //localTime = TimeUtil.getTimeString(msgDate, localDateFormat)
                localTime = Utils().timestampToString(it.msgTime)
            }
            if (!item.isLeft) {
                holder.ivRightImg.tag = position
                holder.tvRightTime.text = localTime
                holder.tvRightTime.visibility = View.VISIBLE
                holder.lySend.visibility = View.VISIBLE
                holder.civKefuRightImage.visibility = View.VISIBLE
                holder.ivRightChatarrow.visibility = View.VISIBLE
                holder.ivRightPlay.visibility = View.VISIBLE

                holder.ivKefuImage.visibility = View.GONE
                holder.ivArrow.visibility = View.GONE
                holder.tvLeftTime.visibility = View.GONE
                holder.ivLeftImg.visibility = View.GONE
                holder.ivPlay.visibility = View.GONE

                if (item.sendStatus != MessageSendState.发送成功) {
                    holder.ivSendStatus.visibility = View.VISIBLE
                } else
                    holder.ivSendStatus.visibility = View.GONE

                holder.ivRightImg.visibility = View.VISIBLE
                var meidaUrl = Constants.baseUrlImage + item.cMsg!!.video.uri

                if (item.cMsg!!.video.thumbnailUri.isNotEmpty()) {
                    meidaUrl = Constants.baseUrlImage + item.cMsg!!.video.thumbnailUri
                } else if (item.cMsg!!.image.uri.isNotEmpty()) {
                    meidaUrl = Constants.baseUrlImage + item.cMsg!!.image.uri
                    holder.ivRightPlay.visibility = View.GONE
                }

                holder.ivRightImg.setOnClickListener {
                    var tag = holder.ivRightImg.tag as Int
                    val myItem = msgList!![tag]

                    if ((myItem.cMsg?.video?.uri?:"").isNotEmpty()) {
                        var videoUrl = item.cMsg?.video!!.uri
                        if (item.cMsg?.video!!.hlsUri.isNotEmpty()){
                            videoUrl = item.cMsg?.video!!.hlsUri
                        }
                        listener?.onPlayVideo(Constants.baseUrlImage + videoUrl)
                    }else {
                        listener?.onPlayImage(meidaUrl)
                    }
                }

                var thumb = meidaUrl
                Glide.with(act)
                    .load(thumb)
                    .apply(
                        RequestOptions()
                            .placeholder(com.teneasy.chatuisdk.R.drawable.loading_animation)
                            .dontAnimate().skipMemoryCache(true)
                    )
                    .listener(object : RequestListener<Drawable?> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable?>,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable?>?,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            val bitmap = Utils().drawableToBitmap(resource);
                            print(bitmap.width)
                            if (bitmap.height > bitmap.width) {
                                Utils().updateLayoutParams(
                                    holder.rlImagecontainer,
                                    Utils().dp2px(106.0f),
                                    Utils().dp2px(176.0f)
                                )
                            } else {
                                Utils().updateLayoutParams(
                                    holder.rlImagecontainer,
                                    Utils().dp2px(176.0f),
                                    Utils().dp2px(106.0f)
                                )
                            }
                            return false
                        }
                    })
                    .into(holder.ivRightImg)
            } else {
                holder.ivLeftImg.tag = position
                holder.tvLeftTime.text = localTime
                holder.tvLeftTime.visibility = View.VISIBLE
                holder.ivKefuImage.visibility = View.VISIBLE
                holder.ivArrow.visibility = View.VISIBLE
                holder.ivPlay.visibility = View.VISIBLE

                holder.ivRightChatarrow.visibility = View.GONE
                holder.tvRightTime.visibility = View.GONE
                holder.ivRightImg.visibility = View.GONE
                holder.lySend.visibility = View.GONE
                holder.civKefuRightImage.visibility = View.GONE
                holder.ivPlay.visibility = View.GONE

                holder.ivLeftImg.visibility = View.VISIBLE
                var meidaUrl = Constants.baseUrlImage + item.cMsg!!.video.uri

                if (item.cMsg!!.video.thumbnailUri.isNotEmpty()) {
                    meidaUrl = Constants.baseUrlImage + item.cMsg!!.video.thumbnailUri
                    holder.ivPlay.visibility = View.VISIBLE
                } else if (item.cMsg!!.image.uri.isNotEmpty()) {
                    meidaUrl = Constants.baseUrlImage + item.cMsg!!.image.uri
                }

                holder.ivLeftImg.setOnClickListener {
                    var tag = holder.ivLeftImg.tag as Int
                    val myItem = msgList!![tag]

                    if ((myItem.cMsg?.video?.uri?:"").isNotEmpty()) {
                        var videoUrl = item.cMsg?.video!!.uri
                        if (item.cMsg?.video!!.hlsUri.isNotEmpty()){
                            videoUrl = item.cMsg?.video!!.hlsUri
                        }
                        listener?.onPlayVideo(Constants.baseUrlImage + videoUrl)
                    }else {
                        listener?.onPlayImage(meidaUrl)
                    }
                }

                if (item.cMsg!!.video.thumbnailUri.isNotEmpty()) {
                    meidaUrl = Constants.baseUrlImage + item.cMsg!!.video.thumbnailUri
                }
                var thumb = meidaUrl
                Glide.with(act)
                    .load(thumb)
                    .apply(
                        RequestOptions()
                            .placeholder(R.drawable.loading_animation)
                            .dontAnimate().skipMemoryCache(true)
                    )
                    .listener(object : RequestListener<Drawable?> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable?>,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable?>?,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            val bitmap = Utils().drawableToBitmap(resource);
                            print(bitmap.width)
                            if (bitmap.height > bitmap.width) {
                                Utils().updateLayoutParams(
                                    holder.rlLeftImagecontainer,
                                    Utils().dp2px(106.0f),
                                    Utils().dp2px(176.0f)
                                )
                            } else {
                                Utils().updateLayoutParams(
                                    holder.rlLeftImagecontainer,
                                    Utils().dp2px(176.0f),
                                    Utils().dp2px(106.0f)
                                )
                            }
                            return false
                        }
                    })
                    .into(holder.ivLeftImg)

                //客服头像
                val url = Constants.baseUrlImage + Constants.workerAvatar
                print("avatar:$url")
                Glide.with(holder.ivKefuImage).load(url).dontAnimate()
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .into(holder.ivKefuImage)
            }
        }
        else if (holder is TextImagesViewHolder) {
            val item = msgList!![position]
            var textImages = item.cMsg?.content?.data?: ""
            //textImages = "{\"message\":\"您要看什么图片，不会是瑟瑟的图吧！这我不会发给你的。\",\"imgs\":[\"/public/tenant_298/20250517/Images/9e3b8aa1-f612-4fb2-b6a4-66cf9e25341c/小鱼儿和花无缺.jpeg\",\"/public/tenant_298/20250517/Images/c4ceb97d-d0cd-4c6f-8ce4-a2c11c784e40/客服1.jpg\",\"/public/tenant_298/20250517/Images/615c605d-e9fd-49b3-9fd2-3d8f005dfd07/客服2.jpeg\",\"/public/tenant_298/20250517/Images/4597dfae-57c2-4289-a8da-d89dfa5dd7fe/客服3.jpeg\",\"/public/tenant_298/20250517/Images/098871a8-75f2-48af-a1e6-ea85cbcb720b/客服4.jpeg\",\"/public/tenant_298/20250517/Images/e5e6508b-f3d5-4b31-afc5-100505ab207e/客服5.jpeg\",\"/public/tenant_298/20250517/Images/452b2b36-ad7f-4ed6-99ce-b0ea052292d9/客服6.jpeg\",\"/public/tenant_298/20250517/Images/f47ca62d-1a6a-4cc4-8b30-33e6998b2731/老板1.png\",\"/public/tenant_298/20250517/Images/7ffc3d11-49d0-4ed6-ae12-239363f1d559/老板2.jpeg\"]}"
            val gson = Gson()
            try {
                holder.llTextImages.tag = position
                val textBody: TextImages = gson.fromJson(textImages, TextImages::class.java)
                val adapter = ImageAdapter(textBody.imgs, act, object : ImageOnListener {
                    override fun onQuote(position: Int) {
                        var pos = holder.llTextImages.tag?:0
                        listener?.onQuote(pos as Int)
                    }
                })
                holder.rvList.adapter = adapter
                val layoutManager = GridLayoutManager(act, 2)
                layoutManager.orientation = LinearLayoutManager.HORIZONTAL
                holder.rvList.layoutManager = layoutManager
                holder.tvMsg.text = textBody.message
                val url = Constants.baseUrlImage + Constants.workerAvatar
                Glide.with(act).load(url).dontAnimate()
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .into(holder.civKefuImage)

            }catch (ex:Exception){
                ToastUtils.showToast(act, ex.message)
                print(ex)
            }
            val localTime = Utils().timestampToDate(System.currentTimeMillis() + 700)
            holder.tvLeftTime.text = localTime
        }
        else if (holder is TipMsgViewHolder) {
            val item = msgList!![position]
            item.cMsg?.let {
                val timeStr = Utils().timestampToString(it.msgTime)
                holder.tvCurrentTime.text = timeStr
                holder.tvTitle.text = it.content.data?:""
            }
        }
        else if (holder is TextMsgViewHolder) {
            //因为headerView占了1个位置，所以要减1
            val item = msgList!![position]

            if (item.cMsg == null) {
                return
            }

            var localTime = "Time error"
            item.cMsg?.let {
                localTime = Utils().timestampToString(it.msgTime)
            }
            if (!item.isLeft) {
                holder.tvRightMsg.tag = position
                holder.llReplyRight.tag = position
                holder.tvRightTime.text = localTime
                holder.tvRightTime.visibility = View.VISIBLE
                holder.tvLeftTime.visibility = View.GONE

                holder.lyLeftContent.visibility = View.GONE
                holder.lyRightContent.visibility = View.VISIBLE
                holder.rlImagecontainer.visibility = View.GONE

                if (item.sendStatus != MessageSendState.发送成功) {
                    holder.ivSendStatus.visibility = View.VISIBLE
                } else
                    holder.ivSendStatus.visibility = View.GONE

                var text = item.cMsg!!.content.data
                //val dd = "{\"content\":\"文本\",\"image\":\"图片链接\",\"video\":\"视频链接\",\"color\":\"文本颜色\"}";

                if (text.contains("\"color\"")){
                    val gson = Gson()
                    try {
                        val textBody: TextBody = gson.fromJson(text, TextBody::class.java)
                        text = textBody.content
                        println("Content: ${textBody.content}")
                        println("Image: ${textBody.image}")
                        println("Video: ${textBody.video}")
                        println("Color: ${textBody.color}")

                        //holder.tvRightMsg.setTextColor(Color.parseColor(textBody.color))
                        holder.ivRightImg.visibility = View.VISIBLE
                        holder.ivRightPlay.visibility = View.GONE
                        holder.ivSendStatus.visibility = View.GONE
                        holder.tvRightMsg.setTextColor(Utils().hexToColor(textBody.color?:"#ffffff"))

                        var meidaUrl = textBody.image

                        if ((textBody.video?:"").length > 0) {
                            meidaUrl = textBody.video
                            holder.ivRightPlay.visibility = View.VISIBLE
                        }

                        if ((meidaUrl?:"").isNotEmpty()) {
                            holder.rlImagecontainer.visibility = View.VISIBLE
                        }else{
                            return
                        }

                        holder.ivRightImg.setOnClickListener {
                            var tag = holder.tvRightMsg.tag as Int
                            val myItem = msgList!![tag]

                            if ((textBody.video?:"").isNotEmpty()) {
                                listener?.onPlayVideo(textBody.video?:"")
                            }else {
                                listener?.onPlayImage(textBody.image?:"")
                            }
                        }

                        var thumb = meidaUrl
                        Glide.with(act)
                            .load(thumb)
                            .apply(
                                RequestOptions()
                                    .placeholder(R.drawable.loading_animation)
                                    .dontAnimate().skipMemoryCache(true)
                            )
                            .listener(object : RequestListener<Drawable?> {
                                override fun onLoadFailed(
                                    e: GlideException?,
                                    model: Any?,
                                    target: Target<Drawable?>,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    return false
                                }

                                override fun onResourceReady(
                                    resource: Drawable,
                                    model: Any,
                                    target: Target<Drawable?>?,
                                    dataSource: DataSource,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    val bitmap = Utils().drawableToBitmap(resource);
                                    print(bitmap.width)
                                    if (bitmap.height > bitmap.width) {
                                        Utils().updateLayoutParams(
                                            holder.rlImagecontainer,
                                            Utils().dp2px(106.0f),
                                            Utils().dp2px(176.0f)
                                        )
                                    } else {
                                        Utils().updateLayoutParams(
                                            holder.rlImagecontainer,
                                            Utils().dp2px(176.0f),
                                            Utils().dp2px(106.0f)
                                        )
                                    }
                                    return false
                                }
                            })
                            .into(holder.ivRightImg)

                        // 必须在事件发生前，调用这个方法来监视View的触摸
//                        val builder: XPopup.Builder = XPopup.Builder(act)
//                            .watchView(holder.tvLeftMsg)
                        holder.tvRightMsg.setOnLongClickListener(OnLongClickListener {
                            holder.builder3.asAttachList(
                                arrayOf<String>("复制"), null,
                                object : OnSelectListener {
                                    override fun onSelect(position: Int, text: String) {
                                        when (position) {
                                            0 -> {
                                                println("复制")
                                                listener?.onCopy(it.tag as Int)
                                            }
                                        }
                                    }
                                })
                                .show()
                            false
                        })
                    } catch (e: Exception) {
                        println("Error parsing JSON: ${e.message}")
                        e.printStackTrace()
                    }
                }else{
                    // 必须在事件发生前，调用这个方法来监视View的触摸
//                    val builder: XPopup.Builder = XPopup.Builder(act)
//                        .watchView(holder.tvLeftMsg)
                    holder.tvRightMsg.setOnLongClickListener(OnLongClickListener {
                        holder.builder3.asAttachList(
                            arrayOf<String>("复制","回复"), null,
                            object : OnSelectListener {
                                override fun onSelect(position: Int, text: String) {
                                    when (position) {
                                        0 -> {
                                            println("复制")
                                            listener?.onCopy(it.tag as Int)
                                        }
                                        1 -> {
                                            println("回复")
                                            listener?.onQuote(it.tag as Int)
                                        }
                                    }
                                }
                            })
                            .show()
                        false
                    })
                }

                processTextWithLinks(holder.tvRightMsg, text)
                if (item.cMsg!!.replyMsgId > 0){
                    holder.tvRightSize.visibility = View.GONE
                    if ((item.replyItem?.fileName?:"").isNotEmpty()){
                        val ext = item.replyItem?.fileName?.split(".")?.last()
                        if (ext != null && Constants.fileTypes.contains(ext)){
                            holder.tvRightSize.text = ((item.replyItem?.size?:0)  * 0.001).toString() + "K"
                            holder.tvRightSize.visibility = View.VISIBLE
                        }
                        holder.ivRightReplyImage.visibility = View.VISIBLE
                    }else{
                        holder.ivRightReplyImage.visibility = View.GONE
                        holder.tvRightReplyOrigin.text = item.replyItem?.content?: ""
                    }

                    val fileName = item.replyItem?.fileName?:""
                    if (fileName.isNotEmpty()){
                        holder.tvRightReplyOrigin.text = fileName
                        holder.ivRightReplyImage.setImageResource(getFileThumbnail(fileName.split(".").last()))
                    }

                    holder.llReplyRight.setOnClickListener(object : View.OnClickListener {
                        override fun onClick(v: View?) {
                            listener?.onShowOriginal(v?.tag as Int)
                        }
                    })
                    holder.llReplyRight.visibility = View.VISIBLE
                    //holder.tvRightReplyOrigin.text = text.substring(0,text.indexOf("回复："))
                }else{
                    holder.llReplyRight.visibility = View.GONE
                }
            } else {
                holder.llReplyLeft.tag = position
                holder.tvLeftMsg.tag = position
                holder.tvLeftTime.text = localTime
                holder.tvRightTime.visibility = View.GONE
                holder.tvLeftTime.visibility = View.VISIBLE

                holder.lyLeftContent.visibility = View.VISIBLE
                holder.lyRightContent.visibility = View.GONE
                holder.rlLeftImagecontainer.visibility = View.GONE
                var text = item.cMsg!!.content.data

                if (text.contains("\"color\"")){
                    val gson = Gson()
                    try {
                        val textBody: TextBody = gson.fromJson(text, TextBody::class.java)
                        text = textBody.content
                        holder.ivLeftImage.visibility = View.VISIBLE
                        holder.ivLeftPlay.visibility = View.GONE
                        holder.ivSendStatus.visibility = View.GONE
                        holder.tvLeftMsg.setTextColor(Utils().hexToColor(textBody.color?:"#ffffff"))
                        var meidaUrl = textBody.image

                        if ((textBody.video?:"").length > 0) {
                            meidaUrl = textBody.video
                            holder.ivLeftPlay.visibility = View.VISIBLE
                        }

                        if ((meidaUrl?:"").isNotEmpty()) {
                            holder.rlLeftImagecontainer.visibility = View.VISIBLE
                            holder.ivLeftImage.setOnClickListener {
                                var tag = holder.tvLeftMsg.tag as Int
                                val myItem = msgList!![tag]

                                if ((textBody.video ?: "").isNotEmpty()) {
                                    listener?.onPlayVideo(textBody.video ?: "")
                                } else {
                                    listener?.onPlayImage(textBody.image ?: "")
                                }
                            }

                            var thumb = meidaUrl
                            Glide.with(act)
                                .load(thumb)
                                .apply(
                                    RequestOptions()
                                        .placeholder(R.drawable.loading_animation)
                                        .dontAnimate().skipMemoryCache(true)
                                )
                                .listener(object : RequestListener<Drawable?> {
                                    override fun onLoadFailed(
                                        e: GlideException?,
                                        model: Any?,
                                        target: Target<Drawable?>,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        return false
                                    }

                                    override fun onResourceReady(
                                        resource: Drawable,
                                        model: Any,
                                        target: Target<Drawable?>?,
                                        dataSource: DataSource,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        val bitmap = Utils().drawableToBitmap(resource);
                                        print(bitmap.width)
                                        if (bitmap.height > bitmap.width) {
                                            Utils().updateLayoutParams(
                                                holder.rlLeftImagecontainer,
                                                Utils().dp2px(106.0f),
                                                Utils().dp2px(176.0f)
                                            )
                                        } else {
                                            Utils().updateLayoutParams(
                                                holder.rlLeftImagecontainer,
                                                Utils().dp2px(176.0f),
                                                Utils().dp2px(106.0f)
                                            )
                                        }
                                        return false
                                    }
                                })
                                .into(holder.ivLeftImage)
                        }

                        // 必须在事件发生前，调用这个方法来监视View的触摸
//                        val builder: XPopup.Builder = XPopup.Builder(act)
//                            .watchView(holder.tvLeftMsg)
                        holder.tvLeftMsg.setOnLongClickListener(OnLongClickListener {
                            holder.builder.asAttachList(
                                arrayOf<String>("复制"), null,
                                object : OnSelectListener {
                                    override fun onSelect(position: Int, text: String) {
                                        when (position) {
                                            0 -> {
                                                println("复制")
                                                listener?.onCopy(it.tag as Int)
                                            }
                                        }
                                    }
                                })
                                .show()
                            false
                        })

                    } catch (e: Exception) {
                        println("Error parsing JSON: ${e.message}")
                        e.printStackTrace()
                    }
                }else{
                    // 必须在事件发生前，调用这个方法来监视View的触摸
//                    val builder: XPopup.Builder = XPopup.Builder(act)
//                        .watchView(holder.tvLeftMsg)
                    holder.tvLeftMsg.setOnLongClickListener(OnLongClickListener {
                        holder.builder.asAttachList(
                            arrayOf<String>("复制","回复"), null,
                            object : OnSelectListener {
                                override fun onSelect(position: Int, text: String) {
                                    when (position) {
                                        0 -> {
                                            println("复制")
                                            listener?.onCopy(it.tag as Int)
                                        }
                                        1 -> {
                                            println("回复")
                                            listener?.onQuote(it.tag as Int)
                                        }
                                    }
                                }
                            })
                            .show()
                        false
                    })
                }
                processTextWithLinks(holder.tvLeftMsg, text)
                if (item.cMsg!!.replyMsgId > 0){
                    holder.tvLeftSize.visibility = View.GONE
                    val fileName = item.replyItem?.fileName?:""
                    if (fileName.isNotEmpty()){
                        val ext = item.replyItem?.fileName?.split(".")?.last()
                        if (ext != null && Constants.fileTypes.contains(ext)){
                            holder.tvLeftSize.text = ((item.replyItem?.size?:0)  * 0.001).toString() + "K"
                            holder.tvLeftSize.visibility = View.VISIBLE
                        }
                        holder.tvLeftReplyOrigin.text = fileName
                        holder.ivLeftReplyImage.setImageResource(getFileThumbnail(fileName.split(".").last()))
                        holder.ivLeftReplyImage.visibility = View.VISIBLE
                    }else{
                        holder.ivLeftReplyImage.visibility = View.GONE
                        holder.tvLeftReplyOrigin.text = item.replyItem?.content?: ""
                    }

                    holder.llReplyLeft.setOnClickListener(object : View.OnClickListener {
                        override fun onClick(v: View?) {
                            listener?.onShowOriginal(v?.tag as Int)
                        }
                    })
                    holder.llReplyLeft.visibility = View.VISIBLE
                }else{
                    holder.llReplyLeft.visibility = View.GONE
                }
                //客服头像
                val url = Constants.baseUrlImage + Constants.workerAvatar
                print("avatar:$url")
                Glide.with(holder.ivKefuImage).load(url).dontAnimate()
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .into(holder.ivKefuImage)
            }
        }
    }

    override
    fun getItemViewType(position: Int) : Int {
        msgList?.let {
          return  it.get(position).cellType.value
        }
        return 0
    }

    override fun getItemCount(): Int {
        return if (msgList == null) 1 else msgList!!.size
    }

    inner class QAViewHolder(binding: ItemQaListBinding) : RecyclerView.ViewHolder(binding.root){
        var rcvQa = binding.rcvQa
        var tvTitle = binding.tvTitle
        var tvLeftTime = binding.tvLeftTime;
        var qaAdapter: GroupedQAdapter
        var ivKefuImage =  binding.civKefuImage

        init {
            // 初始化自动回复列表
            rcvQa.layoutManager = LinearLayoutManager(act)
            qaAdapter = GroupedQAdapter(act, ArrayList(), null)
            rcvQa.adapter = qaAdapter

            // 提问列表点击事件
            qaAdapter.setOnHeaderClickListener { _, _, groupPosition ->
                /*
                自动回复 有两种情况：
                1、一级问题，点击后回复对应的答案；
                2、一级问题，点击展示与一级相关的问题分类（及二级问题），点击二级对应应的问题，则回复答案。
                */

                val QA = qaAdapter.data.get(groupPosition)
                qaClicked(QA)
                if (qaAdapter.data.get(groupPosition).related != null && (qaAdapter.data.get(
                        groupPosition
                    ).related!!.size ?: 0) > 0
                ) {
                    if (qaAdapter.isExpand(groupPosition)) {
                        qaAdapter.collapseGroup(groupPosition)

                    } else {
                        qaAdapter.collapseTheResetGroup(groupPosition)
                        qaAdapter.expandGroup(groupPosition)
                    }
                }
            }

            // 问题点击事件
            qaAdapter.setOnChildClickListener { _, _, groupPosition, childPosition ->

                val QA = qaAdapter.data.get(groupPosition).related?.get(childPosition)
                qaClicked(QA)
            }
        }

        fun qaClicked(QA: QA?){
            QA?.apply {
                if (this.clicked ?: true) {
                    return
                }

                val questionTxt = this.question.content.data ?: ""
                val txtAnswer = this.content ?: "null"

                var withAutoReplyBuilder = CMessage.WithAutoReply.newBuilder()

                withAutoReplyBuilder.title = questionTxt
                withAutoReplyBuilder.id = QA.id
                withAutoReplyBuilder.createdTime = Utils().getNowTimeStamp()

                val multipAnswer = this.answer?.joinToString(separator = "\n") ?: ""
                // 发送提问消息
                if (txtAnswer.isNotEmpty()) {
                    listener?.onSendLocalMsg(questionTxt, false)
                    // 自动回答
                    listener?.onSendLocalMsg(txtAnswer, true)
                    QA.clicked = true;
                    qaAdapter.notifyDataChanged()

                    val uAnswer = CMessage.MessageUnion.newBuilder()
                    val uQC = CMessage.MessageContent.newBuilder()
                    uQC.data = txtAnswer
                    uAnswer.content = uQC.build()
                    withAutoReplyBuilder.addAnswers(uAnswer)
                }
                if (multipAnswer.isNotEmpty()) {
                    //listener?.onSendLocalMsg(questionTxt, false)
                    for (a in this.answer) {
                        //if (a!!.image != null) {
                        a?.image?.uri?.let {
                            // 自动回答
                            listener?.onSendLocalMsg(it, true, "MSG_IMG")

                            val uAnswer = CMessage.MessageUnion.newBuilder()
                            val uQC = CMessage.MessageImage.newBuilder()
                            uQC.uri = it
                            uAnswer.image = uQC.build()
                            withAutoReplyBuilder.addAnswers(uAnswer)
                        }

                        a?.content?.data?.let {
                            // 自动回答
                            listener?.onSendLocalMsg(it, true, "MSG_TEXT")

                            val uAnswer = CMessage.MessageUnion.newBuilder()
                            val uQC = CMessage.MessageContent.newBuilder()
                            uQC.data = txtAnswer
                            uAnswer.content = uQC.build()
                            withAutoReplyBuilder.addAnswers(uAnswer)
                        }

                        //}
                    }
                    QA.clicked = true;
                    qaAdapter.notifyDataChanged()
                }
                withAutoReplyU = withAutoReplyBuilder.build()
            }
        }
    }

    inner class TextMsgViewHolder(binding: ItemTextMessageBinding) : RecyclerView.ViewHolder(binding.root){
        val tvLeftTime = binding.tvLeftTime
        var tvLeftMsg =  binding.tvLeftMsg
        var tvRightTime =  binding.tvRightTime
        var tvRightMsg =  binding.tvRightMsg
        var ivSendStatus =  binding.ivSendStatus
        var ivArrow =  binding.ivArrow
        var ivRightChatarrow =  binding.ivRightChatarrow
        var ivKefuImage =  binding.civKefuImage
        var ivRightImage =  binding.civRightImage

        var llReplyLeft = binding.llReplyLeft
        var tvLeftReplyOrigin = binding.tvLeftReplyOrigin

        var llReplyRight = binding.llReplyRight
        var tvRightReplyOrigin = binding.tvRightReplyOrigin

        var tvLeftSize = binding.tvLeftReplySize
        var tvRightSize = binding.tvRightReplySize

        var ivLeftReplyImage = binding.ivLeftReplyImage
        var ivRightReplyImage = binding.ivRightReplyImage

        var lyLeftContent = binding.lyLeftContent
        var lyRightContent = binding.lyRightContent

        var ivRightImg =  binding.ivRightImage
        var ivRightPlay = binding.ivRightPlay
        var rlImagecontainer = binding.rlImagecontainer

        var ivLeftImage =  binding.ivLeftImage
        var ivLeftPlay = binding.ivPlay
        var rlLeftImagecontainer = binding.rlLeftImagecontainer
        val builder: XPopup.Builder = XPopup.Builder(act).watchView(tvLeftMsg)
        val builder3: XPopup.Builder = XPopup.Builder(act).watchView(tvRightMsg)
        init {
            tvLeftMsg.movementMethod = LinkMovementMethod.getInstance()
            tvRightMsg.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    inner class TipMsgViewHolder(binding: ItemTipMsgBinding) : RecyclerView.ViewHolder(binding.root) {
        val tvTitle = binding.tvTitle
        val tvCurrentTime = binding.tvCurrentTime
    }

    inner class ItemLastLineViewHolder(binding: ItemLastLineBinding) : RecyclerView.ViewHolder(binding.root) {
        val tvTitle = binding.tvTitle
    }

    inner class ItemVideoViewHolder(binding: ItemVideoImageMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        val tvLeftTime = binding.tvLeftTime
        var ivLeftImg =  binding.ivLeftImage

        var tvRightTime =  binding.tvRightTime
        var ivRightImg =  binding.ivRightImage

        var ivArrow =  binding.ivArrow
        var ivRightChatarrow =  binding.ivRightChatarrow

        var ivKefuImage =  binding.civKefuImage
        var civKefuRightImage =  binding.civKefuRightImage

        var ivPlay =  binding.ivPlay
        val ivRightPlay = binding.ivRightPlay

        var lySend =  binding.layoutSend
        var ivSendStatus =  binding.ivSendStatus

        var rlImagecontainer =  binding.rlImagecontainer
        var rlLeftImagecontainer =  binding.rlLeftImagecontainer

        init {

            val builder2: XPopup.Builder = XPopup.Builder(act)
                .watchView(ivLeftImg)
            ivLeftImg.setOnLongClickListener(OnLongClickListener {
                builder2.asAttachList(
                    arrayOf<String>("回复", "下载"), null,
                    object : OnSelectListener {
                        override fun onSelect(position: Int, text: String) {
                            when (position) {
                                0 -> {
                                    println("回复")
                                    listener?.onQuote(it.tag as Int)
                                }

                                1 -> {
                                    //删除
                                    println("下载")
                                    listener?.onDownload(it.tag as Int)
                                }
                            }
                        }
                    })
                    .show()
                false
            })
            // 必须在事件发生前，调用这个方法来监视View的触摸
            val builder4: XPopup.Builder = XPopup.Builder(act)
                .watchView(ivRightImg)
            ivRightImg.setOnLongClickListener(OnLongClickListener {
                builder4.asAttachList(
                    //"删除"前端App不需要
                    arrayOf<String>("回复", "下载"), null,
                    object : OnSelectListener {
                        override fun onSelect(position: Int, text: String) {
                            when (position) {
                                0 -> {
                                    println("回复")
                                    listener?.onQuote(it.tag as Int)
                                }

                             1 -> {
                                    //删除
                                    println("下载")
                                    listener?.onDownload(it.tag as Int)
                                }
                            }
                        }
                    })
                    .show()
                false
            })
        }
    }

    inner class ItemFileViewHolder(binding: ItemFileMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        val tvLeftTime = binding.tvLeftTime
        var ivLeftImg =  binding.ivFile

        var civKefuImage = binding.civKefuImage

        var tvRightTime =  binding.tvRightTime
        var ivRightImg =  binding.ivRightFile

        var tvRightFileName =  binding.tvRightFileName
        var tvRightFileSize =  binding.tvRightFileSize

        var tvLeftFileName =  binding.tvLeftFileName
        var tvLeftFileSize =  binding.tvLeftFileSize

        var ivSendStatus =  binding.ivSendStatus

        var llLeftContent =  binding.llLeftContent
        var llRightContent =  binding.llRightContent

        init {

            val builder2: XPopup.Builder = XPopup.Builder(act)
                .watchView(llLeftContent)
            llLeftContent.setOnLongClickListener(OnLongClickListener {
                builder2.asAttachList(
                    arrayOf<String>("回复", "下载"), null,
                    object : OnSelectListener {
                        override fun onSelect(position: Int, text: String) {
                            when (position) {
                                0 -> {
                                    println("回复")
                                    listener?.onQuote(it.tag as Int)
                                }

                                1 -> {
                                    //删除
                                    println("下载")
                                    listener?.onDownload(it.tag as Int)
                                }
                            }
                        }
                    })
                    .show()
                false
            })
            // 必须在事件发生前，调用这个方法来监视View的触摸
            val builder4: XPopup.Builder = XPopup.Builder(act)
                .watchView(llRightContent)
            llRightContent.setOnLongClickListener(OnLongClickListener {
                builder4.asAttachList(
                    //"删除"前端App不需要
                    arrayOf<String>("回复", "下载"), null,
                    object : OnSelectListener {
                        override fun onSelect(position: Int, text: String) {
                            when (position) {
                                0 -> {
                                    println("回复")
                                    listener?.onQuote(it.tag as Int)
                                }

                                1 -> {
                                    //删除
                                    println("下载")
                                    listener?.onDownload(it.tag as Int)
                                }
                            }
                        }
                    })
                    .show()
                false
            })
        }
    }

    inner class TextImagesViewHolder(binding: ItemTextImagesMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        val tvLeftTime = binding.tvLeftTime
        var civKefuImage = binding.civKefuImage
        var tvRightTime =  binding.tvRightTime
        var civRightImage = binding.civRightImage

        var tvMsg =  binding.tvMsg
        var rvList = binding.rvList

        var llTextImages = binding.llTextImages

        init {

            val builder2: XPopup.Builder = XPopup.Builder(act)
                .watchView(llTextImages)
            llTextImages.setOnLongClickListener(OnLongClickListener {
                builder2.asAttachList(
                    arrayOf<String>("回复", "复制"), null,
                    object : OnSelectListener {
                        override fun onSelect(position: Int, text: String) {
                            when (position) {
                                0 -> {
                                    println("回复")
                                    listener?.onQuote(it.tag as Int)
                                }  1 -> {
                                println("复制")
                                listener?.onCopy(it.tag as Int)
                            }
                            }
                        }
                    })
                    .show()
                false
            })
        }
    }



    fun showBigImage(imageView: ImageView, url: String){
        listener?.onPlayImage(url)
    }

    fun getFileThumbnail(ext: String) : Int{
        if (ext == "docx" || ext == "doc"){
            return R.drawable.word_default
        }else if(ext == "pdf"){
            return R.drawable.pdf_default
        }else if(ext == "xls" || ext == "xlsx" || ext == "csv"){
            return R.drawable.excel_default
        }else if (ext == "ppt" || ext == "pptx"){
            return R.drawable.ppt_default
        }else if (ext == "mp3" || ext == "m4a"){
            return R.drawable.audio_default
        }else if (Constants.imageTypes.contains(ext)){
            return R.drawable.image_default
        }else if (Constants.videoTypes.contains(ext)){
            return R.drawable.video_default
        }else{
            return R.drawable.unknown_default
        }
    }

    private fun processTextWithLinks(textView: com.teneasy.chatuisdk.ui.utils.emoji.EmoticonTextView, text: String) {
        // 设置原始文本
        textView.text = text
        
        // 使用Linkify自动识别链接
        Linkify.addLinks(textView, Linkify.WEB_URLS)
        
        // 设置链接颜色
        textView.setLinkTextColor(act.resources.getColor(android.R.color.holo_blue_light))
    }
}
