package com.teneasy.chatuisdk.ui.http.bean

import com.google.gson.annotations.SerializedName

class TextBody {
    @SerializedName("content" ) var content : String? = null
    @SerializedName("image"   ) var image   : String? = null
    @SerializedName("video"   ) var video   : String? = null
    @SerializedName("color"   ) var color   : String? = null
}


 class TextImages {
    val message: String = ""
    val imgs: List<String> = listOf()
}