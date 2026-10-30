package com.openagnes.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openagnes.app.R
import com.openagnes.app.viewmodel.EditUiState
import com.openagnes.app.viewmodel.EditViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: EditViewModel) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.initApiKeyStore() }
    BackHandler(enabled = state.editorOpen) { viewModel.closeEditor() }

    when {
        state.editorOpen -> EditImageScreen(
            state = state,
            onPromptChange = viewModel::onPromptChange,
            onQuickPrompt = viewModel::applyQuickPrompt,
            onSend = viewModel::submitEditor,
            onClose = viewModel::closeEditor
        )
        else -> HomeScreen(viewModel, state)
    }

    ApiKeyDialog(
        visible = state.showApiKeyDialog,
        onDismiss = viewModel::dismissApiKeyDialog,
        onSave = viewModel::onApiKeySaved
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(viewModel: EditViewModel, state: EditUiState) {
    val scope = rememberCoroutineScope()

    val pickLauncher = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onImagePicked(uri)
    }

    Scaffold(
        containerColor = AgnesColors.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = LocalContext.current.resString(R.string.app_title),
                        color = AgnesColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.requestApiKeyDialog() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_menu),
                            contentDescription = LocalContext.current.resString(R.string.menu_api_key),
                            tint = AgnesColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AgnesColors.Background)
            )
        },
        bottomBar = {
            TypeBar(
                prompt = state.prompt,
                onPromptChange = { viewModel.onPromptChange(it) },
                onPickImage = { pickLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
                onSend = { scope.launch { viewModel.enhance(); viewModel.generate() } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            state.error?.let {
                Text(
                    text = it,
                    color = AgnesColors.Error,
                    fontSize = 14.sp
                )
            }
            EditScreen(viewModel = viewModel, state = state)
        }
    }
}

@Composable
private fun TypeBar(
    prompt: String,
    onPromptChange: (String) -> Unit,
    onPickImage: () -> Unit,
    onSend: () -> Unit
) {
    val context = LocalContext.current
    Surface(color = AgnesColors.Background) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = AgnesColors.AssistantBubble
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPickImage) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = context.resString(R.string.button_select_image),
                            tint = AgnesColors.TextSecondary
                        )
                    }
                    BasicTextField(
                        value = prompt,
                        onValueChange = onPromptChange,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 24.dp)
                            .padding(vertical = 8.dp),
                        textStyle = TextStyle(color = AgnesColors.TextPrimary, fontSize = 16.sp),
                        singleLine = false,
                        maxLines = 4,
                        cursorBrush = SolidColor(AgnesColors.Accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSend() }),
                        decorationBox = { innerTextField ->
                            Box {
                                if (prompt.isEmpty()) {
                                    Text(
                                        text = context.resString(R.string.type_placeholder),
                                        color = AgnesColors.TextSecondary,
                                        fontSize = 16.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    if (prompt.isNotBlank()) {
                        IconButton(onClick = onSend) {
                            Icon(
                                painter = painterResource(R.drawable.ic_send),
                                contentDescription = context.resString(R.string.button_generate),
                                tint = AgnesColors.Accent
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun android.content.Context.resString(id: Int, vararg args: Any): String =
    getString(id, *args)
