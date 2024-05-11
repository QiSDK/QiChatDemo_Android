package com.teneasy.chatuisdk.ui.http.bean

import com.google.gson.annotations.SerializedName


data class WorkerChanged (

  @SerializedName("workerClientId" ) var workerClientId : String? = null,
  @SerializedName("workerId"       ) var workerId       : Int?    = null,
  @SerializedName("name"           ) var name           : String? = null,
  @SerializedName("avatar"         ) var avatar         : String? = null,
  @SerializedName("greeting"       ) var greeting       : String? = null,
  @SerializedName("State"          ) var State          : String? = null,
  @SerializedName("consultId"      ) var consultId      : String? = null

)