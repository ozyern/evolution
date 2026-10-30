package com.ozyern.evolution.ui.main

import androidx.collection.IntIntPair
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ozyern.evolution.R

@Composable
fun MainSnackbar(snackbarText: IntIntPair?) {
    val snackbarHostState = remember { SnackbarHostState() }
    SnackbarHost(hostState = snackbarHostState, modifier = Modifier.statusBarsPadding())

    if (snackbarText == null) {
        snackbarHostState.currentSnackbarData?.dismiss()
        return
    }

    val message = stringResource(snackbarText.first)
    val action = stringResource(snackbarText.second)
    LaunchedEffect(snackbarText) {
        snackbarHostState.showSnackbar(message, action, false, SnackbarDuration.Indefinite)
    }
}

val NoConnectionSnackbarData = IntIntPair(
    R.string.error_app_requires_network_connection, android.R.string.ok
)
