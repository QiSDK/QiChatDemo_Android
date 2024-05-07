package com.teneasy.chatuisdk.ui.base

class Constants {


    //开发环境

    //线上环境
    //  val baseUrlImage = "https://images2acc.wwc09.com" //用于拼接图片地址
    //  val baseUrlApi = "https://csapi.ertw.xyz"//用于请求数据，上传图片
    //测试环境

    var uId = 123;
    companion object {
         var CONSULT_ID: Long = 123
         var httpToken = "COYBEAUYASDwASja5o2V9DE.9Fhv9o1HueJOkqzylMJoUggw7PjsoBtF38-vncusatONba9rgIv3LcrMZj7kjTA_79IvBOYpGTx-ygEt2wpSDA"
         var cert = "COYBEAUYASDwASja5o2V9DE.9Fhv9o1HueJOkqzylMJoUggw7PjsoBtF38-vncusatONba9rgIv3LcrMZj7kjTA_79IvBOYpGTx-ygEt2wpSDA"
        val baseUrl = "https://csapi.hfxg.xyz"  //用于大多数Api
        val baseUrlApi = "https://csapi.xdev.stream"  //用于请求数据，上传图片
        val baseUrlImage = "https://sssacc.wwc09.com" //用于拼接图片地址


        val wss_token = "wss_token"
    }
}