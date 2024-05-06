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

class SelectConsultTypeViewModel : ViewModel() {
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun startLoading() {
        _isLoading.value = true
    }

    fun stopLoading() {
        _isLoading.value = false
    }
    private val _data = MutableLiveData<List<String>>()
    val data: LiveData<List<String>>
        get() = _data

    fun setData(data: List<String>) {
        _data.value = data
    }

    //query-entrance
    private fun queryEntrance(consultId: Int) {
        startLoading()
        val param = JsonObject()

        param.addProperty("consultId", consultId)
        val request = XHttp.custom().accessToken(false)
        request.headers("X-Token", Constants.httpToken)
        request.call(request.create(MainApi.IMainTask::class.java)
            .workerInfo(param),
            object : ProgressLoadingCallBack<ReturnData<WorkerInfo>>(null) {
                override fun onSuccess(res: ReturnData<WorkerInfo>) {

                }
            })
    }
}