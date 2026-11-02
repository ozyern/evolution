package com.ozyern.evolution.workers

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Notification.CATEGORY_PROGRESS
import android.content.ContentValues
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_LOW
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ozyern.evolution.R
import com.ozyern.evolution.extensions.formatFileSize
import com.ozyern.evolution.repositories.CnRomRepository
import com.ozyern.evolution.utils.NotificationChannels.DownloadAndInstallationGroup.DownloadStatusNotifChannelId
import com.ozyern.evolution.utils.NotificationIds
import com.ozyern.evolution.utils.logInfo
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/**
 * Downloads a China ROM inside the app: resumable (the partial file is kept when paused/interrupted, and
 * continued with an HTTP Range request), verified against the archive's MD5, then saved to
 * `Download/Evolution`. Saving uses a plain move if the app has "All files access", and MediaStore
 * otherwise, which needs no permission at all.
 *
 * Signed CDN links are short-lived, so a fresh one is fetched for every attempt, and again if the CDN
 * rejects an expired link mid-way.
 */
@HiltWorker
class CnRomDownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted parameters: WorkerParameters,
    private val repository: CnRomRepository,
    private val workManager: WorkManager,
) : CoroutineWorker(context, parameters) {

    private val deviceKey = inputData.getString(InputDeviceKey)
    private val index = inputData.getInt(InputIndex, -1)
    private val label = inputData.getString(InputLabel)
    private val fileName = inputData.getString(InputFileName)
    private val md5 = inputData.getString(InputMd5)
    private val title = inputData.getString(InputTitle) ?: label ?: ""

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private var notification: Notification? = null
    private var lastPublishMs = 0L

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (deviceKey == null || index < 0 || label == null || fileName == null) return@withContext failure(ErrorInternal)
        trySetForeground(progressNotification(null, 0, 0))

        val partFile = partFile(context, fileName)
        try {
            if (!download(partFile)) return@withContext failure(ErrorLink)
        } catch (e: IOException) {
            logInfo(TAG, "Download interrupted", e)
            // Keep the partial file; WorkManager retries with backoff once we're back online
            return@withContext if (runAttemptCount < MaxAttempts) Result.retry() else failure(ErrorNetwork)
        }

        if (md5 != null) {
            publish(PhaseVerifying, partFile.length(), partFile.length(), 0, force = true)
            val actual = partFile.inputStream().use { md5Of(it) }
            if (!actual.equals(md5, ignoreCase = true)) {
                logInfo(TAG, "MD5 mismatch: expected $md5, got $actual")
                partFile.delete()
                return@withContext failure(ErrorChecksum)
            }
        }

        publish(PhaseSaving, partFile.length(), partFile.length(), 0, force = true)
        val path = try {
            save(partFile, fileName)
        } catch (e: IOException) {
            logInfo(TAG, "Couldn't save to Downloads", e)
            return@withContext failure(ErrorStorage)
        }

        showCompleteNotification(path)
        Result.success(workDataOf(OutputPath to path))
    }

    /** @return `false` if no download link could be obtained */
    private suspend fun download(partFile: File): Boolean {
        var url = freshUrl() ?: return false
        var refreshed = false

        while (true) {
            val start = partFile.length()
            val request = Request.Builder().url(url).apply {
                if (start > 0) header("Range", "bytes=$start-")
            }.build()

            client.newCall(request).execute().use { response ->
                when (response.code) {
                    // Expired/forbidden link: get a new one, once
                    401, 403, 410 -> {
                        if (refreshed) throw IOException("HTTP ${response.code} after refreshing link")
                        refreshed = true
                        url = freshUrl() ?: return false
                        return@use
                    }

                    // Range starts at/after the end => we already have the whole file
                    416 -> return true

                    200, 206 -> {
                        val append = response.code == 206
                        val body = response.body
                        val length = body.contentLength()
                        // 0 => unknown (no Content-Length), which the UI shows as indeterminate
                        val total = if (length < 0) 0 else (if (append) start else 0) + length
                        RandomAccessFile(partFile, "rw").use { file ->
                            if (append) file.seek(start) else file.setLength(0)
                            copy(body.byteStream(), file, if (append) start else 0, total)
                        }
                        return true
                    }

                    else -> throw IOException("HTTP ${response.code}")
                }
            }
        }
    }

    private suspend fun copy(input: InputStream, output: RandomAccessFile, initial: Long, total: Long) {
        val buffer = ByteArray(BufferSize)
        var done = initial
        var windowStartMs = System.currentTimeMillis()
        var windowStartBytes = done
        var speed = 0L

        while (true) {
            coroutineContext.ensureActive() // pause/cancel => stop promptly, keeping what we have
            val read = input.read(buffer)
            if (read < 0) break
            output.write(buffer, 0, read)
            done += read

            val now = System.currentTimeMillis()
            if (now - windowStartMs >= MinMsBetweenProgressPublish) {
                speed = (done - windowStartBytes) * 1000 / (now - windowStartMs)
                windowStartMs = now
                windowStartBytes = done
            }
            publish(PhaseDownloading, done, total, speed)
        }
    }

    private suspend fun freshUrl(): String? {
        val build = repository.resolve(deviceKey!!, index)?.takeIf { it.label == label } ?: return null
        return repository.fetchDownloadUrl(build)
    }

    private suspend fun publish(phase: Int, done: Long, total: Long, speed: Long, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastPublishMs < MinMsBetweenProgressPublish) return
        lastPublishMs = now

        setProgress(
            workDataOf(
                ProgressPhase to phase,
                ProgressBytesDone to done,
                ProgressTotalBytes to total,
                ProgressBytesPerSecond to speed,
            )
        )
        trySetForeground(progressNotification(phase, done, total))
    }

    /**
     * @return a human-readable location of the saved file
     * @throws IOException if it couldn't be saved anywhere public
     */
    @SuppressLint("NewApi", "InlinedApi") // MediaStore path only runs on Q+, see `canWriteDirectly`
    @Throws(IOException::class)
    private fun save(partFile: File, name: String): String {
        val canWriteDirectly = SDK_INT < VERSION_CODES.Q ||
                (SDK_INT >= VERSION_CODES.R && Environment.isExternalStorageManager())

        if (canWriteDirectly) {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), SubDirectory)
            if (!dir.exists() && !dir.mkdirs()) throw IOException("Couldn't create $dir")
            val target = File(dir, name)
            if (target.exists() && !target.delete()) throw IOException("Couldn't replace $target")
            // Same volume, so this is usually an instant rename
            if (!partFile.renameTo(target)) {
                partFile.copyTo(target, overwrite = true)
                partFile.delete()
            }
            return target.absolutePath
        }

        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$SubDirectory")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("MediaStore insert failed")
        try {
            val output: OutputStream = resolver.openOutputStream(uri) ?: throw IOException("No output stream")
            output.use { out -> partFile.inputStream().use { it.copyTo(out, BufferSize) } }
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (e: IOException) {
            resolver.delete(uri, null, null)
            throw e
        }
        partFile.delete()
        return "${Environment.DIRECTORY_DOWNLOADS}/$SubDirectory/$name"
    }

    private fun progressNotification(phase: Int?, done: Long, total: Long): Notification {
        val text = when (phase) {
            PhaseVerifying -> context.getString(R.string.cn_roms_verifying)
            PhaseSaving -> context.getString(R.string.cn_roms_saving)
            null -> context.getString(R.string.download_pending)
            else -> if (total > 0) "${context.formatFileSize(done)} / ${context.formatFileSize(total)}" else context.formatFileSize(done)
        }
        val percent = if (total > 0) (done * 100 / total).toInt() else 0
        return NotificationCompat.Builder(context, DownloadStatusNotifChannelId)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(text)
            .setProgress(100, percent, phase != PhaseDownloading || total <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(CATEGORY_PROGRESS)
            .setPriority(PRIORITY_LOW)
            .addAction(android.R.drawable.ic_delete, context.getString(R.string.cn_roms_pause), workManager.createCancelPendingIntent(id))
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .build()
            .also { notification = it }
    }

    @SuppressLint("MissingPermission") // best-effort; nothing to do if notifications are off
    private fun showCompleteNotification(path: String) = try {
        NotificationManagerCompat.from(context).notify(
            NotificationIds.LocalCnRomDownload,
            NotificationCompat.Builder(context, DownloadStatusNotifChannelId)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(context.getString(R.string.cn_roms_download_complete, title))
                .setContentText(path)
                .setStyle(NotificationCompat.BigTextStyle().bigText(path))
                .setAutoCancel(true)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .build()
        )
    } catch (_: Exception) {
        // ignore
    }

    private suspend fun trySetForeground(notification: Notification) = try {
        setForeground(foregroundInfo(notification))
    } catch (e: Exception) {
        logInfo(TAG, "setForeground failed", e)
    }

    /** Must be overridden because we use [androidx.work.WorkRequest.Builder.setExpedited] */
    override suspend fun getForegroundInfo() = foregroundInfo(notification ?: progressNotification(null, 0, 0))

    private fun foregroundInfo(notification: Notification) = if (SDK_INT >= VERSION_CODES.Q) ForegroundInfo(
        NotificationIds.LocalCnRomDownloadForeground, notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC // keep in sync with `foregroundServiceType` in AndroidManifest
    ) else ForegroundInfo(NotificationIds.LocalCnRomDownloadForeground, notification)

    private fun failure(error: Int) = Result.failure(Data.Builder().putInt(OutputError, error).build())

    companion object {
        private const val TAG = "CnRomDownloadWorker"

        const val InputDeviceKey = "deviceKey"
        const val InputIndex = "index"
        const val InputLabel = "label"
        const val InputFileName = "fileName"
        const val InputMd5 = "md5"
        const val InputTitle = "title"

        const val ProgressPhase = "phase"
        const val ProgressBytesDone = "bytesDone"
        const val ProgressTotalBytes = "totalBytes"
        const val ProgressBytesPerSecond = "bytesPerSecond"

        const val PhaseDownloading = 0
        const val PhaseVerifying = 1
        const val PhaseSaving = 2

        const val OutputPath = "path"
        const val OutputError = "error"

        const val ErrorInternal = 0
        const val ErrorLink = 1
        const val ErrorNetwork = 2
        const val ErrorChecksum = 3
        const val ErrorStorage = 4

        const val SubDirectory = "Evolution"
        private const val BufferSize = 256 * 1024
        private const val MaxAttempts = 5

        /** Partial downloads live in app-specific storage until complete */
        fun partFile(context: Context, fileName: String): File {
            // getExternalFilesDir creates its directory, but the internal fallback doesn't exist yet
            val directory = context.getExternalFilesDir(PartDirectory)
                ?: File(context.filesDir, PartDirectory).apply { mkdirs() }
            return File(directory, "$fileName.part")
        }

        private const val PartDirectory = "cnroms"

        fun md5Of(input: InputStream): String {
            val digest = MessageDigest.getInstance("MD5")
            val buffer = ByteArray(BufferSize)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
