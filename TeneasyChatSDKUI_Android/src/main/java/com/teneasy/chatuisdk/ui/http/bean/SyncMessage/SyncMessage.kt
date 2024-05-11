package com.teneasy.chatuisdk.ui.http.bean

import com.google.gson.annotations.SerializedName


data class SyncMessage (

  @SerializedName("code" ) var code : Int?    = null,
  @SerializedName("msg"  ) var msg  : String? = null,
  @SerializedName("data" ) var data : Data?   = Data()

)