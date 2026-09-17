package io.tyrizx

import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    // Native method implemented in native-lib.cpp
    private external fun startNodeWithArguments(arguments: Array<String>): Int

    companion object {
        init {
            System.loadLibrary("node")
            System.loadLibrary("tyrizx")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            webViewClient = WebViewClient()
        }

        setContentView(webView)

        // Extract assets and start Node on a background thread
        Thread {
            extractAssets()
            startNode()
        }.start()

        // Load WebView after giving the server time to boot
        webView.postDelayed({
            Log.d("Tyrizx", "Loading WebView at http://127.0.0.1:8080")
            webView.loadUrl("http://127.0.0.1:8080")
        }, 30000)
    }

    private fun extractAssets() {
        val targetDir = File(filesDir, "nodejs-project")

        if (targetDir.exists()) {
            Log.d("Tyrizx", "Deleting existing nodejs-project folder...")
            targetDir.deleteRecursively()
        }

        Log.d("Tyrizx", "Extracting assets...")
        try {
            copyAssetsToDir("nodejs-project", targetDir)
            Log.d("Tyrizx", "Extraction complete: ${targetDir.absolutePath}")
        } catch (e: IOException) {
            Log.e("Tyrizx", "Extraction failed: ${e.message}")
        }
    }

    private fun startNode() {
        val projectDir = File(filesDir, "nodejs-project")
        val mainJs = File(projectDir, "main.js")

        if (!mainJs.exists()) {
            Log.e("Tyrizx", "main.js not found at ${mainJs.absolutePath}")
            return
        }

        Log.d("Tyrizx", "Starting Node.js runtime via nodejs-mobile...")

        val exitCode = startNodeWithArguments(arrayOf(
            "node",
            mainJs.absolutePath
        ))

        Log.d("Tyrizx", "Node.js runtime exited with code: $exitCode")
    }

    private fun copyAssetsToDir(assetPath: String, targetDir: File) {
        val assetList = assets.list(assetPath)

        if (assetList.isNullOrEmpty()) {
            // It's a file
            targetDir.parentFile?.mkdirs()
            assets.open(assetPath).use { input ->
                FileOutputStream(targetDir).use { output ->
                    input.copyTo(output)
                }
            }
        } else {
            // It's a directory
            if (!targetDir.exists()) targetDir.mkdirs()
            for (file in assetList) {
                val subAssetPath = if (assetPath.isEmpty()) file else "$assetPath/$file"
                copyAssetsToDir(subAssetPath, File(targetDir, file))
            }
        }
    }
}
