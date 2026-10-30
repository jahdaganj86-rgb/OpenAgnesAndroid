package com.openagnes.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openagnes.app.R
import com.openagnes.app.viewmodel.EditUiState
import com.openagnes.app.viewmodel.EditViewModel
import com.openagnes.app.viewmodel.GenerationMode

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
            onSend = viewModel::submit,
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
    val context = LocalContext.current

    val pickLauncher = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onImagePicked(uri)
    }

    Scaffold(
        containerColor = AgnesColors.Background,
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = context.resString(R.string.app_title),
                            color = AgnesColors.TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.requestApiKeyDialog() }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_menu),
                                contentDescription = context.resString(R.string.menu_api_key),
                                tint = AgnesColors.TextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AgnesColors.Background)
                )
                ModeSwitch(
                    mode = state.mode,
                    enabled = !state.loading,
                    onModeChange = viewModel::onModeChange
                )
            }
        },
        bottomBar = {
            TypeBar(
                prompt = state.prompt,
                placeholder = context.resString(
                    when (state.mode) {
                        GenerationMode.CREATE -> R.string.create_type_placeholder
                        GenerationMode.VIDEO -> R.string.video_type_placeholder
                        GenerationMode.IMAGE -> R.string.type_placeholder
                    }
                ),
                onPromptChange = { viewModel.onPromptChange(it) },
                onPickImage = { pickLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
                onSend = viewModel::submit
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
    placeholder: String,
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
                                        text = placeholder,
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

@Composable
private fun ModeSwitch(
    mode: GenerationMode,
    enabled: Boolean,
    onModeChange: (GenerationMode) -> Unit
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = CircleShape,
        color = AgnesColors.AssistantBubble
    ) {
        Row(
            modifier = Modifier
                .padding(4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ModeSegment(
                modifier = Modifier,
                selected = mode == GenerationMode.CREATE,
                enabled = enabled,
                iconRes = R.drawable.ic_sparkle,
                label = context.resString(R.string.mode_create_image),
                onClick = { onModeChange(GenerationMode.CREATE) }
            )
            ModeSegment(
                modifier = Modifier,
                selected = mode == GenerationMode.IMAGE,
                enabled = enabled,
                iconRes = R.drawable.ic_edit,
                label = context.resString(R.string.mode_edit_image),
                onClick = { onModeChange(GenerationMode.IMAGE) }
            )
            ModeSegment(
                modifier = Modifier,
                selected = mode == GenerationMode.VIDEO,
                enabled = enabled,
                iconRes = R.drawable.ic_video,
                label = context.resString(R.string.mode_photo_to_video),
                onClick = { onModeChange(GenerationMode.VIDEO) }
            )
        }
    }
}

@Composable
private fun ModeSegment(
    modifier: Modifier,
    selected: Boolean,
    enabled: Boolean,
    iconRes: Int,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(36.dp)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.Tab,
                onClick = onClick
            ),
        shape = CircleShape,
        color = if (selected) AgnesColors.Chip else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (selected) AgnesColors.TextPrimary else AgnesColors.TextSecondary
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                color = if (selected) AgnesColors.TextPrimary else AgnesColors.TextSecondary,
                fontSize = 14.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

internal fun android.content.Context.resString(id: Int, vararg args: Any): String =
    getString(id, *args)
