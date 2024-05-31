package com.teneasy.chatuisdk.ui.main;

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.tbruyelle.rxpermissions3.RxPermissions
import com.teneasy.chatuisdk.R

/**
 * 客户activity。
 */
class KeFuActivity : AppCompatActivity() {

    var TAG_FRAGMENT = "KeFuFragment"
    private var rxPermissions: RxPermissions? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kefu)

        //获取相机的权限
        rxPermissions = RxPermissions(this)
        rxPermissions!!
            .request(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
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


    override fun onBackPressed() {
        val fragment: KeFuFragment? =
            supportFragmentManager.findFragmentByTag(TAG_FRAGMENT) as KeFuFragment?
        fragment?.exitChat()
        super.onBackPressed()
    }
}