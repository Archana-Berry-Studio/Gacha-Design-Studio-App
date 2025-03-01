package com.lunime.githubcollab.archanaberry.gachadesignstudio.jsbridge

import android.webkit.WebView

object GachaEngineJS {
    fun runScript(webView: WebView, script: String) {
        webView.post {
            webView.evaluateJavascript(script, null)
        }
    }
}