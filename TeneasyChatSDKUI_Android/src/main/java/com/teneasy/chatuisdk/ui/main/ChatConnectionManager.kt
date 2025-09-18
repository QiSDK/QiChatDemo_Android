package com.teneasy.chatuisdk.ui.main

import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 管理聊天连接的协程轮询任务，负责执行初始化、重连以及上传进度刷新。
 */
class ChatConnectionManager(
    private val lifecycleScope: LifecycleCoroutineScope,
    private val config: Config
) {

    data class Config(
        val ensureDomain: () -> Unit,
        val isConnected: () -> Boolean,
        val showTip: (String) -> Unit,
        val initSdk: () -> Unit,
        val makeConnect: () -> Unit,
        val updateUploadProgress: () -> Unit
    )

    private var job: Job? = null

    fun start(isFirstLoad: Boolean) {
        if (config.isConnected()) {
            config.showTip("状态：已连接")
            return
        }

        if (isFirstLoad) {
            config.showTip("初始化SDK")
        }

        config.ensureDomain()
        stop()

        job = lifecycleScope.launch {
            delay(INITIAL_DELAY_MS)
            while (isActive) {
                when {
                    config.isConnected() -> {
                        // 已连接时仅更新进度，避免重复初始化
                        config.updateUploadProgress()
                    }
                    else -> {
                        config.initSdk()
                        config.makeConnect()
                        config.updateUploadProgress()
                    }
                }
                delay(POLLING_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    companion object {
        private const val INITIAL_DELAY_MS = 6000L
        private const val POLLING_INTERVAL_MS = 3000L
    }
}
