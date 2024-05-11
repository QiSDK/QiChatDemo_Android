package com.teneasy.chatuisdk.ui.http.bean

import com.google.gson.annotations.SerializedName


data class AutoReplyFlag (

  @SerializedName("id"   ) var id   : String? = null,
  @SerializedName("qaId" ) var qaId : Int?    = null

)