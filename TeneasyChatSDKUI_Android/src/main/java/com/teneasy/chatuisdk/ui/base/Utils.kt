package com.teneasy.chatuisdk.ui.base

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.google.protobuf.Timestamp
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.net.NetworkInterface

class Utils {

    companion object {
        var localDateFormat = "yyyy-MM-dd HH:mm:ss"
        var TAG = "Utils"
    }

    fun copyText(text: String, context: Context) {
        // Get the system clipboard service
        val clipboardManager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

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
        Constants.baseUrlImage =
            UserPreferences().getString(PARAM_IMAGEBASEURL, Constants.baseUrlImage)
        Constants.maxSessionMins = UserPreferences().getInt(PARAM_MAXSESSIONMINS, Constants.maxSessionMins);

        Constants.userName = UserPreferences().getString(PARAM_USERNAME, Constants.userName)
        Constants.userLevel = UserPreferences().getInt(PARAM_USER_LEVEL, Constants.userLevel)
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
            val dateFormat =
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'", Locale.getDefault())
            //dateFormat.timeZone = TimeZone.getDefault()
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            date = dateFormat.parse(datetimeString)
        } catch (ex: Exception) {

        }
        return date
    }

    fun differenceInMinutes(date1: Date, date2: Date): Long {
        val diffInMillis = date2.time - date1.time
        return TimeUnit.MILLISECONDS.toMinutes(diffInMillis)
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
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val date = Date(timestamp)
        return sdf.format(date)
    }

    fun getBitmapFromFile(imageFile: File): Bitmap {
        return BitmapFactory.decodeFile(imageFile.absolutePath)
    }

    fun saveImageToGallery(context: Context, f: File) {
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_STARTED)
        //val f = File(photoPath)
        val contentUri = Uri.fromFile(f)
        mediaScanIntent.setData(contentUri)
        context.sendBroadcast(mediaScanIntent)
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
            Log.d("chatLibUtils", "Failed to save compressed bitmap to file")
        }
    }

    fun encodeFilePath(filePath: String): String {
        return URLEncoder.encode(filePath, StandardCharsets.UTF_8.toString())
    }

    fun timestampToString(timestamp: Timestamp): String {
        // Convert the protobuf Timestamp to milliseconds
        val millis = timestamp.seconds * 1000 + timestamp.nanos / 1_000_000

        // Create a Date object from milliseconds
        val date = Date(millis)

        // Create a SimpleDateFormat object for the desired format
        val sdf = SimpleDateFormat(localDateFormat, Locale.getDefault())

        // Set the timezone to UTC to avoid any timezone offset issues
        sdf.timeZone = TimeZone.getDefault()

        // Format the date to the desired string format
        return sdf.format(date)
    }

    /**
     * Creates a bitmap thumbnail from video uri of the scheme type content://
     */
    fun getVideoThumb(context: Context, uri: Uri): Bitmap? {
        try {
            // Load thumbnail of a specific media item.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val thumbnail: Bitmap =
                    context.contentResolver.loadThumbnail(
                        uri, Size(640, 480), null
                    )
                return thumbnail
            } else {
                val mediaMetadataRetriever = MediaMetadataRetriever()
                mediaMetadataRetriever.setDataSource(context, uri)
                return mediaMetadataRetriever.frameAtTime
            }
        } catch (ex: Exception) {
//            Toast
//                .makeText(context, "从图像获取缩略图失败", Toast.LENGTH_SHORT)
//                .show()
        }
        return null

    }

    /**
     * Value of dp to value of px.
     *
     * @param dpValue The value of dp.
     * @return value of px
     */
    fun dp2px(dpValue: Float): Int {
        val scale = Resources.getSystem().displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    /**
     * Value of px to value of dp.
     *
     * @param pxValue The value of px.
     * @return value of dp
     */
    fun px2dp(pxValue: Float): Int {
        val scale = Resources.getSystem().displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }

    fun drawableToBitmap(drawable: Drawable): Bitmap {
        var bitmap: Bitmap? = null

        if (drawable is BitmapDrawable) {
            val bitmapDrawable = drawable
            if (bitmapDrawable.bitmap != null) {
                return bitmapDrawable.bitmap
            }
        }

        bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            Bitmap.createBitmap(
                1,
                1,
                Bitmap.Config.ARGB_8888
            ) // Single color bitmap will be created of 1x1 pixel
        } else {
            Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
        }

        val canvas: Canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight())
        drawable.draw(canvas)
        return bitmap
    }

    fun updateLayoutParams(relativeLayout: RelativeLayout, newWidth: Int, newHeight: Int) {
        val layoutParams = relativeLayout.layoutParams
        layoutParams.width = newWidth
        layoutParams.height = newHeight
        relativeLayout.layoutParams = layoutParams
    }

    fun resizePhoto(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val aspRat = w / h
        val W = 400
        val H = W * aspRat
        val b = Bitmap.createScaledBitmap(bitmap, W, H, false)
        return b
    }

    fun getNowTimeStamp(): Timestamp {
        val d = Timestamp.newBuilder()
        val cal = Calendar.getInstance()
        cal.time = Date()

        val millis = cal.timeInMillis
        d.seconds = (millis * 0.001).toLong()
        //d.nanos = System.nanoTime().toInt();
        return d.build()
    }

    suspend fun compressVideo(inputFilePath: String, outputFilePath: String): Int {
        println("FFmpeg 开始压缩")
        return withContext(Dispatchers.IO) {
            // Define the FFmpeg command to compress the video
            val command = arrayOf(
                "-i",
                inputFilePath,       // Input file path
                "-c:v",
                "libx264",           // Video codec (H.264)
                "-pix_fmt",
                "yuv420p",           // Pixel format (yuv420p is widely compatible)
                "-crf",
                "28",                // Compression level (lower value = higher quality, larger file)
                "-preset",
                "fast",              // Compression speed
                outputFilePath       // Output file path
            )

            // Execute the command
            val session: FFmpegSession = FFmpegKit.execute(command.joinToString(" "))

            // Capture the result of the execution
            val returnCode = session.returnCode

            // Check if the command was successful
            if (returnCode.isValueSuccess) {
                0  // Success
            } else {
                // Log the error message or handle it accordingly
                println("FFmpeg 压缩失败: $returnCode")
                -1  // Failure
            }
        }
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return when {
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }


    fun getIPAddress(): String? {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            if (!networkInterface.isUp || networkInterface.isLoopback || networkInterface.isVirtual) {
                continue
            }
            val addresses = networkInterface.inetAddresses
            while (addresses.hasMoreElements()) {
                val address = addresses.nextElement()
                if (!address.isLoopbackAddress) { // IPv4 and not loopback
                    return address.hostAddress
                }
            }
        }
        return null
    }

    fun downloadVideo(url: String, onProgress: (progress: Int) -> Unit) {
        val client = OkHttpClient()

        val request = Request.Builder()
            .url(url)
            .build()

        Thread {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                onProgress(-1)
                throw Exception("Failed to download file: ${response.code}")
            }

            var fileName = url.split("/").last();

            var outputFilePath = Environment.DIRECTORY_DOWNLOADS + "/" + fileName
            val body = response.body ?: throw Exception("Response body is null")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    //put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val resolver = ApplicationExt.context?.contentResolver
                val uri = resolver?.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                uri?.let { downloadUri ->
                    resolver.openOutputStream(downloadUri)?.use { outputStream ->
                        outputStream.write(body.bytes())
                        onProgress(100)
                    }
                }
            }
        }.start()
    }

    @Throws(IOException::class)
     fun uriToFile(context: Context, uri: Uri, fileName: String): File {
        val contentResolver = context.contentResolver
        val TEMP_FILE_PREFIX = "temp_file_"
        val TEMP_FILE_SUFFIX = "." + fileName.split(".").last() //".tmp"
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val tempFile = File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX, context.cacheDir)
        tempFile.deleteOnExit()

        inputStream?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IOException("Could not open input stream for URI: $uri")

        return tempFile
    }

    /**
     * Retrieves and prints the display name of a file from a given URI using ContentResolver.
     *
     * @param contentResolver The ContentResolver instance to use for querying.
     * @param fileUri The URI of the file to query.
     */
    fun getFileNameFromUri(contentResolver: ContentResolver, fileUri: Uri) : String {
        // Define the projection (columns to retrieve) explicitly.
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        // Use a try-with-resources equivalent (use) for proper cursor management.
        contentResolver.query(fileUri, projection, null, null, null)?.use { cursor ->
            // Check if the cursor is valid and has at least one row.
            if (cursor.moveToFirst()) {
                // Get the column index safely.
                val displayNameColumnIndex = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                // Retrieve the display name.
                val displayName = cursor.getString(displayNameColumnIndex)
                // Log the file name.
                println("Selected file: $displayName")
                return displayName
            } else {
                println("No file found at the specified URI.")
                return ""
            }
        } ?: run {
            println("Failed to query the content resolver for the given URI.")
            return ""
        }
    }

    /**
     * Opens a PDF file from a given URI in an external browser.
     *
     * @param context The context to use for launching the intent.
     * @param pdfUri The URI of the PDF file to open.
     */
    fun openPdfInBrowser(context: Context, pdfUri: Uri) {
        try {
            // Create an intent to view the PDF file.
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = pdfUri
                //setDataAndType(pdfUri, getMimeType(pdfUri.path?.split(".")?.lastOrNull() ?: "*/*"))
                //setDataAndType(pdfUri, getMimeType(pdfUri.path?.split(".")?.lastOrNull() ?: ""))
                //flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            // Check if there's an app that can handle the intent.
            if (intent.resolveActivity(context.packageManager) != null) {
                // Start the activity.
                ContextCompat.startActivity(context, intent, null)
            } else {
                // No app found to handle the intent.
                Log.e(TAG, "No PDF viewer app found.")
                Toast.makeText(context, "No viewer app found.", Toast.LENGTH_SHORT).show()
                // Optionally, you can show a message to the user here.
            }
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "No PDF viewer app found.", e)
            Toast.makeText(context, "No viewer app found.", Toast.LENGTH_SHORT).show()
            // Optionally, you can show a message to the user here.
        } catch (e: Exception) {
            Log.e(TAG, "Error opening PDF in browser.", e)
            // Optionally, you can show a message to the user here.
        }
    }

    private fun getMimeType2(ext: String): String {
        return "application/octet-stream";
//        return when (ext.lowercase()) {
//            "pdf" -> "application/pdf"
//            "doc", "docx" -> "application/msword"
//            "xls", "xlsx" -> "application/vnd.ms-excel"
//            "ppt", "pptx" -> "application/vnd.ms-powerpoint"
//            else -> "application/octet-stream" // Default MIME type
//        }
    }

    private fun getMimeType(ext: String): String {
        return when (ext.lowercase()) {
            "pdf" -> "application/pdf"
            "doc", "docx" -> "application/msword"
            "xls", "xlsx" -> "application/vnd.ms-excel"
            else -> "*/*"
        }
    }
}