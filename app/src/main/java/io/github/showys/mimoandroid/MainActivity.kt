package io.github.showys.mimoandroid

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.webkit.HttpAuthHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var root: FrameLayout
    private lateinit var logView: TextView
    private var web: WebView? = null
    private val prefs by lazy { Prefs(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        root = FrameLayout(this)
        logView = TextView(this).apply {
            textSize = 12f
            setPadding(48, 120, 48, 48)
            text = "MiMo 终端\n正在准备 Linux 环境…\n"
        }
        root.addView(ScrollView(this).apply { addView(logView) }, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)

        RuntimeService.onLog = { line -> runOnUiThread { logView.append("$line\n") } }
        RuntimeService.onReady = { port, pass -> runOnUiThread { showWeb(port, pass) } }

        val remote = prefs.serverUrl
        if (remote.isNotEmpty()) {
            showWebUrl(remote, null)
            return
        }

        startForegroundService(Intent(this, RuntimeService::class.java))
    }

    private fun showWeb(port: Int, pass: String) = showWebUrl("http://127.0.0.1:$port/", pass)

    private fun showWebUrl(url: String, pass: String?) {
        if (web != null) return
        val w = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = false
            settings.builtInZoomControls = false
            setBackgroundColor(0xFF101014.toInt())
            webViewClient = object : WebViewClient() {
                override fun onReceivedHttpAuthRequest(
                    view: WebView,
                    handler: HttpAuthHandler,
                    host: String,
                    realm: String,
                ) {
                    if (pass != null) handler.proceed("mimo", pass) else handler.cancel()
                }

                override fun onPageFinished(view: WebView, url: String) {
                    view.evaluateJavascript(KEYBAR_JS, null)
                }

                override fun onReceivedError(
                    view: WebView,
                    request: android.webkit.WebResourceRequest,
                    error: android.webkit.WebResourceError,
                ) {
                    Log.e(Runtime.TAG, "webview error ${error.errorCode} ${error.description} $url")
                }
            }
        }
        root.removeAllViews()
        root.addView(w, FrameLayout.LayoutParams(-1, -1, 0))
        w.loadUrl(url)
        web = w

        // 放到 TUI 起来之后再问通知权限，避免首次启动被系统弹窗挡住
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onDestroy() {
        web?.destroy()
        web = null
        super.onDestroy()
    }

    private companion object {
        val KEYBAR_JS = """
        (() => {
          if (window.__mimoKeybar) return;
          window.__mimoKeybar = true;
          const bar = document.createElement('div');
          bar.style.cssText = 'position:fixed;left:0;right:0;bottom:0;z-index:99999;display:flex;gap:6px;padding:6px 8px;background:#18181c;border-top:1px solid #2a2a30;overflow-x:auto';
          const keys = [['Esc','Escape',27,false],['Tab','Tab',9,false],['Ctrl','Control',17,false],
                        ['Alt','Alt',18,false],['↑','ArrowUp',38,false],['↓','ArrowDown',40,false],
                        ['←','ArrowLeft',37,false],['→','ArrowRight',39,false],['Ctrl+C','c',67,true]];
          for (const [label, key, code, ctrl] of keys) {
            const b = document.createElement('button');
            b.textContent = label;
            b.style.cssText = 'flex:0 0 auto;padding:8px 12px;border-radius:8px;border:1px solid #333;background:#242429;color:#eee;font-size:13px';
            b.addEventListener('click', (ev) => {
              ev.preventDefault();
              const ta = document.querySelector('.xterm-helper-textarea');
              if (!ta) return;
              ta.focus();
              ta.dispatchEvent(new KeyboardEvent('keydown', {key, code: label, keyCode: code, which: code,
                ctrlKey: !!ctrl, bubbles: true, cancelable: true}));
              ta.dispatchEvent(new KeyboardEvent('keyup', {key, code: label, keyCode: code, which: code,
                ctrlKey: !!ctrl, bubbles: true}));
            });
            bar.appendChild(b);
          }
          document.body.appendChild(bar);
          document.body.style.paddingBottom = '52px';
        })();
        """.trimIndent()
    }
}
