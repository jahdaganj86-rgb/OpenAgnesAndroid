package com.openagnes.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.openagnes.app.R
import com.openagnes.app.viewmodel.EditViewModel

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
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                },
                actions = {
                    IconButton(onClick = { showApiKeyDialog = true }) {
                        Icon(
                            imageVector = androidx.compose.material3.Icons.Default.Settings,
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

private fun android.content.Context.resString(id: Int, vararg args: Any): String =
    getString(id, *args)
