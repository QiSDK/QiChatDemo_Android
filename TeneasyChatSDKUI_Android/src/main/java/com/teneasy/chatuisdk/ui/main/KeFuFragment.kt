package com.teneasy.chatuisdk.ui.main;

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.gson.Gson
import com.luck.picture.lib.basic.PictureSelector
import com.luck.picture.lib.config.SelectMimeType
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.interfaces.OnResultCallbackListener
import com.luck.picture.lib.thread.PictureThreadUtils.runOnUiThread
import com.luck.picture.lib.utils.ToastUtils
import com.teneasy.chatuisdk.ARG_IMAGEURL
import com.teneasy.chatuisdk.ARG_KEFUNAME
import com.teneasy.chatuisdk.BR
import com.teneasy.chatuisdk.MediaPagerActivity
import com.teneasy.chatuisdk.ui.base.ChatMediaItem
import com.teneasy.chatuisdk.R
import com.teneasy.chatuisdk.SelectConsultTypeViewModel
import com.teneasy.chatuisdk.WebViewActivity
import com.teneasy.chatuisdk.ui.base.Constants
import com.teneasy.chatuisdk.DeviceInfoActivity
import com.teneasy.chatuisdk.ui.base.AppChatTheme
import com.teneasy.chatuisdk.ui.base.Constants.Companion.CONSULT_ID
import com.teneasy.chatuisdk.ui.base.Constants.Companion.baseUrlApi
import com.teneasy.chatuisdk.ui.base.Constants.Companion.chatLib
import com.teneasy.chatuisdk.ui.base.Constants.Companion.domain
import com.teneasy.chatuisdk.ui.base.Constants.Companion.fileTypes
import com.teneasy.chatuisdk.ui.base.Constants.Companion.getCustomParam
import com.teneasy.chatuisdk.ui.base.Constants.Companion.imageTypes
//import com.teneasy.chatuisdk.ui.base.Constants.Companion.unSentMessage
import com.teneasy.chatuisdk.ui.base.Constants.Companion.videoTypes
import com.teneasy.chatuisdk.ui.base.Constants.Companion.withAutoReplyU
import com.teneasy.chatuisdk.ui.base.Constants.Companion.workerAvatar
import com.teneasy.chatuisdk.ui.base.Constants.Companion.xToken
import com.teneasy.chatuisdk.ui.base.GlobalChatListener
import com.teneasy.chatuisdk.ui.base.GlobalChatManager
import com.teneasy.chatuisdk.ui.base.GlobalMessageManager
import com.teneasy.chatuisdk.ui.base.GlideEngine
import com.teneasy.chatuisdk.ui.base.PARAM_DOMAIN
import com.teneasy.chatuisdk.ui.base.UserPreferences
import com.teneasy.chatuisdk.ui.base.Utils
import com.teneasy.chatuisdk.ui.http.MainApi
import com.teneasy.chatuisdk.ui.http.ReturnData
import com.teneasy.chatuisdk.ui.http.bean.EvaluationConfig
import com.teneasy.chatuisdk.ui.http.bean.EvaluationStatus
import com.teneasy.chatuisdk.ui.http.bean.TextBody
import com.teneasy.chatuisdk.ui.http.bean.TextImages
import com.teneasy.chatuisdk.ui.http.bean.WorkerInfo
import com.teneasy.sdk.ChatLib
import com.teneasy.sdk.Result
import com.teneasy.sdk.TeneasySDKDelegate
import com.teneasy.sdk.ui.CellType
import com.teneasy.sdk.ui.MessageItem
import com.teneasy.sdk.ui.MessageSendState
import com.teneasy.sdk.ui.ReplyMessageItem
import com.teneasyChat.api.common.CMessage
import com.teneasyChat.gateway.GGateway
import com.google.gson.JsonObject
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.callback.ProgressLoadingCallBack
import com.xuexiang.xhttp2.exception.ApiException
import com.xuexiang.xhttp2.subsciber.ProgressDialogLoader
import com.xuexiang.xhttp2.subsciber.impl.IProgressLoader
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.Date
import java.util.UUID
import kotlin.collections.ArrayList


/**
 * 客服主界面fragment
 */
class KeFuFragment : KeFuBaseFragment(), TeneasySDKDelegate {
    companion object {
        private const val PICK_FILE_REQUEST_CODE = 1001

        /** 宿主可通过该 Intent extra 指定主题下标；缺省 / 负数表示随机一套。 */
        const val EXTRA_THEME_INDEX = "theme_index"
    }

    // 聊天页主题：优先用入口页透传的主题（nav 参数），其次宿主 Intent extra，最后随机一套（对齐 Flutter AppChatTheme）
    val chatTheme: AppChatTheme by lazy {
        val fromArgs = arguments?.getInt(EXTRA_THEME_INDEX, -1) ?: -1
        val index = if (fromArgs >= 0) fromArgs else activity?.intent?.getIntExtra(EXTRA_THEME_INDEX, -1) ?: -1
        AppChatTheme.fromIndex(index)
    }

    /** 当前主题在预设中的下标，用于透传给设备信息页保持一致。 */
    private val chatThemeIndex: Int get() = AppChatTheme.presets.indexOf(chatTheme)

    // UI组件
    private lateinit var msgAdapter: MessageListAdapter
    private lateinit var viewModel: KeFuViewModel
    private lateinit var dialogBottomMenu: DialogBottomMenu
    private lateinit var pickFileLauncher: ActivityResultLauncher<Intent>
    private lateinit var uploadHandler: ChatUploadHandler

    // 状态变量
    private var mIProgressLoader: IProgressLoader? = null
    private var isFirstLoad = true
    private var tempContent = ""
    private var chatExpireTime = 0 // 会话过期时间（秒）
    private var uploadProgressTimer: Handler? = null
    private var uploadProgressRunnable: Runnable? = null

    // 聊天相关数据
    private var lastMsg: CMessage.Message? = null
    private var workInfo = WorkerInfo()
    private var lastActiveDateTime = Date()

    // 评价功能
    private var evaluationConfig: EvaluationConfig? = null
    private var evaluationDialog: EvaluationDialog? = null

    // 仅统计本次会话内用户新发出的消息（不含历史记录），用于控制「客服评价」按钮的显示
    private var hasSentInSession = false

    /** 底部键盘/面板是否占用，占用时隐藏评价悬浮按钮（对应 Flutter onExpandedChanged 门控）。 */
    private var bottomExpanded = false

    // 评价状态：0=未评价 1=已评价 2=已关闭 3=空会话。1/2 视为已完成，按钮置灰禁用
    private var evaluationStatus: Int? = null
    private val isEvaluationDone get() = evaluationStatus == 1 || evaluationStatus == 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupBackPressHandler()
        viewModel = KeFuViewModel()
        ensureDomainExists()
        // SDK已在SelectConsultTypeFragment中通过GlobalChatManager初始化
        // GlobalChatManager会自动管理连接，无需手动初始化
        mIProgressLoader = getProgressLoader()
        initializePickFileLauncher()
    }

    /**
     * 确保域名存在
     */
    private fun ensureDomainExists() {
        if (Constants.domain.isEmpty()) {
            Utils().readConfig()
        }
    }

    /**
     * 设置返回键处理
     */
    private fun setupBackPressHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            exitChat()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // GlobalChatManager 已在 SelectConsultTypeFragment 中初始化并自动管理连接

        uploadHandler = ChatUploadHandler(
            fragment = this,
            lifecycleScope = viewLifecycleOwner.lifecycleScope,
            progressLoaderProvider = { getProgressLoader() },
            callbacks = object : ChatUploadHandler.Callbacks {
                override fun onCallBackImageUploaded(urls: com.teneasy.sdk.Urls) {
                    onUploadSuccess(urls, false)
                }

                override fun onCallBackVideoUploaded(urls: com.teneasy.sdk.Urls) {
                    onUploadSuccess(urls, true)
                }

                override fun onCallBackUploadFailed(message: String) {
                    onUploadFailed(message)
                }

                override fun onCallBackUploadProgress(progress: Int) {
                    onUploadProgress(progress)
                }
            }
        )

        requireActivity().title = "客服"
        hidetvQuotedMsg()
        isFirstLoad = true
        // 注册会话媒体收集器，供媒体浏览器左右滑浏览（对齐 Flutter ChatPage.currentMediaItems）
        MediaPagerActivity.mediaItemsProvider = { currentMediaItems() }
        viewModel.mlAutoReplyItem.observe(viewLifecycleOwner, {
            if (it?.qa?.size ?: 0 > 0) {
                msgAdapter.setAutoReply(it!!)
            }

            var list = ArrayList<MessageItem>()
            //添加自动回复Cell
            var qaItem = MessageItem()
            qaItem.sendStatus = MessageSendState.发送成功
            qaItem.cellType = CellType.TYPE_QA
            list.add(qaItem)
            //viewModel.addAllMsgItem(qaList)

            //添加一个空白Cell，确保列表滚动到最后能看到所有内容
            qaItem = MessageItem()
            qaItem.sendStatus = MessageSendState.发送成功
            qaItem.cellType = CellType.TYPE_LastLine
            list.add(qaItem)

            viewModel.addAllMsgItem(list)
        })

        binding?.ivClose?.setOnClickListener {
            hidetvQuotedMsg()
        }

        binding?.llEvaluationButton?.setOnClickListener {
            onEvaluationButtonClick()
        }

        fetchEvaluationConfig()
    }


    override fun onResume() {
        super.onResume()
        GlobalChatManager.instance.connectIfNeeded()
        // 设置当前Fragment为活跃监听器
        GlobalChatListener.instance.activeListener = this

        // 设置当前聊天的consultId
        Constants.currentChatConsultId = Constants.CONSULT_ID
        // 清零当前会话的未读数
        GlobalMessageManager.instance.clearUnReadCount(Constants.CONSULT_ID)
        // 通知全局消息委托，未读数已更新
        Constants.globalMessageDelegate?.onMessageReceived(Constants.CONSULT_ID)


            updateWorkerNameIfAvailable()

    }

    override fun onPause() {
        super.onPause()
        // 离开聊天页面时，重置当前聊天consultId
        Constants.currentChatConsultId = 0

        viewModel.apply {
           // getUnSendMsg()
            reportError()
        }
    }

    /**
     * 更新客服名称（如果可用）
     */
    private fun updateWorkerNameIfAvailable() {
        if (workInfo.workerName != null) {
            binding?.tvTitle?.text = workInfo.workerName
        }
    }

    // UI初始化
    override fun initView() {
        binding?.apply {

            this.setVariable(BR.vm, viewModel)

            // 初始化聊天消息列表
            msgAdapter = MessageListAdapter(requireActivity(), object : MessageItemOperateListener {
                //长按消息，删除消息的功能，按实际需求，可能不需要
                override fun onDelete(position: Int) {
                    val messageItem = msgAdapter.msgList?.get(position)
                    messageItem?.let {
                        Constants.chatLib?.deleteMessage(it.cMsg?.msgId ?: 0)
                        viewModel.removeMsgItem(it)
                    }
                }

                //长按消息，复制文本内容
                override fun onCopy(position: Int) {
                    val messageItem = msgAdapter.msgList?.get(position)
                    var text = messageItem?.cMsg?.content?.data ?: ""
                    var srcType =
                        messageItem?.cMsg?.msgSourceType ?: CMessage.MsgSourceType.MST_DEFAULT
                    if (srcType == CMessage.MsgSourceType.MST_SYSTEM_WORKER || srcType == CMessage.MsgSourceType.MST_SYSTEM_CUSTOMER) {
                        val gson = Gson()
                        try {
                            val textBody: TextBody = gson.fromJson(text, TextBody::class.java)
                            text = textBody.content ?: ""
                        } catch (e: Exception) {

                        }
                    } else if (text.contains("\"imgs\"")) {
                        val gson = Gson()
                        try {
                            val textBody: TextImages = gson.fromJson(text, TextImages::class.java)
                            text = textBody.message
                        } catch (e: Exception) {

                        }
                    }
                    Utils().copyText(text, requireContext())
                }

                //重发发送失败的消息
                override fun onReSend(position: Int) {
                    //chatLib.resendMSg(msg, 0)
                }

                override fun onPlayVideo(url: String) {
                    // D4：打开会话媒体浏览器并定位到该视频（对齐 Flutter MediaPagerView(startUrl)）
                    MediaPagerActivity.start(requireActivity(), url, isVideo = true)
                }

                override fun onOpenFile(url: String) {
                    //Utils().openPdfInBrowser(requireContext(), Uri.parse(url))
                    //onPlayImage(url)
                    var ext = url?.split(".")?.last() ?: ""
                    if (ext.lowercase() == "pdf") {
                        //val pdfUrl = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf"
                        Utils().openPdfFile(requireContext(), url, "示例PDF文档")
                        return
                    } else if (ext.lowercase() == "csv") {
                        //googleDocsUrl = "https://docs.google.com/gview?embedded=true&url=$imageUrl"
                        ToastUtils.showToast(
                            requireActivity(),
                            "暂不支持在线查看CSV文件，但您可以下载后再浏览。（确保您的设备里有查看CSV文件的应用程序）"
                        )
                        return
                    }

                    // D5：Office 文档用在线预览地址打开（对齐 Flutter File_cell onTap 的 officeapps 链接）
                    val previewUrl = "https://view.officeapps.live.com/op/view.aspx?src=" +
                        java.net.URLEncoder.encode(url, "UTF-8")
                    val intent = Intent(requireContext(), WebViewActivity::class.java)
                    intent.putExtra(ARG_IMAGEURL, previewUrl)
                    intent.putExtra(ARG_KEFUNAME, workInfo.workerName)
                    intent.setClass(requireContext(), WebViewActivity::class.java)
                    requireActivity().startActivity(intent)
                }

                override fun onPlayImage(url: String) {
                    // D4：打开会话媒体浏览器并定位到该图片（对齐 Flutter MediaPagerView(startUrl)）
                    MediaPagerActivity.start(requireActivity(), url, isVideo = false)
                }

                //长按消息，引用消息并回复
                override fun onQuote(position: Int) {
                    binding?.llReplyBar?.visibility = View.VISIBLE
                    binding?.tvQuotedMsg?.tag = position
                    val msg = msgAdapter.msgList?.get(position)?.cMsg
                    val srcType = msg?.msgSourceType ?: CMessage.MsgSourceType.MST_SYSTEM_WORKER
                    if ((msg?.image?.uri ?: "").isNotEmpty()) {
                        showQuotedMsg("回复：【图片】")
                    } else if ((msg?.file?.uri ?: "").isNotEmpty()) {
                        showQuotedMsg("回复：【文件】")
                    } else if ((msg?.video?.uri ?: "").isNotEmpty()) {
                        showQuotedMsg("回复：【视频】")
                    } else {
                        var txt = msgAdapter.msgList?.get(position)?.cMsg?.content?.data ?: " "
                        txt = txt.split("回复：")[0]
                        if (txt.contains("\"imgs\"")) {
                            val gson = Gson()
                            try {
                                val textBody: TextImages =
                                    gson.fromJson(txt, TextImages::class.java)
                                txt = textBody.message
                            } catch (e: Exception) {

                            }
                        }
                        showQuotedMsg("回复：" + txt)
                    }
                }

                //长按消息，引用消息并回复
                override fun onDownload(position: Int) {
                    val msg = msgAdapter.msgList?.get(position)?.cMsg
                    var url = ""
                    if ((msg?.image?.uri ?: "").isNotEmpty()) {
                        url = Constants.baseUrlImage + msg?.image?.uri ?: ""
                    } else if ((msg?.video?.uri ?: "").isNotEmpty()) {
                        url = Constants.baseUrlImage + msg?.video?.uri ?: ""
                    } else if ((msg?.file?.uri ?: "").isNotEmpty()) {
                        url = Constants.baseUrlImage + msg?.file?.uri ?: ""
                        Utils().openFileInBrower(requireContext(), url)
                        return
                    }
                    this@KeFuFragment.mIProgressLoader?.updateMessage("请稍等...")
                    this@KeFuFragment.mIProgressLoader?.showLoading()
                    Utils().downloadFile(url, object :
                            (Int) -> Unit {
                        override fun invoke(progress: Int) {
                            Log.d(TAG, "下载进度：$progress")
                            if (progress == 100) {
                                this@KeFuFragment.mIProgressLoader?.dismissLoading()
                                ToastUtils.showToast(
                                    this@KeFuFragment.requireContext(),
                                    "已保存到相册"
                                );
                            } else if (progress == -1) {
                                this@KeFuFragment.mIProgressLoader?.dismissLoading()
                                ToastUtils.showToast(
                                    this@KeFuFragment.requireContext(),
                                    "下载失败！"
                                );
                                return
                            }
                        }
                    });
                }

                override fun onShowOriginal(position: Int) {
                    val curMsg = viewModel.mlMsgList.value?.get(position)
                    curMsg?.let {
                        var ext = it.replyItem?.fileName?.split(".")?.last() ?: "#"
                        var fileName = it.replyItem?.fileName ?: "#"
                        if (imageTypes.contains(ext)) {
                            onPlayImage(Constants.baseUrlImage + fileName)
                        } else if (videoTypes.contains(ext)) {
                            onPlayVideo(Constants.baseUrlImage + fileName)
                        } else {
                            onOpenFile(Constants.baseUrlImage + fileName)
                        }
                    }
                }

                override fun onShowReplyContent(text: String) {
                    showReplyContentDialog(text)
                }

                //这里实现自动回复的功能，属于本地消息
                override fun onSendLocalMsg(msg: String, isLeft: Boolean, msgType: String) {
                    if (msgType == "MSG_TEXT") {
                        this@KeFuFragment.sendLocalMsg(msg, isLeft)
                    } else if (msgType == "MSG_IMG") {
                        this@KeFuFragment.sendLocalImgMsg(msg, isLeft)
                    }
                }
            }, chatTheme)

            //初始化一个空的列表给adapter
            if (viewModel.mlMsgList.value?.isEmpty() == true) {
                msgAdapter.setList(ArrayList())
            }

            //设置recycleView的LayoutManager
            val layoutManager = LinearLayoutManager(context)
            layoutManager.orientation = LinearLayoutManager.VERTICAL
            this.rcvMsg.layoutManager = layoutManager
            this.rcvMsg.adapter = msgAdapter

            // 应用聊天页主题（渐变背景 + 头部着色 + 平台名）
            applyTheme()

            // 初始化输入框
            this.etMsg.setOnFocusChangeListener { v: View, hasFocus: Boolean ->
                if (!hasFocus) {
                    Utils().closeSoftKeyboard(v)
                }
            }
            // 聊天界面输入框，输入事件。实现文本输入和表情输入的UI切换功能
            this.etMsg.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    // 输入框有内容的时候，显示发送按钮，隐藏图片选择按钮
                    viewModel.mlBtnSendVis.value = s != null && s.isNotEmpty()
                }

                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?, start: Int, before: Int,
                    count: Int
                ) {
                    val txt = binding?.etMsg?.text ?: ""
                    if (txt.length > 500) {
                        binding?.etMsg!!.setText(txt.substring(0, 500))
                    }
                }
            })

            // 发送走键盘发送键（imeOptions=actionSend），不再常驻发送按钮
            this.etMsg.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    doSend()
                    true
                } else {
                    false
                }
            }
            this.etMsg.isFocusable = true
            this.etMsg.isFocusableInTouchMode = true;

            // emoji 面板内发送键
            this.panelEmotion.findViewById<View>(R.id.iv_emoji_send)?.setOnClickListener {
                doSend()
            }

            // 功能面板四个入口：图片 / 视频 / 文件 / 设备信息（对齐 Flutter _buildActionPanel）
            this.panelMore.findViewById<View>(R.id.ll_action_image)?.setOnClickListener {
                selectImageOrVideo(0)
            }
            this.panelMore.findViewById<View>(R.id.ll_action_video)?.setOnClickListener {
                selectImageOrVideo(2)
            }
            this.panelMore.findViewById<View>(R.id.ll_action_file)?.setOnClickListener {
                openFilePicker()
            }
            this.panelMore.findViewById<View>(R.id.ll_action_device)?.setOnClickListener {
                openDeviceInfo()
            }
            // 底部菜单初始化
            dialogBottomMenu = DialogBottomMenu(context)
                .setItems(resources.getStringArray(R.array.bottom_menu))
                .setOnItemClickListener(AdapterView.OnItemClickListener { adapterView, view, i, l ->
                    selectImageOrVideo(i)
                })
                .build()
            this.tvTips.visibility = View.GONE
            initObserver()

            this.llClose.setOnClickListener {
                exitChat()
            }
        }

        print("已经连接？" + chatLib?.isConnected)
        if (chatLib?.isConnected == true) {
            afterConnected()
        }
    }

    private fun selectImageOrVideo(i: Int) {
        when (i) {
            0 -> {
                // 选择相册
                showSelectPic(object : OnResultCallbackListener<LocalMedia> {
                    override fun onResult(result: java.util.ArrayList<LocalMedia>) {
                        if (result != null && result.size > 0) {
                            val item = result[0]
                            dialogBottomMenu.dismiss()
                            uploadHandler.beforeUpload(item.realPath)
                        }
                    }

                    override fun onCancel() {}
                })
            }

            1 -> {
                // 拍照
                showCamera(object : OnResultCallbackListener<LocalMedia> {
                    override fun onResult(result: java.util.ArrayList<LocalMedia>) {
                        if (result != null && result.size > 0) {
                            val item = result[0]
                            dialogBottomMenu.dismiss()
                            uploadHandler.beforeUpload(item.realPath)
                        }
                    }

                    override fun onCancel() {}
                })
            }

            2 -> {
                // 选择视频（相册，仅视频）
                showSelectVideo(object : OnResultCallbackListener<LocalMedia> {
                    override fun onResult(result: java.util.ArrayList<LocalMedia>) {
                        if (result != null && result.size > 0) {
                            val item = result[0]
                            dialogBottomMenu.dismiss()
                            uploadHandler.beforeUpload(item.realPath)
                        }
                    }

                    override fun onCancel() {}
                })
            }

            else -> {
                dialogBottomMenu.dismiss()
            }
        }
    }

    private fun initializePickFileLauncher() {
        pickFileLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.data?.let { fileUri ->
                        val fileName =
                            Utils().getFileNameFromUri(requireContext().contentResolver, fileUri)
                        val myFile = Utils().uriToFile(requireContext(), fileUri, fileName)
                        uploadHandler.beforeUpload(myFile.absolutePath)
                    }
                } else {
                    Log.w(TAG, "File selection cancelled or failed.")
                }
            }
    }

    fun openFilePicker() {
        val ALLOWED_MIME_TYPES = arrayOf(
            "application/msword", // .doc
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
            "application/pdf", // .pdf
            "application/vnd.ms-excel", // .xls
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // .xlsx
            "application/csv",
            "text/csv", "application/csv", "text/comma-separated-values",
        )
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*" // Set a general type to allow filtering
            putExtra(Intent.EXTRA_MIME_TYPES, ALLOWED_MIME_TYPES) // Filter by allowed MIME types
        }
        pickFileLauncher.launch(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.data?.let { fileUri ->
                val fileName = Utils().getFileNameFromUri(requireContext().contentResolver, fileUri)
                var myFile = Utils().uriToFile(requireContext(), fileUri, fileName)
                //UploadUtil(this).uploadFile(myFile)
            }
        }
    }

    private fun initObserver() {
        viewModel.mlMsgList.observe(viewLifecycleOwner) {
            if ((it?.count() ?: 0) > 0) {
                msgAdapter.setList(it)
                refreshList()
            }
        }

        viewModel.mlAssignWorker.observe(viewLifecycleOwner) {
            if (it == null) {

                Log.d(TAG, "assignWorker 失败: null")
                return@observe
            }
            workInfo.workerName = it.nick
            workInfo.workerAvatar = it.avatar
            workerAvatar = it.avatar ?: ""
            workInfo.id = it.workerId ?: 0

            Log.d(TAG, "assignWorker Id: ${workInfo.id}")

//            if (Constants.workerId == 0 || Constants.workerId != it.workerId) {
//                this.lifecycleScope.launch {
//                    //delay(100L)
//                    if (isFirstLoad) {
//                        Log.d(TAG, "重新获取聊天历史")
                        viewModel.queryChatHistory(Constants.CONSULT_ID)
//                    }
//                }

                updateWorkInf(workInfo)
            //}
        }

        viewModel.mlNewWorkAssigned.observe(viewLifecycleOwner) {
            if (it) {
                sendMsg(tempContent, true)
            }
        }

        viewModel.mHistoryHMessage.observe(viewLifecycleOwner) {
            it?.run {
                //BuildHistory
                if (!this.isEmpty()) {
                    Constants.chatId = this[0].chatId
                }
                var historyList = ArrayList<MessageItem>()
                for (item in this.reversed()) {
                    // sender如果=chatid就是 用户 发的，反之是 客服 或者系统发的
                    var isLeft = true
                    if (item.sender == item.chatId) {
                        isLeft = false
                    }
                    if (item.msgSourceType == CMessage.MsgSourceType.MST_SYSTEM_CUSTOMER) {
                        isLeft = false
                    }

                    if (item.msgOp == "MSG_OP_DELETE") {
                        // 历史里被撤回的消息显示为灰色提示条（对应 Flutter _buildHistory）
                        val tipItem = MessageItem().apply {
                            cMsg = CMessage.Message.newBuilder().apply {
                                msgTime = Utils().getNowTimeStamp()
                                msgId = (item.msgId ?: "0").toLong()
                                val contentBuilder = CMessage.MessageContent.newBuilder().apply {
                                    data = "对方撤回了一条消息"
                                }
                                setContent(contentBuilder)
                            }.build()
                            this.isLeft = isLeft
                            this.cellType = CellType.TYPE_Tip
                            this.msgId = (item.msgId ?: "0").toLong()
                            this.sendStatus = MessageSendState.发送成功
                        }
                        historyList.add(tipItem)
                        continue
                    }

                    if (item.msgFmt == "MSG_VIDEO") {
                        val historyItem = viewModel.composeVideoMsg(item, isLeft)
                        historyItem.cellType = CellType.TYPE_VIDEO
                        historyItem.msgId = (item.msgId ?: "0").toLong()
                        historyList.add(historyItem)
                    } else if (item.msgFmt == "MSG_TEXT") {
                        val historyItem = viewModel.composeTextMsg(item, isLeft)
                        historyItem.msgId = (item.msgId ?: "0").toLong()
                        historyList.add(historyItem)
                    } else if (item.msgFmt == "MSG_IMG") {
                        val historyItem = viewModel.composeImgMsg(item, isLeft)
                        historyItem.msgId = (item.msgId ?: "0").toLong()
                        historyItem.cellType = CellType.TYPE_Image
                        historyList.add(historyItem)
                    } else if (item.msgFmt == "MSG_FILE") {
                        val historyItem = viewModel.composeFileMsg(item, isLeft)
                        historyItem.msgId = (item.msgId ?: "0").toLong()
                        historyItem.cellType = CellType.TYPE_File
                        historyList.add(historyItem)
                    }
                }

                viewModel.mlMsgList.value?.clear()
                viewModel.addAllMsgItem(historyList)

                if (isFirstLoad) {
                    isFirstLoad = false
                    viewModel.composeLocalMsg("您好，${workInfo.workerName}为您服务！", true, false)
                }
                if (viewModel.mlAutoReplyItem.value == null) {
                    viewModel.queryAutoReply(Constants.CONSULT_ID, Constants.workerId)
                }
            }
            mIProgressLoader?.dismissLoading()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onQADisplayedEvent(event: QADisplayedEvent) {
        refreshList()
    }

    //刷新列表，确保每次有新消息都刷新并滚动到底部
    private fun refreshList() {
        if (!isAdded) return // 防止Fragment已分离导致的崩溃

        runOnUiThread {
            try {
                msgAdapter.notifyDataSetChanged()
                binding?.rcvMsg?.post {
                    binding?.rcvMsg?.scrollToPosition(msgAdapter.itemCount - 1)
                    Log.i(TAG, "刷新列表，滚动到底部")
                }
            } catch (e: Exception) {
                Log.e(TAG, "刷新列表失败: ${e.message}")
            }
        }
    }

    private fun updateUploadProgressIfNeeded() {
        if (com.teneasy.sdk.UploadUtil.uploadProgress in 1..95 && (com.teneasy.sdk.UploadUtil.uploadProgress < 67 || com.teneasy.sdk.UploadUtil.uploadProgress >= 69)) {
            com.teneasy.sdk.UploadUtil.uploadProgress += 3
            onUploadProgress(com.teneasy.sdk.UploadUtil.uploadProgress)
        }
    }

    fun getProgressLoader(): IProgressLoader? {
        if (mIProgressLoader == null) {
            mIProgressLoader =
                ProgressDialogLoader(context)
        }
        return mIProgressLoader
    }

    fun toast(string: String) {
        runOnUiThread {
            this.activity?.let {
                Toast.makeText(context, string, Toast.LENGTH_LONG).show()
            }
            Log.e(TAG, string)
        }
    }

    //页面销毁前销毁聊天
    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy")
        MediaPagerActivity.mediaItemsProvider = null
        dismissEvaluationDialog()
        exitChat()
        mIProgressLoader = null
    }

    /**
     * 释放聊天相关资源
     */
    fun exitChat() {
        try {
            releaseResources()
            resetState()
            Log.i(TAG, "聊天资源已释放")
        } catch (e: Exception) {
            Log.e(TAG, "释放聊天资源时发生错误: ${e.message}")
        }

        // 仅在 Fragment 仍处于附加状态时执行返回导航。
        // onDestroy 也会调用 exitChat()，此时 isAdded=false，跳过导航避免把已经返回到的
        // 上级页面也一起 finish 掉。
        if (!isAdded) return
        try {
            // 返回上一页；若已在返回栈底（popBackStack 返回 false，无上级页面可回），
            // 直接结束 Activity，避免「点了返回键没反应」（例如看完全屏图片回来后）。
            if (!findNavController().popBackStack()) {
                activity?.finish()
            }
        } catch (e: Exception) {
            Log.e(TAG, "返回上一页失败，直接结束页面: ${e.message}")
            activity?.finish()
        }
    }

    /**
     * 释放资源
     */
    private fun releaseResources() {
        // GlobalChatManager 会自动管理连接，无需手动断开
    }

    /**
     * 重置状态
     */
    private fun resetState() {
        Constants.workerId = 0
        isFirstLoad = true
        hasSentInSession = false
        evaluationStatus = null
        // 取消活跃监听器
        if (GlobalChatListener.instance.activeListener == this) {
            GlobalChatListener.instance.activeListener = null
        }
        var sViewModel = SelectConsultTypeViewModel();
        sViewModel.markRead()
    }

    //==========图片选择===========//
    // 调用拍照
    private fun showCamera(
        resultCallbackListener: OnResultCallbackListener<LocalMedia>
    ) {
        PictureSelector.create(KeFuFragment@ this)
            .openCamera(SelectMimeType.TYPE_ALL)
            .setRecordVideoMaxSecond(300)
            .forResult(resultCallbackListener)
    }

    // 选择视频（相册仅视频）
    private fun showSelectVideo(resultCallbackListener: OnResultCallbackListener<LocalMedia>) {
        PictureSelector.create(KeFuFragment@ this)
            .openGallery(SelectMimeType.ofVideo())
            .setImageEngine(GlideEngine.createGlideEngine())
            .setMaxSelectNum(1)
            .isDisplayCamera(false)
            .forResult(resultCallbackListener)
    }

    // 选择图片
    private fun showSelectPic(resultCallbackListener: OnResultCallbackListener<LocalMedia>) {

        /*
         .openGallery(PictureMimeType.ofVideo()) // or .ofAll() to support both images/videos
    .setQueryMimeType(PictureMimeType.ofAll()) // optional: to allow multiple formats
    .setCustomMimeType(new ArrayList<>(Arrays.asList("video/mp4", "video/webm"))) // add webm

         */
        PictureSelector.create(KeFuFragment@ this)
            .openGallery(SelectMimeType.TYPE_ALL)
            .setImageEngine(GlideEngine.createGlideEngine())
            .setMaxSelectNum(1)
            .isGif(true)
            .isDisplayCamera(false)
            .forResult(resultCallbackListener)
    }

    /**
     * 根据输入框的内容，发送文本消息。
     * 一般来讲，每发消息就需要跟当前时间做比较，除非设置force = true
     */
    fun sendMsg(txt: String, force: Boolean = false, replyMsgId: Long = 0) {
        if (Constants.chatLib == null) {
            showTip("SDK还未初始化")
            return
        }

        val trimmedText = txt.trim()
        if (trimmedText.isEmpty()) {
            Toast.makeText(requireContext(), "请输入内容", Toast.LENGTH_SHORT).show()
            return
        }

        // 检查会话是否超时，需要分配新客服
        if (!force && lastMsg != null) {
            val lastMsgTime = Date(lastMsg?.msgTime?.seconds ?: 0L)
            val sendingMsgTime = Date()

            if (Utils().sessionTimeout(lastMsgTime, sendingMsgTime, chatExpireTime)) {
                Log.i(TAG, "超过配置的时间，调用分流接口")
                tempContent = trimmedText // 保存消息内容，等待分配新客服后发送
                viewModel.assignNewWorker(Constants.CONSULT_ID)
                return
            }
        }

        // 如果已经发送过消息，不再附带自动回复
        if (lastMsg != null) {
            withAutoReplyU = null
        }

        // 发送消息
        Constants.chatLib?.sendMessage(
            trimmedText,
            CMessage.MessageFormat.MSG_TEXT,
            Constants.CONSULT_ID,
            replyMsgId,
            withAutoReplyU
        )

        // 添加到消息列表
        val messageItem = MessageItem().apply {
            cMsg = Constants.chatLib?.sendingMessage
            isLeft = false
        }
        viewModel.addMsgItem(messageItem, Constants.chatLib?.payloadId ?: 0)
        lastActiveDateTime = Date()
        markSentInSession()
    }

    /**
     * 根据传递的图片地址，发送图片消息。该方法会发送socket消息
     */
    fun sendImgMsg(url: com.teneasy.sdk.Urls) {
        if (Constants.chatLib == null) {
            Toast.makeText(context, "SDK还未初始化", Toast.LENGTH_SHORT).show()
            return
        }

        //说明至少已经发送成功1条消息，无需再附带自动回复消息
        if (lastMsg != null) {
            withAutoReplyU = null
        }

        val ext = url.uri.split(".").last()
        if (imageTypes.contains(ext.lowercase())) {
            Constants.chatLib?.sendMessage(
                url.uri,
                CMessage.MessageFormat.MSG_IMG,
                Constants.CONSULT_ID,
                0,
                withAutoReplyU
            )
        } else if (fileTypes.contains(ext.lowercase())) {
            Constants.chatLib?.sendMessage(
                url.uri,
                CMessage.MessageFormat.MSG_FILE,
                Constants.CONSULT_ID,
                0,
                withAutoReplyU,
                url.fileSize,
                url.fileName
            )
        } else {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "不支持的文件类型", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val messageItem = MessageItem()
        messageItem.cMsg = Constants.chatLib?.sendingMessage
        messageItem.isLeft = false
        viewModel.addMsgItem(messageItem, Constants.chatLib?.payloadId ?: 0)
        lastActiveDateTime = Date()
        markSentInSession()
    }

    fun sendVideoMsg(urls: com.teneasy.sdk.Urls) {
        if (Constants.chatLib == null) {
            Toast.makeText(context, "SDK还未初始化", Toast.LENGTH_SHORT).show()
            return
        }

        //说明至少已经发送成功1条消息，无需再附带自动回复消息
        if (lastMsg != null) {
            withAutoReplyU = null
        }
        //withAutoReplyU参数, 把用户点自动回复的最后一条消息带到客服端，方便客服端显示，仅用户主动发的第一条消息会这样做，其余会被SDK忽略
        Constants.chatLib?.sendVideoMessage(
            urls.uri,
            urls.thumbnailUri,
            urls.hlsUri,
            Constants.CONSULT_ID,
            0,
            withAutoReplyU
        )
        val messageItem = MessageItem()
        messageItem.cMsg = Constants.chatLib?.sendingMessage
        messageItem.isLeft = false
        viewModel.addMsgItem(messageItem, Constants.chatLib?.payloadId ?: 0)
        lastActiveDateTime = Date()
        markSentInSession()
    }

    //聊天sdk连接成功的回调
    override fun connected(c: GGateway.SCHi) {
        afterConnected()
    }

    private fun afterConnected(){
        viewModel.assignWorker(Constants.CONSULT_ID)
        //chatExpireTime = c.chatExpireTime.toInt()
        //showTip("连接成功")


        //检查并重发上次连接未发出去的消息
//        if (unSentMessage[CONSULT_ID] == null || unSentMessage[CONSULT_ID]!!.isEmpty()) {
//            viewModel.getUnSendMsg()
//        }
        //msgAdapter.msgList?.let { Constants.chatLib?.let { it1 -> viewModel.handleUnSendMsg(it, it1) } }
    }

    //聊天sdk里面有什么异常，会从这个回调告诉
    override fun systemMsg(msg: Result) {
        /*
        code: 1010 在别处登录了
code: 1002 无效的Token
code: 1005 会话超时
         */
        Log.i(TAG, msg.msg)
        if (msg.code == 1002 || msg.code == 1010 || msg.code == 1005) {
            if (msg.code == 1002) {
                //showTip("无效的Token")
                //有时候服务器反馈的这个消息不准，可忽略它
                //toast("无效的Token ")
            } else {
                //1010
                //showTip("在别处登录了")

                //1005，会话超时
                toast(msg.msg)
                //禁掉重试机制
                    mIProgressLoader?.dismissLoading()
                    exitChat()
                GlobalChatManager.instance.stopGlobalChat()
                Log.i(TAG, "返回页面")
            }
        } else {
            showTip("")
        }

        //可选：如果断开连接，可以上报日志
        val wssUrl = "wss://" + Constants.domain + "/v1/gateway/h5?"
        //连接SDK时候所使用的参数
        val param =
            Constants.cert + " token:" + Constants.xToken + " x-trace-id" + UUID.randomUUID()
                .toString() + " tenantId:" + Constants.merchantId + " CONSULT_ID:" + Constants.CONSULT_ID + " userid:" + Constants.userId + "custom:" + getCustomParam()
        viewModel.logError(msg.code, param, Constants.xToken, msg.msg, wssUrl)
    }

    //对方删除了消息，会回调这个函数
    override fun msgDeleted(msg: CMessage.Message?, payloadId: Long, msgId: Long, errMsg: String) {
        viewModel.removeMsgItem(payloadId, msg?.msgId ?: 0)
        viewModel.composeLocalMsg("对方撤回了一条消息", true, isTip = true)
    }

    //消息发送出去，收到回执才算成功，并需要改变消息的状态
    override fun msgReceipt(msg: CMessage.Message?, payloadId: Long, msgId: Long, errMsg: String) {
        Log.i(TAG, "收到回执payloadId：${payloadId}")
        val index = viewModel.mlMsgList.value?.indexOfFirst { it.payLoadId == payloadId }
        if (index != null && index != -1) {
            val item = viewModel.mlMsgList.value!![index];
            if ((msg?.replyMsgId ?: 0) > 0) {
                val referMsg =
                    viewModel.mlMsgList.value?.firstOrNull { it.msgId == msg?.replyMsgId }
                if (referMsg != null) {
                    getReply(referMsg.cMsg!!, item, msgId)
                }
            } else {
                item.cMsg = msg
            }
            lastMsg = msg
            item.sendStatus = MessageSendState.发送成功
            //由于消息体是只读的，所以把msgId分配给model，便于从列表里面找记录
            //切记，这里的msgId不是从消息体里面来的，而是从这个函数的第三个参数来的
            item.msgId = msgId
            viewModel.mlMsgList.value?.set(index, item)
            //第一条发出去成功之后，把自动回复设置为null
            withAutoReplyU = null
        }

        Handler(Looper.getMainLooper()).postDelayed(
            {
                refreshList()
            }, 500
        )
        Log.i(TAG, "收到回执：${msg?.content?.data}")
    }

    //收到对方消息
    override fun receivedMsg(msg: CMessage.Message) {
        if (!isAdded) return // 防止Fragment已分离导致的崩溃

        // 注意：全局消息监听和未读数逻辑已在GlobalChatListener中处理

        // 当用户端在当前会话，但是其他咨询类型客服发来了消息，给予提醒
        if (msg.consultId != Constants.CONSULT_ID) {
            handleOtherConsultMessage()
            return
        }

        if (msg.msgSourceType == CMessage.MsgSourceType.MST_SYSTEM_AUTO_TRANSFER) {
            print("这种消息是自动回复的消息，不会计入未读消息")
        }

        // 评价触发消息：匹配 triggerMessages 才弹评价；其他原文不进入聊天列表
        if (msg.msgSourceType == CMessage.MsgSourceType.MST_EVALUATE) {
            handleEvaluateTriggerMessage(msg.content?.data ?: "")
            return
        }

        // 处理当前会话的消息
        processReceivedMessage(msg)
    }

    /**
     * 处理其他咨询类型的消息提醒
     */
    private fun handleOtherConsultMessage() {
        showTip("其他客服有新消息！", 3000)
    }

    /**
     * 处理接收到的消息
     */
    private fun processReceivedMessage(msg: CMessage.Message) {
        // 去重：同一条服务器消息可能因「历史拉取(HTTP)」与「实时推送(WS)」竞态各进一次。
        // 例如用户退出后客服发消息，再次进入时 queryChatHistory 已含该条，WS 又推一遍。
        // 注意：编辑消息(MSG_OP_EDIT)复用原消息的 msgId，必须放行去重以便命中编辑逻辑（对齐 Flutter receivedMsg）。
        if (msg.msgOp != CMessage.MessageOperate.MSG_OP_EDIT &&
            msg.msgId > 0 &&
            viewModel.mlMsgList.value?.any { it.msgId == msg.msgId } == true) {
            Log.i(TAG, "重复消息，跳过 msgId=${msg.msgId}")
            return
        }

        var left = true;
        if (msg.msgSourceType == CMessage.MsgSourceType.MST_SYSTEM_CUSTOMER) {
            left = false;
        }

        // 创建消息项
        val messageItem = MessageItem().apply {
            cMsg = msg
            msgId = msg.msgId
            isLeft = left
            sendStatus = MessageSendState.发送成功
        }

        // 处理回复消息
        if (msg.replyMsgId > 0) {
            processReplyMessage(messageItem, msg)
        } else {
            // 普通消息直接添加
            viewModel.addMsgItem(messageItem, 0)
        }
    }

    /**
     * 处理回复类型的消息
     */
    private fun processReplyMessage(messageItem: MessageItem, msg: CMessage.Message) {
        // 先查找本地消息列表中是否有被回复的消息
        val referMsg = viewModel.mlMsgList.value?.firstOrNull { it.msgId == msg.replyMsgId }

        if (referMsg != null) {
            // 本地有被回复的消息
            getReply(referMsg.cMsg!!, messageItem, msg.msgId)
            viewModel.addMsgItem(messageItem, 0)
        } else {
            // 本地没有被回复的消息，需要从服务器查询
            viewModel.queryMessage(msg.replyMsgId.toString()) { replyMsg ->
                replyMsg?.let {
                    messageItem.replyItem = viewModel.getReplyItem(it)
                    viewModel.addMsgItem(messageItem, 0)
                }
            }
        }
    }

    //客服更换了，需要更换客服信息
    override fun workChanged(msg: GGateway.SCWorkerChanged) {
        workInfo = WorkerInfo()
        workInfo.workerName = msg.workerName
        workInfo.workerAvatar = msg.workerAvatar
        workerAvatar = msg.workerAvatar ?: ""
        Constants.CONSULT_ID = msg.consultId
        Log.d(TAG, "workChanged WorkerId: ${workInfo.id} ${Constants.CONSULT_ID}")
        if (msg.workerId != Constants.workerId) {
            Constants.workerId = msg.workerId
            this.lifecycleScope.launch {
                viewModel.queryChatHistory(Constants.CONSULT_ID)
            }
            runOnUiThread {
                updateWorkInf(workInfo)
            }
        }
    }

    /**
     * 显示提示信息
     * @param msg 提示内容
     * @param duration 显示时长(毫秒)，0表示一直显示
     */
    private fun showTip(msg: String, duration: Long = 3000) {
        if (!isAdded) return // 防止Fragment已分离导致的崩溃

        runOnUiThread {
            binding?.tvTips?.apply {
                text = msg
                visibility = if (msg.isNotEmpty()) View.VISIBLE else View.GONE

                // 如果设置了显示时长，则自动隐藏
                if (duration > 0 && msg.isNotEmpty()) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (isAdded) {
                            binding?.tvTips?.visibility = View.GONE
                        }
                    }, duration)
                }
            }
        }
    }

    /**
     * 隐藏提示信息
     */
    private fun hideTip() {
        if (!isAdded) return

        runOnUiThread {
            binding?.tvTips?.apply {
                visibility = View.GONE
                text = ""
            }
        }
    }

    //更新客服信息
    private fun updateWorkInf(workerInfo: WorkerInfo) {
        binding?.let {
            it.tvTitle.text = "${workerInfo.workerName}"
//            if (!isFirstLoad) {
//                showTip("您好，${workerInfo.workerName}为您服务！")
//            }
            if (Constants.workerId != workInfo.id) {
                showTip("您好，${workerInfo.workerName}为您服务！")
            }
            Constants.workerId = workInfo.id
            // 更新头像
            if (workerInfo.workerAvatar != null && workerInfo.workerAvatar?.isEmpty() == false) {
                val url = Constants.baseUrlImage + workerInfo.workerAvatar
                print("workerAvatar:$url \n")
                Glide.with(requireContext()).load(url)
                    .dontAnimate()
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .into(it.civAuthorImage)
            }
        }
    }

    private fun sendLocalMsg(msg: String, isLeft: Boolean = true) {
        if (Constants.chatLib == null) {
            showTip("SDK还未初始化")
            return
        }
        var chatModel = MessageItem()
        chatModel.cMsg = Constants.chatLib?.composeALocalMessage(msg)
        chatModel.isLeft = isLeft
        chatModel.sendStatus = MessageSendState.发送成功
        viewModel.addMsgItem(chatModel, 0)
    }

    private fun sendLocalImgMsg(imgPath: String, isLeft: Boolean = true) {
        if (Constants.chatLib == null) {
            showTip("SDK还未初始化")
            return
        }

        viewModel.addMsgItem(viewModel.composeImgMsg(null, isLeft, imgPath), 0)
    }

    private fun hidetvQuotedMsg() {
        binding?.llReplyBar?.visibility = View.GONE
        binding?.tvQuotedMsg?.text = ""
        //切记在这需要把tag置为-1
        binding?.tvQuotedMsg?.tag = -1
    }

    private fun showQuotedMsg(txt: String) {
        binding?.llReplyBar?.visibility = View.VISIBLE
        binding?.tvQuotedMsg?.text = txt
    }

    fun getReply(oriMsg: CMessage.Message, model: MessageItem, newMsgId: Long) {
        var replyItem = ReplyMessageItem()
        if (oriMsg != null) {
            if (oriMsg.msgFmt.toString() == "MSG_TEXT") {
                var text = oriMsg.content?.data ?: ""
                if (text.contains("\"imgs\"")) {
                    val gson = Gson()
                    try {
                        val textBody: TextImages = gson.fromJson(text, TextImages::class.java)
                        text = textBody.message
                    } catch (e: Exception) {

                    }
                }
                replyItem.content = text
            } else if (oriMsg.msgFmt.toString() == "MSG_IMG") {
                replyItem.fileName = oriMsg.image?.uri ?: ""
            } else if (oriMsg.msgFmt.toString() == "MSG_VIDEO") {
                replyItem.fileName = oriMsg.video?.uri ?: ""
            } else if (oriMsg.msgFmt.toString() == "MSG_FILE") {
                replyItem.size = (oriMsg?.file?.size ?: 0).toLong()
                replyItem.fileName = oriMsg?.file?.uri ?: ""
            }
        }
        model.replyItem = replyItem
    }

    fun composATextMessage(textMsg: String, msgId: Long, replyMsgId: Long = 0): CMessage.Message {
        //第一层
        var cMsg = CMessage.Message.newBuilder()
        //第二层
        var cMContent = CMessage.MessageContent.newBuilder()

        //d.t = msgDate.time
        cMsg.msgId = msgId
        cMsg.replyMsgId = replyMsgId
        cMsg.msgTime = Utils().getNowTimeStamp()
        cMContent.data = textMsg
        cMsg.setContent(cMContent)

        return cMsg.build()
    }

//    override fun uploadSuccess(urls: Urls, isVideo: Boolean) {
//        uploadProgress = 0
//        if (isVideo) {
//            sendVideoMsg(urls)//Constants.baseUrlImage +
//        } else {
//            // 发送图片或文件
//            sendImgMsg(urls)
//        }
//        runOnUiThread {
//            mIProgressLoader?.updateMessage("")
//            mIProgressLoader?.dismissLoading()
//        }
//    }

    private fun onUploadProgress(progress: Int) {
        Log.i(TAG, "上传中 " + progress.toString() + "%")
        runOnUiThread {
            mIProgressLoader?.updateMessage("上传中 " + progress.toString() + "%")
        }

        // 当进度为1时，启动定时器
        if (progress == 1) {
            startUploadProgressTimer()
        }
    }

    /**
     * 启动上传进度更新定时器
     */
    private fun startUploadProgressTimer() {
        stopUploadProgressTimer() // 先停止现有的定时器

        uploadProgressTimer = Handler(Looper.getMainLooper())
        uploadProgressRunnable = object : Runnable {
            override fun run() {
                updateUploadProgressIfNeeded()
                uploadProgressTimer?.postDelayed(this, 3000) // 每3秒执行一次
            }
        }
        uploadProgressTimer?.postDelayed(uploadProgressRunnable!!, 3000)
    }

    /**
     * 停止上传进度更新定时器
     */
    private fun stopUploadProgressTimer() {
        uploadProgressRunnable?.let {
            uploadProgressTimer?.removeCallbacks(it)
        }
        uploadProgressTimer = null
        uploadProgressRunnable = null
    }

    private fun onUploadSuccess(urls: com.teneasy.sdk.Urls, isVideo: Boolean) {
        stopUploadProgressTimer() // 停止定时器
        com.teneasy.sdk.UploadUtil.uploadProgress = 0
        if (isVideo) {
            sendVideoMsg(urls)
        } else {
            sendImgMsg(urls)
        }
        runOnUiThread {
            mIProgressLoader?.updateMessage("")
            mIProgressLoader?.dismissLoading()
        }
    }

    private fun onUploadFailed(message: String) {
        stopUploadProgressTimer() // 停止定时器
        com.teneasy.sdk.UploadUtil.uploadProgress = 0
        Log.i(TAG, "上传失败：$message")
        runOnUiThread {
            ToastUtils.showToast(requireContext(), message)
            mIProgressLoader?.dismissLoading()
        }
    }

    /**
     * D1：收集当前会话的全部媒体（图片/视频），按时间正序输出，
     * 供 [MediaPagerActivity] 左右滑浏览（对齐 Flutter ChatPage.currentMediaItems）。
     * 覆盖：单图/单视频、文件消息按扩展名识别媒体、多图 JSON 的 imgs 数组、
     * 系统图文消息 TextBody 的 image/video 字段；相对 URL 统一补 baseUrlImage 前缀。
     */
    private fun currentMediaItems(): List<ChatMediaItem> {
        val result = ArrayList<ChatMediaItem>()
        val list = viewModel.mlMsgList.value ?: return result
        val gson = Gson()
        // mlMsgList 已是时间正序（历史倒序翻转后追加实时消息）
        for (item in list) {
            val msg = item.cMsg ?: continue
            if (item.cellType == CellType.TYPE_Tip || item.cellType == CellType.TYPE_QA) continue
            val imgUri = msg.image?.uri ?: ""
            val videoUri = msg.video?.uri ?: ""
            val hlsUri = msg.video?.hlsUri ?: ""
            val fileUri = msg.file?.uri ?: ""
            when {
                imgUri.isNotEmpty() ->
                    result.add(ChatMediaItem(ChatMediaItem.absUrl(imgUri), isVideo = false))
                videoUri.isNotEmpty() || hlsUri.isNotEmpty() -> {
                    val u = if (hlsUri.isNotEmpty()) hlsUri else videoUri
                    result.add(ChatMediaItem(ChatMediaItem.absUrl(u), isVideo = true))
                }
                fileUri.isNotEmpty() -> {
                    val ext = fileUri.substringAfterLast('.', "").lowercase()
                    when {
                        imageTypes.contains(ext) ->
                            result.add(ChatMediaItem(ChatMediaItem.absUrl(fileUri), isVideo = false))
                        videoTypes.contains(ext) ->
                            result.add(ChatMediaItem(ChatMediaItem.absUrl(fileUri), isVideo = true))
                    }
                }
                else -> {
                    val text = msg.content?.data ?: ""
                    if (text.isEmpty()) continue
                    if (text.contains("\"imgs\"")) {
                        // 多图消息：JSON 含 imgs 数组
                        try {
                            val ti = gson.fromJson(text, TextImages::class.java)
                            for (u in ti.imgs) {
                                val ext = u.substringAfterLast('.', "").lowercase()
                                result.add(ChatMediaItem(ChatMediaItem.absUrl(u), videoTypes.contains(ext)))
                            }
                        } catch (_: Exception) {
                        }
                    } else if (msg.msgSourceType == CMessage.MsgSourceType.MST_SYSTEM_CUSTOMER ||
                        msg.msgSourceType == CMessage.MsgSourceType.MST_SYSTEM_WORKER
                    ) {
                        // 系统图文消息：TextBody 带 image / video 字段
                        try {
                            val tb = gson.fromJson(text, TextBody::class.java)
                            val img = tb.image?.trim() ?: ""
                            val vid = tb.video?.trim() ?: ""
                            if (img.isNotEmpty()) {
                                result.add(ChatMediaItem(ChatMediaItem.absUrl(img), isVideo = false))
                            }
                            if (vid.isNotEmpty()) {
                                result.add(ChatMediaItem(ChatMediaItem.absUrl(vid), isVideo = true))
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        }
        return result
    }

    // ============================== 客服评价 ==============================

    /**
     * 进入聊天页时拉取评价配置；若开启则显示浮动按钮
     */
    private fun fetchEvaluationConfig() {
        val request = XHttp.custom().accessToken(false)
        request.headers("X-Token", Constants.xToken)
        request.headers("x-trace-id", UUID.randomUUID().toString())
        request.call(
            request.create(MainApi.IMainTask::class.java).evaluationConfig(JsonObject()),
            object : ProgressLoadingCallBack<ReturnData<EvaluationConfig>>(null) {
                override fun onSuccess(res: ReturnData<EvaluationConfig>) {
                    if (res.code == 0 && res.data?.evaluationEnabled == true) {
                        evaluationConfig = res.data
                        runOnUiThread {
                            if (isAdded) {
                                refreshEvaluationButtonVisibility()
                            }
                        }
                        fetchEvaluationStatus()
                    }
                }

                override fun onError(e: ApiException?) {
                    super.onError(e)
                    Log.w(TAG, "拉取评价配置失败: ${e?.message}")
                }
            }
        )
    }

    /**
     * 应用聊天页主题：整页渐变背景 + 头部着色 + 平台名展示（对齐 Flutter ChatPage build()）。
     */
    private fun applyTheme() {
        val b = binding ?: return
        // A2 整页渐变背景
        b.main.background = chatTheme.newGradientDrawable()
        // A3 导航条着色：背景取 ChatTheme 的顶部渐变色（statusBarColor 已按渐变方向取顶端色），
        // 标题 / 返回键用 tintColor。让导航条与页面顶部渐变无缝衔接。
        b.llTop.setBackgroundColor(chatTheme.statusBarColor)
        // 导航条可点击，独占自身触摸区域，避免其它视图盖到顶栏抢走返回键的点击
        b.llTop.isClickable = true
        // 状态栏与头部同色，按亮度切换图标明暗，整页观感统一
        applyStatusBarColor(chatTheme.statusBarColor)
        b.tvTitle.setTextColor(chatTheme.tintColor)
        b.llClose.setColorFilter(chatTheme.tintColor)
        // 平台 / 商户名（对齐 Flutter AppBar actions）
        val platform = Constants.platformName
        b.tvPlatformName.text = platform
        b.tvPlatformName.setTextColor(chatTheme.tintColor)
        b.tvPlatformName.visibility = if (platform.isEmpty()) View.GONE else View.VISIBLE
        // 设备信息入口已迁入底部功能面板（见 panelMore 的 ll_action_device），头部不再放图标
    }

    /** 打开设备信息页，主题下标透传保持配色一致（对应 Flutter _onDeviceInfoTap）。 */
    private fun openDeviceInfo() {
        val intent = Intent(requireContext(), DeviceInfoActivity::class.java)
        intent.putExtra(DeviceInfoActivity.EXTRA_THEME_INDEX, chatThemeIndex)
        startActivity(intent)
    }

    /** 发送当前输入：处理引用、清空输入框（替代原常驻发送按钮）。 */
    private fun doSend() {
        val et = binding?.etMsg ?: return
        val raw = et.text ?: ""
        if (raw.isEmpty()) return
        val txt = raw.toString()
        var replayMsgId = 0L
        // 这里的 msgId 从列表 model 里拿，不是从消息体
        if (((binding?.tvQuotedMsg?.tag ?: 0) as Int) >= 0) {
            viewModel.mlMsgList.value?.get(
                (binding?.tvQuotedMsg?.tag ?: 0) as Int
            )?.msgId?.let { replayMsgId = it }
        }
        sendMsg(txt.trim(), false, replayMsgId)
        hidetvQuotedMsg()
        et.text?.clear()
    }

    /**
     * 点击引用条时弹出被引用的完整内容（对齐 Flutter _showReplyContentDialog）。
     * 白色圆角弹窗，标题「回复内容」，正文可滚动。
     */
    private fun showReplyContentDialog(text: String) {
        if (!isAdded || text.isEmpty()) return
        val ctx = requireContext()
        val pad = Utils().dp2px(20f)

        val title = android.widget.TextView(ctx).apply {
            this.text = getString(R.string.reply_content_title)
            textSize = 16f
            setTextColor(ctx.getColor(R.color.black))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = android.view.Gravity.CENTER
        }
        val body = android.widget.TextView(ctx).apply {
            this.text = text
            textSize = 14f
            setTextColor(ctx.getColor(R.color.black4848))
            setLineSpacing(0f, 1.5f)
        }
        val scroll = android.widget.ScrollView(ctx).apply {
            val maxH = (resources.displayMetrics.heightPixels * 0.6f).toInt()
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = Utils().dp2px(16f) }
            isFillViewport = false
            addView(body)
            // 限制最大高度
            viewTreeObserver.addOnPreDrawListener(object : android.view.ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (height > maxH) {
                        layoutParams = layoutParams.also { it.height = maxH }
                        requestLayout()
                    }
                    viewTreeObserver.removeOnPreDrawListener(this)
                    return true
                }
            })
        }
        val container = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            addView(title)
            addView(scroll)
        }
        androidx.appcompat.app.AlertDialog.Builder(ctx)
            .setView(container)
            .create()
            .show()
    }

    // 状态栏颜色跟随主题，并按背景亮度切换图标明暗（浅色背景用深色图标）
    private fun applyStatusBarColor(color: Int) {
        val window = activity?.window ?: return
        window.statusBarColor = color
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = ColorUtils.calculateLuminance(color) > 0.5
    }

    /**
     * 评价浮动按钮的显示条件：评价已开启 且 本次会话用户已发过消息
     * （对应 Flutter _hasSentInSession 门控）
     */
    override fun onBottomExpandedChanged(expanded: Boolean) {
        if (bottomExpanded == expanded) return
        bottomExpanded = expanded
        refreshEvaluationButtonVisibility()
    }

    private fun refreshEvaluationButtonVisibility() {
        if (!isAdded) return
        val show = evaluationConfig?.evaluationEnabled == true && hasSentInSession && !bottomExpanded
        binding?.llEvaluationButton?.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            val done = isEvaluationDone
            // 已完成时按钮置灰，对应 Flutter _evaluationDone 配色
            binding?.ivEvaluationStar?.setColorFilter(
                if (done) android.graphics.Color.parseColor("#C7C7C7")
                else chatTheme.tintColor
            )
            binding?.tvEvaluationLabel?.setTextColor(
                android.graphics.Color.parseColor(if (done) "#FF999999" else "#FF333333")
            )
            binding?.llEvaluationButton?.isEnabled = !done
        }
    }

    /**
     * 拉取当前会话的评价状态；1/2 表示已完成，用于按钮置灰禁用（对应 Flutter _fetchEvaluationStatus）
     */
    private fun fetchEvaluationStatus() {
        val request = XHttp.custom().accessToken(false)
        request.headers("X-Token", Constants.xToken)
        request.headers("x-trace-id", UUID.randomUUID().toString())
        val param = JsonObject().apply {
            addProperty("consultId", Constants.CONSULT_ID)
        }
        request.call(
            request.create(MainApi.IMainTask::class.java).evaluationStatus(param),
            object : ProgressLoadingCallBack<ReturnData<EvaluationStatus>>(null) {
                override fun onSuccess(res: ReturnData<EvaluationStatus>) {
                    if (res.code == 0) {
                        evaluationStatus = res.data?.status
                        runOnUiThread {
                            if (isAdded) refreshEvaluationButtonVisibility()
                        }
                    }
                }

                override fun onError(e: ApiException?) {
                    super.onError(e)
                    Log.w(TAG, "拉取评价状态失败: ${e?.message}")
                }
            }
        )
    }

    /**
     * 标记本次会话用户已主动发过消息，并刷新评价按钮显示
     */
    private fun markSentInSession() {
        hasSentInSession = true
        runOnUiThread { refreshEvaluationButtonVisibility() }
    }

    /**
     * 点击浮动 ★ 按钮：弹出评价框（手动场景）
     */
    private fun onEvaluationButtonClick() {
        if (isEvaluationDone) return
        val cfg = evaluationConfig ?: return
        showEvaluationDialog(EvaluationScene.MANUAL, cfg)
    }

    /**
     * 收到触发评价消息：先查 status，0 才弹
     */
    private fun handleEvaluateTriggerMessage(content: String) {
        val cfg = evaluationConfig ?: return
        if (cfg.evaluationEnabled != true) return
        val triggers = cfg.triggerMessages ?: return
        if (content.isBlank() || !triggers.contains(content)) return

        val request = XHttp.custom().accessToken(false)
        request.headers("X-Token", Constants.xToken)
        request.headers("x-trace-id", UUID.randomUUID().toString())
        val param = JsonObject().apply {
            addProperty("consultId", Constants.CONSULT_ID)
        }
        request.call(
            request.create(MainApi.IMainTask::class.java).evaluationStatus(param),
            object : ProgressLoadingCallBack<ReturnData<EvaluationStatus>>(null) {
                override fun onSuccess(res: ReturnData<EvaluationStatus>) {
                    if (res.code == 0 && res.data?.status == 0) {
                        runOnUiThread {
                            if (isAdded) {
                                showEvaluationDialog(EvaluationScene.TRIGGERED, cfg)
                            }
                        }
                    }
                }

                override fun onError(e: ApiException?) {
                    super.onError(e)
                    Log.w(TAG, "查评价状态失败: ${e?.message}")
                }
            }
        )
    }

    private fun showEvaluationDialog(scene: EvaluationScene, cfg: EvaluationConfig) {
        if (evaluationDialog?.isShowing == true) return
        val tintColor = resources.getColor(R.color.blue, null)
        evaluationDialog = EvaluationDialog(
            context = requireContext(),
            scene = scene,
            config = cfg,
            consultId = Constants.CONSULT_ID,
            tintColor = tintColor,
            onStatusChanged = { newStatus ->
                evaluationStatus = newStatus
                runOnUiThread { if (isAdded) refreshEvaluationButtonVisibility() }
            }
        ).also { it.show() }
    }

    private fun dismissEvaluationDialog() {
        try {
            evaluationDialog?.takeIf { it.isShowing }?.dismiss()
        } catch (_: Exception) {}
        evaluationDialog = null
    }
}

