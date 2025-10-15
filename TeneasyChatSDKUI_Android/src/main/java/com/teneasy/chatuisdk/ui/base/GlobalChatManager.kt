package com.teneasy.chatuisdk.ui.base

import android.util.Log
import com.google.protobuf.Extension
import com.luck.picture.lib.utils.ToastUtils
import com.teneasy.sdk.ChatLib
import com.teneasy.sdk.Result
import com.teneasy.sdk.TeneasySDKDelegate
import com.teneasyChat.api.common.CMessage
import com.teneasyChat.gateway.GGateway
import kotlinx.coroutines.*
import java.util.Date

/**
 * 全局聊天管理器
 * 负责整个应用的聊天连接管理和消息分发
 * 参考 iOS 的 GlobalChatManager 实现
 */
class GlobalChatManager private constructor() : TeneasySDKDelegate {

    companion object {
        private const val TAG = "GlobalChatManager"
        private const val CONNECTION_CHECK_INTERVAL = 6000L // 6秒检查一次连接

        val instance: GlobalChatManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            GlobalChatManager()
        }
    }

    private var connectionJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isInitialized = false

    /**
     * 初始化全局聊天管理器
     */
    fun initializeGlobalChat() {
        if (isInitialized) {
            Log.w(TAG, "GlobalChatManager已经初始化，跳过重复初始化")
            return
        }

        // 初始化全局chatLib
        if (Constants.chatLib == null) {
            Constants.chatLib = ChatLib.getInstance()
        }

        // 设置GlobalChatListener为主监听器，它会转发给GlobalChatManager和activeListener
        Constants.chatLib?.listener = GlobalChatListener.instance
        // 设置GlobalChatManager为全局处理器
        GlobalChatListener.instance.globalHandler = this
        Log.i(TAG, "全局ChatLib已初始化")

        isInitialized = true
        startConnectionMonitoring()
    }

    /**
     * 根据需要建立连接
     */
   fun connectIfNeeded() {
        val chatLib = Constants.chatLib
        if (chatLib == null) {
            Log.w(TAG, "chatLib为null，无法连接")
            return
        }

        if (chatLib.isConnected) {
            Log.d(TAG, "sdk状态：已连接 ${Date()}")
            return
        }

        // 从 UserDefaults 获取 domain（如果为空）
        if (Constants.domain.isEmpty()) {
            Constants.domain = UserPreferences().getString(PARAM_DOMAIN, "")
            Log.d(TAG, "从UserDefaults获取domain：${Constants.domain}")
        }

        // 确保全局变量已初始化
        if (Constants.domain.isEmpty()) {
            Log.w(TAG, "domain为空，无法连接")
            return
        }

        if (chatLib.payloadId == 0L) {
            Log.i(TAG, "初始化SDK连接")
            val wssUrl = "wss://${Constants.domain}/v1/gateway/h5?"
            chatLib.apply {
                init(
                    Constants.cert,
                    if (Constants.xToken.isEmpty()) Constants.cert else Constants.xToken,
                    wssUrl,
                    Constants.userId,
                    "9zgd9YUc",
                    0L,
                    Constants.getCustomParam(),
                    Constants.maxSessionMins
                )
                try {
                    makeConnect()
                } catch (e: Exception) {
                    Log.e(TAG, "连接失败: ${e.message}")
                }
            }
        } else {
            Log.i(TAG, "重新连接 " + Date())
            try {
                chatLib.makeConnect()
            } catch (e: Exception) {
                Log.e(TAG, "重新连接失败: ${e.message}")
            }
        }
    }

    /**
     * 开始连接监控
     */
    private fun startConnectionMonitoring() {
        connectionJob?.cancel()
        connectionJob = scope.launch {
            while (isActive) {
                delay(CONNECTION_CHECK_INTERVAL)
                connectIfNeeded()
            }
        }
    }

    /**
     * 停止全局聊天管理器
     */
    fun stopGlobalChat() {
        connectionJob?.cancel()
        connectionJob = null
        Constants.chatLib?.disConnect()
        Constants.unReadList.clear()
        //Constants.unSentMessage.clear()
        Log.i(TAG, "全局ChatLib已停止")
    }

    // MARK: - TeneasySDKDelegate 实现

    override fun receivedMsg(msg: CMessage.Message) {
        Log.d(TAG, "GlobalChatManager收到消息: consultId=${msg.consultId}, current=${Constants.currentChatConsultId}")

        // 如果不在当前聊天页面，增加未读数
        if (msg.consultId != Constants.currentChatConsultId) {
            GlobalMessageManager.instance.addUnReadMessage(msg.consultId)
        }
        if (msg.msgSourceType == CMessage.MsgSourceType.MST_EVALUATE){
            ToastUtils.showToast(ApplicationExt.context!!, "收到要求评估的消息！")
            return
        }
        // 通过事件总线分发消息
        ChatEventBus.post(ChatEvent.MessageReceived(msg))
    }

    override fun msgDeleted(msg: CMessage.Message?, payloadId: Long, msgId: Long, errMsg: String) {
        ChatEventBus.post(ChatEvent.MessageDeleted(msg, payloadId, msgId, errMsg))
    }

    override fun msgReceipt(msg: CMessage.Message?, payloadId: Long, msgId: Long, errMsg: String) {
        ChatEventBus.post(ChatEvent.MessageReceipt(msg, payloadId, msgId, errMsg))
    }

    override fun workChanged(msg: GGateway.SCWorkerChanged) {
        ChatEventBus.post(ChatEvent.WorkerChanged(msg))
    }

    override fun systemMsg(msg: Result) {
        Log.i(TAG, "GlobalChatManager系统消息: ${msg.msg} Code: ${msg.code}")
        ChatEventBus.post(ChatEvent.SystemMessage(msg))
    }

    override fun connected(c: GGateway.SCHi) {
        Log.i(TAG, "GlobalChatManager连接成功")
        Constants.xToken = c.token
        UserPreferences().putString(PARAM_XTOKEN, c.token)
        ChatEventBus.post(ChatEvent.Connected(c))
    }
}
