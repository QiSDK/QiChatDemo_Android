package com.teneasy.chatuisdk.ui.http.bean

import com.google.gson.annotations.SerializedName


data class Data (

  @SerializedName("request"   ) var request   : Request?        = Request(),
  @SerializedName("list"      ) var list      : ArrayList<List> = arrayListOf(),
  @SerializedName("lastMsgId" ) var lastMsgId : String?         = null,
  @SerializedName("nick"      ) var nick      : String?         = null

)