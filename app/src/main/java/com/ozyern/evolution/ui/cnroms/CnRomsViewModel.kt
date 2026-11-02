package com.ozyern.evolution.ui.cnroms

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.compose.runtime.Immutable
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozyern.evolution.models.ChinaDevices
import com.ozyern.evolution.models.CnRomBuild
import com.ozyern.evolution.models.CnRomDevice
import com.ozyern.evolution.models.CnRomParser
import com.ozyern.evolution.models.SystemVersionProperties
import com.ozyern.evolution.repositories.CnRomRepository
import androidx.work.WorkManager
import com.ozyern.evolution.workers.CnRomDownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val KeyDismissedWorkId = "cnRomDownloadDismissedWorkId"

@HiltViewModel
class CnRomsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: CnRomRepository,
    private val workManager: WorkManager,
    private val sharedPreferences: SharedPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(CnRomsState())
    val state = _state.asStateFlow()

    private val _detail = MutableStateFlow<CnRomDetailState?>(null)
    val detail = _detail.asStateFlow()

    private var detailJob: Job? = null

    /**
     * ID of the work the user has cancelled or dismissed, which must no longer be shown. WorkManager only
     * forgets finished work on its own schedule, and [WorkManager.pruneWork] isn't an option because it
     * would also wipe the OTA downloader's finished work, which [com.ozyern.evolution.viewmodels.MainViewModel]
     * derives its download status from.
     */
    private val dismissedWorkId = MutableStateFlow(sharedPreferences.getString(KeyDismissedWorkId, null))

    /** The in-app download, if any. Lives in WorkManager, so it survives the app being closed. */
    val download = combine(
        workManager.getWorkInfosForUniqueWorkFlow(CnRomDownload.WorkName),
        dismissedWorkId,
    ) { infos, dismissed ->
        val info = infos.lastOrNull()?.takeUnless { it.id.toString() == dismissed }
        withContext(Dispatchers.IO) { CnRomDownload.from(context, info) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun startDownload() {
        val current = _detail.value ?: return
        val build = current.build ?: return
        val previous = download.value?.job
        val job = CnRomDownload.jobFor(build, current.device.key, current.index)
        CnRomDownload.enqueue(workManager, job)

        // Starting a build replaces a paused/failed one, whose partial file would otherwise linger
        if (previous != null && previous.fileName != job.fileName) viewModelScope.launch(Dispatchers.IO) {
            CnRomDownloadWorker.partFile(context, previous.fileName).delete()
        }
    }

    /** Stops the download but keeps the partial file, so it can be resumed */
    fun pauseDownload() = workManager.cancelUniqueWork(CnRomDownload.WorkName).let {}

    fun resumeDownload() {
        val job = download.value?.job ?: return
        CnRomDownload.enqueue(workManager, job)
    }

    /** Stops the download (if still running), deletes the partial file, and hides it from the UI */
    fun cancelDownload() {
        val job = download.value?.job ?: return
        viewModelScope.launch(Dispatchers.IO) {
            // Must finish even if the user leaves the screen, to not strand a multi-GB partial file
            withContext(NonCancellable) {
                val workId = workManager.getWorkInfosForUniqueWork(CnRomDownload.WorkName).get().lastOrNull()?.id?.toString()
                sharedPreferences.edit { putString(KeyDismissedWorkId, workId) }
                dismissedWorkId.value = workId

                workManager.cancelUniqueWork(CnRomDownload.WorkName).result.get()
                // Give the worker a moment to stop writing before deleting what it wrote
                delay(500)
                CnRomDownloadWorker.partFile(context, job.fileName).delete()
            }
        }
    }

    /** Opens the detail sheet of the download's build (switching device if needed) */
    fun openDownload() {
        val job = download.value?.job ?: return
        _state.value.devices.firstOrNull { it.key == job.deviceKey }?.let(::select) ?: return
        open(job.index)
    }

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(refreshing = true) }
        val devices = repository.fetchDevices(force = _state.value.devices.isNotEmpty())
        _state.update { old ->
            if (devices == null) old.copy(refreshing = false, error = old.devices.isEmpty()) else {
                val matched = CnRomParser.match(
                    devices = devices,
                    model = Build.MODEL,
                    marketName = ChinaDevices.current ?: SystemVersionProperties.deviceMarketName,
                )
                CnRomsState(
                    refreshing = false,
                    error = false,
                    devices = devices,
                    thisDeviceKey = matched?.key,
                    // Keep the user's choice across refreshes; otherwise default to this phone
                    selectedKey = old.selectedKey?.takeIf { key -> devices.any { it.key == key } } ?: matched?.key,
                )
            }
        }
    }

    fun select(device: CnRomDevice) = _state.update { it.copy(selectedKey = device.key) }

    /** Opens the detail sheet for build #[index] of the selected device, and resolves its details */
    fun open(index: Int) {
        val device = _state.value.selected ?: return
        val label = device.builds.getOrNull(index) ?: return
        detailJob?.cancel()
        _detail.value = CnRomDetailState(device = device, index = index, label = label)
        detailJob = viewModelScope.launch { resolve(device, index, label) }
    }

    fun retryDetail() {
        val current = _detail.value ?: return
        detailJob?.cancel()
        _detail.value = current.copy(error = false, build = null)
        detailJob = viewModelScope.launch { resolve(current.device, current.index, current.label) }
    }

    fun closeDetail() {
        detailJob?.cancel()
        _detail.value = null
    }

    /**
     * Fetches a fresh signed download link. The archive keeps only one resolved build per session,
     * so if the link can't be fetched (e.g. session expired, or another build was opened since), the
     * build is resolved again and we retry once.
     */
    fun fetchDownloadUrl(onResult: (String?) -> Unit) {
        val current = _detail.value ?: return
        val build = current.build ?: return
        _detail.value = current.copy(fetchingLink = true)
        viewModelScope.launch {
            var url = repository.fetchDownloadUrl(build)
            if (url == null) repository.resolve(current.device.key, current.index)
                ?.takeIf { it.label == current.label }
                ?.let { fresh ->
                    _detail.update { it?.copy(build = fresh) }
                    url = repository.fetchDownloadUrl(fresh)
                }
            _detail.update { it?.copy(fetchingLink = false) }
            onResult(url)
        }
    }

    private suspend fun resolve(device: CnRomDevice, index: Int, label: String) {
        // Guard against the session returning a different build than requested
        val build = repository.resolve(device.key, index)?.takeIf { it.label == label }
        _detail.update {
            if (it?.label != label) it // user moved on
            else it.copy(build = build, error = build == null)
        }
    }
}

@Immutable
data class CnRomsState(
    val refreshing: Boolean = true,
    val error: Boolean = false,
    val devices: List<CnRomDevice> = listOf(),
    /** Archive device matching this phone, if any */
    val thisDeviceKey: String? = null,
    val selectedKey: String? = null,
) {
    val selected get() = devices.firstOrNull { it.key == selectedKey }
}

@Immutable
data class CnRomDetailState(
    val device: CnRomDevice,
    val index: Int,
    val label: String,
    val build: CnRomBuild? = null,
    val error: Boolean = false,
    val fetchingLink: Boolean = false,
)
