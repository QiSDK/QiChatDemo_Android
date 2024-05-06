package com.teneasy.chatuisdk

import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.gson.JsonObject
import com.teneasy.chatuisdk.ui.base.Constants
import com.teneasy.chatuisdk.ui.http.MainApi
import com.teneasy.chatuisdk.ui.http.ReturnData
import com.teneasy.chatuisdk.ui.http.bean.WorkerInfo
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
    fun queryEntrance(consultId: Int) {
        startLoading()
        val param = JsonObject()

        param.addProperty("consultId", consultId)
        val request = XHttp.custom().accessToken(false)
        request.headers("X-Token", Constants.httpToken)
        request.call(request.create(MainApi.IMainTask::class.java)
            .queryEntrance(param),
            object : ProgressLoadingCallBack<ReturnData<Entrance>>(null) {
                override fun onSuccess(res: ReturnData<Entrance>) {
                 //  setData(res.data.consults)
                    consultList.value = res.data.consults
                }

                override fun onError(e: ApiException?) {
                    super.onError(e)
                    println(e)
                }
            })
    }
}