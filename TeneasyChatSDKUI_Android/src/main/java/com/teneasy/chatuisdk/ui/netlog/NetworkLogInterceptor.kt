package com.teneasy.chatuisdk.ui.netlog

import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import org.json.JSONObject
import java.io.IOException
import java.util.Date

/**
 * OkHttp 拦截器：捕获 UISDK 经由 XHttp2 / OkHttp 发出的 HTTP 请求，写入 [NetworkLogBuffer]。
 *
 * 在 ApplicationExt.initXHttp2() 里通过 `XHttp.getInstance().addInterceptor(...)` 注册一次，
 * 此后每个请求（generateOkClient 会从全局 OkHttpClient.Builder 构建）都会经过本拦截器。
 *
 * 读取 body 的策略：
 * - 请求体：非 multipart、非流式、大小可控时读取明文；否则记为 null（避免把上传大文件读进内存）。
 * - 响应体：用 peekBody 读取快照（上限 [MAX_BODY_BYTES]），不消费原始流，不影响下游解析。
 */
class NetworkLogInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val timestamp = Date()
        val startNs = System.nanoTime()

        val headers = LinkedHashMap<String, String>()
        for (name in request.headers.names()) {
            headers[name] = request.headers.values(name).joinToString(", ")
        }
        val requestBody = readRequestBody(request)

        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: IOException) {
            val duration = (System.nanoTime() - startNs) / 1_000_000_000.0
            NetworkLogBuffer.append(
                NetworkLog(
                    url = request.url.toString(),
                    method = request.method,
                    requestHeaders = headers,
                    requestBodyPlain = requestBody,
                    httpStatusCode = -1,
                    responseBody = null,
                    apiCode = -1,
                    apiMsg = "",
                    error = e.message ?: e.toString(),
                    duration = duration,
                    timestamp = timestamp
                )
            )
            throw e
        }

        val duration = (System.nanoTime() - startNs) / 1_000_000_000.0
        val responseBody = readResponseBody(response)
        val (apiCode, apiMsg) = parseApiCodeMsg(responseBody)

        NetworkLogBuffer.append(
            NetworkLog(
                url = request.url.toString(),
                method = request.method,
                requestHeaders = headers,
                requestBodyPlain = requestBody,
                httpStatusCode = response.code,
                responseBody = responseBody,
                apiCode = apiCode,
                apiMsg = apiMsg,
                error = null,
                duration = duration,
                timestamp = timestamp
            )
        )

        return response
    }

    private fun readRequestBody(request: okhttp3.Request): String? {
        val body = request.body ?: return null
        // 上传 / 流式 / 双工请求不读取，避免把大文件读进内存或消费一次性流
        if (body.isDuplex() || body.isOneShot()) return null
        val contentType = body.contentType()
        if (contentType?.type == "multipart") return "[multipart body, ${body.contentLength()} bytes]"
        val length = body.contentLength()
        if (length > MAX_BODY_BYTES) return "[body ${length} bytes, 已省略]"
        return try {
            val buffer = Buffer()
            body.writeTo(buffer)
            val charset = contentType?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
            buffer.readString(charset)
        } catch (_: Exception) {
            null
        }
    }

    private fun readResponseBody(response: Response): String? {
        return try {
            val peeked = response.peekBody(MAX_BODY_BYTES)
            peeked.string()
        } catch (_: Exception) {
            null
        }
    }

    private fun parseApiCodeMsg(body: String?): Pair<Int, String> {
        if (body.isNullOrBlank()) return -1 to ""
        return try {
            val trimmed = body.trim()
            if (!trimmed.startsWith("{")) return -1 to ""
            val json = JSONObject(trimmed)
            val code = if (json.has("code")) json.optInt("code", -1) else -1
            val msg = json.optString("msg", "")
            code to msg
        } catch (_: Exception) {
            -1 to ""
        }
    }

    companion object {
        /** 读取 body 的上限，超出则省略（避免大响应/上传把内存打满） */
        private const val MAX_BODY_BYTES = 1024L * 1024L // 1MB
    }
}
