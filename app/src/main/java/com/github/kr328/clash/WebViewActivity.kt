package com.github.kr328.clash

import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.github.kr328.clash.common.compat.isLightNavigationBarCompat
import com.github.kr328.clash.common.compat.isLightStatusBarsCompat
import com.github.kr328.clash.design.WebviewDesign
import com.github.kr328.clash.design.ui.DayNight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.activity.result.contract.ActivityResultContracts

class WebViewActivity : BaseActivity<WebviewDesign>() {
    private var filePathCallback: android.webkit.ValueCallback<Array<android.net.Uri>>? = null

    override suspend fun main() {
        val titleStr = intent.getStringExtra("title") ?: "Web"
        title = titleStr

        val design = WebviewDesign(this)
        
        setContentDesign(design)

        // Clear translucent flags and enable draws system bar backgrounds
        window.clearFlags(
            android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
            android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
        )
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        // Make system status and navigation bars transparent to show the CoordinatorLayout background
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        // Disable contrast enforcement on API >= 29 to prevent dark scrim overlays
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }

        val url = intent.getStringExtra("url") ?: "https://www.google.com"

        withContext(Dispatchers.Main) {
            design.webView.apply {
                // Set WebView background transparent to avoid any white flash during initial rendering
                setBackgroundColor(android.graphics.Color.TRANSPARENT)

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    databaseEnabled = true
                }

                // Register Javascript Interface
                addJavascriptInterface(WebAppInterface(this@WebViewActivity), "Android")

                val jsInjector = { view: WebView? ->
                    val js = """
                        (function() {
                            function updateColor() {
                                var meta = document.querySelector('meta[name="theme-color"]');
                                if (meta) {
                                    var color = meta.getAttribute('content');
                                    if (color && window.Android && typeof window.Android.setStatusBarColor === 'function') {
                                        window.Android.setStatusBarColor(color);
                                    }
                                }
                            }
                            updateColor();
                            var target = document.head || document.documentElement || document;
                            if (window.MutationObserver && target) {
                                var observer = new MutationObserver(function(mutations) {
                                    updateColor();
                                });
                                observer.observe(target, { childList: true, subtree: true, attributes: true });
                            }
                        })();
                    """.trimIndent()
                    view?.evaluateJavascript(js, null)
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        val pageTitle = view?.title
                        if (!pageTitle.isNullOrEmpty()) {
                            this@WebViewActivity.title = pageTitle
                            findViewById<android.widget.TextView>(com.github.kr328.clash.design.R.id.activity_bar_title_view)?.text = pageTitle
                        }
                        jsInjector(view)
                    }
                }

                webChromeClient = object : android.webkit.WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        if (newProgress >= 30) {
                            jsInjector(view)
                        }
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        if (!title.isNullOrEmpty()) {
                            this@WebViewActivity.title = title
                            findViewById<android.widget.TextView>(com.github.kr328.clash.design.R.id.activity_bar_title_view)?.text = title
                        }
                        jsInjector(view)
                    }

                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: android.webkit.ValueCallback<Array<android.net.Uri>>?,
                        fileChooserParams: FileChooserParams?
                    ): Boolean {
                        if (filePathCallback == null) return false

                        this@WebViewActivity.filePathCallback?.onReceiveValue(null)
                        this@WebViewActivity.filePathCallback = filePathCallback

                        val intent = fileChooserParams?.createIntent() ?: android.content.Intent(android.content.Intent.ACTION_GET_CONTENT).apply {
                            type = "*/*"
                            addCategory(android.content.Intent.CATEGORY_OPENABLE)
                        }

                        this@WebViewActivity.launch {
                            try {
                                val result = this@WebViewActivity.startActivityForResult(
                                    ActivityResultContracts.StartActivityForResult(),
                                    intent
                                )
                                val results = android.webkit.WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
                                this@WebViewActivity.filePathCallback?.onReceiveValue(results)
                            } catch (e: Exception) {
                                this@WebViewActivity.filePathCallback?.onReceiveValue(null)
                            } finally {
                                this@WebViewActivity.filePathCallback = null
                            }
                        }
                        return true
                    }
                }

                loadUrl(url)
            }
        }

        while (isActive) {
            events.receive()
        }
    }

    fun updateStatusBarColor(colorStr: String) {
        val cleanColor = colorStr.trim().lowercase()
        if (cleanColor.isEmpty()) return

        try {
            var argbColor = 0
            if (cleanColor.startsWith("#")) {
                val hex = cleanColor.substring(1)
                if (hex.length == 3) {
                    val r = hex[0]
                    val g = hex[1]
                    val b = hex[2]
                    argbColor = android.graphics.Color.parseColor("#$r$r$g$g$b$b")
                } else if (hex.length == 4) {
                    val r = hex[0]
                    val g = hex[1]
                    val b = hex[2]
                    val a = hex[3]
                    argbColor = android.graphics.Color.parseColor("#$a$a$r$r$g$g$b$b")
                } else if (hex.length == 6) {
                    argbColor = android.graphics.Color.parseColor(cleanColor)
                } else if (hex.length == 8) {
                    val rr = hex.substring(0, 2)
                    val gg = hex.substring(2, 4)
                    val bb = hex.substring(4, 6)
                    val aa = hex.substring(6, 8)
                    argbColor = android.graphics.Color.parseColor("#$aa$rr$gg$bb")
                } else {
                    argbColor = android.graphics.Color.parseColor(cleanColor)
                }
            } else if (cleanColor.startsWith("rgb")) {
                val parts = cleanColor.replace("rgba", "").replace("rgb", "")
                    .replace("(", "").replace(")", "").split(",")
                if (parts.size >= 3) {
                    val r = parts[0].trim().toInt()
                    val g = parts[1].trim().toInt()
                    val b = parts[2].trim().toInt()
                    val a = if (parts.size >= 4) (parts[3].trim().toFloat() * 255).toInt() else 255
                    argbColor = android.graphics.Color.argb(a, r, g, b)
                }
            } else {
                argbColor = when (cleanColor) {
                    "transparent" -> android.graphics.Color.TRANSPARENT
                    "black" -> android.graphics.Color.BLACK
                    "white" -> android.graphics.Color.WHITE
                    "red" -> android.graphics.Color.RED
                    "green" -> android.graphics.Color.GREEN
                    "blue" -> android.graphics.Color.BLUE
                    "yellow" -> android.graphics.Color.YELLOW
                    "cyan" -> android.graphics.Color.CYAN
                    "magenta" -> android.graphics.Color.MAGENTA
                    "gray", "grey" -> android.graphics.Color.GRAY
                    "lightgray", "lightgrey" -> android.graphics.Color.LTGRAY
                    "darkgray", "darkgrey" -> android.graphics.Color.DKGRAY
                    else -> android.graphics.Color.parseColor(cleanColor)
                }
            }

            // Ensure status/navigation bars remain transparent to let CoordinatorLayout's background show through
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT

            // Dynamic light/dark icon overlays for status and navigation bars
            val isLight = isColorLight(argbColor)
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                window.isLightStatusBarsCompat = isLight
            }
            if (android.os.Build.VERSION.SDK_INT >= 27) {
                window.isLightNavigationBarCompat = isLight
            }

            // Paint the root Layout container background so the transparent paddings blend perfectly
            design?.root?.setBackgroundColor(argbColor)
        } catch (e: Exception) {
            // Ignore color parsing errors gracefully
        }
    }

    private fun isColorLight(color: Int): Boolean {
        if (color == android.graphics.Color.TRANSPARENT) {
            return dayNight == DayNight.Day
        }
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
        return luminance > 0.5
    }

    override fun onBackPressed() {
        val design = design
        if (design != null && design.webView.canGoBack()) {
            design.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    class WebAppInterface(private val activity: WebViewActivity) {
        @JavascriptInterface
        fun setStatusBarColor(colorHex: String) {
            activity.runOnUiThread {
                activity.updateStatusBarColor(colorHex)
            }
        }

        @JavascriptInterface
        fun openSystemBrowser(url: String) {
            activity.runOnUiThread {
                try {
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(url)
                    )
                    activity.startActivity(intent)
                } catch (e: Exception) {
                    // Ignore errors gracefully
                }
            }
        }

        @JavascriptInterface
        fun exitWebView() {
            activity.runOnUiThread {
                activity.finish()
            }
        }
    }

    override fun onDestroy() {
        filePathCallback?.onReceiveValue(null)
        filePathCallback = null
        super.onDestroy()
    }
}
