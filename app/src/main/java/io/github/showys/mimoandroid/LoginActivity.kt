package io.github.showys.mimoandroid

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * 应用内登录：直接加载 MiMoCode 给出的 OAuth 授权页。
 *
 * 好处：不触发 HyperOS 的「链式启动」确认框；登录完成后小米会把浏览器重定向到
 * http://localhost:<port>/（guest 里的回调服务与 Android 共享同一 loopback），
 * 本 WebView 打开该地址即完成回调，TUI 自动变成已登录。
 */
class LoginActivity : Activity() {
    private val prefs by lazy { Prefs(this) }
    private var rememberBrowser: android.widget.CheckBox? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.getStringExtra(EXTRA_URL).orEmpty()
        if (url.isEmpty()) {
            finish()
            return
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#101014"))
        }

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(32, 16, 32, 16)
            setBackgroundColor(Color.parseColor("#18181c"))
        }
        bar.addView(
            TextView(this).apply {
                text = "登录小米账号"
                setTextColor(Color.parseColor("#e8e8ea"))
                textSize = 16f
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            },
        )
        rememberBrowser = android.widget.CheckBox(this).apply {
            text = "以后用浏览器"
            setTextColor(Color.parseColor("#b8b8c0"))
            isChecked = prefs.loginMode == Prefs.LOGIN_BROWSER
            setOnCheckedChangeListener { _, checked ->
                prefs.loginMode = if (checked) Prefs.LOGIN_BROWSER else Prefs.LOGIN_INAPP
            }
        }
        bar.addView(rememberBrowser)
        bar.addView(
            Button(this).apply {
                text = "改用浏览器"
                setOnClickListener { openExternal(url) }
            },
        )
        bar.addView(
            Button(this).apply {
                text = "关闭"
                setOnClickListener { finish() }
            },
        )
        root.addView(bar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        root.addView(
            TextView(this).apply {
                text = "在此登录一次后会记住登录状态；若本机小米账号已在浏览器里登录，点「改用浏览器」通常只需确认一次授权。"
                setTextColor(Color.parseColor("#8a8a94"))
                textSize = 12f
                setPadding(32, 12, 32, 12)
            },
        )

        val web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.userAgentString = settings.userAgentString + " MiMoTerminal/1.0"
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: android.webkit.WebResourceRequest,
                ): Boolean {
                    val target = request.url.toString()
                    // 回调地址交给我们自己的 WebView 打开，guest 里的监听器会收到
                    Log.i(Runtime.TAG, "login nav: $target")
                    return false
                }
            }
        }
        root.addView(web, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        web.loadUrl(url)
    }

    private fun openExternal(url: String) {
        prefs.loginMode = Prefs.LOGIN_BROWSER
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(intent) }
            .onFailure { Log.w(Runtime.TAG, "no browser: ${it.message}") }
    }

    override fun onPause() {
        CookieManager.getInstance().flush()
        super.onPause()
    }

    companion object {
        const val EXTRA_URL = "url"
    }
}
