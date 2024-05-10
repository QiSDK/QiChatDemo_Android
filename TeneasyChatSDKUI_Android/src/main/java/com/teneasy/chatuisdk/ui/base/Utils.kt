package com.teneasy.chatuisdk.ui.base

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

class Utils {

     fun copyText(text: String, context: Context){
        // Get the system clipboard service
        val clipboardManager =  context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        // Create a new ClipData object with the text
        val clipData = ClipData.newPlainText("Copied Text", text)

        // Set the primary clip on the clipboard
        clipboardManager.setPrimaryClip(clipData)

        //Toast.makeText(context, "复制成功", Toast.LENGTH_SHORT).show()
    }
}