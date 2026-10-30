package com.openagnes.app.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.openagnes.app.R

@Composable
fun ApiKeyDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    if (!visible) return
    var key by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = context.getString(R.string.dialog_title)) },
        text = {
            TextField(
                value = key,
                onValueChange = { key = it },
                placeholder = { Text(text = context.getString(R.string.dialog_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(key); onDismiss() }) {
                Text(text = context.getString(R.string.dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = context.getString(R.string.dialog_cancel))
            }
        }
    )
}
