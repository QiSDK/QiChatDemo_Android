package com.teneasy.chatuisdk.ui.base

import com.google.gson.Gson
import com.teneasy.chatuisdk.ui.http.bean.Custom
import com.teneasy.chatuisdk.ui.http.bean.ErrorReport
import com.teneasy.sdk.ui.MessageItem
import com.teneasyChat.api.common.CMessage
import java.net.URLEncoder


const val PARAM_USER_ID = "USER_ID"
const val PARAM_CERT = "CERT"
const val PARAM_MERCHANT_ID = "MERCHANT_ID"
const val PARAM_XTOKEN = "HTTPTOKEN"
const val PARAM_LINES = "LINES"
const val PARAM_DOMAIN = "wssBaseUrl"
const val PARAM_IMAGEBASEURL = "baseUrlImage"
const val PARAM_USERNAME = "USER_NAME"
const val PARAM_MAXSESSIONMINS = "MAXSESSIONMINS"
const val PARAM_USER_LEVEL = "USER_LEVEL"

////这几个是需要在设置里面配置
//var lines = ""
//var cert = ""
//var merchantId: Int = 0
//var userId: Int32 = 0//1125324
//var baseUrlImage = "" //用于拼接图片地址

class Constants {

    companion object {

        var unSentMessage: MutableMap<Long, ArrayList<MessageItem>> = mutableMapOf()

        //这部分是在设置里面获取的
//        var lines = ""
//        var cert = ""
//        var merchantId = 0
//        var userId = 0//1125324
//        var baseUrlImage = "" //用于拼接图片地址


     //这部分是在设置里面获取的
        var lines = "https://csapi.hfxg.xyz,https://xxxapi.qixin14.xyz"
        //var cert = "COYBEAUYASDyASiG2piD9zE.te46qua5ha2r-Caz03Vx2JXH5OLSRRV2GqdYcn9UslwibsxBSP98GhUKSGEI0Z84FRMkp16ZK8eS-y72QVE2AQ"
        var merchantId = 230
        var userId = 666688//1125324
        var baseUrlImage = "https://imagesacc.hfxg.xyz" //用于拼接图片地址

        //lucky test, 对应：2025_1	test000000
        var cert = "CMUCEAUYASCUAyjs8YWbyTI.xnc1L2bV5n21oQ2RNN8ojJD4IS-hTvXo2cgvFka4SLjsQ6rdhFFyfWKnNX3iU2VYtZtGn2-BfpI5J_xuYiy8CA"
//        var merchantId = 225
//        var userId = 777772
//        var baseUrlImage = "https://sssacc.wwc09.com" //用于拼接图片地址


        /*雷哥的测试环境*/
//var lines = "https://csapi.hfxg.xyz,https://xxx.qixin14.xyz"
//var cert = "COgBEAUYASDzASitlJSF9zE.5uKWeVH-7G8FIgkaLIhvzCROkWr4D3pMU0-tqk58EAQcLftyD2KBMIdYetjTYQEyQwWLy7Lfkm8cs3aogaThAw"
//var merchantId = 232
//var userId = 364310
//                var baseUrlImage = "https://sssacc.wwc09.com" //用于拼接图片地址



        /*lucky 的环境 */
//        var lines = "https://csapi.hfxg.xyz,https://xxx.qixin14.xyz"
//var cert = "COgBEAUYASDzASitlJSF9zE.5uKWeVH-7G8FIgkaLIhvzCROkWr4D3pMU0-tqk58EAQcLftyD2KBMIdYetjTYQEyQwWLy7Lfkm8cs3aogaThAw"
//var merchantId = 232
//var userId = 6666668// 364310
//                var baseUrlImage = "https://sssacc.wwc09.com" //用于拼接图片地址


        var userName = "Wang Wu"
        var maxSessionMins = 19999999
        var userLevel = 88

     //运行时候生成的
         var xToken = ""
        //聊天SDK所需要的域名，例如www.abc.com，没有https前缀
        var domain = ""  //domain
        var workerId = 0
        var CONSULT_ID: Long = 0
        //val wss_token = "wss_token"
        var workerAvatar = ""
        var chatId = "0";

        //var autoPlay = CMessage.MessageAutoReply.newBuilder()

        //var withAutoReplyU = CMessage.WithAutoReply.newBuilder()
        var withAutoReplyU: CMessage.WithAutoReply? = null
        var errorReport = ErrorReport(arrayListOf())

        var uploadProgress = 0;
        //var reportTimes = 0

        //self.autoReply.qa.removeAll()

        fun baseUrlApi() : String {
            val baseUrlApi = "https://" + domain
            return baseUrlApi
        }

        fun getCustomParam() : String {
            // 初始化sdk的时候，如果需要传更多参数，在参数的最后一个，可以使用自定义参数
            var custom = Custom()
            custom.username = userName;
            custom.platform = 2;
            custom.userlevel = userLevel
            val cust = Gson().toJson(custom)
            val c = URLEncoder.encode(cust, "utf-8")
            return c
        }

        val imageTypes = arrayOf("tif","tiff","bmp", "jpg", "jpeg", "png", "gif", "webp", "ico", "svg")
        val fileTypes = arrayOf("docx","doc","pdf", "xls", "xlsx", "csv")
        val videoTypes = arrayOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm")
    }
}