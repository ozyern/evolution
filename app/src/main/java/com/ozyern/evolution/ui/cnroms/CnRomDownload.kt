package com.ozyern.evolution.ui.cnroms

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Immutable
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ozyern.evolution.models.CnRomBuild
import com.ozyern.evolution.workers.CnRomDownloadWorker
import java.util.concurrent.TimeUnit

/** The (single) in-app China ROM download, derived from its WorkManager work */
@Immutable
sealed interface CnRomDownload {
    val job: Job

    /** What's being downloaded; stored in work tags so it can be resumed after the app restarts */
    @Immutable
    data class Job(
        val deviceKey: String,
        val index: Int,
        val label: String,
        val fileName: String,
        val md5: String?,
        val title: String,
    )

    data class Queued(override val job: Job) : CnRomDownload

    data class Running(
        override val job: Job,
        val phase: Int,
        val bytesDone: Long,
        val totalBytes: Long,
        val bytesPerSecond: Long,
    ) : CnRomDownload {
        val fraction get() = if (totalBytes > 0) (bytesDone.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
        val secondsLeft get() = if (bytesPerSecond > 0 && totalBytes > 0) (totalBytes - bytesDone) / bytesPerSecond else -1L
    }

    data class Paused(override val job: Job, val bytesDone: Long) : CnRomDownload
    data class Done(override val job: Job, val path: String) : CnRomDownload
    data class Failed(override val job: Job, val error: Int) : CnRomDownload

    companion object {
        const val WorkName = "WorkUniqueCnRomDownload"
        private const val TagPrefix = "cnrom:"

        fun enqueue(workManager: WorkManager, job: Job) = workManager.enqueueUniqueWork(
            WorkName,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<CnRomDownloadWorker>()
                .setInputData(
                    workDataOf(
                        CnRomDownloadWorker.InputDeviceKey to job.deviceKey,
                        CnRomDownloadWorker.InputIndex to job.index,
                        CnRomDownloadWorker.InputLabel to job.label,
                        CnRomDownloadWorker.InputFileName to job.fileName,
                        CnRomDownloadWorker.InputMd5 to job.md5,
                        CnRomDownloadWorker.InputTitle to job.title,
                    )
                )
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .apply { job.toTags().forEach { addTag(it) } }
                .build()
        )

        fun jobFor(build: CnRomBuild, deviceKey: String, index: Int) = Job(
            deviceKey = deviceKey,
            index = index,
            label = build.label,
            fileName = build.fileName,
            md5 = build.md5,
            title = build.deviceName.ifEmpty { build.label }.let { "$it ${build.version}" },
        )

        /** @return `null` if there's nothing to show (no work, or cancelled without a partial file) */
        fun from(context: Context, info: WorkInfo?): CnRomDownload? {
            info ?: return null
            val job = jobFromTags(info.tags) ?: return null
            val progress = info.progress
            return when (info.state) {
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> {
                    // Retrying after an interruption looks like a pause with progress kept
                    val part = CnRomDownloadWorker.partFile(context, job.fileName)
                    if (part.exists() && info.runAttemptCount > 0) Paused(job, part.length()) else Queued(job)
                }

                WorkInfo.State.RUNNING -> if (progress.keyValueMap.isEmpty()) Queued(job) else Running(
                    job = job,
                    phase = progress.getInt(CnRomDownloadWorker.ProgressPhase, CnRomDownloadWorker.PhaseDownloading),
                    bytesDone = progress.getLong(CnRomDownloadWorker.ProgressBytesDone, 0),
                    totalBytes = progress.getLong(CnRomDownloadWorker.ProgressTotalBytes, 0),
                    bytesPerSecond = progress.getLong(CnRomDownloadWorker.ProgressBytesPerSecond, 0),
                )

                WorkInfo.State.SUCCEEDED -> Done(job, info.outputData.getString(CnRomDownloadWorker.OutputPath) ?: "")
                WorkInfo.State.FAILED -> Failed(job, info.outputData.getInt(CnRomDownloadWorker.OutputError, 0))
                WorkInfo.State.CANCELLED -> CnRomDownloadWorker.partFile(context, job.fileName).let {
                    if (it.exists()) Paused(job, it.length()) else null
                }
            }
        }

        @VisibleForTesting
        internal fun Job.toTags() = listOf(
            "${TagPrefix}key=$deviceKey",
            "${TagPrefix}index=$index",
            "${TagPrefix}label=$label",
            "${TagPrefix}file=$fileName",
            "${TagPrefix}title=$title",
        ) + listOfNotNull(md5?.let { "${TagPrefix}md5=$it" })

        @VisibleForTesting
        internal fun jobFromTags(tags: Set<String>): Job? {
            val map = tags.filter { it.startsWith(TagPrefix) }.associate {
                it.removePrefix(TagPrefix).substringBefore('=') to it.substringAfter('=')
            }
            return Job(
                deviceKey = map["key"] ?: return null,
                index = map["index"]?.toIntOrNull() ?: return null,
                label = map["label"] ?: return null,
                fileName = map["file"] ?: return null,
                md5 = map["md5"],
                title = map["title"] ?: map["label"] ?: return null,
            )
        }
    }
}
