package com.teneasy.chatuisdk.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.teneasy.chatuisdk.Consults
import com.teneasy.chatuisdk.R

class MyAdapter (private val data: ArrayList<Consults>) : RecyclerView.Adapter<MyAdapter.NormalViewHolder>() {
    private var dataList: ArrayList<Consults> = data
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NormalViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.simple_list_item, parent, false)
        return NormalViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: NormalViewHolder, position: Int) {
        holder.tv_title.text = data[position].name
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

    fun updateData(newData: ArrayList<Consults>) {
        dataList.clear()
        dataList.addAll(newData)
        notifyDataSetChanged()
    }
}