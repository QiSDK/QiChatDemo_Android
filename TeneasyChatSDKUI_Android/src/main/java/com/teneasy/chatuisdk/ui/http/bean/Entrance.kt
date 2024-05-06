package com.teneasy.chatuisdk

import com.google.gson.annotations.SerializedName


data class Entrance (

  @SerializedName("name"              ) var name              : String?             = null,
  @SerializedName("nick"              ) var nick              : String?             = null,
  @SerializedName("avatar"            ) var avatar            : String?             = null,
  @SerializedName("guide"             ) var guide             : String?             = null,
  @SerializedName("defaultConsultId"  ) var defaultConsultId  : Int?                = null,
  @SerializedName("changeDefaultTime" ) var changeDefaultTime : String?             = null,
  @SerializedName("consults"          ) var consults          : ArrayList<Consults> = arrayListOf()

)