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

    /** 右侧图片链接（仅精准问题可用）。空串 / null 表示无图。 */
    val rightImageUrl: String? = json["rightImageUrl"] as? String

    val keywords: List<String> =
        (json["keywords"] as? List<*>)?.map { it.toString() } ?: emptyList()
    val weight: Int = (json["weight"] as? Number)?.toInt() ?: 0
    /** 跳转分类，见 [jumpNone] / [jumpMiniProgram] / [jumpH5] / [jumpNative]。 */
    val jumpCategory: Int? = (json["jumpCategory"] as? Number)?.toInt()
    val jumpUrl: String? = json["jumpUrl"] as? String

    /** 是否需要跳转：jumpCategory 非「无」且 jumpUrl 非空。 */
    val hasJump: Boolean
        get() = (jumpCategory ?: jumpNone) != jumpNone && !jumpUrl.isNullOrEmpty()

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
        rightImageUrl?.let { map["rightImageUrl"] = it }
        map["keywords"] = keywords
        map["weight"] = weight
        jumpCategory?.let { map["jumpCategory"] = it }
        jumpUrl?.let { map["jumpUrl"] = it }
        return Gson().toJson(map)
    }

    companion object {
        // jumpCategory 取值（见规范 mst_card_msg.md）：跳转分类。
        const val jumpNone = 0        // 无跳转
        const val jumpMiniProgram = 1 // 小程序
        const val jumpH5 = 2          // H5
        const val jumpNative = 3      // 原生页

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

/**
 * 宿主处理「卡片跳转」的回调。
 *
 * [jumpUrl] 为跳转链接，[jumpCategory] 为跳转类型：1=小程序（如 pages/Withdraw/Record）、
 * 2=H5（完整网址）、3=原生页（见 [ServiceKeyword.jumpMiniProgram] 等常量）。宿主据此把
 * 用户导航到自己的小程序容器 / WebView / 原生页。
 *
 * 注意：一旦注册处理器，【所有类型】（含 H5）都交给宿主；未注册时 SDK 才会用
 * 「H5 → 外部浏览器、其余 → 内置模拟页」兜底。
 */
fun interface CardJumpHandler {
    fun onJump(jumpUrl: String, jumpCategory: Int?)
}
