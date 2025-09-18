# TODO

- [ ] 整理配置管理方案（移除 Constants 中的硬编码，规划 BuildConfig/安全存储）
- [ ] 拆分 KeFuFragment：连接管理、消息流、上传、UI 分层
- [ ] 统一上传与网络通道，淘汰 UploadUtilWithProgress 硬编码实现
- [✅] 重构 SelectConsultTypeAdapter，迁移业务逻辑到 Fragment/ViewModel
- [✅] (high) 清理 GlobalScope 与 TimerTask，采用 lifecycleScope/viewModelScope 与协程流
- [ ] 引入测试与代码规范（ktlint/detekt、核心用例单元/仪器测试）
