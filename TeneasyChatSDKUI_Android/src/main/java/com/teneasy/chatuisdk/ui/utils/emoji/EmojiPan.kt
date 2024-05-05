package com.teneasy.chatuisdk.ui.utils.emoji

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.chad.library.adapter.base.listener.OnItemClickListener
import com.teneasy.chatuisdk.R
import com.teneasy.chatuisdk.databinding.WidgetEmojiPanBinding

class EmojiPan @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    private val binding: WidgetEmojiPanBinding

    init {

        binding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.widget_emoji_pan, this, true)
        binding.ivDeleteChat.isEnabled = false

        initEmojiList()
    }

    private fun initEmojiList() {
        with(binding.rvEmoji) {
            layoutManager = GridLayoutManager(context, 8).apply {
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return when (position) {
                            0 -> 8
                            else -> 1
                        }
                    }
                }
            }
            val data = mutableListOf<MultiItemEntity>(EmojiPanText(context.getString(R.string.all_emoji)))
            data.addAll(EmoticonHandler.sEmojisMap.values.map { EmojiPanEmoji(it) })
            adapter = EmojiPanAdapter(data)
        }
    }

    fun setOnEmojiItemClickListener(listener: OnItemClickListener) {
        (binding.rvEmoji.adapter as EmojiPanAdapter).setOnItemClickListener(listener)
    }

    fun setOnDeleteClickListener(listener: OnClickListener) {
        binding.ivDeleteChat.setOnClickListener(listener)
    }

    fun setOnSendClickListener(listener: OnClickListener) {
        binding.tvSendChat.setOnClickListener(listener)
    }

    fun setOnEmojiClickListener(listener: OnClickListener) {
        binding.ivEmojiChat.setOnClickListener(listener)
    }

    fun changeDeleteSendState(enable: Boolean) {
        binding.ivDeleteChat.isEnabled = enable
        binding.tvSendChat.isEnabled = enable
    }

    fun showLayout() {
        hideKeyboard(this.context)
        // 延迟一会，让键盘先隐藏再显示表情键盘，否则会有一瞬间表情键盘和软键盘同时显示
        postDelayed({
            visibility = VISIBLE
        }, 50)
    }

    fun hideLayout() {
        visibility = GONE
    }

    fun isShow(): Boolean = getVisibility() == VISIBLE


    companion object {

        /**
         * 隐藏软键盘
         */
        fun hideKeyboard(context: Context?) {
            val activity = context as Activity?
            if (activity != null) {
                val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                if (imm.isActive && activity.currentFocus != null) {
                    imm.hideSoftInputFromWindow(activity.currentFocus?.windowToken, 0)
                }
            }
        }

        /**
         * 显示软键盘
         */
        fun showKeyboard(context: Context?) {
            val activity = context as Activity?
            if (activity != null) {
                val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInputFromInputMethod(activity.currentFocus?.getWindowToken(), 0)
                imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS)
            }
        }
    }

}

