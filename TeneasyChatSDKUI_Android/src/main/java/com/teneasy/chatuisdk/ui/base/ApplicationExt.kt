package com.teneasy.chatuisdk.ui.base

import android.app.Application
import android.content.Context
import com.xuexiang.xhttp2.XHttpSDK

class ApplicationExt: Application(){
    companion object {
        var context: Context? = null
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        initXHttp2(this)
    }


    private fun initXHttp2(application: Application) {
        //初始化网络请求框架，必须首先执行
        XHttpSDK.init(application)
        //需要调试的时候执行
//        if (MyApp.isDebug()) {
        XHttpSDK.debug()
//        }
        //设置网络请求的全局基础地址
        XHttpSDK.setBaseUrl(Constants.baseUrl)
    }
}