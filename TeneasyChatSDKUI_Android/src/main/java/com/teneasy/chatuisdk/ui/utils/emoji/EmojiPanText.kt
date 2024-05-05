package com.teneasy.chatuisdk.ui.utils.emoji

import com.chad.library.adapter.base.entity.MultiItemEntity

data class EmojiPanText(
    val text: String,
    override val itemType: Int = EmojiPanAdapter.EMOJI_PAN_TEXT,
) : MultiItemEntity