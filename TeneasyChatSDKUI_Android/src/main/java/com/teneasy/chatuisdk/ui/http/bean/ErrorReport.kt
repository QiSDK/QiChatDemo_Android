package com.teneasy.chatuisdk.ui.http.bean

data class ErrorReport(
    val data: ArrayList<ErrorItem>
)

data class ErrorItem(
    var url: String,
    var code: Int,
    var payload: String = "",
    //val ip: String,
    var platform: Int,
    var created_at: String,
    //var tenantId: Int = 0
)

class ErrorPayload  (
    var header: String? = "",
    var request: String? = "",
    var resp: String? = ""
)