package com.teneasy.chatuisdk.ui.base

/**
 * 会话媒体项（图片/视频），供媒体浏览器左右滑浏览使用。
 * 对齐 Flutter lib/src/model/MediaItem.dart。
 */
data class ChatMediaItem(
    val url: String,
    val isVideo: Boolean
) {
    companion object {
        /**
         * 相对路径补图片服务器前缀（对齐 Flutter ChatPage._absUrl）
         */
        fun absUrl(url: String): String {
            if (url.isEmpty()) return url
            if (url.contains("http")) return url
            return Constants.baseUrlImage + url
        }
    }
}
