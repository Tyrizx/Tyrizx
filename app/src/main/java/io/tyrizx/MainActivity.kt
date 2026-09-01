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

        // Delete old folder to force a clean copy
        if (serverDir.exists()) {
            Log.d("Tyrizx", "Deleting existing folder...")
            serverDir.deleteRecursively()
        }

        Log.d("Tyrizx", "Extracting assets...")
        try {
            copyAssetsToDir("code-server", serverDir)
            Log.d("Tyrizx", "Extraction complete.")
        } catch (e: IOException) {
            Log.e("Tyrizx", "Extraction failed: ${e.message}")
            e.printStackTrace()
            return
        }

        // Locate the binary (try multiple possible paths)
        val binaryCandidates = listOf(
            File(serverDir, "bin/code-server"),
            File(serverDir, "bin/code-server/code-server"),
            File(serverDir, "code-server")
        )

        val serverBinary = binaryCandidates.firstOrNull { it.exists() }

        if (serverBinary == null) {
            Log.e("Tyrizx", "code-server binary not found.")
            return
        }

        Log.d("Tyrizx", "Binary found at: ${serverBinary.absolutePath}")
        serverBinary.setExecutable(true)

        val nodeBinary = File(serverDir, "lib/node")
        if (nodeBinary.exists()) {
            nodeBinary.setExecutable(true)
        }

        // chmod 755 on the binary
        try {
            val chmod = Runtime.getRuntime().exec(arrayOf("chmod", "755", serverBinary.absolutePath))
            chmod.waitFor()
            Log.d("Tyrizx", "chmod code-server exit: ${chmod.exitValue()}")
        } catch (e: Exception) {
            Log.e("Tyrizx", "chmod failed: ${e.message}")
        }

        val userDataDir = File(filesDir, ".tyrizx-userdata")
        userDataDir.mkdirs()

        Log.d("Tyrizx", "Starting server...")
        val processBuilder = ProcessBuilder(
            serverBinary.absolutePath,
            "--port", "8080",
            "--host", "127.0.0.1",
            "--auth", "none",
            "--user-data-dir", userDataDir.absolutePath
        )
        processBuilder.directory(serverDir)
        processBuilder.environment()["LD_LIBRARY_PATH"] = "/system/lib64:${serverDir.absolutePath}"
        processBuilder.redirectErrorStream(true)

        try {
            serverProcess = processBuilder.start()
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

        webView.postDelayed({
            Log.d("Tyrizx", "Loading WebView at http://127.0.0.1:8080")
            webView.loadUrl("http://127.0.0.1:8080")
        }, 15000)
    }

    /**
     * Recursively copy assets from the APK to the internal storage.
     * Handles files and directories correctly.
     */
    private fun copyAssetsToDir(assetPath: String, targetDir: File) {
        val assetList = assets.list(assetPath)
        if (assetList.isNullOrEmpty()) {
            // It's a file – copy it
            val outFile = File(targetDir, assetPath.substringAfterLast('/'))
            outFile.parentFile?.mkdirs()
            assets.open(assetPath).use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
        } else {
            // It's a directory – create it and recurse
            targetDir.mkdirs()
            for (file in assetList) {
                val subPath = if (assetPath.isEmpty()) file else "$assetPath/$file"
                copyAssetsToDir(subPath, File(targetDir, file))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serverProcess?.destroy()
    }
}
