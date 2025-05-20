package com.teneasy.chatuisdk.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.teneasy.chatuisdk.R
import com.teneasy.chatuisdk.ui.base.Constants

class ImageAdapter(private val imageUrls: List<String>) :
    RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_text_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUrl = imageUrls[position]
        holder.bind(imageUrl)
    }

    //override fun getItemCount(): Int = imageUrls.size

    override fun getItemCount(): Int{
        return 9
        //return imageUrls.size
    }

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.iv_image)
        private val ivPlay: ImageView = itemView.findViewById(R.id.iv_play)

        fun bind(imageUrl: String) {
            // Use Glide (or your preferred image loading library) to load the image
            // You might need to prepend a base URL if the image URLs are relative
            Glide.with(itemView.context)
                .load(Constants.baseUrlImage + imageUrl) // If imageUrls are full URLs
                // .load("YOUR_BASE_URL" + imageUrl) // If imageUrls are relative paths
                .placeholder(R.drawable.image_default) // Optional: placeholder image
                .error(R.drawable.loading_animation)         // Optional: error image
                .into(imageView)
        }
    }
}