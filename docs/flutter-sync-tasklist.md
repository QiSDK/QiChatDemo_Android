# Flutter ChatPage 改进 → Android 同步任务清单

把 Flutter 端 `lib/src/vc/ChatPage.dart` 及其各 cell 在过去两周（`983797e` → `HEAD`）的改进，同步到 Android 原生版 `KeFuFragment.kt` 及相关文件。

- 分支：`sync/flutter-chatpage-improvements`
- 构建验证：`JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home ./gradlew :TeneasyChatSDKUI_Android:compileDebugKotlin`
  （环境默认 JDK 21 会因 Kotlin 插件报 "Unknown Kotlin JVM target: 21"，需指定 JDK 17）

## 进度

| # | 任务 | 涉及文件 | 难度 | 状态 |
|---|------|---------|------|------|
| 1 | 下载成功提示改为「已保存到相册」 | KeFuFragment | ⭐ | ✅ 完成 |
| 2 | 评价弹窗点击空白处收起键盘 | EvaluationDialog | ⭐ | ✅ 完成 |
| 3 | 历史记录中的撤回消息显示灰条 | KeFuFragment | ⭐ | ✅ 完成 |
| 4 | 消息去重（历史 HTTP 与实时 WS 竞态） | KeFuFragment | ⭐⭐ | ✅ 完成 |
| 5 | 评价按钮仅在本次会话发过消息后显示 | KeFuFragment | ⭐⭐ | ✅ 完成 |
| 6 | 评价完成后按钮置灰禁用 + 状态回调 | KeFuFragment + EvaluationDialog + fragment_kefu.xml | ⭐⭐⭐ | ✅ 完成 |
| 7 | 消息气泡/文字使用主题色 | MessageListAdapter（需先搭主题基础设施） | ⭐⭐⭐ | ⏳ 待办 |
| 8 | 图文 cell：网格列数 / HTML / 颜色 / 容错 | MessageListAdapter | ⭐⭐⭐⭐ | ⏳ 待办 |
| 9 | emoji 面板独立发送/删除按钮 | EmojiFragment / 表情面板 | ⭐⭐⭐ | ⏳ 待办 |
| 10 | 媒体左右滑动浏览器（MediaPagerView） | 新建 ViewPager2 Activity | ⭐⭐⭐⭐⭐ | ⏳ 待办 |
| 11 | 编辑消息处理（保留作者/时间 + 已编辑标记） | KeFuFragment（当前完全没处理 MSG_OP_EDIT） | ⭐⭐ | ⏳ 待办 |

## 重型移植项的注意事项（#7–#11）

- **#7 主题色**：Android 没有 `AppChatTheme`（Flutter 每会话随机一套主题），目前写死 `R.color.blue`。"同步"等于先搭一套主题基础设施。
- **#8 图文 cell**：Android 用 RecyclerView item 布局 + Glide，不是 Flutter 的 `Html` widget；网格列数 1/2/3、`TextBody.color` 解析、JSON try-catch 容错、链接外部打开。
- **#9 emoji 按钮**：对应 `custom_bottom` emoji 面板新增的圆形 backspace + send 按钮。
- **#10 媒体浏览器**：对应 `MediaPagerView`/`MediaItem`/`currentMediaItems`，需收集全会话图片视频，点击任意项可左右翻页；替代现有 `FullImageActivity`/`FullVideoActivity` 单项查看。
- **#11 编辑消息**：Android `receivedMsg` 当前完全没处理 `MSG_OP_EDIT`，这是"从零实现编辑消息"而非"同步改进"。

## 备注

- 第 1 个 commit 把 `KeFuFragment.kt` 中此前未提交的评价功能 WIP 一并带入（单文件内 hunk 无法非交互式拆分）。
- 无关的未提交 WIP（`GlobalChatManager.kt` / `MainApi.java` / `styles.xml`）未被触碰，仍为未提交状态。
