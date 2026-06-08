package com.teneasy.qldemo

import android.graphics.Color

/** 渐变方向（对齐 iOS ChatTheme.GradientDirection） */
enum class GradientDirection {
    TOP_TO_BOTTOM, BOTTOM_TO_TOP, LEFT_TO_RIGHT,
    RIGHT_TO_LEFT, TOP_LEFT_TO_BOTTOM_RIGHT, TOP_RIGHT_TO_BOTTOM_LEFT
}

/**
 * Demo 专用主题模型。
 * 只包含主屏幕用到的字段（渐变 + tintColor + 标题着色用的 leftBubble*），
 * 不传入聊天 SDK。颜色照搬 iOS ChatTheme.swift presets。
 */
data class ChatTheme(
    val gradientStart: Int,
    val gradientEnd: Int,
    val direction: GradientDirection,
    val tintColor: Int,
    val leftBubbleColor: Int,
    val leftBubbleTextColor: Int,
) {
    companion object {
        val presets: List<ChatTheme> = listOf(
            // 1. 晨雾白
            ChatTheme(
                gradientStart = Color.rgb(252, 250, 252),
                gradientEnd = Color.rgb(220, 232, 244),
                direction = GradientDirection.TOP_LEFT_TO_BOTTOM_RIGHT,
                tintColor = Color.rgb(120, 140, 210),
                leftBubbleColor = Color.argb(140, 255, 255, 255),
                leftBubbleTextColor = Color.rgb(46, 46, 46),
            ),
            // 2. 暗夜神殿
            ChatTheme(
                gradientStart = Color.rgb(74, 62, 112),
                gradientEnd = Color.rgb(28, 22, 62),
                direction = GradientDirection.TOP_TO_BOTTOM,
                tintColor = Color.rgb(155, 120, 240),
                leftBubbleColor = Color.argb(184, 60, 50, 95),
                leftBubbleTextColor = Color.rgb(242, 242, 242),
            ),
            // 3. 蜜桃粉
            ChatTheme(
                gradientStart = Color.rgb(255, 245, 242),
                gradientEnd = Color.rgb(255, 217, 209),
                direction = GradientDirection.BOTTOM_TO_TOP,
                tintColor = Color.rgb(235, 105, 110),
                leftBubbleColor = Color.argb(77, 255, 245, 242),
                leftBubbleTextColor = Color.rgb(38, 38, 38),
            ),
            // 4. 抹茶绿
            ChatTheme(
                gradientStart = Color.rgb(242, 250, 237),
                gradientEnd = Color.rgb(209, 237, 194),
                direction = GradientDirection.TOP_TO_BOTTOM,
                tintColor = Color.rgb(76, 175, 80),
                leftBubbleColor = Color.argb(77, 242, 250, 237),
                leftBubbleTextColor = Color.rgb(38, 38, 38),
            ),
            // 5. 日落橙
            ChatTheme(
                gradientStart = Color.rgb(255, 247, 235),
                gradientEnd = Color.rgb(255, 224, 184),
                direction = GradientDirection.BOTTOM_TO_TOP,
                tintColor = Color.rgb(255, 152, 0),
                leftBubbleColor = Color.argb(77, 255, 247, 235),
                leftBubbleTextColor = Color.rgb(38, 38, 38),
            ),
            // 6. 星空靛
            ChatTheme(
                gradientStart = Color.rgb(224, 230, 247),
                gradientEnd = Color.rgb(158, 173, 224),
                direction = GradientDirection.TOP_TO_BOTTOM,
                tintColor = Color.rgb(63, 81, 181),
                leftBubbleColor = Color.argb(77, 224, 230, 247),
                leftBubbleTextColor = Color.rgb(38, 38, 38),
            ),
            // 7. 暗夜紫
            ChatTheme(
                gradientStart = Color.rgb(199, 38, 107),
                gradientEnd = Color.rgb(31, 15, 61),
                direction = GradientDirection.BOTTOM_TO_TOP,
                tintColor = Color.rgb(148, 107, 242),
                leftBubbleColor = Color.argb(77, 199, 38, 107),
                leftBubbleTextColor = Color.rgb(230, 230, 230),
            ),
            // 8. 极简灰
            ChatTheme(
                gradientStart = Color.rgb(246, 247, 250),
                gradientEnd = Color.rgb(246, 247, 250),
                direction = GradientDirection.TOP_TO_BOTTOM,
                tintColor = Color.rgb(55, 120, 244),
                leftBubbleColor = Color.argb(235, 255, 255, 255),
                leftBubbleTextColor = Color.rgb(38, 38, 38),
            ),
            // 9. 幻夜紫
            ChatTheme(
                gradientStart = Color.rgb(165, 150, 187),
                gradientEnd = Color.rgb(31, 31, 76),
                direction = GradientDirection.BOTTOM_TO_TOP,
                tintColor = Color.rgb(155, 120, 240),
                leftBubbleColor = Color.argb(77, 165, 150, 187),
                leftBubbleTextColor = Color.rgb(230, 230, 230),
            ),
            // 10. 晴空蓝
            ChatTheme(
                gradientStart = Color.rgb(240, 247, 255),
                gradientEnd = Color.rgb(199, 224, 255),
                direction = GradientDirection.BOTTOM_TO_TOP,
                tintColor = Color.rgb(69, 137, 246),
                leftBubbleColor = Color.argb(77, 240, 247, 255),
                leftBubbleTextColor = Color.rgb(38, 38, 38),
            ),
            // 11. 薄暮紫
            ChatTheme(
                gradientStart = Color.rgb(245, 237, 250),
                gradientEnd = Color.rgb(209, 184, 235),
                direction = GradientDirection.TOP_TO_BOTTOM,
                tintColor = Color.rgb(128, 90, 210),
                leftBubbleColor = Color.argb(77, 245, 237, 250),
                leftBubbleTextColor = Color.rgb(38, 38, 38),
            ),
        )
    }
}
