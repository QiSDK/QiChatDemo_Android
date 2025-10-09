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

    override fun connected(c: GGateway.SCHi) {
        activeListener?.connected(c)
    }

    override fun systemMsg(msg: Result) {
        activeListener?.systemMsg(msg)
    }

    override fun receivedMsg(msg: CMessage.Message) {
        Log.d(TAG, "GlobalChatListener收到消息: consultId=${msg.consultId}, currentChatConsultId=${Constants.currentChatConsultId}")

        // 全局消息监听：如果不在当前聊天页面，增加未读数
        if (msg.consultId != Constants.currentChatConsultId) {
            // 不在当前聊天页面，增加未读数
            Constants.incrementUnreadCount(msg.consultId)
            Log.d(TAG, "增加未读数: consultId=${msg.consultId}, count=${Constants.getUnreadCount(msg.consultId)}")
            // 通知全局消息委托
            Constants.globalMessageDelegate?.onMessageReceived(msg.consultId)
        }

        // 转发给活跃的监听器
        activeListener?.receivedMsg(msg)
    }

    override fun msgReceipt(msg: CMessage.Message?, payloadId: Long, msgId: Long, errMsg: String) {
        activeListener?.msgReceipt(msg, payloadId, msgId, errMsg)
    }

    override fun msgDeleted(msg: CMessage.Message?, payloadId: Long, msgId: Long, errMsg: String) {
        activeListener?.msgDeleted(msg, payloadId, msgId, errMsg)
    }

    override fun workChanged(msg: GGateway.SCWorkerChanged) {
        activeListener?.workChanged(msg)
    }
}
