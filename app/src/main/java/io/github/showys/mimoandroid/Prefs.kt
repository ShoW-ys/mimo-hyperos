package io.github.showys.mimoandroid

import android.content.Context

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("mimo", Context.MODE_PRIVATE)

    var serverUrl: String
        get() = sp.getString("server_url", "") ?: ""
        set(value) = sp.edit().putString("server_url", normalizeUrl(value)).apply()

    companion object {
        fun normalizeUrl(raw: String): String {
            val t = raw.trim()
            if (t.isEmpty()) return ""
            val withScheme = if (t.startsWith("http://") || t.startsWith("https://")) t else "http://$t"
            return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
        }
    }
}
