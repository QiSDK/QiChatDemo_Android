package com.teneasy.chatuisdk.ui.main;

import android.Manifest
import android.app.Application
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.tbruyelle.rxpermissions3.RxPermissions
import com.teneasy.chatuisdk.R
import com.teneasy.chatuisdk.ui.base.Constants
import com.xuexiang.xhttp2.XHttpSDK

/**
 * 客户activity。
 */
class KeFuActivity : AppCompatActivity() {

    var TAG_FRAGMENT = "KeFuFragment"
    private var rxPermissions: RxPermissions? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kefu)
        initXHttp2(application)
        if (savedInstanceState == null) {
            // transaction.replace(R.id.fragment, fragment, TAG_FRAGMENT);
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, KeFuFragment.newInstance(), TAG_FRAGMENT)
                .commitNow()
        }
        rxPermissions = RxPermissions(this)
        rxPermissions!!
            .request(Manifest.permission.CAMERA)
            .subscribe { granted ->
                if (granted) { // Always true pre-M
                    // I can control the camera now
                    Log.i(TAG_FRAGMENT, "授权摄像机")
                } else {
                    // Oups permission denied
                    Log.i(TAG_FRAGMENT, "拒绝摄像机")
                }
            }
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

    override fun onBackPressed() {
        val fragment: KeFuFragment? =
            supportFragmentManager.findFragmentByTag(TAG_FRAGMENT) as KeFuFragment?
        fragment?.exit()
        super.onBackPressed()
    }
}