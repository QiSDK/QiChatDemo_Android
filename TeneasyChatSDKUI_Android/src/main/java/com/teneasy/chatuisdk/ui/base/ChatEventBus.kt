package com.teneasy.chatuisdk.ui.base

import com.teneasy.sdk.Result
import com.teneasyChat.api.common.CMessage
import com.teneasyChat.gateway.GGateway
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 聊天事件
 * 用于在应用内传递聊天相关的事件
 */
sealed class ChatEvent {
    data class MessageReceived(val message: CMessage.Message) : ChatEvent()
    data class MessageDeleted(val message: CMessage.Message?, val payloadId: Long, val msgId: Long, val errMsg: String) : ChatEvent()
    data class MessageReceipt(val message: CMessage.Message?, val payloadId: Long, val msgId: Long, val errMsg: String) : ChatEvent()
    data class WorkerChanged(val workerChanged: GGateway.SCWorkerChanged) : ChatEvent()
    data class SystemMessage(val result: Result) : ChatEvent()
    data class Connected(val connection: GGateway.SCHi) : ChatEvent()
}

/**
 * 聊天事件总线
 * 用于在应用内分发聊天事件，类似 iOS 的 NotificationCenter
 */
object ChatEventBus {
    private val _events = MutableSharedFlow<ChatEvent>(replay = 0, extraBufferCapacity = 64)
    val events = _events.asSharedFlow()

    fun post(event: ChatEvent) {
        _events.tryEmit(event)
    }
}
