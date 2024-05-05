package com.teneasy.chatuisdk.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.teneasy.chatuisdk.R

class MyAdapter (private val data: List<String>) : RecyclerView.Adapter<MyAdapter.NormalViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NormalViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.simple_list_item, parent, false)
        return NormalViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: NormalViewHolder, position: Int) {
        holder.tv_title.text = data[position]
    }

    inner class NormalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val tv_title: TextView
        init {
            tv_title = itemView.findViewById(R.id.tv_title)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }
}