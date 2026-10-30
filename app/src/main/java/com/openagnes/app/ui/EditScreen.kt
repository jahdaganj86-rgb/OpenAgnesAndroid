package com.openagnes.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.openagnes.app.R
import com.openagnes.app.model.Skill
import com.openagnes.app.viewmodel.EditUiState
import com.openagnes.app.viewmodel.EditViewModel
import kotlinx.coroutines.launch

@Composable
fun EditScreen(viewModel: EditViewModel, state: EditUiState) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val pickLauncher = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onImagePicked(uri)
    }
    LaunchedEffect(Unit) {
        viewModel.initApiKeyStore()
    }

    val keySaved = viewModel.getStoredApiKey() != null

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.height(180.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                state.previewBitmap?.let {
                    Image(
                        it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } ?: Text(
                    text = "Kein Bild gewählt",
                    color = Color(0xFF757575),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { pickLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }) {
                Text(text = context.resString(R.string.button_select_image))
            }
            if (state.imageUri != null) {
                Text(
                    text = context.resString(R.string.selected_image),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB0B0B0)
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
        ) {
            TextField(
                value = state.prompt,
                onValueChange = { viewModel.onPromptChange(it) },
                placeholder = { Text(text = context.resString(R.string.prompt_placeholder)) },
                label = { Text(text = "Prompt") },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                singleLine = false,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )
        }

        var editing by remember { mutableStateOf(false) }
        Button(
            onClick = {
                scope.launch {
                    editing = true
                    viewModel.enhance()
                    viewModel.generate()
                    editing = false
                }
            },
            enabled = state.imageDataUri != null && state.prompt.isNotBlank() && keySaved,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (editing) "Bearbeite…" else context.resString(R.string.button_generate))
        }

        state.message?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF03DAC6))
        }

        if (state.skills.isNotEmpty()) {
            Text(
                text = context.resString(R.string.skills_header),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFB0B0B0)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.skills.forEach { skill ->
                    SkillCard(name = skill.name, chars = skill.chars)
                }
            }
        }

        if (state.originalPrompt != null || state.enhancedPrompt != null) {
            Text(
                text = context.resString(R.string.section_enhancement),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFB0B0B0)
            )
            ExpandablePromptCard(
                title = context.resString(R.string.card_original_prompt),
                content = state.originalPrompt ?: ""
            )
            ExpandablePromptCard(
                title = context.resString(R.string.card_enhanced_prompt),
                content = state.enhancedPrompt ?: ""
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = context.resString(R.string.footer_type),
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFFBB86FC)
        )
    }
}

@Composable
private fun ExpandablePromptCard(title: String, content: String) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = Color(0xFFBB86FC))
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = content, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFFFFFFF))
            }
        }
    }
}

@Composable
fun RowScope.SkillCard(name: String, chars: Int) {
    Card(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = name, style = MaterialTheme.typography.labelLarge, color = Color(0xFFBB86FC))
                Text(
                    text = LocalContext.current.resString(R.string.skill_loaded, chars),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFFFFFF)
                )
            }
        }
    }
}
