package com.teneasy.chatuisdk.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OVER_SCROLL_NEVER
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.android.common.bean.chat.ChatExpressionNavigation
import com.android.common.bean.chat.ChatExpressionNavigationType
import com.teneasy.chatuisdk.R

abstract class BaseBindingFragment<T : ViewBinding?> : Fragment() {
    // 子类使用该方法来使用binding
    var binding: T? = null
        private set

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 调用onCreateViewBinding方法获取binding
        binding = onCreateViewBinding(inflater, container)
        initView()
        return binding!!.root
    }

    protected open fun initView() {

    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 引用置空处理
        binding = null
    }

    // 由子类去重写
    protected abstract fun onCreateViewBinding(inflater: LayoutInflater, parent: ViewGroup?): T
}