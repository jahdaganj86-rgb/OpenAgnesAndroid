package com.openagnes.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openagnes.app.R
import com.openagnes.app.model.Skill
import com.openagnes.app.viewmodel.EditUiState
import com.openagnes.app.viewmodel.EditViewModel

@Composable
fun EditScreen(viewModel: EditViewModel, state: EditUiState) {
    val context = LocalContext.current

    if (state.submitted || state.originalPrompt != null) {
        Bubble(
            text = state.originalPrompt ?: state.prompt,
            color = AgnesColors.UserBubble
        )
    }

    state.previewBitmap?.let { bitmap ->
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .width(120.dp)
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }

    if (state.enhancedPrompt != null) {
        Bubble(text = context.resString(R.string.assistant_ack), color = AgnesColors.AssistantBubble)
    }

    if (state.skills.isNotEmpty()) {
        ThoughtsCard(skills = state.skills, seconds = state.thoughtSeconds)
    }

    if (state.originalPrompt != null || state.enhancedPrompt != null) {
        PromptEnhancementCard(
            original = state.originalPrompt,
            enhanced = state.enhancedPrompt
        )
    }

    if (state.loading) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = AgnesColors.Accent
            )
            Text(text = "Agnes denkt nach…", color = AgnesColors.TextSecondary, fontSize = 14.sp)
        }
    }

    if (state.resultBitmap != null) {
        ResultScreen(
            state = state,
            onDownload = { viewModel.saveResult() },
            onEditImage = { viewModel.useResultAsSource() }
        )
    }
}

@Composable
private fun Bubble(text: String, color: androidx.compose.ui.graphics.Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = color
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            color = AgnesColors.TextPrimary,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun ThoughtsCard(skills: List<Skill>, seconds: Int) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(true) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = AgnesColors.Card
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_thoughts),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = AgnesColors.TextSecondary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = context.resString(R.string.thoughts_title),
                    color = AgnesColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = context.resString(R.string.thoughts_takes, seconds),
                    color = AgnesColors.TextSecondary,
                    fontSize = 14.sp
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_down),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp).rotate(if (expanded) 180f else 0f),
                    tint = AgnesColors.TextSecondary
                )
            }
            if (expanded) {
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    skills.forEach { SkillRow(it) }
                }
            }
        }
    }
}

@Composable
private fun SkillRow(skill: Skill) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = AgnesColors.Inset
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_skill),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = AgnesColors.TextSecondary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = context.resString(R.string.load_skill),
                color = AgnesColors.TextPrimary,
                fontSize = 14.sp
            )
            Spacer(Modifier.width(8.dp))
            SkillChip(skill.name)
            Spacer(Modifier.weight(1f))
            Text(
                text = context.resString(R.string.skill_loaded, skill.chars),
                color = AgnesColors.TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SkillChip(name: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = AgnesColors.Chip
    ) {
        Text(
            text = name,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = AgnesColors.TextPrimary,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun PromptEnhancementCard(original: String?, enhanced: String?) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = AgnesColors.Card
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_sparkle),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = AgnesColors.Accent
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = context.resString(R.string.section_enhancement),
                        color = AgnesColors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = context.resString(R.string.section_enhancement_subtitle),
                        color = AgnesColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
            original?.let {
                Spacer(Modifier.height(12.dp))
                PromptDisclosure(title = context.resString(R.string.card_original_prompt), content = it)
            }
            enhanced?.let {
                Spacer(Modifier.height(10.dp))
                PromptDisclosure(title = context.resString(R.string.card_enhanced_prompt), content = it)
            }
        }
    }
}

@Composable
private fun PromptDisclosure(title: String, content: String) {
    var expanded by remember { mutableStateOf(true) }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_skill),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = AgnesColors.TextSecondary
            )
            Spacer(Modifier.width(8.dp))
            Text(text = title, color = AgnesColors.TextPrimary, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            Icon(
                painter = painterResource(R.drawable.ic_chevron_down),
                contentDescription = null,
                modifier = Modifier.size(18.dp).rotate(if (expanded) 180f else 0f),
                tint = AgnesColors.TextSecondary
            )
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = AgnesColors.Inset
            ) {
                Text(
                    text = content,
                    modifier = Modifier.padding(12.dp),
                    color = AgnesColors.TextPrimary,
                    fontSize = 14.sp
                )
            }
        }
    }
}
