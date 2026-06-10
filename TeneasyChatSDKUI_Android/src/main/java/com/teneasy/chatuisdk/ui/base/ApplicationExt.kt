package com.teneasy.chatuisdk.ui.base

import android.app.Application
import android.content.Context
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import com.xuexiang.xhttp2.XHttpSDK

class ApplicationExt: Application(){
    companion object {
        var context: Context? = null

        @Volatile
        private var environmentReady = false

        /**
         * 幂等的环境准备：context 兜底、未读快照加载、XHttp 初始化、EmojiCompat。
         * ApplicationExt.onCreate 与 TeneasyChatUISDK.init 都会调用，先到先得；
         * 宿主 App 有自己的 Application 类（manifest merge 顶掉本类）时，
         * 由 TeneasyChatUISDK.init 触发，避免 context 为空导致 UserPreferences 崩溃。
         */
        fun prepareEnvironment(appContext: Context) {
            if (context == null) {
                context = appContext.applicationContext
            }
            if (environmentReady) return
            synchronized(this) {
                if (environmentReady) return
                // 加载本地持久化的未读数快照（对齐 Flutter UnreadManager.init）
                GlobalMessageManager.instance.init()
                initXHttp2(appContext.applicationContext as Application)
                EmojiCompat.init(BundledEmojiCompatConfig(appContext.applicationContext))
                environmentReady = true
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
            if (Constants.domain.isEmpty()) {
                Constants.domain = Constants.sanitizeDomain(Constants.lines.split(",").firstOrNull()?.trim().orEmpty())
            }
            if (Constants.domain.isNotEmpty()) {
                XHttpSDK.setBaseUrl(Constants.baseUrlApi())
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        prepareEnvironment(this)

        //PRDownloader.initialize(getApplicationContext());
    }
}
