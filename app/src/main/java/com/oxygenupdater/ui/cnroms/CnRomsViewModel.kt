package com.oxygenupdater.ui.cnroms

import android.os.Build
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oxygenupdater.models.ChinaDevices
import com.oxygenupdater.models.CnRomBuild
import com.oxygenupdater.models.CnRomDevice
import com.oxygenupdater.models.CnRomParser
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.repositories.CnRomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CnRomsViewModel @Inject constructor(
    private val repository: CnRomRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CnRomsState())
    val state = _state.asStateFlow()

    private val _detail = MutableStateFlow<CnRomDetailState?>(null)
    val detail = _detail.asStateFlow()

    private var detailJob: Job? = null

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
            if (url == null) repository.resolve(current.device, current.index)
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
        val build = repository.resolve(device, index)?.takeIf { it.label == label }
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
