package com.zheting.mobile.core.network

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 网易云端点客户端。字段/参数规则逐条对齐原项目 src/lib/api/client.ts。
 *
 * cookie 与 randomCNIP 由 SessionInterceptor 统一注入，方法签名只保留业务参数。
 */
interface NeteaseApi : SongUrlEndpoints {

    // ---- 会话（phase 1 已有） ----

    @FormUrlEncoded
    @POST("/login/status")
    suspend fun loginStatus(
        @Field("timestamp") timestampMs: Long,
        @Field("ua") ua: String = "pc",
    ): AccountResultDto

    @FormUrlEncoded
    @POST("/user/account")
    suspend fun userAccount(
        @Field("timestamp") timestampMs: Long,
    ): AccountResultDto

    @POST("/logout")
    suspend fun logout(): AccountResultDto

    // ---- 读端点（契约 §2） ----

    @GET("/personalized")
    suspend fun personalized(
        @Query("limit") limit: Int = 10,
    ): ListResponse

    @GET("/personalized/newsong")
    suspend fun personalizedNewSong(
        @Query("limit") limit: Int = 12,
    ): ListResponse

    @GET("/cloudsearch")
    suspend fun cloudsearch(
        @Query("keywords") keywords: String,
        @Query("limit") limit: Int = 30,
        @Query("offset") offset: Int = 0,
    ): CloudsearchResponse

    @GET("/playlist/detail")
    suspend fun playlistDetail(
        @Query("id") id: String,
    ): PlaylistDetailResponse

    @GET("/song/detail")
    suspend fun songDetail(
        @Query("ids") ids: String,
    ): SongDetailResponse

    // ---- 播放 URL（契约 §2.6；对齐 url-resolver.ts 候选链端点） ----

    @GET("/song/url/v1")
    override suspend fun songUrlV1(
        @Query("id") id: String,
        @Query("level") level: String,
        @Query("unblock") unblock: String,
    ): SongUrlResponse

    @GET("/song/url/match")
    override suspend fun songUrlMatch(
        @Query("id") id: String,
    ): SongUrlResponse

    @GET("/song/url")
    override suspend fun songUrlLegacy(
        @Query("id") id: String,
        @Query("br") br: Long,
    ): SongUrlResponse
}