# MainFragment iOS 对齐 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 Android `MainFragment` 主屏幕对齐 iOS `ViewController`：新增 11 套主题色卡 + 渐变背景（仅主屏，不进聊天）、备用客服深链按钮 + 网页兜底，并在设置页加备用网页 URL 配置；移除隐藏的 PDF 按钮。

**Architecture:** 纯客户端 UI 改动，集中在 `app` 模块；主题数据用 demo 专用 `ChatTheme.kt`（仅含主屏用到的字段，颜色照搬 iOS 预设），主屏渐变/按钮/色卡全部代码绘制，无新增图片资源。备用客服走 `juhekefu://open` Intent，失败回退到 `Constants.backupWebUrl`。设置项复用现有 `Constants`/`Utils.readConfig`/`SettingsFragment` 模式。

**Tech Stack:** Kotlin, Android View Binding, ConstraintLayout, GradientDrawable, androidx Navigation。

> **测试说明：** 本项目对该 Fragment 没有任何自动化 UI 测试基础设施，且新增的是纯视图代码。本计划采用**编译通过 + 手动验证**作为验证手段（每个任务以 `./gradlew :app:assembleDebug` 成功为门槛），而非 TDD 自动化测试。最后一个任务集中做手动验证。

---

## File Structure

- **Create** `app/src/main/java/com/teneasy/qldemo/ChatTheme.kt` — demo 专用主题模型 + 11 套预设（主屏字段）
- **Modify** `app/src/main/res/layout/fragment_main.xml` — 重写主屏布局（标题/色卡/两个按钮/设置图标/版本号），移除 PDF 按钮
- **Modify** `app/src/main/java/com/teneasy/qldemo/MainFragment.kt` — 主题选择 + 渐变 + 备用客服深链逻辑
- **Modify** `TeneasyChatSDKUI_Android/.../ui/base/Constants.kt` — 新增 `PARAM_BACKUP_WEB_URL` + `backupWebUrl`
- **Modify** `TeneasyChatSDKUI_Android/.../ui/base/Utils.kt` — `readConfig()` 读取备用网页 URL
- **Modify** `TeneasyChatSDKUI_Android/src/main/res/layout/fragment_settings.xml` — 新增备用网页 URL 输入框
- **Modify** `TeneasyChatSDKUI_Android/.../SettingsFragment.kt` — 填充 + 保存备用网页 URL

---

## Task 1: 备用网页 URL 配置（Constants + Utils）

**Files:**
- Modify: `TeneasyChatSDKUI_Android/src/main/java/com/teneasy/chatuisdk/ui/base/Constants.kt`
- Modify: `TeneasyChatSDKUI_Android/src/main/java/com/teneasy/chatuisdk/ui/base/Utils.kt`

- [ ] **Step 1: 新增常量 key**

在 `Constants.kt` 顶部常量区（`PARAM_USER_TYPE` 之后，第 27 行后）追加：

```kotlin
const val PARAM_BACKUP_WEB_URL = "PARAM_BACKUP_WEB_URL"  // 备用客服网页URL
```

- [ ] **Step 2: 新增运行时属性**

在 `Constants.kt` 的 `companion object` 内，`var userType = defaultUserType` 之后（约第 86 行后）追加：

```kotlin
var backupWebUrl = ""  // 备用客服网页URL（深链失败时回退）
```

- [ ] **Step 3: resetToDefaults 重置该字段**

在 `Constants.kt` 的 `resetToDefaults()` 内，`xToken = ""` 那一行附近追加：

```kotlin
backupWebUrl = ""
```

- [ ] **Step 4: readConfig 读取该字段**

在 `Utils.kt` 的 `readConfig()` 内（`Constants.userType = ...` 那一行之后，约第 90 行后）追加：

```kotlin
Constants.backupWebUrl = UserPreferences().getString(PARAM_BACKUP_WEB_URL, Constants.backupWebUrl)
```

确保 `Utils.kt` 顶部已 `import com.teneasy.chatuisdk.ui.base.PARAM_BACKUP_WEB_URL`（若 `Utils.kt` 与 `Constants` 同包 `com.teneasy.chatuisdk.ui.base`，常量为同包顶层声明，无需 import；先确认包名，缺则加）。

- [ ] **Step 5: 编译**

Run: `./gradlew :TeneasyChatSDKUI_Android:compileReleaseKotlin`
Expected: BUILD SUCCESSFUL（无未解析引用）

- [ ] **Step 6: 提交**

```bash
git add TeneasyChatSDKUI_Android/src/main/java/com/teneasy/chatuisdk/ui/base/Constants.kt \
        TeneasyChatSDKUI_Android/src/main/java/com/teneasy/chatuisdk/ui/base/Utils.kt
git commit -m "feat: 新增备用客服网页URL配置项 (Constants + readConfig)"
```

---

## Task 2: 设置页备用网页 URL 输入框

**Files:**
- Modify: `TeneasyChatSDKUI_Android/src/main/res/layout/fragment_settings.xml`
- Modify: `TeneasyChatSDKUI_Android/src/main/java/com/teneasy/chatuisdk/SettingsFragment.kt`

- [ ] **Step 1: 布局新增输入框**

在 `fragment_settings.xml` 中，`Spinner`（id `spinner_UserType`，约第 114 行）**之前**插入：

```xml
    <com.google.android.material.textfield.TextInputLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="8dp">

        <EditText android:id="@+id/et_BackupWebUrl"
            android:hint="备用客服网页 URL（可选）"
            android:inputType="textUri"
            android:layout_width="match_parent"
            android:layout_height="wrap_content" />
    </com.google.android.material.textfield.TextInputLayout>
```

- [ ] **Step 2: SettingsFragment 填充已保存值**

在 `SettingsFragment.kt` 的 `binding?.apply { ... }` 内，`this.etUserLevel?.setText(...)`（约第 74 行）之后追加：

```kotlin
            this.etBackupWebUrl?.setText(Constants.backupWebUrl)  // 备用客服网页URL
```

- [ ] **Step 3: SettingsFragment 保存该值**

在 `SettingsFragment.kt` 的 `btnSave` 点击回调内，`Constants.userLevel = ...`（约第 102 行）之后追加：

```kotlin
                Constants.backupWebUrl = this.etBackupWebUrl.text.toString().trim()
```

并在写入 `UserPreferences` 的那一组（`UserPreferences().putInt(PARAM_USER_TYPE, ...)` 之后，约第 123 行）追加：

```kotlin
                UserPreferences().putString(PARAM_BACKUP_WEB_URL, Constants.backupWebUrl)
```

- [ ] **Step 4: 补充 import**

在 `SettingsFragment.kt` 顶部 import 区追加：

```kotlin
import com.teneasy.chatuisdk.ui.base.PARAM_BACKUP_WEB_URL
```

- [ ] **Step 5: 编译**

Run: `./gradlew :TeneasyChatSDKUI_Android:assembleDebug`
Expected: BUILD SUCCESSFUL（`etBackupWebUrl` 由 ViewBinding 生成，能解析）

- [ ] **Step 6: 提交**

```bash
git add TeneasyChatSDKUI_Android/src/main/res/layout/fragment_settings.xml \
        TeneasyChatSDKUI_Android/src/main/java/com/teneasy/chatuisdk/SettingsFragment.kt
git commit -m "feat: 设置页新增备用客服网页URL输入框"
```

---

## Task 3: ChatTheme 模型 + 11 套预设

**Files:**
- Create: `app/src/main/java/com/teneasy/qldemo/ChatTheme.kt`

颜色按 iOS `ChatTheme.swift` 的 `presets` 忠实换算（0~1 → 0~255，四舍五入；alpha<1 用 `Color.argb`）。

- [ ] **Step 1: 创建文件**

写入 `app/src/main/java/com/teneasy/qldemo/ChatTheme.kt`：

```kotlin
package com.teneasy.qldemo

import android.graphics.Color

/** 渐变方向（对齐 iOS ChatTheme.GradientDirection） */
enum class GradientDirection {
    TOP_TO_BOTTOM, BOTTOM_TO_TOP, LEFT_TO_RIGHT,
    RIGHT_TO_LEFT, TOP_LEFT_TO_BOTTOM_RIGHT, TOP_RIGHT_TO_BOTTOM_LEFT
}

/**
 * Demo 专用主题模型。
 * 只包含主屏幕用到的字段（渐变 + tintColor + 标题着色用的 leftBubble*），
 * 不传入聊天 SDK。颜色照搬 iOS ChatTheme.swift presets。
 */
data class ChatTheme(
    val gradientStart: Int,
    val gradientEnd: Int,
    val direction: GradientDirection,
    val tintColor: Int,
    val leftBubbleColor: Int,
    val leftBubbleTextColor: Int,
) {
    companion object {
        val presets: List<ChatTheme> = listOf(
            // 1. 晨雾白
            ChatTheme(
                gradientStart = Color.rgb(252, 250, 252),
                gradientEnd = Color.rgb(220, 232, 244),
                direction = GradientDirection.TOP_LEFT_TO_BOTTOM_RIGHT,
                tintColor = Color.rgb(120, 140, 210),
                leftBubbleColor = Color.argb(140, 255, 255, 255),
                leftBubbleTextColor = Color.rgb(46, 46, 46),
            ),
            // 2. 暗夜神殿
            ChatTheme(
                gradientStart = Color.rgb(74, 62, 112),
                gradientEnd = Color.rgb(28, 22, 62),
                direction = GradientDirection.TOP_TO_BOTTOM,
                tintColor = Color.rgb(155, 120, 240),
                leftBubbleColor = Color.argb(184, 60, 50, 95),
                leftBubbleTextColor = Color.rgb(242, 242, 242),
            ),
            // 3. 蜜桃粉
            ChatTheme(
                gradientStart = Color.rgb(255, 245, 242),
                gradientEnd = Color.rgb(255, 217, 209),
                direction = GradientDirection.BOTTOM_TO_TOP,
                tintColor = Color.rgb(235, 105, 110),
                leftBubbleColor = Color.argb(77, 255, 245, 242),
                leftBubbleTextColor = Color.rgb(38, 38, 38),
            ),
            // 4. 抹茶绿
            ChatTheme(
                gradientStart = Color.rgb(242, 250, 237),
                gradientEnd = Color.rgb(209, 237, 194),
                direction = GradientDirection.TOP_TO_BOTTOM,
                tintColor = Color.rgb(76, 175, 80),
                leftBubbleColor = Color.argb(77, 242, 250, 237),
                leftBubbleTextColor = Color.rgb(38, 38, 38),
            ),
            // 5. 日落橙
            ChatTheme(
                gradientStart = Color.rgb(255, 247, 235),
                gradientEnd = Color.rgb(255, 224, 184),
                direction = GradientDirection.BOTTOM_TO_TOP,
                tintColor = Color.rgb(255, 152, 0),
                leftBubbleColor = Color.argb(77, 255, 247, 235),
                leftBubbleTextColor = Color.rgb(38, 38, 38),
            ),
            // 6. 星空靛
            ChatTheme(
                gradientStart = Color.rgb(224, 230, 247),
                gradientEnd = Color.rgb(158, 173, 224),
                direction = GradientDirection.TOP_TO_BOTTOM,
                tintColor = Color.rgb(63, 81, 181),
                leftBubbleColor = Color.argb(77, 224, 230, 247),
                leftBubbleTextColor = Color.rgb(38, 38, 38),
            ),
            // 7. 暗夜紫
            ChatTheme(
                gradientStart = Color.rgb(199, 38, 107),
                gradientEnd = Color.rgb(31, 15, 61),
                direction = GradientDirection.BOTTOM_TO_TOP,
                tintColor = Color.rgb(148, 107, 242),
                leftBubbleColor = Color.argb(77, 199, 38, 107),
                leftBubbleTextColor = Color.rgb(230, 230, 230),
            ),
            // 8. 极简灰
            ChatTheme(
                gradientStart = Color.rgb(246, 247, 250),
                gradientEnd = Color.rgb(246, 247, 250),
                direction = GradientDirection.TOP_TO_BOTTOM,
                tintColor = Color.rgb(55, 120, 244),
                leftBubbleColor = Color.argb(235, 255, 255, 255),
                leftBubbleTextColor = Color.rgb(38, 38, 38),
            ),
            // 9. 幻夜紫
            ChatTheme(
                gradientStart = Color.rgb(165, 150, 187),
                gradientEnd = Color.rgb(31, 31, 76),
                direction = GradientDirection.BOTTOM_TO_TOP,
                tintColor = Color.rgb(155, 120, 240),
                leftBubbleColor = Color.argb(77, 165, 150, 187),
                leftBubbleTextColor = Color.rgb(230, 230, 230),
            ),
            // 10. 晴空蓝
            ChatTheme(
                gradientStart = Color.rgb(240, 247, 255),
                gradientEnd = Color.rgb(199, 224, 255),
                direction = GradientDirection.BOTTOM_TO_TOP,
                tintColor = Color.rgb(69, 137, 246),
                leftBubbleColor = Color.argb(77, 240, 247, 255),
                leftBubbleTextColor = Color.rgb(38, 38, 38),
            ),
            // 11. 薄暮紫
            ChatTheme(
                gradientStart = Color.rgb(245, 237, 250),
                gradientEnd = Color.rgb(209, 184, 235),
                direction = GradientDirection.TOP_TO_BOTTOM,
                tintColor = Color.rgb(128, 90, 210),
                leftBubbleColor = Color.argb(77, 245, 237, 250),
                leftBubbleTextColor = Color.rgb(38, 38, 38),
            ),
        )
    }
}
```

- [ ] **Step 2: 编译**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/teneasy/qldemo/ChatTheme.kt
git commit -m "feat: 新增 demo 主题模型 ChatTheme + 11 套预设（移植自 iOS）"
```

---

## Task 4: 重写主屏布局 fragment_main.xml

**Files:**
- Modify: `app/src/main/res/layout/fragment_main.xml`

- [ ] **Step 1: 整体替换布局内容**

把 `fragment_main.xml` 全文替换为：

```xml
<?xml version="1.0" encoding="utf-8"?>
<layout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    xmlns:app="http://schemas.android.com/apk/res-auto">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:id="@+id/main"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        tools:context=".MainFragment">

        <ImageView
            android:id="@+id/iv_settings"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:padding="8dp"
            android:background="@drawable/settings"
            android:layout_marginTop="16dp"
            android:layout_marginEnd="20dp"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintEnd_toEndOf="parent" />

        <TextView
            android:id="@+id/tv_title"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="选择主题"
            android:textSize="24sp"
            android:textStyle="bold"
            android:textColor="@color/black"
            android:layout_marginTop="60dp"
            android:layout_marginStart="20dp"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintStart_toStartOf="parent" />

        <HorizontalScrollView
            android:id="@+id/theme_scroll"
            android:layout_width="0dp"
            android:layout_height="100dp"
            android:scrollbars="none"
            android:clipToPadding="false"
            android:paddingStart="20dp"
            android:paddingEnd="20dp"
            android:layout_marginTop="20dp"
            app:layout_constraintTop_toBottomOf="@id/tv_title"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toEndOf="parent">

            <LinearLayout
                android:id="@+id/theme_container"
                android:layout_width="wrap_content"
                android:layout_height="match_parent"
                android:orientation="horizontal"
                android:gravity="center_vertical" />
        </HorizontalScrollView>

        <androidx.constraintlayout.utils.widget.MotionButton
            android:id="@+id/btn_send"
            android:layout_width="220dp"
            android:layout_height="56dp"
            android:text="联系客服"
            android:textColor="@android:color/white"
            android:textSize="18sp"
            android:textStyle="bold"
            android:elevation="4dp"
            app:layout_constraintBottom_toTopOf="@id/btn_backup"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toEndOf="parent"
            android:layout_marginBottom="12dp" />

        <androidx.constraintlayout.utils.widget.MotionButton
            android:id="@+id/btn_backup"
            android:layout_width="220dp"
            android:layout_height="50dp"
            android:text="备用客服"
            android:textSize="18sp"
            android:textStyle="bold"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toEndOf="parent"
            android:layout_marginBottom="40dp" />

        <TextView
            android:id="@+id/tv_version_number"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Version: 1.0.0"
            android:textSize="12sp"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            android:layout_marginStart="16dp"
            android:layout_marginBottom="8dp" />

    </androidx.constraintlayout.widget.ConstraintLayout>
</layout>
```

> 注意：`btn_open_pdf` 已移除；`btn_send`/`iv_settings`/`tv_version_number` 的 id 保留，避免后续代码改名。

- [ ] **Step 2: 编译（仅资源/绑定生成）**

Run: `./gradlew :app:assembleDebug`
Expected: 此时 `MainFragment.kt` 仍引用旧的 `btnOpenPdf`，**预期编译失败**于 MainFragment（未解析 `btnOpenPdf`）。这是正常的，下个任务会改 MainFragment。若想单独验证布局，可改跑 `./gradlew :app:processDebugResources`，Expected: BUILD SUCCESSFUL。

- [ ] **Step 3: 提交**

```bash
git add app/src/main/res/layout/fragment_main.xml
git commit -m "feat: 重写主屏布局（主题色卡+两按钮），移除PDF按钮"
```

---

## Task 5: 重写 MainFragment（主题 + 备用客服）

**Files:**
- Modify: `app/src/main/java/com/teneasy/qldemo/MainFragment.kt`

- [ ] **Step 1: 整体替换 MainFragment.kt**

把 `MainFragment.kt` 全文替换为：

```kotlin
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
```

- [ ] **Step 2: 编译整个 app**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL（`btnBackup`/`tvTitle`/`themeContainer` 等由 Task 4 的布局 ViewBinding 生成）

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/teneasy/qldemo/MainFragment.kt
git commit -m "feat: MainFragment 对齐 iOS（主题切换+渐变+备用客服深链）"
```

---

## Task 6: 整体编译 + 手动验证

**Files:** 无（验证任务）

- [ ] **Step 1: 全量编译**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 安装并手动验证（真机/模拟器）**

Run: `./gradlew :app:installDebug` 后打开 App，逐项确认：
- 主屏顶部「选择主题」标题 + 右上角设置图标。
- 横向色卡可滑动，共 11 个；点任意色卡 → 背景渐变、「联系客服」填充色、「备用客服」边框/文字色随之改变；选中色卡有白色描边并放大。
- 点「联系客服」→ 进入客服聊天页（行为同改造前）。
- 点设置图标 → 进入设置页；设置页底部能看到「备用客服网页 URL（可选）」输入框；填入一个 URL 保存后返回再进设置页，值仍在。
- 点「备用客服」：
  - 未安装 `juhekefu://` 处理方 且未配置网页 → Toast「未安装客服中心 App，且未配置备用网页」。
  - 配置了网页 URL → 浏览器打开该网页，query 带 cert/userId/merchantId/userName/userType/themeIndex（有 token 时带 xToken）。
- 底部左下角版本号正常显示。

- [ ] **Step 3: 记录验证结果**

把实际观察到的结果写回对话（哪些通过、哪些异常）。不要在未真正运行的情况下声称通过。

---

## Self-Review 结论

- **Spec 覆盖：** 主题选择器 UI（Task 3/4/5）、渐变背景（Task 5 updateThemeUI）、备用客服深链+网页兜底（Task 5）、备用网页配置项（Task 1/2）、移除 PDF 按钮（Task 4）、线路检测保持现状（未触及 SelectConsultTypeFragment）——均有对应任务。
- **占位符：** 无 TBD/TODO；所有代码步骤含完整代码。
- **类型一致：** `ChatTheme`/`GradientDirection` 字段名在 Task 3 定义，Task 5 使用一致（`gradientStart`/`gradientEnd`/`direction`/`tintColor`/`leftBubbleColor`/`leftBubbleTextColor`）；ViewBinding 名（`btnSend`/`btnBackup`/`ivSettings`/`tvTitle`/`themeContainer`/`tvVersionNumber`/`main`）与 Task 4 布局 id 一致。
- **已知顺序约束：** Task 4 改完布局后单独编 app 会因旧 MainFragment 失败（已在 Task 4 Step 2 注明），Task 5 完成后恢复正常。
