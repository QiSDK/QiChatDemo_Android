package com.teneasy.chatuisdk.ui.netlog

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 网络日志列表页面 - 对齐 ObridgeSDK 的 NetworkLogActivity。
 * 展示 UISDK 经由 XHttp2 / OkHttp 发出的 HTTP 请求，点击进入详情。
 *
 * 开发者通过 `TeneasyChatUISDK.openNetworkLog(context)` 打开，或使用可拖动悬浮按钮。
 */
class NetworkLogActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LogAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.apply {
            title = "网络日志"
            setDisplayHomeAsUpEnabled(true)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFF5F5F5.toInt())
            fitsSystemWindows = true
        }

        recyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@NetworkLogActivity)
            addItemDecoration(DividerItemDecoration(this@NetworkLogActivity, DividerItemDecoration.VERTICAL))
        }
        root.addView(
            recyclerView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        )

        setContentView(root)

        adapter = LogAdapter(this) { log ->
            NetworkLogDetailActivity.start(this, log)
        }
        recyclerView.adapter = adapter

        refreshData()

        NetworkLogBuffer.onLogsChanged = {
            refreshData()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        NetworkLogBuffer.onLogsChanged = null
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refreshData() {
        adapter.logs = NetworkLogBuffer.logs
        adapter.notifyDataSetChanged()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(0, MENU_CLEAR, 0, "清空")
        return true
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(MENU_CLEAR)?.setOnMenuItemClickListener {
            NetworkLogBuffer.clear()
            true
        }
        return super.onPrepareOptionsMenu(menu)
    }

    // ========== RecyclerView Adapter ==========

    private class LogAdapter(
        private val context: Context,
        private val onClick: (NetworkLog) -> Unit
    ) : RecyclerView.Adapter<LogViewHolder>() {

        var logs: List<NetworkLog> = emptyList()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
            val view = LogItemView(context)
            return LogViewHolder(view)
        }

        override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
            val log = logs[position]
            (holder.itemView as LogItemView).bind(log)
            holder.itemView.setOnClickListener { onClick(log) }
        }

        override fun getItemCount(): Int = logs.size
    }

    private class LogViewHolder(view: View) : RecyclerView.ViewHolder(view)

    // ========== 日志列表项布局 ==========

    @SuppressLint("SetTextI18n")
    private class LogItemView(context: Context) : LinearLayout(context) {

        private val urlLabel: TextView
        private val statusLabel: TextView
        private val infoLabel: TextView

        init {
            orientation = VERTICAL
            setPadding(dp(15), dp(12), dp(15), dp(12))
            setBackgroundColor(Color.WHITE)

            val topRow = LinearLayout(context).apply {
                orientation = HORIZONTAL
            }

            urlLabel = TextView(context).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTextColor(0xFF333333.toInt())
                maxLines = 2
            }
            topRow.addView(urlLabel, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))

            statusLabel = TextView(context).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                gravity = Gravity.END
                layoutParams = LayoutParams(dp(60), LayoutParams.WRAP_CONTENT)
            }
            topRow.addView(statusLabel)

            addView(topRow, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

            infoLabel = TextView(context).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setTextColor(0xFF999999.toInt())
                maxLines = 1
                val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                lp.topMargin = dp(4)
                layoutParams = lp
            }
            addView(infoLabel)
        }

        fun bind(log: NetworkLog) {
            try {
                val url = URL(log.url)
                urlLabel.text = "${log.method} ${url.path}"
            } catch (_: Exception) {
                urlLabel.text = "${log.method} ${log.url}"
            }

            val code = log.httpStatusCode
            statusLabel.text = "$code"
            statusLabel.setTextColor(
                when {
                    code in 200..299 -> 0xFF4CAF50.toInt() // green
                    code < 0 -> 0xFFF44336.toInt() // red
                    else -> 0xFFFF9800.toInt() // orange
                }
            )

            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val timeStr = timeFormat.format(log.timestamp)
            val durationStr = String.format("%.0fms", log.duration * 1000)
            infoLabel.text = "$timeStr | $durationStr | apiCode:${log.apiCode}"
        }

        private fun dp(dp: Int): Int {
            return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp.toFloat(),
                resources.displayMetrics
            ).toInt()
        }
    }

    companion object {
        private const val MENU_CLEAR = 1
    }
}
