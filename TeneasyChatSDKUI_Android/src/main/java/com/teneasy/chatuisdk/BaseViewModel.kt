package com.teneasy.chatuisdk

import android.os.Handler
import android.os.Looper
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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.internal.http2.Http2Reader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

open class BaseViewModel : ViewModel() {

    private val TAG = "BaseViewModel"
    fun logError(code: Int, request: String, header: String, resp: String,  url: String) {
        // 无可用线路是大事件，需要上报
        var errorItem = ErrorItem(url, code, "", 2, "")
        errorItem.code = code
        errorItem.url = url
        errorItem.tenantId = Constants.merchantId
            // Platform_IOS: 1; Platform_ANDROID: 2; Platform_H5: 4;
        errorItem.platform = 2
        errorItem.created_at = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+08:00", Locale.getDefault()).apply {
                timeZone = TimeZone.getDefault()
            //timeZone = TimeZone.getTimeZone("GMT")
            }.format(Date())

        val errorPayload = ErrorPayload().apply {
            this.request = request
            this.body = resp
            this.header = header
        }

        var bodyStr = Gson().toJson(errorPayload)
        Log.d(TAG, "bodyStr: $bodyStr")
        errorItem.payload = bodyStr

        if (errorReport.data.size > 0) {
            //避免太多重复的日志，最新1条的日志，跟数组里面的最后一条做比较，如果不同，则添加
            if (errorItem.url != errorReport.data[0].url && errorItem.code != errorReport.data[0].code){
                errorReport.data.add(errorItem)
            }
        }else{
            errorReport.data.add(errorItem)

//            val handler = Handler(Looper.getMainLooper())
//            handler.postDelayed({
//                reportError(errorReport)
//            }, 3000) // 500 milliseconds delay

            GlobalScope.launch {
                delay(3000) // Non-blocking delay
                reportError(errorReport)
            }
        }
    }

    //获取咨询列表之后调用，清除未读数
    fun reportError(error: ErrorReport) {
        val request = XHttp.custom().accessToken(false)
//        if (Constants.xToken.length > 0){
//            request.headers("X-Token", Constants.xToken)
//        }else {
//            request.headers("X-Token", Constants.cert)
//        }

        var errorStr = Gson().toJson(error)
        Log.d(TAG, "errorReport: $errorStr")

        request.headers("x-trace-id", UUID.randomUUID().toString())
        request.call(request.create(MainApi.IMainTask::class.java)
            .reportError(error),
            object : ProgressLoadingCallBack<ReturnData<Any>>(null) {
                override fun onSuccess(res: ReturnData<Any>) {
                    errorReport.data.clear()
                    Log.d("reportError", "上报成功")
                }
                override fun onError(e: ApiException?) {
                    super.onError(e)
                    println(e)
                }
            })
    }
}