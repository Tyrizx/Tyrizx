package io.tyrizx

import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.caoccao.javet.interop.V8Host
import com.caoccao.javet.interop.V8Runtime
import com.caoccao.javet.interop.NodeRuntime
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var nodeRuntime: NodeRuntime? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            webViewClient = WebViewClient()
        }

        setContentView(webView)

        // Extract assets first, then start the server in a background thread
        Thread {
            extractAssets()
            startServerWithJavet()
        }.start()

        // Give the server time to start, then load it
        webView.postDelayed({
            Log.d("Tyrizx", "Loading WebView at http://127.0.0.1:8080")
            webView.loadUrl("http://127.0.0.1:8080")
        }, 20000)
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

    private fun startServerWithJavet() {
        try {
            val projectDir = File(filesDir, "nodejs-project")
            val mainJs = File(projectDir, "main.js")

            if (!mainJs.exists()) {
                Log.e("Tyrizx", "main.js not found at ${mainJs.absolutePath}")
                return
            }

            Log.d("Tyrizx", "Creating Node.js runtime via Javet...")

            // Use NodeRuntime explicitly to avoid type inference issues
            val runtime = V8Host.getNodeInstance().createV8Runtime<NodeRuntime>()
            nodeRuntime = runtime

            Log.d("Tyrizx", "Executing main.js...")
            // Use the File overload of getExecutor
            runtime.getExecutor(mainJs).executeVoid()

            Log.d("Tyrizx", "Server started via Javet. Entering event loop...")

            // Keep the Node.js event loop alive (critical for HTTP servers)
            runtime.await()

        } catch (e: Exception) {
            Log.e("Tyrizx", "Failed to start server: ${e.message}")
            e.printStackTrace()
        }
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
            if (!targetDir.exists()) targetDir.mkdirs()
            for (file in assetList) {
                val subAssetPath = if (assetPath.isEmpty()) file else "$assetPath/$file"
                copyAssetsToDir(subAssetPath, File(targetDir, file))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            nodeRuntime?.close()
        } catch (e: Exception) {
            Log.e("Tyrizx", "Error closing runtime: ${e.message}")
        }
    }
}
