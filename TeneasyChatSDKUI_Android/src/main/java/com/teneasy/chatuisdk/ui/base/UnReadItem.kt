package com.teneasy.chatuisdk.ui.base

/**
 * 未读消息项
 * @property consultId 咨询会话ID
 * @property unReadCount 未读消息数量
 */
data class UnReadItem(
    var consultId: Long,
    var unReadCount: Int = 0
)
