package com.teneasy.chatuisdk.ui.netlog

/**
 * 网络日志缓冲区 - 对齐 ObridgeSDK 的 NetworkLogBuffer。
 *
 * 收集 [NetworkLogInterceptor] 捕获的 HTTP 请求日志，最多保留 [MAX_COUNT] 条（超出丢弃最旧）。
 * 线程安全，支持监听日志变化（回调在主线程触发）。
 *
 * 与 Obridge 版差异：日志来源是 OkHttp 拦截器，拦截器直接调用 [append]，无需 start()。
 */
object NetworkLogBuffer {

    private const val MAX_COUNT = 200

    private val lock = Any()
    private val _logs = mutableListOf<NetworkLog>()

    /** 日志变化监听器（主线程回调） */
    var onLogsChanged: (() -> Unit)? = null

    /** 获取当前日志列表（副本，最新的在前） */
    val logs: List<NetworkLog>
        get() = synchronized(lock) { _logs.toList() }

    /** 追加一条日志（由拦截器调用，可能在任意线程） */
    fun append(log: NetworkLog) {
        synchronized(lock) {
            _logs.add(0, log)
            while (_logs.size > MAX_COUNT) {
                _logs.removeAt(_logs.size - 1)
            }
        }
        notifyChanged()
    }

    /** 清空所有日志 */
    fun clear() {
        synchronized(lock) {
            _logs.clear()
        }
        notifyChanged()
    }

    private fun notifyChanged() {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            onLogsChanged?.invoke()
        }
    }
}
