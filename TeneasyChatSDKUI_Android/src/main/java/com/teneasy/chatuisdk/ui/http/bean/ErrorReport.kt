package com.teneasy.chatuisdk.ui.http.bean

data class ErrorReport(
    val data: ArrayList<ErrorItem>
)

data class ErrorItem(
    val url: String,
    val code: Int,
    val payload: String,
    //val ip: String,
    val platform: Int,
    val created_at: String
)

class ErrorPayload  (
    val header: String?,
    val request: String?,
    val body: String?
)