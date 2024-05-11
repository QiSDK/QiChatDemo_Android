package com.teneasy.chatuisdk.ui.http.bean

import com.google.gson.annotations.SerializedName


data class AutoReply (

  @SerializedName("id"       ) var id       : String?       = null,
  @SerializedName("title"    ) var title    : String?       = null,
  @SerializedName("delaySec" ) var delaySec : Int?          = null,
  @SerializedName("qa"       ) var qa       : ArrayList<Qa> = arrayListOf()

)