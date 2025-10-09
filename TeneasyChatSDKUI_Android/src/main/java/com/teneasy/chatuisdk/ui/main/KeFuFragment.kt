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
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import com.teneasy.chatuisdk.ARG_VIDEOURL
import com.teneasy.chatuisdk.BR
import com.teneasy.chatuisdk.FullImageActivity
import com.teneasy.chatuisdk.FullVideoActivity
import com.teneasy.chatuisdk.R
import com.teneasy.chatuisdk.WebViewActivity
import com.teneasy.chatuisdk.ui.base.Constants
import com.teneasy.chatuisdk.ui.base.Constants.Companion.CONSULT_ID
import com.teneasy.chatuisdk.ui.base.Constants.Companion.baseUrlApi
import com.teneasy.chatuisdk.ui.base.Constants.Companion.chatLib
import com.teneasy.chatuisdk.ui.base.Constants.Companion.domain
import com.teneasy.chatuisdk.ui.base.Constants.Companion.fileTypes
import com.teneasy.chatuisdk.ui.base.Constants.Companion.getCustomParam
import com.teneasy.chatuisdk.ui.base.Constants.Companion.imageTypes
import com.teneasy.chatuisdk.ui.base.Constants.Companion.unSentMessage
import com.teneasy.chatuisdk.ui.base.Constants.Companion.videoTypes
import com.teneasy.chatuisdk.ui.base.Constants.Companion.withAutoReplyU
import com.teneasy.chatuisdk.ui.base.Constants.Companion.workerAvatar
import com.teneasy.chatuisdk.ui.base.Constants.Companion.xToken
import com.teneasy.chatuisdk.ui.base.GlobalChatListener
import com.teneasy.chatuisdk.ui.base.GlobalChatManager
import com.teneasy.chatuisdk.ui.base.GlideEngine
import com.teneasy.chatuisdk.ui.base.PARAM_DOMAIN
import com.teneasy.chatuisdk.ui.base.PARAM_XTOKEN
import com.teneasy.chatuisdk.ui.base.UserPreferences
import com.teneasy.chatuisdk.ui.base.Utils
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
    }

    // UI组件
    private lateinit var msgAdapter: MessageListAdapter
    private lateinit var viewModel: KeFuViewModel
    private lateinit var dialogBottomMenu: DialogBottomMenu
    private lateinit var pickFileLauncher: ActivityResultLauncher<Intent>
    private lateinit var uploadHandler: ChatUploadHandler

    // 状态变量
    private var mIProgressLoader: IProgressLoader? = null
    private var isConnected = false
    private var isFirstLoad = true
    private var tempContent = ""
    private var chatExpireTime = 0 // 会话过期时间（秒）

    // 聊天相关数据
    private var lastMsg: CMessage.Message? = null
    private var workInfo = WorkerInfo()
    private var lastActiveDateTime = Date()

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
            findNavController().popBackStack()
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

        binding?.ivFile?.setOnClickListener {
            openFilePicker()
        }
    }


    override fun onResume() {
        super.onResume()
        // 设置当前Fragment为活跃监听器
        GlobalChatListener.instance.activeListener = this

        // 设置当前聊天的consultId
        Constants.currentChatConsultId = Constants.CONSULT_ID
        // 清零当前会话的未读数
        Constants.clearUnreadCount(Constants.CONSULT_ID)
        // 通知全局消息委托，未读数已更新
        Constants.globalMessageDelegate?.onMessageReceived(Constants.CONSULT_ID)


            updateWorkerNameIfAvailable()

    }

    override fun onPause() {
        super.onPause()
        // 取消活跃监听器
        if (GlobalChatListener.instance.activeListener == this) {
            GlobalChatListener.instance.activeListener = null
        }

        // 离开聊天页面时，重置当前聊天consultId
        Constants.currentChatConsultId = 0

        viewModel.apply {
            getUnSendMsg()
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
                        messageItem?.cMsg?.msgSourceType ?: CMessage.MsgSourceType.MST_SYSTEM_WORKER
                    if (text.contains("\"color\"")) {
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
                    val intent = Intent(requireContext(), FullVideoActivity::class.java)
                    intent.putExtra(ARG_VIDEOURL, url)
                    intent.putExtra(ARG_KEFUNAME, workInfo.workerName)
                    intent.setClass(requireContext(), FullVideoActivity::class.java)
                    requireActivity().startActivity(intent)
                }

                override fun onOpenFile(url: String) {
                    //Utils().openPdfInBrowser(requireContext(), Uri.parse(url))
                    //onPlayImage(url)
                    var ext = url?.split(".")?.last() ?: ""
                    if (ext.lowercase() == "pdf") {
                        //val pdfUrl = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf"
                        Utils().openPdfFile(requireContext(), url, "示例PDF文档")
                        return
                    } else if (ext.lowercase() == "pdf") {
                        //googleDocsUrl = "https://docs.google.com/gview?embedded=true&url=$imageUrl"
                        ToastUtils.showToast(
                            requireActivity(),
                            "暂不支持在线查看PDF和CSV文件，但您可以下载后再浏览，也确保您的设备里有查看PDF和CSV文件的应用程序"
                        )
                        return
                    }

                    val intent = Intent(requireContext(), WebViewActivity::class.java)
                    intent.putExtra(ARG_IMAGEURL, url)
                    intent.putExtra(ARG_KEFUNAME, workInfo.workerName)
                    intent.setClass(requireContext(), WebViewActivity::class.java)
                    requireActivity().startActivity(intent)
                }

                override fun onPlayImage(url: String) {
                    val intent = Intent(requireContext(), FullImageActivity::class.java)
                    intent.putExtra(ARG_IMAGEURL, url)
                    intent.putExtra(ARG_KEFUNAME, workInfo.workerName)
                    intent.setClass(requireContext(), FullImageActivity::class.java)
                    requireActivity().startActivity(intent)
                }

                //长按消息，引用消息并回复
                override fun onQuote(position: Int) {
                    binding?.tvQuotedMsg?.visibility = View.VISIBLE
                    binding?.tvQuotedMsg?.tag = position
                    val msg = msgAdapter.msgList?.get(position)?.cMsg
                    val srcType = msg?.msgSourceType ?: CMessage.MsgSourceType.MST_SYSTEM_WORKER
                    if ((msg?.image?.uri ?: "").isNotEmpty()) {
                        showQuotedMsg("回复：图片")
                    } else if ((msg?.file?.uri ?: "").isNotEmpty()) {
                        showQuotedMsg("回复：文件")
                    } else if ((msg?.video?.uri ?: "").isNotEmpty()) {
                        showQuotedMsg("回复：视频")
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
                                    "下载成功！"
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

                //这里实现自动回复的功能，属于本地消息
                override fun onSendLocalMsg(msg: String, isLeft: Boolean, msgType: String) {
                    if (msgType == "MSG_TEXT") {
                        this@KeFuFragment.sendLocalMsg(msg, isLeft)
                    } else if (msgType == "MSG_IMG") {
                        this@KeFuFragment.sendLocalImgMsg(msg, isLeft)
                    }
                }
            })

            //初始化一个空的列表给adapter
            if (viewModel.mlMsgList.value?.isEmpty() == true) {
                msgAdapter.setList(ArrayList())
            }

            //设置recycleView的LayoutManager
            val layoutManager = LinearLayoutManager(context)
            layoutManager.orientation = LinearLayoutManager.VERTICAL
            this.rcvMsg.layoutManager = layoutManager
            this.rcvMsg.adapter = msgAdapter

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
                    binding?.tvCount?.text =
                        binding?.etMsg?.text.toString().length.toString() + "/500"
                }
            })

            // 点击发送按钮，发送消息
            this.btnSend.setOnClickListener { v: View ->
                binding?.etMsg?.apply {
                    if ((this.text ?: "").isEmpty()) {
                        Utils().closeSoftKeyboard(v)
                    } else {
                        val txt = this.text.toString()
                        var replayMsgId = 0L
                        //这里的msgId是从列表的model里面拿，不是从消息体
                        if (((binding?.tvQuotedMsg?.tag ?: 0) as Int) >= 0) {
                            viewModel.mlMsgList.value?.get(
                                (binding?.tvQuotedMsg?.tag ?: 0) as Int
                            )?.msgId?.let {
                                replayMsgId = it;
                            }
                        }
                        sendMsg(txt.trim(), false, replayMsgId)
                        hidetvQuotedMsg()
                        binding?.etMsg?.text?.clear()
                    }
                }
            }
            this.etMsg.isFocusable = true
            this.etMsg.isFocusableInTouchMode = true;

            this.ivPhoto.setOnClickListener { v: View ->
                selectImageOrVideo(0)
            }
            this.ivVideo.setOnClickListener { v: View ->
                selectImageOrVideo(1)
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
                findNavController().popBackStack()
            }
        }

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

            if (Constants.workerId == 0 || Constants.workerId != it.workerId) {
                this.lifecycleScope.launch {
                    //delay(100L)
                    if (isFirstLoad) {
                        Log.d(TAG, "重新获取聊天历史")
                        viewModel.queryChatHistory(Constants.CONSULT_ID)
                    }
                }
                Constants.workerId = workInfo.id
                updateWorkInf(workInfo)
            }
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
                    if (item.sender == item.chatId || item.msgSourceType == CMessage.MsgSourceType.MST_SYSTEM_WORKER) {
                        isLeft = false
                    }
                    if (item.msgSourceType == CMessage.MsgSourceType.MST_SYSTEM_CUSTOMER) {
                        isLeft = true
                    }

                    if (item.msgOp == "MSG_OP_DELETE") {
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
        isConnected = false
        isFirstLoad = true
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
    }

    //聊天sdk连接成功的回调
    override fun connected(c: GGateway.SCHi) {
        afterConnected()
    }

    private fun afterConnected(){
        //把连接状态放到当前页面
        isConnected = true;
        viewModel.assignWorker(Constants.CONSULT_ID)
        //chatExpireTime = c.chatExpireTime.toInt()
        showTip("连接成功")


        //检查并重发上次连接未发出去的消息
        if (unSentMessage[CONSULT_ID] == null || unSentMessage[CONSULT_ID]!!.isEmpty()) {
            viewModel.getUnSendMsg()
        }
        msgAdapter.msgList?.let { Constants.chatLib?.let { it1 -> viewModel.handleUnSendMsg(it, it1) } }
    }

    //聊天sdk里面有什么异常，会从这个回调告诉
    override fun systemMsg(msg: Result) {
        /*
        code: 1010 在别处登录了
code: 1002 无效的Token
code: 1005 会话超时
         */
        Log.i(TAG, msg.msg)
        if (msg.code in 1000..1010) {
            isConnected = false
        }
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
                runOnUiThread {
                    mIProgressLoader?.dismissLoading()
                    exitChat()
                    //返回到上个页面
                    findNavController().popBackStack()
                }
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
        var left = true;
        if (msg.msgSourceType == CMessage.MsgSourceType.MST_SYSTEM_WORKER) {
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
    private fun showTip(msg: String, duration: Long = 0) {
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
            if (!isFirstLoad) {
                showTip("您好，${workerInfo.workerName}为您服务！")
            }

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
        binding?.tvQuotedMsg?.visibility = View.GONE
        binding?.tvQuotedMsg?.text = ""
        //切记在这需要把tag置为-1
        binding?.tvQuotedMsg?.tag = -1
    }

    private fun showQuotedMsg(txt: String) {
        binding?.tvQuotedMsg?.visibility = View.VISIBLE
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
    }

    private fun onUploadSuccess(urls: com.teneasy.sdk.Urls, isVideo: Boolean) {
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
        com.teneasy.sdk.UploadUtil.uploadProgress = 0
        Log.i(TAG, "上传失败：$message")
        runOnUiThread {
            ToastUtils.showToast(requireContext(), message)
            mIProgressLoader?.dismissLoading()
        }
    }
}

