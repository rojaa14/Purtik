package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// Entity representing downloaded videos
@Entity(tableName = "downloaded_videos")
data class DownloadedVideo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val title: String,
    val authorName: String,
    val authorHandle: String,
    val viewCount: Long,
    val likeCount: Long,
    val commentCount: Long,
    val shareCount: Long,
    val duration: Int, // in seconds
    val fileSizeMb: Double,
    val downloadProgress: Int, // 0 to 100
    val isDownloaded: Boolean,
    val localUri: String, // Simulated content/cache path
    val timestamp: Long, // Saved epoch milis
    val collectionName: String = "Unsorted",
    val isFavorite: Boolean = false
)

// Entity representing tracked creators
@Entity(tableName = "tracked_creators")
data class TrackedCreator(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val handle: String, // e.g. @bellapoarch
    val name: String,
    val followerCount: Long,
    val videoCount: Int,
    val totalLikes: Long,
    val avatarIdentifier: String, // "avatar_1", "avatar_2", etc.
    val category: String, // "Comedy", "Aesthetic", "Dances", "Cooking" etc.
    val notes: String = "",
    val rating: Float = 5.0f,
    val timestampAdded: Long = System.currentTimeMillis()
)

// Entity representing custom folders/collections
@Entity(tableName = "content_collections")
data class ContentCollection(
    @PrimaryKey(autoGenerate = false) val name: String, // Primary key is the clean name itself
    val description: String,
    val colorHex: String, // Color accent
    val iconName: String, // Icon ID
    val timestampAdded: Long = System.currentTimeMillis()
)

@Dao
interface VideoDao {
    @Query("SELECT * FROM downloaded_videos ORDER BY timestamp DESC")
    fun getAllVideos(): Flow<List<DownloadedVideo>>

    @Query("SELECT * FROM downloaded_videos WHERE collectionName = :collectionName ORDER BY timestamp DESC")
    fun getVideosByCollection(collectionName: String): Flow<List<DownloadedVideo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: DownloadedVideo)

    @Update
    suspend fun updateVideo(video: DownloadedVideo)

    @Delete
    suspend fun deleteVideo(video: DownloadedVideo)

    @Query("DELETE FROM downloaded_videos WHERE id = :id")
    suspend fun deleteVideoById(id: Int)

    @Query("DELETE FROM downloaded_videos")
    suspend fun clearAllVideos()
}

@Dao
interface CreatorDao {
    @Query("SELECT * FROM tracked_creators ORDER BY followerCount DESC")
    fun getAllCreators(): Flow<List<TrackedCreator>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreator(creator: TrackedCreator)

    @Update
    suspend fun updateCreator(creator: TrackedCreator)

    @Delete
    suspend fun deleteCreator(creator: TrackedCreator)

    @Query("DELETE FROM tracked_creators WHERE id = :id")
    suspend fun deleteCreatorById(id: Int)
}

@Dao
interface CollectionDao {
    @Query("SELECT * FROM content_collections ORDER BY timestampAdded ASC")
    fun getAllCollections(): Flow<List<ContentCollection>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: ContentCollection)

    @Delete
    suspend fun deleteCollection(collection: ContentCollection)
}

@Database(
    entities = [DownloadedVideo::class, TrackedCreator::class, ContentCollection::class],
    version = 1,
    exportSchema = false
)
abstract class PuretikDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun creatorDao(): CreatorDao
    abstract fun collectionDao(): CollectionDao
}
