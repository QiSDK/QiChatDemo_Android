package com.teneasy.chatuisdk.ui.main;

//import com.teneasy.chatuisdk.ui.utils.emoji.EmoticonTextView

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.interfaces.OnSelectListener
import com.teneasy.chatuisdk.databinding.ItemHeaderRecyleviewBinding
import com.teneasy.chatuisdk.databinding.ItemMessageBinding
import com.teneasy.chatuisdk.ui.base.Constants
import com.teneasy.sdk.TimeUtil
import com.teneasy.sdk.ui.MessageItem
import com.teneasy.sdk.ui.MessageSendState
import java.util.*
import androidx.recyclerview.widget.LinearLayoutManager
import com.teneasy.chatuisdk.databinding.ItemLastLineBinding
import com.teneasy.chatuisdk.databinding.ItemTipMsgBinding
import com.teneasy.chatuisdk.databinding.ItemVideoPlayerBinding
import com.teneasy.sdk.ui.CellType
import android.widget.ImageView
import androidx.annotation.OptIn
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.teneasy.chatuisdk.ui.http.bean.AutoReplyItem
import com.teneasy.chatuisdk.ui.http.bean.QA


interface MessageItemOperateListener {
    fun onDelete(position: Int)
    fun onCopy(position: Int)
    fun onReSend(position: Int)
    fun onQuote(position: Int)
    fun onSendLocalMsg(msg: String, isLeft: Boolean, msgType: String = "MSG_TEXT")
    fun onPlayVideo(url: String)
    fun onPlayImage(url: String)
}

data class QADisplayedEvent(val tag: Int)
/**
 * 聊天界面列表adapter
 */
class MessageListAdapter (myContext: Context,  listener: MessageItemOperateListener?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var msgList: ArrayList<MessageItem>? = null
    val act: Context = myContext
    private var listener: MessageItemOperateListener? = listener
    var autoReplyItem: AutoReplyItem? = null
//    fun getList(): ArrayList<MessageItem>? {
//        return msgList
//    }

    fun setList(list: ArrayList<MessageItem>?) {
        this.msgList = list//ArrayList(list)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == CellType.TYPE_QA.value) {
            val binding = ItemHeaderRecyleviewBinding.inflate(
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
        }else  if (viewType == CellType.TYPE_LastLine.value) {
            val binding = ItemLastLineBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false)
            return ItemLastLineViewHolder(binding)
        }else  if (viewType == CellType.TYPE_VIDEO.value) {
            val binding = ItemVideoPlayerBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false)
            return ItemVideoViewHolder(binding)
        }else {
            val binding = ItemMessageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return MsgViewHolder(binding)
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
                    holder.tvTitle.visibility = View.VISIBLE
                }
            }
            holder.tvTitle.text = ""
        }
       else if (holder is ItemVideoViewHolder) {
            holder.tvTitle.text = ""
            val item = msgList!![position]
            item.cMsg?.let {
                val msgDate = Date(it.msgTime.seconds * 1000L)
                holder.tvTitle.text = TimeUtil.getTimeStringAutoShort2(msgDate, true) + "\n" + it.content.data + "\n"
                val videoUrl = Constants.baseUrlImage + item.cMsg?.video?.uri?: ""
                //val mediaItem = MediaItem.fromUri(videoUrl)
                val mediaItem = MediaItem.Builder().setMediaId("ddd").setTag(position).setUri(videoUrl).build()
                val player = ExoPlayer.Builder(act).build()
                holder.playerView.player = player
                holder.playerView.tag = position
                holder.playerView.hideController()

                player.setMediaItem(mediaItem)
                // Prepare the player.
                player.prepare()
                // Start the playback.
                player.pause()

                holder.iv_play.setOnClickListener {
                    listener?.onPlayVideo(videoUrl)
                }
            }
            if (!item.isLeft){
                holder.tvTitle.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    startToStart =  ConstraintLayout.LayoutParams.UNSET
                    endToEnd = holder.root.id
                }

                holder.playerView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    startToStart =  ConstraintLayout.LayoutParams.UNSET
                    endToEnd = holder.root.id
                }
            }
        }
        else if (holder is TipMsgViewHolder) {
            val item = msgList!![position]
            item.cMsg?.let {
                val msgDate = Date(it.msgTime.seconds * 1000L)
                holder.tvTitle.text = TimeUtil.getTimeStringAutoShort2(msgDate, true) + "\n" + it.content.data + "\n"
            }
        }
        else if (holder is MsgViewHolder) {
            //因为headerView占了1个位置，所以要减1
            val item = msgList!![position]

            if (item.cMsg == null) {
                return
            }

            var localTime = "Time error"

            item.cMsg?.let {
                val msgDate = Date(it.msgTime.seconds * 1000L)
                localTime = TimeUtil.getTimeStringAutoShort2(msgDate, true)
            }
            if (!item.isLeft) {
                holder.tvRightMsg.tag = position
                holder.ivRightImg.tag = position
                holder.tvRightTime.text = localTime
                holder.tvRightTime.visibility = View.VISIBLE
                holder.tvRightMsg.visibility = View.VISIBLE
                holder.lySend.visibility = View.VISIBLE

                holder.tvLeftTime.visibility = View.GONE
                holder.ivLeftImg.visibility = View.GONE
                holder.tvLeftMsg.visibility = View.GONE

                if (item.sendStatus != MessageSendState.发送成功) {
                    holder.ivSendStatus.visibility = View.VISIBLE
                } else
                    holder.ivSendStatus.visibility = View.GONE

                if (getItemViewType(position) == CellType.TYPE_Text.value) {
                    holder.tvRightMsg.visibility = View.VISIBLE
                    holder.ivRightImg.visibility = View.GONE
                    holder.tvRightMsg.text = item.cMsg!!.content.data

                } else {
                    holder.tvRightMsg.visibility = View.GONE
                    holder.ivRightImg.visibility = View.VISIBLE
                    holder.ivRightImg.setOnClickListener {
                        showBigImage(it as ImageView, Constants.baseUrlImage + item.cMsg!!.image.uri)
                    }
//                Glide.with(act).load(item.cMsg!!.image.uri).dontAnimate()
//                    .skipMemoryCache(true)
//                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
//                    .into(holder.ivRightImg)
                    Log.d("AdapterNChatLib", "imageUrl:" + Constants.baseUrlImage + item.cMsg!!.image.uri)
                    Glide.with(act)
                        //.asBitmap()
                        .load(Constants.baseUrlImage + item.cMsg!!.image.uri)
                        .skipMemoryCache(true)
                        //.override(600,400)
                        //.centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .into(holder.ivRightImg)
                }
            } else {
                holder.tvLeftMsg.tag = position
                holder.ivLeftImg.tag = position
                holder.tvLeftTime.text = localTime
                holder.tvLeftTime.visibility = View.VISIBLE
                holder.tvLeftMsg.visibility = View.VISIBLE
                holder.tvRightTime.visibility = View.GONE
                holder.tvRightMsg.visibility = View.GONE
                holder.ivRightImg.visibility = View.GONE
                holder.tvRightMsg.visibility = View.GONE
                holder.lySend.visibility = View.GONE

                if (getItemViewType(position) == CellType.TYPE_Text.value) {
                    holder.tvLeftMsg.visibility = View.VISIBLE
                    holder.ivLeftImg.visibility = View.GONE
                    holder.tvLeftMsg.text = item.cMsg!!.content.data
                } else {
                    holder.tvLeftMsg.visibility = View.GONE
                    holder.ivLeftImg.visibility = View.VISIBLE
                    holder.ivLeftImg.setOnClickListener {
                        showBigImage(it as ImageView, Constants.baseUrlImage + item.cMsg!!.image.uri)
                    }

                Glide.with(act).load(item.cMsg!!.image.uri).dontAnimate()
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .into(holder.ivLeftImg)

                }
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

    inner class QAViewHolder(binding: ItemHeaderRecyleviewBinding) : RecyclerView.ViewHolder(binding.root){
        var rcvQa = binding.rcvQa
        var tvTitle = binding.tvTitle
        var qaAdapter: GroupedQAdapter

        init {
            // 初始化自动回复列表
            rcvQa.layoutManager = LinearLayoutManager(act)
            qaAdapter = GroupedQAdapter(act, ArrayList(), null)
            rcvQa.adapter = qaAdapter

         /*   val param = JsonObject()
            param.addProperty("consultId", Constants.CONSULT_ID)
            param.addProperty("workerId", Constants.workerId)
            val request = XHttp.custom().accessToken(false)
            request.headers("X-Token", Constants.xToken)
            request.call(request.create(MainApi.IMainTask::class.java)
                .queryAutoReply(param),
                object : ProgressLoadingCallBack<ReturnData<AutoReply>>(null) {
                    override fun onSuccess(res: ReturnData<AutoReply>) {
                        if (res.code != 200 || res.data == null || res.data.autoReplyItem == null){
                            Log.d("AdapterNChatLib", "自动回复为空")
                        }
                            tvTitle.text = res.data.autoReplyItem?.title
                            res.data.autoReplyItem?.qa?.let {
                            qaAdapter.setDataList(it)
                            tvTitle.visibility = View.VISIBLE
                            EventBus.getDefault().post(QADisplayedEvent(100))
                        }
                    } override fun onError(e: ApiException?) {
                        super.onError(e)
                        println(e)
                    }
                }
            )*/

            // 提问列表点击事件
            qaAdapter.setOnHeaderClickListener { _, _, groupPosition ->
                /*
                自动回复 有两种情况：
                1、一级问题，点击后回复对应的答案；
                2、一级问题，点击展示与一级相关的问题分类（及二级问题），点击二级对应应的问题，则回复答案。
                */

                var QA = qaAdapter.data.get(groupPosition)

                QA?.apply {
                    if (this.clicked ?: true) {
                        return@setOnHeaderClickListener
                    }

                    val questionTxt = this.question?.content?.data ?: ""
                    val txtAnswer = this.content ?: "null"

                    val multipAnswer = this.answer?.joinToString(separator = "\n") ?: ""
                    if (txtAnswer.isNotEmpty()) {
                        // 发送提问消息
                        listener?.onSendLocalMsg(questionTxt, false)
                        // 自动回答
                        listener?.onSendLocalMsg(txtAnswer, true)
                        QA.clicked = true;
                        qaAdapter.notifyDataChanged()
                    }
                    if (multipAnswer.isNotEmpty()) {
                        listener?.onSendLocalMsg(questionTxt, false)
                        for (a in this.answer) {
                            if (a!!.image != null) {
                                // 自动回答
                                listener?.onSendLocalMsg(a.image.uri, true, "MSG_IMG")
                            }
                        }
                        QA.clicked = true;
                        qaAdapter.notifyDataChanged()
                    } else {
                        if (qaAdapter.isExpand(groupPosition)) {
                            qaAdapter.collapseGroup(groupPosition)
                        } else {
                            qaAdapter.collapseTheResetGroup(groupPosition)
                            qaAdapter.expandGroup(groupPosition)
                        }
                    }
                }
            }

            // 问题点击事件
            qaAdapter.setOnChildClickListener { _, _, groupPosition, childPosition ->

                var QA = qaAdapter.data.get(groupPosition).related?.get(childPosition)
                QA?.apply {
                    if (this.clicked?:true){
                        return@setOnChildClickListener
                    }

                    val questionTxt = this.question?.content?.data ?:""
                    val txtAnswer = this.content ?:"null"

                    val multipAnswer = this.answer?.joinToString(separator = "\n")  ?:""
                    // 发送提问消息
                    if (txtAnswer.isNotEmpty()){
                        listener?.onSendLocalMsg(questionTxt, false)
                        // 自动回答
                        listener?.onSendLocalMsg(txtAnswer, true)
                        QA.clicked = true;
                        qaAdapter.notifyDataChanged()
                    }
                    if (multipAnswer.isNotEmpty()){
                        listener?.onSendLocalMsg(questionTxt, false)
                        for (a in this.answer){
                            if (a!!.image != null){
                                // 自动回答
                                listener?.onSendLocalMsg(a.image.uri, true, "MSG_IMG")
                            }
                        }
                        QA.clicked = true;
                        qaAdapter.notifyDataChanged()
                    }
                }
                }
        }
    }
    inner class MsgViewHolder(binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root){
        val tvLeftTime = binding.tvLeftTime
        var tvLeftMsg =  binding.tvLeftMsg
        var tvRightTime =  binding.tvRightTime
        var tvRightMsg =  binding.tvRightMsg
        var ivRightImg =  binding.ivRightImage
        var lySend =  binding.layoutSend
        var ivLeftImg =  binding.ivLeftImage
        var ivSendStatus =  binding.ivSendStatus

        init {
            // 必须在事件发生前，调用这个方法来监视View的触摸
            val builder: XPopup.Builder = XPopup.Builder(act)
                .watchView(tvLeftMsg)
            tvLeftMsg.setOnLongClickListener(OnLongClickListener {
                builder.asAttachList(
                    //"删除"前端App不需要
                    arrayOf<String>("复制","回复"), null,
                    object : OnSelectListener {
                        override fun onSelect(position: Int, text: String) {
                            when (position) {
                                0 -> {
                                    //置顶
                                    println("复制")
                                    listener?.onCopy(it.tag as Int)
                                }

                                1 -> {
                                    //复制
                                    println("回复")
                                    listener?.onQuote(it.tag as Int)
                                }
                                2 -> {
                                    //删除
                                    println("删除")
                                    listener?.onDelete(it.tag as Int)
                                }
                            }
                        }
                    })
                    .show()
                false
            })

            val builder2: XPopup.Builder = XPopup.Builder(act)
                .watchView(ivLeftImg)
            ivLeftImg.setOnLongClickListener(OnLongClickListener {
                builder2.asAttachList(
                    //"撤回", "编辑" 前端App不需要
                    arrayOf<String>("回复"), null,
                    object : OnSelectListener {
                        override fun onSelect(position: Int, text: String) {
                            when (position) {
                                0 -> {
                                    //置顶
                                    println("复制")
                                    listener?.onCopy(it.tag as Int)
                                }

                                1 -> {
                                    //复制
                                    println("回复")
                                    listener?.onQuote(it.tag as Int)
                                }
                                2 -> {
                                    //删除
                                    println("删除")
                                    listener?.onDelete(it.tag as Int)
                                } 3 -> {
                                //删除
                                println("编辑")
                                listener?.onReSend(it.tag as Int)
                            }
                            }
                        }
                    })
                    .show()
                false
            })

            // 必须在事件发生前，调用这个方法来监视View的触摸
            val builder3: XPopup.Builder = XPopup.Builder(act)
                .watchView(tvRightMsg)
            tvRightMsg.setOnLongClickListener(OnLongClickListener {
                builder3.asAttachList(
                    //"删除"前端App不需要
                    arrayOf<String>("复制","回复"), null,
                    object : OnSelectListener {
                        override fun onSelect(position: Int, text: String) {
                            when (position) {
                                0 -> {
                                    //置顶
                                    println("复制")
                                    listener?.onCopy(it.tag as Int)
                                }

                                1 -> {
                                    //复制
                                    println("回复")
                                    listener?.onQuote(it.tag as Int)
                                }
                                2 -> {
                                    //删除
                                    println("删除")
                                    listener?.onDelete(it.tag as Int)
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
                    arrayOf<String>("回复"), null,
                    object : OnSelectListener {
                        override fun onSelect(position: Int, text: String) {
                            when (position) {
                                0 -> {
                                    //置顶
                                    println("复制")
                                    listener?.onCopy(it.tag as Int)
                                }

                                1 -> {
                                    //复制
                                    println("回复")
                                    listener?.onQuote(it.tag as Int)
                                }
                                2 -> {
                                    //删除
                                    println("删除")
                                    listener?.onDelete(it.tag as Int)
                                }
                            }
                        }
                    })
                    .show()
                false
            })
        }
    }

    inner class TipMsgViewHolder(binding: ItemTipMsgBinding) : RecyclerView.ViewHolder(binding.root) {
        val tvTitle = binding.tvTitle
    }

    inner class ItemLastLineViewHolder(binding: ItemLastLineBinding) : RecyclerView.ViewHolder(binding.root) {
        val tvTitle = binding.tvTitle
    }

    inner class ItemVideoViewHolder(binding: ItemVideoPlayerBinding) : RecyclerView.ViewHolder(binding.root) {
        val tvTitle = binding.tvTitle
        val playerView = binding.playerView
        var iv_play = binding.ivPlay
        val root = binding.csParent

        init {
            // 必须在事件发生前，调用这个方法来监视View的触摸
            val builder: XPopup.Builder = XPopup.Builder(act)
                .watchView(playerView)
            playerView.setOnLongClickListener(OnLongClickListener {
                builder.asAttachList(
                    //"删除"前端App不需要
                    arrayOf<String>("回复"), null,
                    object : OnSelectListener {
                        override fun onSelect(position: Int, text: String) {
                            when (position) {
                                0 -> {
                                    //置顶
                                    println("复制")
                                    listener?.onCopy(it.tag as Int)
                                }

                                1 -> {
                                    //复制
                                    println("回复")
                                    listener?.onQuote(it.tag as Int)
                                }

                                2 -> {
                                    //删除
                                    println("删除")
                                    listener?.onDelete(it.tag as Int)
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

}