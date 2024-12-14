package com.teneasy.chatuisdk.ui.http

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.teneasy.chatuisdk.FilePath
import com.teneasy.chatuisdk.ui.base.Constants
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit


interface UploadListener {
    fun uploadSuccess(path: String, isVideo: Boolean);
    fun uploadProgress(progress: Int)
    fun uploadFailed(msg: String);
}

class UploadUtil(lis: UploadListener) {
    private var listener: UploadListener? = null
    private val imageTypes = arrayOf("tif","tiff","bmp", "jpg", "jpeg", "png", "gif", "webp", "ico", "svg")
    private var TAG = "UploadUtil"

    init {
        listener = lis
    }

    /**
     * 上传图片。上传成功后，会直接调用socket进行消息发送。
     *  @param filePath
     *  // 文件类型类型 0 ～ 4
     * enum AssetKind {
     *   ASSET_KIND_NONE = 0;
     *   // 商户公共文件
     *   ASSET_KIND_PUBLIC = 1;
     *   // 商户私有文件
     *   ASSET_KIND_PRIVATE = 2;
     *   // 头像
     *   ASSET_KIND_AVATAR = 3;
     *   // 会话私有文件
     *   ASSET_KIND_SESSION = 4;
     * }
     */
    //Date().time.toString() + "." + file.extension
    //这个函数可以上传图片和视频
    fun uploadFile(file: File) {
        Thread(Runnable {
            kotlin.run {
                val multipartBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("myFile", file.path,  RequestBody.create("multipart/form-data".toMediaTypeOrNull(), file))
                    .addFormDataPart("type", "4")
                    .build()// + file.extension

                println("上传地址：" + Constants.baseUrlApi() + "/v1/assets/upload-v4" + "\n" + file.path)
                val request2 = Request.Builder().url(Constants.baseUrlApi() + "/v1/assets/upload-v4")
                    .addHeader("X-Token", Constants.xToken)
                    .post(multipartBody).build()

                val okHttpClient: OkHttpClient = OkHttpClient().newBuilder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(1, TimeUnit.MINUTES)
                    .readTimeout(1, TimeUnit.MINUTES)
                    .build()
                val call = okHttpClient.newCall(request2)
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        listener?.uploadFailed(e.message ?: "上传失败");
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val body = response.body
                        if(response.code == 200 || response.code == 202 && body != null) {
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
                                    listener?.uploadSuccess(
                                        b.data?.filepath ?:"",
                                        !imageTypes.contains(file.extension)
                                    )
                                }
                            }else if (response.code == 202){
                                var b = gson.fromJson(bodyStr, ReturnData<String>()::class.java)
                                subscribeToSSE(
                                    Constants.baseUrlApi() + "/v1/assets/upload-v4?uploadId=" + b.data,
                                    file.extension
                                )
                            }else{
                                listener?.uploadFailed("上传失败 " + bodyStr)
                            }
                        } else {
                            listener?.uploadFailed("上传失败 " + response.code)
                        }
                    }
                })

            }
        }).start()
    }

   private fun subscribeToSSE(url: String, ext: String) {
         val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)  // Set timeouts as needed
            .readTimeout(0, TimeUnit.SECONDS)      // Set readTimeout to 0 for long-lived connections
            .build()

       println("上传监听地址：" + url)
        val request = Request.Builder()
            .addHeader("X-Token", Constants.xToken)
            //.addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle failure
                listener?.uploadFailed("上传失败 Code:" + e.message)
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val body = response.body
                    if(response.code == 200 && body != null) {
                        val strData = body.string()
                        val lines = strData.split("\n");
                        var event = ""
                        var data = ""

                        print("上传监听返回 " + strData);

                        if (lines.size <= 0){
                            listener?.uploadFailed("数据为空，上传失败")
                            return
                        }

                        for (line in lines) {
                            if (line.startsWith("event:", ignoreCase = true))  {
                                event = line.replace("event:", "")
                            } else if (line.startsWith("data:", ignoreCase = true)) {
                                data = line.replace("data:", "")
                                val gson = Gson()
                                val result = gson.fromJson(data, UploadPercent::class.java)

                                if (result.percentage == 100) {
                                    listener?.uploadSuccess(
                                        result.path ?: "",
                                        !imageTypes.contains(ext)
                                    )
                                    Log.i(TAG, ("上传成功" + result.path))
                                } else {
                                    listener?.uploadProgress(result.percentage)
                                    Log.i(TAG, ("上传进度 " + result.percentage))
                                }
                            }
                        }
                    } else {
                        listener?.uploadFailed("上传失败 Code:" + response.code)
                    }
                } else {
                    listener?.uploadFailed("Failed to connect to SSE stream")
                }
            }
        })
    }
}

/*
{
"percentage": 0-100,  // 处理进度
"url": "处理完成后的HLS主文件URL"  // 仅在100%时返回
}
*/
class UploadPercent {
    var percentage: Int = 0
    var path: String? = ""
}