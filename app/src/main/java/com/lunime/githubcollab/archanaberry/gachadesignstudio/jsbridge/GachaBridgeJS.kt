package com.lunime.githubcollab.archanaberry.gachadesignstudio.jsbridge

import android.webkit.JavascriptInterface
import android.webkit.WebView

class GachaBridgeJS(private val webView: WebView) {

    init {
        webView.settings.javaScriptEnabled = true
        webView.addJavascriptInterface(this, "GachaBridge")
    }

    @JavascriptInterface
    fun initEngine(script: String) {
        GachaEngineJS.runScript(webView, script)
    }
}