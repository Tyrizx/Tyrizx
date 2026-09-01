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

        // === DEBUG: write directory tree to external storage ===
        try {
            val debugFile = File(getExternalFilesDir(null), "tyrizx_debug.txt")
            writeDirectoryTree(serverDir, debugFile)
            Log.d("Tyrizx", "Debug tree written to ${debugFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("Tyrizx", "Failed to write debug file: ${e.message}")
        }

        // Try to find the binary
        val possiblePaths = listOf(
            File(serverDir, "bin/code-server"),
            File(serverDir, "code-server"),
            File(serverDir, "lib/node_modules/.bin/code-server")
        )

        val serverBinary = possiblePaths.firstOrNull { it.exists() }

        if (serverBinary == null) {
            Log.e("Tyrizx", "code-server binary not found in any expected location.")
            return
        }

        Log.d("Tyrizx", "Binary found at: ${serverBinary.absolutePath}, setting executable...")
        serverBinary.setExecutable(true)

        val nodeBinary = File(serverDir, "lib/node")
        if (nodeBinary.exists()) {
            Log.d("Tyrizx", "Setting executable on node binary")
            nodeBinary.setExecutable(true)
        }

        // Force chmod 755 on both binaries
        try {
            val chmod1 = Runtime.getRuntime().exec(arrayOf("chmod", "755", serverBinary.absolutePath))
            chmod1.waitFor()
            Log.d("Tyrizx", "chmod code-server exit: ${chmod1.exitValue()}")
            if (nodeBinary.exists()) {
                val chmod2 = Runtime.getRuntime().exec(arrayOf("chmod", "755", nodeBinary.absolutePath))
                chmod2.waitFor()
                Log.d("Tyrizx", "chmod node exit: ${chmod2.exitValue()}")
            }
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

    private fun writeDirectoryTree(dir: File, outputFile: File) {
        val basePath = dir.absolutePath
        outputFile.printWriter().use { writer ->
            dir.walkTopDown().forEach {
                val relativePath = it.absolutePath.removePrefix(basePath)
                val depth = relativePath.count { it == File.separatorChar }
                val indent = "  ".repeat(depth)
                writer.println("$indent${it.name}${if (it.isDirectory) "/" else ""}")
            }
        }
    }

    private fun copyAssets(assetPath: String, targetDir: File) {
        val assetList = assets.list(assetPath)
        if (assetList.isNullOrEmpty()) {
            val outFile = File(targetDir, assetPath.substringAfterLast('/'))
            outFile.parentFile?.mkdirs()
            assets.open(assetPath).use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
        } else {
            targetDir.mkdirs()
            for (file in assetList) {
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
