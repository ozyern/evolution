package com.ozyern.evolution.repositories

import android.content.SharedPreferences
import androidx.collection.ArrayMap
import com.ozyern.evolution.utils.CrashLogger
import com.ozyern.evolution.BuildConfig
import com.ozyern.evolution.OxygenUpdater
import com.ozyern.evolution.apis.ServerApi
import com.ozyern.evolution.dao.ArticleDao
import com.ozyern.evolution.dao.UpdateDataDao
import com.ozyern.evolution.extensions.get
import com.ozyern.evolution.extensions.set
import com.ozyern.evolution.internal.NotSetL
import com.ozyern.evolution.internal.settings.KeyDevice
import com.ozyern.evolution.internal.settings.KeyDeviceId
import com.ozyern.evolution.internal.settings.KeyNotificationDelayInSeconds
import com.ozyern.evolution.internal.settings.KeyUpdateMethodId
import com.ozyern.evolution.models.Article
import com.ozyern.evolution.models.DeviceRequestFilter
import com.ozyern.evolution.models.DownloadErrorBody
import com.ozyern.evolution.models.FreshUpdateDataDownloadUrlBody
import com.ozyern.evolution.models.OsInfoBody
import com.ozyern.evolution.models.ServerStatus
import com.ozyern.evolution.models.SubmitOtaDbBody
import com.ozyern.evolution.models.SystemVersionProperties
import com.ozyern.evolution.models.UpdateData
import com.ozyern.evolution.utils.logInfo
import com.ozyern.evolution.utils.logVerbose
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepository @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val serverApi: ServerApi,
    private val updateDataDao: UpdateDataDao,
    private val articleDao: ArticleDao,
    private val crashlytics: CrashLogger,
) {

    suspend fun fetchFaq() = performServerRequest { serverApi.fetchFaq() }

    suspend fun fetchInstallGuide() = performServerRequest { serverApi.fetchInstallGuide() }

    suspend fun fetchDevices(filter: DeviceRequestFilter) = performServerRequest {
        serverApi.fetchDevices(filter.value)
    }

    val updateDataFlow
        get() = updateDataDao.getFlow()

    suspend fun fetchUpdateData(deviceId: Long, methodId: Long) = performServerRequest {
        serverApi.fetchUpdateData(
            deviceId = deviceId,
            updateMethodId = methodId,
            incrementalSystemVersion = SystemVersionProperties.otaVersion,
            osVersion = SystemVersionProperties.osVersion,
            osType = SystemVersionProperties.osType,
            fingerprint = SystemVersionProperties.fingerprint,
            oplusPipeline = SystemVersionProperties.pipeline,
            oplusPipelineCode = SystemVersionProperties.pipelineCode,
            oplusManifestHash = SystemVersionProperties.manifestHash,
            deviceMarketName = SystemVersionProperties.deviceMarketName,
            isEuBuild = SystemVersionProperties.isEuBuild,
            appVersion = BuildConfig.VERSION_NAME,
        )
    }.let {
        if (it?.shouldFetchMostRecent == true) performServerRequest {
            serverApi.fetchMostRecentUpdateData(
                deviceId = deviceId,
                updateMethodId = methodId,
            )
        } else it
    }.let {
        updateDataDao.refresh(it)
    }

    suspend fun getFreshUpdateDataDownloadUrl(
        deviceId: Long,
        methodId: Long,
        updateData: UpdateData,
    ) = performServerRequest {
        serverApi.getFreshUpdateDataDownloadUrl(
            deviceId = deviceId,
            updateMethodId = methodId,
            appVersion = BuildConfig.VERSION_NAME,
            body = FreshUpdateDataDownloadUrlBody(
                id = updateData.id,
                otaVersionNumber = updateData.otaVersionNumber,
                downloadUrl = updateData.downloadUrl,
                md5sum = updateData.md5sum,
            )
        )
    }.let {
        if (it == null || !it.success) return@let
        val result = it.result ?: return@let
        updateDataDao.updateDownloadUrl(updateData.id ?: return@let, result)
    }

    suspend fun fetchServerStatus() = performServerRequest { serverApi.fetchServerStatus() }.let { status ->
        val automaticInstallationEnabled = false
        val pushNotificationsDelaySeconds = sharedPreferences[KeyNotificationDelayInSeconds, 10]

        val response = if (status == null && OxygenUpdater.isNetworkAvailable.value) ServerStatus(
            ServerStatus.Status.UNREACHABLE,
            BuildConfig.VERSION_NAME,
            automaticInstallationEnabled,
            pushNotificationsDelaySeconds
        ) else status ?: ServerStatus(
            ServerStatus.Status.NORMAL,
            BuildConfig.VERSION_NAME,
            automaticInstallationEnabled,
            pushNotificationsDelaySeconds
        )

        sharedPreferences[KeyNotificationDelayInSeconds] = response.pushNotificationDelaySeconds

        response
    }

    suspend fun fetchServerMessages() = performServerRequest {
        serverApi.fetchServerMessages(
            deviceId = sharedPreferences[KeyDeviceId, NotSetL],
            updateMethodId = sharedPreferences[KeyUpdateMethodId, NotSetL],
        )
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun fetchNewsFromDb() = articleDao.getAll()

    @Suppress("RedundantSuspendModifier")
    suspend fun markAllReadLocally() = articleDao.markAllRead()

    suspend fun fetchNews() = performServerRequest {
        serverApi.fetchNews(
            deviceId = sharedPreferences[KeyDeviceId, NotSetL],
            updateMethodId = sharedPreferences[KeyUpdateMethodId, NotSetL],
        )
    }.let {
        if (!it.isNullOrEmpty()) articleDao.refreshArticles(it)

        // Note: we're returning a local copy so that read statuses are respected
        fetchNewsFromDb()
    }

    suspend fun fetchArticle(id: Long) = performServerRequest {
        serverApi.fetchArticle(id)
    }.let {
        if (it != null) articleDao.insertOrUpdate(it)

        articleDao.getById(id)
    }

    suspend fun toggleArticleReadLocally(
        article: Article,
        read: Boolean = !article.readState,
    ) = articleDao.toggleRead(article, read)

    suspend fun markArticleRead(id: Long) = performServerRequest {
        serverApi.markArticleRead(ArrayMap<String, Long>(1).apply { put("news_item_id", id) })
    }?.also {
        if (!it.success) crashlytics.logWarning(
            TAG,
            "Failed to mark article as read on the server: ${it.errorMessage}"
        )
    }

    suspend fun fetchUpdateMethodsForDevice(deviceId: Long) = performServerRequest {
        serverApi.fetchUpdateMethodsForDevice(deviceId)
    }

    suspend fun osInfoHeartbeat(fromIntentAction: String) = performServerRequest {
        serverApi.osInfoHeartbeat(OsInfoBody.from(fromIntentAction))
    }

    suspend fun submitOtaDbRows(rows: List<Map<String, String?>>) = performServerRequest {
        serverApi.submitOtaDbRows(SubmitOtaDbBody.from(rows, sharedPreferences[KeyDevice, "<UNKNOWN>"]))
    }

    suspend fun logDownloadError(
        url: String?,
        filename: String?,
        version: String?,
        otaVersion: String?,
        httpCode: Int,
        httpMessage: String?,
    ) = performServerRequest {
        serverApi.logDownloadError(
            DownloadErrorBody(
                url = url,
                filename = filename,
                version = version,
                otaVersion = otaVersion,
                httpCode = httpCode,
                httpMessage = httpMessage,
                appVersion = BuildConfig.VERSION_NAME,
                deviceName = sharedPreferences[KeyDevice, "<UNKNOWN>"],
                actualDeviceName = SystemVersionProperties.deviceProductName,
            )
        )
    }

    @Suppress("RedundantSuspendModifier")
    private suspend inline fun <R> performServerRequest(block: () -> Response<R>) = try {
        val response = block()
        if (response.isSuccessful) response.body().apply {
            logVerbose(TAG, "Response: $this")
        } else null
    } catch (e: Exception) {
        // Don't log cancellations to crashlytics, we don't care
        if (e is CancellationException) logInfo(TAG, e.message ?: "CancellationException")
        else crashlytics.logError(TAG, "Error performing request", e)
        null
    }

    companion object {
        private const val TAG = "ServerRepository"
    }
}
