package com.teneasy.chatuisdk.ui.main;

import android.content.Context.INPUT_METHOD_SERVICE
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.Selection
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.OVER_SCROLL_NEVER
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.widget.doAfterTextChanged
import androidx.emoji2.text.EmojiCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.android.chat.ui.fragment.expression.ChatEmojiInputEvent
import com.android.chat.ui.fragment.expression.EmojiFragment
import com.android.common.bean.chat.ChatExpressionNavigation
import com.android.common.bean.chat.ChatExpressionNavigationType
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.drake.brv.utils.bindingAdapter
import com.drake.brv.utils.linear
import com.drake.brv.utils.setup
import com.effective.android.panel.PanelSwitchHelper
import com.effective.android.panel.interfaces.listener.OnEditFocusChangeListener
import com.effective.android.panel.interfaces.listener.OnKeyboardStateListener
import com.effective.android.panel.interfaces.listener.OnPanelChangeListener
import com.effective.android.panel.interfaces.listener.OnViewClickListener
import com.effective.android.panel.utils.PanelUtil.clearData
import com.effective.android.panel.view.panel.IPanelView
import com.effective.android.panel.view.panel.PanelView
import com.teneasy.chatuisdk.R
import com.teneasy.chatuisdk.UploadResult
import com.teneasy.chatuisdk.databinding.FragmentKefuBinding
import com.teneasy.chatuisdk.databinding.ItemPanelExpressionBinding
import com.teneasy.chatuisdk.ui.BigImageView
import com.teneasy.chatuisdk.ui.VideoPlayView
import com.teneasy.chatuisdk.ui.base.CfLog
import com.teneasy.chatuisdk.ui.base.Utils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * 客服主界面fragment
 */
open class KeFuBaseFragment : BaseBindingFragment<FragmentKefuBinding>() {
    private lateinit var dialogBottomMenu: DialogBottomMenu
    private val mExpressionNavigation = mutableListOf<ChatExpressionNavigation>()
    private var mPanelNavigationRcv: RecyclerView? = null
    private var mPanelExpressionVP: ViewPager2? = null
    private var mLimitInput: String = ""
    var mHelper: PanelSwitchHelper? = null
    open var TAG: String = "KeFuBaseFragment"
    //0=文字输入模式 1=语音输入模式 2==禁言模式
    private var mInputType: Int = 0
    private var unfilledHeight = 0
    //    private var mRealLength = 0
    private var mMaxLength = 800

    override fun onCreateViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): FragmentKefuBinding {
        return FragmentKefuBinding.inflate(layoutInflater, parent, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if(!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }

        initEmojiView()
        setExpressionPanel()
    }

    override fun onStart() {
        super.onStart()
        initHelper()
    }


    private fun initEmojiView() {
        binding?.panelEmotion?.findViewById<RecyclerView>(R.id.rcv_navigation)?.let {
            mPanelNavigationRcv = it
            it.linear(RecyclerView.HORIZONTAL)
                .setup {
                    addType<ChatExpressionNavigation>(R.layout.item_panel_expression)
                    onClick(R.id.iv_emoji_chat) {
                        val model = getModel<ChatExpressionNavigation>()
                        if (model.select)
                            return@onClick
                        else {
                            mutable?.forEachIndexed { index, bean ->
                                if (bean is ChatExpressionNavigation) {
                                    if (bean.type == model.type) {
                                        mPanelExpressionVP?.setCurrentItem(index, true)
                                    }
                                    bean.select = bean.type == model.type
                                }
                            }
                            notifyDataSetChanged()
                        }
                    }
                    onPayload {
                        val binding = getBinding<ItemPanelExpressionBinding>()
                        val model = getModel<ChatExpressionNavigation>()
                        if (model.select) {
                            binding.ivEmojiChat.setBackgroundResource(R.drawable.bg_emoji_white)
                        } else {
                            binding.ivEmojiChat.background = null
                        }
                    }
                    onBind {
                        val binding = getBinding<ItemPanelExpressionBinding>()
                        val model = getModel<ChatExpressionNavigation>()
                        when (model.type) {
                            ChatExpressionNavigationType.CUSTOM.toString() -> binding.ivEmojiChat.setImageResource(
                                R.drawable.ic_emoji
                            )

                            ChatExpressionNavigationType.COLLECT.toString() -> binding.ivEmojiChat.setImageResource(
                                R.drawable.vector_emoji_collect
                            )

                        }
                        if (model.select) {
                            binding.ivEmojiChat.setBackgroundResource(R.drawable.bg_emoji_white)
                        } else {
                            binding.ivEmojiChat.background = null
                        }
                    }
                }

        }
        setExpressionPanel()



        binding?.panelEmotion?.findViewById<View>(R.id.iv_delete_chat)?.isEnabled = false
        binding?.panelEmotion?.findViewById<View>(R.id.iv_delete_chat)?.setOnClickListener {
            val keyCode = KeyEvent.KEYCODE_DEL
            val keyEventDown = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
            val keyEventUp = KeyEvent(KeyEvent.ACTION_UP, keyCode)
            binding?.etMsg?.onKeyDown(keyCode, keyEventDown)
            binding?.etMsg?.onKeyUp(keyCode, keyEventUp)
        }



        binding?.etMsg?.doAfterTextChanged {
            val input = it?.toString()
            input?.let {
                //                mRealLength = it.replace(Regex("\\[emoticon_\\d{1,3}\\]"), "*").length
                if (it.length > mMaxLength) {
                    binding?.etMsg?.setText(mLimitInput)
                    Selection.setSelection(binding?.etMsg?.text, mLimitInput.length)
                } else {
                    mLimitInput = it
                }
            }
            if (TextUtils.isEmpty(binding?.etMsg?.text)) {
                binding?.panelEmotion?.findViewById<View>(R.id.iv_delete_chat)?.isEnabled = false
                binding?.btnSend?.visibility = View.GONE
                if (binding?.panelEmotion?.visibility != View.VISIBLE) {
                    binding?.etMsg?.clearFocus()
                }
            } else {
                binding?.panelEmotion?.findViewById<View>(R.id.iv_delete_chat)?.isEnabled = true
                binding?.btnSend?.visibility = View.VISIBLE
                binding?.etMsg?.requestFocus()
            }
        }
    }

    private fun setExpressionPanel() {
        mExpressionNavigation.clear()
        mExpressionNavigation.add(
            ChatExpressionNavigation(
                null,
                ChatExpressionNavigationType.CUSTOM.toString(),
                true
            )
        )

        mPanelNavigationRcv?.bindingAdapter?.models = mExpressionNavigation
        binding?.panelEmotion?.findViewById<ViewPager2>(R.id.viewPager2)?.apply {
            getChildAt(0).overScrollMode = OVER_SCROLL_NEVER
            mPanelExpressionVP = this
            val data = mutableListOf<Fragment>()

            mExpressionNavigation.forEach {
                when (it.type) {
                    ChatExpressionNavigationType.CUSTOM.toString() -> {
                        data.add(EmojiFragment())
                    }
                }
            }

            adapter = PanelExpressionPagerAdapter(requireActivity(), data)
            offscreenPageLimit = data.size
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    if (position == 0) {
                        binding?.panelEmotion?.findViewById<View>(R.id.tv_more)?.visibility =
                            View.INVISIBLE
                        binding?.panelEmotion?.findViewById<View>(R.id.iv_delete_chat)?.visibility =
                            View.VISIBLE
                    }
                    mPanelNavigationRcv?.bindingAdapter?.mutable?.forEachIndexed { index, any ->
                        if (any is ChatExpressionNavigation) {
                            any.select = index == position
                            mPanelNavigationRcv?.bindingAdapter?.notifyItemChanged(index, "payload")
                        }
                    }
                }
            })

        }
    }

    /**
     * 初始化面板切换帮助器
     */
    private fun initHelper() {
        if (mHelper == null) {
            mHelper = PanelSwitchHelper.Builder(this) //可选
                .addKeyboardStateListener(object : OnKeyboardStateListener {
                    override fun onKeyboardChange(visible: Boolean, height: Int) {
                       
                    }
                }) //可选
                .addEditTextFocusChangeListener(object : OnEditFocusChangeListener {
                    override fun onFocusChange(view: View?, hasFocus: Boolean) {
                        CfLog.d(
                            TAG, "输入框是否获得焦点 : $hasFocus"
                        )
                        if (hasFocus) {
                          //  scrollToBottom()
                        }
                    }
                }) //可选
//                .addViewClickListener(object : OnViewClickListener {
//                    override fun onClickBefore(view: View?) {
//                        when (view?.id) {
//                             R.id.ivEmoj -> {
//                                clearData(requireContext())
//                                if (view.id == R.id.ivEmoj) {
//                                    if (mInputType == 1) {
//                                        showEmotionPanel()
//                                    }
//                                }
////                                if (view.id == R.id.btn_more) {
////                                    showTextModeNoKeyBoard()
////                                }
//
//                            }
//
//                        }
//                        CfLog.d(TAG, "点击了View : $view")
//                    }
//                }) //可选
                .addPanelChangeListener(object : OnPanelChangeListener {
                    override fun onKeyboard() {
                        CfLog.d(TAG, "唤起系统输入法")
                        //scrollToBottom()
                        binding?.ivEmoj?.isSelected = false
                        binding?.ivEmoj?.setImageResource(R.drawable.emoj)
                    }

                    override fun onNone() {
                        CfLog.d(TAG, "隐藏所有面板")
                        binding?.ivEmoj?.isSelected = false
                        binding?.ivEmoj?.setImageResource(R.drawable.emoj)
                    }

                    override fun onPanel(panel: IPanelView?) {
                        CfLog.d(TAG, "唤起面板 : $panel")
                        if (panel is PanelView) {
                            //scrollToBottom()
                            if (panel.id == R.id.panel_emotion) {
                                //表情
                                binding?.ivEmoj?.isSelected = true
                                binding?.ivEmoj?.setImageResource(R.drawable.ht_shuru)

                            }

                            /*else if (panel.id == R.id.panel_more) {
                                //更多

                            }*/

                        }
                    }

                    override fun onPanelSizeChange(
                        panelView: IPanelView?,
                        portrait: Boolean,
                        oldWidth: Int,
                        oldHeight: Int,
                        width: Int,
                        height: Int
                    ) {

                    }
                })
                .addContentScrollMeasurer { //可选，滑动模式下，可以针对内容面板内的view，定制滑动距离，默认滑动距离为 defaultDistance
                    getScrollDistance { defaultDistance ->
                        defaultDistance - unfilledHeight
                    }
                    getScrollViewId { R.id.rcv_msg }
                }.addPanelHeightMeasurer {
                    getTargetPanelDefaultHeight { Utils().dp2px(250f) }
                    getPanelTriggerId { R.id.ivEmoj }
                }.logTrack(false).build()
        }

        //binding?.rcvMsg?.setPanelSwitchHelper(mHelper)
    }
    /**
     * 表情输入模式
     *
     */
//    fun showEmotionPanel() {
//        binding?.ivEmoj?.setImageResource(R.drawable.ht_shuru)
//        binding?.etMsg?.visibility = View.VISIBLE
//        binding?.etMsg?.requestFocus()
//        mHelper?.toPanelState(R.id.panel_emotion)
//    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onChatEmojiEvent(event: ChatEmojiInputEvent) {
        if (this.isVisible) {
            try {
                if (event.position == 0)
                    return
                val sb =
                    SpannableStringBuilder(this.binding?.etMsg?.text)

                val emoji = "[emoticon_${event.position}]"
                // 最大长度，对应maxLength
                if (sb.toString().length + emoji.length > mMaxLength) {
                    return
                }
                sb.append(emoji)
                this.binding?.etMsg?.text = sb
                if (sb.isNotEmpty()) {
                    this.binding?.etMsg?.setSelection(sb.length)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }

    private inner class PanelExpressionPagerAdapter(
        fa: FragmentActivity,
        var data: MutableList<Fragment>
    ) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = data.size

        override fun createFragment(position: Int): Fragment {
            return data[position]
        }
    }

}