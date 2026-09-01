package io.tyrizx

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var serverProcess: Process? = null

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
        startServerAndLoad()
    }

    private fun startServerAndLoad() {
        val serverDir = File(filesDir, "code-server")
        if (!serverDir.exists()) {
            try {
                copyAssets("code-server", serverDir)
            } catch (e: IOException) {
                e.printStackTrace()
                return
            }
        }

        val serverBinary = File(serverDir, "code-server")
        serverBinary.setExecutable(true)

        val processBuilder = ProcessBuilder(
            serverBinary.absolutePath,
            "--port", "8080",
            "--host", "127.0.0.1",
            "--auth", "none",
            "--user-data-dir", "${filesDir.absolutePath}/.tyrizx-userdata"
        )
        processBuilder.directory(serverDir)
        processBuilder.environment()["LD_LIBRARY_PATH"] = "/system/lib64:${serverDir.absolutePath}"
        serverProcess = processBuilder.start()

        val reader = BufferedReader(InputStreamReader(serverProcess?.inputStream))
        val output = reader.readText()
        Log.d("Tyrizx", "Server output: $output")

        webView.postDelayed({
            webView.loadUrl("http://127.0.0.1:8080")
        }, 8000)
    }

    private fun copyAssets(assetPath: String, targetDir: File) {
        val list = assets.list(assetPath) ?: return
        if (list.isEmpty()) {
            val outFile = File(targetDir, assetPath.substringAfterLast('/'))
            assets.open(assetPath).use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
        } else {
            targetDir.mkdirs()
            for (file in list) {
                val subPath = if (assetPath.isEmpty()) file else "$assetPath/$file"
                copyAssets(subPath, File(targetDir, file))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serverProcess?.destroy()
    }
}
