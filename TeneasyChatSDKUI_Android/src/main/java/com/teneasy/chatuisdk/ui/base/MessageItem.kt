package com.teneasy.sdk.ui

import com.teneasyChat.api.common.CMessage


//enum MessageSendState: String { case 发送中="0", 发送成功="1", 发送失败="2", 未知="-1" }
enum class MessageSendState {
        发送中, 发送成功, 发送失败, 未知
}

class MessageItem {//:Serializable
        var id: String = ""
        var isLeft : Boolean = false
        //var sendError: Boolean = false
        var cMsg: CMessage.Message? = null
        var sendStatus: MessageSendState = MessageSendState.发送中
        //使用payLoadId来进行发送消息，消息回执的匹配
        var payLoadId: Long = 0
        var msgId: Long = 0
        var imgPath: String = ""
        //var status : Int = 0
    }