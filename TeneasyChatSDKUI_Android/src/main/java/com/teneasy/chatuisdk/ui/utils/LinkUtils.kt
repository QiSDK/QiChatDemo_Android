package com.teneasy.chatuisdk.ui.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.util.Patterns
import android.view.View
import android.widget.TextView
import com.teneasy.chatuisdk.ui.utils.emoji.EmoticonTextView
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * 链接处理工具类
 */
object LinkUtils {
    // URL正则表达式模式
    private val URL_PATTERN = Pattern.compile(
        "((https?|ftp|gopher|telnet|file):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * 处理文本中的链接
     * @param textView 文本视图
     * @param text 原始文本
     * @param linkColor 链接颜色
     * @param context 上下文
     */
    fun processTextWithLinks(textView: TextView, text: String, linkColor: Int, context: Context) {
        val spannableString = SpannableString(text)
        val matcher: Matcher = URL_PATTERN.matcher(text)
        
        // 查找所有链接
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            val url = text.substring(start, end)
            
            // 创建可点击的span
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    // 打开链接
                    openUrl(url, context)
                }
                
                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    // 设置链接颜色和下划线
                    ds.color = linkColor
                    ds.isUnderlineText = true
                }
            }
            
            // 应用span到文本
            spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        
        // 设置处理后的文本
        textView.text = spannableString
    }
    
    /**
     * 处理表情文本中的链接
     * @param textView 表情文本视图
     * @param text 原始文本
     * @param linkColor 链接颜色
     * @param context 上下文
     */
    fun processEmoticonTextWithLinks(textView: EmoticonTextView, text: String, linkColor: Int, context: Context) {
        // 先设置原始文本，让表情处理完成
        textView.text = text
        
        // 然后处理链接
        val spannableString = SpannableString(textView.text)
        val matcher: Matcher = URL_PATTERN.matcher(textView.text)
        
        // 查找所有链接
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            val url = textView.text.substring(start, end)
            
            // 创建可点击的span
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    // 打开链接
                    openUrl(url, context)
                }
                
                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    // 设置链接颜色和下划线
                    ds.color = linkColor
                    ds.isUnderlineText = true
                }
            }
            
            // 应用span到文本
            spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        
        // 设置处理后的文本
        textView.text = spannableString
    }
    
    /**
     * 打开URL
     * @param url 链接地址
     * @param context 上下文
     */
    private fun openUrl(url: String, context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}