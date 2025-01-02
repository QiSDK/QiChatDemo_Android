package com.teneasy.chatuisdk.ui.http.bean.ChatHistory

import com.teneasy.chatuisdk.ui.http.Urls

data class ChatHistory (
  val request: Request,
  val list: List<list>,
  val replyList: List<list>,
  val lastMsgID: String,

  val nick: String
)

data class list (
  val chatId: String,

  val msgId: String,

  val msgTime: String,
  val sender: String,

  val replyMsgId: String?,

  val msgOp: String,
  val worker: Long,
  //val autoReplyFlag: AutoReplyFlag,
  val msgFmt: String,

  val consultID: String,

  val content: Content?,
  val image: Audio?,
  val audio: Audio?,
  val video: Urls?,
  val geo: Geo,
  val file: File?,
  val workerTrans: WorkerTrans,
  val blacklistApply: Blacklist,
  val blacklistConfirm: Blacklist,
  val autoReply: AutoReply,
  val workerChanged: WorkerChanged?
)

data class Audio (
  val uri: String
)

data class AutoReply (
  val id: String,
  val title: String,

  val delaySEC: Long,

  val qa: List<QA>
)

data class QA (
  val id: Long,
  val question: Question,
  val answer: List<Question>
)

data class Question (
  val content: Content,
  val image: Audio,
  val audio: Audio,
  val video: Audio,
  val geo: Geo,
  val file: File
)

data class Content (
  val data: String
)

data class File (
  val uri: String,
  val fileName: String,
  val size: Long
)

data class Geo (
  val longitude: String,
  val latitude: String
)

data class AutoReplyFlag (
  val id: String,

  val qaID: Long
)

data class Blacklist (
  val workerID: Int
)

data class WorkerChanged (
  val workerClientID: String,

  val workerID: Int,

  val name: String,
  val avatar: String,
  val greeting: String,

  val state: String,

  val consultID: String
)

data class WorkerTrans (
  val workerID: Int,

  val workerName: String,
  val workerAvatar: String,

  val consultID: Int
)

data class Request (
  val chatId: Long,

  val msgId: String,

  val count: Int,
  val withLastOne: Boolean,

  val workerId: Int,

  val consultId: Long,

  val userId: Int
)

