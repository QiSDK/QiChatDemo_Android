package com.teneasy.chatuisdk

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.JsonObject
import com.teneasy.chatuisdk.ui.base.Constants
import com.teneasy.chatuisdk.ui.base.Constants.Companion.errorReport
import com.teneasy.chatuisdk.ui.http.MainApi
import com.teneasy.chatuisdk.ui.http.ReturnData
import com.teneasy.chatuisdk.ui.http.bean.DataItem
import com.teneasy.chatuisdk.ui.http.bean.ErrorItem
import com.teneasy.chatuisdk.ui.http.bean.ErrorReport
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.callback.ProgressLoadingCallBack
import com.xuexiang.xhttp2.exception.ApiException

class SelectConsultTypeViewModel : BaseViewModel() {
     var consultList = MutableLiveData<ArrayList<Consults>>()
    //获取咨询类型列表
    fun queryEntrance() {
        val param = JsonObject()
        val request = XHttp.custom().accessToken(false)
        //这里需要用cert
        if (Constants.xToken.length > 0){
            request.headers("X-Token", Constants.xToken)
        }else {
            request.headers("X-Token", Constants.cert)
        }
        request.call(request.create(MainApi.IMainTask::class.java)
            .queryEntrance(param),
            object : ProgressLoadingCallBack<ReturnData<Entrance>>(null) {
                override fun onSuccess(res: ReturnData<Entrance>) {
                    consultList.value = res.data.consults
                }

                override fun onError(e: ApiException?) {
                    super.onError(e)
                    consultList.value = ArrayList()

                   var errorItem =  ErrorItem(request.url.toString(), 0, "", 3, "")))
                    errorReport.data.add(errorItem)
                    reportError(errorReport)
                }
            })
    }

    //获取咨询列表之后调用，清除未读数
    fun markRead() {
       // startLoading()
        val request = XHttp.custom().accessToken(false)
        val param = JsonObject()
        param.addProperty("consultId", Constants.CONSULT_ID)
        //这里需要用cert
        if (Constants.xToken.length > 0){
            request.headers("X-Token", Constants.xToken)
        }else {
            request.headers("X-Token", Constants.cert)
        }
        request.call(request.create(MainApi.IMainTask::class.java)
            .markRead(param),
            object : ProgressLoadingCallBack<ReturnData<Any>>(null) {
                override fun onSuccess(res: ReturnData<Any>) {
                    Log.d("Consult_ChatLib", "清零成功")
                }

                override fun onError(e: ApiException?) {
                    super.onError(e)
                    println(e)
                }
            })
    }



}