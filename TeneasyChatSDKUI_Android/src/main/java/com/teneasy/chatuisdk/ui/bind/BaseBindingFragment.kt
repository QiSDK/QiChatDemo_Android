package com.teneasy.chatuisdk.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

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

    protected open fun initView() {}

    override fun onDestroyView() {
        super.onDestroyView()
        // 引用置空处理
        binding = null
    }

    // 由子类去重写
    protected abstract fun onCreateViewBinding(inflater: LayoutInflater, parent: ViewGroup?): T
}