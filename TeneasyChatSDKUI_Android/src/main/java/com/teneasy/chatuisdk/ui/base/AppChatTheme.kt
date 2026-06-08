package com.teneasy.chatuisdk.ui.base

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.core.graphics.ColorUtils
import kotlin.random.Random

/**
 * 聊天页主题配置：渐变背景 + 统一按钮着色 + 左右气泡色。
 *
 * 对齐 Flutter `lib/src/model/AppChatTheme.dart` 与 iOS `ChatTheme`。
 * 每次进入会话默认随机一套（见 [random]），宿主也可指定（见 [fromIndex]）。
 */
enum class AppChatGradientDirection {
    TOP_TO_BOTTOM,
    BOTTOM_TO_TOP,
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT,
    TOP_LEFT_TO_BOTTOM_RIGHT,
    TOP_RIGHT_TO_BOTTOM_LEFT,
}

data class AppChatTheme(
    /** 渐变起始颜色 */
    val gradientStartColor: Int,
    /** 渐变结束颜色 */
    val gradientEndColor: Int,
    /** 渐变方向 */
    val gradientDirection: AppChatGradientDirection = AppChatGradientDirection.TOP_TO_BOTTOM,
    /** 统一按钮 / 图标着色 */
    val tintColor: Int,
    /** 左侧（客服）气泡背景色，默认白 88% */
    val leftBubbleColor: Int = withOpacity(Color.WHITE, 0.88),
    /** 左侧（客服）气泡文字色 */
    val leftBubbleTextColor: Int = 0xFF1A1A1A.toInt(),
    /** 右侧（用户）气泡背景色，默认 tintColor 92% */
    val rightBubbleColor: Int = Int.MIN_VALUE,
    /** 右侧（用户）气泡文字色 */
    val rightBubbleTextColor: Int = Color.WHITE,
) {
    /** rightBubbleColor 若未显式给出（哨兵值），用 tintColor 92% 兜底。 */
    val resolvedRightBubbleColor: Int
        get() = if (rightBubbleColor == Int.MIN_VALUE) withOpacity(tintColor, 0.92) else rightBubbleColor

    /** GradientDrawable 的方向枚举。 */
    val gradientOrientation: GradientDrawable.Orientation
        get() = when (gradientDirection) {
            AppChatGradientDirection.TOP_TO_BOTTOM -> GradientDrawable.Orientation.TOP_BOTTOM
            AppChatGradientDirection.BOTTOM_TO_TOP -> GradientDrawable.Orientation.BOTTOM_TOP
            AppChatGradientDirection.LEFT_TO_RIGHT -> GradientDrawable.Orientation.LEFT_RIGHT
            AppChatGradientDirection.RIGHT_TO_LEFT -> GradientDrawable.Orientation.RIGHT_LEFT
            AppChatGradientDirection.TOP_LEFT_TO_BOTTOM_RIGHT -> GradientDrawable.Orientation.TL_BR
            AppChatGradientDirection.TOP_RIGHT_TO_BOTTOM_LEFT -> GradientDrawable.Orientation.TR_BL
        }

    /** 渐变顶部颜色：边到边渐变背景时用作状态栏色，使其与屏幕顶部视觉对齐。 */
    val statusBarColor: Int
        get() = when (gradientDirection) {
            AppChatGradientDirection.BOTTOM_TO_TOP -> gradientEndColor
            else -> gradientStartColor
        }

    /** 构造整页背景用的渐变 Drawable。 */
    fun newGradientDrawable(): GradientDrawable =
        GradientDrawable(gradientOrientation, intArrayOf(gradientStartColor, gradientEndColor))

    companion object {
        /** Flutter `Color.fromRGBO(r,g,b,o)` 等价：用透明度 [opacity]（0~1）调制颜色 alpha。 */
        fun withOpacity(color: Int, opacity: Double): Int =
            ColorUtils.setAlphaComponent(color, (opacity.coerceIn(0.0, 1.0) * 255).toInt())

        private fun rgb(r: Int, g: Int, b: Int): Int = Color.rgb(r, g, b)

        /** SDK 内置默认主题（晴空蓝）。 */
        val defaultTheme = AppChatTheme(
            gradientStartColor = rgb(242, 247, 255),
            gradientEndColor = rgb(204, 224, 255),
            gradientDirection = AppChatGradientDirection.TOP_TO_BOTTOM,
            tintColor = rgb(69, 137, 246),
        )

        /** 随机获取一套精选主题。 */
        fun random(): AppChatTheme =
            if (presets.isEmpty()) defaultTheme else presets[Random.nextInt(presets.size)]

        /** 按下标取主题；越界回退默认。负数表示随机。 */
        fun fromIndex(index: Int): AppChatTheme = when {
            index < 0 -> random()
            index < presets.size -> presets[index]
            else -> defaultTheme
        }

        /** 10 套精选主题（与 Flutter / iOS 对齐）。 */
        val presets: List<AppChatTheme> = listOf(
            // 1. 晴空蓝（默认蓝系，清爽专业）
            AppChatTheme(
                gradientStartColor = rgb(240, 247, 255),
                gradientEndColor = rgb(199, 224, 255),
                gradientDirection = AppChatGradientDirection.BOTTOM_TO_TOP,
                tintColor = rgb(69, 137, 246),
                leftBubbleColor = withOpacity(rgb(240, 247, 255), 0.3),
                leftBubbleTextColor = rgb(38, 38, 38),
            ),
            // 2. 薄暮紫（优雅渐变紫）
            AppChatTheme(
                gradientStartColor = rgb(245, 237, 250),
                gradientEndColor = rgb(209, 184, 235),
                tintColor = rgb(128, 90, 210),
                leftBubbleColor = withOpacity(rgb(245, 237, 250), 0.3),
                leftBubbleTextColor = rgb(38, 38, 38),
            ),
            // 3. 蜜桃粉（温暖柔和）
            AppChatTheme(
                gradientStartColor = rgb(255, 245, 242),
                gradientEndColor = rgb(255, 217, 209),
                gradientDirection = AppChatGradientDirection.BOTTOM_TO_TOP,
                tintColor = rgb(235, 105, 110),
                leftBubbleColor = withOpacity(rgb(255, 245, 242), 0.3),
                leftBubbleTextColor = rgb(38, 38, 38),
            ),
            // 4. 抹茶绿（清新自然）
            AppChatTheme(
                gradientStartColor = rgb(242, 250, 237),
                gradientEndColor = rgb(209, 237, 194),
                tintColor = rgb(76, 175, 80),
                leftBubbleColor = withOpacity(rgb(242, 250, 237), 0.3),
                leftBubbleTextColor = rgb(38, 38, 38),
            ),
            // 5. 日落橙（活力暖色）
            AppChatTheme(
                gradientStartColor = rgb(255, 247, 235),
                gradientEndColor = rgb(255, 224, 184),
                gradientDirection = AppChatGradientDirection.BOTTOM_TO_TOP,
                tintColor = rgb(255, 152, 0),
                leftBubbleColor = withOpacity(rgb(255, 247, 235), 0.3),
                leftBubbleTextColor = rgb(38, 38, 38),
            ),
            // 6. 星空靛（深邃高级感）
            AppChatTheme(
                gradientStartColor = rgb(224, 230, 247),
                gradientEndColor = rgb(158, 173, 224),
                tintColor = rgb(63, 81, 181),
                leftBubbleColor = withOpacity(rgb(224, 230, 247), 0.3),
                leftBubbleTextColor = rgb(38, 38, 38),
            ),
            // 7. 暗夜紫（深色优雅，高端质感）
            AppChatTheme(
                gradientStartColor = rgb(199, 38, 107),
                gradientEndColor = rgb(31, 15, 61),
                gradientDirection = AppChatGradientDirection.BOTTOM_TO_TOP,
                tintColor = rgb(148, 107, 242),
                leftBubbleColor = withOpacity(rgb(199, 38, 107), 0.3),
                leftBubbleTextColor = rgb(230, 230, 230),
                rightBubbleColor = withOpacity(rgb(128, 89, 224), 0.92),
                rightBubbleTextColor = withOpacity(Color.WHITE, 0.95),
            ),
            // 8. 极简灰（纯色背景，干净利落）
            AppChatTheme(
                gradientStartColor = rgb(246, 247, 250),
                gradientEndColor = rgb(246, 247, 250),
                tintColor = rgb(55, 120, 244),
                leftBubbleColor = withOpacity(Color.WHITE, 0.92),
                leftBubbleTextColor = rgb(38, 38, 38),
                rightBubbleColor = withOpacity(rgb(55, 120, 244), 0.92),
                rightBubbleTextColor = withOpacity(Color.WHITE, 0.95),
            ),
            // 9. 幻夜紫（深紫渐变，神秘高级）
            AppChatTheme(
                gradientStartColor = rgb(165, 150, 187),
                gradientEndColor = rgb(31, 31, 76),
                gradientDirection = AppChatGradientDirection.BOTTOM_TO_TOP,
                tintColor = rgb(155, 120, 240),
                leftBubbleColor = withOpacity(rgb(165, 150, 187), 0.3),
                leftBubbleTextColor = rgb(230, 230, 230),
                rightBubbleColor = withOpacity(rgb(120, 85, 210), 0.90),
                rightBubbleTextColor = withOpacity(Color.WHITE, 0.95),
            ),
            // 10. 晨雾白（极淡渐变，干净柔和）
            AppChatTheme(
                gradientStartColor = rgb(252, 250, 252),
                gradientEndColor = rgb(220, 232, 244),
                gradientDirection = AppChatGradientDirection.TOP_LEFT_TO_BOTTOM_RIGHT,
                tintColor = rgb(120, 140, 210),
                leftBubbleColor = withOpacity(Color.WHITE, 0.55),
                leftBubbleTextColor = rgb(46, 46, 46),
            ),
        )
    }
}
