# TODO

- [ ] 把chatLib放到一个全局的地方，例如java/com/teneasy/chatuisdk/ui/main/ChatConnectionManager.kt里面或Constant.kt，目的是用户不管在哪个页面都能监听到消息。
- [ ]在Constant.kt, 只要用户不在聊天页面收到的消息，每收到1条消息这个consultId对应的未读数就+1。
可以参考 iOS版本/Users/xuefeng/Desktop/teneasy/QiChatDemo_iOS/TeneasyChatSDKUI_iOS/Classes/constant.swift 文件里面，这几个变量的使用：
public var unReadList: [UnReadItem] = []
public var globalMessageDelegate: GlobalMessageDelegate?
public var currentChatConsultId: Int64 = 0
- [ ] ChatLib的使用，可以参考已有的代码：java/com/teneasy/chatuisdk/ui/main/KeFuFragment.kt，第231行
- [ ] 在java/com/teneasy/chatuisdk/SelectConsultTypeFragment.kt客服咨询列表页面，已经有未读数的显示，但是如果当用户停留在这页面的时候，有消息收到，就更新列表里面的未读数。
- [ ]  创建这个文件
public struct UnReadItem {
    var consultId: Int64
    var unReadCount: Int
}
- [ ] 未读数消息持久化，只需在内存中维护；只要不是在聊天页面收到的消息，都计为未读数。
- [ ] 未读数消息清零：在进入聊天页面的时候清零


- [✅] 整理配置管理方案（移除 Constants 中的硬编码，规划 BuildConfig/安全存储）
- [✅] 重构 SelectConsultTypeAdapter，迁移业务逻辑到 Fragment/ViewModel
- [✅] 清理 GlobalScope 与 TimerTask，采用 lifecycleScope/viewModelScope 与协程流
- [ ] 暂时忽略此任务：引入测试与代码规范（ktlint/detekt、核心用例单元/仪器测试）
