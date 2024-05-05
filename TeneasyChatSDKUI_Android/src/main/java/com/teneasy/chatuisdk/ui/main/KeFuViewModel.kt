package com.teneasy.chatuisdk.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.protobuf.Timestamp
import com.teneasy.chatuisdk.R
import com.teneasy.sdk.ChatLib
import com.teneasy.sdk.TeneasySDKDelegate
import com.teneasy.sdk.ui.MessageItem
import com.teneasyChat.api.common.CMessage
import java.util.Calendar
import java.util.Date


/**
 * 客户界面的viewModel，主要UI层的数据。例如：socket消息发送、聊天数据。
 */
class KeFuViewModel() : ViewModel() {
    // TODO: Implement the ViewModel

    val mlSendMsg = MutableLiveData<String>()

    val mlTitle = MutableLiveData<String>()
    val mlBtnSendVis = MutableLiveData<Boolean>()
    val mlExprIcon = MutableLiveData<Int>()
    val mlMsgTypeTxt = MutableLiveData<Boolean>()

    val mlMsgList = MutableLiveData<ArrayList<MessageItem>?>()

    val mlMsgMap = MutableLiveData<HashMap<Long, MessageItem>?>()


    init {
        mlSendMsg.value = ""
        mlTitle.value = ""
        mlExprIcon.value = R.drawable.h5_biaoqing
        mlMsgTypeTxt.value = true
        mlBtnSendVis.value = false
        mlMsgList.value = ArrayList()
        mlMsgMap.value = hashMapOf()
    }

    /**
     * 往聊天界面添加一个消息，不会触发socket消息发送。该方法自动会生成消息ID（以当前时间currentTimeMillis）
     *
     */
    fun addMsgItem(data: MessageItem, payLoadId: Long) {
        val list = mlMsgList.value
        data.payLoadId = payLoadId
        list?.add(data)
        mlMsgList.value = list
        //mlMsgMap.value!![data.payLoadId] = data
    }

    fun getToken():String {
       // return chatLib.token
        return ""
    }

    /**
    * 通过指定的图片地址，创建图片消息实体。一般用于UI层对用户显示的自定义消息（该方法并未调用socket发送消息）。
    * 如需发送至后端，需获取返回的消息实体，再调用发送方法
    * @param imgPath
    * @param isLeft
    */
    //撰写一条图片信息
    fun composemodelImg(imgPath: String, isLeft: Boolean) : MessageItem{
        var cMsg = CMessage.Message.newBuilder()
        var cMContent = CMessage.MessageImage.newBuilder()

        var d = Timestamp.newBuilder()
        val cal = Calendar.getInstance()
        cal.time = Date()
        val millis = cal.timeInMillis
        d.seconds = (millis * 0.001).toLong()

        //d.t = msgDate.time
        cMsg.msgTime = d.build()
        cMContent.uri = imgPath
        cMsg.setImage(cMContent)

        var chatModel = MessageItem()
        chatModel.cMsg = cMsg.build()
        chatModel.imgPath = imgPath
        chatModel.isLeft = isLeft
        return chatModel
    }


}