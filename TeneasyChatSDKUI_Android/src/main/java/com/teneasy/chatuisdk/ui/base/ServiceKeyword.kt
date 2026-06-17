package com.teneasy.chatuisdk.ui.base

import com.google.gson.Gson

/**
 * 宿主 App 通过自己的接口拿到的「服务关键词」配置。
 *
 * 当用户在聊天页输入的文本【包含】任一 keyword 时，UISDK 会以
 * msgSourceType = MST_AUTO_CARD、type = MSG_TEXT 发送一条卡片消息，
 * 消息体即本对象的 [toJsonString] 结果，UI 上由 AutoCardViewHolder 渲染成卡片。
 *
 * content 是多态的：
 * - questionType == 1：数组，渲染成可点选项按钮（见 [options]）；
 * - questionType == 2：字符串，渲染成正文段落（见 [contentText]）。
 *
 * 对齐 iOS `ServiceKeyword` / Flutter `ServiceKeyword`。
 */
class ServiceKeyword(json: Map<String, Any?>) {
    val id: Int? = (json["id"] as? Number)?.toInt()
    val questionType: Int? = (json["questionType"] as? Number)?.toInt()
    val category: Int? = (json["category"] as? Number)?.toInt()
    val subject: String? = json["subject"] as? String

    /** content 为数组时的选项列表；否则为空。 */
    val options: List<String>
    /** content 为字符串时的正文；否则为空串。 */
    val contentText: String
    /** 原始 content 是否为数组，决定 [toJsonString] 还原成数组还是字符串。 */
    val isContentArray: Boolean

    val keywords: List<String> =
        (json["keywords"] as? List<*>)?.map { it.toString() } ?: emptyList()
    val weight: Int = (json["weight"] as? Number)?.toInt() ?: 0
    val jumpCategory: Int? = (json["jumpCategory"] as? Number)?.toInt()
    val jumpUrl: String? = json["jumpUrl"] as? String

    init {
        when (val content = json["content"]) {
            is List<*> -> {
                options = content.map { it.toString() }
                contentText = ""
                isContentArray = true
            }
            is String -> {
                options = emptyList()
                contentText = content
                isContentArray = false
            }
            else -> {
                options = emptyList()
                contentText = ""
                isContentArray = false
            }
        }
    }

    /** 原样还原条目 JSON（content 数组/字符串两种形态都保真）——即卡片消息的文本体。 */
    fun toJsonString(): String {
        val map = LinkedHashMap<String, Any?>()
        id?.let { map["id"] = it }
        questionType?.let { map["questionType"] = it }
        category?.let { map["category"] = it }
        subject?.let { map["subject"] = it }
        map["content"] = if (isContentArray) options else contentText
        map["keywords"] = keywords
        map["weight"] = weight
        jumpCategory?.let { map["jumpCategory"] = it }
        jumpUrl?.let { map["jumpUrl"] = it }
        return Gson().toJson(map)
    }

    companion object {
        /** 从卡片消息文本体反解析（供 AutoCardViewHolder 渲染用）。 */
        @Suppress("UNCHECKED_CAST")
        fun fromJsonString(s: String): ServiceKeyword? = try {
            val map = Gson().fromJson(s, Map::class.java) as? Map<String, Any?>
            if (map != null) ServiceKeyword(map) else null
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * 在 [list] 中查找命中 [input] 的卡片配置。
 *
 * 命中规则：[input]【包含】某条目的任一 keyword 子串即命中；多条命中时取 weight
 * 最大的那条，weight 并列取列表中靠前的一条。无命中返回 null。
 */
fun matchAutoCard(input: String, list: List<ServiceKeyword>): ServiceKeyword? {
    if (input.isEmpty() || list.isEmpty()) return null
    var best: ServiceKeyword? = null
    for (item in list) {
        val hit = item.keywords.any { it.isNotEmpty() && input.contains(it) }
        // 严格大于才替换 → 并列时保留先出现的（靠前）那条。
        if (hit && (best == null || item.weight > best!!.weight)) {
            best = item
        }
    }
    return best
}
