package com.teneasy.chatuisdk.ui.main

import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleCoroutineScope
import com.luck.picture.lib.utils.ToastUtils
import com.teneasy.chatuisdk.ui.base.Constants
import com.teneasy.chatuisdk.ui.base.Utils
import com.teneasy.sdk.Urls
import com.teneasy.sdk.UploadListener
import com.xuexiang.xhttp2.subsciber.impl.IProgressLoader
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 负责处理聊天中的文件、图片、视频上传流程。
 */
class ChatUploadHandler(
    private val fragment: Fragment,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val progressLoaderProvider: () -> IProgressLoader?,
    private val callbacks: Callbacks
) : UploadListener {

    interface Callbacks {
        fun onCallBackImageUploaded(urls: Urls)
        fun onCallBackVideoUploaded(urls: Urls)
        fun onCallBackUploadFailed(message: String)
        fun onCallBackUploadProgress(progress: Int)
    }

    fun beforeUpload(filePath: String) {
        val loader = progressLoaderProvider.invoke()
        loader?.updateMessage("正在上传...")
        loader?.showLoading()
        com.teneasy.sdk.UploadUtil.uploadProgress = 0

        val file = File(filePath)
        if (!file.exists()) {
            ToastUtils.showToast(fragment.requireContext(), "文件不存在")
            loader?.dismissLoading()
            callbacks.onCallBackUploadFailed("文件不存在")
            return
        }

        when (file.extension.lowercase()) {
            in Constants.imageTypes -> handleImageUpload(file)
            else -> handleVideoOrFileUpload(file)
        }
    }

    private fun handleImageUpload(file: File) {
        val loader = progressLoaderProvider.invoke()
        val maxImageSize = 20 * 1024 * 1024 // 20MB
        if (file.length() >= maxImageSize) {
            ToastUtils.showToast(fragment.requireContext(), "图片限制20M")
            loader?.dismissLoading()
             callbacks.onCallBackUploadFailed("图片限制20M")
            return
        }

        if (file.extension.equals("tiff", ignoreCase = true)) {
            val cacheDir = fragment.requireContext().cacheDir
            val pngCacheFile = File(cacheDir, "${file.nameWithoutExtension}.png")
            val converted = Utils().convertTiffToPng(file, pngCacheFile.absolutePath)
            if (!converted) {
                ToastUtils.showToast(fragment.requireContext(), "处理文件失败")
                loader?.dismissLoading()
                 callbacks.onCallBackUploadFailed("处理文件失败")
                return
            }
            com.teneasy.sdk.UploadUtil(this, Constants.baseUrlApi(), Constants.xToken).uploadFile(pngCacheFile)
        } else {
            com.teneasy.sdk.UploadUtil(this, Constants.baseUrlApi(), Constants.xToken).uploadFile(file)
        }
    }

    private fun handleVideoOrFileUpload(file: File) {
        val loader = progressLoaderProvider.invoke()
        val maxFileSize = 300 * 1024 * 1024 // 300MB
        val compressionThreshold = 900 * 1024 * 1024 // 900MB

        if (file.length() <= compressionThreshold) {
            com.teneasy.sdk.UploadUtil(this, Constants.baseUrlApi(), Constants.xToken).uploadFile(file)
            return
        }

        val newFilePath = "${file.absolutePath.replace("." + file.extension, "").replace(".", "")}${System.currentTimeMillis()}.${file.extension}"
        val newFile = File(newFilePath)
        progressLoaderProvider.invoke()?.updateMessage("正在压缩视频...")

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                Utils().compressVideo(file.absolutePath, newFilePath)
            }
            when {
                result == 0 && newFile.length() < maxFileSize -> {
                    progressLoaderProvider.invoke()?.updateMessage("开始上传...")
                    com.teneasy.sdk.UploadUtil.uploadProgress = 65
                    callbacks.onCallBackUploadProgress(com.teneasy.sdk.UploadUtil.uploadProgress)
                    com.teneasy.sdk.UploadUtil(this@ChatUploadHandler, Constants.baseUrlApi(), Constants.xToken).uploadFile(newFile)
                }
                file.length() < maxFileSize -> {
                    com.teneasy.sdk.UploadUtil(this@ChatUploadHandler, Constants.baseUrlApi(), Constants.xToken).uploadFile(file)
                }
                else -> {
                    ToastUtils.showToast(fragment.requireContext(), "视频/文件限制300M")
                    loader?.dismissLoading()
                     callbacks.onCallBackUploadFailed("视频/文件限制300M")
                }
            }
        }
    }

    override fun uploadSuccess(urls: Urls, isVideo: Boolean) {
        progressLoaderProvider.invoke()?.dismissLoading()
        if (isVideo) {
            callbacks.onCallBackVideoUploaded(urls)
        } else {
            callbacks.onCallBackImageUploaded(urls)
        }
    }

    override fun uploadProgress(progress: Int) {
        callbacks.onCallBackUploadProgress(progress)
    }

    override fun uploadFailed(msg: String) {
        progressLoaderProvider.invoke()?.dismissLoading()
         callbacks.onCallBackUploadFailed(msg)
    }
}
