package com.teneasy.chatuisdk.ui.base;

import android.util.Log;


import com.teneasy.sdk.BuildConfig;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 日志接口
 *
 * @author Administrator
 * @version [版本号]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */
public class CfLog {

    private CfLog() {
    }

    public static boolean isDebuggable() {
        return BuildConfig.DEBUG;
    }


    public static void e(String tag, String message) {
        if (!isDebuggable())
            return;
        Log.e(tag, message);
    }


    public static void w(String tag, String message) {
        if (!isDebuggable())
            return;
        Log.w(tag, message);
    }


    public static void i(String tag, String message) {
        if (!isDebuggable())
            return;
        Log.i(tag, message);
    }


    public static void d(String message, String args) {
        if (!isDebuggable())
            return;
        Log.d(message, args.toString());
    }

}
