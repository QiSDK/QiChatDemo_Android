package com.teneasy.chatuisdk.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.teneasy.chatuisdk.Consults
import com.teneasy.chatuisdk.R
import com.teneasy.chatuisdk.SelectConsultTypeViewModel
import com.teneasy.chatuisdk.databinding.ConsultTypeItemBinding
import com.teneasy.chatuisdk.ui.base.Constants
import com.teneasy.chatuisdk.ui.base.PARAM_DOMAIN

class SelectConsultTypeAdapter (private val data: ArrayList<Consults>) : RecyclerView.Adapter<SelectConsultTypeAdapter.NormalViewHolder>() {
    private var dataList: ArrayList<Consults> = data
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NormalViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ConsultTypeItemBinding.inflate(inflater, parent, false)
        return NormalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NormalViewHolder, position: Int) {
        holder.tvTitle.text = data[position].name
        holder.itemView.setOnClickListener {
            Constants.CONSULT_ID = data[position].consultId ?: 0L

            var bundle = Bundle()
            bundle.putString(PARAM_DOMAIN, Constants.domain)
            it.findNavController().navigate(R.id.frg_kefu_main, bundle)

            //未读数清0
            val viewModel = SelectConsultTypeViewModel()
            viewModel.markRead()
        }
        if (data[position].unread ?: 0 > 0) {
            holder.tvRedDotView.setUnreadCount(data[position].unread ?: 0)
            holder.tvRedDotView.visibility = View.VISIBLE
        } else {
            holder.tvRedDotView.visibility = View.GONE
        }

        if (data.get(position).Works.size > 0) {
            val url = Constants.baseUrlImage + data.get(position).Works[0].avatar
            print("avatar:$url")
            Glide.with(holder.civKefuImage).load(url).dontAnimate()
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(holder.civKefuImage)
        }
    }

    class NormalViewHolder(binding: ConsultTypeItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val tvTitle = binding.tvTitle
        val tvRedDotView = binding.redDotView
        val civKefuImage = binding.civKefuImage
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