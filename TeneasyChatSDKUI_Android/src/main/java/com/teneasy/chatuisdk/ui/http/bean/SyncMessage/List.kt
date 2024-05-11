package com.teneasy.chatuisdk.ui.http.bean

import com.google.gson.annotations.SerializedName


data class List (

  @SerializedName("chatId"           ) var chatId           : String?           = null,
  @SerializedName("msgId"            ) var msgId            : String?           = null,
  @SerializedName("msgTime"          ) var msgTime          : String?           = null,
  @SerializedName("sender"           ) var sender           : String?           = null,
  @SerializedName("replyMsgId"       ) var replyMsgId       : String?           = null,
  @SerializedName("msgOp"            ) var msgOp            : String?           = null,
  @SerializedName("worker"           ) var worker           : Int?              = null,
  @SerializedName("autoReplyFlag"    ) var autoReplyFlag    : AutoReplyFlag?    = AutoReplyFlag(),
  @SerializedName("msgFmt"           ) var msgFmt           : String?           = null,
  @SerializedName("consultId"        ) var consultId        : String?           = null,
  @SerializedName("content"          ) var content          : Content?          = Content(),
  @SerializedName("image"            ) var image            : Image?            = Image(),
  @SerializedName("audio"            ) var audio            : Audio?            = Audio(),
  @SerializedName("video"            ) var video            : Video?            = Video(),
  @SerializedName("geo"              ) var geo              : Geo?              = Geo(),
  @SerializedName("file"             ) var file             : File?             = File(),
  @SerializedName("workerTrans"      ) var workerTrans      : WorkerTrans?      = WorkerTrans(),
  @SerializedName("blacklistApply"   ) var blacklistApply   : BlacklistApply?   = BlacklistApply(),
  @SerializedName("blacklistConfirm" ) var blacklistConfirm : BlacklistConfirm? = BlacklistConfirm(),
  @SerializedName("autoReply"        ) var autoReply        : AutoReply?        = AutoReply(),
  @SerializedName("workerChanged"    ) var workerChanged    : WorkerChanged?    = WorkerChanged()

)