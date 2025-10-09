package com.teneasy.chatuisdk.ui.base

import android.util.Log
import com.teneasy.sdk.Result
import com.teneasy.sdk.TeneasySDKDelegate
import com.teneasyChat.api.common.CMessage
import com.teneasyChat.gateway.GGateway

/**
 * 全局聊天监听器
 * 用于在任何页面都能监听到消息，并处理未读数逻辑
 */
class GlobalChatListener : TeneasySDKDelegate {

    companion object {
        private const val TAG = "GlobalChatListener"

        // 单例
        val instance: GlobalChatListener by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            GlobalChatListener()
        }
    }

    // 实际的业务监听器（通常是当前活跃的Fragment）
    var activeListener: TeneasySDKDelegate? = null

    // 全局处理器（GlobalChatManager）
    var globalHandler: TeneasySDKDelegate? = null

    override fun connected(c: GGateway.SCHi) {
        // 先通知全局处理器
        globalHandler?.connected(c)
        // 再通知活跃监听器
        activeListener?.connected(c)
    }

    override fun systemMsg(msg: Result) {
        // 先通知全局处理器
        globalHandler?.systemMsg(msg)
        // 再通知活跃监听器
        activeListener?.systemMsg(msg)
    }

    override fun receivedMsg(msg: CMessage.Message) {
        Log.d(TAG, "GlobalChatListener收到消息: consultId=${msg.consultId}, currentChatConsultId=${Constants.currentChatConsultId}")

        // 先通知全局处理器（处理未读数和事件总线）
        globalHandler?.receivedMsg(msg)

        // 再转发给活跃的监听器
        activeListener?.receivedMsg(msg)
    }

    override fun msgReceipt(msg: CMessage.Message?, payloadId: Long, msgId: Long, errMsg: String) {
        Log.d(TAG, "GlobalChatListener收到回执: payloadId=$payloadId, msgId=$msgId")
        // 先通知全局处理器
        globalHandler?.msgReceipt(msg, payloadId, msgId, errMsg)
        // 再通知活跃监听器
        activeListener?.msgReceipt(msg, payloadId, msgId, errMsg)
    }

    override fun msgDeleted(msg: CMessage.Message?, payloadId: Long, msgId: Long, errMsg: String) {
        // 先通知全局处理器
        globalHandler?.msgDeleted(msg, payloadId, msgId, errMsg)
        // 再通知活跃监听器
        activeListener?.msgDeleted(msg, payloadId, msgId, errMsg)
    }

    override fun workChanged(msg: GGateway.SCWorkerChanged) {
        // 先通知全局处理器
        globalHandler?.workChanged(msg)
        // 再通知活跃监听器
        activeListener?.workChanged(msg)
    }
}
