package com.teneasy.chatuisdk.ui.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.luck.picture.lib.utils.ToastUtils
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.interfaces.OnSelectListener
import com.teneasy.chatuisdk.ARG_IMAGEURL
import com.teneasy.chatuisdk.ARG_KEFUNAME
import com.teneasy.chatuisdk.FullImageActivity
import com.teneasy.chatuisdk.R
import com.teneasy.chatuisdk.ui.base.Constants
import com.teneasy.chatuisdk.ui.base.Utils

interface ImageOnListener {
    fun onQuote(position: Int)
}

class ImageAdapter(private val imageUrls: List<String>, private val act: Activity, private var listener: ImageOnListener) :
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

    override fun getItemCount(): Int = imageUrls.size

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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

            imageView.setOnClickListener {
                onPlayImage(Constants.baseUrlImage + imageUrl)
            }

            val builder2: XPopup.Builder = XPopup.Builder(act)
                .watchView(imageView)
            imageView.setOnLongClickListener(View.OnLongClickListener {
                builder2.asAttachList(
                    arrayOf<String>("回复", "下载"), null,
                    object : OnSelectListener {
                        override fun onSelect(position: Int, text: String) {
                            when (position) {
                                0 -> {
                                    listener.onQuote(position)
                                }
                                1 -> {
                                    //删除
                                    println("下载")
                                    Utils().downloadFile(Constants.baseUrlImage + imageUrl, object :
                                            (Int) -> Unit {
                                        override fun invoke(progress: Int) {
                                            if (progress == 100) {
                                                ToastUtils.showToast(
                                                    act,
                                                    "下载成功！"
                                                );
                                            } else if (progress == -1) {
                                                ToastUtils.showToast(
                                                    act,
                                                    "下载失败！"
                                                );
                                                return
                                            }
                                        }
                                    });

                                }
                            }
                        }
                    })
                    .show()
                false
            })
        }


    }
    fun onPlayImage(url: String) {
        val intent = Intent(this.act, FullImageActivity::class.java)
        intent.putExtra(ARG_IMAGEURL, url)
        intent.putExtra(ARG_KEFUNAME, "")
        intent.setClass(act, FullImageActivity::class.java)
        act.startActivity(intent)
    }

}