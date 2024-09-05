package com.teneasy.chatuisdk.ui.main

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.JsonObject
import com.google.protobuf.Timestamp
import com.teneasy.chatuisdk.R
import com.teneasy.chatuisdk.ui.base.Constants
import com.teneasy.chatuisdk.ui.base.Constants.Companion.CONSULT_ID
import com.teneasy.chatuisdk.ui.base.Constants.Companion.unSentMessage
import com.teneasy.chatuisdk.ui.base.Utils
import com.teneasy.chatuisdk.ui.http.MainApi
import com.teneasy.chatuisdk.ui.http.ReturnData
import com.teneasy.chatuisdk.ui.http.bean.AssignWorker
import com.teneasy.chatuisdk.ui.http.bean.AutoReply
import com.teneasy.chatuisdk.ui.http.bean.AutoReplyItem
import com.teneasy.chatuisdk.ui.http.bean.ChatHistory.ChatHistory
import com.teneasy.chatuisdk.ui.http.bean.ChatHistory.list
import com.teneasy.chatuisdk.ui.http.bean.WorkerInfo
import com.teneasy.sdk.ChatLib
import com.teneasy.sdk.TimeUtil
import com.teneasy.sdk.ui.CellType
import com.teneasy.sdk.ui.MessageItem
import com.teneasy.sdk.ui.MessageSendState
import com.teneasyChat.api.common.CMessage
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.callback.ProgressLoadingCallBack
import com.xuexiang.xhttp2.exception.ApiException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.internal.filterList
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
    val mlWorkerInfo = MutableLiveData<WorkerInfo>()
    val mlAutoReplyItem = MutableLiveData<AutoReplyItem?>()
    val mlAssignWorker = MutableLiveData<AssignWorker>()
    val mlMsgMap = MutableLiveData<HashMap<Long, MessageItem>?>()
    val mHistoryList = MutableLiveData<List<list>?>()

    var mReplyList = ArrayList<list>()
    val TAG = "ChatLibViewModel"
    val mlNewWorkAssigned = MutableLiveData<Boolean>()

    init {
        mlSendMsg.value = ""
        mlTitle.value = ""
        mlExprIcon.value = R.drawable.h5_biaoqing
        mlMsgTypeTxt.value = true
        mlBtnSendVis.value = false
        mlMsgList.value = ArrayList()
        mlMsgMap.value = hashMapOf()
    }

    fun addMsgItem(newItem: MessageItem, payLoadId: Long) {
        newItem.payLoadId = payLoadId

        if (newItem.cMsg?.video != null && newItem.cMsg!!.video.uri.isNotEmpty()){
            newItem.cellType = CellType.TYPE_VIDEO
        }else if  (newItem.cMsg?.image != null && newItem.cMsg!!.image.uri.isNotEmpty()){
            newItem.cellType = CellType.TYPE_Image
        }else{
            //对方已经编辑信息，做对应个更新
            if (newItem.cMsg?.msgOp == CMessage.MessageOperate.MSG_OP_EDIT){
                var index = mlMsgList.value?.indexOfFirst { it.cMsg?.msgId == newItem.cMsg?.msgId}
                if (index != null && index != -1){
                    mlMsgList.value!![index].cMsg = newItem.cMsg
                    mlMsgList.postValue(mlMsgList.value)
                    return
                }
            }
             newItem.cellType = CellType.TYPE_Text
        }

        mlMsgList.value?.add(newItem)
        mlMsgList.postValue(mlMsgList.value)
    }

    fun addAllMsgItem(newItems: List<MessageItem>) {
        mlMsgList.value?.addAll(newItems)
        mlMsgList.postValue(mlMsgList.value)
    }

    fun removeMsgItem(payLoadId: Long, msgId: Long){
        val newList = mlMsgList.value?.toMutableList() ?: mutableListOf()
        for (item in newList) {
            if (item.payLoadId == payLoadId || item.cMsg?.msgId == msgId) {
                newList.remove(item)
                mlMsgList.postValue(newList as ArrayList<MessageItem>?)
                return
            }
        }
    }

    fun removeMsgItem(messageItem: MessageItem){
        mlMsgList.value?.remove(messageItem)
        mlMsgList.postValue(mlMsgList.value)
    }

    /**
    * 通过指定的图片地址，创建图片消息实体。（该方法并未调用chatLib发送消息）。
    * @param imgPath
    * @param isLeft
    */
    //撰写一条图片信息
    fun composeImgMsg(history: list?, isLeft: Boolean, imgPath: String = "") : MessageItem{
        var cMsg = CMessage.Message.newBuilder()
        //cMsg.consultId = history?.consultID ?: Constants.CONSULT_ID
        var cMContent = CMessage.MessageImage.newBuilder()

        var d = Timestamp.newBuilder()
        val cal = Calendar.getInstance()
        if (history?.msgTime != null) {
            cal.time = Utils().convertStrToDate(history.msgTime)
        }else{
            cal.time = Date()
        }
        val millis = cal.timeInMillis
        d.seconds = (millis * 0.001).toLong()
        d.nanos = System.nanoTime().toInt();
        cMsg.msgTime = d.build()

        if (!imgPath.isEmpty()){
            cMContent.uri = imgPath
        }else {
            cMContent.uri = history?.image?.uri ?: ""
        }
        cMsg.setImage(cMContent)

        cMsg.msgId = (history?.msgId?: "0").toLong()

        var chatModel = MessageItem()
        chatModel.cMsg = cMsg.build()
        chatModel.cellType = CellType.TYPE_Image
        //chatModel.imgPath = imgPath
        chatModel.isLeft = isLeft
        chatModel.sendStatus = MessageSendState.发送成功

        return chatModel
    }

    /**
     * 通过视频地址，创建图片消息实体。（该方法并未调用chatLib发送消息）。
     * @param imgPath
     * @param isLeft
     */
    //撰写一条图片信息
    fun composeVideoMsg(history: list?, isLeft: Boolean, videoPath: String = "") : MessageItem{
        var cMsg = CMessage.Message.newBuilder()
        var cMContent = CMessage.MessageVideo.newBuilder()

        var d = Timestamp.newBuilder()
        val cal = Calendar.getInstance()
        if (history?.msgTime != null) {
            cal.time = Utils().convertStrToDate(history.msgTime)
        }else{
            cal.time = Date()
        }
        val millis = cal.timeInMillis
        d.seconds = (millis * 0.001).toLong()
        d.nanos = System.nanoTime().toInt();
        cMsg.msgTime = d.build()

        cMsg.msgId = (history?.msgId?: "0").toLong()

        if (!videoPath.isEmpty()){
            cMContent.uri = videoPath
        }
        else if (history?.video != null) {
            cMContent.uri = history.video.uri
        }else{
            cMContent.uri = ""
        }
        cMsg.setVideo(cMContent)

        var chatModel = MessageItem()
        chatModel.cMsg = cMsg.build()
        //chatModel.imgPath = imgPath
        chatModel.isLeft = isLeft
        chatModel.sendStatus = MessageSendState.发送成功

        return chatModel
    }

    fun composeTextMsg(history: list,  isLeft: Boolean) : MessageItem{
        var chatModel = MessageItem()

        var cMsg = CMessage.Message.newBuilder()
        var cMContent = CMessage.MessageContent.newBuilder()

        cMsg.msgTime = Utils().getNowTimeStamp()

        //回复消息
        val replyMsgId = (history.replyMsgId?: "0").toLong()
        if (replyMsgId > 0){
            var replyText = history.content?.data?: "no txt"
            val oriMsg =  mReplyList.firstOrNull { it.msgId == replyMsgId.toString() }
            if (oriMsg != null){
                if (oriMsg.msgFmt == "MSG_TEXT"){
                    replyText = "$replyText\n回复：${oriMsg.content?.data}"
                }else if (oriMsg.msgFmt == "MSG_IMAGE"){
                    replyText = "$replyText\n回复：[图片]"
                }else if (oriMsg.msgFmt == "MSG_VIDEO"){
                    replyText = "$replyText\n回复：[视频]"
                }
            }
            cMContent.data = replyText
        }
        else if (history.workerChanged != null){
            cMContent.data = history.workerChanged.greeting
            chatModel.cellType = CellType.TYPE_Tip
        }
        else if (history.content != null) {
            cMContent.data = history.content.data
        }else{
            cMContent.data = "历史消息"
        }
        cMsg.setContent(cMContent)
        cMsg.msgId = (history.msgId?: "0").toLong()
        chatModel.cMsg = cMsg.build()
        chatModel.isLeft = isLeft
        chatModel.sendStatus = MessageSendState.发送成功

        //mlMsgList.value?.add(chatModel)
        //mlMsgList.postValue(mlMsgList.value)
        return chatModel
    }


    fun composeLocalMsg(text: String, isLeft: Boolean, isTip: Boolean = false, msgId: Long = 0) : MessageItem{
        var cMsg = CMessage.Message.newBuilder()
        var cMContent = CMessage.MessageContent.newBuilder()

        cMsg.msgTime = Utils().getNowTimeStamp()
        cMsg.msgId = msgId
        cMContent.data = text.trim()
        cMsg.setContent(cMContent)

        var chatModel = MessageItem()
        chatModel.cMsg = cMsg.build()
        chatModel.isLeft = isLeft

        if (isTip) {
            chatModel.cellType = CellType.TYPE_Tip
        }

        mlMsgList.value?.add(chatModel)
        mlMsgList.postValue(mlMsgList.value)
        return chatModel
    }

    /**
     * 通过workerId加载客服信息
     * @param workerId
     */
   /* fun loadWorker(workerId: Int) {
        val param = JsonObject()
        param.addProperty("workerId", workerId)
        val request = XHttp.custom().accessToken(false)
        request.headers("X-Token", Constants.xToken)
        request.call(request.create(MainApi.IMainTask::class.java)
            .workerInfo(param),
            object : ProgressLoadingCallBack<ReturnData<WorkerInfo>>(null) {
                override fun onSuccess(res: ReturnData<WorkerInfo>) {
                    mlWorkerInfo.postValue(res.data)
                } override fun onError(e: ApiException?) {
                    super.onError(e)
                    println(e)
                }

            }
        )
    }*/

    /**
     * 通过选择的consultId分配客服
     * @param consultId
     */
    fun assignWorker(consultId: Long) {
        Log.d(TAG, "assignWorker(consultId: Long):" + consultId)
        val param = JsonObject()
        param.addProperty("consultId", consultId)
        val request = XHttp.custom().accessToken(false)
        request.headers("X-Token", Constants.xToken)
        request.call(request.create(MainApi.IMainTask::class.java)
            .assignWorker(param),
            object : ProgressLoadingCallBack<ReturnData<AssignWorker>>(null) {
                override fun onSuccess(res: ReturnData<AssignWorker>) {
                    mlAssignWorker.postValue(res.data)
                    print(res.data)
                } override fun onError(e: ApiException?) {
                    super.onError(e)
                    println(e)
                    Log.d(TAG, "assignWorker error")
                }
            }
        )
    }

    /**
     * 跟上个消息比较，如果超过5分钟，重新分配客服
     * @param consultId
     */
    fun assignNewWorker(consultId: Long) {
        val param = JsonObject()
        param.addProperty("consultId", consultId)
        val request = XHttp.custom().accessToken(false)
        request.headers("X-Token", Constants.xToken)
        request.call(request.create(MainApi.IMainTask::class.java)
            .assignWorker(param),
            object : ProgressLoadingCallBack<ReturnData<AssignWorker>>(null) {
                override fun onSuccess(res: ReturnData<AssignWorker>) {
                    mlNewWorkAssigned.postValue(true)
                } override fun onError(e: ApiException?) {
                    super.onError(e)
                    mlNewWorkAssigned.postValue(false)
                    println(e)
                }
            }
        )
    }

    /**
     * 通过选择的consultId获取自动回复
     * @param consultId
     */
    fun queryAutoReply(consultId: Long, workerId: Int) {
        val param = JsonObject()
        param.addProperty("consultId", consultId)
        param.addProperty("workerId", workerId)
        val request = XHttp.custom().accessToken(false)
        request.headers("X-Token", Constants.xToken)
        request.call(request.create(MainApi.IMainTask::class.java)
            .queryAutoReply(param),
            object : ProgressLoadingCallBack<ReturnData<AutoReply>>(null) {
                override fun onSuccess(res: ReturnData<AutoReply>) {
                        if (res.code != 0 || res.data == null || res.data.autoReplyItem == null){
                            Log.d("AdapterNChatLib", "自动回复为空")
                        }else {
                            res.data.autoReplyItem?.let {
                                mlAutoReplyItem.postValue(it)
                            }
                        }
                } override fun onError(e: ApiException?) {
                    super.onError(e)
                    println(e)
                }
            }
        )
    }


    /*
    {
  "chatId": "0",
  "msgId": "0",
  "count": 0,
  "withLastOne": true,
  "workerId": 0,
  "consultId": 0,
  "userId": 0
}
 */
    fun queryChatHistory(consultId: Long) {
        //var param = Request(0, "0", 0, true, 0, consultId, 0)
        val param = JsonObject()
        param.addProperty("consultId", consultId)
        param.addProperty("chatId", 0)
        param.addProperty("count", 50)
        param.addProperty("userId", Constants.userId)
        val request = XHttp.custom().accessToken(false)
        request.headers("X-Token", Constants.xToken)
        request.call(request.create(MainApi.IMainTask::class.java)
            .queryChatHistory(param),
            object : ProgressLoadingCallBack<ReturnData<ChatHistory>>(null) {
                override fun onSuccess(res: ReturnData<ChatHistory>) {
                    res.data?.list?.let {
                        mHistoryList.postValue(it)
                    }

                    res.data?.replyList?.let {
                        mReplyList = it as ArrayList<list>
                    }
                } override fun onError(e: ApiException?) {
                    super.onError(e)
                    Log.d(TAG, e.toString())
                }
            }
        )
    }

    fun handleUnSendMsg(list: ArrayList<MessageItem>, chatLib: ChatLib): Boolean {
        unSentMessage[CONSULT_ID]?.let {
            Log.i(TAG, "handleUnSendMsg: " + it.size)
            for (item in it) {
                if (item.sendStatus != MessageSendState.发送成功) {
                    runBlocking {
                        launch {
                            delay(300L)
                            item.cMsg?.let {
                                Log.i(TAG, "resend payloadId:" + item.payLoadId)
                                chatLib.resendMSg(it, item.payLoadId)
                            }
                        }
                    }
                }
            }
            unSentMessage[CONSULT_ID] = java.util.ArrayList()
        }


        return true
    }

    fun getUnSendMsg(){
        if (mlMsgList.value?.size == 0){
            return
        }
        val filteredList =
            mlMsgList.value?.filter { it.sendStatus != MessageSendState.发送成功 && it.isLeft == false }

        unSentMessage[CONSULT_ID] = filteredList as ArrayList<MessageItem>
        Log.i(TAG, "getUnSendMsg: " + filteredList.size)
    }

}