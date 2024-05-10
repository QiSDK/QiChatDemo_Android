package com.teneasy.chatuisdk.ui.main;

//import com.teneasy.chatuisdk.ui.utils.emoji.EmoticonTextView

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
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
import com.teneasy.chatuisdk.databinding.ItemMessageBinding
import com.teneasy.chatuisdk.ui.base.Constants
import com.teneasy.sdk.TimeUtil
import com.teneasy.sdk.ui.MessageItem
import com.teneasy.sdk.ui.MessageSendState
import java.util.*


interface MessageItemOperateListener {
    fun onDelete(position: Int)
    fun onCopy(position: Int)
    fun onReSend(position: Int)
    fun onQuote(position: Int)
}
/**
 * 聊天界面列表adapter
 */
class MessageListAdapter (myContext: Context,  listener: MessageItemOperateListener?) : RecyclerView.Adapter<MessageListAdapter.MsgViewHolder>() {
    var msgList: ArrayList<MessageItem>? = null
    var TYPE_Text : Int = 0
    val TYPE_Image : Int = 1
    val act: Context = myContext
    private var listener: MessageItemOperateListener? = listener
//    fun getList(): ArrayList<MessageItem>? {
//        return msgList
//    }

    fun setList(list: ArrayList<MessageItem>?) {
        this.msgList = list//ArrayList(list)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MsgViewHolder {
        val binding = ItemMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MsgViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MsgViewHolder, position: Int) {
        if (msgList == null) {
            return
        }
        val item = msgList!![position]

        if (item.cMsg == null){
            return
        }

        var localTime = "Time error"

        item.cMsg?.let {
            val msgDate =  Date(it.msgTime.seconds * 1000L)
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

            if(item.sendStatus != MessageSendState.发送成功) {
                holder.ivSendStatus.visibility = View.VISIBLE
            } else
                holder.ivSendStatus.visibility = View.GONE

            if (getItemViewType(position) == TYPE_Text){
                holder.tvRightMsg.visibility = View.VISIBLE
               holder.ivRightImg.visibility = View.GONE
                holder.tvRightMsg.text = item.cMsg!!.content.data
            }else{
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

            if (getItemViewType(position) == TYPE_Text){
                holder.tvLeftMsg.visibility = View.VISIBLE
                holder.ivLeftImg.visibility = View.GONE
                holder.tvLeftMsg.text = item.cMsg!!.content.data
            }else{
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
                    .into(object: CustomTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap,transition: Transition<in Bitmap>?
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

    override
    fun getItemViewType(position: Int) : Int{
        if (msgList == null) {
            return TYPE_Text
        }
        val obj = msgList!![position]
        obj.cMsg?.apply {
            return if (this.hasImage()){
                return TYPE_Image
            } else {
                return TYPE_Text
            }
        }
        // 因为要处理接收和自己发送的消息，所以单纯的判断msg是不够的。需要直接判断imgPath是否为空
//        if(obj.isSend) {
//            if(obj.imgPath != null && obj.imgPath.isNotEmpty()) {
//                return TYPE_Image
//            }
//        } else {
//        }
        return TYPE_Text
    }

    override fun getItemCount(): Int {
        return if (msgList == null) 0 else msgList!!.size
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

}