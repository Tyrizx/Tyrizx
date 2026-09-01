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

        // Node binary is extracted to nativeLibraryDir (libnode.so)
        val nativeLibDir = applicationInfo.nativeLibraryDir
        val nodeBinary = File(nativeLibDir, "libnode.so")

        if (!nodeBinary.exists()) {
            Log.e("Tyrizx", "libnode.so not found at ${nodeBinary.absolutePath}")
            return
        }

        // Ensure execute permission (SELinux allows this in nativeLibraryDir)
        nodeBinary.setExecutable(true)
        nodeBinary.setWritable(false)
        nodeBinary.setReadable(true)

        // Server entry point is in assets (copied to serverDir)
        val serverMain = File(serverDir, "out/server-main.js")
        if (!serverMain.exists()) {
            Log.e("Tyrizx", "server-main.js not found at ${serverMain.absolutePath}")
            return
        }

        Log.d("Tyrizx", "Starting server with Node from nativeLibraryDir...")

        val processBuilder = ProcessBuilder(
            nodeBinary.absolutePath,
            serverMain.absolutePath,
            "--port", "8080",
            "--host", "127.0.0.1",
            "--without-connection-token"
        )
        processBuilder.directory(serverDir)

        // Set environment variables
        val nodeModulesPath = File(serverDir, "node_modules").absolutePath
        processBuilder.environment()["NODE_PATH"] = nodeModulesPath

        // Also add nativeLibraryDir to LD_LIBRARY_PATH for libc++_shared.so
        val libPath = "$nativeLibDir:${serverDir.absolutePath}"
        processBuilder.environment()["LD_LIBRARY_PATH"] = libPath
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
