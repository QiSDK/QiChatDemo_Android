# MainFragment → iOS ViewController 对齐设计

日期: 2026-06-08
分支: sync/flutter-chatpage-improvements

## 背景

iOS Demo 的 `ViewController.swift` 主屏幕有 4 块功能；Android `MainFragment.kt` 目前只有一个「联系客服」按钮、隐藏的 PDF 按钮、设置图标和版本号。本设计把 Android 主屏幕对齐到 iOS，但根据已确认的范围做裁剪。

## 范围决策（已与用户确认）

| 功能 | iOS | 本次 Android |
|------|-----|-------------|
| 设置按钮（右上角） | 有 | 复用现有 `iv_settings`，调整样式 |
| 主题选择器 | 渐变主题 + tintColor 传入聊天 | **只做主屏幕 UI**，切换改变主屏外观；**不**传入聊天 SDK（聊天界面保持固定蓝色） |
| 备用客服按钮 | `juhekefu://` 深链 + 网页兜底 | **实现**，含新增「备用网页 URL」设置项 |
| 线路检测 + 状态文字 | 在主屏幕 | **保持现状**，仍在 `SelectConsultTypeFragment`，主屏不做检测 |
| PDF 示例按钮 | 无 | **移除** |

## 组件设计

### 1. 新增 `app/src/main/java/com/teneasy/qldemo/ChatTheme.kt`

Demo 专用数据类，只包含主屏幕用到的字段（不复制 iOS 中聊天气泡相关字段以外用不到的部分，但保留 `leftBubbleColor`/`leftBubbleTextColor` 因为 iOS `themeSelected` 用它们给标题着色）。

```kotlin
enum class GradientDirection { TOP_TO_BOTTOM, BOTTOM_TO_TOP, LEFT_TO_RIGHT,
    RIGHT_TO_LEFT, TOP_LEFT_TO_BOTTOM_RIGHT, TOP_RIGHT_TO_BOTTOM_LEFT }

data class ChatTheme(
    val gradientStart: Int,        // ARGB
    val gradientEnd: Int,
    val direction: GradientDirection,
    val tintColor: Int,
    val leftBubbleColor: Int,
    val leftBubbleTextColor: Int,
) {
    companion object { val presets: List<ChatTheme> /* 11 套，按 iOS ChatTheme.swift 忠实移植 */ }
}
```

- 颜色用 `android.graphics.Color.argb` 表示，数值与 iOS 一一对应（iOS 0~1 RGBA → 0~255）。
- `GradientDirection` → 在 `MainFragment` 里映射到 `GradientDrawable.Orientation`。
- 11 套预设的颜色/方向严格照搬 iOS `presets`（晨雾白、暗夜神殿、蜜桃粉、抹茶绿、日落橙、星空靛、暗夜紫、极简灰、幻夜紫、晴空蓝、薄暮紫）。

### 2. 重写 `app/src/main/res/layout/fragment_main.xml`

`ConstraintLayout`（根，渐变在代码里设置）包含：
- `iv_settings`（保留 id）：右上角，白色 tint，半透明圆形背景
- `tv_title`「选择主题」：左上，24sp 加粗
- `theme_scroll`（`HorizontalScrollView`）+ 内部 `LinearLayout` `theme_container`：色卡在代码里按预设动态添加
- `btn_send`「联系客服」（保留 id）：填充 tintColor，圆角，阴影/elevation
- `btn_backup`「备用客服」：描边按钮（tintColor 边框+文字，透明背景）
- `tv_version_number`（保留 id）：底部角落

移除 `btn_open_pdf`。

### 3. 重写 `MainFragment.kt`

状态：`selectedTheme: ChatTheme = ChatTheme.presets[0]`、`selectedThemeIndex: Int = 0`。

- `onCreateView`：构建色卡（每个预设一个圆形 swatch，背景=`gradientEnd`），点击 → 更新 `selectedTheme`/index + 动画 + `updateThemeUI()`，并把标题背景/文字着色（对齐 iOS `themeSelected`）。
- `updateThemeUI()`：
  - 根视图背景 = `GradientDrawable(orientation, intArrayOf(gradientStart, gradientEnd))`
  - `btn_send` 背景色/阴影色 = `tintColor`
  - `btn_backup` 边框色 + 文字色 = `tintColor`
  - 选中 swatch 加白色描边 + 放大 1.15，其它还原
  - `iv_settings` tint 白色（选中后 iOS 设为 tintColor，可对齐）
- `btn_send` → `startActivity(Intent(requireContext(), KeFuActivity::class.java))`（行为不变）
- `iv_settings` → `findNavController().navigate(R.id.frg_settings)`（不变）
- `btn_backup` → `openBackupCustomerService()`
- 版本号：保留现有 `getAppVersion`。

### 4. 备用客服深链逻辑

```
openBackupCustomerService():
  读取 Constants.cert / userId / merchantId / userName / userType / xToken
  themeIndex = selectedThemeIndex
  构建 Uri: juhekefu://open?cert=..&userId=..&merchantId=..&userName=..&userType=..&themeIndex=..[&xToken=..]
  try startActivity(Intent(ACTION_VIEW, uri))
  catch ActivityNotFoundException -> openBackupWebUrl(params)

openBackupWebUrl(params):
  webUrl = Constants.backupWebUrl.trim()
  if webUrl 为空 -> Toast("未安装客服中心 App，且未配置备用网页"); return
  把 params 作为 query 追加到 webUrl
  try startActivity(ACTION_VIEW, 合并后的 url)
  catch -> Toast("打开备用网页失败")
```

- 参数用 `Uri.Builder` / `appendQueryParameter`，自动编码。
- 与 iOS `backupClick` / `openBackupWebUrl` 行为一致。

### 5. 备用网页 URL 配置

- `Constants.kt`：新增 `const val PARAM_BACKUP_WEB_URL = "PARAM_BACKUP_WEB_URL"`（与 iOS key 完全一致）和 `var backupWebUrl = ""`。
- `Utils.readConfig()`：增加 `Constants.backupWebUrl = UserPreferences().getString(PARAM_BACKUP_WEB_URL, Constants.backupWebUrl)`。
- `fragment_settings.xml`：在保存按钮前增加一个 `TextInputLayout` + `EditText`（id `et_BackupWebUrl`，hint「备用客服网页 URL（可选）」）。
- `SettingsFragment.kt`：填充已保存值；保存时写入 `Constants.backupWebUrl` 和 `UserPreferences().putString(PARAM_BACKUP_WEB_URL, ...)`。

## 不在范围内

- 不修改聊天 SDK，不把主题传入 `KeFuActivity` / 聊天界面。
- 不把线路检测搬到主屏幕。
- 不动 `GlobalChatManager`、`MainApi`、评价相关等已有未提交改动。

## 测试 / 验证

- 项目无单元测试覆盖该 UI，采用编译 + 手动验证：
  - `./gradlew :app:assembleDebug` 编译通过。
  - 手动：切换主题主屏渐变/按钮变色；点「联系客服」进入聊天；点「备用客服」未安装时按是否配置网页 Toast 或跳转；设置页可保存备用网页 URL。

## 风险

- `juhekefu://` scheme 在本机未注册会抛 `ActivityNotFoundException`，已用 try/catch 兜底。
- 渐变 + 动态色卡为纯代码绘制，无新增图片资源依赖。
