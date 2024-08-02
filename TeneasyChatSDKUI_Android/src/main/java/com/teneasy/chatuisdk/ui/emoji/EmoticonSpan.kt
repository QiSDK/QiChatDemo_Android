package com.android.common.view.chat.emoji

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.style.DynamicDrawableSpan
import android.util.Log
import androidx.core.content.ContextCompat
import java.lang.ref.WeakReference

internal class EmoticonSpan(
    var mContext: Context?,
    private val mResourceId: Int,
    private val mSize: Int,
    alignment: Int,
    textSize: Int
) : DynamicDrawableSpan(alignment) {
    private val mTextSize: Int
    private var mHeight: Int
    private var mWidth: Int
    private var mTop = 0
    private var mDrawable: Drawable? = null
    private var mDrawableRef: WeakReference<Drawable?>? = null

    init {
        mHeight = mSize
        mWidth = mHeight
        mTextSize = textSize
    }

    override fun getDrawable(): Drawable? {
        if (mDrawable == null) {
            try {
                mDrawable = ContextCompat.getDrawable(mContext!!, mResourceId)
                mHeight = mSize
                mWidth = mHeight * mDrawable!!.intrinsicWidth / mDrawable!!.intrinsicHeight
                mTop = (mTextSize - mHeight) / 2
                mDrawable!!.setBounds(0, mTop, mWidth, mTop + mHeight)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (mDrawable==null){
            mDrawable = ContextCompat.getDrawable(mContext!!, mResourceId)
        }
        return mDrawable
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        //super.draw(canvas, text, start, end, x, top, y, bottom, paint);
        val b = cachedDrawable
        canvas.save()
        var transY = bottom - b!!.bounds.bottom
        if (mVerticalAlignment == ALIGN_BASELINE) {
            transY = top + (bottom - top) / 2 - (b.bounds.bottom - b.bounds.top) / 2 - mTop
        }
        canvas.translate(x, transY.toFloat())
        b.draw(canvas)
        canvas.restore()
    }

    private val cachedDrawable: Drawable?
        get() {
            if (mDrawableRef == null || mDrawableRef!!.get() == null) {
                mDrawableRef = WeakReference(drawable)
            }
            return mDrawableRef!!.get()
        }
}