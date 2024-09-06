package com.teneasy.chatuisdk.ui.http.bean

data class ErrorReport(
    val data: List<DataItem>
)

data class DataItem(
    val url: String,
    val code: Int,
    val payload: String,
    //val ip: String,
    val platform: Int,
    val created_at: String
)

