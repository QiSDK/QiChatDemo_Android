package com.teneasy.qldemo

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.teneasy.qldemo.databinding.FragmentMainBinding
import com.teneasy.chatuisdk.ui.base.Constants
import com.teneasy.chatuisdk.ui.base.Utils
import com.teneasy.chatuisdk.ui.main.KeFuActivity

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    var binding: FragmentMainBinding? = null

    private var selectedThemeIndex = 0
    private val selectedTheme get() = ChatTheme.presets[selectedThemeIndex]
    private val swatches = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Utils().readConfig()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMainBinding.inflate(inflater, container, false)

        binding?.apply {
            buildThemeSwatches()

            btnSend.setOnClickListener {
                startActivity(Intent(requireContext(), KeFuActivity::class.java))
            }

            btnBackup.setOnClickListener { openBackupCustomerService() }

            ivSettings.setOnClickListener {
                findNavController().navigate(R.id.frg_settings)
            }

            tvVersionNumber.text = "Version: ${getAppVersion(requireContext()).first}"
        }

        updateThemeUI()
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 避免视图绑定在返回栈期间持有已销毁的视图层级
        swatches.clear()
        binding = null
    }

    // 构建主题色卡（每个预设一个圆形 swatch）
    private fun buildThemeSwatches() {
        val container = binding?.themeContainer ?: return
        container.removeAllViews()
        swatches.clear()

        val swatchSize = dp(60)
        val cellSize = dp(70)

        ChatTheme.presets.forEachIndexed { index, theme ->
            val cell = FrameLayout(requireContext())
            val cellLp = ViewGroup.MarginLayoutParams(cellSize, cellSize)
            cellLp.marginEnd = dp(15)
            cell.layoutParams = cellLp

            val swatch = View(requireContext())
            val swatchLp = FrameLayout.LayoutParams(swatchSize, swatchSize)
            swatchLp.gravity = Gravity.CENTER
            swatch.layoutParams = swatchLp
            swatch.background = circleDrawable(theme.gradientEnd, selected = false)
            swatch.setOnClickListener { selectTheme(index) }

            cell.addView(swatch)
            container.addView(cell)
            swatches.add(swatch)
        }
    }

    // 点击色卡：切换主题（对齐 iOS themeSelected）
    private fun selectTheme(index: Int) {
        selectedThemeIndex = index
        updateThemeUI()
        binding?.tvTitle?.setBackgroundColor(selectedTheme.leftBubbleColor)
        binding?.tvTitle?.setTextColor(selectedTheme.leftBubbleTextColor)
        binding?.ivSettings?.setColorFilter(selectedTheme.tintColor)
    }

    // 刷新主屏外观（对齐 iOS updateThemeUI）
    private fun updateThemeUI() {
        val theme = selectedTheme
        binding?.apply {
            main.background = GradientDrawable(
                orientationOf(theme.direction),
                intArrayOf(theme.gradientStart, theme.gradientEnd)
            )

            val supportBg = GradientDrawable().apply {
                cornerRadius = dp(28).toFloat()
                setColor(theme.tintColor)
            }
            btnSend.background = supportBg
            btnSend.backgroundTintList = null
            btnSend.setTextColor(Color.WHITE)

            val backupBg = GradientDrawable().apply {
                cornerRadius = dp(25).toFloat()
                setColor(Color.TRANSPARENT)
                setStroke(dp(2), theme.tintColor)
            }
            btnBackup.background = backupBg
            btnBackup.backgroundTintList = null
            btnBackup.setTextColor(theme.tintColor)

            ivSettings.setColorFilter(Color.WHITE)

            swatches.forEachIndexed { i, v ->
                v.background = circleDrawable(
                    ChatTheme.presets[i].gradientEnd,
                    selected = i == selectedThemeIndex
                )
                val scale = if (i == selectedThemeIndex) 1.15f else 1f
                v.scaleX = scale
                v.scaleY = scale
            }
        }
    }

    private fun circleDrawable(fill: Int, selected: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(fill)
            if (selected) setStroke(dp(3), Color.WHITE)
        }
    }

    private fun orientationOf(d: GradientDirection): GradientDrawable.Orientation = when (d) {
        GradientDirection.TOP_TO_BOTTOM -> GradientDrawable.Orientation.TOP_BOTTOM
        GradientDirection.BOTTOM_TO_TOP -> GradientDrawable.Orientation.BOTTOM_TOP
        GradientDirection.LEFT_TO_RIGHT -> GradientDrawable.Orientation.LEFT_RIGHT
        GradientDirection.RIGHT_TO_LEFT -> GradientDrawable.Orientation.RIGHT_LEFT
        GradientDirection.TOP_LEFT_TO_BOTTOM_RIGHT -> GradientDrawable.Orientation.TL_BR
        GradientDirection.TOP_RIGHT_TO_BOTTOM_LEFT -> GradientDrawable.Orientation.TR_BL
    }

    // 备用客服：juhekefu:// 深链，失败回退网页（对齐 iOS backupClick）
    private fun openBackupCustomerService() {
        val params = linkedMapOf(
            "cert" to Constants.cert,
            "userId" to Constants.userId.toString(),
            "merchantId" to Constants.merchantId.toString(),
            "userName" to Constants.userName,
            "userType" to Constants.userType.toString(),
            "themeIndex" to selectedThemeIndex.toString(),
        )
        if (Constants.xToken.isNotEmpty()) {
            params["xToken"] = Constants.xToken
        }

        val deepLink = Uri.Builder()
            .scheme("juhekefu")
            .authority("open")
            .apply { params.forEach { (k, v) -> appendQueryParameter(k, v) } }
            .build()

        try {
            startActivity(Intent(Intent.ACTION_VIEW, deepLink))
        } catch (e: ActivityNotFoundException) {
            openBackupWebUrl(params)
        }
    }

    private fun openBackupWebUrl(params: Map<String, String>) {
        val webUrl = Constants.backupWebUrl.trim()
        if (webUrl.isEmpty()) {
            Toast.makeText(requireContext(), "未安装客服中心 App，且未配置备用网页", Toast.LENGTH_SHORT).show()
            return
        }
        val builder = Uri.parse(webUrl).buildUpon()
        params.forEach { (k, v) -> builder.appendQueryParameter(k, v) }
        try {
            startActivity(Intent(Intent.ACTION_VIEW, builder.build()))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "打开备用网页失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    fun getAppVersion(context: Context): Pair<String, Int> {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionName = packageInfo.versionName
            val versionCode = packageInfo.versionCode
            return Pair(versionName, versionCode)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            return Pair("Unknown", -1)
        }
    }
}
