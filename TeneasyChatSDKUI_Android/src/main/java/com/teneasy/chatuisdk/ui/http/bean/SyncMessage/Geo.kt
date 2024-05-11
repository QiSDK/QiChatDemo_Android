package com.teneasy.chatuisdk.ui.http.bean

import com.google.gson.annotations.SerializedName


data class Geo (

  @SerializedName("longitude" ) var longitude : String? = null,
  @SerializedName("latitude"  ) var latitude  : String? = null

)