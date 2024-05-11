package com.teneasy.chatuisdk.ui.http.bean

import com.google.gson.annotations.SerializedName


data class Qa (

  @SerializedName("id"       ) var id       : Int?              = null,
  @SerializedName("question" ) var question : Question?         = Question(),
  @SerializedName("answer"   ) var answer   : ArrayList<Answer> = arrayListOf()

)