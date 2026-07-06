package com.teneasy.chatuisdk.ui.netlog

import java.util.Date

/**
 * UISDK 一次 HTTP 请求的完整日志（请求 + 响应关键数据）。
 *
 * 数据来源为 [NetworkLogInterceptor]，捕获经由 XHttp2 / OkHttp 发出的所有 HTTP 请求。
 * 对齐 ObridgeSDK 的 ObridgeNetworkLog（去掉了加密相关字段，UISDK 请求体为明文 JSON）。
 */
data class NetworkLog(
    /** 完整请求 URL */
    val url: String,
    /** HTTP 方法（GET / POST ...） */
    val method: String,
    /** HTTP 请求头（Key-Value） */
    val requestHeaders: Map<String, String>,
    /** 请求体明文，无请求体或不便读取（如 multipart 上传）时为 null */
    val requestBodyPlain: String?,
    /** HTTP 响应状态码，网络错误时为 -1 */
    val httpStatusCode: Int,
    /** 响应体字符串，无响应体或读取失败时为 null */
    val responseBody: String?,
    /** 业务层响应码（ReturnData.code），无法解析时为 -1 */
    val apiCode: Int,
    /** 业务层响应消息（ReturnData.msg），无法解析时为空串 */
    val apiMsg: String,
    /** 网络错误描述，无错误时为 null */
    val error: String?,
    /** 从发送请求到收到响应的耗时（秒） */
    val duration: Double,
    /** 请求发出的时间戳 */
    val timestamp: Date
)
