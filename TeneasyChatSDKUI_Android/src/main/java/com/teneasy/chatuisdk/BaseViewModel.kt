package com.teneasy.chatuisdk

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.teneasy.chatuisdk.ui.base.Constants
import com.teneasy.chatuisdk.ui.base.Constants.Companion.errorReport
import com.teneasy.chatuisdk.ui.http.MainApi
import com.teneasy.chatuisdk.ui.http.ReturnData
import com.teneasy.chatuisdk.ui.http.bean.ErrorItem
import com.teneasy.chatuisdk.ui.http.bean.ErrorPayload
import com.teneasy.chatuisdk.ui.http.bean.ErrorReport
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.callback.ProgressLoadingCallBack
import com.xuexiang.xhttp2.exception.ApiException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

open class BaseViewModel : ViewModel() {

    fun logError(code: Int, request: String, header: String, resp: String,  url: String) {
        // 无可用线路是大事件，需要上报
        var errorItem = ErrorItem(url, code, "", 1, "")
        errorItem.code = code
        errorItem.url = url
            // Platform_IOS: 1; Platform_ANDROID: 2; Platform_H5: 4;
        errorItem.platform = 1
        errorItem.created_at = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+08:00", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("GMT")
            }.format(Date())

        val errorPayload = ErrorPayload().apply {
            this.request = request
            this.body = resp
            this.header = header
        }

        var bodyStr = Gson().toJson(errorPayload)
        errorItem.payload = bodyStr

        errorReport.data.add(errorItem)

        reportError(errorReport)
    }

    //获取咨询列表之后调用，清除未读数
    fun reportError(error: ErrorReport) {
        val request = XHttp.custom().accessToken(false)
        //这里需要用cert
        if (Constants.xToken.length > 0){
            request.headers("X-Token", Constants.xToken)
        }else {
            request.headers("X-Token", Constants.cert)
        }
        request.headers("x-trace-id", UUID.randomUUID().toString())
        request.call(request.create(MainApi.IMainTask::class.java)
            .reportError(error),
            object : ProgressLoadingCallBack<ReturnData<Any>>(null) {
                override fun onSuccess(res: ReturnData<Any>) {
                    Log.d("reportError", "上报成功")
                }
                override fun onError(e: ApiException?) {
                    super.onError(e)
                    println(e)
                }
            })
    }
}