package com.teneasy.chatuisdk.ui.main;

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.fragment.NavHostFragment
import com.tbruyelle.rxpermissions3.RxPermissions
import com.teneasy.chatuisdk.R
import com.teneasy.chatuisdk.ui.base.Constants
import com.teneasy.chatuisdk.ui.base.GlobalChatManager
import com.teneasy.chatuisdk.ui.base.PARAM_DOMAIN
import com.teneasy.chatuisdk.ui.base.UserPreferences
import com.teneasy.chatuisdk.ui.base.Utils
import com.teneasy.sdk.LineDetectDelegate
import com.teneasy.sdk.LineDetectLib
import com.teneasy.sdk.Result
import com.xuexiang.xhttp2.XHttpSDK

/**
 * 客户activity。
 */
class KeFuActivity : AppCompatActivity() {

    companion object {
        /** 宿主可通过该 Intent extra 直达聊天页（跳过咨询类型入口页），值为目标 consultId。 */
        const val EXTRA_DIRECT_CONSULT_ID = "direct_consult_id"
    }

    var TAG_FRAGMENT = "KeFuFragment"
    private var rxPermissions: RxPermissions? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kefu)

        //获取相机的权限
        rxPermissions = RxPermissions(this)
        rxPermissions!!
            .request(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .subscribe { granted ->
                if (granted) { // Always true pre-M
                    // I can control the camera now
                    Log.i(TAG_FRAGMENT, "授权摄像机")
                } else {
                    // Oups permission denied
                    Log.i(TAG_FRAGMENT, "拒绝摄像机")
                }
            }

        val directConsultId = intent.getLongExtra(EXTRA_DIRECT_CONSULT_ID, 0L)
        if (directConsultId > 0) {
            prepareDirectChat(directConsultId)
        } else {
            showEntrancePage()
        }
    }

    /** 常规流程：导航图起点为咨询类型入口页。 */
    private fun showEntrancePage() {
        navController().apply {
            graph = navInflater.inflate(R.navigation.nav_graph)
        }
    }

    /**
     * 直达流程：入口页 SelectConsultTypeFragment 负责的前置初始化在这里补齐
     * （读配置、线路检测得到 domain、设置 HTTP 基址、初始化全局聊天），完成后直接进聊天页。
     */
    private fun prepareDirectChat(consultId: Long) {
        Utils().readConfig()
        // 必要参数缺失时回退入口页，那里有"请在设置页面设置好参数"的提示
        if (Constants.lines.isEmpty() || Constants.cert.isEmpty() || Constants.baseUrlImage.isEmpty() ||
            Constants.merchantId == 0 || Constants.userId == 0
        ) {
            showEntrancePage()
            return
        }
        Constants.CONSULT_ID = consultId

        // 宿主已通过 TeneasyChatUISDK.init 完成初始化时，跳过重复线路检测零等待直进
        if (GlobalChatManager.instance.initialized && Constants.domain.isNotEmpty()) {
            enterDirectChat()
            return
        }

        LineDetectLib(Constants.lines, object : LineDetectDelegate {
            override fun useTheLine(line: String) {
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    val sanitized = Constants.sanitizeDomain(line)
                    Constants.domain = sanitized
                    UserPreferences().putString(PARAM_DOMAIN, sanitized)
                    enterDirectChat()
                }
            }

            override fun lineError(error: Result) {
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    // 检测失败回退上次缓存的域名；连缓存都没有则无法聊天，关闭页面
                    if (Constants.domain.isNotEmpty()) {
                        enterDirectChat()
                    } else {
                        Toast.makeText(this@KeFuActivity, "无可用线路", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }, Constants.merchantId).getLine()
    }

    /** 前置初始化就绪后，把导航起点切到聊天页（与入口页跳转时传参保持一致）。 */
    private fun enterDirectChat() {
        XHttpSDK.setBaseUrl(Constants.baseUrlApi())
        GlobalChatManager.instance.initializeGlobalChat()
        navController().apply {
            val directGraph = navInflater.inflate(R.navigation.nav_graph)
            directGraph.setStartDestination(R.id.frg_kefu_main)
            setGraph(
                directGraph,
                bundleOf(
                    PARAM_DOMAIN to Constants.domain,
                    KeFuFragment.EXTRA_THEME_INDEX to intent.getIntExtra(KeFuFragment.EXTRA_THEME_INDEX, -1)
                )
            )
        }
    }

    private fun navController() =
        (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController

    override fun onBackPressed() {
        val fragment: KeFuFragment? =
            supportFragmentManager.findFragmentByTag(TAG_FRAGMENT) as KeFuFragment?
        fragment?.exitChat()
        super.onBackPressed()
    }
}
