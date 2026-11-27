package com.pnotes.ui

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import com.pnotes.ui.theme.LocalPalette
import com.pnotes.ui.theme.toComposeColor

/**
 * The note-title dialog: tapping the title in the editor's top bar opens it with the current name
 * already selected, so typing replaces it and Return commits.
 *
 * This was previously defined alongside the unreachable toolbar composables, which the tool-slot
 * work deleted; it lives here now because the top bar is what actually uses it.
 */
@Composable
fun RenameDialog(
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    var text by remember { mutableStateOf(TextFieldValue(initial, selection = TextRange(0, initial.length))) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val confirm = {
        val name = text.text.trim()
        if (name.isNotEmpty()) onConfirm(name)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename note", color = palette.text.toComposeColor()) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = palette.text.toComposeColor(),
                    unfocusedTextColor = palette.text.toComposeColor(),
                    focusedBorderColor = palette.accent.toComposeColor(),
                    unfocusedBorderColor = palette.border.toComposeColor(),
                    cursorColor = palette.accent.toComposeColor(),
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { confirm() }),
                modifier = Modifier
                    .focusRequester(focus)
                    .onPreviewKeyEvent { ev ->
                        when {
                            ev.type != KeyEventType.KeyDown -> false
                            ev.key == Key.Enter || ev.key == Key.NumPadEnter -> { confirm(); true }
                            ev.key == Key.Escape -> { onDismiss(); true }
                            else -> false
                        }
                    },
            )
        },
        confirmButton = {
            TextButton(onClick = { confirm() }, enabled = text.text.isNotBlank()) { Text("Rename") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = palette.textDim.toComposeColor()) }
        },
        containerColor = palette.menuBg.toComposeColor(),
        titleContentColor = palette.text.toComposeColor(),
        textContentColor = palette.text.toComposeColor(),
    )
}
