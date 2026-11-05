package io.github.glassbeat.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey val id: String,
    val title: String,
    val path: String,
    val artist: String = "本地音乐",
    val durationMs: Long = 0,
    val mime: String = "audio/mpeg",
    val cover: String = "",
    val favorite: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
    val source: String = "本地导入"
)
@Entity(tableName = "playlists")
data class Playlist(@PrimaryKey val id: String, val name: String)
@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"],
    foreignKeys = [ForeignKey(entity = Playlist::class, parentColumns = ["id"], childColumns = ["playlistId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Song::class, parentColumns = ["id"], childColumns = ["songId"], onDelete = ForeignKey.CASCADE)], indices = [Index("songId")])
data class PlaylistSong(val playlistId: String, val songId: String)

@Dao
interface LibraryDao {
    @Query("SELECT * FROM songs ORDER BY addedAt DESC") fun songs(): Flow<List<Song>>
    @Query("SELECT * FROM songs WHERE id = :id") suspend fun song(id: String): Song?
    @Query("SELECT COUNT(*) FROM songs WHERE path = :path") suspend fun references(path: String): Int
    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE") fun playlists(): Flow<List<Playlist>>
    @Query("SELECT * FROM playlist_songs") fun memberships(): Flow<List<PlaylistSong>>
    @Upsert suspend fun save(song: Song)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun importLegacy(songs: List<Song>)
    @Query("UPDATE songs SET favorite = NOT favorite WHERE id = :id") suspend fun favorite(id: String)
    @Query("UPDATE songs SET title = :title WHERE id = :id") suspend fun rename(id: String, title: String)
    @Query("UPDATE songs SET cover = :path WHERE id = :id") suspend fun cover(id: String, path: String)
    @Query("DELETE FROM songs WHERE id = :id") suspend fun delete(id: String)
    @Upsert suspend fun save(playlist: Playlist)
    @Query("DELETE FROM playlists WHERE id = :id") suspend fun deletePlaylist(id: String)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun add(entry: PlaylistSong)
    @Delete suspend fun remove(entry: PlaylistSong)
}
@Database(entities = [Song::class, Playlist::class, PlaylistSong::class], version = 1, exportSchema = true)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun library(): LibraryDao
    companion object {
        @Volatile private var instance: LibraryDatabase? = null
        fun get(context: Context): LibraryDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, LibraryDatabase::class.java, "glassbeat.db").build().also { instance = it }
        }
    }
}
