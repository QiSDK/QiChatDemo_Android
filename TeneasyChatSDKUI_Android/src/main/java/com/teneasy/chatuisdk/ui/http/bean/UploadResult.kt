package com.teneasy.chatuisdk

import com.google.gson.annotations.SerializedName


data class UploadResult (

  @SerializedName("code"    ) var code    : Int?    = null,
  @SerializedName("message" ) var message : String? = null,
  @SerializedName("data"    ) var data    : FilePath?   = null



)

class FilePath (

  @SerializedName("filepath" ) var filepath : String? = null

)