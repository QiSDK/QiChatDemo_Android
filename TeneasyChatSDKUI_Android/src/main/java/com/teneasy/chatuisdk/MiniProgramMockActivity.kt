package com.teneasy.chatuisdk

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.teneasy.chatuisdk.ui.base.AppChatTheme

/**
 * 内置的「小程序页面」模拟页。
 *
 * 当卡片带 jumpUrl（如 pages/Withdraw/Record）而宿主又没有通过
 * TeneasyChatUISDK.setCardJumpHandler 注册自己的跳转处理器时，SDK 用本页兜底，
 * 让接入方在没有真实小程序运行时的情况下也能直观看到「跳转」发生。
 *
 * 真实接入时，宿主应注册自己的处理器，把 jumpUrl 导航到真正的小程序容器 / 原生页 /
 * WebView；本页仅用于演示。对齐 iOS MiniProgramMockViewController / Flutter MiniProgramMockPage。
 */
class MiniProgramMockActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_JUMP_URL = "jump_url"
        const val EXTRA_JUMP_CATEGORY = "jump_category"
        const val EXTRA_THEME_INDEX = "theme_index"
    }

    private val theme by lazy { AppChatTheme.fromIndex(intent.getIntExtra(EXTRA_THEME_INDEX, -1)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val jumpUrl = intent.getStringExtra(EXTRA_JUMP_URL) ?: ""
        val jumpCategory = if (intent.hasExtra(EXTRA_JUMP_CATEGORY))
            intent.getIntExtra(EXTRA_JUMP_CATEGORY, 0) else null
        val tint = theme.tintColor

        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            setPadding(dp(24), dp(24), dp(24), dp(24))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        val title = TextView(this).apply {
            text = "模拟打开小程序页面"
            setTextColor(tint)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        root.addView(title)

        val pathLab = TextView(this).apply {
            text = jumpUrl
            setTextColor(Color.DKGRAY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        }
        root.addView(pathLab)

        if (jumpCategory != null) {
            root.addView(TextView(this).apply {
                text = "jumpCategory：$jumpCategory"
                setTextColor(Color.GRAY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                gravity = Gravity.CENTER
                setPadding(0, dp(8), 0, 0)
            })
        }

        root.addView(TextView(this).apply {
            text = "这是 SDK 内置的占位页。真实接入时由宿主注册处理器，\n把此路径导航到真正的小程序 / 原生页 / WebView。"
            setTextColor(Color.GRAY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            gravity = Gravity.CENTER
            setPadding(0, dp(20), 0, dp(20))
        })

        val backBtn = Button(this).apply {
            text = "返回聊天"
            isAllCaps = false
            setTextColor(Color.WHITE)
            background?.mutate()?.setTint(tint)
            setOnClickListener { finish() }
        }
        root.addView(backBtn)

        setContentView(root)
    }
}
