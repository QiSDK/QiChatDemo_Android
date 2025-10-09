package com.teneasy.chatuisdk.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.teneasy.chatuisdk.Consults
import com.teneasy.chatuisdk.databinding.ConsultTypeItemBinding
import com.teneasy.chatuisdk.ui.base.Constants

class SelectConsultTypeAdapter(
    private val dataList: ArrayList<Consults>,
    private val onItemClick: (Consults) -> Unit
) : RecyclerView.Adapter<SelectConsultTypeAdapter.NormalViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NormalViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ConsultTypeItemBinding.inflate(inflater, parent, false)
        return NormalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NormalViewHolder, position: Int) {
        val item = dataList[position]
        holder.tvTitle.text = item.name
        holder.itemView.setOnClickListener { onItemClick(item) }

        // 从全局未读数列表中获取实时未读数
        val unreadCount = Constants.getUnreadCount(item.consultId ?: 0L)
        if (unreadCount > 0) {
            holder.tvRedDotView.setUnreadCount(unreadCount)
            holder.tvRedDotView.visibility = View.VISIBLE
        } else {
            holder.tvRedDotView.visibility = View.GONE
        }

        if (item.Works.isNotEmpty()) {
            val url = Constants.baseUrlImage + item.Works[0].avatar
            Glide.with(holder.civKefuImage)
                .load(url)
                .dontAnimate()
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(holder.civKefuImage)
        } else {
            holder.civKefuImage.setImageDrawable(null)
        }
    }

    override fun getItemCount(): Int = dataList.size

    fun updateData(newData: List<Consults>) {
        dataList.clear()
        dataList.addAll(newData)
        notifyDataSetChanged()
    }

    class NormalViewHolder(binding: ConsultTypeItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val tvTitle = binding.tvTitle
        val tvRedDotView = binding.redDotView
        val civKefuImage = binding.civKefuImage
    }
}
