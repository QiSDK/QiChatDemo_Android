package com.teneasy.chatuisdk.ui.base

import com.google.gson.Gson
import com.teneasy.chatuisdk.ui.http.bean.Custom
import com.teneasy.chatuisdk.ui.http.bean.ErrorReport
import com.teneasy.sdk.ui.MessageItem
import com.teneasyChat.api.common.CMessage
import java.net.URLEncoder

// SharedPreferences 键值对常量
const val PARAM_USER_ID = "USER_ID"           // 用户ID
const val PARAM_CERT = "CERT"                 // 认证证书
const val PARAM_MERCHANT_ID = "MERCHANT_ID"   // 商户ID
const val PARAM_XTOKEN = "HTTPTOKEN"          // HTTP Token
const val PARAM_LINES = "LINES"               // 服务器线路
const val PARAM_DOMAIN = "wssBaseUrl"         // WebSocket基础URL
const val PARAM_IMAGEBASEURL = "baseUrlImage" // 图片服务器基础URL
const val PARAM_USERNAME = "USER_NAME"        // 用户名
const val PARAM_MAXSESSIONMINS = "MAXSESSIONMINS" // 最大会话时长(分钟)
const val PARAM_USER_LEVEL = "USER_LEVEL"     // 用户等级

class Constants {
    companion object {
        // 文件类型集合
        val imageTypes = arrayOf(
            "tif",
            "tiff",
            "bmp",
            "jpg",
            "jpeg",
            "png",
            "gif",
            "webp",
            "ico",
            "svg"
        )  // 支持的图片格式
        val fileTypes = arrayOf("docx", "doc", "pdf", "xls", "xlsx", "csv")  // 支持的文档格式
        val videoTypes = arrayOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm")  // 支持的视频格式

        // 基础配置属性
        var lines = "https://csapi.hfxg.xyz,https://xxxapi.qixin14.xyz"  // 服务器线路地址
        //var cert = "CMUCEAUYASCUAyjs8YWbyTI.xnc1L2bV5n21oQ2RNN8ojJD4IS-hTvXo2cgvFka4SLjsQ6rdhFFyfWKnNX3iU2VYtZtGn2-BfpI5J_xuYiy8CA"  // 认证证书
        //var cert = "COgBEAUYASDzASitlJSF9zE.5uKWeVH-7G8FIgkaLIhvzCROkWr4D3pMU0-tqk58EAQcLftyD2KBMIdYetjTYQEyQwWLy7Lfkm8cs3aogaThAw"
        var cert = "COYBEAUYASDyASiG2piD9zE.te46qua5ha2r-Caz03Vx2JXH5OLSRRV2GqdYcn9UslwibsxBSP98GhUKSGEI0Z84FRMkp16ZK8eS-y72QVE2AQ"
        var merchantId = 230  // 商户ID
        var userId = 666688   // 用户ID
        var baseUrlImage = "https://imagesacc.hfxg.xyz"  // 图片服务器地址

        // 用户设置
        var userName = "Wang Wu"  // 用户名称
        var maxSessionMins = 19999999  // 最大会话时长
        var userLevel = 88  // 用户等级

        // 运行时属性
        var xToken = ""  // HTTP请求Token
        var domain = ""  // 当前使用的域名
        var workerId = 0  // 客服工作者ID
        var CONSULT_ID: Long = 0  // 咨询会话ID
        var workerAvatar = ""  // 客服头像
        var chatId = "0"  // 聊天ID
        var withAutoReplyU: CMessage.WithAutoReply? = null  // 自动回复配置
        var errorReport = ErrorReport(arrayListOf())  // 错误报告
        var uploadProgress = 0  // 上传进度

        // 消息存储
        var unSentMessage: MutableMap<Long, ArrayList<MessageItem>> = mutableMapOf()  // 未发送消息缓存

        // 工具函数
        /**
         * 获取API基础URL
         * @return 完整的API基础URL，包含https前缀
         */
        fun baseUrlApi(): String = "https://$domain"

        /**
         * 获取自定义参数
         * 用于SDK初始化时传递额外的用户信息
         * @return URL编码后的JSON字符串
         */
        fun getCustomParam(): String {
            val custom = Custom().apply {
                username = userName
                platform = 2  // 2表示Android平台
                userlevel = userLevel
            }
            return URLEncoder.encode(Gson().toJson(custom), "utf-8")
        }
    }
}
