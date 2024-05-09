package com.teneasy.chatuisdk.ui.main

import android.content.Context
import com.donkingliang.groupedadapter.adapter.GroupedRecyclerViewAdapter
import com.donkingliang.groupedadapter.holder.BaseViewHolder
import com.teneasy.chatuisdk.R
import com.teneasy.chatuisdk.ui.http.bean.QA
import com.teneasy.chatuisdk.ui.http.bean.Question

class groupedQuestionAdapter(
    var context: Context, var data: MutableList<QA>,
    var selectedAuthKind: Question
) : GroupedRecyclerViewAdapter(context) {

    override fun getGroupCount(): Int {
        return data.size
    }

    override fun getChildrenCount(position: Int): Int {
        if (!isExpand(position)) {
            return 0
        }
        val qa = data[position]
        return qa.related?.size ?: 0
    }

    override fun hasHeader(position: Int): Boolean {
        return true
    }

    override fun hasFooter(position: Int): Boolean {
        return false
    }

    override fun getHeaderLayout(viewType: Int): Int {
        return R.layout.simple_list_item
    }

    override fun getFooterLayout(viewType: Int): Int {
        return 0
    }

    override fun getChildLayout(viewType: Int): Int {
        return R.layout.simple_list_item
    }

    override fun onBindHeaderViewHolder(
        holder: BaseViewHolder?, position: Int
    ) {
        val bean = data[position]
        holder?.setText(R.id.tv_title, bean.question.content.data)

    }

    override fun onBindFooterViewHolder(
        holder: BaseViewHolder?, position: Int
    ) {
    }

    override fun onBindChildViewHolder(
        holder: BaseViewHolder?, position: Int, childPosition: Int
    ) {

        val bean = data[position]
        holder?.setText(R.id.tv_title, bean.related?.get(childPosition)?.question?.content?.data ?:"")
    }

    /**
     * 判断当前组是否展开
     *
     * @param position
     * @return
     */
    fun isExpand(position: Int): Boolean {
        val entity = data[position]
        return entity.isExpand
    }

    /**
     * 展开一个组
     *
     * @param position
     */
    fun expandGroup(position: Int) {
        expandGroup(position, false)
    }

    /**
     * 展开一个组
     *
     * @param position
     * @param animate
     */
    fun expandGroup(position: Int, animate: Boolean) {
        val entity = data[position]
        entity.isExpand = true
        if (animate) {
            notifyChildrenInserted(position)
        } else {
            notifyDataChanged()
        }
    }

    /**
     * 收起一个组
     *
     * @param position
     */
    fun collapseGroup(position: Int) {
        collapseGroup(position, false)
    }

    fun collapseTheResetGroup(position: Int) {
        for (i in 0..data.size - 1) {
            collapseGroup(i, false)
        }
    }

    /**
     * 收起一个组
     *
     * @param position
     * @param animate
     */
    private fun collapseGroup(position: Int, animate: Boolean) {
        val entity = data[position]
        entity.isExpand = false
        if (animate) {
            notifyChildrenRemoved(position)
        } else {
            notifyDataChanged()
        }
    }
}