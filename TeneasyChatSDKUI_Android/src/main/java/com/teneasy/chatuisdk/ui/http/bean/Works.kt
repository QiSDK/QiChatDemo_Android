package com.teneasy.chatuisdk

import com.google.gson.annotations.SerializedName


data class Works (

  @SerializedName("nick"         ) var nick         : String? = null,
  @SerializedName("avatar"       ) var avatar       : String? = null,
  @SerializedName("workerId"     ) var workerId     : Int?    = null,
  @SerializedName("nimId"        ) var nimId        : String? = null,
  @SerializedName("connectState" ) var connectState : String? = null,
  @SerializedName("onlineState"  ) var onlineState  : String? = null
)