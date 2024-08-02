package com.teneasy.chatuisdk

import com.google.gson.annotations.SerializedName


data class Consults (

  @SerializedName("consultId" ) var consultId : Long?             = null,
  @SerializedName("name"      ) var name      : String?          = null,
  @SerializedName("guide"     ) var guide     : String?          = null,
  @SerializedName("Works"     ) var Works     : ArrayList<Works> = arrayListOf(),
  @SerializedName("unread"    ) var unread    : Int?             = null,
  @SerializedName("priority"  ) var priority  : Int?             = null

)