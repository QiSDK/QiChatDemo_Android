package com.teneasy.chatuisdk.ui.http.bean

import com.google.gson.annotations.SerializedName


data class Request (

  @SerializedName("chatId"      ) var chatId      : String?  = null,
  @SerializedName("msgId"       ) var msgId       : String?  = null,
  @SerializedName("count"       ) var count       : Int?     = null,
  @SerializedName("withLastOne" ) var withLastOne : Boolean? = null,
  @SerializedName("workerId"    ) var workerId    : Int?     = null,
  @SerializedName("consultId"   ) var consultId   : Int?     = null,
  @SerializedName("userId"      ) var userId      : Int?     = null

)