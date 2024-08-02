package com.android.common.view.chat.emoji

import android.content.Context
import android.text.NoCopySpan
import android.text.TextUtils
import android.text.style.DynamicDrawableSpan
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import androidx.appcompat.widget.AppCompatEditText
import com.sunhapper.x.spedit.gif.watcher.GifWatcher
import com.sunhapper.x.spedit.mention.watcher.SpanChangedWatcher
import com.sunhapper.x.spedit.view.DefaultKeyEventProxy
import com.sunhapper.x.spedit.view.KeyEventProxy
import com.sunhapper.x.spedit.view.SpXEditableFactory
import com.teneasy.chatuisdk.R
import java.util.*

class EmoticonEditText : AppCompatEditText {
    private var mEmojiconSize = 0
    private var mEmojiconAlignment = 0
    private var mEmojiconTextSize = 0
    private var mUseSystemDefault = false
    private var emojiPan: EmojiPan? = null

    private var mKeyEventProxy: KeyEventProxy = DefaultKeyEventProxy()

    init {
        val watchers = ArrayList<NoCopySpan>()
        watchers.add(SpanChangedWatcher())
        watchers.add(GifWatcher(this))
        setEditableFactory(SpXEditableFactory(watchers))
        setOnKeyListener { _, _, event -> handleKeyEvent(event) }
    }

    constructor(context: Context?) : super(context!!) {
        mEmojiconSize = textSize.toInt()
        mEmojiconTextSize = textSize.toInt()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
        init(attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context!!, attrs, defStyle) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.Emojicon)
        mEmojiconSize = a.getDimension(R.styleable.Emojicon_emojiconSize, textSize).toInt()
        mEmojiconAlignment = a.getInt(R.styleable.Emojicon_emojiconAlignment, DynamicDrawableSpan.ALIGN_BASELINE)
        mUseSystemDefault = a.getBoolean(R.styleable.Emojicon_emojiconUseSystemDefault, false)
        a.recycle()
        mEmojiconTextSize = textSize.toInt()
        setOnClickListener {
            if (emojiPan == null) return@setOnClickListener
            if (emojiPan!!.isShow()) {
                emojiPan!!.hideLayout()
            }
        }
    }

    override fun onTextChanged(text: CharSequence, start: Int, lengthBefore: Int, lengthAfter: Int) {
        updateText()
        emojiPan?.changeDeleteSendState(this.text?.isNotEmpty() != false)
    }

    /**
     * Set the size of emojicon in pixels.
     */
    fun setEmojiconSize(pixels: Int) {
        mEmojiconSize = pixels
        updateText()
    }

    private fun updateText() {
        EmoticonHandler.addEmojis(context, text, mEmojiconSize,
            mEmojiconAlignment, mEmojiconTextSize);
    }

    fun bindEmojiPan(emojiPan: EmojiPan) {
        this.emojiPan = emojiPan
    }

    private fun handleKeyEvent(event: KeyEvent): Boolean {
        if (TextUtils.isEmpty(text)) {
            return false
        }
        return mKeyEventProxy.onKeyEvent(event, text!!)
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        return CustomInputConnectionWrapper(super.onCreateInputConnection(outAttrs)!!, true)
    }


    /**
     * 解决google输入法删除不走OnKeyListener()回调问题
     */
    private inner class CustomInputConnectionWrapper
    /**
     * Initializes a wrapper.
     *
     *
     * **Caveat:** Although the system can accept `(InputConnection) null` in some
     * places, you cannot emulate such a behavior by non-null [InputConnectionWrapper] that
     * has `null` in `target`.
     *
     * @param target  the [InputConnection] to be proxied.
     * @param mutable set `true` to protect this object from being reconfigured to target
     * another [InputConnection].  Note that this is ignored while the target is `null`.
     */
        (target: InputConnection, mutable: Boolean) : InputConnectionWrapper(target, mutable) {


        override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
            return if (beforeLength == 1 && afterLength == 0) {
                sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN,
                    KeyEvent.KEYCODE_DEL)) && sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP,
                    KeyEvent.KEYCODE_DEL))
            } else super.deleteSurroundingText(beforeLength, afterLength)
        }

        override fun sendKeyEvent(event: KeyEvent): Boolean {
            return handleKeyEvent(event) || super.sendKeyEvent(event)
        }
    }

    fun setKeyEventProxy(keyEventProxy: KeyEventProxy) {
        mKeyEventProxy = keyEventProxy
    }

}