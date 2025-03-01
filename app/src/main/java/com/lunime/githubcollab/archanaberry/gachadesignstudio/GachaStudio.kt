package com.lunime.githubcollab.archanaberry.gachadesignstudio

import com.lunime.githubcollab.archanaberry.gachadesignstudio.gachadynamicstorage.GachaDynamicStorage
import com.lunime.githubcollab.archanaberry.gachadesignstudio.jsbridge.GachaBridgeJS

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.os.Process
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import java.io.File

import android.view.WindowInsets
import android.view.WindowInsetsController
import android.os.Handler
import android.os.Looper

import android.webkit.WebChromeClient
import android.webkit.ConsoleMessage
import android.content.Context

import android.view.Window
import android.view.WindowManager
import android.os.Build
import androidx.core.content.ContextCompat

class GachaStudio : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var localization: GachaStudioLocalization // Inisialisasi library localization

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gachastudio_main) // Pastikan layout sesuai
        
        //Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this))

        // Inisialisasi GachaStudioLocalization
        localization = GachaStudioLocalization.load(this)

        // Inisialisasi WebView
        webView = findViewById(R.id.web)
        loadWebViewContent()
        
        // Mengatur fullscreen dan mengisi area notch/cutout
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // Untuk Android 11 (API 30) ke atas
        window.setDecorFitsSystemWindows(false)

        val controller = window.decorView.windowInsetsController
        controller?.apply {
            hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // Mengizinkan layar menggunakan area cutout (poni)
        val params = window.attributes
        params.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        window.attributes = params

    } else {
        // Untuk Android 10 (API 29) ke bawah
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }

    // Mengatur warna transparan untuk status bar dan navigasi
    window.statusBarColor = ContextCompat.getColor(this, android.R.color.transparent)
    window.navigationBarColor = ContextCompat.getColor(this, android.R.color.transparent)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setUpWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            allowContentAccess = true
            allowFileAccess = true
            mediaPlaybackRequiresUserGesture = false

// Log menggunakan localization
GachaStudioLogger.log("""
    ${localization.getString("using_settings")}:
    ${localization.getString("javascript_execute")}:${localization.getString("enabled0")}
    ${localization.getString("allow_content_access")}:${localization.getString("enabled0")}
    ${localization.getString("allow_file_access")}:${localization.getString("enabled0")}
    ${localization.getString("media_playback_requires_user_gesture")}:${localization.getString("disabled0")}
""")
}

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                GachaStudioLogger.log(localization.getString("loading_web_client"))
                return false // Biarkan WebView memuat URL
            }
        }

        // Set WebChromeClient untuk menangkap log dari console
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                // Ambil log dari console
                consoleMessage?.let {
                    val message = it.message()
                    val level = it.messageLevel()
                    handleConsoleMessage(message, level)
                }
                return true
            }
        }
    }

    private fun handleConsoleMessage(message: String, level: ConsoleMessage.MessageLevel) {
        // Kirim log ke GachaStudioLogger
        when (level) {
            ConsoleMessage.MessageLevel.DEBUG -> {
                GachaStudioLogger.log("Debug: $message")
            }
            ConsoleMessage.MessageLevel.ERROR -> {
                GachaStudioLogger.log("Error: $message", isError = true)
            }
            ConsoleMessage.MessageLevel.LOG -> {
                GachaStudioLogger.log("Log: $message")
            }
            ConsoleMessage.MessageLevel.WARNING -> {
                GachaStudioLogger.log("Warning: $message", isError = true)
            }
            ConsoleMessage.MessageLevel.TIP -> {
                GachaStudioLogger.log("Tip: $message")
            }
        }
    }

    /*
    private fun startFullscreenMode() {
        val decor = window.decorView
        decor.setOnSystemUiVisibilityChangeListener { visibility ->
            if ((visibility and View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                timeout.startTimeout(5000) {
                    GachaStudioLogger.log(localization.getString("fullscreen_started")) // Layar penuh dimulai..
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        // API 30 ke atas: Gunakan WindowInsetsController
                        window.setDecorFitsSystemWindows(false)
                        val insetsController = window.insetsController
                        insetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                        insetsController?.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE)
                    } else {
                        // API 29 ke bawah: Gunakan setSystemUiVisibility
                        GachaStudioLogger.log(localization.getString("fullscreen_deprecated_started")) // Layar penuh (LAWAS) diaktifkan...
                        decor.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
                    }
                }
            }
        }
    }
    */

    private fun loadWebViewContent() {
        GachaStudioLogger.log(localization.getString("setting_web_content"))
        setUpWebView()
        
        // Implementasi JSBridge: Register GachaBridgeJS sebagai jembatan untuk JavaScript
        webView.addJavascriptInterface(GachaBridgeJS(webView), "GachaBridge")

        // Path file HTML
        val baseDir = GachaDynamicStorage.detectDynamicDirectory()
        val htmlFilePath = "$baseDir$APP_FOLDER/mainmenu.html"
        val file = File(htmlFilePath)

        if (file.exists()) {
            // Memuat file jika ditemukan
            GachaStudioLogger.log(localization.getString("file_gds_found", htmlFilePath))
            webView.loadUrl("file:///$htmlFilePath")
        } else {
            // Jika file tidak ditemukan
            val fileName = file.name // Ambil nama file sebagai string
            val htmlContent = GachaStudioError.Err404(fileName) // Menyertakan fileName sebagai parameter

            // Menampilkan HTML content ke WebView
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)

            GachaStudioLogger.log(localization.getString("file_gds_not_found", htmlFilePath), isError = true)
			Toast.makeText(this, localization.getString("file_gds_not_found", htmlFilePath), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        GachaStudioLogger.log(localization.getString("low_memory_warning"), isError = true)
        Log.e("GachaStudio", localization.getString("low_memory_warning"))
    }

    override fun onBackPressed() {
        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - backPressedTime > PRESS_BACK_INTERVAL) {
            backPressedTime = currentTime
            GachaStudioLogger.log(localization.getString("back_button_pressed"))
            Toast.makeText(this, localization.getString("press_back_again_to_exit"), Toast.LENGTH_SHORT).show()
        } else {
            super.onBackPressed()
            GachaStudioLogger.log(localization.getString("exit_using_back_button"), isError = true)
            Log.i("GachaStudio", localization.getString("exit_using_back_button"))
        }
    }

    /*
    // TimerTimeout class to manage timeouts for UI visibility
    private class TimerTimeout {
        private val handler = Handler(Looper.getMainLooper()) // Handler with main looper
        private var timeoutRunnable: Runnable? = null

        fun startTimeout(delayMillis: Long, action: () -> Unit) {
            stopTimeout() // Ensure no other timeout is running
            timeoutRunnable = Runnable {
                action()
            }
            timeoutRunnable?.let {
                handler.postDelayed(it, delayMillis)
            }
        }

        fun stopTimeout() {
            timeoutRunnable?.let { handler.removeCallbacks(it) }
        }
    }
    */
}