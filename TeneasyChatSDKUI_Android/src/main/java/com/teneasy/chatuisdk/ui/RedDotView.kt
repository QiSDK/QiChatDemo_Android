package com.teneasy.chatuisdk.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.teneasy.chatuisdk.R

class RedDotView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    private val redDot: ImageView
    private val unreadCount: TextView

    init {
        inflate(context, R.layout.red_dot_layout, this)
        redDot = findViewById(R.id.red_dot)
        unreadCount = findViewById(R.id.tv_count)
    }

    fun showRedDot(show: Boolean) {
        redDot.visibility = if (show) VISIBLE else GONE
    }

    fun setUnreadCount(count: Int) {
//        unreadCount.text = count.toString()
//        unreadCount.visibility = if (count > 0) VISIBLE else GONE
        unreadCount.visibility = View.GONE
    }
}