package com.openagnes.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.openagnes.app.R
import com.openagnes.app.viewmodel.EditViewModel

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: EditViewModel) {
    val state by viewModel.uiState.collectAsState()
    var showApiKeyDialog by remember { mutableStateOf(false) }
    val showResult = state.resultB64 != null && state.resultBitmap != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Open Agnes",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = { showApiKeyDialog = true }) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(R.drawable.ic_settings),
                            contentDescription = "API-Key"
                        )
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color(0xFF000000)
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            state.error?.let {
                Text(text = it, color = Color(0xFFCF6679), style = MaterialTheme.typography.bodyMedium)
            }
            if (showResult) {
                ResultScreen(
                    viewModel = viewModel,
                    state = state,
                    onEditImage = { },
                    onDownload = { viewModel.saveResult() }
                )
            } else {
                EditScreen(viewModel = viewModel, state = state)
            }
            ApiKeyDialog(
                visible = showApiKeyDialog,
                onDismiss = { showApiKeyDialog = false },
                onSave = { key ->
                    viewModel.saveApiKey(key)
                    showApiKeyDialog = false
                }
            )
        }
    }
}

internal fun android.content.Context.resString(id: Int, vararg args: Any): String =
    getString(id, *args)
