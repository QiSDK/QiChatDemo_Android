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
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

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
                val url = URL(pdfUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()
                
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    runOnUiThread {
                        displayPdfFromStream(inputStream)
                    }
                } else {
                    runOnUiThread {
                        textView.visibility = View.VISIBLE
                        textView.text = "下载PDF文件失败，错误码: ${connection.responseCode}"
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
    
    private fun displayPdfFromStream(inputStream: InputStream) {
        pdfView.fromStream(inputStream)
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
