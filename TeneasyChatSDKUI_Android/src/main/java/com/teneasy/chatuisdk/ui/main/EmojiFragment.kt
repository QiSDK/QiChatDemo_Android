package com.android.chat.ui.fragment.expression

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.android.common.view.chat.emoji.EmojiPanAdapter
import com.android.common.view.chat.emoji.EmojiPanEmoji
import com.android.common.view.chat.emoji.EmojiPanText
import com.android.common.view.chat.emoji.EmoticonHandler
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.teneasy.chatuisdk.R
import com.teneasy.chatuisdk.databinding.FragmentKefuBinding
import com.teneasy.chatuisdk.databinding.LayoutEmojiBinding
import com.teneasy.chatuisdk.ui.main.BaseBindingFragment
import org.greenrobot.eventbus.EventBus

data class ChatEmojiInputEvent(val position:Int) {
}

class EmojiFragment  : BaseBindingFragment<LayoutEmojiBinding>() {
    private var mAdapter: EmojiPanAdapter? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        if(!EventBus.getDefault().isRegistered(this)) {
//            EventBus.getDefault().register(this)
//        }

        binding?.rcv?.layoutManager = GridLayoutManager(context, 8).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (position) {
                        0 -> 8
                        else -> 1
                    }
                }
            }
        }
        val data =
            mutableListOf<MultiItemEntity>(EmojiPanText(getString(R.string.all_emoji)))
        data.addAll(EmoticonHandler.sEmojisMap.values.map { EmojiPanEmoji(it) })
        mAdapter = EmojiPanAdapter(data)
        binding?.rcv?.adapter = mAdapter!!

        mAdapter?.setOnItemClickListener { _, _, position ->
            EventBus.getDefault().post(ChatEmojiInputEvent(position))
        }
    }



    override fun onCreateViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): LayoutEmojiBinding {
        return LayoutEmojiBinding.inflate(layoutInflater, parent, false)
    }
}