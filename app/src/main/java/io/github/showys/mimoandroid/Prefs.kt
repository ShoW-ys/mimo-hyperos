package io.github.showys.mimoandroid

import android.content.Context

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("mimo", Context.MODE_PRIVATE)

    var serverUrl: String
        get() = sp.getString("server_url", "") ?: ""
        set(value) = sp.edit().putString("server_url", normalizeUrl(value)).apply()

    /** 登录方式：LOGIN_INAPP（应用内 WebView，默认）或 LOGIN_BROWSER（系统浏览器，可能已登录小米账号）。 */
    var loginMode: String
        get() = sp.getString("login_mode", LOGIN_INAPP) ?: LOGIN_INAPP
        set(value) = sp.edit().putString("login_mode", value).apply()

    companion object {
        const val LOGIN_INAPP = "inapp"
        const val LOGIN_BROWSER = "browser"

        fun normalizeUrl(raw: String): String {
            val t = raw.trim()
            if (t.isEmpty()) return ""
            val withScheme = if (t.startsWith("http://") || t.startsWith("https://")) t else "http://$t"
            return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
        }
    }
}
