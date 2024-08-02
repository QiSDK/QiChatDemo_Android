package com.android.common.bean.chat

import androidx.databinding.BaseObservable

data class ChatExpressionPanel(
   // val model: StickerBean? = null,
    val type: String,
    var select: Boolean,
    var index: Int? = 0
) :
    BaseObservable()


data class ChatExpressionNavigation(
    val model: String? = null,
    val type: String,
    var select: Boolean
) :
    BaseObservable()

enum class ChatExpressionNavigationType(){
    CUSTOM,
    COLLECT,
    OTHER
}
