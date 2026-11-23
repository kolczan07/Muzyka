package pl.mdinstal.muzyka

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

object Prefs {
    private fun sp(c: Context): SharedPreferences = c.getSharedPreferences("muzyka", Context.MODE_PRIVATE)

    fun lastUpdateCheck(c: Context): Long = sp(c).getLong("upd_check", 0L)
    fun setLastUpdateCheck(c: Context, t: Long) { sp(c).edit().putLong("upd_check", t).apply() }

    /** Motyw aplikacji: "club" (klubowy fiolet) albo "oled" (czysta czerń). */
    fun theme(c: Context): String = sp(c).getString("theme", "club") ?: "club"
    fun setTheme(c: Context, v: String) { sp(c).edit().putString("theme", v).apply() }

    /** Wygląd dużego odtwarzacza: "classic" albo "winamp". */
    fun playerSkin(c: Context): String = sp(c).getString("player_skin", "classic") ?: "classic"
    fun setPlayerSkin(c: Context, v: String) { sp(c).edit().putString("player_skin", v).apply() }

    fun favorites(c: Context): List<Station> = try {
        val a = JSONArray(sp(c).getString("fav", "[]"))
        (0 until a.length()).map { a.getJSONObject(it) }
            .map { Station(it.optString("name"), it.optString("url"), it.optString("logo")) }
            .filter { it.url.isNotEmpty() }
    } catch (e: Exception) { emptyList() }

    fun setFavorites(c: Context, l: List<Station>) {
        val a = JSONArray()
        l.forEach { a.put(JSONObject().put("name", it.name).put("url", it.url).put("logo", it.logo)) }
        sp(c).edit().putString("fav", a.toString()).apply()
    }
}

fun appVersionName(ctx: Context): String = try {
    ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "0"
} catch (e: Exception) { "0" }
