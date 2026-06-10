package com.teneasy.chatuisdk.ui.base

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONObject

/**
 * 全局消息管理器
 * 负责未读消息的统计和管理
 * 参考 iOS 的 GlobalMessageManager / Flutter 的 UnreadManager 实现
 *
 * 存储维度：consultId -> unreadCount（内存源为 [Constants.unReadList]）。
 * 持久化：每次变更后 debounce 写入 SharedPreferences（[PREFS_NAME] / [KEY_UNREAD]），
 * 进程重启后通过 [init] 异步加载历史快照，避免未读数在杀进程后丢失（对齐 Flutter UnreadManager）。
 */
class GlobalMessageManager private constructor() {

    companion object {
        private const val TAG = "GlobalMessageManager"
        private const val PREFS_NAME = "MySharedPreferences"
        private const val KEY_UNREAD = "unread_counts_v1"
        private const val PERSIST_DEBOUNCE_MS = 300L

        val instance: GlobalMessageManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            GlobalMessageManager()
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val persistRunnable = Runnable { persistNow() }
    @Volatile
    private var loaded = false

    /**
     * 从本地加载历史未读快照到 [Constants.unReadList]。可重复调用，已加载则跳过。
     * 建议在 Application.onCreate 调用（对齐 Flutter UnreadManager.init）。
     */
    fun init() {
        if (loaded) return
        loaded = true
        try {
            val raw = prefs()?.getString(KEY_UNREAD, null)
            if (!raw.isNullOrEmpty()) {
                val obj = JSONObject(raw)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val cid = key.toLongOrNull() ?: continue
                    val cnt = obj.optInt(key, 0)
                    if (cnt > 0 && Constants.unReadList.none { it.consultId == cid }) {
                        Constants.unReadList.add(UnReadItem(cid, cnt))
                    }
                }
            }
            Log.d(TAG, "加载本地未读完成，共 ${Constants.unReadList.size} 条会话有未读")
        } catch (e: Exception) {
            Log.w(TAG, "加载本地未读失败: ${e.message}")
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
        schedulePersist()

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
            schedulePersist()
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
     * 仅当本地没有该会话的非零未读时，用 [count] 兜底写入（对齐 Flutter setUnreadIfAbsent）。
     *
     * 用于宿主从服务端 entrance 接口拉到「快照未读」后回填：本地 WS 实时累加值优先，
     * 避免服务端慢一拍的快照覆盖刚到达的新消息（之前的无条件覆盖会把本地新未读盖掉）。
     * @param consultId 咨询会话ID
     * @param count 接口返回的未读数
     */
    fun setUnreadIfAbsent(consultId: Long, count: Int) {
        if (count <= 0) return
        // 本地已有非零未读则不覆盖，保留本地实时累加值
        if (getUnReadCount(consultId) > 0) return

        val existingItem = Constants.unReadList.find { it.consultId == consultId }
        if (existingItem != null) {
            existingItem.unReadCount = count
        } else {
            Constants.unReadList.add(UnReadItem(consultId, count))
        }

        Log.d(TAG, "兜底回填未读数: consultId=$consultId, count=$count")
        schedulePersist()
    }

    // ========== 持久化 ==========

    private fun prefs() =
        ApplicationExt.context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 变更后 debounce 写盘，合并短时间内的连续变更（对齐 Flutter 300ms debounce） */
    private fun schedulePersist() {
        mainHandler.removeCallbacks(persistRunnable)
        mainHandler.postDelayed(persistRunnable, PERSIST_DEBOUNCE_MS)
    }

    private fun persistNow() {
        val sp = prefs() ?: return
        try {
            val nonZero = Constants.unReadList.filter { it.unReadCount > 0 }
            if (nonZero.isEmpty()) {
                sp.edit().remove(KEY_UNREAD).apply()
            } else {
                val obj = JSONObject()
                nonZero.forEach { obj.put(it.consultId.toString(), it.unReadCount) }
                sp.edit().putString(KEY_UNREAD, obj.toString()).apply()
            }
        } catch (e: Exception) {
            Log.w(TAG, "持久化未读数失败: ${e.message}")
        }
    }
}
