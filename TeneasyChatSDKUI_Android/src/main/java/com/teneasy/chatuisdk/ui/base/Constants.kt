package com.teneasy.chatuisdk.ui.base

import android.util.Log
import com.google.gson.Gson
import com.teneasy.chatuisdk.BuildConfig
import com.teneasy.chatuisdk.ui.http.bean.Custom
import com.teneasy.chatuisdk.ui.http.bean.ErrorReport
import com.teneasy.sdk.ChatLib
import com.teneasy.sdk.UploadListener
import com.teneasy.sdk.UploadUtil
import com.teneasy.sdk.ui.MessageItem
import com.teneasyChat.api.common.CMessage
import java.net.URLEncoder
import java.util.Date

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
const val PARAM_USER_TYPE = "USER_TYPE"       // 用户类型

class Constants {
    companion object {
        val imageTypes = arrayOf(
            "tif",
            "tiff",
            "bmp",
            "jpg",
            "jpeg",
            "jfif",
            "png",
            "gif",
            "webp",
            "heic",
            "ico",
            "svg"
        )  // 支持的图片格式
        val fileTypes = arrayOf("docx", "doc", "pdf", "xls", "xlsx", "csv")  // 支持的文档格式
        val videoTypes = arrayOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm")  // 支持的视频格式

        //新环境
        //var lines = "https://csh5-3-test.qlbig05.xyz"  // 服务器线路地址
        //var cert = "CO8CEAUYASDIAyjd4eSD-jI.Uv6jzpPQvkKsXgmVYzhLp08H_NBAt4zNm7J5UxUfsFHXhresw0YJDrWvcgb00MxD59avzaS_MDC2dF-tvcimDA"
        //var baseUrlImage = "https://images-3-test.qlbig05.xyz"  // 图片服务器地址

        // 基础配置属性
        //var cert = "CMUCEAUYASCUAyjs8YWbyTI.xnc1L2bV5n21oQ2RNN8ojJD4IS-hTvXo2cgvFka4SLjsQ6rdhFFyfWKnNX3iU2VYtZtGn2-BfpI5J_xuYiy8CA"  // 认证证书
        //var cert = "COgBEAUYASDzASitlJSF9zE.5uKWeVH-7G8FIgkaLIhvzCROkWr4D3pMU0-tqk58EAQcLftyD2KBMIdYetjTYQEyQwWLy7Lfkm8cs3aogaThAw"

        private val defaultLines = BuildConfig.DEFAULT_LINES
        private val defaultCert = BuildConfig.DEFAULT_CERT
        private val defaultBaseUrlImage = BuildConfig.DEFAULT_BASE_URL_IMAGE
        private val defaultMerchantId = BuildConfig.DEFAULT_MERCHANT_ID
        private val defaultUserId = BuildConfig.DEFAULT_USER_ID
        private val defaultUserName = BuildConfig.DEFAULT_USER_NAME
        private val defaultMaxSessionMins = BuildConfig.DEFAULT_MAX_SESSION_MINS
        private val defaultUserLevel = BuildConfig.DEFAULT_USER_LEVEL
        private val defaultUserType = BuildConfig.DEFAULT_USER_TYPE

        var lines = defaultLines  // 服务器线路地址
        var cert = defaultCert
        var baseUrlImage = defaultBaseUrlImage  // 图片服务器地址

        var merchantId = defaultMerchantId  // 商户ID
        var userId = defaultUserId   // 用户ID


        //九月
//        var cert = "CAEQBRgBIIcCKPHr3dPoMg.ed_euM3a4Ew7QTiJKg4XQskD5KTzvqXdFKRPnVyNmyZNF-Cyq7g9XMr3a41OvVtoovp15IBrfYveDZTJPEldBA"
//        var lines = "https://d2jt4g8mgfvbcl.cloudfront.net"
//        var baseUrlImage = "https://d2uzsk40324g7l.cloudfront.net"


        // 用户设置
        var userName = defaultUserName  // 用户名称
        var maxSessionMins = defaultMaxSessionMins  // 最大会话时长
        var userLevel = defaultUserLevel  // 用户等级
        //用户类型 1-官方会员 2-邀请好友 3-合营会员
        var userType = defaultUserType

        // 运行时属性
        var xToken = ""  // HTTP请求Token
        var domain = ""  // 当前使用的域名
        var workerId = 0  // 客服工作者ID
        var CONSULT_ID: Long = 0  // 咨询会话ID
        var workerAvatar = ""  // 客服头像
        var chatId = "0"  // 聊天ID
        var withAutoReplyU: CMessage.WithAutoReply? = null  // 自动回复配置
        var errorReport = ErrorReport(arrayListOf())  // 错误报告
        //var uploadProgress = UploadUtil.uploadProgress  // 上传进度

        // 消息存储
        var unSentMessage: MutableMap<Long, ArrayList<MessageItem>> = mutableMapOf()  // 未发送消息缓存

        // 全局消息监听相关
        var unReadList: MutableList<UnReadItem> = mutableListOf()  // 未读消息列表
        var globalMessageDelegate: GlobalMessageDelegate? = null  // 全局消息委托
        var currentChatConsultId: Long = 0  // 当前正在聊天的consultId
        var chatLib: ChatLib? = null  // 全局ChatLib实例

        fun sanitizeDomain(raw: String): String {
            var result = raw.trim()
            when {
                result.startsWith("http://", ignoreCase = true) -> result = result.substring(7)
                result.startsWith("https://", ignoreCase = true) -> result = result.substring(8)
                result.startsWith("wss://", ignoreCase = true) -> result = result.substring(6)
                result.startsWith("ws://", ignoreCase = true) -> result = result.substring(5)
            }
            return result.trimEnd('/')
        }

        fun resetToDefaults() {
            lines = defaultLines
            cert = defaultCert
            baseUrlImage = defaultBaseUrlImage
            merchantId = defaultMerchantId
            userId = defaultUserId
            userName = defaultUserName
            maxSessionMins = defaultMaxSessionMins
            userLevel = defaultUserLevel
            userType = defaultUserType
            xToken = ""
            domain = sanitizeDomain(defaultLines.split(",").firstOrNull()?.trim().orEmpty())
            workerId = 0
            CONSULT_ID = 0
            workerAvatar = ""
            chatId = "0"
            withAutoReplyU = null
        }

        /**
         * 同步接口返回的未读数到全局未读列表
         * @param consultId 咨询会话ID
         * @param count 接口返回的未读数
         */
        fun syncUnreadCount(consultId: Long, count: Int) {
            GlobalMessageManager.instance.syncUnreadCount(consultId, count)
        }

        /**
         * 增加未读消息数
         * @param consultId 咨询会话ID
         */
        fun incrementUnreadCount(consultId: Long) {
            GlobalMessageManager.instance.addUnReadMessage(consultId)
        }

        /**
         * 清零某个consultId的未读数
         * @param consultId 咨询会话ID
         */
        fun clearUnreadCount(consultId: Long) {
            GlobalMessageManager.instance.clearUnReadCount(consultId)
        }

        /**
         * 获取某个consultId的未读数
         * @param consultId 咨询会话ID
         * @return 未读消息数量
         */
        fun getUnreadCount(consultId: Long): Int {
            return GlobalMessageManager.instance.getUnReadCount(consultId)
        }

        /**
         * 获取总未读数
         * @return 所有会话的未读消息总数
         */
        fun getTotalUnreadCount(): Int {
            return GlobalMessageManager.instance.getTotalUnReadCount()
        }

        // 工具函数
        /**
         * 获取API基础URL
         * @return 完整的API基础URL，包含https前缀
         */
        fun baseUrlApi(): String {
            val targetDomain = if (domain.isNotEmpty()) {
                domain
            } else {
                defaultLines.split(",").firstOrNull()?.trim().orEmpty()
            }
            if (targetDomain.isEmpty()) {
                return ""
            }
            return if (targetDomain.startsWith("http")) {
                targetDomain
            } else {
                "https://${targetDomain}"
            }
        }

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
                usertype = userType //usertype: 用户类型 1-官方会员 2-邀请好友 3-合营会员
            }
            return Gson().toJson(custom)
            //return URLEncoder.encode(Gson().toJson(custom), "utf-8")
        }
    }
}
