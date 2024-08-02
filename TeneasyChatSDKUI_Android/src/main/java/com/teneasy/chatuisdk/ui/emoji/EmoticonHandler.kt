package com.android.common.view.chat.emoji

import android.content.Context
import android.text.Spannable
import com.teneasy.chatuisdk.R

object EmoticonHandler {
    val sEmojisMap = mapOf(
        "[emoticon_1]" to R.mipmap.emoticon_1,
        "[emoticon_2]" to R.mipmap.emoticon_2,
        "[emoticon_3]" to R.mipmap.emoticon_3,
        "[emoticon_4]" to R.mipmap.emoticon_4,
        "[emoticon_5]" to R.mipmap.emoticon_5,
        "[emoticon_6]" to R.mipmap.emoticon_6,
        "[emoticon_7]" to R.mipmap.emoticon_7,
        "[emoticon_8]" to R.mipmap.emoticon_8,
        "[emoticon_9]" to R.mipmap.emoticon_9,
        "[emoticon_10]" to R.mipmap.emoticon_10,
        "[emoticon_11]" to R.mipmap.emoticon_11,
        "[emoticon_12]" to R.mipmap.emoticon_12,
        "[emoticon_13]" to R.mipmap.emoticon_13,
        "[emoticon_14]" to R.mipmap.emoticon_14,
        "[emoticon_15]" to R.mipmap.emoticon_15,
        "[emoticon_16]" to R.mipmap.emoticon_16,
        "[emoticon_17]" to R.mipmap.emoticon_17,
        "[emoticon_18]" to R.mipmap.emoticon_18,
        "[emoticon_19]" to R.mipmap.emoticon_19,
        "[emoticon_20]" to R.mipmap.emoticon_20,
        "[emoticon_21]" to R.mipmap.emoticon_21,
        "[emoticon_22]" to R.mipmap.emoticon_22,
        "[emoticon_23]" to R.mipmap.emoticon_23,
        "[emoticon_24]" to R.mipmap.emoticon_24,
        "[emoticon_25]" to R.mipmap.emoticon_25,
        "[emoticon_26]" to R.mipmap.emoticon_26,
        "[emoticon_27]" to R.mipmap.emoticon_27,
        "[emoticon_28]" to R.mipmap.emoticon_28,
        "[emoticon_29]" to R.mipmap.emoticon_29,
        "[emoticon_30]" to R.mipmap.emoticon_30,
        "[emoticon_31]" to R.mipmap.emoticon_31,
        "[emoticon_32]" to R.mipmap.emoticon_32,
        "[emoticon_33]" to R.mipmap.emoticon_33,
        "[emoticon_34]" to R.mipmap.emoticon_34,
        "[emoticon_35]" to R.mipmap.emoticon_35,
        "[emoticon_36]" to R.mipmap.emoticon_36,
        "[emoticon_37]" to R.mipmap.emoticon_37,
        "[emoticon_38]" to R.mipmap.emoticon_38,
        "[emoticon_39]" to R.mipmap.emoticon_39,
        "[emoticon_40]" to R.mipmap.emoticon_40,
        "[emoticon_41]" to R.mipmap.emoticon_41,
        "[emoticon_42]" to R.mipmap.emoticon_42,
        "[emoticon_43]" to R.mipmap.emoticon_43,
        "[emoticon_44]" to R.mipmap.emoticon_44,
        "[emoticon_45]" to R.mipmap.emoticon_45,
        "[emoticon_46]" to R.mipmap.emoticon_46,
        "[emoticon_47]" to R.mipmap.emoticon_47,
        "[emoticon_48]" to R.mipmap.emoticon_48,
        "[emoticon_49]" to R.mipmap.emoticon_49,
        "[emoticon_50]" to R.mipmap.emoticon_50,
        "[emoticon_51]" to R.mipmap.emoticon_51,
        "[emoticon_52]" to R.mipmap.emoticon_52,
        "[emoticon_53]" to R.mipmap.emoticon_53,
        "[emoticon_54]" to R.mipmap.emoticon_54,
        "[emoticon_55]" to R.mipmap.emoticon_55,
        "[emoticon_56]" to R.mipmap.emoticon_56,
        "[emoticon_57]" to R.mipmap.emoticon_57,
        "[emoticon_58]" to R.mipmap.emoticon_58,
        "[emoticon_59]" to R.mipmap.emoticon_59,
        "[emoticon_60]" to R.mipmap.emoticon_60,
        "[emoticon_61]" to R.mipmap.emoticon_61,
        "[emoticon_62]" to R.mipmap.emoticon_62,
        "[emoticon_63]" to R.mipmap.emoticon_63,
        "[emoticon_64]" to R.mipmap.emoticon_64,
        "[emoticon_65]" to R.mipmap.emoticon_65,
        "[emoticon_66]" to R.mipmap.emoticon_66,
        "[emoticon_67]" to R.mipmap.emoticon_67,
        "[emoticon_68]" to R.mipmap.emoticon_68,
        "[emoticon_69]" to R.mipmap.emoticon_69,
        "[emoticon_70]" to R.mipmap.emoticon_70,
        "[emoticon_71]" to R.mipmap.emoticon_71,
        "[emoticon_72]" to R.mipmap.emoticon_72,
        "[emoticon_73]" to R.mipmap.emoticon_73,
        "[emoticon_74]" to R.mipmap.emoticon_74,
        "[emoticon_75]" to R.mipmap.emoticon_75,
        "[emoticon_76]" to R.mipmap.emoticon_76,
        "[emoticon_77]" to R.mipmap.emoticon_77,
        "[emoticon_78]" to R.mipmap.emoticon_78,
        "[emoticon_79]" to R.mipmap.emoticon_79,
        "[emoticon_80]" to R.mipmap.emoticon_80,
        "[emoticon_81]" to R.mipmap.emoticon_81,
        "[emoticon_82]" to R.mipmap.emoticon_82,
        "[emoticon_83]" to R.mipmap.emoticon_83,
        "[emoticon_84]" to R.mipmap.emoticon_84,
        "[emoticon_85]" to R.mipmap.emoticon_85,
        "[emoticon_86]" to R.mipmap.emoticon_86,
        "[emoticon_87]" to R.mipmap.emoticon_87,
        "[emoticon_88]" to R.mipmap.emoticon_88,
        "[emoticon_89]" to R.mipmap.emoticon_89,
        "[emoticon_90]" to R.mipmap.emoticon_90,
        "[emoticon_91]" to R.mipmap.emoticon_91,
        "[emoticon_92]" to R.mipmap.emoticon_92,
        "[emoticon_93]" to R.mipmap.emoticon_93,
        "[emoticon_94]" to R.mipmap.emoticon_94,
        "[emoticon_95]" to R.mipmap.emoticon_95,
        "[emoticon_96]" to R.mipmap.emoticon_96,
        "[emoticon_97]" to R.mipmap.emoticon_97,
        "[emoticon_98]" to R.mipmap.emoticon_98,
        "[emoticon_99]" to R.mipmap.emoticon_99,
        "[emoticon_100]" to R.mipmap.emoticon_100,
        "[emoticon_101]" to R.mipmap.emoticon_101,
        "[emoticon_102]" to R.mipmap.emoticon_102,
        "[emoticon_103]" to R.mipmap.emoticon_103,
        "[emoticon_104]" to R.mipmap.emoticon_104,
        "[emoticon_105]" to R.mipmap.emoticon_105,
        "[emoticon_106]" to R.mipmap.emoticon_106,
        "[emoticon_107]" to R.mipmap.emoticon_107,
        "[emoticon_108]" to R.mipmap.emoticon_108,
        "[emoticon_109]" to R.mipmap.emoticon_109,
        "[emoticon_110]" to R.mipmap.emoticon_110,
        "[emoticon_111]" to R.mipmap.emoticon_111,
        "[emoticon_112]" to R.mipmap.emoticon_112,
        "[emoticon_113]" to R.mipmap.emoticon_113,
        "[emoticon_114]" to R.mipmap.emoticon_114,
        "[emoticon_115]" to R.mipmap.emoticon_115,
        "[emoticon_116]" to R.mipmap.emoticon_116,
        "[emoticon_117]" to R.mipmap.emoticon_117,
        "[emoticon_118]" to R.mipmap.emoticon_118,
        "[emoticon_119]" to R.mipmap.emoticon_119,
        "[emoticon_120]" to R.mipmap.emoticon_120,
        "[emoticon_121]" to R.mipmap.emoticon_121,
        "[emoticon_122]" to R.mipmap.emoticon_122,
        "[emoticon_123]" to R.mipmap.emoticon_123,
        "[emoticon_124]" to R.mipmap.emoticon_124,
        "[emoticon_125]" to R.mipmap.emoticon_125,
        "[emoticon_126]" to R.mipmap.emoticon_126,
        "[emoticon_127]" to R.mipmap.emoticon_127,
        "[emoticon_128]" to R.mipmap.emoticon_128,
        "[emoticon_129]" to R.mipmap.emoticon_129,
        "[emoticon_130]" to R.mipmap.emoticon_130,
        "[emoticon_131]" to R.mipmap.emoticon_131,
        "[emoticon_132]" to R.mipmap.emoticon_132,
        "[emoticon_133]" to R.mipmap.emoticon_133,
        "[emoticon_134]" to R.mipmap.emoticon_134,
        "[emoticon_135]" to R.mipmap.emoticon_135,
        "[emoticon_136]" to R.mipmap.emoticon_136,
    )
    private var mEmoticonSpan: EmoticonSpan? = null
    fun addEmojis(
        context: Context,
        text: Spannable?,
        emojiSize: Int,
        emojiAlignment: Int,
        textSize: Int
    ) {

        if (text == null) return

        val textLength = text.length
        if (textLength <= 10) return

        val oldSpans = text.getSpans(0, textLength, EmoticonSpan::class.java)
        for (oldSpan in oldSpans) {
            text.removeSpan(oldSpan)
        }

        var emojiStartIndex = -1
        var emojiEndIndex = -1
        for ((i, c) in text.withIndex()) {
            if (c.toString() == "[" && isEmoticonMark(text, i)) {
                emojiStartIndex = i
            } else if (c.toString() == "]" && emojiStartIndex > -1) {
                emojiEndIndex = i + 1
            }

            if (emojiStartIndex > -1 && emojiEndIndex > -1) {
                val emoji = sEmojisMap[text.substring(emojiStartIndex, emojiEndIndex)] ?: return
                mEmoticonSpan =
                    EmoticonSpan(context.applicationContext, emoji, emojiSize, emojiAlignment, textSize)
                text.setSpan(
                    mEmoticonSpan,
                    emojiStartIndex,
                    emojiEndIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                emojiEndIndex = -1
                emojiStartIndex = -1
            }
        }
    }

    private fun isEmoticonMark(text: Spannable, i: Int) = try {
        text.substring(i, i + 10) == "[emoticon_"
    } catch (e: Exception) {
        false
    }


}