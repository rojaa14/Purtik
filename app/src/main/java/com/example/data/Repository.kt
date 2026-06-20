package com.example.data

import kotlinx.coroutines.flow.Flow

class PuretikRepository(
    private val videoDao: VideoDao,
    private val creatorDao: CreatorDao,
    private val collectionDao: CollectionDao
) {
    // Flows
    val allVideos: Flow<List<DownloadedVideo>> = videoDao.getAllVideos()
    val allCreators: Flow<List<TrackedCreator>> = creatorDao.getAllCreators()
    val allCollections: Flow<List<ContentCollection>> = collectionDao.getAllCollections()

    fun getVideosByCollection(collectionName: String): Flow<List<DownloadedVideo>> {
        return videoDao.getVideosByCollection(collectionName)
    }

    // Video Operations
    suspend fun insertVideo(video: DownloadedVideo) {
        videoDao.insertVideo(video)
    }

    suspend fun updateVideo(video: DownloadedVideo) {
        videoDao.updateVideo(video)
    }

    suspend fun deleteVideo(video: DownloadedVideo) {
        videoDao.deleteVideo(video)
    }

    suspend fun deleteVideoById(id: Int) {
        videoDao.deleteVideoById(id)
    }

    suspend fun clearAllVideos() {
        videoDao.clearAllVideos()
    }

    // Creator Operations
    suspend fun insertCreator(creator: TrackedCreator) {
        creatorDao.insertCreator(creator)
    }

    suspend fun updateCreator(creator: TrackedCreator) {
        creatorDao.updateCreator(creator)
    }

    suspend fun deleteCreator(creator: TrackedCreator) {
        creatorDao.deleteCreator(creator)
    }

    suspend fun deleteCreatorById(id: Int) {
        creatorDao.deleteCreatorById(id)
    }

    // Collection Operations
    suspend fun insertCollection(collection: ContentCollection) {
        collectionDao.insertCollection(collection)
    }

    suspend fun deleteCollection(collection: ContentCollection) {
        collectionDao.deleteCollection(collection)
    }
}
