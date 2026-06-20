package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class TikWmResponse(
    @Json(name = "code") val code: Int,
    @Json(name = "msg") val msg: String?,
    @Json(name = "data") val data: TikWmVideoData?
)

@JsonClass(generateAdapter = true)
data class TikWmVideoData(
    @Json(name = "id") val id: String?,
    @Json(name = "title") val title: String?,
    @Json(name = "cover") val cover: String?,
    @Json(name = "origin_cover") val originCover: String?,
    @Json(name = "duration") val duration: Int?,
    @Json(name = "play") val playUrl: String?,
    @Json(name = "wmplay") val wmPlayUrl: String?,
    @Json(name = "hdplay") val hdPlayUrl: String?,
    @Json(name = "size") val size: Long?,
    @Json(name = "hd_size") val hdSize: Long?,
    @Json(name = "wm_size") val wmSize: Long?,
    @Json(name = "play_count") val playCount: Long?,
    @Json(name = "digg_count") val diggCount: Long?,
    @Json(name = "comment_count") val commentCount: Long?,
    @Json(name = "share_count") val shareCount: Long?,
    @Json(name = "download_count") val downloadCount: Long?,
    @Json(name = "author") val author: TikWmAuthor?,
    @Json(name = "music") val musicUrl: String?,
    @Json(name = "music_info") val musicInfo: TikWmMusicInfo?
)

@JsonClass(generateAdapter = true)
data class TikWmAuthor(
    @Json(name = "id") val id: String?,
    @Json(name = "unique_id") val uniqueId: String?,
    @Json(name = "nickname") val nickname: String?,
    @Json(name = "avatar") val avatarUrl: String?
)

@JsonClass(generateAdapter = true)
data class TikWmMusicInfo(
    @Json(name = "id") val id: String?,
    @Json(name = "title") val title: String?,
    @Json(name = "play") val playUrl: String?,
    @Json(name = "author") val author: String?,
    @Json(name = "cover") val coverUrl: String?
)

interface TikWmApiService {
    @GET("api/")
    suspend fun fetchVideoDetails(
        @Query("url") url: String,
        @Query("hd") hd: Int = 1
    ): TikWmResponse
}

object RetrofitClient {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val apiService: TikWmApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.tikwm.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TikWmApiService::class.java)
    }
}
