package com.teneasy.chatuisdk.ui.base

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import kotlin.math.abs
import kotlin.math.max

/**
 * 全屏媒体页「下拉关闭」容器（对齐 Flutter MediaPagerView/FullImageView 的
 * onVerticalDragUpdate/onVerticalDragEnd 手势）：
 * - 垂直下拉时第一个子 View 跟手平移，背景透明度按拖拽距离淡出（fadeDistance）
 * - 松手位移 > dismissThreshold 触发 [onDismiss]，否则 200ms 回弹
 *
 * 只平移 getChildAt(0)（内容层），其余子 View（如顶栏）保持固定，对齐 Flutter
 * 只 Transform.translate 包 body、AppBar 不动的结构。
 */
class DragDismissFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    /** 下拉超过阈值松手时回调（宿主 Activity 在此 finish） */
    var onDismiss: (() -> Unit)? = null

    /** 拖拽进度 0..1（offset/fadeDistance），宿主据此淡出背景 */
    var onDragFraction: ((Float) -> Unit)? = null

    /** 可垂直滚动的子 View（如 ScrollView）；其能继续向上滚时不拦截下拉手势 */
    var scrollableChild: View? = null

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val density = context.resources.displayMetrics.density
    // 对齐 Flutter _dismissThreshold=120 / _fadeDistance=400（逻辑像素≈dp）
    private val dismissThreshold = 120f * density
    private val fadeDistance = 400f * density

    private var downX = 0f
    private var downY = 0f
    private var dragging = false
    private var dragOffset = 0f
    private var resetAnimator: ValueAnimator? = null

    private val contentView: View?
        get() = if (childCount > 0) getChildAt(0) else null

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                dragging = false
                resetAnimator?.cancel()
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.x - downX
                val dy = ev.y - downY
                // 仅在「下拉为主」且内部可滚动子 View 已到顶时接管
                if (!dragging && dy > touchSlop && dy > abs(dx) * 1.2f && canDragDown()) {
                    dragging = true
                    downY = ev.y
                    return true
                }
            }
        }
        return dragging
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_MOVE -> {
                if (!dragging) {
                    // DOWN 落在无人消费的空白处时由本层兜底开启拖拽
                    val dy = ev.y - downY
                    if (dy > touchSlop && dy > abs(ev.x - downX) * 1.2f && canDragDown()) {
                        dragging = true
                        downY = ev.y
                    }
                }
                if (dragging) {
                    applyOffset(max(0f, ev.y - downY))
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragging) {
                    dragging = false
                    if (dragOffset > dismissThreshold) {
                        onDismiss?.invoke()
                    } else {
                        animateReset()
                    }
                }
            }
        }
        return true
    }

    private fun canDragDown(): Boolean {
        return scrollableChild?.canScrollVertically(-1) != true
    }

    private fun applyOffset(offset: Float) {
        dragOffset = offset
        contentView?.translationY = offset
        onDragFraction?.invoke((offset / fadeDistance).coerceIn(0f, 1f))
    }

    private fun animateReset() {
        resetAnimator?.cancel()
        resetAnimator = ValueAnimator.ofFloat(dragOffset, 0f).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            addUpdateListener { applyOffset(it.animatedValue as Float) }
            start()
        }
    }
}
