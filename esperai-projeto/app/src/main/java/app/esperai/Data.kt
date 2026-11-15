package app.esperai

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity
data class Line(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val name: String,
    val origin: String,
    val destination: String,
    val notes: String = ""
)

@Entity
data class Stop(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val reference: String = "",
    val lat: Double? = null,
    val lng: Double? = null
)

@Entity(primaryKeys = ["lineId", "stopId"])
data class LineStop(val lineId: Long, val stopId: Long, val position: Int)

@Entity
data class Trip(@PrimaryKey(autoGenerate = true) val id: Long = 0, val lineId: Long, val dayType: Int)

@Entity(primaryKeys = ["tripId", "stopId"])
data class StopTime(val tripId: Long, val stopId: Long, val time: String)

data class LineTime(val lineId: Long, val dayType: Int, val time: String)

@Dao
abstract class AppDao {
    @Query("SELECT * FROM Line ORDER BY CAST(number AS INTEGER), number")
    abstract fun lines(): Flow<List<Line>>

    @Query("SELECT * FROM Line WHERE id = :id")
    abstract suspend fun line(id: Long): Line?

    @Query("SELECT * FROM Stop ORDER BY name")
    abstract fun stops(): Flow<List<Stop>>

    @Query("SELECT * FROM Stop WHERE id = :id")
    abstract suspend fun stop(id: Long): Stop?

    @Query("SELECT * FROM Stop WHERE id = :id")
    abstract fun stopFlow(id: Long): Flow<Stop?>

    @Query("SELECT s.* FROM Stop s JOIN LineStop ls ON ls.stopId = s.id WHERE ls.lineId = :lineId ORDER BY ls.position")
    abstract fun stopsOfLine(lineId: Long): Flow<List<Stop>>

    @Query("SELECT s.* FROM Stop s JOIN LineStop ls ON ls.stopId = s.id WHERE ls.lineId = :lineId ORDER BY ls.position")
    abstract suspend fun stopsOfLineOnce(lineId: Long): List<Stop>

    @Query("SELECT * FROM Trip WHERE lineId = :lineId")
    abstract fun tripsOfLine(lineId: Long): Flow<List<Trip>>

    @Query("SELECT st.* FROM StopTime st JOIN Trip t ON t.id = st.tripId WHERE t.lineId = :lineId")
    abstract fun timesOfLine(lineId: Long): Flow<List<StopTime>>

    @Query("SELECT t.lineId AS lineId, t.dayType AS dayType, st.time AS time FROM StopTime st JOIN Trip t ON t.id = st.tripId WHERE st.stopId = :stopId ORDER BY st.time")
    abstract fun timesAtStop(stopId: Long): Flow<List<LineTime>>

    @Query(
        "SELECT t.lineId AS lineId, t.dayType AS dayType, st.time AS time FROM Trip t " +
            "JOIN StopTime st ON st.tripId = t.id " +
            "JOIN LineStop ls ON ls.lineId = t.lineId AND ls.stopId = st.stopId " +
            "WHERE ls.position = (SELECT MIN(position) FROM LineStop WHERE lineId = t.lineId)"
    )
    abstract fun departures(): Flow<List<LineTime>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun saveLine(l: Line): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun saveStop(s: Stop): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun insertTrip(t: Trip): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun insertTimes(l: List<StopTime>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun insertLineStops(l: List<LineStop>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun insertLines(l: List<Line>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun insertStops(l: List<Stop>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun insertTrips(l: List<Trip>)

    @Query("DELETE FROM StopTime WHERE tripId = :id") abstract suspend fun deleteTimesOfTrip(id: Long)
    @Query("DELETE FROM Trip WHERE id = :id") abstract suspend fun deleteTripRow(id: Long)
    @Query("DELETE FROM LineStop WHERE lineId = :id") abstract suspend fun clearLineStops(id: Long)
    @Query("DELETE FROM StopTime WHERE tripId IN (SELECT id FROM Trip WHERE lineId = :id)") abstract suspend fun deleteTimesOfLine(id: Long)
    @Query("DELETE FROM Trip WHERE lineId = :id") abstract suspend fun deleteTripsOfLine(id: Long)
    @Query("DELETE FROM Line WHERE id = :id") abstract suspend fun deleteLineRow(id: Long)
    @Query("DELETE FROM LineStop WHERE stopId = :id") abstract suspend fun deleteLineStopsOfStop(id: Long)
    @Query("DELETE FROM StopTime WHERE stopId = :id") abstract suspend fun deleteTimesOfStop(id: Long)
    @Query("DELETE FROM Stop WHERE id = :id") abstract suspend fun deleteStopRow(id: Long)

    @Query("SELECT * FROM Line") abstract suspend fun allLines(): List<Line>
    @Query("SELECT * FROM Stop") abstract suspend fun allStops(): List<Stop>
    @Query("SELECT * FROM LineStop") abstract suspend fun allLineStops(): List<LineStop>
    @Query("SELECT * FROM Trip") abstract suspend fun allTrips(): List<Trip>
    @Query("SELECT * FROM StopTime") abstract suspend fun allTimes(): List<StopTime>

    @Query("DELETE FROM Line") abstract suspend fun clearLines()
    @Query("DELETE FROM Stop") abstract suspend fun clearStops()
    @Query("DELETE FROM LineStop") abstract suspend fun clearAllLineStops()
    @Query("DELETE FROM Trip") abstract suspend fun clearTrips()
    @Query("DELETE FROM StopTime") abstract suspend fun clearTimes()

    @Transaction
    open suspend fun setLineStops(lineId: Long, stopIds: List<Long>) {
        clearLineStops(lineId)
        insertLineStops(stopIds.mapIndexed { i, s -> LineStop(lineId, s, i) })
    }

    @Transaction
    open suspend fun saveTrip(t: Trip, times: List<StopTime>) {
        val id = insertTrip(t)
        deleteTimesOfTrip(id)
        insertTimes(times.map { it.copy(tripId = id) })
    }

    @Transaction
    open suspend fun deleteTripFull(id: Long) {
        deleteTimesOfTrip(id)
        deleteTripRow(id)
    }

    @Transaction
    open suspend fun deleteLineFull(id: Long) {
        deleteTimesOfLine(id)
        deleteTripsOfLine(id)
        clearLineStops(id)
        deleteLineRow(id)
    }

    @Transaction
    open suspend fun deleteStopFull(id: Long) {
        deleteLineStopsOfStop(id)
        deleteTimesOfStop(id)
        deleteStopRow(id)
    }

    @Transaction
    open suspend fun replaceAll(l: List<Line>, s: List<Stop>, ls: List<LineStop>, t: List<Trip>, st: List<StopTime>) {
        clearLines(); clearStops(); clearAllLineStops(); clearTrips(); clearTimes()
        insertLines(l); insertStops(s); insertLineStops(ls); insertTrips(t); insertTimes(st)
    }
}

@Database(entities = [Line::class, Stop::class, LineStop::class, Trip::class, StopTime::class], version = 1, exportSchema = false)
abstract class Db : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile private var instance: Db? = null
        fun get(c: Context): Db = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(c.applicationContext, Db::class.java, "esperai.db").build().also { instance = it }
        }
    }
}
