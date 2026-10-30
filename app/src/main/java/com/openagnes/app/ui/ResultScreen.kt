package com.openagnes.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openagnes.app.R
import com.openagnes.app.viewmodel.EditUiState

@Composable
fun ResultScreen(
    state: EditUiState,
    onDownload: () -> Unit,
    onEditImage: () -> Unit,
    doneText: String,
    actionLabel: String
) {
    val context = LocalContext.current

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
                text = doneText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                color = AgnesColors.TextPrimary,
                fontSize = 15.sp
            )
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            state.resultBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(bitmap.width.toFloat() / bitmap.height.coerceAtLeast(1))
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            IconButton(
                onClick = onDownload,
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

        ResultActionRow()

        Surface(
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier
                .border(1.dp, AgnesColors.Divider, CircleShape)
                .clickable { onEditImage() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = AgnesColors.TextPrimary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = actionLabel,
                    color = AgnesColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
internal fun ResultActionRow() {
    val context = LocalContext.current
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        ActionIcon(R.drawable.ic_like, context.resString(R.string.action_like))
        ActionIcon(R.drawable.ic_dislike, context.resString(R.string.action_dislike))
        ActionIcon(R.drawable.ic_bookmark, context.resString(R.string.action_bookmark))
        ActionIcon(R.drawable.ic_copy, context.resString(R.string.action_copy))
        ActionIcon(R.drawable.ic_external, context.resString(R.string.action_share))
        ActionIcon(R.drawable.ic_rotate, context.resString(R.string.action_rotate))
    }
}

@Composable
internal fun ActionIcon(iconRes: Int, label: String) {
    Icon(
        painter = painterResource(iconRes),
        contentDescription = label,
        modifier = Modifier.size(22.dp),
        tint = AgnesColors.TextSecondary
    )
}
