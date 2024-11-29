package com.teneasy.chatuisdk.ui.main;

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.gson.Gson
import com.google.protobuf.Timestamp
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
import com.teneasy.chatuisdk.UploadResult
import com.teneasy.chatuisdk.ui.base.Constants
import com.teneasy.chatuisdk.ui.base.Constants.Companion.CONSULT_ID
import com.teneasy.chatuisdk.ui.base.Constants.Companion.getCustomParam
import com.teneasy.chatuisdk.ui.base.Constants.Companion.unSentMessage
import com.teneasy.chatuisdk.ui.base.Constants.Companion.withAutoReplyU
import com.teneasy.chatuisdk.ui.base.Constants.Companion.workerAvatar
import com.teneasy.chatuisdk.ui.base.GlideEngine
import com.teneasy.chatuisdk.ui.base.PARAM_DOMAIN
import com.teneasy.chatuisdk.ui.base.PARAM_XTOKEN
import com.teneasy.chatuisdk.ui.base.UserPreferences
import com.teneasy.chatuisdk.ui.base.Utils
import com.teneasy.chatuisdk.ui.http.UploadListener
import com.teneasy.chatuisdk.ui.http.UploadUtil
import com.teneasy.chatuisdk.ui.http.bean.Custom
import com.teneasy.chatuisdk.ui.http.bean.WorkerInfo
import com.teneasy.sdk.ChatLib
import com.teneasy.sdk.Result
import com.teneasy.sdk.TeneasySDKDelegate
import com.teneasy.sdk.ui.CellType
import com.teneasy.sdk.ui.MessageItem
import com.teneasy.sdk.ui.MessageSendState
import com.teneasyChat.api.common.CEntrance
import com.teneasyChat.api.common.CMessage
import com.teneasyChat.gateway.GGateway
import com.xuexiang.xhttp2.subsciber.ProgressDialogLoader
import com.xuexiang.xhttp2.subsciber.impl.IProgressLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


/**
 * 客服主界面fragment
 */
class KeFuFragment : KeFuBaseFragment(), TeneasySDKDelegate, UploadListener {

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
     override var TAG = "KefuNchatlib"
    //是否第一次加载页面的标志，历史记录和打招呼只需要页面第一次加载的时候显示
    private var isFirstLoad = true

    private var tempContent = ""
    private var chatExpireTime = 0 //in seconds, chatExpireTime

    private var lastMsg: CMessage.Message? = null
    private var workInfo = WorkerInfo()
    private var lastTimestamp: Timestamp? = null

    private var isFirstSend = true
    val imageTypes = arrayOf("tif","tiff","bmp", "jpg", "jpeg", "png", "gif", "webp", "ico", "svg")

    private var lastActiveDateTime = Date()

    private lateinit var dialogBottomMenu: DialogBottomMenu
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = KeFuViewModel()
        //检查内存变量的域名还存在不
        if (Constants.domain.isEmpty()){
            Utils().readConfig()
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
//        if(!EventBus.getDefault().isRegistered(this)) {
//            EventBus.getDefault().register(this)
//        }
        requireActivity().title = "客服"
        hidetvQuotedMsg()
        isFirstLoad = true
        viewModel.mlAutoReplyItem.observe(viewLifecycleOwner, {
            if (it?.qa?.size?:0 > 0){
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
    }

    //初始化聊天SDK
    private fun initChatSDK(baseUrl: String){
        val wssUrl = "wss://" + baseUrl + "/v1/gateway/h5?"
        Log.i(TAG, "x-token:" + Constants.xToken + "\n" + Date())

        chatLib = ChatLib(Constants.cert , Constants.xToken, wssUrl, Constants.userId, "9zgd9YUc",  0L, getCustomParam(), Constants.maxSessionMins)
        chatLib?.listener = this
        Log.i(TAG, "开始初始化SDK")
        chatLib?.makeConnect()
    }

    /* 初始化sdk的时候，如果需要传更多参数，在参数的最后一个，可以使用自定义参数
            var custom = Custom()
        custom.username = "xiaoming"
        val cust = Gson().toJson(custom)
         val ENCODED_cust = URLEncoder.encode(cust, "utf-8")

     */

    override fun onResume() {
        super.onResume()

        if (workInfo.workerName != null){
            binding?.tvTitle?.text = workInfo.workerName
        }

        //定时检测链接状态
        startTimer()
    }

    override fun onPause() {
        super.onPause()
        viewModel.getUnSendMsg()
        viewModel.reportError()
    }

    // UI初始化
    override fun initView() {
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
//                    val bundle = Bundle()
//                    bundle.putString(ARG_VIDEOURL, url)
//                    bundle.putString(ARG_KEFUNAME, workInfo.workerName)
//                    findNavController().navigate(R.id.frg_video_full, bundle)
                    // 单张图片场景
//                    XPopup.Builder(requireContext())
//                        .asCustom(VideoPlayView(requireContext(), url))
//                        .show()

                    val intent = Intent(requireContext(), FullVideoActivity::class.java)
                    intent.putExtra(ARG_VIDEOURL, url)
                    intent.putExtra(ARG_KEFUNAME, workInfo.workerName)
                    intent.setClass(requireContext(), FullVideoActivity::class.java)
                    requireActivity().startActivity(intent)
                }

                override fun onPlayImage(url: String) {
//                    XPopup.Builder(requireContext())
//
//                        .asCustom(BigImageView(requireContext(), url))
//                        .show()
//                    val bundle = Bundle()
//                    bundle.putString(ARG_IMAGEURL, url)
//                    bundle.putString(ARG_KEFUNAME, workInfo.workerName)
//                    findNavController().navigate(R.id.frg_image_full, bundle)

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

                //长按消息，引用消息并回复
                override fun onDownload(position: Int) {
                    val msg = msgAdapter.msgList?.get(position)?.cMsg
                    var url = ""
                    if ((msg?.image?.uri?: "").isNotEmpty()){
                        url = Constants.baseUrlImage + msg?.image?.uri?: ""
                    }else if ((msg?.video?.uri?: "").isNotEmpty()){
                        url = Constants.baseUrlImage + msg?.video?.uri?: ""
                    }
                    this@KeFuFragment.mIProgressLoader?.updateMessage("请稍等...")
                    this@KeFuFragment.mIProgressLoader?.showLoading()
                    Utils().downloadVideo( url, object :
                            (Int) -> Unit {
                        override fun invoke(progress: Int) {
                            Log.d(TAG, "下载进度：$progress")
                            if (progress == 100) {
                                this@KeFuFragment.mIProgressLoader?.dismissLoading()
                                ToastUtils.showToast(this@KeFuFragment.requireContext(), "下载成功！");
                            }else if(progress == -1){
                                this@KeFuFragment.mIProgressLoader?.dismissLoading()
                                ToastUtils.showToast(this@KeFuFragment.requireContext(), "下载失败！");
                                return
                            }
                            //this@KeFuFragment.mIProgressLoader?.updateMessage("下载进度：$progress");
                        }
                    });
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
                ) {}

                override fun onTextChanged(
                    s: CharSequence?, start: Int, before: Int,
                    count: Int
                ) {
                    val txt =  binding?.etMsg?.text?: ""
                    if (txt.length > 500){
                        binding?.etMsg!!.setText(txt.substring(0, 500))
                    }
                    binding?.tvCount?.text = binding?.etMsg?.text.toString().length.toString() + "/500"
                }
            })

            // 点击发送按钮，发送消息
            this.btnSend.setOnClickListener { v: View ->
                binding?.etMsg?.apply {
                    if ((this.text?:"").isEmpty()){
                        Utils().closeSoftKeyboard(v)
                    }else{
                        val txt = this.text.toString()
                        var replayMsgId = 0L
                        //这里的msgId是从列表的model里面拿，不是从消息体
                        if (((binding?.tvQuotedMsg?.tag?:0) as Int) >= 0 ){
                            viewModel.mlMsgList.value?.get((binding?.tvQuotedMsg?.tag?:0) as Int)?.msgId?.let {
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
                .setOnItemClickListener(AdapterView.OnItemClickListener{ adapterView, view, i, l ->
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
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun selectImageOrVideo(i: Int){
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

                                beforeUpload(item.realPath)

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

                            beforeUpload(item.realPath)

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

    private fun initObserver(){
        viewModel.mlMsgList.observe(viewLifecycleOwner) {
            if ((it?.count() ?: 0) > 0){
                msgAdapter.setList(it)
                refreshList()
            }
        }

        viewModel.mlAssignWorker.observe(viewLifecycleOwner){
            if (it == null){
                Log.d(TAG, "assignWorker 失败: null")
                return@observe
            }
                workInfo.workerName = it.nick
                workInfo.workerAvatar = it.avatar
                workerAvatar = it.avatar?:""
                workInfo.id = it.workerId ?: 0

                Log.d(TAG, "assignWorker Id: ${workInfo.id}")

            if (Constants.workerId == 0 || Constants.workerId != it.workerId) {
                this.lifecycleScope.launch {
                    //delay(100L)
                    if(isFirstLoad) {
                        Log.d(TAG, "重新获取聊天历史")
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

                   if (item.msgFmt == "MSG_VIDEO"){
                       val historyItem =  viewModel.composeVideoMsg(item, isLeft)
                       historyItem.cellType = CellType.TYPE_VIDEO
                       historyItem.msgId = (item.msgId?: "0").toLong()
                       historyList.add(historyItem)
                   }
                   else if(item.msgFmt == "MSG_TEXT") {
                       val historyItem =  viewModel.composeTextMsg(item, isLeft)
                       historyItem.msgId = (item.msgId?: "0").toLong()
                       historyList.add(historyItem)
                   }else if(item.msgFmt == "MSG_IMG") {
                       val historyItem = viewModel.composeImgMsg(item, isLeft)
                       historyItem.msgId = (item.msgId?: "0").toLong()
                       historyItem.cellType = CellType.TYPE_Image
                       historyList.add(historyItem)
                   }
               }

               viewModel.mlMsgList.value?.clear()
               viewModel.addAllMsgItem(historyList)

               if (isFirstLoad){
                   isFirstLoad = false
                   viewModel.composeLocalMsg("您好，${workInfo.workerName}为您服务！", true, false)
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
                Log.i(TAG, "刷新列表，滚动到底部")
            }
        }
    }

    //开一个定时器每隔几秒检查连接状态
    private fun startTimer() {
        //当页面在后台很多个小时之后, reConnectTimer期待为无效，如果仍然不为null，就需要判断并强制为null以边重新初始化
//        if (reConnectTimer != null && lastTimestamp != null){
//            val curTimestamp = Timestamp.newBuilder().setSeconds(Date().time / 1000).build()
//            Log.d(TAG, "时间间隔：${(curTimestamp.seconds - lastTimestamp?.seconds!!)}")
//            if (((curTimestamp.seconds - lastTimestamp?.seconds!! ) ?: 0) > 10){
//                lastTimestamp = null
//                reConnectTimer = null
//                Log.d(TAG, "reConnectTimer 已经无效")
//            }
//        }

        //当页面在后台很多个小时之后, reConnectTimer期待为无效，如果仍然不为null，就需要判断并强制为null以边重新初始化
//        Log.d(TAG, "时间间隔：${(Utils().differenceInMinutes(lastActiveDateTime, Date()))}")
//        if (Utils().differenceInMinutes(lastActiveDateTime, Date()) > 60 * 4){
//            reConnectTimer = null
//        }

        Log.i(TAG, "检查连接状态:${isConnected}")
        if(isConnected) {
            Log.i(TAG, "startTimer return")
            showTip("状态：已连接")
           return
        }
        if (isFirstLoad) {
            showTip("初始化SDK")
        }

        Constants.domain = UserPreferences().getString(PARAM_DOMAIN, Constants.domain)
        if(reConnectTimer == null) {
            reConnectTimer?.cancel()
            reConnectTimer?.purge()
            reConnectTimer = Timer()
            reConnectTimer?.schedule(object : TimerTask() {
                override fun run() {
                    if (chatLib == null) {
                        Log.d(TAG, "SDK重新初始化...")
                        showTip("初始化SDK...")
                        initChatSDK(Constants.domain)
                    } else if (!isConnected){
                        //showTip("SDK连接中...")
                        Log.d(TAG, "SDK连接中")
                        chatLib?.makeConnect()
                    }
                    lastTimestamp = Timestamp.newBuilder().setSeconds(Date().time / 1000).build()
                }
            }, 6000, 3000) //这里必须Delay 3s及以上，给初始化SDK足够的时间
        } else{
                showTip("请稍等...")
        }
    }

    // 关闭连接状态定时器
    private fun closeTimer() {
        if(reConnectTimer != null) {
            reConnectTimer?.purge()
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

    fun beforeUpload(filePath: String){
        println("开始上传。。。")
        mIProgressLoader?.updateMessage("开始上传。。。")
        mIProgressLoader?.showLoading()

        //val filePath = Utils().encodeFilePath(mediaPath)
        var file = File(filePath)

        if (!file.exists()){
            ToastUtils.showToast(requireContext(), "文件不存在")
            mIProgressLoader?.dismissLoading()
            return
        }
        val ext = file.absoluteFile.extension

        if (imageTypes.contains(ext.lowercase())){
            if (file.length() >= 2000 * 10 * 1000){
                ToastUtils.showToast(requireContext(), "图片限制20M")
                mIProgressLoader?.dismissLoading()
                return
            }
            //uploadFile(file)
            UploadUtil(this).uploadFile(file)
        }else{
            val newFilePath = file.absolutePath.replace("." + ext,"").replace(".","") + Date().time + "." + ext
            val newFile = File(newFilePath)
            mIProgressLoader?.updateMessage("正在压缩。。。")
            CoroutineScope(Dispatchers.Main).launch {
                val resultCode = Utils().compressVideo(file.absolutePath.toString(), newFilePath)

                withContext(Dispatchers.Main) {
                    if (resultCode == 0) {
                        Log.i(TAG, "原始文件大小:" + file.length() )
                        Log.i(TAG, "Video compression succeeded")
                        // If you need to update the UI, do it here
                        if (newFile.length() >= 30000 * 10 * 1000){
                            ToastUtils.showToast(requireContext(), "视频/文件限制300M")
                            mIProgressLoader?.dismissLoading()
                            return@withContext
                        }

                        if (newFile.length() > 0) {
                            file = newFile
                        }


                        val videoThumbnail = Utils().getVideoThumb(requireContext(), Uri.fromFile(file))
                        if (videoThumbnail == null){
                            //ToastUtils.showToast(requireContext(), "获取视频缩略图失败")
                           // mIProgressLoader?.dismissLoading()
                            //return@withContext
                        }
                        UploadUtil(this@KeFuFragment).uploadFile(file)
                        //uploadFile(file)
                        Log.i(TAG, "上传文件大小:" + file.length() )
                    } else {
                        Log.i(TAG, "Video compression failed with return code $resultCode")
                        //Config.printLastCommandOutput(Log.INFO)
                        if (file.length() >= 30000 * 10 * 1000){
                            ToastUtils.showToast(requireContext(), "视频/文件限制300M")
                            mIProgressLoader?.dismissLoading()
                            return@withContext
                        }
                        Log.i(TAG, "上传文件大小:" + file.length() )
                        UploadUtil(this@KeFuFragment).uploadFile(file)
                        //uploadFile(file)
                    }
                }

            }
        }
    }


    //这个函数可以上传图片和视频
     fun uploadFile(file: File) {
        mIProgressLoader?.updateMessage("上传中。。。")
        Thread(Runnable {
            kotlin.run {
                val multipartBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("myFile", Date().time.toString() + "." + file.extension,  RequestBody.create("multipart/form-data".toMediaTypeOrNull(), file))
                    .addFormDataPart("type", "4")
                    .build()// + file.extension

                val request2 = Request.Builder().url(Constants.baseUrlApi() + "/v1/assets/upload-v3")
                    .addHeader("X-Token", Constants.xToken)
                    .post(multipartBody).build()

                val okHttpClient: OkHttpClient = OkHttpClient().newBuilder()
                    .connectTimeout(50, TimeUnit.SECONDS)
                    .writeTimeout(50, TimeUnit.MINUTES)
                    .readTimeout(50, TimeUnit.MINUTES)
                    .build()
                val call = okHttpClient.newCall(request2)
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        // 上传失败
                        toast("上传失败" + e.localizedMessage)
                        mIProgressLoader?.updateMessage("");
                        mIProgressLoader?.dismissLoading()
                    }

                    override fun onResponse(call: Call, response: Response) {
                        mIProgressLoader?.dismissLoading()
                        val body = response.body
                        if(response.code == 200 && body != null) {
                            val path = response.body!!.string()
                            val gson = Gson()
                            val result = gson.fromJson(path, UploadResult::class.java)

                            if (result.code == 0 || result.code == 200) {
                                if (imageTypes.contains(file.absoluteFile.extension.lowercase())) {
                                    // 发送图片
                                    sendImgMsg(result.data?.filepath?: "")//Constants.baseUrlImage +
                                } else {
                                    sendVideoMsg(result.data?.filepath?: "")//Constants.baseUrlImage +
                                }
                                Log.i(TAG, ("上传成功" + result.data?.filepath))
                            } else {
                                toast(result.message?: "上传失败");
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
    }

    //释放聊天相关库和变量
    fun exitChat(){
        closeTimer()
        Constants.workerId = 0
        isConnected = false
        chatLib?.disConnect()
        chatLib = null
        isFirstLoad = true
        Log.i(TAG, "销毁聊天")
    }

    //==========图片选择===========//
    // 调用拍照
    private fun showCamera(resultCallbackListener: OnResultCallbackListener<LocalMedia>
    ) {
        PictureSelector.create(KeFuFragment@this)
            .openCamera(SelectMimeType.TYPE_ALL)
            .setRecordVideoMaxSecond(300)
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
    fun sendMsg(txt: String, force: Boolean = false, replyMsgId: Long = 0) {
        if(chatLib == null){
            // Toast.makeText(context, "SDK还未初始化", Toast.LENGTH_SHORT).show()
            showTip("SDK还未初始化")
            return
        }

        if (txt.isEmpty()){
            Toast.makeText(requireContext(), "请输入内容", Toast.LENGTH_SHORT).show()
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
        //说明至少已经发送成功1条消息，无需再附带自动回复消息
        if (lastMsg != null){
            withAutoReplyU = null
        }
        chatLib?.sendMessage(txt, CMessage.MessageFormat.MSG_TEXT, Constants.CONSULT_ID, replyMsgId, withAutoReplyU)
        val messageItem = MessageItem()
        messageItem.cMsg = chatLib?.sendingMessage
        messageItem.isLeft = false
        viewModel.addMsgItem(messageItem, chatLib?.payloadId ?: 0)
        lastActiveDateTime = Date()
    }

    /**
     * 根据传递的图片地址，发送图片消息。该方法会发送socket消息
     */
    fun sendImgMsg(url: String) {
        if(chatLib == null){
            Toast.makeText(context, "SDK还未初始化", Toast.LENGTH_SHORT).show()
            return
        }

        //说明至少已经发送成功1条消息，无需再附带自动回复消息
        if (lastMsg != null){
            withAutoReplyU = null
        }

        chatLib?.sendMessage(url, CMessage.MessageFormat.MSG_IMG, Constants.CONSULT_ID, 0, withAutoReplyU)
        val messageItem = MessageItem()
        messageItem.cMsg = chatLib?.sendingMessage
        messageItem.isLeft = false
        viewModel.addMsgItem(messageItem, chatLib?.payloadId ?: 0)
        lastActiveDateTime = Date()
    }

    fun sendVideoMsg(url: String) {
        if(chatLib == null){
            Toast.makeText(context, "SDK还未初始化", Toast.LENGTH_SHORT).show()
            return
        }

        //说明至少已经发送成功1条消息，无需再附带自动回复消息
        if (lastMsg != null){
            withAutoReplyU = null
        }
        //withAutoReplyU参数, 把用户点自动回复的最后一条消息带到客服端，方便客服端显示，仅用户主动发的第一条消息会这样做，其余会被SDK忽略
        chatLib?.sendMessage(url, CMessage.MessageFormat.MSG_VIDEO, Constants.CONSULT_ID, 0, withAutoReplyU)
        val messageItem = MessageItem()
        messageItem.cMsg = chatLib?.sendingMessage
        messageItem.isLeft = false
        viewModel.addMsgItem(messageItem, chatLib?.payloadId ?: 0)
        lastActiveDateTime = Date()
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

        //检查并重发上次连接未发出去的消息
        if (unSentMessage[CONSULT_ID] == null || unSentMessage[CONSULT_ID]!!.isEmpty()){
            viewModel.getUnSendMsg()
        }
        msgAdapter.msgList?.let { chatLib?.let { it1 -> viewModel.handleUnSendMsg(it, it1) } }
    }

    //聊天sdk里面有什么异常，会从这个回调告诉
    override fun systemMsg(msg: Result) {
        /*
        code: 1010 在别处登录了
code: 1002 无效的Token
         */
        Log.i(TAG, msg.msg)
        if (msg.code in 1000..1010) {
            isConnected = false
        }
        if (msg.code == 1002 || msg.code == 1010 || msg.code == 1005) {
            if (msg.code == 1002){
                //showTip("无效的Token")
                //有时候服务器反馈的这个消息不准，可忽略它
                //toast("无效的Token ")
            }else {
                //1010
                //showTip("在别处登录了")

                //1005，会话超时
                toast(msg.msg)
                //禁掉重试机制
                runOnUiThread{
                    mIProgressLoader?.dismissLoading()
                    exitChat()
                    //返回到上个页面
                    findNavController().popBackStack()
                }
                Log.i(TAG, "返回页面")
            }
        }else{
            showTip("")
        }

        //可选：如果断开连接，可以上报日志
        val wssUrl = "wss://" + Constants.domain + "/v1/gateway/h5?"
        //连接SDK时候所使用的参数
        val param = Constants.cert + " token:" +  Constants.xToken + " x-trace-id" + UUID.randomUUID().toString() + " tenantId:" + Constants.merchantId + " CONSULT_ID:" + Constants.CONSULT_ID  + " userid:" + Constants.userId + "custom:" + getCustomParam()
        viewModel.logError(msg.code, param, Constants.xToken, msg.msg, wssUrl)
    }

    //对方删除了消息，会回调这个函数
    override fun msgDeleted(msg: CMessage.Message?, payloadId: Long, msgId: Long, errMsg: String) {
        viewModel.removeMsgItem(payloadId, msg?.msgId?:0)
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
                    handleReply(referMsg.cMsg!!, item, msgId)
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
        //当用户端在当前会话，但是其他咨询类型客服发来了消息，给予提醒。
        if (msg.consultId != Constants.CONSULT_ID){
            showTip("其他客服有新消息！")
            val tMsg = binding?.tvTips?.text
            if (tMsg != null && tMsg.isNotEmpty()){
               // return
                Handler(Looper.getMainLooper()).postDelayed(
                    {
                      showTip(tMsg.toString())
                    },
                    3000 // value in milliseconds
                )
            }

//            runOnUiThread(
//                Runnable {
//                    Toast.makeText(context, "其他客服有新消息！", Toast.LENGTH_SHORT).show()
//                }
//            )
        }else {

            //把收到的消息插入到列表
            val messageItem = MessageItem()
            messageItem.cMsg = msg

            if (msg.replyMsgId > 0) {
                val referMsg = viewModel.mlMsgList.value?.firstOrNull { it.msgId == msg.replyMsgId }
                if (referMsg != null) {
                    handleReply(referMsg.cMsg!!, messageItem, msg.msgId)
                }
            }

            //切记，在这也付值msgId，便于从列表里面找记录
            messageItem.msgId = msg.msgId
            messageItem.isLeft = true
            viewModel.addMsgItem(messageItem, 0)
        }
    }

    //客服更换了，需要更换客服信息
    override fun workChanged(msg: GGateway.SCWorkerChanged) {
         workInfo = WorkerInfo()
        workInfo.workerName = msg.workerName
        workInfo.workerAvatar = msg.workerAvatar
        workerAvatar = msg.workerAvatar?: ""
        Constants.CONSULT_ID = msg.consultId
        Log.d(TAG, "workChanged WorkerId: ${workInfo.id} ${Constants.CONSULT_ID}")
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

                if (!isFirstLoad){
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
        //切记在这需要把tag置为-1
        binding?.tvQuotedMsg?.tag = -1
    }

    private fun showQuotedMsg(txt: String){
        binding?.tvQuotedMsg?.visibility = View.VISIBLE
        binding?.tvQuotedMsg?.text = txt
    }

    fun handleReply(referMsg: CMessage.Message, model: MessageItem, newMsgId: Long){
        val txt = referMsg.content?.data?.split("回复：")?.get(0) ?: ""

        var referText = "回复：$txt"
        if (!(referMsg.video?.uri ?: "").isEmpty()) {
            referText = "回复：[视频]"
        } else if (!(referMsg.image?.uri ?: "").isEmpty()) {
            referText = "回复：[图片]"
        }
        val newText = "${model.cMsg?.content?.data}\n$referText"
        model.cMsg = composATextMessage(newText.trim(), newMsgId)
    }

    fun composATextMessage(textMsg: String, msgId: Long) : CMessage.Message{
        //第一层
        var cMsg = CMessage.Message.newBuilder()
        //第二层
        var cMContent = CMessage.MessageContent.newBuilder()

        //d.t = msgDate.time
        cMsg.msgId = msgId
        cMsg.msgTime = Utils().getNowTimeStamp()
        cMContent.data = textMsg
        cMsg.setContent(cMContent)

        return cMsg.build()
    }

    override fun uploadSuccess(path: String, isVideo: Boolean) {
        if (isVideo) {
            sendVideoMsg(path)//Constants.baseUrlImage +
        } else {
            // 发送图片
            sendImgMsg(path)
        }
        mIProgressLoader?.updateMessage("")
        mIProgressLoader?.dismissLoading()
    }

    override fun uploadProgress(progress: Int) {
        mIProgressLoader?.updateMessage("上传中 " + progress.toString() + "%")
    }

    override fun uploadFailed(msg: String) {
        ToastUtils.showToast(requireContext(), msg)
        mIProgressLoader?.dismissLoading()
    }
}