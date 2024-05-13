package com.teneasy.chatuisdk.ui.base


const val PARAM_WSS_BASE_URL = "wssBaseUrl"
class Constants {

    //开发环境

    //线上环境
    //  val baseUrlImage = "https://images2acc.wwc09.com" //用于拼接图片地址
    //  val baseUrlApi = "https://csapi.ertw.xyz"//用于请求数据，上传图片
    //测试环境

    companion object {
         var originConsultId: Long = 123
         var CONSULT_ID: Long = 123
         var httpToken = "COYBEAEYASDyASje0-aD9zE.7pv4F2mBEwiQSbNSzTB8nqt9uLHdAGwq_Oz9QjlcQoF2NGihilLq0ZawGv8bDkr8Huq_wsFy2bQWFYCrISqFAw"
         var cert = "COYBEAUYASDyASiG2piD9zE.te46qua5ha2r-Caz03Vx2JXH5OLSRRV2GqdYcn9UslwibsxBSP98GhUKSGEI0Z84FRMkp16ZK8eS-y72QVE2AQ"
        var baseUrl = "csapi.hfxg.xyz"  //用于大多数Api
        val baseUrlApi = "https://" + baseUrl  //用于请求数据，上传图片
        val baseUrlImage = "https://sssacc.wwc09.com" //用于拼接图片地址

        var merchantId = 230
        var userId = 1125324
        var workerId = 2
        val wss_token = "wss_token"
    }
}