package com.teneasy.chatuisdk.ui.base

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.DisplayMetrics
import android.view.View
import android.view.inputmethod.InputMethodManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


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

    fun readConfig() {
        Constants.xToken = UserPreferences().getString(PARAM_XTOKEN, Constants.xToken)
        Constants.domain = UserPreferences().getString(PARAM_DOMAIN, Constants.domain)
        Constants.userId = UserPreferences().getInt(PARAM_USER_ID, Constants.userId)
        Constants.merchantId = UserPreferences().getInt(PARAM_MERCHANT_ID, Constants.merchantId)
        Constants.lines = UserPreferences().getString(PARAM_LINES, Constants.lines)
        Constants.cert = UserPreferences().getString(PARAM_CERT, Constants.cert)
        Constants.baseUrlImage = UserPreferences().getString(PARAM_IMAGEBASEURL, Constants.baseUrlImage)
    }

         fun closeSoftKeyboard(view: View?) {
            if (view == null || view.windowToken == null) {
                return
            }
            val imm: InputMethodManager =
                view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }

    fun convertStrToDate(datetimeString: String): Date {
        //yyyy-MM-dd'T'HH:mm:sss'Z'
        var date = Date()
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'", Locale.getDefault())
             date = dateFormat.parse(datetimeString)
        }catch (ex:Exception){

        }
        return date
    }

    fun differenceInMinutes(date1: Date, date2: Date): Long {
        val diffInMillis = date2.time - date1.time
        return TimeUnit.MILLISECONDS.toMinutes(diffInMillis)
    }


    fun isMessageTimeDifferenceValid(lastMsgTime: Date?, sendingMsgTime: Date?, minutesDifference: Int): Boolean {
        if (lastMsgTime == null || sendingMsgTime == null) {
            return false
        }

        // Calculate the time 5 minutes before lastMsgTime
        val fiveMinutesInMillis = minutesDifference * 60 * 1000
        val lastMsgTimeMinusFiveMinutes = Date(lastMsgTime.time - fiveMinutesInMillis)

        // Compare the adjusted lastMsgTime with sendingMsgTime
        return lastMsgTimeMinusFiveMinutes > sendingMsgTime
    }

    fun sessionTimeout(lastMsgTime: Date?, sendingMsgTime: Date?, secondsDifference: Int): Boolean {
        if (lastMsgTime == null || sendingMsgTime == null) {
            return false
        }

        // Calculate the time 5 minutes before lastMsgTime
        val fiveMinutesInMillis = secondsDifference * 1000
        val lastMsgTimeMinusFiveMinutes = Date(lastMsgTime.time - fiveMinutesInMillis)

        // Compare the adjusted lastMsgTime with sendingMsgTime
        return lastMsgTimeMinusFiveMinutes > sendingMsgTime
    }


    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    fun convertDpToPixel(dp: Float, context: Context): Float {
        return dp * (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }

    fun getScreenWidth(): Int {
        return Resources.getSystem().getDisplayMetrics().widthPixels
    }

    fun getScreenHeight(): Int {
        return Resources.getSystem().getDisplayMetrics().heightPixels
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px A value in px (pixels) unit. Which we need to convert into db
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent dp equivalent to px value
     */
    fun convertPixelsToDp(px: Float, context: Context): Float {
        return px / (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }

    fun timestampToDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'")
        val date = Date(timestamp)
        return sdf.format(date)
    }

    fun saveBitmapToFile(file: File): File {
        try {
            // BitmapFactory options to downsize the image

            val o = BitmapFactory.Options()
            o.inJustDecodeBounds = true
            o.inSampleSize = 6

            // factor of downsizing the image
            var inputStream: FileInputStream = FileInputStream(file)
            //Bitmap selectedBitmap = null;
            BitmapFactory.decodeStream(inputStream, null, o)
            inputStream.close()

            // The new size we want to scale to
            val REQUIRED_SIZE = 75

            // Find the correct scale value. It should be the power of 2.
            var scale = 1
            while (o.outWidth / scale / 2 >= REQUIRED_SIZE &&
                o.outHeight / scale / 2 >= REQUIRED_SIZE
            ) {
                scale *= 2
            }

            val o2 = BitmapFactory.Options()
            o2.inSampleSize = scale
            inputStream = FileInputStream(file)

            val selectedBitmap = BitmapFactory.decodeStream(inputStream, null, o2)
            inputStream.close()

            // here i override the original image file
            file.createNewFile()
            val outputStream: FileOutputStream = FileOutputStream(file)

            selectedBitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)

            return file
        } catch (e: java.lang.Exception) {
            //Log.e("", e.message + " 压缩文件失败")
        }
        return  File("ddd")
    }

    fun getBitmapFromFile(imageFile: File): Bitmap {
        return BitmapFactory.decodeFile(imageFile.absolutePath)
    }

    fun compressBitmap(bitmap: Bitmap, quality: Int = 50): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return outputStream.toByteArray()
    }

    fun saveCompressedBitmapToFile(compressedData: ByteArray, outputFile: File) {
        try {
            val fos = FileOutputStream(outputFile)
            fos.write(compressedData)
            fos.flush()
            fos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}