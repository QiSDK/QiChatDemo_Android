package com.teneasy.chatuisdk.ui.base



const val PARAM_USER_ID = "USER_ID"
const val PARAM_CERT = "CERT"
const val PARAM_MERCHANT_ID = "MERCHANT_ID"
const val PARAM_XTOKEN = "HTTPTOKEN"
const val PARAM_LINES = "LINES"

const val PARAM_DOMAIN = "wssBaseUrl"

class Constants {

    //开发环境

    //线上环境
    //  val baseUrlImage = "https://images2acc.wwc09.com" //用于拼接图片地址
    //  val baseUrlApi = "https://csapi.ertw.xyz"//用于请求数据，上传图片
    //测试环境

    companion object {
     //这部分是在设置里面获取的
        /*var lines = "https://csapi.xdev.stream,https://wcsapi.qixin14.xyz,https://wcsapi.qixin14.xyz"
        var CONSULT_ID: Long = 1
        var cert = "COYBEAUYASDyASiG2piD9zE.te46qua5ha2r-Caz03Vx2JXH5OLSRRV2GqdYcn9UslwibsxBSP98GhUKSGEI0Z84FRMkp16ZK8eS-y72QVE2AQ"
        var merchantId = 230
        var userId = 1125324

         */


        var lines = "https://csapi.xdev.stream,https://wcsapi.qixin14.xyz,https://wcsapi.qixin14.xyz"
        var CONSULT_ID: Long = 1
        var cert = "COEBEAUYASDjASiewpj-8TE.-1R9Mw9xzDNrSxoQ5owopxciklACjBUe43NANibVuy-XPlhqnhAOEaZpxjvTyJ6n79P5bUBCGxO7PcEFQ9p9Cg"
        var merchantId = 225
        var userId = 1125345

     //运行时候生成的
         var xToken = ""
        var domain = "wcsapi.qixin14.xyz"  //domain
        val baseUrlApi = "https://" + domain  //用于请求数据，上传图片
        val baseUrlImage = "https://sssacc.wwc09.com" //用于拼接图片地址
        var workerId = 2
        //val wss_token = "wss_token"
    }
}