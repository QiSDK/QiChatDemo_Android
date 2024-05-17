package com.teneasy.chatuisdk.ui.main;

//import com.teneasy.chatuisdk.ui.utils.emoji.EmoticonTextView

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
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
import com.google.gson.JsonObject
import com.teneasy.chatuisdk.databinding.ItemLastLineBinding
import com.teneasy.chatuisdk.databinding.ItemTipMsgBinding
import com.teneasy.chatuisdk.ui.http.MainApi
import com.teneasy.chatuisdk.ui.http.ReturnData
import com.teneasy.chatuisdk.ui.http.bean.AutoReply
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.callback.ProgressLoadingCallBack
import com.xuexiang.xhttp2.exception.ApiException


interface MessageItemOperateListener {
    fun onDelete(position: Int)
    fun onCopy(position: Int)
    fun onReSend(position: Int)
    fun onQuote(position: Int)
    fun onSendLocalMsg(msg: String, isLeft: Boolean)
}
/**
 * 聊天界面列表adapter
 */
class MessageListAdapter (myContext: Context,  listener: MessageItemOperateListener?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var msgList: ArrayList<MessageItem>? = null
    var TYPE_Text : Int = 0
    val TYPE_Image : Int = 1
    val TYPE_Tip: Int = 3
    val TYPE_QA : Int = 4
    val TYPE_LastLine : Int = 5
    val act: Context = myContext
    private var listener: MessageItemOperateListener? = listener
    private lateinit var qaAdapter: GroupedQAdapter
//    fun getList(): ArrayList<MessageItem>? {
//        return msgList
//    }

    fun setList(list: ArrayList<MessageItem>?) {
        this.msgList = list//ArrayList(list)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == TYPE_QA) {
            val binding = ItemHeaderRecyleviewBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false)
            return HeaderViewHolder(binding)
        } else  if (viewType == TYPE_Tip) {
            val binding = ItemTipMsgBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false)
            return TipMsgViewHolder(binding)
        }else  if (viewType == TYPE_LastLine) {
            val binding = ItemLastLineBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false)
            return ItemLastLineViewHolder(binding)
        }else {
            val binding = ItemMessageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return MsgViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
           // holder.rcvQa.adapter = qaAdapter
        }else if (holder is ItemLastLineViewHolder) {
            // holder.rcvQa.adapter = qaAdapter
        }else  if (holder is TipMsgViewHolder) {
            val item = msgList!![position]
            item.cMsg?.let {
                val msgDate = Date(it.msgTime.seconds * 1000L)
                holder.tvTitle.text = TimeUtil.getTimeStringAutoShort2(msgDate, true) + "\n\n" + it.content.data
            }
        }else if (holder is MsgViewHolder) {
            if (msgList == null) {
                return
            }
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

                if (getItemViewType(position) == TYPE_Text) {
                    holder.tvRightMsg.visibility = View.VISIBLE
                    holder.ivRightImg.visibility = View.GONE
                    holder.tvRightMsg.text = item.cMsg!!.content.data
                } else {
                    holder.tvRightMsg.visibility = View.GONE
                    holder.ivRightImg.visibility = View.VISIBLE
//                Glide.with(act).load(item.cMsg!!.image.uri).dontAnimate()
//                    .skipMemoryCache(true)
//                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
//                    .into(holder.ivRightImg)

                    Glide.with(act)
                        .asBitmap()
                        .load(Constants.baseUrlImage + item.cMsg!!.image.uri)
                        //.skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .into(holder.ivRightImg)
                }
            } else {
                holder.tvLeftMsg.tag = position
                holder.tvLeftTime.text = localTime
                holder.tvLeftTime.visibility = View.VISIBLE
                holder.tvLeftMsg.visibility = View.VISIBLE
                holder.tvRightTime.visibility = View.GONE
                holder.tvRightMsg.visibility = View.GONE
                holder.ivRightImg.visibility = View.GONE
                holder.tvRightMsg.visibility = View.GONE
                holder.lySend.visibility = View.GONE

                if (getItemViewType(position) == TYPE_Text) {
                    holder.tvLeftMsg.visibility = View.VISIBLE
                    holder.ivLeftImg.visibility = View.GONE
                    holder.tvLeftMsg.text = item.cMsg!!.content.data
                } else {
                    holder.tvLeftMsg.visibility = View.GONE
                    holder.ivLeftImg.visibility = View.VISIBLE
//                Glide.with(act).load(item.cMsg!!.image.uri).dontAnimate()
//                    .skipMemoryCache(true)
//                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
//                    .into(holder.ivLeftImg)
                    Glide.with(act)
                        .asBitmap()
                        .load(Constants.baseUrlImage + item.cMsg!!.image.uri).dontAnimate()
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onResourceReady(
                                resource: Bitmap, transition: Transition<in Bitmap>?
                            ) {
                                holder.ivLeftImg.setImageBitmap(resource)
//                            resource.width
//                            holder.ivLeftImg.width
//                            holder.ivLeftImg.measuredHeight
                            }

                            override fun onLoadCleared(placeholder: Drawable?) {}
                        })
                }
            }
        }
    }

    override
    fun getItemViewType(position: Int) : Int{

        if (msgList == null) {
            return TYPE_Text
        }

        val obj = msgList!![position]

        if (obj.isTipMsg){
            return TYPE_Tip
        }
        else if (obj.isLastLine){
            return TYPE_LastLine
        }
        else if (obj.isQA){
            return TYPE_QA
        }
        obj.cMsg?.apply {
            return if (this.hasImage()){
                return TYPE_Image
            } else {
                return TYPE_Text
            }
        }
        return TYPE_Text
    }

    override fun getItemCount(): Int {
        return if (msgList == null) 1 else msgList!!.size
    }

    inner class HeaderViewHolder(binding: ItemHeaderRecyleviewBinding) : RecyclerView.ViewHolder(binding.root){
        var rcvQa = binding.rcvQa
        var tvTitle = binding.tvTitle
        init {
            // 初始化自动回复列表
            rcvQa.layoutManager = LinearLayoutManager(act)
            qaAdapter = GroupedQAdapter(act, ArrayList(), null)
            rcvQa.adapter = qaAdapter

            val param = JsonObject()
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
                        res.data.autoReplyItem?.qa?.let {
                            qaAdapter.setDataList(it)
                            tvTitle.visibility = View.VISIBLE
                        }
                    } override fun onError(e: ApiException?) {
                        super.onError(e)
                        println(e)
                    }
                }
            )

            // 提问列表点击事件
            qaAdapter.setOnHeaderClickListener { _, _, groupPosition ->
                if (qaAdapter.isExpand(groupPosition)) {
                    qaAdapter.collapseGroup(groupPosition)
                } else {
                    qaAdapter.collapseTheResetGroup(groupPosition)
                    qaAdapter.expandGroup(groupPosition)
                }
            }

            // 问题点击事件
            qaAdapter.setOnChildClickListener { _, _, groupPosition, childPosition ->
                val answerTxt = qaAdapter.data.get(groupPosition).related?.get(childPosition)?.content ?:"null"
                val questionTxt = qaAdapter.data.get(groupPosition).related?.get(childPosition)?.question?.content?.data ?:""
                // 发送提问消息
                listener?.onSendLocalMsg(questionTxt, false)
                // 自动回答
                listener?.onSendLocalMsg(answerTxt, true)
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
                .watchView(tvRightMsg)
            tvRightMsg.setOnLongClickListener(OnLongClickListener {
                builder2.asAttachList(
                    //"撤回", "编辑" 前端App不需要
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
        }
    }

    inner class TipMsgViewHolder(binding: ItemTipMsgBinding) : RecyclerView.ViewHolder(binding.root) {
        val tvTitle = binding.tvTitle
    }

    inner class ItemLastLineViewHolder(binding: ItemLastLineBinding) : RecyclerView.ViewHolder(binding.root) {
        val tvTitle = binding.tvTitle
    }

}