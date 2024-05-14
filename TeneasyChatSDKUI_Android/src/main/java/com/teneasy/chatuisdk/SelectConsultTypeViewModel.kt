package com.teneasy.chatuisdk

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.JsonObject
import com.teneasy.chatuisdk.ui.base.Constants
import com.teneasy.chatuisdk.ui.http.MainApi
import com.teneasy.chatuisdk.ui.http.ReturnData
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.callback.ProgressLoadingCallBack
import com.xuexiang.xhttp2.exception.ApiException

class SelectConsultTypeViewModel : ViewModel() {
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun startLoading() {
        _isLoading.value = true
    }

    fun stopLoading() {
        _isLoading.value = false
    }
     var consultList = MutableLiveData<ArrayList<Consults>>()
//    val consultList: LiveData<ArrayList<Consults>>
//        get() = _consultList
//
//    fun setData(data: ArrayList<Consults>) {
//        _consultList.value = data
//    }

    //query-entrance
    fun queryEntrance() {
        startLoading()
        val param = JsonObject()
        val request = XHttp.custom().accessToken(false)
        //这里需要用cert
        request.headers("X-Token", Constants.cert)
        request.call(request.create(MainApi.IMainTask::class.java)
            .queryEntrance(param),
            object : ProgressLoadingCallBack<ReturnData<Entrance>>(null) {
                override fun onSuccess(res: ReturnData<Entrance>) {
                    consultList.value = res.data.consults
                }

                override fun onError(e: ApiException?) {
                    super.onError(e)
                    consultList.value = ArrayList()
                    println(e)
                }
            })
    }
}