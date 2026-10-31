package de.remk0.flickpick.data.watchlist

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
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

enum class WatchStatus(val dbValue: String) {
    WAITING("waiting"),
    AVAILABLE("available");

    companion object {
        fun fromDb(v: String): WatchStatus = entries.firstOrNull { it.dbValue == v } ?: WAITING
    }
}

@Entity(tableName = "watchlist")
data class WatchlistEntry(
    @PrimaryKey val tmdbId: Int,
    val title: String,
    val year: Int?,
    val posterPath: String?,
    val addedAt: Long,
    val status: WatchStatus = WatchStatus.WAITING,
    val availableAt: Long? = null,
)

class Converters {
    @TypeConverter fun statusToDb(s: WatchStatus): String = s.dbValue
    @TypeConverter fun statusFromDb(v: String): WatchStatus = WatchStatus.fromDb(v)
}

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<WatchlistEntry>>

    @Query("SELECT * FROM watchlist")
    suspend fun getAll(): List<WatchlistEntry>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: WatchlistEntry)

    @Update
    suspend fun update(entries: List<WatchlistEntry>)

    @Query("DELETE FROM watchlist WHERE tmdbId = :tmdbId")
    suspend fun delete(tmdbId: Int)

    @Query("DELETE FROM watchlist WHERE status = 'available'")
    suspend fun deleteAvailable()
}

@Database(entities = [WatchlistEntry::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class WatchlistDb : RoomDatabase() {
    abstract fun dao(): WatchlistDao

    companion object {
        fun create(context: Context): WatchlistDb =
            Room.databaseBuilder(context, WatchlistDb::class.java, "watchlist.db").build()
    }
}
