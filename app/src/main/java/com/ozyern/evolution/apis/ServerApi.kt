package com.ozyern.evolution.apis

import com.ozyern.evolution.models.Article
import com.ozyern.evolution.models.Device
import com.ozyern.evolution.models.DownloadErrorBody
import com.ozyern.evolution.models.FreshUpdateDataDownloadUrlBody
import com.ozyern.evolution.models.InAppFaq
import com.ozyern.evolution.models.InstallGuide
import com.ozyern.evolution.models.OsInfoBody
import com.ozyern.evolution.models.ServerMessage
import com.ozyern.evolution.models.ServerPostResult
import com.ozyern.evolution.models.ServerStatus
import com.ozyern.evolution.models.SubmitOtaDbBody
import com.ozyern.evolution.models.UpdateData
import com.ozyern.evolution.models.UpdateMethod
import com.ozyern.evolution.ui.currentLanguage
import com.ozyern.evolution.ui.currentLocale
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Used to communicate with the Oxygen Updater backend */
interface ServerApi {

    @GET("flattenedFaq")
    suspend fun fetchFaq(
        @Query("language") language: String = currentLanguage,
    ): Response<List<InAppFaq>>

    @GET("installGuide")
    suspend fun fetchInstallGuide(
        @Query("language") language: String = currentLanguage,
    ): Response<List<InstallGuide>>

    @GET("devices/{filter}")
    suspend fun fetchDevices(@Path("filter") filter: String): Response<List<Device>>

    @GET("updateData/{deviceId}/{updateMethodId}/{incrementalSystemVersion}")
    suspend fun fetchUpdateData(
        @Path("deviceId") deviceId: Long,
        @Path("updateMethodId") updateMethodId: Long,
        @Path("incrementalSystemVersion") incrementalSystemVersion: String,
        @Query("osVersion") osVersion: String,
        @Query("osType") osType: String,
        @Query("fingerprint") fingerprint: String,
        @Query("oplusPipeline") oplusPipeline: String,
        @Query("oplusPipelineCode") oplusPipelineCode: String,
        @Query("oplusManifestHash") oplusManifestHash: String,
        @Query("deviceMarketName") deviceMarketName: String,
        @Query("isEuBuild") isEuBuild: Boolean,
        @Query("appVersion") appVersion: String,
    ): Response<UpdateData>

    @GET("mostRecentUpdateData/{deviceId}/{updateMethodId}")
    suspend fun fetchMostRecentUpdateData(
        @Path("deviceId") deviceId: Long,
        @Path("updateMethodId") updateMethodId: Long,
    ): Response<UpdateData>

    @POST("updateDataDownloadUrl/{deviceId}/{updateMethodId}")
    suspend fun getFreshUpdateDataDownloadUrl(
        @Path("deviceId") deviceId: Long,
        @Path("updateMethodId") updateMethodId: Long,
        @Query("appVersion") appVersion: String,
        @Body body: FreshUpdateDataDownloadUrlBody,
    ): Response<ServerPostResult>

    @GET("serverStatus")
    suspend fun fetchServerStatus(): Response<ServerStatus>

    @GET("serverMessages/{deviceId}/{updateMethodId}")
    suspend fun fetchServerMessages(
        @Path("deviceId") deviceId: Long,
        @Path("updateMethodId") updateMethodId: Long,
        @Query("language") language: String = currentLanguage,
    ): Response<List<ServerMessage>>

    @GET("updateMethods/{deviceId}")
    suspend fun fetchUpdateMethodsForDevice(
        @Path("deviceId") deviceId: Long,
        @Query("language") language: String = currentLanguage,
    ): Response<List<UpdateMethod>>

    @GET("news/{deviceId}/{updateMethodId}")
    suspend fun fetchNews(
        @Path("deviceId") deviceId: Long,
        @Path("updateMethodId") updateMethodId: Long,
        @Query("language") language: String = if (currentLocale.language == "nl") "nl" else "en",
    ): Response<List<Article>>

    @GET("news-item/{id}")
    suspend fun fetchArticle(
        @Path("id") id: Long,
        @Query("language") language: String = if (currentLocale.language == "nl") "nl" else "en",
    ): Response<Article>

    @POST("news-read")
    suspend fun markArticleRead(@Body id: Map<String, Long>): Response<ServerPostResult>

    @POST("osInfoHeartbeat")
    suspend fun osInfoHeartbeat(@Body body: OsInfoBody): Response<ServerPostResult>

    /**
     * @param body includes the following fields:
     * * `rows: List<Map<String, Any?>>`,
     * * `otaVersion: String`,
     * * `isEuBuild: Boolean`,
     * * `appVersion: String`,
     * * `deviceName: String`,
     * * `actualDeviceName: String`,
     */
    @POST("submit-update-url")
    suspend fun submitOtaDbRows(@Body body: SubmitOtaDbBody): Response<ServerPostResult>

    /**
     * @param body includes the following fields:
     * * `url: String`
     * * `otaVersion: String`,
     * * `httpCode: Int`,
     * * `httpMessage: String`,
     * * `appVersion: String`,
     * * `deviceName: String`
     */
    @POST("log-download-error")
    suspend fun logDownloadError(@Body body: DownloadErrorBody): Response<ServerPostResult>
}
