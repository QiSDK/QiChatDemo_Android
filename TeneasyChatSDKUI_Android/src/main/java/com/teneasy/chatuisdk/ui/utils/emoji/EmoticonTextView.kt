package com.teneasy.chatuisdk.ui.utils.emoji

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.DynamicDrawableSpan
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.teneasy.chatuisdk.ui.utils.emoji.EmoticonHandler.addEmojis
import com.teneasy.chatuisdk.R

class EmoticonTextView : AppCompatTextView {
    private var mEmoticonSize = 0
    private var mEmoticonAlignment = 0
    private var mEmoticonTextSize = 0
    private var mTextStart = 0
    private var mTextLength = -1
    private var mUseSystemDefault = false

    constructor(context: Context?) : super(context!!) {
        init(null)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
        init(attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context!!, attrs, defStyle) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        mEmoticonTextSize = textSize.toInt()
        if (attrs == null) {
            mEmoticonSize = textSize.toInt()
        } else {
            val a = context.obtainStyledAttributes(attrs, R.styleable.Emojicon)
            mEmoticonSize = a.getDimension(R.styleable.Emojicon_emojiconSize, textSize).toInt()
            mEmoticonAlignment = a.getInt(R.styleable.Emojicon_emojiconAlignment, DynamicDrawableSpan.ALIGN_BASELINE)
            mTextStart = a.getInteger(R.styleable.Emojicon_emojiconTextStart, 0)
            mTextLength = a.getInteger(R.styleable.Emojicon_emojiconTextLength, -1)
            mUseSystemDefault = a.getBoolean(R.styleable.Emojicon_emojiconUseSystemDefault, false)
            a.recycle()
        }
        text = text
    }

    override fun setText(text: CharSequence, type: BufferType) {
        var t: CharSequence? = text
        if (!TextUtils.isEmpty(t)) {
            val builder = SpannableStringBuilder(t)
            addEmojis(context, builder, mEmoticonSize,
                mEmoticonAlignment, mEmoticonTextSize)
            t = builder
        }
        super.setText(t, type)
    }

    /**
     * Set the size of emojicon in pixels.
     */
    fun setEmoticonSize(pixels: Int) {
        mEmoticonSize = pixels
        super.setText(text)
    }

    /**
     * Set whether to use system default emojicon
     */
    fun setUseSystemDefault(useSystemDefault: Boolean) {
        mUseSystemDefault = useSystemDefault
    }
}