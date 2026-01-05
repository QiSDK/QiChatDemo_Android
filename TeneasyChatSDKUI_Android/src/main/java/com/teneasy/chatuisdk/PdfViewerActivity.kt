package com.teneasy.chatuisdk

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class PdfViewerActivity : AppCompatActivity(), OnPageChangeListener, OnLoadCompleteListener,
    OnPageErrorListener {
    
    private lateinit var pdfView: PDFView
    private lateinit var textView: TextView
    private var pageNumber = 0
    
    companion object {
        private const val TAG = "PdfViewerActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_viewer)
        
        pdfView = findViewById(R.id.pdfView)
        textView = findViewById(R.id.textView)
        
        val pdfUrl = intent.getStringExtra("pdf_url")
        val pdfTitle = intent.getStringExtra("pdf_title") ?: "PDF文档"
        
        title = pdfTitle
        
        if (pdfUrl != null) {
            downloadAndOpenPdf(pdfUrl)
        } else {
            textView.visibility = View.VISIBLE
            textView.text = "PDF文件路径无效"
        }
    }
    
    private fun downloadAndOpenPdf(pdfUrl: String) {
        Thread {
            try {
                // 检查本地是否已下载
                val localFile = getLocalPdfFile(pdfUrl)

                if (localFile.exists()) {
                    Log.d(TAG, "PDF已存在本地缓存: ${localFile.absolutePath}")
                    runOnUiThread {
                        displayPdfFromFile(localFile)
                    }
                } else {
                    // 下载PDF
                    Log.d(TAG, "开始下载PDF: $pdfUrl")
                    val url = URL(pdfUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connect()

                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val inputStream = connection.inputStream

                        // 保存到本地
                        saveToLocal(inputStream, localFile)

                        runOnUiThread {
                            displayPdfFromFile(localFile)
                        }
                    } else {
                        runOnUiThread {
                            textView.visibility = View.VISIBLE
                            textView.text = "下载PDF文件失败，错误码: ${connection.responseCode}"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "下载PDF文件时出错", e)
                runOnUiThread {
                    textView.visibility = View.VISIBLE
                    textView.text = "下载PDF文件时出错: ${e.message}"
                }
            }
        }.start()
    }

    /**
     * 获取本地PDF文件路径
     */
    private fun getLocalPdfFile(pdfUrl: String): File {
        val cacheDir = File(cacheDir, "pdfs")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        // 使用URL的MD5作为文件名
        val fileName = pdfUrl.toMD5() + ".pdf"
        return File(cacheDir, fileName)
    }

    /**
     * 保存输入流到本地文件
     */
    private fun saveToLocal(inputStream: InputStream, file: File) {
        try {
            val outputStream = FileOutputStream(file)
            val buffer = ByteArray(8192)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }

            outputStream.close()
            inputStream.close()
            Log.d(TAG, "PDF已保存到本地: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "保存PDF到本地失败", e)
            throw e
        }
    }

    /**
     * 字符串转MD5
     */
    private fun String.toMD5(): String {
        val bytes = MessageDigest.getInstance("MD5").digest(this.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    private fun displayPdfFromFile(file: File) {
        pdfView.fromFile(file)
            .defaultPage(pageNumber)
            .onPageChange(this)
            .enableAnnotationRendering(true)
            .onLoad(this)
            .scrollHandle(DefaultScrollHandle(this))
            .spacing(10) // in dp
            .onPageError(this)
            .load()
    }
    
    override fun loadComplete(nbPages: Int) {
        textView.visibility = View.GONE
        pdfView.visibility = View.VISIBLE
        textView.text = "PDF加载完成，共 $nbPages 页"
        Log.d(TAG, "PDF加载完成，共 $nbPages 页")
    }
    
    override fun onPageChanged(page: Int, pageCount: Int) {
        pageNumber = page
        title = "PDF文档 (${page + 1} / $pageCount)"
    }
    
    override fun onPageError(page: Int, t: Throwable?) {
        Log.e(TAG, "PDF页面加载错误 page=$page", t)
        textView.visibility = View.VISIBLE
        textView.text = "PDF页面加载错误: ${t?.message}"
    }
}
