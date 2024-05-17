package com.teneasy.chatuisdk.ui.main;

import android.content.Context.INPUT_METHOD_SERVICE
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
import com.teneasy.chatuisdk.BR
import com.teneasy.chatuisdk.R
import com.teneasy.chatuisdk.databinding.FragmentKefuBinding
import com.teneasy.chatuisdk.ui.base.Constants
import com.teneasy.chatuisdk.ui.base.GlideEngine
import com.teneasy.chatuisdk.ui.base.PARAM_DOMAIN
import com.teneasy.chatuisdk.ui.base.PARAM_XTOKEN
import com.teneasy.chatuisdk.ui.base.UserPreferences
import com.teneasy.chatuisdk.ui.base.Utils
import com.teneasy.chatuisdk.ui.base.scrollToBottomWithMargin
import com.teneasy.chatuisdk.ui.http.bean.WorkerInfo
import com.teneasy.sdk.ChatLib
import com.teneasy.sdk.Result
import com.teneasy.sdk.TeneasySDKDelegate
import com.teneasy.sdk.ui.MessageItem
import com.teneasy.sdk.ui.MessageSendState
import com.teneasyChat.api.common.CMessage
import com.teneasyChat.gateway.GGateway
import com.xuexiang.xhttp2.subsciber.ProgressDialogLoader
import com.xuexiang.xhttp2.subsciber.impl.IProgressLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList


/**
 * 客服主界面fragment
 */
class KeFuFragment : BaseBindingFragment<FragmentKefuBinding>(), TeneasySDKDelegate {

    private lateinit var msgAdapter: MessageListAdapter
    private lateinit var qaAdapter: GroupedQAdapter
    private lateinit var viewModel: KeFuViewModel

    private var mIProgressLoader: IProgressLoader? = null

    private var reConnectTimer: Timer? = null
    private var chatLib: ChatLib? = null
    private var connected = false
    private val TAG = "KefuNchatlib"
    private var sayHello = false
    private var wssBaseUrl = ""
    private var retryTimes = 0
    private var tempContent = ""
    private var mins = 0

    private var lastMsg: CMessage.Message? = null

    private lateinit var dialogBottomMenu: DialogBottomMenu
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = KeFuViewModel()

        arguments?.let {
            wssBaseUrl = it.getString(PARAM_DOMAIN) ?: ""
            initChatSDK(wssBaseUrl)
        }

       requireActivity().onBackPressedDispatcher.addCallback(this) {
            findNavController().popBackStack()
        }

        mIProgressLoader = getProgressLoader()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        /*if(!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }*/
        requireActivity().title = "客服"

        hidetvQuotedMsg()
    }

    private fun initChatSDK(baseUrl: String){
        val wssUrl = "wss://" + baseUrl + "/v1/gateway/h5?"
        Log.i(TAG, "x-token:" + Constants.xToken)
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
                override fun onDelete(position: Int) {
                   val messageItem = msgAdapter.msgList?.get(position)
                    messageItem?.let {
                        chatLib?.deleteMessage(it.cMsg?.msgId ?: 0)
                        viewModel.removeMsgItem(it)
                    }
                }

                override fun onCopy(position: Int) {
                    val messageItem = msgAdapter.msgList?.get(position)
                    val text = messageItem?.cMsg?.content?.data?:""
                    Utils().copyText(text, requireContext())
                }

                override fun onReSend(position: Int) {

                }

                override fun onQuote(position: Int) {
                    binding?.tvQuotedMsg?.visibility = View.VISIBLE
                    binding?.tvQuotedMsg?.tag = position
                    showQuotedMsg("回复：" + msgAdapter.msgList?.get(position)?.cMsg?.content?.data)
                }

                override fun onSendLocalMsg(msg: String, isLeft: Boolean) {
                   this@KeFuFragment.sendLocalMsg(msg, isLeft)
                }
            } )
            msgAdapter.setList(ArrayList())

            val layoutManager = LinearLayoutManager(context)
            layoutManager.orientation = LinearLayoutManager.VERTICAL
            //layoutManager.stackFromEnd = false
            this.rcvMsg.layoutManager = layoutManager
            this.rcvMsg.adapter = msgAdapter

            // 初始化自动回复列表
            qaAdapter = GroupedQAdapter(requireContext(), ArrayList(), null)

            // 提问列表点击事件
            qaAdapter.setOnHeaderClickListener { _, _, groupPosition ->
                /*
                自动回复 有两种情况：
                1、一级问题，点击后回复对应的答案；
                2、一级问题，点击展示与一级相关的问题分类（及二级问题），点击二级对应应的问题，则回复答案。
                */
                val questionTxt = qaAdapter.data.get(groupPosition).content ?:"null"
                val answerTxt = qaAdapter.data.get(groupPosition).answer.joinToString(separator = "\n")  ?:""

               if (answerTxt.isEmpty()){
                   if (qaAdapter.isExpand(groupPosition)) {
                       qaAdapter.collapseGroup(groupPosition)
                   } else {
                       qaAdapter.collapseTheResetGroup(groupPosition)
                       qaAdapter.expandGroup(groupPosition)
                   }
               }else{
                   // 发送提问消息
                   sendLocalMsg(questionTxt, false)
                   // 自动回答
                   sendLocalMsg(answerTxt)
               }
            }

            // 问题点击事件
            qaAdapter.setOnChildClickListener { _, _, groupPosition, childPosition ->
                val answerTxt = qaAdapter.data.get(groupPosition).related?.get(childPosition)?.content ?:"null"
                val questionTxt = qaAdapter.data.get(groupPosition).related?.get(childPosition)?.question?.content?.data ?:""
                // 发送提问消息
                sendLocalMsg(questionTxt, false)
                // 自动回答
                sendLocalMsg(answerTxt)
            }

            // 初始化输入框
            this.etMsg.setOnFocusChangeListener { v: View, hasFocus: Boolean ->
                if (!hasFocus) {
                    Utils().closeSoftKeyboard(v)
                }
            }
            // 聊天界面输入框，输入事件。实现文本输入和表情输入的UI切换功能
            this.etMsg.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    // TODO Auto-generated method stub
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
                        sendMsg(txt)
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
          //  initData()
            initObserver()
            //viewModel.assignWorker(Constants.CONSULT_ID)
            //viewModel.queryAutoReply(Constants.CONSULT_ID)
            this.llClose.setOnClickListener {
                findNavController().popBackStack()
            }
        }
    }

    private fun initObserver(){
        mIProgressLoader?.showLoading()
        viewModel.mlMsgList.observe(this@KeFuFragment) {
            msgAdapter.setList(it)
            refreshList()
        }
        viewModel.mlWorkerInfo.observe(this@KeFuFragment){
            updateWorkInf(it)
        }

        viewModel.mlAssignWorker.observe(this@KeFuFragment){
            //if(it.workerId != 0) {
                //viewModel.loadWorker(it.workerId)
                val workInfo = WorkerInfo()
                workInfo.workerName = it.nick
                workInfo.workerAvatar = it.avatar
                workInfo.id = it.workerId
                updateWorkInf(workInfo)
                lifecycleScope.launch {
                    //delay(100L)
                    viewModel.queryChatHistory(Constants.CONSULT_ID)
                }
            //}
        }

        viewModel.mlNewWorkAssigned.observe(this@KeFuFragment){
            if (it){
                sendMsg(tempContent, true)
            }
        }

        viewModel.mHistoryList.observe(this@KeFuFragment){
           it?.run {
               var qaList = ArrayList<MessageItem>()
               for (item in this.reversed()) {
                   // sender如果=chatid就是 用户 发的，反之是 客服 或者系统发的
                   var isLeft = true
                   if (item.sender == item.chatId){
                       isLeft = false
                   }
                   if(item.msgFmt == "MSG_TEXT") {
                       val qaItem =  viewModel.composeTextMsg(item, isLeft)
                       qaList.add(qaItem)
                   }else if(item.msgFmt == "MSG_IMG") {
                       val qaItem = viewModel.composeImgMsg(item, isLeft)
                       qaList.add(qaItem)
                   }
               }
               var qaItem = MessageItem()
               qaItem.isQA = true
               qaList.add(qaItem)
               //viewModel.addAllMsgItem(qaList)

               //添加一个空白的，确保列表滚动到最后能看到所有内容
                qaItem = MessageItem()
               qaItem.isLastLine = true
               qaList.add(qaItem)

               viewModel.addAllMsgItem(qaList)
               mIProgressLoader?.dismissLoading()
           }
        }

        /*viewModel.mlAutoReplyItem.observe(this@KeFuFragment){

            it.qa.apply {
                qaAdapter.setDataList(this)
            }

        }*/
    }


    private fun refreshList(){
        runOnUiThread {
            msgAdapter.notifyDataSetChanged()
            binding?.let {
                //it.rcvMsg.scrollToBottomWithMargin(100)
                it.rcvMsg.scrollToPosition(msgAdapter.itemCount - 1)
            }
        }
        Log.i(TAG, "刷新列表")
//        val layoutManager  = binding!!.rcvMsg.layoutManager as LinearLayoutManager
//        layoutManager.scrollToPositionWithOffset(msgAdapter.itemCount - 1, 0)
//        binding!!.rcvMsg.scrollToBottomWithMargin(100)
    }

    private fun startTimer() {
        //closeTimer()
        if(connected) {
           return
        }
        showTip("正在重新连接...")
        Log.i(TAG, "正在重新连接...")
        Constants.domain = UserPreferences().getString(PARAM_DOMAIN, Constants.domain)
        if(reConnectTimer == null) {
            reConnectTimer = Timer()

            reConnectTimer?.schedule(object : TimerTask() {
                override fun run() {
                    if (chatLib == null || !connected) {
                        Log.d(TAG, "SDK 重新初始化")
                        initChatSDK(Constants.domain)
                    }else{
                        // closeTimer()
                        //hideTip()
                    }
                }
            }, 3000,5000)
        }
    }

    // 关闭计时器
    private fun closeTimer() {
        //Log.d(TAG, "Timer 关闭")
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
     */
    fun uploadImg(filePath: String) {
        // 多文件上传Builder,用以匹配后台Springboot MultipartFile
        val file = File(filePath)
        Thread(Runnable {
            kotlin.run {
                val multipartBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("myFile", file.name,  RequestBody.create("multipart/form-data".toMediaTypeOrNull(), file))
                    .addFormDataPart("type", "1")
                    .build()

                val request2 = Request.Builder().url(Constants.baseUrlApi + "/v1/assets/upload/")
                    .addHeader("X-Token", Constants.xToken)
                    .post(multipartBody).build()

                val okHttpClient = OkHttpClient()
                val call = okHttpClient.newCall(request2)
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        // 上传失败
                        mIProgressLoader?.dismissLoading()
                    }

                    override fun onResponse(call: Call, response: Response) {
                        mIProgressLoader?.dismissLoading()
                        val body = response.body
                        if(body != null) {
                            val path = response.body!!.string()
                            // 发送图片
                            sendImgMsg(path)//Constants.baseUrlImage +
                        } else {
                            // 上传失败
                            Toast.makeText(context, "上传失败", Toast.LENGTH_LONG).show()
                        }
                    }
                })

            }
        }).start()
    }

    override fun onDestroy() {
        super.onDestroy()
      exitChat()
    }

    fun exitChat(){
        closeTimer()
        chatLib?.disConnect()
        chatLib = null
//        if(!EventBus.getDefault().isRegistered(this)) {
//            EventBus.getDefault().unregister(this)
//        }
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
            .openCamera(SelectMimeType.ofImage())
            .forResult(resultCallbackListener)
    }

    // 选择图片
    private fun showSelectPic(resultCallbackListener: OnResultCallbackListener<LocalMedia>) {
        PictureSelector.create(KeFuFragment@this)
            .openGallery(SelectMimeType.ofImage())
            .setImageEngine(GlideEngine.createGlideEngine())
            .setMaxSelectNum(1)
            .isDisplayCamera(false)
            .forResult(resultCallbackListener)
    }

    /**
     * 根据输入框的内容，发送文本消息。
     */
    fun sendMsg(txt: String, force: Boolean = false) {
        if(chatLib == null){
           // Toast.makeText(context, "SDK还未初始化", Toast.LENGTH_SHORT).show()
            showTip("SDK还未初始化")
            return
        }
        /*
      todo: 用户发送消息，要先比对上一条时间 ，超过 配置的时间（默认5分钟），就调用 分流接口  v1/api/assign-worker
         */
        if(!force) {
            tempContent = txt
            val lastMsgTime = Date(lastMsg?.msgTime?.seconds?: 0L)
            val sendingMsgTime = Date(chatLib?.sendingMessage?.msgTime?.seconds?: 0L)

            val diffTime = Utils().isMessageTimeDifferenceValid(lastMsgTime, sendingMsgTime, mins)
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
        /*
           todo: 用户发送消息，要先比对上一条时间 ，超过 配置的时间（默认5分钟），就调用 分流接口  v1/api/assign-worker
              */
        chatLib?.sendMessage(url, CMessage.MessageFormat.MSG_IMG, Constants.CONSULT_ID)
        var messageItem = MessageItem()
        messageItem.cMsg = chatLib?.sendingMessage
        messageItem.isLeft = false
        viewModel.addMsgItem(messageItem, chatLib?.payloadId ?: 0)
    }

    /**
     * 需要每60秒调用一次这个函数，确保socket的活动状态。
     */
    fun sendHeartBeat() {
        chatLib?.sendHeartBeat()
        println("确保通信在活跃状态")
    }

    fun onDestory() {
        chatLib?.disConnect()
    }

    override fun connected(c: GGateway.SCHi) {
        connected = true;
        println(c.id)
        showTip("连接成功")
        Log.i(TAG, "连接成功")

        UserPreferences().putString(PARAM_XTOKEN, c.token)
        Constants.xToken = c.token
        if (!sayHello) {
            viewModel.assignWorker(Constants.CONSULT_ID)
        }
       mins = c.chatExpireTime.toInt()
    }

    override fun systemMsg(msg: Result) {
        if (msg.code >= 1000 && msg.code < 1010) {
            connected = false
        }else{
        }
        Log.i(TAG, msg.msg)
    }


    override fun msgDeleted(msg: CMessage.Message, payloadId: Long, msgId: Long, errMsg: String) {
        viewModel.removeMsgItem(payloadId, msg.msgId)
        viewModel.composeLocalMsg("对方撤回了一条消息", true, isTip = true)
    }

    override fun msgReceipt(msg: CMessage.Message, payloadId: Long, msgId: Long, errMsg: String) {
        val item = viewModel.mlMsgList.value?.find { it.payLoadId == payloadId }
        if(item != null) {
            item.sendStatus = MessageSendState.发送成功
        }
        lastMsg = msg
        refreshList()
        Log.i(TAG, "收到回执：${msg.content.data}")
        //hideTip()
    }

    override fun receivedMsg(msg: CMessage.Message) {
        if (msg.consultId != Constants.CONSULT_ID){
            //免用户端在当前会话里显示其他咨询类型客服发来的消息。
            showTip("其他客服有新消息！")
        }else {
            var messageItem = MessageItem()
            messageItem.cMsg = msg
            messageItem.isLeft = true
            viewModel.addMsgItem(messageItem, 0)
        }
    }

    override fun workChanged(msg: GGateway.SCWorkerChanged) {
        var workInfo = WorkerInfo()
        workInfo.workerName = msg.workerName
        workInfo.workerAvatar = msg.workerAvatar
        updateWorkInf(workInfo)
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

    private fun updateWorkInf(workerInfo: WorkerInfo){
            binding?.let {
                it.tvTitle.text = "${workerInfo.workerName}"
                if(!sayHello) {
                    //sendLocalMsg("您好，请问有什么可以帮到您！")
                    sayHello = true
                }
                Constants.workerId = workerInfo.id
                showTip("您好，${workerInfo.workerName}客服为您服务！")
                // 更新头像
                if (workerInfo.workerAvatar != null && workerInfo.workerAvatar?.isEmpty() == false) {
                    val url = Constants.baseUrlImage + workerInfo.workerAvatar
                    print("avatar:$url")
                    Glide.with(binding!!.civAuthorImage).load(url).dontAnimate()
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .into(binding!!.civAuthorImage)
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

    private fun hidetvQuotedMsg(){
        binding?.tvQuotedMsg?.visibility = View.GONE
        binding?.tvQuotedMsg?.text = ""
    }

    private fun showQuotedMsg(txt: String){
        binding?.tvQuotedMsg?.visibility = View.VISIBLE
        binding?.tvQuotedMsg?.text = txt
    }
}