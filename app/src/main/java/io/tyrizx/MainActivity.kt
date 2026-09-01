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
import java.io.InputStream

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
        Log.d("Tyrizx", "Server dir: ${serverDir.absolutePath}")

        if (!serverDir.exists()) {
            Log.d("Tyrizx", "Extracting assets...")
            try {
                copyAssets("code-server", serverDir)
                Log.d("Tyrizx", "Extraction complete.")
            } catch (e: IOException) {
                Log.e("Tyrizx", "Extraction failed: ${e.message}")
                e.printStackTrace()
                return
            }
        }

        // Check if code-server binary exists
        val serverBinary = File(serverDir, "bin/code-server")
        if (!serverBinary.exists()) {
            Log.e("Tyrizx", "code-server binary not found at ${serverBinary.absolutePath}")
            return
        }

        Log.d("Tyrizx", "Binary exists, setting executable...")
        serverBinary.setExecutable(true)

        // Create user data dir
        val userDataDir = File(filesDir, ".tyrizx-userdata")
        userDataDir.mkdirs()

        Log.d("Tyrizx", "Starting server...")
        val processBuilder = ProcessBuilder(
            serverBinary.absolutePath,
            "--port", "8080",
            "--host", "127.0.0.1",  // Bind to loopback only
            "--auth", "none",
            "--user-data-dir", userDataDir.absolutePath
        )
        processBuilder.directory(serverDir)
        processBuilder.environment()["LD_LIBRARY_PATH"] = "/system/lib64:${serverDir.absolutePath}"
        processBuilder.redirectErrorStream(true)

        try {
            serverProcess = processBuilder.start()

            // Read stdout/stderr in a separate thread to avoid blocking
            Thread {
                val reader = BufferedReader(InputStreamReader(serverProcess?.inputStream ?: InputStream.nullInputStream()))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    Log.d("Tyrizx", "Server: $line")
                }
            }.start()

        } catch (e: Exception) {
            Log.e("Tyrizx", "Failed to start server: ${e.message}")
            e.printStackTrace()
            return
        }

        // Wait for server to start, then load WebView
        webView.postDelayed({
            Log.d("Tyrizx", "Loading WebView at http://127.0.0.1:8080")
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
