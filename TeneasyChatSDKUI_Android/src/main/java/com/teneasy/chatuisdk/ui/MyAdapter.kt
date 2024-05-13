package com.teneasy.chatuisdk.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.teneasy.chatuisdk.Consults
import com.teneasy.chatuisdk.R
import com.teneasy.chatuisdk.databinding.SimpleListItemBinding
import com.teneasy.chatuisdk.ui.base.Constants
import com.teneasy.chatuisdk.ui.base.PARAM_WSS_BASE_URL

class MyAdapter (private val data: ArrayList<Consults>) : RecyclerView.Adapter<MyAdapter.NormalViewHolder>() {
    private var dataList: ArrayList<Consults> = data
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NormalViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = SimpleListItemBinding.inflate(inflater, parent, false)
        return NormalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NormalViewHolder, position: Int) {
        holder.tvTitle.text = data[position].name
        holder.itemView.setOnClickListener {
            Constants.CONSULT_ID = data[position].consultId?:0L

            var bundle = Bundle()
            bundle.putString(PARAM_WSS_BASE_URL, Constants.baseUrl)
            it.findNavController().navigate(R.id.frg_kefu_main, bundle)
        }
    }

    class NormalViewHolder(binding: SimpleListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val tvTitle = binding.tvTitle
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