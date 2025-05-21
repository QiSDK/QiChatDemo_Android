package com.teneasy.chatuisdk.ui.main

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.protobuf.Timestamp
import com.teneasy.chatuisdk.BaseViewModel
import com.teneasy.chatuisdk.R
import com.teneasy.chatuisdk.ui.base.Constants
import com.teneasy.chatuisdk.ui.base.Constants.Companion.CONSULT_ID
import com.teneasy.chatuisdk.ui.base.Constants.Companion.chatId
import com.teneasy.chatuisdk.ui.base.Constants.Companion.unSentMessage
import com.teneasy.chatuisdk.ui.base.Utils
import com.teneasy.chatuisdk.ui.http.MainApi
import com.teneasy.chatuisdk.ui.http.ReturnData
import com.teneasy.chatuisdk.ui.http.bean.AssignWorker
import com.teneasy.chatuisdk.ui.http.bean.AutoReply
import com.teneasy.chatuisdk.ui.http.bean.AutoReplyItem
import com.teneasy.chatuisdk.ui.http.bean.ChatHistory.ChatHistory
import com.teneasy.chatuisdk.ui.http.bean.ChatHistory.hMessage
import com.teneasy.chatuisdk.ui.http.bean.ReplyList
import com.teneasy.chatuisdk.ui.http.bean.TextImages
import com.teneasy.chatuisdk.ui.http.bean.WorkerInfo
import com.teneasy.sdk.ChatLib
import com.teneasy.sdk.ui.CellType
import com.teneasy.sdk.ui.MessageItem
import com.teneasy.sdk.ui.MessageSendState
import com.teneasy.sdk.ui.ReplyMessageItem
import com.teneasyChat.api.common.CMessage
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.callback.ProgressLoadingCallBack
import com.xuexiang.xhttp2.exception.ApiException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import java.util.Date
import java.util.UUID

/**
 * 客户界面的viewModel，主要UI层的数据。例如：socket消息发送、聊天数据。
 */
class KeFuViewModel : BaseViewModel() {
    companion object {
        private const val TAG = "KeFuViewModel"
    }

    // LiveData 数据
    val mlSendMsg = MutableLiveData("")
    val mlTitle = MutableLiveData("")
    val mlBtnSendVis = MutableLiveData(false)
    val mlExprIcon = MutableLiveData(R.drawable.h5_biaoqing)
    val mlMsgTypeTxt = MutableLiveData(true)
    val mlMsgList = MutableLiveData<ArrayList<MessageItem>?>(ArrayList())
    val mlWorkerInfo = MutableLiveData<WorkerInfo>()
    val mlAutoReplyItem = MutableLiveData<AutoReplyItem?>()
    val mlAssignWorker = MutableLiveData<AssignWorker>()
    val mlMsgMap = MutableLiveData<HashMap<Long, MessageItem>?>(hashMapOf())
    val mHistoryHMessage = MutableLiveData<List<hMessage>?>()
    val mHmessage = MutableLiveData<hMessage>()
    val mlNewWorkAssigned = MutableLiveData<Boolean>()
    
    // 非LiveData数据
    var mReplyHMessage = ArrayList<hMessage>()

    /**
     * 添加消息项到列表
     * @param newItem 新消息项
     * @param payLoadId 消息ID
     */
    fun addMsgItem(newItem: MessageItem, payLoadId: Long) {
        newItem.payLoadId = payLoadId

        // 根据消息类型设置cellType
        newItem.cellType = when {
            newItem.cMsg?.video != null && newItem.cMsg!!.video.uri.isNotEmpty() -> CellType.TYPE_VIDEO
            newItem.cMsg?.image != null && newItem.cMsg!!.image.uri.isNotEmpty() -> CellType.TYPE_Image
            newItem.cMsg?.file != null && newItem.cMsg!!.file.uri.isNotEmpty() -> CellType.TYPE_File
            //说明文本消息里面包含有图片的链接
            newItem.cMsg?.content?.data != null && (newItem.cMsg?.content?.data ?:"").contains("\"imgs\"") -> CellType.TYPE_Text_Images
            else -> {
                // 处理编辑消息的情况
                if (newItem.cMsg?.msgOp == CMessage.MessageOperate.MSG_OP_EDIT) {
                    val index = mlMsgList.value?.indexOfFirst { it.cMsg?.msgId == newItem.cMsg?.msgId }
                    if (index != null && index != -1) {
                        mlMsgList.value!![index].cMsg = newItem.cMsg
                        mlMsgList.postValue(mlMsgList.value)
                        return
                    }
                }
                CellType.TYPE_Text
            }
        }

        mlMsgList.value?.add(newItem)
        mlMsgList.postValue(mlMsgList.value)
    }

    /**
     * 添加多个消息项到列表
     * @param newItems 新消息项列表
     */
    fun addAllMsgItem(newItems: List<MessageItem>) {
        mlMsgList.value?.addAll(newItems)
        mlMsgList.postValue(mlMsgList.value)
    }

    /**
     * 根据ID移除消息项
     * @param payLoadId 消息负载ID
     * @param msgId 消息ID
     */
    fun removeMsgItem(payLoadId: Long, msgId: Long) {
        val index = mlMsgList.value?.indexOfFirst { it.payLoadId == payLoadId || it.cMsg?.msgId == msgId }
        if (index != null && index != -1) {
            mlMsgList.value?.removeAt(index)
            mlMsgList.postValue(mlMsgList.value)
        }
    }

    /**
     * 移除指定消息项
     * @param messageItem 要移除的消息项
     */
    fun removeMsgItem(messageItem: MessageItem) {
        mlMsgList.value?.remove(messageItem)
        mlMsgList.postValue(mlMsgList.value)
    }

    /**
     * 创建图片消息实体
     * @param history 历史消息
     * @param isLeft 是否显示在左侧
     * @param imgPath 图片路径
     * @return 消息项
     */
    fun composeImgMsg(history: hMessage?, isLeft: Boolean, imgPath: String = ""): MessageItem {
        val cMsg = CMessage.Message.newBuilder().apply {
            msgTime = Utils().strintToTimeStamp(history?.msgTime)
            msgFmt = CMessage.MessageFormat.MSG_IMG
            msgId = (history?.msgId ?: "0").toLong()
            replyMsgId = (history?.replyMsgId ?: "0").toLong()
            
            val imageBuilder = CMessage.MessageImage.newBuilder()
            imageBuilder.uri = if (imgPath.isNotEmpty()) imgPath else history?.image?.uri ?: ""
            setImage(imageBuilder)
        }

        return MessageItem().apply {
            this.cMsg = cMsg.build()
            this.cellType = CellType.TYPE_Image
            this.isLeft = isLeft
            this.sendStatus = MessageSendState.发送成功
        }
    }

    /**
     * 创建文件消息实体
     * @param history 历史消息
     * @param isLeft 是否显示在左侧
     * @param filePath 文件路径
     * @return 消息项
     */
    fun composeFileMsg(history: hMessage?, isLeft: Boolean, filePath: String = ""): MessageItem {
        val cMsg = CMessage.Message.newBuilder().apply {
            msgTime = Utils().strintToTimeStamp(history?.msgTime)
            msgFmt = CMessage.MessageFormat.MSG_FILE
            msgId = (history?.msgId ?: "0").toLong()
            replyMsgId = (history?.replyMsgId ?: "0").toLong()
            
            val fileBuilder = CMessage.MessageFile.newBuilder().apply {
                uri = if (filePath.isNotEmpty()) filePath else history?.file?.uri ?: ""
                fileName = history?.file?.fileName ?: ""
                size = (history?.file?.size ?: 0).toInt()
            }
            setFile(fileBuilder)
        }

        return MessageItem().apply {
            this.cMsg = cMsg.build()
            this.cellType = CellType.TYPE_File
            this.isLeft = isLeft
            this.sendStatus = MessageSendState.发送成功
        }
    }

    /**
     * 创建视频消息实体
     * @param history 历史消息
     * @param isLeft 是否显示在左侧
     * @param videoPath 视频路径
     * @return 消息项
     */
    fun composeVideoMsg(history: hMessage?, isLeft: Boolean, videoPath: String = ""): MessageItem {
        val cMsg = CMessage.Message.newBuilder().apply {
            msgTime = Utils().strintToTimeStamp(history?.msgTime)
            msgFmt = CMessage.MessageFormat.MSG_VIDEO
            msgId = (history?.msgId ?: "0").toLong()
            replyMsgId = (history?.replyMsgId ?: "0").toLong()
            
            val videoBuilder = CMessage.MessageVideo.newBuilder()
            if (videoPath.isNotEmpty()) {
                videoBuilder.uri = videoPath
            } else if (history?.video != null) {
                videoBuilder.uri = history.video.uri
                videoBuilder.hlsUri = history.video.hlsUri
                videoBuilder.thumbnailUri = history.video.thumbnailUri
            }
            setVideo(videoBuilder)
        }

        return MessageItem().apply {
            this.cMsg = cMsg.build()
            this.isLeft = isLeft
            this.sendStatus = MessageSendState.发送成功
        }
    }

    /**
     * 创建文本消息实体
     * @param history 历史消息
     * @param isLeft 是否显示在左侧
     * @return 消息项
     */
    fun composeTextMsg(history: hMessage, isLeft: Boolean): MessageItem {
        val chatModel = MessageItem()
        
        val cMsg = CMessage.Message.newBuilder().apply {
            msgTime = Utils().getNowTimeStamp()
            msgFmt = CMessage.MessageFormat.MSG_TEXT
            msgId = (history.msgId ?: "0").toLong()
            
            // 处理回复消息
            val replyMsgId = (history.replyMsgId ?: "0").toLong()
            this.replyMsgId = replyMsgId
            
            val contentBuilder = CMessage.MessageContent.newBuilder()
            if (replyMsgId > 0) {
                val oriMsg = mReplyHMessage.firstOrNull { it.msgId == replyMsgId.toString() }
                oriMsg?.apply {
                    chatModel.replyItem = getReplyItem(this)
                }
                contentBuilder.data = history.content?.data ?: ""
            } else if (history.workerChanged != null) {
                contentBuilder.data = history.workerChanged.greeting
                chatModel.cellType = CellType.TYPE_Tip
            } else if ((history.content?.data ?:"").contains("\"imgs\"")) {
                contentBuilder.data = history.content?.data ?: ""
                //说明文本消息里面包含有图片的链接
                chatModel.cellType = CellType.TYPE_Text_Images
            } else {
                contentBuilder.data = history.content?.data ?: "历史消息"
            }
            setContent(contentBuilder)
        }

        chatModel.cMsg = cMsg.build()
        chatModel.isLeft = isLeft
        chatModel.sendStatus = MessageSendState.发送成功
        
        return chatModel
    }

    /**
     * 创建本地消息实体
     * @param text 消息文本
     * @param isLeft 是否显示在左侧
     * @param isTip 是否为提示消息
     * @param msgId 消息ID
     * @return 消息项
     */
    fun composeLocalMsg(text: String, isLeft: Boolean, isTip: Boolean = false, msgId: Long = 0): MessageItem {
        val cMsg = CMessage.Message.newBuilder().apply {
            msgTime = Utils().getNowTimeStamp()
            this.msgId = msgId
            val contentBuilder = CMessage.MessageContent.newBuilder().apply {
                data = text.trim()
            }
            setContent(contentBuilder)
        }

        val chatModel = MessageItem().apply {
            this.cMsg = cMsg.build()
            this.isLeft = isLeft
            if (isTip) {
                this.cellType = CellType.TYPE_Tip
            }
        }

        mlMsgList.value?.add(chatModel)
        mlMsgList.postValue(mlMsgList.value)
        return chatModel
    }

    /**
     * 分配客服
     * @param consultId 咨询ID
     */
    fun assignWorker(consultId: Long) {
        Log.d(TAG, "assignWorker(consultId: Long): $consultId")
        val param = JsonObject().apply {
            addProperty("consultId", consultId)
        }
        
        val request = XHttp.custom().accessToken(false)
        val traceId = UUID.randomUUID().toString()
        request.headers("X-Token", Constants.xToken)
        request.headers("x-trace-id", traceId)

        val requestUrl = "${Constants.baseUrlApi()}/v1/api/assign-worker"
        request.call(
            request.create(MainApi.IMainTask::class.java).assignWorker(param),
            object : ProgressLoadingCallBack<ReturnData<AssignWorker>>(null) {
                override fun onSuccess(res: ReturnData<AssignWorker>) {
                    mlAssignWorker.postValue(res.data)
                    if (res.code != 0) {
                        val resp = Gson().toJson(res)
                        logError(res.code, "", "x-token ${Constants.xToken}, x-trace-id $traceId", resp, requestUrl)
                    }
                }

                override fun onError(e: ApiException?) {
                    super.onError(e)
                    logError(e?.code ?: 500, "", "x-token ${Constants.xToken}, x-trace-id $traceId", e?.message ?: "", requestUrl)
                }
            }
        )
    }

    /**
     * 分配新客服（超过5分钟后）
     * @param consultId 咨询ID
     */
    fun assignNewWorker(consultId: Long) {
        val param = JsonObject().apply {
            addProperty("consultId", consultId)
        }
        
        val request = XHttp.custom().accessToken(false)
        request.headers("X-Token", Constants.xToken)
        request.headers("x-trace-id", UUID.randomUUID().toString())
        
        request.call(
            request.create(MainApi.IMainTask::class.java).assignWorker(param),
            object : ProgressLoadingCallBack<ReturnData<AssignWorker>>(null) {
                override fun onSuccess(res: ReturnData<AssignWorker>) {
                    mlNewWorkAssigned.postValue(true)
                }
                
                override fun onError(e: ApiException?) {
                    super.onError(e)
                    mlNewWorkAssigned.postValue(false)
                    Log.e(TAG, "分配新客服失败: ${e?.message}")
                }
            }
        )
    }

    /**
     * 查询自动回复
     * @param consultId 咨询ID
     * @param workerId 客服ID
     */
    fun queryAutoReply(consultId: Long, workerId: Int) {
        val param = JsonObject().apply {
            addProperty("consultId", consultId)
            addProperty("workerId", workerId)
        }
        
        val request = XHttp.custom().accessToken(false)
        request.headers("X-Token", Constants.xToken)
        request.headers("x-trace-id", UUID.randomUUID().toString())
        
        val requestUrl = "${Constants.baseUrlApi()}/v1/api/query-auto-reply"
        request.call(
            request.create(MainApi.IMainTask::class.java).queryAutoReply(param),
            object : ProgressLoadingCallBack<ReturnData<AutoReply>>(null) {
                override fun onSuccess(res: ReturnData<AutoReply>) {
                    if (res.code != 0 || res.data == null || res.data.autoReplyItem == null) {
                        val resp = Gson().toJson(res)
                        logError(res.code, "", "x-token ${Constants.xToken}", resp, requestUrl)
                        Log.d("AdapterNChatLib", "自动回复为空")
                    } else {
                        res.data.autoReplyItem?.let {
                            mlAutoReplyItem.postValue(it)
                        }
                    }
                }
                
                override fun onError(e: ApiException?) {
                    super.onError(e)
                    Log.e(TAG, "查询自动回复失败: ${e?.message}")
                    logError(e?.code ?: 500, "", "x-token ${Constants.xToken}", e?.message ?: "", requestUrl)
                }
            }
        )
    }

    /**
     * 查询聊天历史
     * @param consultId 咨询ID
     */
    fun queryChatHistory(consultId: Long) {
        val param = JsonObject().apply {
            addProperty("consultId", consultId)
            addProperty("chatId", 0)
            addProperty("count", 50)
            addProperty("userId", Constants.userId)
        }
        
        val request = XHttp.custom().accessToken(false)
        request.headers("X-Token", Constants.xToken)
        request.headers("x-trace-id", UUID.randomUUID().toString())
        
        val requestUrl = "${Constants.baseUrlApi()}/v1/api/message/sync"
        request.call(
            request.create(MainApi.IMainTask::class.java).queryChatHistory(param),
            object : ProgressLoadingCallBack<ReturnData<ChatHistory>>(null) {
                override fun onSuccess(res: ReturnData<ChatHistory>) {
                    res.data?.list?.let {
                        mHistoryHMessage.postValue(it)
                    }
                    res.data?.replyList?.let {
                        mReplyHMessage = it as ArrayList<hMessage>
                    }
                    if (res.code != 0) {
                        val resp = Gson().toJson(res)
                        logError(res.code, "", "x-token ${Constants.xToken}", resp, requestUrl)
                    }
                }
                
                override fun onError(e: ApiException?) {
                    super.onError(e)
                    Log.e(TAG, "查询聊天历史失败: ${e?.message}")
                    logError(e?.code ?: 500, "", "x-token ${Constants.xToken}", e?.message ?: "", requestUrl)
                }
            }
        )
    }

    /**
     * 查询单条消息
     * @param msgId 消息ID
     * @param callback 回调函数
     */
    fun queryMessage(msgId: String, callback: (hMessage?) -> Unit) {
        val jsonString = """
        {
            "chatId": "$chatId",
            "msgIds": ["$msgId"]
        }"""
        
        val jsonElement = JsonParser.parseString(jsonString)
        val param = jsonElement.asJsonObject
        
        val request = XHttp.custom().accessToken(false)
        request.headers("X-Token", Constants.xToken)
        request.headers("x-trace-id", UUID.randomUUID().toString())
        
        val requestUrl = "${Constants.baseUrlApi()}/v1/api/message/reply-message/sync"
        request.call(
            request.create(MainApi.IMainTask::class.java).queryMessage(param),
            object : ProgressLoadingCallBack<ReturnData<ReplyList>>(null) {
                override fun onSuccess(res: ReturnData<ReplyList>) {
                    res.data?.replyList?.let {
                        if (it.isNotEmpty()) {
                            callback(it[0])
                        } else {
                            callback(null)
                        }
                    } ?: callback(null)
                    
                    if (res.code != 0) {
                        val resp = Gson().toJson(res)
                        logError(res.code, "", "x-token ${Constants.xToken}", resp, requestUrl)
                    }
                }
                
                override fun onError(e: ApiException?) {
                    super.onError(e)
                    Log.e(TAG, "查询消息失败: ${e?.message}")
                    callback(null)
                    logError(e?.code ?: 500, "", "x-token ${Constants.xToken}", e?.message ?: "", requestUrl)
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
                            delay(400L)
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

    fun getReplyItem(oriMsg: hMessage) : ReplyMessageItem{
        var replyItem = ReplyMessageItem()
        if (oriMsg != null){
            if (oriMsg.msgFmt == "MSG_TEXT"){
                var text = oriMsg.content?.data?:""
                if (text.contains("\"imgs\"")) {
                    val gson = Gson()
                    try {
                        val textBody: TextImages = gson.fromJson(text, TextImages::class.java)
                        text = textBody.message
                    } catch (e: Exception) {

                    }
                }
                replyItem.content = text
            }else if (oriMsg.msgFmt == "MSG_IMG"){
                replyItem.fileName = oriMsg.image?.uri?:""
            }else if (oriMsg.msgFmt == "MSG_VIDEO"){
                replyItem.fileName = oriMsg.video?.uri?:""
            }else if (oriMsg.msgFmt == "MSG_FILE"){
                replyItem.size = oriMsg?.file?.size?: 0
                //replyItem.fileName = oriMsg?.file?.fileName?:""
                replyItem.fileName = oriMsg?.file?.uri?:""
            }
        }
        return replyItem
    }

}
