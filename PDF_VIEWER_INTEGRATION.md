# Android PDF查看功能集成指南

## 概述
本文档说明了如何在Android应用中集成在线PDF文件查看功能。该功能允许用户直接在应用内查看在线PDF文件，而无需离开应用。

## 功能特性
- 在线PDF文件查看
- 支持PDF文件的缩放和滚动
- 显示加载进度和错误处理
- 可自定义的PDF查看界面

## 集成步骤

### 1. 添加依赖
PDF查看功能依赖于`AndroidPdfViewer`库，该依赖已经在`app/build.gradle`文件中添加：

```gradle
implementation 'com.github.barteksc:android-pdf-viewer:3.2.0-beta.1'
```

### 2. 添加网络权限
确保在`AndroidManifest.xml`中添加了网络权限：

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

### 3. 注册Activity
PDF查看器Activity已在`AndroidManifest.xml`中注册：

```xml
<activity android:name=".PdfViewerActivity"
    android:exported="false">
</activity>
```

### 4. 使用PDF查看功能

#### 4.1 通过Utils工具类打开PDF文件
在代码中使用`Utils.openPdfFile()`方法打开PDF文件：

```kotlin
val utils = com.teneasy.chatuisdk.ui.base.Utils()
utils.openPdfFile(context, "https://example.com/sample.pdf", "PDF文档标题")
```

参数说明：
- `context`: Android上下文
- `pdfUrl`: PDF文件的在线URL地址
- `pdfTitle`: PDF文档的标题（可选）

#### 4.2 直接启动PdfViewerActivity
也可以直接启动PdfViewerActivity来查看PDF文件：

```kotlin
val intent = Intent(context, PdfViewerActivity::class.java)
intent.putExtra("pdf_url", "https://example.com/sample.pdf")
intent.putExtra("pdf_title", "PDF文档标题")
startActivity(intent)
```

## 自定义配置

### 修改PDF查看器界面
可以通过修改`activity_pdf_viewer.xml`布局文件来自定义PDF查看器的界面。

### 调整PDF查看参数
在`PdfViewerActivity.kt`中，可以调整以下参数：
- `spacing`: 页面间距
- `defaultPage`: 默认显示的页面
- 滚动处理方式等

## 错误处理
PDF查看器包含了完整的错误处理机制：
- 网络连接错误
- 文件下载失败
- PDF文件解析错误
- 页面加载错误

所有错误都会在界面上显示相应的提示信息。

## 示例代码
在`MainFragment.kt`中有一个使用示例：

```kotlin
this.btnOpenPdf.setOnClickListener {
    val pdfUrl = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf"
    val utils = com.teneasy.chatuisdk.ui.base.Utils()
    utils.openPdfFile(requireContext(), pdfUrl, "示例PDF文档")
}
```

## 注意事项
1. 确保设备具有网络连接权限
2. PDF文件URL必须是可公开访问的
3. 大文件可能需要较长时间加载，请考虑添加加载提示
4. 某些受保护的PDF文件可能无法正常显示

## 支持的PDF特性
- 文本选择和复制
- 缩放和滚动
- 密码保护的PDF文件（需要提供密码）
- 注释和表单（部分支持）

## 故障排除
如果遇到问题，请检查：
1. 网络权限是否正确配置
2. PDF文件URL是否有效
3. 设备是否有足够的存储空间
4. 应用是否有足够的运行内存

## 版本兼容性
该功能支持Android API 26及以上版本。

## 更新日志
- v1.0.0: 初始版本，支持基本的PDF查看功能
