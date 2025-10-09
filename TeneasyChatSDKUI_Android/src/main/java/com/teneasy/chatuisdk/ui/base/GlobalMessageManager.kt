package com.teneasy.chatuisdk.ui.base

import android.util.Log

/**
 * 全局消息管理器
 * 负责未读消息的统计和管理
 * 参考 iOS 的 GlobalMessageManager 实现
 */
class GlobalMessageManager private constructor() {

    companion object {
        private const val TAG = "GlobalMessageManager"

        val instance: GlobalMessageManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            GlobalMessageManager()
        }
    }

    /**
     * 添加未读消息
     * @param consultId 咨询会话ID
     */
    fun addUnReadMessage(consultId: Long) {
        // 如果是当前聊天页面，不增加未读数
        if (consultId == Constants.currentChatConsultId) {
            return
        }

        val existingItem = Constants.unReadList.find { it.consultId == consultId }
        if (existingItem != null) {
            existingItem.unReadCount++
        } else {
            Constants.unReadList.add(UnReadItem(consultId, 1))
        }

        Log.d(TAG, "添加未读消息: consultId=$consultId, count=${getUnReadCount(consultId)}")

        // 通知未读数变化
        Constants.globalMessageDelegate?.onMessageReceived(consultId)
    }

    /**
     * 清除指定会话的未读数
     * @param consultId 咨询会话ID
     */
    fun clearUnReadCount(consultId: Long) {
        val existingItem = Constants.unReadList.find { it.consultId == consultId }
        if (existingItem != null) {
            existingItem.unReadCount = 0
            Log.d(TAG, "清除未读数: consultId=$consultId")
        }

        // 通知未读数变化
        Constants.globalMessageDelegate?.onMessageReceived(consultId)
    }

    /**
     * 获取总未读数
     * @return 所有会话的未读消息总数
     */
    fun getTotalUnReadCount(): Int {
        return Constants.unReadList.sumOf { it.unReadCount }
    }

    /**
     * 获取指定会话的未读数
     * @param consultId 咨询会话ID
     * @return 未读消息数量
     */
    fun getUnReadCount(consultId: Long): Int {
        return Constants.unReadList.find { it.consultId == consultId }?.unReadCount ?: 0
    }

    /**
     * 同步接口返回的未读数
     * @param consultId 咨询会话ID
     * @param count 接口返回的未读数
     */
    fun syncUnreadCount(consultId: Long, count: Int) {
        val existingItem = Constants.unReadList.find { it.consultId == consultId }
        if (existingItem != null) {
            existingItem.unReadCount = count
        } else {
            Constants.unReadList.add(UnReadItem(consultId, count))
        }

        Log.d(TAG, "同步未读数: consultId=$consultId, count=$count")
    }
}
