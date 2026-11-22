package pl.mdinstal.muzyka

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

object Prefs {
    private fun sp(c: Context): SharedPreferences = c.getSharedPreferences("muzyka", Context.MODE_PRIVATE)

    fun lastUpdateCheck(c: Context): Long = sp(c).getLong("upd_check", 0L)
    fun setLastUpdateCheck(c: Context, t: Long) { sp(c).edit().putLong("upd_check", t).apply() }

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
