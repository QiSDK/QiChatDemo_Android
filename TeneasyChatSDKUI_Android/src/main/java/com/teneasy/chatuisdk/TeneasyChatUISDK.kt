package com.teneasy.chatuisdk

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.teneasy.chatuisdk.ui.base.ApplicationExt
import com.teneasy.chatuisdk.ui.base.Constants
import com.teneasy.chatuisdk.ui.base.GlobalChatManager
import com.teneasy.chatuisdk.ui.base.PARAM_BACKUP_WEB_URL
import com.teneasy.chatuisdk.ui.base.PARAM_CERT
import com.teneasy.chatuisdk.ui.base.PARAM_DOMAIN
import com.teneasy.chatuisdk.ui.base.PARAM_IMAGEBASEURL
import com.teneasy.chatuisdk.ui.base.PARAM_LINES
import com.teneasy.chatuisdk.ui.base.PARAM_MAXSESSIONMINS
import com.teneasy.chatuisdk.ui.base.PARAM_MERCHANT_ID
import com.teneasy.chatuisdk.ui.base.PARAM_PLATFORM_NAME
import com.teneasy.chatuisdk.ui.base.PARAM_USERNAME
import com.teneasy.chatuisdk.ui.base.PARAM_USER_ID
import com.teneasy.chatuisdk.ui.base.PARAM_USER_LEVEL
import com.teneasy.chatuisdk.ui.base.PARAM_USER_TYPE
import com.teneasy.chatuisdk.ui.base.ServiceKeyword
import com.teneasy.chatuisdk.ui.base.UserPreferences
import com.teneasy.sdk.LineDetectDelegate
import com.teneasy.sdk.LineDetectLib
import com.teneasy.sdk.Result
import com.xuexiang.xhttp2.XHttpSDK
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 宿主接入参数。必填四项缺一不可；可选项缺省沿用 SDK 当前值（BuildConfig 默认或上次设置）。
 */
data class ChatSDKConfig(
    val cert: String,
    val userId: Int,
    val merchantId: Int,
    /** 线路检测地址，多个以逗号分隔 */
    val lines: String,
    val baseUrlImage: String,
    val userName: String = Constants.userName,
    val userLevel: Int = Constants.userLevel,
    val userType: Int = Constants.userType,
    val maxSessionMins: Int = Constants.maxSessionMins,
    val platformName: String = Constants.platformName,
    val backupWebUrl: String = Constants.backupWebUrl,
)

/**
 * UISDK 对宿主的统一入口。
 *
 * 宿主 App 启动（或登录拿到 userId 后）调用 [init]：
 * 准备环境 → 写入参数 → 线路检测 → 初始化全局聊天并建立 WebSocket。
 * 此后宿主任意页面都能通过 ChatEventBus / GlobalMessageManager 实时收到消息与未读数，
 * 进入聊天页（含 KeFuActivity 直达模式）无需再等待初始化。
 *
 * 幂等，可重复调用；参数变化时以最新一次为准。
 */
object TeneasyChatUISDK {
    private const val TAG = "TeneasyChatUISDK"

    /**
     * 设置「关键词自动卡片」配置。宿主调用自己的接口拿到 service_keyword 数组后原样传入。
     * 可在任意时机重复调用覆盖。
     *
     * 之后当用户在聊天页输入的文本包含某条的任一 keyword 时，UISDK 会自动以
     * msgSourceType = MST_AUTO_CARD 追加发送一条卡片消息。
     */
    fun setAutoCardKeywords(serviceKeywords: List<Map<String, Any?>>) {
        Constants.serviceKeywords = serviceKeywords.map { ServiceKeyword(it) }
    }

    fun init(context: Context, config: ChatSDKConfig) {
        // 宿主有自己的 Application 类时 ApplicationExt 不会运行，这里兜底环境准备
        ApplicationExt.prepareEnvironment(context)

        if (config.cert.isEmpty() || config.userId == 0 || config.merchantId == 0 || config.lines.isEmpty()) {
            Log.w(TAG, "参数不完整（cert/userId/merchantId/lines 必填），跳过初始化")
            return
        }

        applyConfig(config)

        // LineDetectLib 每次内部重试失败都会回调 lineError，一次检测会话只允许启动一次
        val started = AtomicBoolean(false)
        LineDetectLib(Constants.lines, object : LineDetectDelegate {
            override fun useTheLine(line: String) {
                val sanitized = Constants.sanitizeDomain(line)
                Constants.domain = sanitized
                UserPreferences().putString(PARAM_DOMAIN, sanitized)
                Log.i(TAG, "线路检测成功: $sanitized")
                if (started.compareAndSet(false, true)) {
                    startGlobalChat()
                }
            }

            override fun lineError(error: Result) {
                // 检测失败回退上次缓存的域名；连缓存都没有时等下次 init 或进入 SDK 页面再试
                if (Constants.domain.isEmpty()) {
                    Constants.domain = UserPreferences().getString(PARAM_DOMAIN, "")
                }
                if (Constants.domain.isNotEmpty()) {
                    if (started.compareAndSet(false, true)) {
                        Log.w(TAG, "线路检测失败(${error.msg})，使用缓存域名: ${Constants.domain}")
                        startGlobalChat()
                    }
                } else {
                    Log.w(TAG, "线路检测失败(${error.msg})且无缓存域名，初始化中止")
                }
            }
        }, Constants.merchantId).getLine()
    }

    /** 写入 Constants 并持久化，与设置页 / Utils.readConfig 使用同一套存储 */
    private fun applyConfig(config: ChatSDKConfig) {
        Constants.cert = config.cert
        Constants.userId = config.userId
        Constants.merchantId = config.merchantId
        Constants.lines = config.lines
        Constants.baseUrlImage = config.baseUrlImage
        Constants.userName = config.userName
        Constants.userLevel = config.userLevel
        Constants.userType = config.userType
        Constants.maxSessionMins = config.maxSessionMins
        Constants.platformName = config.platformName
        Constants.backupWebUrl = config.backupWebUrl

        UserPreferences().apply {
            putString(PARAM_CERT, config.cert)
            putInt(PARAM_USER_ID, config.userId)
            putInt(PARAM_MERCHANT_ID, config.merchantId)
            putString(PARAM_LINES, config.lines)
            putString(PARAM_IMAGEBASEURL, config.baseUrlImage)
            putString(PARAM_USERNAME, config.userName)
            putInt(PARAM_USER_LEVEL, config.userLevel)
            putInt(PARAM_USER_TYPE, config.userType)
            putInt(PARAM_MAXSESSIONMINS, config.maxSessionMins)
            putString(PARAM_PLATFORM_NAME, config.platformName)
            putString(PARAM_BACKUP_WEB_URL, config.backupWebUrl)
        }

        // session token 按 (merchantId, userId) 隔离，参数就绪后读取上次会话
        Constants.xToken = UserPreferences().getString(Constants.tokenStorageKey(), "")
    }

    /** 线路就绪后启动全局聊天；LineDetect 回调可能在子线程，统一切回主线程 */
    private fun startGlobalChat() {
        Handler(Looper.getMainLooper()).post {
            XHttpSDK.setBaseUrl(Constants.baseUrlApi())
            GlobalChatManager.instance.initializeGlobalChat()
            GlobalChatManager.instance.connectIfNeeded()
        }
    }
}
