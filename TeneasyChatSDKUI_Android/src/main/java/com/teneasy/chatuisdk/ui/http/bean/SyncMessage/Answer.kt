package com.teneasy.chatuisdk.ui.http.bean

import com.google.gson.annotations.SerializedName


data class Answer (

  @SerializedName("content" ) var content : Content? = Content(),
  @SerializedName("image"   ) var image   : Image?   = Image(),
  @SerializedName("audio"   ) var audio   : Audio?   = Audio(),
  @SerializedName("video"   ) var video   : Video?   = Video(),
  @SerializedName("geo"     ) var geo     : Geo?     = Geo(),
  @SerializedName("file"    ) var file    : File?    = File()

)