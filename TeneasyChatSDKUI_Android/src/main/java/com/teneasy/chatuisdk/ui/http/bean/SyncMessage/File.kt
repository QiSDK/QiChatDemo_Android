package com.teneasy.chatuisdk.ui.http.bean

import com.google.gson.annotations.SerializedName


data class File (

  @SerializedName("uri"      ) var uri      : String? = null,
  @SerializedName("fileName" ) var fileName : String? = null,
  @SerializedName("size"     ) var size     : Int?    = null

)