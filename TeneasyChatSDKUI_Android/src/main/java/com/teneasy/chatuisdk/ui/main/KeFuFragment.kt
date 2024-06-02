package com.teneasy.chatuisdk.ui.main;

import android.content.Context.INPUT_METHOD_SERVICE
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.luck.picture.lib.basic.PictureSelector
import com.luck.picture.lib.config.SelectMimeType
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.interfaces.OnResultCallbackListener
import com.luck.picture.lib.thread.PictureThreadUtils.runOnUiThread
import com.luck.picture.lib.utils.ToastUtils
import com.lxj.xpopup.XPopup
import com.teneasy.chatuisdk.BR
import com.teneasy.chatuisdk.R
import com.teneasy.chatuisdk.databinding.FragmentKefuBinding
import com.teneasy.chatuisdk.ui.BigImageView
import com.teneasy.chatuisdk.ui.VideoPlayView
import com.teneasy.chatuisdk.ui.base.Constants
import com.teneasy.chatuisdk.ui.base.GlideEngine
import com.teneasy.chatuisdk.ui.base.PARAM_DOMAIN
import com.teneasy.chatuisdk.ui.base.PARAM_XTOKEN
import com.teneasy.chatuisdk.ui.base.UserPreferences
import com.teneasy.chatuisdk.ui.base.Utils
import com.teneasy.chatuisdk.ui.base.showToast
import com.teneasy.chatuisdk.ui.http.bean.WorkerInfo
import com.teneasy.sdk.ChatLib
import com.teneasy.sdk.Result
import com.teneasy.sdk.TeneasySDKDelegate
import com.teneasy.sdk.ui.CellType
import com.teneasy.sdk.ui.MessageItem
import com.teneasy.sdk.ui.MessageSendState
import com.teneasyChat.api.common.CMessage
import com.teneasyChat.gateway.GGateway
import com.xuexiang.xhttp2.subsciber.ProgressDialogLoader
import com.xuexiang.xhttp2.subsciber.impl.IProgressLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


/**
 * 客服主界面fragment
 */
class KeFuFragment : BaseBindingFragment<FragmentKefuBinding>(), TeneasySDKDelegate {

    //消息列表的Adapter
    private lateinit var msgAdapter: MessageListAdapter
    //聊天页面的ViewModel
    private lateinit var viewModel: KeFuViewModel
    //加载进度框
    private var mIProgressLoader: IProgressLoader? = null
    //设置一个timer, 每隔几秒检查连接状态
    private var reConnectTimer: Timer? = null
    //聊天SDK库
    private var chatLib: ChatLib? = null
    //联结状态标志
    private var isConnected = false
    //自定义日志Tag
    private val TAG = "KefuNchatlib"
    //是否第一次加载页面的标志，历史记录和打招呼只需要页面第一次加载的时候显示
    private var isFirstLoad = true


    private var tempContent = ""
    private var chatExpireTime = 0 //in seconds, chatExpireTime

    private var lastMsg: CMessage.Message? = null
    private var workInfo = WorkerInfo()

    private lateinit var dialogBottomMenu: DialogBottomMenu
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = KeFuViewModel()

        //检查内存变量的域名还存在不
        if (Constants.domain.isEmpty()){
            //域名为空就从Preferences读取
            UserPreferences().getString(PARAM_DOMAIN, Constants.domain)
        }
        //初始化SDK
        initChatSDK(Constants.domain)

        //硬返回按钮点点击之后
       requireActivity().onBackPressedDispatcher.addCallback(this) {
           //断开聊天，停止定时器等
            exitChat()
           //返回到上个页面
            findNavController().popBackStack()
        }

        //初始化进度条
        mIProgressLoader = getProgressLoader()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if(!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        requireActivity().title = "客服"
        hidetvQuotedMsg()

        viewModel.mlAutoReplyItem.observe(viewLifecycleOwner, {
            if (it?.qa?.size?:0 > 0){
                msgAdapter.setAutoReply(it!!)
            }

            var list = ArrayList<MessageItem>()
            //添加自动回复Cell
            var qaItem = MessageItem()
            qaItem.cellType = CellType.TYPE_QA
            list.add(qaItem)
            //viewModel.addAllMsgItem(qaList)

            //添加一个空白Cell，确保列表滚动到最后能看到所有内容
            qaItem = MessageItem()
            qaItem.cellType = CellType.TYPE_LastLine
            list.add(qaItem)

            viewModel.addAllMsgItem(list)

        })

        binding?.ivClose?.setOnClickListener {
            hidetvQuotedMsg()
        }
    }

    //初始化聊天SDK
    private fun initChatSDK(baseUrl: String){
        val wssUrl = "wss://" + baseUrl + "/v1/gateway/h5?"
        Log.i(TAG, "x-token:" + Constants.xToken + "\n" + Date())
        chatLib = ChatLib(Constants.cert , Constants.xToken, wssUrl, Constants.userId, "9zgd9YUc")
        chatLib?.listener = this
        chatLib?.makeConnect()
    }

    override fun onResume() {
        super.onResume()
        //定时检测链接状态
        startTimer()
    }

    override fun onPause() {
        super.onPause()
    }

    // UI初始化
    override fun initView() {
       binding?.lifecycleOwner = this

        binding?.apply {

            this.setVariable(BR.vm, viewModel)

            // 初始化聊天消息列表
            msgAdapter = MessageListAdapter(requireContext(), object : MessageItemOperateListener {
                //长按消息，删除消息的功能，按实际需求，可能不需要
                override fun onDelete(position: Int) {
                   val messageItem = msgAdapter.msgList?.get(position)
                    messageItem?.let {
                        chatLib?.deleteMessage(it.cMsg?.msgId ?: 0)
                        viewModel.removeMsgItem(it)
                    }
                }
                //长按消息，复制文本内容
                override fun onCopy(position: Int) {
                    val messageItem = msgAdapter.msgList?.get(position)
                    val text = messageItem?.cMsg?.content?.data?:""
                    Utils().copyText(text, requireContext())
                }

                //重发发送失败的消息
                override fun onReSend(position: Int) {
                    //chatLib.resendMSg(msg, 0)
                }

                override fun onPlayVideo(url: String) {
                    //val bundle = Bundle()
                    //bundle.putString(ARG_VIDEOURL, url)
                    //findNavController().navigate(R.id.frg_kefu_video_full, bundle)


                    // 单张图片场景
                    XPopup.Builder(requireContext())
                        .asCustom(VideoPlayView(requireContext(), url))
                        .show()
                }

                override fun onPlayImage(url: String) {
                    XPopup.Builder(requireContext())

                        .asCustom(BigImageView(requireContext(), url))
                        .show()
                }

                //长按消息，引用消息并回复
                override fun onQuote(position: Int) {
                    binding?.tvQuotedMsg?.visibility = View.VISIBLE
                    binding?.tvQuotedMsg?.tag = position
                    val msg = msgAdapter.msgList?.get(position)?.cMsg
                    if ((msg?.image?.uri?: "").isNotEmpty()){
                        showQuotedMsg("回复：图片")
                    }else if ((msg?.video?.uri?: "").isNotEmpty()){
                        showQuotedMsg("回复：视频")
                    }else {
                        var txt = msgAdapter.msgList?.get(position)?.cMsg?.content?.data?:" "
                        txt = txt.split("回复：")[0]
                        showQuotedMsg("回复：" + txt)
                    }
                }

                //这里实现自动回复的功能，属于本地消息
                override fun onSendLocalMsg(msg: String, isLeft: Boolean, msgType: String) {

                    if (msgType == "MSG_TEXT") {
                        this@KeFuFragment.sendLocalMsg(msg, isLeft)
                    }else if (msgType == "MSG_IMG") {
                        this@KeFuFragment.sendLocalImgMsg(msg, isLeft)
                    }

                }
            } )
            //初始化一个空的列表给adapter
            msgAdapter.setList(ArrayList())

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
                ) {}

                override fun onTextChanged(
                    s: CharSequence?, start: Int, before: Int,
                    count: Int
                ) {}
            })

            // 点击发送按钮，发送消息
            this.btnSend.setOnClickListener { v: View ->
                binding?.etMsg?.apply {
                    if (this.text.isEmpty()){
                        Utils().closeSoftKeyboard(v)
                    }else{
                        var txt = this.text.toString()
                        if(binding?.tvQuotedMsg?.text.toString().isNotEmpty()){
                            txt = txt + "\n" + binding?.tvQuotedMsg?.text.toString()
                        }
                        sendMsg(txt.trim())
                        hidetvQuotedMsg()
                        binding?.etMsg?.text?.clear()
                    }
                }

            }
            this.etMsg.isFocusable = true
            this.etMsg.isFocusableInTouchMode = true;

            // 发送表情
            this.btnSendExpr.setOnClickListener {
                // 发送表情
                if (viewModel.mlExprIcon.value == R.drawable.h5_biaoqing) {
                    viewModel.mlExprIcon.value = R.drawable.ht_shuru
                    this.etMsg.requestFocus()
                    val inputMethodManager =  requireContext().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.showSoftInput(this.etMsg, InputMethodManager.SHOW_IMPLICIT)
                    this.etMsg.setRawInputType(InputType.TYPE_CLASS_TEXT)
                    this.etMsg.setTextIsSelectable(true)
                } else {
                    viewModel.mlExprIcon.value = R.drawable.h5_biaoqing
                    val inputMethodManager = requireContext().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.showSoftInput(this.etMsg, InputMethodManager.SHOW_IMPLICIT)
                }
            }
            this.btnSendImg.setOnClickListener { v: View ->
                // 发送表情
                dialogBottomMenu.show(v)
            }

            // 底部菜单初始化
            dialogBottomMenu = DialogBottomMenu(context)
                .setItems(resources.getStringArray(R.array.bottom_menu))
                .setOnItemClickListener(AdapterView.OnItemClickListener{ adapterView, view, i, l ->
                    when (i) {
                        0 -> {
                            // 选择相册
                            showSelectPic(object : OnResultCallbackListener<LocalMedia> {
                                override fun onResult(result: java.util.ArrayList<LocalMedia>) {
                                    if(result != null && result.size > 0) {
                                        val item = result[0]
                                        dialogBottomMenu.dismiss()
                                        // 上传图片之前，首先在聊天框添加一个图片消息，更新聊天界面
                                        //val id = viewModel.composeAChatmodelImg(item.path, false)
                                        uploadImg(item.realPath)
                                    }
                                }
                                override fun onCancel() {}
                            })
                        }
                        1 -> {
                            // 拍照
                            showCamera(object : OnResultCallbackListener<LocalMedia> {
                                override fun onResult(result: java.util.ArrayList<LocalMedia>) {
                                    if(result != null && result.size > 0) {
                                        val item = result[0]
                                        dialogBottomMenu.dismiss()
                                        //val id = viewModel.composeAChatmodelImg(item.path, false)
                                        uploadImg(item.realPath)
                                    }
                                }

                                override fun onCancel() {}
                            })
                        }
                        else -> {
                            dialogBottomMenu.dismiss()
                        }
                    }
                })
                .build()


            this.tvTips.visibility = View.GONE
            initObserver()

            this.llClose.setOnClickListener {
                exitChat()
                findNavController().popBackStack()
            }

           /* this.root.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    Utils().closeSoftKeyboard(v)
                }
                true
            }*/
        }
    }

    private fun initObserver(){
        viewModel.mlMsgList.observe(viewLifecycleOwner) {
            msgAdapter.setList(it)
            refreshList()
        }

        viewModel.mlAssignWorker.observe(viewLifecycleOwner){
            if (it == null){
                Log.d(TAG, "assignWorker 失败: null")
                return@observe
            }
                workInfo.workerName = it.nick
                workInfo.workerAvatar = it.avatar
                workInfo.id = it.workerId ?: 0

                Log.d(TAG, "assignWorker Id: ${workInfo.id}")

            if (Constants.workerId == 0 || Constants.workerId != it.workerId) {
                this.lifecycleScope.launch {
                    //delay(100L)
                    if(isFirstLoad) {
                        isFirstLoad = false
                        viewModel.queryChatHistory(Constants.CONSULT_ID)
                    }
                }
                Constants.workerId = workInfo.id
                updateWorkInf(workInfo)
            }

            //}
        }

        viewModel.mlNewWorkAssigned.observe(viewLifecycleOwner){
            if (it){
                sendMsg(tempContent, true)
            }
        }

        viewModel.mHistoryList.observe(viewLifecycleOwner){
           it?.run {
               //BuildHistory
               var historyList = ArrayList<MessageItem>()
               for (item in this.reversed()) {
                   // sender如果=chatid就是 用户 发的，反之是 客服 或者系统发的
                   var isLeft = true
                   if (item.sender == item.chatId){
                       isLeft = false
                   }

                   if (item.msgOp == "MSG_OP_DELETE"){
                       continue
                   }

                   if (item.workerChanged != null){
                       val historyItem =  viewModel.composeTextMsg(item, isLeft)
                       historyItem.cellType = CellType.TYPE_Tip
                       historyList.add(historyItem)
                   }
                   else if (item.msgFmt == "MSG_VIDEO"){
                       val historyItem =  viewModel.composeVideoMsg(item, isLeft)
                       historyItem.cellType = CellType.TYPE_VIDEO
                       historyList.add(historyItem)
                   }
                   else if(item.msgFmt == "MSG_TEXT") {
                       val historyItem =  viewModel.composeTextMsg(item, isLeft)
                       historyList.add(historyItem)
                   }else if(item.msgFmt == "MSG_IMG") {
                       val historyItem = viewModel.composeImgMsg(item, isLeft)
                       historyItem.cellType = CellType.TYPE_Image
                       historyList.add(historyItem)
                   }
               }

               viewModel.mlMsgList.value?.clear()
               viewModel.addAllMsgItem(historyList)

               if (isFirstLoad){
                   viewModel.composeLocalMsg("您好，${workInfo.workerName}为您服务！", true, false)
               }else{
                   viewModel.composeLocalMsg("您好，${workInfo.workerName}为您服务！", false, true)
               }
               if (viewModel.mlAutoReplyItem.value == null){
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
    private fun refreshList(){
        runOnUiThread {
            msgAdapter.notifyDataSetChanged()
            binding?.let {
                //it.rcvMsg.scrollToBottomWithMargin(100)
                it.rcvMsg.scrollToPosition(msgAdapter.itemCount - 1)
            }
        }
        Log.i(TAG, "刷新列表")
    }

    //开一个定时器每隔几秒检查连接状态
    private fun startTimer() {
        if(isConnected) {
           return
        }
        if (!isFirstLoad) {
            showTip("正在重新连接...")
            Log.i(TAG, "正在重新连接...")
        }
        Constants.domain = UserPreferences().getString(PARAM_DOMAIN, Constants.domain)
        if(reConnectTimer == null) {
            reConnectTimer = Timer()
            reConnectTimer?.schedule(object : TimerTask() {
                override fun run() {
                    if (chatLib == null || !isConnected) {
                        Log.d(TAG, "SDK 重新初始化")
                        //runOnUiThread {
                            initChatSDK(Constants.domain)
                       // }
                        /*
                        if (chatLib == null) {
                            Log.d(TAG, "SDK 重新初始化")
                            initChatSDK(Constants.domain)
                        }else{
                            Log.d(TAG, "SDK 重新连接")
                            chatLib?.reConnect()
                        }*/
                    }
                }
            }, 3000, 3000) //这里必须Delay 3s及以上，给初始化SDK足够的时间
        }
    }

    // 关闭连接状态定时器
    private fun closeTimer() {
        if(reConnectTimer != null) {
            reConnectTimer?.cancel()
            reConnectTimer = null
        }
    }

    fun getProgressLoader(): IProgressLoader? {
        if (mIProgressLoader == null) {
            mIProgressLoader =
                ProgressDialogLoader(context)
        }
        return mIProgressLoader
    }

    /**
     * 上传图片。上传成功后，会直接调用socket进行消息发送。
     *  @param filePath
     *  // 文件类型类型 0 ～ 4
     * enum AssetKind {
     *   ASSET_KIND_NONE = 0;
     *   // 商户公共文件
     *   ASSET_KIND_PUBLIC = 1;
     *   // 商户私有文件
     *   ASSET_KIND_PRIVATE = 2;
     *   // 头像
     *   ASSET_KIND_AVATAR = 3;
     *   // 会话私有文件
     *   ASSET_KIND_SESSION = 4;
     * }
     */
    //这个函数可以上传图片和视频
    fun uploadImg(filePath: String) {
        mIProgressLoader?.updateMessage("上传中。。。")
        mIProgressLoader?.showLoading()

        //val filePath = Utils().encodeFilePath(mediaPath)
        var file = File(filePath)

        if (!file.exists()){
            ToastUtils.showToast(requireContext(), "文件不存在")
            mIProgressLoader?.dismissLoading()
            return
        }

        if ((filePath.contains(".png") || filePath.contains(".jpg"))){
            //图片压缩
           // Step 1: Load the image
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            // Step 2: Compress the image
            val compressedData = Utils().compressBitmap(bitmap, 50)
            var extenion = file.absoluteFile.extension
            var newFilePath = file.absolutePath.replace("." + extenion,"") + Date().time + "." + extenion
            var newFile = File(newFilePath)
            // Step 3: Save the compressed image to a file
            Utils().saveCompressedBitmapToFile(compressedData, newFile)
            if (newFile.exists()){
                file = newFile
            }else{
                 //toast("压缩失败")
            }

            if (file.length() > 20 * 1024 * 1024){
                ToastUtils.showToast(requireContext(), "图片限制20M")
                mIProgressLoader?.dismissLoading()
                return
            }
        }else{
            if (file.length() > 300 * 1000 * 1000){
                ToastUtils.showToast(requireContext(), "视频/文件限制300M")
                mIProgressLoader?.dismissLoading()
                return
            }
        }
        Thread(Runnable {
            kotlin.run {
                val multipartBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("myFile", file.name,  RequestBody.create("multipart/form-data".toMediaTypeOrNull(), file))
                    .addFormDataPart("type", "4")
                    .build()

                val request2 = Request.Builder().url(Constants.baseUrlApi() + "/v1/assets/upload/")
                    .addHeader("X-Token", Constants.xToken)
                    .post(multipartBody).build()


                val okHttpClient: OkHttpClient = OkHttpClient().newBuilder()
                    .connectTimeout(50, TimeUnit.SECONDS)
                    .writeTimeout(5, TimeUnit.MINUTES)
                    .readTimeout(5, TimeUnit.MINUTES)
                    .build()
                val call = okHttpClient.newCall(request2)
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        // 上传失败
                        toast("上传失败" + e.localizedMessage)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        mIProgressLoader?.dismissLoading()
                        val body = response.body
                        if(response.code == 200 && body != null) {
                            val path = response.body!!.string()
                            if (path.length < 100) {
                                if (path.contains(".png") || path.contains(".jpg") || path.contains(
                                        ".jpge"
                                    )
                                ) {
                                    // 发送图片
                                    sendImgMsg(path)//Constants.baseUrlImage +
                                } else {
                                    sendVideoMsg(path)//Constants.baseUrlImage +
                                }
                            }else{
                                toast("上传失败，服务器返回无效路径")
                            }
                        } else {
                            toast("上传失败 Code:" + response.code)
                        }
                        //Utils().closeSoftKeyboard(view)
                    }
                })

            }
        }).start()
    }

    fun toast(string: String){
        runOnUiThread {
            showToast(string);
            Log.e(TAG, string)
            mIProgressLoader?.dismissLoading()
        }
    }

    //页面销毁前销毁聊天
    override fun onDestroy() {
        exitChat()
        super.onDestroy()
    }

    //释放聊天相关库和变量
    fun exitChat(){
        closeTimer()
        Constants.workerId = 0
        chatLib?.disConnect()
        chatLib = null
        Log.i(TAG, "销毁聊天")
    }

    override fun onCreateViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): FragmentKefuBinding {
        return FragmentKefuBinding.inflate(layoutInflater, parent, false)
    }

    //==========图片选择===========//
    // 调用拍照
    private fun showCamera(resultCallbackListener: OnResultCallbackListener<LocalMedia>
    ) {
        PictureSelector.create(KeFuFragment@this)
            .openCamera(SelectMimeType.TYPE_ALL)
            .forResult(resultCallbackListener)
    }

    // 选择图片
    private fun showSelectPic(resultCallbackListener: OnResultCallbackListener<LocalMedia>) {
        PictureSelector.create(KeFuFragment@this)
            .openGallery(SelectMimeType.TYPE_ALL)
            .setImageEngine(GlideEngine.createGlideEngine())
            .setMaxSelectNum(1)
            .isDisplayCamera(false)
            .forResult(resultCallbackListener)
    }

    /**
     * 根据输入框的内容，发送文本消息。
     * 一般来讲，每发消息就需要跟当前时间做比较，除非设置force = true
     */
    fun sendMsg(txt: String, force: Boolean = false) {
        if(chatLib == null){
            // Toast.makeText(context, "SDK还未初始化", Toast.LENGTH_SHORT).show()
            showTip("SDK还未初始化")
            return
        }
         /*
            用户发送消息，要先比对上一条时间 ，超过配置的时间就分配新客服
         */
        if(!force && lastMsg != null) {
            //tempContent = txt
            val lastMsgTime = Date(lastMsg?.msgTime?.seconds?: 0L)
            val sendingMsgTime = Date(Date().time)

            val diffTime = Utils().sessionTimeout(lastMsgTime, sendingMsgTime, chatExpireTime)
            if (diffTime) {
                Log.i(TAG, "超过配置的时间，调用分流接口")
                viewModel.assignNewWorker(Constants.CONSULT_ID)
                return
            }

        }
        chatLib?.sendMessage(txt, CMessage.MessageFormat.MSG_TEXT, Constants.CONSULT_ID)
        var messageItem = MessageItem()
        messageItem.cMsg = chatLib?.sendingMessage
        messageItem.isLeft = false
        viewModel.addMsgItem(messageItem, chatLib?.payloadId ?: 0)
    }

    /**
     * 根据传递的图片地址，发送图片消息。该方法会发送socket消息
     */
    fun sendImgMsg(url: String) {
        if(chatLib == null){
            Toast.makeText(context, "SDK还未初始化", Toast.LENGTH_SHORT).show()
            return
        }
        chatLib?.sendMessage(url, CMessage.MessageFormat.MSG_IMG, Constants.CONSULT_ID)
        val messageItem = MessageItem()
        messageItem.cMsg = chatLib?.sendingMessage
        messageItem.isLeft = false
        viewModel.addMsgItem(messageItem, chatLib?.payloadId ?: 0)
    }

    fun sendVideoMsg(url: String) {
        if(chatLib == null){
            Toast.makeText(context, "SDK还未初始化", Toast.LENGTH_SHORT).show()
            return
        }
        chatLib?.sendMessage(url, CMessage.MessageFormat.MSG_VIDEO, Constants.CONSULT_ID)
        val messageItem = MessageItem()
        messageItem.cMsg = chatLib?.sendingMessage
        messageItem.isLeft = false
        viewModel.addMsgItem(messageItem, chatLib?.payloadId ?: 0)
    }

    //聊天sdk连接成功的回调
    override fun connected(c: GGateway.SCHi) {
        //把连接状态放到当前页面
        isConnected = true;
        println(c.id)
        showTip("连接成功")
        Log.i(TAG, "连接成功, xToekn:" + c.token)
        UserPreferences().putString(PARAM_XTOKEN, c.token)
        Constants.xToken = c.token
        viewModel.assignWorker(Constants.CONSULT_ID)
       chatExpireTime = c.chatExpireTime.toInt()
    }

    //聊天sdk里面有什么异常，会从这个回调告诉
    override fun systemMsg(msg: Result) {
        /*
        code: 1010 在别处登录了
code: 1002 无效的Token
         */
        if (msg.code >= 1000 && msg.code <= 1010) {
            isConnected = false
            if (msg.code == 1002){
                //showTip("无效的Token")
                toast("无效的Token")
            }else if (msg.code == 1010){
                //showTip("在别处登录了")
                toast("在别处登录了")
            }
            //禁掉重试机制
            runOnUiThread{
                exitChat()
                //返回到上个页面
                findNavController().popBackStack()
            }
        }else{
            showTip("")
        }
        //按实际需要，显示错误提示，也可以不显示
        //showTip(msg.msg)
        //if (BuildConfig){
          //  showTip(msg.msg)
//        }else {
//        }
        Log.i(TAG, msg.msg)
    }

    //对方删除了消息，会回调这个函数
    override fun msgDeleted(msg: CMessage.Message, payloadId: Long, msgId: Long, errMsg: String) {
        viewModel.removeMsgItem(payloadId, msg.msgId)
        viewModel.composeLocalMsg("对方撤回了一条消息", true, isTip = true)
    }

    //消息发送出去，收到回执才算成功，并需要改变消息的状态
    override fun msgReceipt(msg: CMessage.Message, payloadId: Long, msgId: Long, errMsg: String) {
        val item = viewModel.mlMsgList.value?.find { it.payLoadId == payloadId }
        if(item != null) {
            item.sendStatus = MessageSendState.发送成功
            item.msgId = msgId
        }
        lastMsg = msg
        refreshList()
        Log.i(TAG, "收到回执：${msg.content.data}")
        //hideTip()
    }

    //收到对方消息
    override fun receivedMsg(msg: CMessage.Message) {
        //当用户端在当前会话，但是其他咨询类型客服发来了消息，给予提醒。
        if (msg.consultId != Constants.CONSULT_ID){
            //showTip("其他客服有新消息！")
            runOnUiThread(
                Runnable {
                    Toast.makeText(context, "其他客服有新消息！", Toast.LENGTH_SHORT).show()
                }
            )
        }else {
            //把收到的消息插入到列表
            val messageItem = MessageItem()
            messageItem.cMsg = msg
            messageItem.isLeft = true
            viewModel.addMsgItem(messageItem, 0)
        }
    }

    //客服更换了，需要更换客服信息
    override fun workChanged(msg: GGateway.SCWorkerChanged) {
         workInfo = WorkerInfo()
        workInfo.workerName = msg.workerName
        workInfo.workerAvatar = msg.workerAvatar
        Constants.CONSULT_ID = msg.consultId
        Log.d(TAG, "workChanged WorkerId: ${workInfo.id}")
        if (msg.workerId != Constants.workerId){
            Constants.workerId = msg.workerId
            this.lifecycleScope.launch {
                    viewModel.queryChatHistory(Constants.CONSULT_ID)
            }
            runOnUiThread {
                updateWorkInf(workInfo)
            }
        }
    }

    private fun showTip(msg: String){
        runOnUiThread {
            binding?.tvTips?.visibility = View.VISIBLE
            binding?.tvTips?.text = msg
        }
    }

    private fun hideTip(){
        runOnUiThread {
            binding?.tvTips?.visibility = View.GONE
            binding?.tvTips?.text = ""
        }
    }

    //更新客服信息
    private fun updateWorkInf(workerInfo: WorkerInfo){
            binding?.let {
                it.tvTitle.text = "${workerInfo.workerName}"
                //showTip("您好，${workerInfo.workerName}为您服务！")
                /*if (isFirstLoad){
                    viewModel.composeLocalMsg("您好，${workerInfo.workerName}为您服务！", true, false)
                }else{
                    viewModel.composeLocalMsg("您好，${workerInfo.workerName}为您服务！", false, true)
                }*/
                // 更新头像
                if (workerInfo.workerAvatar != null && workerInfo.workerAvatar?.isEmpty() == false) {
                    val url = Constants.baseUrlImage + workerInfo.workerAvatar
                    print("workerAvatar:$url \n")
                    Glide.with(it.civAuthorImage).load(url)
                        .dontAnimate()
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .into(it.civAuthorImage)
                }
            }
    }

    private fun sendLocalMsg(msg: String, isLeft: Boolean = true){
        if (chatLib == null){
            showTip("SDK还未初始化")
            return
        }
        var chatModel = MessageItem()
        chatModel.cMsg = chatLib?.composeALocalMessage(msg)
        chatModel.isLeft = isLeft
        chatModel.sendStatus = MessageSendState.发送成功
        viewModel.addMsgItem(chatModel, 0)
    }

    private fun sendLocalImgMsg(imgPath: String, isLeft: Boolean = true){
        if (chatLib == null){
            showTip("SDK还未初始化")
            return
        }

        viewModel.addMsgItem(viewModel.composeImgMsg(null, isLeft, imgPath), 0)
    }

    private fun hidetvQuotedMsg(){
        binding?.tvQuotedMsg?.visibility = View.GONE
        binding?.tvQuotedMsg?.text = ""
    }

    private fun showQuotedMsg(txt: String){
        binding?.tvQuotedMsg?.visibility = View.VISIBLE
        binding?.tvQuotedMsg?.text = txt
    }
}