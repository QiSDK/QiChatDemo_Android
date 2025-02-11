package com.teneasy.chatuisdk.ui.http

import android.util.Log
import androidx.media3.common.MediaMetadata
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.teneasy.chatuisdk.FilePath
import com.teneasy.chatuisdk.ui.base.Constants
import com.teneasy.chatuisdk.ui.base.Utils
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink
import java.io.File
import java.io.IOException
import java.lang.reflect.Type
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit


//interface UploadListener {
//    fun uploadSuccess(path: Urls, isVideo: Boolean);
//    fun uploadProgress(progress: Int)
//    fun uploadFailed(msg: String);
//}

class UploadUtilWithProgress(lis: UploadListener) {
    private var listener: UploadListener? = null
    private val imageTypes = arrayOf("tif","tiff","bmp", "jpg", "jpeg", "png", "gif", "webp", "ico", "svg")
    private var TAG = "UploadUtil"
    private var myProgress = 0

    init {
        listener = lis
    }

    //Date().time.toString() + "." + file.extension
    //这个函数可以上传图片和视频
    fun uploadFile(file: File) {
        Constants.domain = "csapi-pc.utigio.com"
        Constants.xToken = "CAEQARjR9yMgswEoreuy4cwy.FI_bZVwCj-QNqGqOEIjLZ0D3dFRMbzhQ9aOLKJdGjEb2Pu-w6KWcCEQepje7AADJucFWW75TGbQO2HCY-rHwCg"
        val calendar = Calendar.getInstance()
        var mSec = calendar.timeInMillis.toString()
        Log.i(TAG, "开始上传。。。")
        Thread(Runnable {
            kotlin.run {
                val multipartBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("myFile",  mSec + "." + file.extension,  RequestBody.create("multipart/form-data".toMediaTypeOrNull(), file))
                    .addFormDataPart("type", "4")
                    .build()// + file.extension

                println("上传地址：" + Constants.baseUrlApi() + "/v1/assets/upload-v4" + "\n" + file.path)
                val request2 = Request.Builder().url(Constants.baseUrlApi() + "/v1/assets/upload-v4")
                    .addHeader("X-Token", Constants.xToken)
                    .post(multipartBody).build()

                val okHttpClient: OkHttpClient = OkHttpClient().newBuilder()
                    .connectTimeout(10, TimeUnit.MINUTES)
                    .writeTimeout(15, TimeUnit.MINUTES)
                    .readTimeout(15, TimeUnit.MINUTES)
                    .build()
                val call = okHttpClient.newCall(request2)
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        listener?.uploadFailed(e.message ?: "上传失败");
                        print(e.message ?: "上传失败")
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val body = response.body
                        if((response.code == 200 || response.code == 202) && body != null) {
                            val bodyStr = response.body!!.string()
                            val gson = Gson()
                            if (response.code == 200){//bodyStr.contains("code\":200")
                                val type: Type = object : TypeToken<ReturnData<FilePath>>() {}.getType()
                                val b: ReturnData<FilePath> = gson.fromJson(bodyStr, type)

                                //var b = gson.fromJson(bodyStr, ReturnData<FilePath>()::class.java)
                                if (b.data.filepath == null || (b.data?.filepath ?:"").isEmpty()){
                                    listener?.uploadFailed("上传失败，path为空");
                                    return;
                                }else {
                                    val urls = Urls()
                                    urls.uri = b.data?.filepath?: ""
                                    listener?.uploadSuccess(
                                        urls,
                                        !imageTypes.contains(file.extension)
                                    )
                                    return;
                                }
                            }else if (response.code == 202){
                                var b = gson.fromJson(bodyStr, ReturnData<String>()::class.java)
                                uploadVideoWithProgress(file,
                                    Constants.baseUrlApi() + "/v1/assets/upload-v4?uploadId=" + b.data,
                                    file.extension
                                )
                            }else{
                                print("上传失败：" + response.code + "")

                                listener?.uploadFailed("上传失败 " + bodyStr)
                            }
                        } else {
                            listener?.uploadFailed("上传失败 " + response.code)
                            print("上传失败 " + response.code)
                        }
                    }
                })

            }
        }).start()
    }

    fun uploadVideoWithProgress(file: File, url: String, ext: String) {
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)  // Set timeouts as needed
            .readTimeout(0, TimeUnit.SECONDS)      // Set readTimeout to 0 for long-lived connections
            .build()
        Log.i(TAG, (Date().toString() + "uploadVideoWithProgress..."))
        // 创建 RequestBody，用于包装文件并监听进度
        val requestBody = object : RequestBody() {
            override fun contentType(): MediaType {
                return "video/mp4".toMediaType() // 根据文件类型设置
            }

            override fun contentLength(): Long {
                return file.length()
            }

            override fun writeTo(sink: BufferedSink) {
                val buffer = sink.buffer()
                val totalBytes = file.length()
                var uploadedBytes = 0L

                file.inputStream().use { inputStream ->
                    val bufferSize = 8192 // 8KB 缓冲区
                    val bytes = ByteArray(bufferSize)
                    var read: Int
                    while (inputStream.read(bytes).also { read = it } != -1) {
                        buffer.write(bytes, 0, read)
                        uploadedBytes += read
                        // 计算并打印上传进度
                        val progress = (uploadedBytes.toDouble() / totalBytes) * 100

                        if (progress.toInt() != myProgress) {
                            println("Uploaded: $uploadedBytes / $totalBytes ($progress%)")
                            listener?.uploadProgress(progress.toInt())
                            Log.i(
                                TAG,
                                (Date().toString() + "上传进度 $uploadedBytes / $totalBytes ($progress%)")
                            )
                            myProgress = progress.toInt()
                        }
                    }
                }
            }
        }


        // .header("Accept", "text/event-stream") // 设置 header
        // 创建请求
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Accept", "text/event-stream")
            .addHeader("X-Token", Constants.xToken)
            .build()

        // 发送请求
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, e.message?:"上传出错了")
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    println("Upload successful!")
                        listener?.uploadSuccess(Urls(), false)
                } else {
                    println("Upload failed: ${response.code} - ${response.message}")
                }
            }
        })
    }
}
