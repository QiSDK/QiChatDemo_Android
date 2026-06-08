package com.teneasy.chatuisdk.ui.main

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.luck.picture.lib.utils.ToastUtils
import com.teneasy.chatuisdk.R
import com.teneasy.chatuisdk.ui.base.Constants
import com.teneasy.chatuisdk.ui.http.MainApi
import com.teneasy.chatuisdk.ui.http.ReturnData
import com.teneasy.chatuisdk.ui.http.bean.EvaluationConfig
import com.teneasy.chatuisdk.ui.http.bean.EvaluationScoreConfig
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.callback.ProgressLoadingCallBack
import com.xuexiang.xhttp2.exception.ApiException
import java.util.UUID

enum class EvaluationScene {
    MANUAL,    // 用户主动点★按钮打开
    TRIGGERED  // 收到触发消息自动弹出
}

/**
 * 客服满意度评价弹窗
 * - 5 颗星 + 留言（200 字）+ 提交
 * - 触发场景下用户点 X 关闭会调用 add(close=1) 通知后端不再弹
 */
class EvaluationDialog(
    context: Context,
    private val scene: EvaluationScene,
    private val config: EvaluationConfig,
    private val consultId: Long,
    private val tintColor: Int,
    /** 评价状态变化回调（1=已评价, 2=已关闭），对应 Flutter onStatusChanged */
    private val onStatusChanged: ((Int) -> Unit)? = null
) : Dialog(context, android.R.style.Theme_Translucent_NoTitleBar) {

    companion object {
        private const val TAG = "EvaluationDialog"
        private const val MAX_REMARK = 200
        private val STAR_UNSELECTED = Color.parseColor("#C7C7C7")
    }

    private lateinit var stars: List<TextView>
    private lateinit var etRemark: EditText
    private lateinit var tvCounter: TextView
    private lateinit var btnSubmit: Button
    private lateinit var pbLoading: ProgressBar
    private lateinit var ivClose: ImageView

    private var selectedScore: Int = 0
    private var submitting: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_evaluation)
        setCancelable(false)
        setCanceledOnTouchOutside(false)

        // 半透明背景
        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.parseColor("#80000000")))
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.CENTER)
            decorView.setPadding(
                dp(24), 0, dp(24), 0
            )
        }

        bindViews()
        setupStars()
        setupRemark()
        setupSubmit()
        setupClose()
        setupTapToHideKeyboard()
    }

    /**
     * 点击留言框以外的空白区域收起软键盘（对应 Flutter 弹窗外层 GestureDetector unfocus）
     */
    private fun setupTapToHideKeyboard() {
        window?.decorView?.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val focused = currentFocus
                if (focused is EditText) {
                    val rect = Rect()
                    focused.getGlobalVisibleRect(rect)
                    if (!rect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                        focused.clearFocus()
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE)
                                as? InputMethodManager
                        imm?.hideSoftInputFromWindow(focused.windowToken, 0)
                    }
                }
            }
            // 不消费事件，保证按钮等正常点击
            false
        }
    }

    private fun bindViews() {
        stars = listOf(
            findViewById(R.id.tv_star1),
            findViewById(R.id.tv_star2),
            findViewById(R.id.tv_star3),
            findViewById(R.id.tv_star4),
            findViewById(R.id.tv_star5)
        )
        etRemark = findViewById(R.id.et_evaluation_remark)
        tvCounter = findViewById(R.id.tv_evaluation_counter)
        btnSubmit = findViewById(R.id.btn_evaluation_submit)
        pbLoading = findViewById(R.id.pb_evaluation_loading)
        ivClose = findViewById(R.id.iv_evaluation_close)
    }

    private fun setupStars() {
        stars.forEachIndexed { idx, star ->
            star.setOnClickListener {
                if (submitting) return@setOnClickListener
                selectedScore = idx + 1
                refreshStars()
                btnSubmit.isEnabled = true
            }
        }
    }

    private fun refreshStars() {
        stars.forEachIndexed { idx, star ->
            star.setTextColor(if (idx < selectedScore) tintColor else STAR_UNSELECTED)
        }
    }

    private fun setupRemark() {
        etRemark.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val len = s?.length ?: 0
                tvCounter.text = "$len/$MAX_REMARK"
            }
        })
    }

    private fun setupSubmit() {
        btnSubmit.setOnClickListener {
            if (selectedScore <= 0 || submitting) return@setOnClickListener
            submit()
        }
    }

    private fun setupClose() {
        ivClose.setOnClickListener {
            if (submitting) return@setOnClickListener
            onCloseClicked()
        }
    }

    private fun onCloseClicked() {
        if (scene == EvaluationScene.TRIGGERED) {
            // 触发场景下关闭 = 通知后端不要再弹出，fire-and-forget
            postEvaluation(0, "", 1, null)
            onStatusChanged?.invoke(2)
        }
        dismissSafely()
    }

    private fun submit() {
        submitting = true
        btnSubmit.isEnabled = false
        btnSubmit.text = ""
        pbLoading.visibility = android.view.View.VISIBLE

        val score = selectedScore
        val remark = etRemark.text?.toString() ?: ""
        postEvaluation(score, remark, 0) { success, errMsg ->
            submitting = false
            if (success) {
                onStatusChanged?.invoke(1)
                dismissSafely()
                showFeedbackToastIfNeeded(score)
            } else {
                pbLoading.visibility = android.view.View.GONE
                btnSubmit.text = "提交"
                btnSubmit.isEnabled = true
                ToastUtils.showToast(context, errMsg ?: "评价提交失败")
            }
        }
    }

    private fun showFeedbackToastIfNeeded(score: Int) {
        val match = config.configs?.firstOrNull { it.score == score } ?: return
        val feedback = match.feedback
        if (match.status == 1 && !feedback.isNullOrEmpty()) {
            ToastUtils.showToast(context, feedback)
        }
    }

    private fun dismissSafely() {
        try {
            if (isShowing) dismiss()
        } catch (e: Exception) {
            Log.e(TAG, "dismiss error: ${e.message}")
        }
    }

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    /**
     * 调用 /v1/tenant/evaluation/add 接口
     * @param onDone 为 null 表示 fire-and-forget（用于触发场景下的关闭通知）
     */
    private fun postEvaluation(
        score: Int,
        remark: String,
        close: Int,
        onDone: ((Boolean, String?) -> Unit)?
    ) {
        val param = JsonObject().apply {
            addProperty("consultId", consultId)
            addProperty("score", score)
            addProperty("remark", remark)
            addProperty("close", close)
        }
        val request = XHttp.custom().accessToken(false)
        request.headers("X-Token", Constants.xToken)
        request.headers("x-trace-id", UUID.randomUUID().toString())

        request.call(
            request.create(MainApi.IMainTask::class.java).evaluationAdd(param),
            object : ProgressLoadingCallBack<ReturnData<Any>>(null) {
                override fun onSuccess(res: ReturnData<Any>) {
                    if (res.code == 0) {
                        onDone?.invoke(true, null)
                    } else {
                        onDone?.invoke(false, res.msg)
                    }
                }

                override fun onError(e: ApiException?) {
                    super.onError(e)
                    onDone?.invoke(false, e?.message ?: "网络错误")
                }
            }
        )
    }
}
