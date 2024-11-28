package com.teneasy.chatuisdk.ui.http

import android.util.Log
import com.google.gson.Gson
import com.teneasy.chatuisdk.UploadResult
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
import java.util.Date
import java.util.concurrent.TimeUnit

class UploadUtil {
    private val imageTypes = arrayOf("tif","tiff","bmp", "jpg", "jpeg", "png", "gif", "webp", "ico", "svg")
    private var TAG = "UploadUtil"
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
    //这个函数可以上传图片和视频
    fun uploadFile(file: File) {
        Thread(Runnable {
            kotlin.run {
                val multipartBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("myFile", Date().time.toString() + "." + file.extension,  RequestBody.create("multipart/form-data".toMediaTypeOrNull(), file))
                    .addFormDataPart("type", "4")
                    .build()// + file.extension

                val request2 = Request.Builder().url(Constants.baseUrlApi() + "/v1/assets/upload-v3")
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

                    }

                    override fun onResponse(call: Call, response: Response) {
                        val body = response.body
                        if(response.code == 200 && body != null) {
                            val path = response.body!!.string()
                            val gson = Gson()
                            val result = gson.fromJson(path, ReturnData<String>()::class.java)

                            if (result.code == 0 || result.code == 200) {
                               //uploadFileStep2(result.data, file.extension)
                                subscribeToSSE(Constants.baseUrlApi() + "/v1/assets/upload-v4?uploadId=" + result.data, file.extension)
                            } else {
                                //toast(result.message?: "上传失败");
                            }
                        } else {
                            //toast("上传失败 Code:" + response.code)
                        }
                        //Utils().closeSoftKeyboard(view)
                    }
                })

            }
        }).start()
    }

    fun uploadFileStep2(uploadId: String, ext: String) {
        Thread(Runnable {
            kotlin.run {
                val request = Request.Builder().url(Constants.baseUrlApi() + "/v1/assets/upload-v4?uploadId=" + uploadId)
                    .addHeader("X-Token", Constants.xToken)
                    //.addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "text/event-stream")
                    .build()

                val okHttpClient: OkHttpClient = OkHttpClient().newBuilder()
                    .connectTimeout(50, TimeUnit.SECONDS)
                    .writeTimeout(50, TimeUnit.MINUTES)
                    .readTimeout(50, TimeUnit.MINUTES)
                    .build()
                val call = okHttpClient.newCall(request)
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {

                    }

                    override fun onResponse(call: Call, response: Response) {
                        val body = response.body
                        if(response.code == 200 && body != null) {
                            val path = response.body!!.string()
                            val gson = Gson()
                            val result = gson.fromJson(path, UploadPercent::class.java)

                            if (result.percentage == 100) {
                                if (imageTypes.contains(ext)) {
                                    // 发送图片
                                    //sendImgMsg(result.data?.filepath?: "")//Constants.baseUrlImage +
                                } else {
                                    //sendVideoMsg(result.data?.filepath?: "")//Constants.baseUrlImage +
                                }
                                Log.i(TAG, ("上传成功" + result.path))
                            }
                        } else {
                            // toast("上传失败 Code:" + response.code)
                        }
                        //Utils().closeSoftKeyboard(view)
                    }
                })

            }
        }).start()

    }

    fun subscribeToSSE(url: String, ext: String) {
         val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)  // Set timeouts as needed
            .readTimeout(0, TimeUnit.SECONDS)      // Set readTimeout to 0 for long-lived connections
            .build()

        val request = Request.Builder()
            .addHeader("X-Token", Constants.xToken)
            //.addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle failure
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                // Handle server-sent events
                if (response.isSuccessful) {
                    val body = response.body
                    if(response.code == 200 && body != null) {
                        val path = response.body!!.string()
                        val gson = Gson()
                        val result = gson.fromJson(path, UploadPercent::class.java)

                        if (result.percentage == 100) {
                            if (imageTypes.contains(ext)) {
                                // 发送图片
                                //sendImgMsg(result.data?.filepath?: "")//Constants.baseUrlImage +
                            } else {
                                //sendVideoMsg(result.data?.filepath?: "")//Constants.baseUrlImage +
                            }
                            Log.i(TAG, ("上传成功" + result.path))
                        }
                    } else {
                        // toast("上传失败 Code:" + response.code)
                    }
                } else {
                    println("Failed to connect to SSE stream")
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