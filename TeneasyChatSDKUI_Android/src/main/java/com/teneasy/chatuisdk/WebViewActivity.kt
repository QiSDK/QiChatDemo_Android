package com.teneasy.chatuisdk

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import com.teneasy.chatuisdk.databinding.ActivityWebViewBinding

class WebViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWebViewBinding
    private var imageUrl: String? = ""
    private var kefuName: String? = ""

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageUrl = intent.getStringExtra(ARG_IMAGEURL)
        kefuName = intent.getStringExtra(ARG_KEFUNAME)

        // View Binding
        binding = ActivityWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // WebView settings
        val webSettings = binding.myWebView.settings
        webSettings.javaScriptEnabled = true // Enable JavaScript (if needed)
        webSettings.domStorageEnabled = true // Enable DOM storage (if needed)

        // Set a WebViewClient to handle page navigation within the WebView
        binding.myWebView.webViewClient = WebViewClient()

        // Set a WebChromeClient to handle JavaScript alerts, etc.
        binding.myWebView.webChromeClient = WebChromeClient()

        //val googleDocsUrl = "https://docs.google.com/gview?embedded=true&url=$imageUrl"
        var googleDocsUrl = "https://view.officeapps.live.com/op/view.aspx?src=$imageUrl"
        if (imageUrl?.split(".")?.last() == "pdf") {
            googleDocsUrl = "https://docs.google.com/gview?embedded=true&url=$imageUrl"
        }
        // Load a URL
        binding.myWebView.loadUrl(googleDocsUrl) // Replace with your URL
        binding?.tvTitle?.text = kefuName
        binding?.ivBack?.setOnClickListener {
            finish()
        }
    }

    override fun onBackPressed() {
        if (binding.myWebView.canGoBack()) {
            binding.myWebView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}