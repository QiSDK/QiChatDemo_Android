package com.teneasy.chatuisdk.ui.base

/**
 * 全局消息委托接口
 * 用于在全局范围内监听消息接收事件
 */
interface GlobalMessageDelegate {
    /**
     * 当收到新消息时调用
     * @param consultId 咨询会话ID
     */
    fun onMessageReceived(consultId: Long)
}
