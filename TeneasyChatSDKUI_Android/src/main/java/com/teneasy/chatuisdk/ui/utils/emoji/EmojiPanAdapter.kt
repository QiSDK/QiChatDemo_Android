package com.teneasy.chatuisdk.ui.utils.emoji

import com.chad.library.adapter.base.BaseMultiItemQuickAdapter
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.teneasy.chatuisdk.R

class EmojiPanAdapter(data: MutableList<MultiItemEntity>) : BaseMultiItemQuickAdapter<MultiItemEntity, BaseViewHolder>(data) {

    init {
        addItemType(EMOJI_PAN_TEXT, R.layout.item_emoji_text)
        addItemType(EMOJI_PAN_EMOJI, R.layout.item_emoji_emoji)
    }

    override fun convert(holder: BaseViewHolder, item: MultiItemEntity) {
        when (item) {
            is EmojiPanText -> holder.setText(R.id.tv_emoji_item, item.text)
            is EmojiPanEmoji -> holder.setImageResource(R.id.iv_emoji_item, item.id)
        }
    }

    companion object {
        const val EMOJI_PAN_TEXT = 0
        const val EMOJI_PAN_EMOJI = 1
    }
}