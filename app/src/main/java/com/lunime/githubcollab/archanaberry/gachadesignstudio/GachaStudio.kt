package com.lunime.githubcollab.archanaberry.gachadesignstudio

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.lunime.githubcollab.archanaberry.gachadesignstudio.gachadynamicstorage.GachaDynamicStorage
import com.lunime.githubcollab.archanaberry.gachadesignstudio.jsbridge.*
import java.io.File
import java.util.*

class GachaStudio : AppCompatActivity() {

    companion object {
        private const val FILE_CHOOSER_REQUEST_CODE = 1001
        private const val PRESS_BACK_INTERVAL = 2000L
    }

    private lateinit var webView: WebView
    private lateinit var localization: GachaStudioLocalization
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    private var backPressedTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gachastudio_main)

        // Inisialisasi localization
        val sysLang = Locale.getDefault().language
        localization = GachaStudioLocalization.load(this, sysLang)

        // Inisialisasi WebView & full screen
        webView = findViewById(R.id.web)
        setupFullScreen()
        loadWebViewContent()
    }

    /** Kelola UI fullscreen & cutout/notch */
    private fun setupFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.decorView.windowInsetsController?.run {
                hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        } else {
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
        window.statusBarColor = ContextCompat.getColor(this, android.R.color.transparent)
        window.navigationBarColor = ContextCompat.getColor(this, android.R.color.transparent)
    }

    /** Konfigurasi WebView dan JS bridges */
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            allowContentAccess = true
            allowFileAccess = true
            mediaPlaybackRequiresUserGesture = false
        }

        // WebViewClient untuk intercept page load
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean = false

            override fun onPageFinished(view: WebView?, url: String?) {
                view?.evaluateJavascript("updateLocalization()", null)
            }
        }

        // WebChromeClient untuk console & file chooser
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    handleConsoleMessage(it.message(), it.messageLevel())
                }
                return true
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
            ): Boolean {
                fileChooserCallback?.onReceiveValue(null)
                fileChooserCallback = filePathCallback

                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = params?.acceptTypes?.firstOrNull().takeIf { !it.isNullOrEmpty() } ?: "*/*"
                    putExtra(Intent.EXTRA_MIME_TYPES, params?.acceptTypes)
                    putExtra(
                        Intent.EXTRA_ALLOW_MULTIPLE,
                        params?.mode == FileChooserParams.MODE_OPEN_MULTIPLE
                    )
                }

                startActivityForResult(
                    Intent.createChooser(intent, "Pilih file…"),
                    FILE_CHOOSER_REQUEST_CODE
                )
                return true
            }
        }

        // JS Bridge
        webView.addJavascriptInterface(GachaJavascript(this), "GachaDesignStudio")
        webView.addJavascriptInterface(
            GachaStudioLocalization.WebAppInterface(this, localization),
            "GachaDesignStudioLang"
        )
        webView.addJavascriptInterface(GachaBridgeJS(webView), "GachaBridge")
    }

    /** Muat konten HTML ke WebView */
    private fun loadWebViewContent() {
        setupWebView()
        val baseDir = GachaDynamicStorage.detectDynamicDirectory()
        val htmlFile = File("$baseDir$APP_FOLDER/mainmenu.html")

        if (htmlFile.exists()) {
            webView.loadUrl("file:///${htmlFile.path}")
        } else {
            val errHtml = GachaStudioError.Err404(htmlFile.name)
            webView.loadDataWithBaseURL(null, errHtml, "text/html", "UTF-8", null)
            Toast.makeText(
                this,
                localization.getString("file_gds_not_found", htmlFile.path),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun handleConsoleMessage(message: String, level: ConsoleMessage.MessageLevel) {
        when (level) {
            ConsoleMessage.MessageLevel.DEBUG -> GachaStudioLogger.log("Debug: $message")
            ConsoleMessage.MessageLevel.ERROR -> GachaStudioLogger.log("Error: $message", isError = true)
            ConsoleMessage.MessageLevel.LOG -> GachaStudioLogger.log("Log: $message")
            ConsoleMessage.MessageLevel.WARNING ->
                GachaStudioLogger.log("Warning: $message", isError = true)
            ConsoleMessage.MessageLevel.TIP -> GachaStudioLogger.log("Tip: $message")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            val uris = mutableListOf<Uri>()
            data?.clipData?.let { clip ->
                for (i in 0 until clip.itemCount) {
                    uris += clip.getItemAt(i).uri
                }
            } ?: data?.data?.let { uris += it }

            fileChooserCallback?.onReceiveValue(
                if (uris.isNotEmpty()) uris.toTypedArray() else null
            )
            fileChooserCallback = null
        }
    }

    override fun onBackPressed() {
        val now = SystemClock.elapsedRealtime()
        if (now - backPressedTime > PRESS_BACK_INTERVAL) {
            backPressedTime = now
            Toast.makeText(
                this,
                localization.getString("press_back_again_to_exit"),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            super.onBackPressed()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.e("GachaStudio", localization.getString("low_memory_warning"))
    }

    /*
    */
}