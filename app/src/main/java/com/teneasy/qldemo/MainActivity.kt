package com.teneasy.qldemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.teneasy.chatuisdk.TeneasyChatUISDK


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        TeneasyChatUISDK.showNetworkLogButton(application)

        print("ddddd")
    }

}