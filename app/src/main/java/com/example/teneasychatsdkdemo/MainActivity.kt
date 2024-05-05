package com.example.teneasychatsdkdemo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.constraintlayout.utils.widget.MotionButton
import com.teneasy.chatuisdk.ui.main.KeFuActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        if (savedInstanceState == null) {
//            supportFragmentManager.beginTransaction()
//                .replace(R.id.container, MainFragment.newInstance())
//                .commitNow()
//        }

        val btnKeFu =  findViewById<MotionButton>(R.id.btn_send)
        btnKeFu.setOnClickListener({
            val keFuIntent = Intent(this, KeFuActivity :: class.java)
            this.startActivity(keFuIntent)
        })
    }

}