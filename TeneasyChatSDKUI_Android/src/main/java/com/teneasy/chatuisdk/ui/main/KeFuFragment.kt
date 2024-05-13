package com.teneasy.chatuisdk.ui.main;

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat.getSystemService
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
import com.teneasy.chatuisdk.ui.base.PARAM_WSS_BASE_URL
import com.teneasy.chatuisdk.ui.base.SharedPreferencesReader
import com.teneasy.chatuisdk.ui.base.Utils
import com.teneasy.chatuisdk.ui.http.bean.WorkerInfo
import com.teneasy.sdk.ChatLib
import com.teneasy.sdk.LineDetectDelegate
import com.teneasy.sdk.LineDetectLib
import com.teneasy.sdk.MessageEventBus
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
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.io.IOException
import java.util.*


/**
 * 客服主界面fragment
 */
class KeFuFragment : BaseBindingFragment<FragmentKefuBinding>(), TeneasySDKDelegate {

    private lateinit var msgAdapter: MessageListAdapter
    private lateinit var qaAdapter: GroupedQAdapter
    private lateinit var viewModel: KeFuViewModel

    private var mIProgressLoader: IProgressLoader? = null

    private var timer: Timer? = null
    private var chatLib: ChatLib? = null
    private var connected = false
    private val TAG = "KeFuFragment"
    private var sayHello = false
    private var wssBaseUrl = ""

    private lateinit var dialogBottomMenu: DialogBottomMenu
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = KeFuViewModel()

        arguments?.let {
            wssBaseUrl = it.getString(PARAM_WSS_BASE_URL) ?: ""
            initChatSDK(Constants.baseUrl)
        }

       requireActivity().onBackPressedDispatcher.addCallback(this) {
            findNavController().popBackStack()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if(!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        requireActivity().title = "客服"

        hidetvQuotedMsg()
    }

    private fun initChatSDK(baseUrl: String){
        var wssUrl = "wss://" + baseUrl + "/v1/gateway/h5?"
        val token = SharedPreferencesReader().getString(Constants.wss_token, "")
        chatLib = ChatLib(Constants.cert , token, wssUrl, Constants.userId, "9zgd9YUc")
        chatLib?.listener = this
        chatLib?.makeConnect()
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
            this.rcvAutoReply.layoutManager = LinearLayoutManager(context)
            qaAdapter = GroupedQAdapter(requireContext(), ArrayList(), null)
            this.rcvAutoReply.adapter = qaAdapter

            // 提问列表点击事件
            qaAdapter.setOnHeaderClickListener { _, _, groupPosition ->
                if (qaAdapter.isExpand(groupPosition)) {
                    qaAdapter.collapseGroup(groupPosition)
                } else {
                    qaAdapter.collapseTheResetGroup(groupPosition)
                    qaAdapter.expandGroup(groupPosition)
                }
//                mAdapter?.selectedAuthKind = mVisibleToList.get(groupPosition).kind
//                mAdapter?.notifyDataChanged()
//                mMomFeedAuthBean?.kind = mVisibleToList.get(groupPosition).kind
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
                    closeSoftKeyboard(v)
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
                        closeSoftKeyboard(v)
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
            viewModel.assignWorker(Constants.CONSULT_ID)

            this.llClose.setOnClickListener {
                findNavController().popBackStack()
            }
        }
    }

    private fun initObserver(){
        viewModel.mlMsgList.observe(this@KeFuFragment) {
            msgAdapter.setList(it)
            refreshList()
        }
        viewModel.mlWorkerInfo.observe(this@KeFuFragment){
            updateWorkInf(it)
        }

        viewModel.mlAssignWorker.observe(this@KeFuFragment){
            if(it.workerId != 0) {
                viewModel.loadWorker(it.workerId)
                lifecycleScope.launch {
                    delay(100L)
                    viewModel.queryAutoReply(Constants.CONSULT_ID)
                }
            }
        }

        /*
        自动回复 有两种情况：
1、一级问题，点击后回复对应的答案；
2、一级问题，点击展示与一级相关的问题分类（及二级问题），点击二级对应应的问题，则回复答案。
         */
        /*viewModel.mlAutoReplyItem.observe(this@KeFuFragment){

            it.qa.apply {
                qaAdapter.setDataList(this)
            }

        }*/
    }

    private fun refreshList(){
        runOnUiThread {
            msgAdapter.notifyDataSetChanged()
            binding?.rcvMsg?.scrollToPosition(msgAdapter.itemCount - 1)
        }
    }

    // 启动计时器，持续调用心跳方法
    private fun startTimer() {
        closeTimer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                //需要执行的任务
                chatLib?.sendHeartBeat()
            }
        }, 0,30000)    //每隔5秒发送心跳
    }

    // 关闭计时器
    private fun closeTimer() {
        if(timer != null) {
            timer?.cancel()
            timer = null
        }
    }

    fun getProgressLoader(): IProgressLoader? {
        if (mIProgressLoader == null) {
            mIProgressLoader =
                ProgressDialogLoader(context)
        }
        return mIProgressLoader
    }


    // EventBus 消息接收解析，针对socket sdk中的消息进行捕捉和解析。
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun updateMsg(event: MessageEventBus<Any>) {
        if(event.what == 0) {
            // 解析状态
            if(event.arg == 200) {
                startTimer()
            } else {
                closeTimer()
            }
        } else if(event.what == 1 && event.data != null) {

        }
    }

    /**
     * 关闭软键盘
     *
     * @param view 当前页面上任意一个可用的view
     */
    private fun closeSoftKeyboard(view: View?) {
        if (view == null || view.windowToken == null) {
            return
        }
        val imm: InputMethodManager =
            view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
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
                    .addHeader("X-Token", Constants.httpToken)
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
      exit()
    }

    fun exit(){
        closeTimer()
        if(!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
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
    fun sendMsg(txt: String) {
        if(chatLib == null){
            Toast.makeText(context, "SDK还未初始化", Toast.LENGTH_SHORT).show()
            return
        }

        if (!connected){
            Toast.makeText(context, "SDK尚未连接成功", Toast.LENGTH_SHORT).show()
            return
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

        if (!connected){
            Toast.makeText(context, "SDK尚未连接成功", Toast.LENGTH_SHORT).show()
            return
        }
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
        SharedPreferencesReader().putString(Constants.wss_token, c.token)
    }

    override fun msgDeleted(msg: CMessage.Message, payloadId: Long, msgId: Long, errMsg: String) {
        viewModel.removeMsgItem(payloadId, msg.msgId)
    }

    override fun msgReceipt(msg: CMessage.Message, payloadId: Long, msgId: Long, errMsg: String) {
        val item = viewModel.mlMsgList.value?.find { it.payLoadId == payloadId }
        if(item != null) {
            item.sendStatus = MessageSendState.发送成功
        }
        refreshList()
        Log.i(TAG, "收到回执：${msg.content.data}")
        hideTip()
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

    override fun systemMsg(msg: Result) {
        showTip(msg.msg)
    }

    override fun workChanged(msg: GGateway.SCWorkerChanged) {
        if (msg.workerId != 0){
           viewModel.loadWorker(msg.workerId)
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

    private fun updateWorkInf(workerInfo: WorkerInfo){
            binding?.let {
                it.tvTitle.text = "${workerInfo.workerName}"
                if(!sayHello) {
                    //sendLocalMsg("您好，请问有什么可以帮到您！")
                    sayHello = true
                }

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