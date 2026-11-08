package com.rubixscript.flipscout.ui.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rubixscript.flipscout.R
import com.rubixscript.flipscout.ui.components.Pill

/** Pick an existing category or type a new one. Saving a blank name removes the keyword from its category. */
@Composable
fun CategoryDialog(
    current: String,
    suggestions: List<String>,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    titleRes: Int = R.string.category_title,
    allowRemove: Boolean = true,
) {
    var name by rememberSaveable { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.category_hint)) },
                    singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
                if (suggestions.isNotEmpty()) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestions.forEach { s ->
                            Pill(s, name.trim().equals(s, ignoreCase = true), MaterialTheme.colorScheme.primary, onClick = { name = s })
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name) }) { Text(stringResource(R.string.save)) } },
        dismissButton = {
            if (allowRemove && current.isNotBlank()) TextButton(onClick = { onSave("") }) { Text(stringResource(R.string.category_remove)) }
            else TextButton(onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
