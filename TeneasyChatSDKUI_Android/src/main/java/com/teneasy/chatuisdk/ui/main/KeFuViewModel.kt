package com.teneasy.chatuisdk.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.JsonObject
import com.google.protobuf.Timestamp
import com.teneasy.chatuisdk.R
import com.teneasy.chatuisdk.ui.base.Constants
import com.teneasy.chatuisdk.ui.http.MainApi
import com.teneasy.chatuisdk.ui.http.ReturnData
import com.teneasy.chatuisdk.ui.http.bean.AssignWorker
import com.teneasy.chatuisdk.ui.http.bean.AutoReplyItem
import com.teneasy.chatuisdk.ui.http.bean.WorkerInfo
import com.teneasy.sdk.ui.MessageItem
import com.teneasyChat.api.common.CMessage
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.callback.ProgressLoadingCallBack
import com.xuexiang.xhttp2.exception.ApiException
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
    val mlAutoReplyItem = MutableLiveData<AutoReplyItem>()
    val mlAssignWorker = MutableLiveData<AssignWorker>()
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

    fun addMsgItem(newItem: MessageItem, payLoadId: Long) {
        newItem.payLoadId = payLoadId
        mlMsgList.value?.add(newItem)
        mlMsgList.postValue(mlMsgList.value)
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

    /**
     * 通过workerId加载客服信息
     * @param workerId
     */
    fun loadWorker(workerId: Int) {
        val param = JsonObject()
        param.addProperty("workerId", workerId)
        val request = XHttp.custom().accessToken(false)
        request.headers("X-Token", Constants.httpToken)
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
    }

    /**
     * 通过选择的consultId分配客服
     * @param consultId
     */
    fun assignWorker(consultId: Int) {
        val param = JsonObject()
        param.addProperty("consultId", consultId)
        val request = XHttp.custom().accessToken(false)
        request.headers("X-Token", Constants.httpToken)
        request.call(request.create(MainApi.IMainTask::class.java)
            .assignWorker(param),
            object : ProgressLoadingCallBack<ReturnData<AssignWorker>>(null) {
                override fun onSuccess(res: ReturnData<AssignWorker>) {
                    mlAssignWorker.postValue(res.data)
                } override fun onError(e: ApiException?) {
                    super.onError(e)
                    println(e)
                }
            }
        )
    }

    /**
     * 通过选择的consultId获取自动回复
     * @param consultId
     */
    fun queryAutoReply(consultId: Int) {
        val param = JsonObject()
        param.addProperty("consultId", consultId)
        val request = XHttp.custom().accessToken(false)
        request.headers("X-Token", Constants.httpToken)
        request.call(request.create(MainApi.IMainTask::class.java)
            .queryAutoReply(param),
            object : ProgressLoadingCallBack<ReturnData<AutoReplyItem>>(null) {
                override fun onSuccess(res: ReturnData<AutoReplyItem>) {
                    mlAutoReplyItem.postValue(res.data)
                } override fun onError(e: ApiException?) {
                    super.onError(e)
                    println(e)
                }
            }
        )
    }

}