package app.esperai

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

val DAY_LABELS = listOf("Dias úteis", "Sábado", "Dom/feriado")

fun todayType(): Int = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
    Calendar.SATURDAY -> 1
    Calendar.SUNDAY -> 2
    else -> 0
}

fun nowHm(): String = SimpleDateFormat("HH:mm", Locale.US).format(Date())

fun toMin(t: String?): Int? {
    val p = t?.split(":") ?: return null
    if (p.size != 2) return null
    val h = p[0].toIntOrNull() ?: return null
    val m = p[1].toIntOrNull() ?: return null
    return h * 60 + m
}

fun duration(a: String?, b: String?): String {
    val x = toMin(a) ?: return ""
    val y = toMin(b) ?: return ""
    var d = y - x
    if (d < 0) d += 1440
    val h = d / 60
    val m = d % 60
    return when {
        h == 0 -> "${m}min"
        m == 0 -> "${h}h"
        else -> "${h}h${m.toString().padStart(2, '0')}"
    }
}

fun normTime(s: String): String? {
    val d = s.filter { it.isDigit() }
    if (d.length !in 3..4) return null
    val p = d.padStart(4, '0')
    val h = p.substring(0, 2).toInt()
    val m = p.substring(2).toInt()
    return if (h < 24 && m < 60) "%02d:%02d".format(h, m) else null
}

fun parseCoords(s: String): Pair<Double, Double>? {
    val n = Regex("-?\\d+\\.\\d+").findAll(s.replace('\u2212', '-')).map { it.value.toDouble() }.toList()
    if (n.size < 2) return null
    val a = n[0]
    val b = n[1]
    return if (a in -90.0..90.0 && b in -180.0..180.0) Pair(a, b) else null
}

data class TripView(val trip: Trip, val times: Map<Long, String>, val dep: String?, val arr: String?)

fun tripViews(trips: List<Trip>, times: List<StopTime>, stops: List<Stop>, day: Int): List<TripView> {
    val first = stops.firstOrNull()?.id
    val last = stops.lastOrNull()?.id
    return trips.filter { it.dayType == day }.map { t ->
        val m = times.filter { it.tripId == t.id }.associate { it.stopId to it.time }
        TripView(t, m, first?.let { m[it] }, last?.let { m[it] })
    }.sortedBy { it.dep ?: "99:99" }
}

fun openUrl(ctx: Context, url: String) {
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
    }
}

fun openMaps(ctx: Context, s: Stop, walk: Boolean) {
    if (s.lat == null || s.lng == null) return
    val url = if (walk) {
        "https://www.google.com/maps/dir/?api=1&destination=${s.lat},${s.lng}&travelmode=walking"
    } else {
        "https://www.google.com/maps/search/?api=1&query=${s.lat},${s.lng}"
    }
    openUrl(ctx, url)
}

fun JSONArray.objs(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

object Holidays {
    data class H(val date: String, val name: String, val dayType: Int)

    private fun prefs(c: Context) = c.getSharedPreferences("esperai", Context.MODE_PRIVATE)

    fun save(c: Context, arr: JSONArray) {
        prefs(c).edit().putString("holidays", arr.toString()).apply()
    }

    fun load(c: Context): List<H> = try {
        JSONArray(prefs(c).getString("holidays", "[]")).objs().map {
            H(it.getString("date"), it.optString("name", "Feriado"), it.getInt("dayType").coerceIn(0, 2))
        }
    } catch (e: Exception) {
        emptyList()
    }

    fun today(c: Context): H? {
        val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return load(c).firstOrNull { it.date == d }
    }
}

fun dayTypeNow(ctx: Context): Int = Holidays.today(ctx)?.dayType ?: todayType()
