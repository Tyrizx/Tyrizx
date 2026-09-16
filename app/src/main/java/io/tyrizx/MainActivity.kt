package io.tyrizx

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    external fun startNodeWithArguments(arguments: Array<String>)

    companion object {
        init {
            System.loadLibrary("node")
            System.loadLibrary("tyrizx-native")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                loadWithOverviewMode = true
                useWideViewPort = true
            }
            webViewClient = WebViewClient()
        }

        setContentView(webView)

        Thread {
            startNodeWithArguments(arrayOf(
                "node",
                "${filesDir.absolutePath}/nodejs-project/main.js"
            ))
        }.start()

        webView.postDelayed({
            webView.loadUrl("http://127.0.0.1:8080")
        }, 15000)
    }
}
