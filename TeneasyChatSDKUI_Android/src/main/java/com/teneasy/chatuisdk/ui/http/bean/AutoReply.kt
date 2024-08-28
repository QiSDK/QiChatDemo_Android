package com.teneasy.chatuisdk.ui.http.bean

import com.teneasy.chatuisdk.ui.http.bean.ChatHistory.Audio


data class AutoReply(
    var autoReplyItem : AutoReplyItem?
)

    data class AutoReplyItem (
        val id: String,
        val name: String,
        val title: String,
        val qa: List<QA>,
        val delaySEC: Long,
        val workerID: List<Any?>,
        val workerNames: List<Any?>
    )

    data class QA (
        val id: Long,
        val question: Question,
        val content: String,
        var clicked: Boolean = false,
        val answer: List<Question?>,
        val related: List<QA>? = null,
        var isExpand: Boolean
    )

    data class Question (
        val chatID: String,
        val msgID: String,
        val msgTime: Any? = null,
        val sender: String,
        val replyMsgID: String,
        val msgOp: String,
        val worker: Long,
       // val autoReplyFlag: Any? = null,
      //  val msgFmt: String,
        val consultID: String,
        val content: Content,
        val image: Audio
    )

    data class Content (
        val data: String
    )
