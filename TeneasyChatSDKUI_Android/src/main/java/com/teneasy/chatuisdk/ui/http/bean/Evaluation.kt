package com.teneasy.chatuisdk.ui.http.bean

data class EvaluationConfig(
    val evaluationEnabled: Boolean? = null,
    val configs: List<EvaluationScoreConfig>? = null,
    val triggerMessages: List<String>? = null
)

data class EvaluationScoreConfig(
    val content: String? = null,
    val score: Int? = null,
    val feedback: String? = null,
    val status: Int? = null
)

// status: 0=未评价 1=已评价 2=已关闭评价 3=空会话,无法评价
data class EvaluationStatus(
    val status: Int? = null
)
