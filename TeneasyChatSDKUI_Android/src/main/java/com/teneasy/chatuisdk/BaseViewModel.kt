package com.teneasy.chatuisdk

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.JsonObject
import com.teneasy.chatuisdk.ui.base.Constants
import com.teneasy.chatuisdk.ui.http.MainApi
import com.teneasy.chatuisdk.ui.http.ReturnData
import com.teneasy.chatuisdk.ui.http.bean.ErrorReport
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.callback.ProgressLoadingCallBack
import com.xuexiang.xhttp2.exception.ApiException

open class BaseViewModel : ViewModel() {
    //获取咨询列表之后调用，清除未读数
    fun reportError(error: ErrorReport) {
        val request = XHttp.custom().accessToken(false)
        //这里需要用cert
        if (Constants.xToken.length > 0){
            request.headers("X-Token", Constants.xToken)
        }else {
            request.headers("X-Token", Constants.cert)
        }
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