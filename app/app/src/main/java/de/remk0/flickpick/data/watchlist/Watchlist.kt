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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

/** A movie the user has already watched; hidden from Top rated unless "Show seen" is on. */
@Entity(tableName = "seen")
data class SeenMovie(
    @PrimaryKey val tmdbId: Int,
    val seenAt: Long,
)

@Dao
interface SeenDao {
    @Query("SELECT tmdbId FROM seen")
    fun observeIds(): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movie: SeenMovie)

    @Query("DELETE FROM seen WHERE tmdbId = :tmdbId")
    suspend fun delete(tmdbId: Int)
}

@Database(entities = [WatchlistEntry::class, SeenMovie::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class WatchlistDb : RoomDatabase() {
    abstract fun dao(): WatchlistDao
    abstract fun seenDao(): SeenDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `seen` (`tmdbId` INTEGER NOT NULL, `seenAt` INTEGER NOT NULL, PRIMARY KEY(`tmdbId`))",
                )
            }
        }

        fun create(context: Context): WatchlistDb =
            Room.databaseBuilder(context, WatchlistDb::class.java, "watchlist.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
