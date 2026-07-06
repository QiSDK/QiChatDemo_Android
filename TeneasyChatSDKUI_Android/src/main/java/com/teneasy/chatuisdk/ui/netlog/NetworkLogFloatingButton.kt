package com.teneasy.chatuisdk.ui.netlog

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView

/**
 * 网络日志悬浮按钮 - 对齐 ObridgeSDK 的 NetworkLogFloatingButton。
 *
 * 在屏幕上显示一个可拖动的悬浮按钮，点击打开 [NetworkLogActivity]。
 * 有 SYSTEM_ALERT_WINDOW 权限时用全局 overlay；否则回退为 Activity 内嵌方式（无需特殊权限）。
 *
 * 开发者通过 `TeneasyChatUISDK.showNetworkLogButton(app)` / `hideNetworkLogButton()` 控制。
 */
object NetworkLogFloatingButton {

    private const val TAG = "NetworkLogButton"

    private var floatingView: View? = null
    private var windowManager: WindowManager? = null
    private var application: Application? = null

    /** 显示悬浮按钮 */
    fun show(app: Application) {
        // 幂等：overlay 方式已显示，或内嵌方式已注册 callbacks，都不再重复
        if (floatingView != null || activityCallbacks != null) return
        application = app

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(app)) {
            Log.w(TAG, "无悬浮窗权限，改用 Activity 内嵌方式显示日志按钮")
            showWithActivityCallback(app)
            return
        }

        showWithOverlay(app)
    }

    /** 隐藏悬浮按钮 */
    fun hide() {
        floatingView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {
            }
        }
        floatingView = null
        windowManager = null

        activityCallbacks?.let {
            application?.unregisterActivityLifecycleCallbacks(it)
        }
        activityCallbacks = null
    }

    // ========== 方式1: WindowManager overlay（需 SYSTEM_ALERT_WINDOW 权限） ==========

    @SuppressLint("ClickableViewAccessibility")
    private fun showWithOverlay(app: Application) {
        val wm = app.getSystemService(Activity.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val button = createButton(app)
        floatingView = button

        val params = WindowManager.LayoutParams(
            dp(app, 50),
            dp(app, 50),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = app.resources.displayMetrics.widthPixels - dp(app, 66)
            y = (app.resources.displayMetrics.heightPixels * 0.65).toInt()
        }

        setupDragAndClick(button, params, wm)
        wm.addView(button, params)
    }

    // ========== 方式2: ActivityLifecycleCallbacks（无需特殊权限） ==========

    private var activityCallbacks: Application.ActivityLifecycleCallbacks? = null

    private fun showWithActivityCallback(app: Application) {
        val callbacks = object : Application.ActivityLifecycleCallbacks {
            private val buttonMap = mutableMapOf<Activity, View>()

            override fun onActivityResumed(activity: Activity) {
                if (activity is NetworkLogActivity || activity is NetworkLogDetailActivity) return
                if (buttonMap.containsKey(activity)) return

                try {
                    val decorView = activity.window.decorView as? FrameLayout ?: return
                    val button = createButton(activity)
                    val size = dp(activity, 50)
                    val marginEnd = dp(activity, 16)
                    val params = FrameLayout.LayoutParams(size, size).apply {
                        gravity = Gravity.END or Gravity.CENTER_VERTICAL
                        setMargins(0, 0, marginEnd, 0)
                    }

                    setupDragOnDecorView(button)
                    decorView.addView(button, params)
                    buttonMap[activity] = button
                } catch (e: Exception) {
                    Log.w(TAG, "添加日志按钮失败: ${e.message}")
                }
            }

            override fun onActivityPaused(activity: Activity) {
                buttonMap.remove(activity)?.let { button ->
                    try {
                        (button.parent as? android.view.ViewGroup)?.removeView(button)
                    } catch (_: Exception) {
                    }
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {
                buttonMap.remove(activity)
            }
        }

        activityCallbacks = callbacks
        app.registerActivityLifecycleCallbacks(callbacks)
    }

    // ========== 通用辅助方法 ==========

    @SuppressLint("SetTextI18n")
    private fun createButton(context: Context): TextView {
        return TextView(context).apply {
            text = "日志"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xD92196F3.toInt()) // systemBlue with 0.85 alpha
            }
            elevation = 8f
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragAndClick(
        view: View,
        params: WindowManager.LayoutParams,
        wm: WindowManager
    ) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var hasMoved = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    hasMoved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (dx * dx + dy * dy > 100) hasMoved = true
                    params.x = initialX + dx.toInt()
                    params.y = initialY + dy.toInt()
                    wm.updateViewLayout(view, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!hasMoved) openLogActivity(view.context)
                    true
                }
                else -> false
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragOnDecorView(view: View) {
        var initialTouchX = 0f
        var initialTouchY = 0f
        var initialViewX = 0f
        var initialViewY = 0f
        var hasMoved = false

        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    initialViewX = v.x
                    initialViewY = v.y
                    hasMoved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (dx * dx + dy * dy > 100) hasMoved = true
                    v.x = initialViewX + dx
                    v.y = initialViewY + dy
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!hasMoved) openLogActivity(v.context)
                    true
                }
                else -> false
            }
        }
    }

    private fun openLogActivity(context: Context) {
        val intent = Intent(context, NetworkLogActivity::class.java)
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun dp(context: Context, dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}
