package com.teneasy.chatuisdk.ui.netlog

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.MenuItem
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 网络日志详情页面 - 对齐 ObridgeSDK 的 NetworkLogDetailActivity。
 * 以纯文本展示单条请求的请求头 / 请求体 / 响应体等完整信息，支持长按复制。
 */
class NetworkLogDetailActivity : AppCompatActivity() {

    companion object {
        private var pendingLog: NetworkLog? = null

        fun start(context: Context, log: NetworkLog) {
            pendingLog = log
            context.startActivity(Intent(context, NetworkLogDetailActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val log = pendingLog
        if (log == null) {
            finish()
            return
        }

        supportActionBar?.apply {
            title = "日志详情"
            setDisplayHomeAsUpEnabled(true)
        }

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(0xFFF5F5F5.toInt())
            fitsSystemWindows = true
        }

        val textView = TextView(this).apply {
            typeface = Typeface.MONOSPACE
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(0xFF333333.toInt())
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setTextIsSelectable(true)
        }

        scrollView.addView(textView)
        setContentView(scrollView)

        val text = buildString {
            append("== 请求 ==\n")
            append("${log.method} ${log.url}\n\n")

            append("-- Headers --\n")
            for ((key, value) in log.requestHeaders.toSortedMap()) {
                append("$key: $value\n")
            }

            log.requestBodyPlain?.let {
                append("\n-- Request Body --\n")
                append(formatJSON(it))
                append("\n")
            }

            append("\n== 响应 ==\n")
            append("HTTP Status: ${log.httpStatusCode}\n")
            append("API Code: ${log.apiCode}\n")
            append("API Msg: ${log.apiMsg}\n")

            val durationStr = String.format("%.3f", log.duration)
            append("耗时: ${durationStr}s\n")

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
            append("时间: ${dateFormat.format(log.timestamp)}\n")

            log.error?.let {
                append("\n-- Error --\n$it\n")
            }

            log.responseBody?.let {
                append("\n-- Response Body --\n")
                append(formatJSON(it))
                append("\n")
            }
        }

        textView.text = text
    }

    override fun onDestroy() {
        super.onDestroy()
        pendingLog = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun formatJSON(string: String): String {
        return try {
            val trimmed = string.trim()
            when {
                trimmed.startsWith("{") -> JSONObject(trimmed).toString(2)
                trimmed.startsWith("[") -> JSONArray(trimmed).toString(2)
                else -> string
            }
        } catch (_: Exception) {
            string
        }
    }

    private fun dp(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
