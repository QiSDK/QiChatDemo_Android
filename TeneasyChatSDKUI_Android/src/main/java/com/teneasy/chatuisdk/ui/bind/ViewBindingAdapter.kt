package com.teneasy.chatuisdk.ui.main

import android.icu.text.Transliterator
import android.text.TextWatcher
import android.view.View
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView

object ViewBindingAdapter {

    @BindingAdapter("clickListener")
    @JvmStatic
    fun setOnclickListener(view: View, listener: View.OnClickListener) {
        view.setOnClickListener(listener)
    }

    @BindingAdapter("setImage")
    @JvmStatic
    fun setImage(view: ImageView, resImgId: Int) {
        view.setImageResource(resImgId)
    }

    @BindingAdapter("textChangedListener")
    @JvmStatic
    fun textChangedListener(view: TextView, listener: TextWatcher) {
        view.addTextChangedListener(listener)
    }

    @BindingAdapter("smoothScrollToPosition")
    @JvmStatic
    fun smoothScrollToPosition(listView: RecyclerView, position: Int) {
        listView.smoothScrollToPosition(position)
    }

    @BindingAdapter("focusChangeListener")
    @JvmStatic
    fun setOnFocusChangeListener(view: View, listener: View.OnFocusChangeListener) {
        view.onFocusChangeListener = listener
    }



    @BindingAdapter("itemClickListener")
    @JvmStatic
    fun setOnItemClickListener(view: AbsListView, listener: AdapterView.OnItemClickListener) {
        view.onItemClickListener = listener
    }

    @BindingAdapter("selected")
    @JvmStatic
    fun setSelected(view: View, selected: Boolean) {
        view.isSelected = selected
    }
}