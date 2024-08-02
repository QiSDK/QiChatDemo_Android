package com.android.common.view.chat.emoji

import com.chad.library.adapter.base.entity.MultiItemEntity

data class EmojiPanEmoji(
    val id: Int,
    override val itemType: Int = EmojiPanAdapter.EMOJI_PAN_EMOJI,
) : MultiItemEntity