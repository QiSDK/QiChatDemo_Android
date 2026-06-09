package com.teneasy.chatuisdk

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.luck.picture.lib.utils.ToastUtils
import com.teneasy.chatuisdk.databinding.ActivityDeviceInfoBinding
import com.teneasy.chatuisdk.ui.base.AppChatTheme
import com.teneasy.chatuisdk.ui.base.CapturePhotoUtils
import com.teneasy.chatuisdk.ui.base.Constants
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 设备信息页：展示设备 / 应用 / SDK 线路状态，支持保存为图片。
 * 对齐 Flutter `lib/src/vc/device_info_page.dart`。
 *
 * 注：登录IP / 线路等级 / 线路扫描 在 Flutter 当前实现里也是占位「—」，此处保持一致。
 */
class DeviceInfoActivity : AppCompatActivity() {

    companion object {
        /** 主题下标，缺省 / 负数表示随机一套（与 KeFuFragment 对齐）。 */
        const val EXTRA_THEME_INDEX = "theme_index"
    }

    private lateinit var binding: ActivityDeviceInfoBinding
    private val theme by lazy { AppChatTheme.fromIndex(intent.getIntExtra(EXTRA_THEME_INDEX, -1)) }

    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val handler = Handler(Looper.getMainLooper())
    private var timeValueView: TextView? = null
    private val ticker = object : Runnable {
        override fun run() {
            timeValueView?.text = timeFormat.format(Date())
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyTheme()
        binding.llClose.setOnClickListener { finish() }
        binding.ivSave.setOnClickListener { saveAsImage() }

        buildRows()
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        super.onDestroy()
    }

    private fun applyTheme() {
        binding.main.background = theme.newGradientDrawable()
        binding.llTop.setBackgroundColor(theme.gradientStartColor)
        binding.tvTitle.setTextColor(theme.tintColor)
        binding.llClose.setColorFilter(theme.tintColor)
        binding.ivSave.setColorFilter(theme.tintColor)
        window.statusBarColor = theme.statusBarColor
    }

    private fun buildRows() {
        addRow("会员账号", memberAccount())
        addDivider()
        addRow("手机型号", Build.MODEL ?: "—")
        addDivider()
        addRow("应用名称", appName())
        addDivider()
        addRow("手机系统版本", "Android_${Build.VERSION.RELEASE}")
        addDivider()
        addRow("APP当前版本", appVersion())
        addDivider()
        timeValueView = addRow("当前时间", timeFormat.format(Date()))
        addDivider()
        addRow("应用包名", packageName)
        addDivider()
        addRow("登录IP", "—", multiLine = true)
        addDivider()
        addRow("当前线路", currentLine())
        addDivider()
        addRow("线路等级", "—")
        addDivider()
        addRow("线路扫描", "—", multiLine = true)

        handler.postDelayed(ticker, 1000)
    }

    private fun memberAccount(): String {
        if (Constants.userName.isNotEmpty()) return Constants.userName
        return if (Constants.userId == 0) "—" else Constants.userId.toString()
    }

    private fun appName(): String =
        applicationInfo.loadLabel(packageManager).toString()

    private fun appVersion(): String = try {
        val info = packageManager.getPackageInfo(packageName, 0)
        val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            info.longVersionCode.toString() else @Suppress("DEPRECATION") info.versionCode.toString()
        "${info.versionName}_$code (V3)"
    } catch (e: Exception) {
        "—"
    }

    /** 当前线路：在线路列表(逗号分隔)中找到 domain 的下标，转成「线路N」。 */
    private fun currentLine(): String {
        val domain = Constants.domain
        if (domain.isEmpty()) return "—"
        val urls = Constants.lines.split(",").map { it.trim() }
        urls.forEachIndexed { i, u ->
            val host = u.replace(Regex("^https?://"), "").substringBefore("/")
            if (u.contains(domain) || domain.contains(host)) return "线路${i + 1}"
        }
        return domain
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    /** 添加一行，返回 value 的 TextView（供「当前时间」实时刷新）。 */
    private fun addRow(label: String, value: String, multiLine: Boolean = false): TextView {
        val labelView = TextView(this).apply {
            text = label
            setTextColor(0xFF1A1A1A.toInt())
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.NORMAL)
        }
        val valueView = TextView(this).apply {
            text = value
            setTextColor(0xFF9AA0A6.toInt())
            textSize = 14f
        }

        val row = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        if (multiLine) {
            row.orientation = LinearLayout.VERTICAL
            valueView.setPadding(0, dp(8), 0, 0)
            row.addView(labelView)
            row.addView(valueView)
        } else {
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            valueView.gravity = Gravity.END
            row.addView(labelView)
            row.addView(valueView, LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            ).apply { marginStart = dp(16) })
        }
        binding.llRows.addView(row)
        return valueView
    }

    private fun addDivider() {
        val divider = View(this).apply {
            setBackgroundColor(0xFFEEEEEE.toInt())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(1) / 2)
            ).apply {
                marginStart = dp(16)
                marginEnd = dp(16)
            }
        }
        binding.llRows.addView(divider)
    }

    /** 把信息卡片渲染为图片保存到相册。 */
    private fun saveAsImage() {
        val card = binding.cardInfo
        if (card.width == 0 || card.height == 0) {
            ToastUtils.showToast(this, "保存失败")
            return
        }
        val bitmap = Bitmap.createBitmap(card.width, card.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        card.draw(canvas)
        Thread {
            val uri = CapturePhotoUtils.saveImageInQ(bitmap)
            runOnUiThread {
                ToastUtils.showToast(this, if (uri != null) "已保存到相册" else "保存失败")
            }
        }.start()
    }
}
