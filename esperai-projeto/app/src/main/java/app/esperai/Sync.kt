package app.esperai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object DataSync {
    data class Result(val updated: Boolean, val appUrl: String?, val error: Boolean)

    private fun prefs(c: Context) = c.getSharedPreferences("esperai", Context.MODE_PRIVATE)

    suspend fun exportData(db: Db): String {
        val d = db.dao()
        val o = JSONObject()
        o.put("dataVersion", System.currentTimeMillis() / 1000)
        o.put("latestApp", BuildConfig.VERSION_CODE)
        o.put("lines", JSONArray().also { a ->
            d.allLines().forEach { a.put(JSONObject().put("id", it.id).put("number", it.number).put("name", it.name).put("origin", it.origin).put("destination", it.destination).put("notes", it.notes)) }
        })
        o.put("stops", JSONArray().also { a ->
            d.allStops().forEach { a.put(JSONObject().put("id", it.id).put("name", it.name).put("reference", it.reference).put("lat", it.lat ?: JSONObject.NULL).put("lng", it.lng ?: JSONObject.NULL)) }
        })
        o.put("lineStops", JSONArray().also { a ->
            d.allLineStops().forEach { a.put(JSONObject().put("lineId", it.lineId).put("stopId", it.stopId).put("position", it.position)) }
        })
        o.put("trips", JSONArray().also { a ->
            d.allTrips().forEach { a.put(JSONObject().put("id", it.id).put("lineId", it.lineId).put("dayType", it.dayType)) }
        })
        o.put("times", JSONArray().also { a ->
            d.allTimes().forEach { a.put(JSONObject().put("tripId", it.tripId).put("stopId", it.stopId).put("time", it.time)) }
        })
        return o.toString(1)
    }

    suspend fun importData(db: Db, text: String) {
        val o = JSONObject(text)
        val lines = o.getJSONArray("lines").objs().map {
            Line(it.getLong("id"), it.getString("number"), it.getString("name"), it.getString("origin"), it.getString("destination"), it.optString("notes", ""))
        }
        val stops = o.getJSONArray("stops").objs().map {
            Stop(
                it.getLong("id"), it.getString("name"), it.optString("reference", ""),
                if (it.isNull("lat")) null else it.getDouble("lat"),
                if (it.isNull("lng")) null else it.getDouble("lng")
            )
        }
        val ls = o.getJSONArray("lineStops").objs().map { LineStop(it.getLong("lineId"), it.getLong("stopId"), it.getInt("position")) }
        val trips = o.getJSONArray("trips").objs().map { Trip(it.getLong("id"), it.getLong("lineId"), it.getInt("dayType")) }
        val times = o.getJSONArray("times").objs().map { StopTime(it.getLong("tripId"), it.getLong("stopId"), it.getString("time")) }
        db.dao().replaceAll(lines, stops, ls, trips, times)
    }

    suspend fun check(ctx: Context): Result = withContext(Dispatchers.IO) {
        val url = BuildConfig.DATA_URL
        if (url.isBlank()) return@withContext Result(false, null, true)
        try {
            val c = URL("$url?t=${System.currentTimeMillis()}").openConnection() as HttpURLConnection
            c.connectTimeout = 8000
            c.readTimeout = 8000
            val text = c.inputStream.bufferedReader().use { it.readText() }
            val o = JSONObject(text)
            val v = o.getLong("dataVersion")
            var updated = false
            if (v > prefs(ctx).getLong("dataVersion", 0)) {
                importData(Db.get(ctx), text)
                prefs(ctx).edit().putLong("dataVersion", v).apply()
                updated = true
            }
            val appUrl = if (o.optInt("latestApp", 0) > BuildConfig.VERSION_CODE) url.substringBeforeLast("/") + "/" else null
            Result(updated, appUrl, false)
        } catch (e: Exception) {
            Result(false, null, true)
        }
    }
}
