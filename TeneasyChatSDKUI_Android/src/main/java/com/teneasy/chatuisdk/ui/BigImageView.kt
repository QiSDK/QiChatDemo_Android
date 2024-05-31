package com.teneasy.chatuisdk.ui

import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.luck.picture.lib.utils.ToastUtils
import com.lxj.xpopup.animator.PopupAnimator
import com.lxj.xpopup.core.CenterPopupView
import com.lxj.xpopup.impl.FullScreenPopupView
import com.teneasy.chatuisdk.R
import com.teneasy.chatuisdk.ui.base.CapturePhotoUtils


class BigImageView(context: Context, url: String): FullScreenPopupView (context){

    private var url: String? = url

    override fun getImplLayoutId(): Int {
        return R.layout.fragment_image_full
    }


    override fun onCreate() {
        super.onCreate()

        val ivBig = findViewById<ImageView>(R.id.ivBig)
        Glide.with(context).load(url).dontAnimate()
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .into(ivBig)

        val tvClose = findViewById<ImageView>(R.id.tv_close)
        tvClose.setOnClickListener {
           this.dismiss()
        }

        val tvSave = findViewById<TextView>(R.id.tv_save)
        tvSave.setOnClickListener {
            //ivBig.invalidate()
            val imageBitmap = ivBig.getDrawable().toBitmap()

           var url = CapturePhotoUtils.saveImageInQ(imageBitmap)
            if (url != null){
                ToastUtils.showToast(context, "保存成功")
            }

          //  val contentResolver = context.getContentResolver()
         //  var url =  CapturePhotoUtils.insertImage(contentResolver, imageBitmap, "Image title ", null)


        //    MediaStore.Images.Media.insertImage(context.contentResolver, imageBitmap, "Image title ", null)

           /* val compressedData = Utils().compressBitmap(imageBitmap)

            val newFile = File(File(context.cacheDir, "images"), Date().time.toString())
            // Step 3: Save the compressed image to a file
            Utils().saveCompressedBitmapToFile(compressedData, newFile)
            Utils().saveImageToGallery(context, newFile)
        */
        }
    }
}