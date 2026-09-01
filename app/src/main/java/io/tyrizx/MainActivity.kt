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
        val serverDir = File(filesDir, "openvscode")
        Log.d("Tyrizx", "Server dir: ${serverDir.absolutePath}")

        if (serverDir.exists()) {
            Log.d("Tyrizx", "Deleting existing folder...")
            serverDir.deleteRecursively()
        }

        Log.d("Tyrizx", "Extracting assets...")
        try {
            copyAssetsToDir("openvscode", serverDir)
            Log.d("Tyrizx", "Extraction complete.")
        } catch (e: IOException) {
            Log.e("Tyrizx", "Extraction failed: ${e.message}")
            e.printStackTrace()
            return
        }

        // Locate files extracted to internal data storage
        val nodeBinary = File(serverDir, "libnode.so")
        val serverMain = File(serverDir, "out/server-main.js")
        val libcShared = File(serverDir, "libc++_shared.so")

        if (!nodeBinary.exists()) {
            Log.e("Tyrizx", "libnode.so not found at ${nodeBinary.absolutePath}")
            return
        }
        if (!serverMain.exists()) {
            Log.e("Tyrizx", "server-main.js not found at ${serverMain.absolutePath}")
            return
        }

        // Fix file permissions explicitly so the system linker is authorized to parse them
        nodeBinary.setReadable(true, false)
        nodeBinary.setExecutable(true, false)
        if (libcShared.exists()) {
            libcShared.setReadable(true, false)
            libcShared.setExecutable(true, false)
        }

        Log.d("Tyrizx", "Starting server through system linker bypass...")

        // CRITICAL SELINUX BYPASS: Execute using system linker64
        val processBuilder = ProcessBuilder(
            "/system/bin/linker64",
            nodeBinary.absolutePath,
            serverMain.absolutePath,
            "--port", "8080",
            "--host", "127.0.0.1",
            "--without-connection-token"
        )
        processBuilder.directory(serverDir)

        // Configure Node runtime environment variables
        processBuilder.environment()["NODE_PATH"] = File(serverDir, "node_modules").absolutePath
        processBuilder.environment()["LD_LIBRARY_PATH"] = serverDir.absolutePath
        processBuilder.redirectErrorStream(true)

        try {
            serverProcess = processBuilder.start()
            Thread {
                val inputStream = serverProcess?.inputStream
                if (inputStream != null) {
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    var line: String?
                    try {
                        while (reader.readLine().also { line = it } != null) {
                            Log.d("Tyrizx", "Server: $line")
                        }
                    } catch (e: IOException) {
                        Log.e("Tyrizx", "Stream closed: ${e.message}")
                    }
                }
            }.start()
        } catch (e: Exception) {
            Log.e("Tyrizx", "Failed to start server: ${e.message}")
            e.printStackTrace()
            return
        }

        webView.postDelayed({
            Log.d("Tyrizx", "Loading WebView at http://127.0.0.1:8080")
            webView.loadUrl("http://127.0.0.1:8080")
        }, 15000)
    }

    private fun copyAssetsToDir(assetPath: String, targetDir: File) {
        val assetList = assets.list(assetPath)
        if (assetList.isNullOrEmpty()) {
            targetDir.parentFile?.mkdirs()
            assets.open(assetPath).use { input ->
                FileOutputStream(targetDir).use { output ->
                    input.copyTo(output)
                }
            }
        } else {
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            for (file in assetList) {
                val subAssetPath = if (assetPath.isEmpty()) file else "$assetPath/$file"
                val subTargetFile = File(targetDir, file)
                copyAssetsToDir(subAssetPath, subTargetFile)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serverProcess?.destroy()
    }
}
