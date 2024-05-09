package com.teneasy.chatuisdk.ui.http.bean


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
        val answer: List<Any?>,
        val related: List<QA>? = null
    )

    data class Question (
        val chatID: String,
        val msgID: String,
        val msgTime: Any? = null,
        val sender: String,
        val replyMsgID: String,
        val msgOp: String,
        val worker: Long,
        val autoReplyFlag: Any? = null,
        val msgFmt: String,
        val consultID: String,
        val content: Content
    )

    data class Content (
        val data: String
    )
