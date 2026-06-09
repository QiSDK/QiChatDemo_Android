# Flutter → Android 同步清单（过去 30 天）

把 Flutter 端 `qichatsdk_demo_flutter/lib/src/`（重点 `vc/` 聊天页与各 `view/` cell）在过去 30 天（约 `2026-05-09` → `HEAD`）的改进，同步到 Android 原生版。

- Flutter 源目录：`/Users/xuefeng/Desktop/teneasy/qichatsdk_demo_flutter/lib/src/`
- Android 目标：`TeneasyChatSDKUI_Android/src/main/java/com/teneasy/chatuisdk/`，聊天页主文件 `ui/main/KeFuFragment.kt`（1564 行）、`ui/main/MessageListAdapter.kt`、`ui/main/EvaluationDialog.kt`、`ui/main/EmojiFragment.kt`
- 分支：`sync/flutter-chatpage-improvements`
- 构建验证（必须 JDK 17，否则 Kotlin 插件报 "Unknown Kotlin JVM target: 21"）：
  ```bash
  JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home \
    ./gradlew :TeneasyChatSDKUI_Android:compileDebugKotlin
  ```

> 说明：Flutter 30 天提交信息大多是 "good"，本清单按**实际 diff 内容**归纳，不按 commit message。前一份 `docs/flutter-sync-tasklist.md` 只覆盖了近两周窗口（第 1–6 项），本清单是过去 30 天的完整版并补齐了它遗漏的大项（**设备信息页、媒体左右滑浏览、全屏下拉关闭、整页渐变主题化**）。

---

## 总览

| 组 | 主题 | 状态 |
|----|------|------|
| A | 聊天页主题化（渐变背景 / 气泡色 / AppBar） | ✅ 完成（图片/视频 cell 为纯媒体无文字气泡，无需着色） |
| B | 客服评价（Evaluation） | ✅ 基本完成，少量待办 |
| C | 撤回 / 编辑消息 | 🟡 撤回完成，编辑待办 |
| D | 媒体浏览（MediaPagerView / 全屏下拉关闭） | ⏳ 待办 |
| E | 输入栏重写（功能面板 / emoji 按钮 / 回复条） | ✅ 完成（重做为 Flutter 同款：圆角输入框+表情/更多两图标+功能面板四按钮） |
| F | 设备信息页（device_info_page，全新） | ✅ 完成（入口已随 E 组迁入底部功能面板） |
| G | 消息时间戳布局（移到气泡上方） | 🟡 待核实 |
| H | 未读管理持久化（unread_manager） | ⏳ 待办 |
| I | 杂项（token key / 引用内容弹窗） | ⏳ 待办 |

---

## A. 聊天页主题化（AppChatTheme）

Flutter 新增 `lib/src/model/AppChatTheme.dart`（对齐 iOS `ChatTheme`），每会话随机一套或由调用方传入，贯穿 ChatPage / EntrancePage / 各 cell。

- [x] **A1 主题模型**：新建 SDK 版 `ui/base/AppChatTheme.kt`（渐变起止色 + 渐变方向 + tintColor + 左右气泡背景/文字色 + `random()` / `fromIndex()` / `presets` 10 套）。
- [x] **A2 整页渐变背景**：`KeFuFragment.applyTheme()` 给根布局 `main` 设 `newGradientDrawable()`；`MessageListAdapter` 把行背景设透明，让渐变透出。
- [x] **A3 头部主题化**：`llTop` 背景=`gradientStartColor`，标题 / 返回键=`tintColor`；新增 `tv_platform_name` 展示 `Constants.platformName`（对齐 Flutter AppBar actions）。
- [x] **A4 气泡用主题色**：文本 cell（`item_text_message`）、文件 cell（`item_file_message` 的 `rl*Imagecontainer` 气泡 + 文件名/大小文字）、图文 cell（`item_text_images_message` 的 `ll_text_images` 气泡 + `tv_msg`）均已套主题左右气泡背景 + 文字色。图片/视频 cell（`item_video_image_message`）是纯媒体，图片填满圆角容器、无文字气泡，无需着色（行背景已透明，渐变可透出）。
- [x] **A5 EntrancePage 主题化**：`SelectConsultTypeFragment.applyEntranceTheme()` 套渐变背景 + 头部 tint + 列表/箭头 `leftBubbleColor`；并把所选主题下标经 nav 参数 `theme_index` 透传给聊天页保持一致。
- [x] **A6 主题透传**：`MessageListAdapter` 构造新增 `theme: AppChatTheme?`，`KeFuFragment` 传入 `chatTheme`；主题下标支持 nav 参数 / Intent extra（`KeFuFragment.EXTRA_THEME_INDEX`），缺省随机。

- [x] **A7 气泡圆角 + 去尾巴箭头**：新增 `bg_bubble_left.xml`（左上角 0、其余 16dp）/ `bg_bubble_right.xml`（右上角 0、其余 16dp），对齐 Flutter `BorderRadius.only` 的尾角样式；文本/文件/图文 cell 的主气泡背景改用这两个 drawable。删除全部聊天气泡尾巴三角（`polygon_1/2` 的 `iv_arrow` / `iv_right_chatarrow`）及 adapter 中相关引用（Flutter 本身无尾巴）。图片/视频 cell 图片保持统一圆角（Flutter 图片用 `circular(12)`，非尾角）。

参考：`ChatPage.dart` build()/_buildChat()，`entrancePage.dart`，`message_cell.dart`，`text_images_cell.dart`。
实现：`AppChatTheme.kt`、`KeFuFragment.applyTheme()`、`MessageListAdapter`（构造参数 + onBindViewHolder 行透明 + 文本/文件/图文气泡着色）、`SelectConsultTypeFragment.applyEntranceTheme()`、`fragment_kefu.xml`（`tv_platform_name`）、`fragment_select_consult_type.xml`（`tv_title` id）。

---

## B. 客服评价（Evaluation）

| # | 任务 | 状态 |
|---|------|------|
| B1 | 下载成功提示改「已保存到相册」 | ✅ 完成 |
| B2 | 评价弹窗点击空白处收起键盘 | ✅ 完成 |
| B3 | 评价完成后按钮置灰禁用 + 状态回调（status 1=已评价/2=已关闭） | ✅ 完成 |
| B4 | 评价按钮仅在本次会话发过消息后显示（`_hasSentInSession`） | ✅ 完成 |

- [ ] **B5 初始化拉取评价状态**：进入会话时调 `evaluationStatus(consultId)`，据 status 1/2 初始化 `_evaluationDone`，避免已评价会话仍显示可点按钮。（Flutter `_fetchEvaluationStatus()`）
- [x] **B6 悬浮按钮防遮挡**：底部面板展开或键盘弹起时隐藏「客服评价」悬浮按钮。已随 E5 落地——见 `KeFuBaseFragment.onBottomExpandedChanged` 钩子驱动 `refreshEvaluationButtonVisibility()` 的 `!bottomExpanded` 门控（对应 Flutter `onExpandedChanged` + `viewInsets.bottom==0`）。
- [x] **B7 评价按钮着色用主题 tintColor**：`refreshEvaluationButtonVisibility()` 用 `chatTheme.tintColor` 替代写死的 `R.color.blue`。

参考：`ChatPage.dart` 评价相关段，`evaluation_dialog.dart`。

---

## C. 撤回 / 编辑消息

| # | 任务 | 状态 |
|---|------|------|
| C1 | 历史记录中的撤回消息显示灰条 | ✅ 完成 |
| C2 | 消息去重（历史 HTTP 与实时 WS 竞态） | ✅ 完成 |

- [ ] **C3 编辑消息（MSG_OP_EDIT）**：编辑后保留**原作者**与**原 createdAt**（不要用当前时间/sender），`metadata` 写 `editedAt` 以便渲染「已编辑」徽标，并把 `tipText` 置回 false（撤回灰条被编辑后恢复普通样式）。Android `KeFuViewModel` 有提到 MSG_OP_EDIT，需核实是否完整实现。
- [ ] **C4 撤回文案统一**为「对方撤回了一条消息」（Flutter 把旧的 "1条" 改为 "一条"，并补 `Content()` 初始化）。

参考：`ChatPage.dart` receivedMsg / queryHistory 中 `MSG_OP_DELETE`、`MSG_OP_EDIT` 段。

---

## D. 媒体浏览（MediaPagerView + 全屏下拉关闭）

Flutter 新增 `lib/src/vc/MediaPagerView.dart` + `lib/src/model/MediaItem.dart` + `ChatPage.currentMediaItems()`。

- [ ] **D1 MediaItem 模型 + 会话媒体收集**：实现 `currentMediaItems()` 等价逻辑——遍历全会话消息，收集图片/视频（含：单图/单视频、多图 JSON 的 `imgs` 数组、系统图文消息 `TextBody.image`/`video` 字段），按时间正序输出 `{url, isVideo}`。注意相对 URL 补 `baseUrlImage` 前缀。
- [ ] **D2 MediaPagerView（左右滑浏览）**：新建 `ViewPager2` Activity/Fragment，图片+视频混排，点击任一缩略图打开并定位到该项，可左右翻页浏览整会话媒体；替代现有 `FullImageActivity`/`FullVideoActivity` 的单项查看。
- [ ] **D3 下拉关闭 + 背景淡出**：垂直拖拽 > 阈值(120) 关闭，否则回弹；背景透明度随拖拽距离淡出（`fadeDistance≈400`）。同样应用到现有 `FullImageActivity` / `FullVideoActivity`（Flutter 的 `FullImageView`/`FullVideoPlayer` 都加了这个手势）。
- [ ] **D4 cell 点击改为打开 MediaPagerView**：`MessageListAdapter` 中 图片/视频/图文/文件 cell 的点击，从打开单项改为打开 `MediaPagerView`（传 startUrl/index）。
- [ ] **D5 File_cell 在线预览**：Office 文档用在线预览地址打开（`view.officeapps.live.com/op/view.aspx?src=` 或 `docs.google.com/gview?embedded=true&url=`）。

参考：`MediaPagerView.dart`、`FullImageView.dart`、`FullVideoPlayer.dart`、`image_thumbnail_cell.dart`、`video_thumbnail_cell.dart`、`text_images_cell.dart`、`File_cell.dart`。

---

## E. 输入栏重写（custom_bottom）

Flutter `lib/src/vc/custom_bottom.dart` 大改（+813 行）。按用户要求把 Android 底栏**重做成 Flutter 同款视觉**：圆角输入框「说点什么吧」+ 右侧两图标（表情 / 更多）；点更多在下方 `panel_more` 展开「图片 / 视频 / 文件 / 设备信息」四个圆形按钮。原来那排内联按钮（图片/视频/文件/字数/发送）已撤掉。

- [x] **E1 功能面板（action panel）**：`layout_more_panel` 改为 图片/视频/文件/设备信息 四个白色圆 + 标签（`bg_panel_action_circle`）；`panel_more` 触发改为新增的「更多」图标 `iv_more`。比 Flutter 多一个「文件」（按用户选择保留 Android 既有发文件能力）。图标用用户提供的 Flutter 同款资源：表情 `biaoqing`、更多 `gengduo_1`(收起)/`gengduo_0`(展开，随面板开合切换)、图片 `tupian`、视频 `shipin`、设备信息 `shebeixinxi`；客服评价星标换 `pingjia`（`iv_evaluation_star` ImageView，按主题 `setColorFilter` 着色，对应 B7）。
- [x] **E2 图片来源选择**：图片→相册(`selectImageOrVideo(0)`)、视频→相册仅视频(新增 `showSelectVideo` / `SelectMimeType.ofVideo()`)、文件→`openFilePicker`、设备信息→`openDeviceInfo`。
- [x] **E3 emoji 面板独立按钮**：emoji 面板（`layout_expression`）已有删除键 `iv_delete_chat`，新增发送键 `iv_emoji_send`（`rounded_corner_blue`）；二者随输入框是否有文字 enable/disable（`KeFuBaseFragment` 文本监听里统一切换 + 发送键 alpha）。
- [x] **E4 回复条 show/hide**：Android 已有 `tv_QuotedMsg` + `iv_Close` 回复条与显隐逻辑，交互一致。
- [x] **E5 面板占位回调（=B6）**：`KeFuBaseFragment` 的 `OnPanelChangeListener` 新增 `onBottomExpandedChanged(expanded)` 钩子（`onKeyboard`/`onPanel`→true、`onNone`→false），`KeFuFragment` 覆写置 `bottomExpanded`，`refreshEvaluationButtonVisibility()` 增加 `&& !bottomExpanded`——键盘或任一面板弹起时隐藏「客服评价」悬浮按钮，对应 Flutter `onExpandedChanged`。同时落地 B6。

> 发送方式（按用户选择）：移除常驻发送按钮，走 **键盘发送键**（`et_msg` 设 `inputType=text` + `imeOptions=actionSend`，`setOnEditorActionListener` → `doSend()`）**+ emoji 面板发送键**。

参考：`custom_bottom.dart`。
实现：`fragment_kefu.xml`（输入行重排：`et_msg` 圆角 pill + `ivEmoj` + `iv_more`，移除 ivPhoto/ivVideo/ivFile/tv_count/btn_send；`panel_more` 触发改 `iv_more`）、`layout_more_panel.xml`（四按钮）、`layout_expression.xml`（`iv_emoji_send`）、`bg_panel_action_circle.xml`、`KeFuBaseFragment`（删除键/发送键 enable + `onBottomExpandedChanged` 钩子）、`KeFuFragment`（`doSend` / IME 监听 / 面板四入口接线 / `openDeviceInfo` / `showSelectVideo` / `bottomExpanded`）。

---

## F. 设备信息页（device_info_page，全新）⭐ 大项

Flutter 新增 `lib/src/vc/device_info_page.dart`（349 行），Android **完全没有**。

- [x] **F1 新建设备信息页** `DeviceInfoActivity` + `activity_device_info.xml`：会员账号、手机型号(`Build.MODEL`)、应用名称、手机系统版本(`Android_${RELEASE}`)、APP 当前版本(`versionName_versionCode (V3)`)、应用包名、当前时间（每秒刷新）、登录 IP、当前线路(`Constants.lines` 中匹配 `domain` → `线路N`)、线路等级、线路扫描。行按 Flutter 风格在白卡里 label/value + 分隔线，programmatic 生成。
  - 与 Flutter 一致：**登录IP / 线路等级 / 线路扫描** 当前为占位「—」（Flutter 实现里也是占位）。
- [x] **F2 保存为图片到相册**：把白卡 `cardInfo` 渲染成 Bitmap（`card.draw(canvas)`）→ 复用 `CapturePhotoUtils.saveImageInQ()` 存入 MediaStore，后台线程执行，toast「已保存到相册 / 保存失败」。
- [x] **F3 主题化**：整页渐变背景 + 头部着色 + 状态栏色（`statusBarColor`），主题下标经 `EXTRA_THEME_INDEX` 由聊天页透传保持一致。
- [x] **F4 入口**：已迁入底部功能面板 `panel_more` 的「设备信息」按钮（`ll_action_device` → `openDeviceInfo()`，对应 Flutter `_onDeviceInfoTap`）；原聊天页头部临时图标 `iv_device_info` 已移除。

参考：`device_info_page.dart`（数据源 `device_info_plus`/`package_info_plus`，线路来自 `config.dart`）。
实现：`DeviceInfoActivity.kt`、`activity_device_info.xml`、`fragment_kefu.xml`（`iv_device_info`）、`KeFuFragment`（`chatThemeIndex` + 点击打开）、`AndroidManifest.xml`（注册 NoActionBar）。

---

## G. 消息时间戳布局

- [ ] **G1 时间戳移到气泡上方**：所有 cell（文本/图文/图片/视频/文件）统一把时间戳放在气泡**外层上方**、按发送方对齐；`msgTime` 来自 `Util.formatTimestamp(createdAt)`。核实 Android `MessageListAdapter` 现有布局是否已是此样式（`msgTime` 字段已存在）。

参考：Flutter commit `f83efcf`（move timestamp above bubble across message cells）。

---

## H. 未读管理持久化（unread_manager）

Flutter `lib/src/manager/unread_manager.dart` 增强。Android `UnReadItem.kt` / `GlobalChatManager.kt` 未见持久化。

- [ ] **H1 持久化未读数**：`consultId -> unreadCount` 写入本地（SharedPreferences），变更后 **debounce 写盘**；启动异步加载历史快照。
- [ ] **H2 `setUnreadIfAbsent` 兜底回填**：从服务端 entrance 快照回填未读时，仅当本地无非零未读才写，避免服务端慢一拍的快照覆盖刚到的新消息（本地 WS 累加优先）。

参考：`unread_manager.dart`。

---

## I. 杂项

- [ ] **I1 token 存储 key**：Flutter 把 `PARAM_XTOKEN` 固定 key 改为 `tokenStorageKey()`（按商户隔离）。核实 Android 是否需要等价的多商户隔离 key。
- [ ] **I2 引用内容弹窗**：长按/点击引用条查看被引用完整内容（Flutter `message_cell` 的 `_showReplyContentDialog`）。

---

## 移植难度与建议顺序

1. **先搭主题基础设施（A 组）** —— B7 / F3 / D 的 AppBar 等都依赖它。
2. **F 设备信息页** —— 相对独立，可单独成 Activity，不阻塞其他。
3. **D 媒体浏览** —— 最重（ViewPager2 + 手势 + 会话媒体收集），建议拆 D1（收集）→ D3（单页下拉关闭，先改造现有全屏页）→ D2/D4（翻页）。
4. **C3 编辑消息、E3 emoji 按钮、H 未读持久化、B5/B6** —— 中等，可并行。
5. **G1 / C4 / I** —— 小修。

> 每完成一项务必跑上方 JDK 17 编译命令验证；涉及 UI 的项建议真机/模拟器走查截图。
