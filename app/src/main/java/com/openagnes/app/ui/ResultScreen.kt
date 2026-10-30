package com.openagnes.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.openagnes.app.R
import com.openagnes.app.viewmodel.EditUiState
import com.openagnes.app.viewmodel.EditViewModel

@Composable
fun ResultScreen(
    viewModel: EditViewModel,
    state: EditUiState,
    onEditImage: () -> Unit,
    onDownload: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().height(340.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                state.resultBitmap?.let {
                    Image(
                        it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                IconButton(
                    onClick = onDownload,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.ic_download),
                        contentDescription = context.resString(R.string.action_download)
                    )
                }
            }
        }

        Text(
            text = state.message ?: context.resString(R.string.result_done),
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF03DAC6)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val actions = listOf(
                R.drawable.ic_like to context.resString(R.string.action_like),
                R.drawable.ic_dislike to context.resString(R.string.action_dislike),
                R.drawable.ic_bookmark to context.resString(R.string.action_bookmark),
                R.drawable.ic_share to context.resString(R.string.action_share),
                R.drawable.ic_rotate to context.resString(R.string.action_rotate)
            )
            actions.forEach { (iconRes, label) ->
                IconButton(onClick = { }) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(iconRes),
                        contentDescription = label
                    )
                }
            }
        }

        Button(
            onClick = {
                viewModel.useResultAsSource()
                onEditImage()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = context.resString(R.string.button_edit_image))
        }

        Text(
            text = context.resString(R.string.footer_type),
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFBB86FC)
        )
    }
}
