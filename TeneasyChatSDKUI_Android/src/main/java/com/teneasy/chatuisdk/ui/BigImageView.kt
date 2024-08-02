package com.teneasy.chatuisdk.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.luck.picture.lib.utils.ToastUtils
import com.lxj.xpopup.impl.FullScreenPopupView
import com.teneasy.chatuisdk.R
import com.teneasy.chatuisdk.ui.base.CapturePhotoUtils
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit


class BigImageView(context: Context, url: String): FullScreenPopupView (context){

    private var url: String? = url

    override fun getImplLayoutId(): Int {
        return R.layout.fragment_image_pop
    }


    override fun onCreate() {
        super.onCreate()

        val ivBig = findViewById<ImageView>(R.id.ivBig)
        Glide.with(context).load(url).dontAnimate()
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .into(ivBig)

        val tvClose = findViewById<ImageView>(R.id.tv_close)
        tvClose.setOnClickListener {
           this.dismiss()
        }

        val tvSave = findViewById<TextView>(R.id.tv_save)
        tvSave.setOnClickListener {
            download(url?: "");
        }
    }

    fun download(url: String){

        if (url.isEmpty()){
            return
        }

        val okHttpClient: OkHttpClient = OkHttpClient().newBuilder()
            .connectTimeout(50, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.MINUTES)
            .readTimeout(5, TimeUnit.MINUTES)
            .build()

        val request: Request = Request.Builder().url(url).build()
        okHttpClient.newCall(request).enqueue(object : Callback {


            override fun onFailure(call: Call, e: IOException) {

            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val bitmap = BitmapFactory.decodeStream(response.body?.byteStream())
                    val savedir = CapturePhotoUtils.saveImageInQ(bitmap)
                    if (savedir != null){
                        ToastUtils.showToast(context, "保存成功")
                    }else{
                        ToastUtils.showToast(context, "保存失败")
                    }
                } else {
                    //Handle the error
                }
            }
        })
    }
}