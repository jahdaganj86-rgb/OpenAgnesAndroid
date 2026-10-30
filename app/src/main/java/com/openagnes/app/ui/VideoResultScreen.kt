package com.openagnes.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openagnes.app.R
import com.openagnes.app.viewmodel.EditUiState

@Composable
fun VideoResultScreen(
    state: EditUiState,
    onDownload: () -> Unit,
    onAnimateAgain: () -> Unit
) {
    val context = LocalContext.current
    val url = state.videoUrl ?: return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = AgnesColors.AssistantBubble
        ) {
            Text(
                text = context.resString(R.string.result_done_video),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                color = AgnesColors.TextPrimary,
                fontSize = 15.sp
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(parseVideoRatio(state.videoRatio))
                .heightIn(max = 480.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
        ) {
            VideoPlayer(url = url, modifier = Modifier.fillMaxSize())
            IconButton(
                onClick = { if (!state.videoSaving) onDownload() },
                enabled = !state.videoSaving,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .size(36.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0x99000000),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (state.videoSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = AgnesColors.TextPrimary
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_download),
                                contentDescription = context.resString(R.string.action_download),
                                modifier = Modifier.size(18.dp),
                                tint = AgnesColors.TextPrimary
                            )
                        }
                    }
                }
            }
        }

        ResultActionRow()

        Surface(
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier
                .border(1.dp, AgnesColors.Divider, CircleShape)
                .clickable { onAnimateAgain() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_video),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = AgnesColors.TextPrimary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = context.resString(R.string.button_animate_again),
                    color = AgnesColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun parseVideoRatio(ratio: String): Float {
    return when (ratio) {
        "21:9" -> 21f / 9f
        "16:9" -> 16f / 9f
        "4:3" -> 4f / 3f
        "3:4" -> 3f / 4f
        "9:16" -> 9f / 16f
        else -> 1f
    }
}