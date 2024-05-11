package com.teneasy.chatuisdk.ui.http.bean

import com.google.gson.annotations.SerializedName


data class WorkerTrans (

  @SerializedName("workerId"     ) var workerId     : Int?    = null,
  @SerializedName("workerName"   ) var workerName   : String? = null,
  @SerializedName("workerAvatar" ) var workerAvatar : String? = null,
  @SerializedName("consultId"    ) var consultId    : Int?    = null

)