package net.rift

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var serverProcess: Process? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create WebView programmatically
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

        // Set the WebView as the content view
        setContentView(webView)

        // Start the server and load the WebView
        startServerAndLoad()
    }

    private fun startServerAndLoad() {
        val serverDir = File(filesDir, "openvscode")
        if (!serverDir.exists()) {
            try {
                copyAssets("openvscode", serverDir)
            } catch (e: IOException) {
                e.printStackTrace()
                return
            }
        }

        val nodeBinary = File(serverDir, "node")
        nodeBinary.setExecutable(true)

        val serverScript = File(serverDir, "bin/openvscode-server")
        serverScript.setExecutable(true)

        val processBuilder = ProcessBuilder(
            serverScript.absolutePath,
            "--port", "8080",
            "--host", "127.0.0.1",
            "--without-connection-token",
            "--user-data-dir", "${filesDir.absolutePath}/.rift-userdata"
        )
        processBuilder.directory(serverDir)
        processBuilder.environment()["LD_LIBRARY_PATH"] = "/system/lib64:${serverDir.absolutePath}"
        serverProcess = processBuilder.start()

        webView.postDelayed({
            webView.loadUrl("http://127.0.0.1:8080")
        }, 3000)
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
