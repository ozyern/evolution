package com.ozyern.evolution.ui.faq

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozyern.evolution.models.InAppFaq
import com.ozyern.evolution.repositories.ServerRepository
import com.ozyern.evolution.ui.RefreshAwareState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FaqViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
) : ViewModel() {

    private val refreshingFlow = MutableStateFlow(true)
    private val flow = MutableStateFlow<List<InAppFaq>>(listOf())

    val state = refreshingFlow.combine(flow) { refreshing, list ->
        RefreshAwareState(refreshing, list)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RefreshAwareState(refreshingFlow.value, flow.value)
    )

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch(Dispatchers.IO) {
        refreshingFlow.value = true
        flow.emit(serverRepository.fetchFaq() ?: listOf())
        refreshingFlow.value = false
    }
}
