package com.teneasy.chatuisdk.ui.base



const val PARAM_USER_ID = "USER_ID"
const val PARAM_CERT = "CERT"
const val PARAM_MERCHANT_ID = "MERCHANT_ID"
const val PARAM_XTOKEN = "HTTPTOKEN"
const val PARAM_LINES = "LINES"

const val PARAM_DOMAIN = "wssBaseUrl"

class Constants {

    companion object {
     //这部分是在设置里面获取的
        var lines = "https://csapi.xdev.stream,https://wcsapi.qixin14.xyz,https://wcsapi.qixin14.xyz"
        var cert = "COYBEAUYASDyASiG2piD9zE.te46qua5ha2r-Caz03Vx2JXH5OLSRRV2GqdYcn9UslwibsxBSP98GhUKSGEI0Z84FRMkp16ZK8eS-y72QVE2AQ"
        var merchantId = 230
        var userId = 666663//1125324

//       var lines = "https://csapi.xdev.stream,https://wcsapi.qixin14.xyz,https://wcsapi.qixin14.xyz"
//        var cert = "COEBEAUYASDjASiewpj-8TE.-1R9Mw9xzDNrSxoQ5owopxciklACjBUe43NANibVuy-XPlhqnhAOEaZpxjvTyJ6n79P5bUBCGxO7PcEFQ9p9Cg"
//        var merchantId = 225
//        var userId = 777772

        /*雷哥的测试环境*/
//var lines = "https://csapi.xdev.stream,https://wcsapi.qixin14.xyz,https://wcsapi.qixin14.xyz"
//var cert = "COgBEAUYASDzASitlJSF9zE.5uKWeVH-7G8FIgkaLIhvzCROkWr4D3pMU0-tqk58EAQcLftyD2KBMIdYetjTYQEyQwWLy7Lfkm8cs3aogaThAw"
//var merchantId = 232
//var userId = 364310


     //运行时候生成的
         var xToken = ""
        //聊天SDK所需要的域名，例如www.abc.com，没有https前缀
        var domain = ""  //domain
        //val baseUrlApi = "https://" + domain  //用于请求数据，上传图片
        const val baseUrlImage = "https://sssacc.wwc09.com" //用于拼接图片地址
        var workerId = 0
        var CONSULT_ID: Long = 1
        //val wss_token = "wss_token"

        fun baseUrlApi() : String {
            val baseUrlApi = "https://" + domain
            return baseUrlApi
        }
    }

    /*
    安卓demo问题：

    1.无法接受起聊发送过来的视频，显示空消息

     */
}