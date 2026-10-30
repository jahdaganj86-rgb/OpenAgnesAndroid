package com.openagnes.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openagnes.app.R
import com.openagnes.app.viewmodel.EditUiState
import com.openagnes.app.viewmodel.GenerationMode

@Composable
fun EditImageScreen(
    state: EditUiState,
    onPromptChange: (String) -> Unit,
    onQuickPrompt: (String) -> Unit,
    onSend: () -> Unit,
    onClose: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val mode = state.mode
    val placeholder = stringResource(
        if (mode == GenerationMode.VIDEO) R.string.video_editor_placeholder
        else R.string.editor_prompt_placeholder
    )

    val upscaleLabel = stringResource(R.string.quick_action_upscale)
    val removeBgLabel = stringResource(R.string.quick_action_remove_bg)
    val textEditLabel = stringResource(R.string.quick_action_text_edit)
    val upscalePrompt = stringResource(R.string.quick_prompt_upscale)
    val removeBgPrompt = stringResource(R.string.quick_prompt_remove_bg)
    val textEditPrompt = stringResource(R.string.quick_prompt_text_edit)

    val animateLabel = stringResource(R.string.video_quick_action_animate)
    val cinematicLabel = stringResource(R.string.video_quick_action_cinematic)
    val slowmoLabel = stringResource(R.string.video_quick_action_slowmo)
    val animatePrompt = stringResource(R.string.video_quick_prompt_animate)
    val cinematicPrompt = stringResource(R.string.video_quick_prompt_cinematic)
    val slowmoPrompt = stringResource(R.string.video_quick_prompt_slowmo)
    val outputHint = stringResource(R.string.video_output_hint, state.videoRatio)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AgnesColors.Background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.action_close),
                    modifier = Modifier.size(26.dp),
                    tint = AgnesColors.TextPrimary
                )
            }
            Text(
                text = stringResource(
                    if (mode == GenerationMode.VIDEO) R.string.video_editor_title
                    else R.string.editor_title
                ),
                modifier = Modifier.align(Alignment.Center),
                color = AgnesColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            state.previewBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = AgnesColors.EditorPanel
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (mode == GenerationMode.VIDEO) {
                            QuickChip(
                                iconRes = R.drawable.ic_video,
                                label = animateLabel,
                                onClick = {
                                    onQuickPrompt(animatePrompt)
                                    focusRequester.requestFocus()
                                }
                            )
                            QuickChip(
                                iconRes = R.drawable.ic_sparkle,
                                label = cinematicLabel,
                                onClick = {
                                    onQuickPrompt(cinematicPrompt)
                                    focusRequester.requestFocus()
                                }
                            )
                            QuickChip(
                                iconRes = R.drawable.ic_rotate,
                                label = slowmoLabel,
                                onClick = {
                                    onQuickPrompt(slowmoPrompt)
                                    focusRequester.requestFocus()
                                }
                            )
                            VideoOutputChip(text = outputHint)
                        } else {
                            QuickChip(
                                iconRes = R.drawable.ic_sparkle,
                                label = upscaleLabel,
                                onClick = {
                                    onQuickPrompt(upscalePrompt)
                                    focusRequester.requestFocus()
                                }
                            )
                            QuickChip(
                                iconRes = R.drawable.ic_remove_bg,
                                label = removeBgLabel,
                                onClick = {
                                    onQuickPrompt(removeBgPrompt)
                                    focusRequester.requestFocus()
                                }
                            )
                            QuickChip(
                                iconRes = R.drawable.ic_text_edit,
                                label = textEditLabel,
                                onClick = {
                                    onQuickPrompt(textEditPrompt)
                                    focusRequester.requestFocus()
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    state.error?.let {
                        Text(
                            text = it,
                            color = AgnesColors.Error,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(4.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        BasicTextField(
                            value = state.prompt,
                            onValueChange = onPromptChange,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 24.dp)
                                .padding(vertical = 8.dp)
                                .focusRequester(focusRequester),
                            textStyle = TextStyle(color = AgnesColors.TextPrimary, fontSize = 16.sp),
                            maxLines = 4,
                            cursorBrush = SolidColor(AgnesColors.Accent),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { onSend() }),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (state.prompt.isEmpty()) {
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
                        if (state.prompt.isNotBlank()) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clickable { onSend() },
                                shape = CircleShape,
                                color = AgnesColors.EditorSendButton
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_arrow_up),
                                        contentDescription = stringResource(R.string.button_generate),
                                        modifier = Modifier.size(20.dp),
                                        tint = AgnesColors.TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoOutputChip(text: String) {
    Surface(
        modifier = Modifier.height(40.dp),
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, AgnesColors.Divider)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                color = AgnesColors.TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun QuickChip(
    iconRes: Int,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .height(40.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, AgnesColors.EditorChipBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = AgnesColors.TextPrimary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                color = AgnesColors.TextPrimary,
                fontSize = 15.sp
            )
        }
    }
}