package com.rubixscript.flipscout.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubixscript.flipscout.R

@Composable
fun EmptyState(filtered: Boolean, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (filtered) "🔎" else "🛒", fontSize = 56.sp)
        Text(
            stringResource(if (filtered) R.string.empty_filtered_title else R.string.empty_title),
            style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            stringResource(if (filtered) R.string.empty_filtered_hint else R.string.empty_hint),
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, start = 32.dp, end = 32.dp),
        )
    }
}
