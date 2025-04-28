package com.teneasy.chatuisdk

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.teneasy.chatuisdk.ui.base.Constants
import com.teneasy.chatuisdk.ui.base.Constants.Companion.xToken
import com.teneasy.chatuisdk.ui.http.MainApi
import com.teneasy.chatuisdk.ui.http.ReturnData
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.callback.ProgressLoadingCallBack
import com.xuexiang.xhttp2.exception.ApiException
import java.util.UUID

/**
 * 咨询类型选择页面的ViewModel
 * 负责处理咨询类型列表的数据获取和未读消息的处理
 */
class SelectConsultTypeViewModel : BaseViewModel() {
    companion object {
        private const val TAG = "SelectConsultTypeVM"
    }

    // LiveData
    private val _consultList = MutableLiveData<ArrayList<Consults>>()
    val consultList: LiveData<ArrayList<Consults>> = _consultList

    /**
     * 获取咨询类型列表
     * 通过API请求获取可用的咨询类型
     */
    fun queryEntrance() {
        val param = JsonObject()
        val request = createRequest()
        val requestUrl = "${Constants.baseUrlApi()}/v1/api/query-entrance"

        //这里需要用cert
        var token = Constants.cert
        if (Constants.xToken.length > 0){
            token =  Constants.xToken
        }

        request.call(
            request.create(MainApi.IMainTask::class.java).queryEntrance(param),
            object : ProgressLoadingCallBack<ReturnData<Entrance>>(null) {
                override fun onSuccess(res: ReturnData<Entrance>) {
                    _consultList.value = res.data.consults

                    // 记录非成功状态的请求
                    if (res.code != 0) {
                        val resp = Gson().toJson(res)
                        logError(res.code, "", "x-token " + token, resp, requestUrl)
                    }
                }

                override fun onError(e: ApiException?) {
                    super.onError(e)
                    _consultList.value = ArrayList()
                    logError(e?.code?:500, "", "x-token " + token, e?.message?: "", requestUrl )
                }
            }
        )
    }

    /**
     * 标记消息为已读
     * 在获取咨询列表后调用，清除未读消息数
     */
    fun markRead() {

        val request = createRequest()
        val param = JsonObject().apply {
            addProperty("consultId", Constants.CONSULT_ID)
        }
        val requestUrl = "${Constants.baseUrlApi()}/v1/api/chat/mark-read"
        if (Constants.xToken.length > 0){
            request.headers("X-Token", Constants.xToken)
        }else {
            request.headers("X-Token", Constants.cert)
        }
        request.call(
            request.create(MainApi.IMainTask::class.java).markRead(param),
            object : ProgressLoadingCallBack<ReturnData<Any>>(null) {
                override fun onSuccess(res: ReturnData<Any>) {
                    Log.d(TAG, "清除未读消息成功")
                }

                override fun onError(e: ApiException?) {
                    super.onError(e)
                    Log.e(TAG, "清除未读消息失败: ${e?.message}")
                    logError(e?.code?:500, "", "x-token " + xToken, e?.message?: "", requestUrl )
                }
            }
        )
    }

    /**
     * 创建HTTP请求实例
     * @return 配置好的XHttp请求对象
     */
    private fun createRequest() = XHttp.custom().accessToken(false).apply {
        // 设置请求头
        val token = if (Constants.xToken.isNotEmpty()) Constants.xToken else Constants.cert
        headers("X-Token", token)
        headers("x-trace-id", UUID.randomUUID().toString())
    }
}
