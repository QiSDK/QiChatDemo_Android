package com.android.common.view.chat.emoji

import com.chad.library.adapter.base.entity.MultiItemEntity

data class EmojiPanText(
    val text: String,
    val showDelete:Boolean= false,
    override val itemType: Int = EmojiPanAdapter.EMOJI_PAN_TEXT,
) : MultiItemEntity