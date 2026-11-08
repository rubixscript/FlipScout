package com.rubixscript.flipscout.ui.editor

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubixscript.flipscout.R
import com.rubixscript.flipscout.domain.Platform
import com.rubixscript.flipscout.ui.components.Pill
import com.rubixscript.flipscout.ui.theme.Palette

enum class AiAssistant(val url: String) {
    CHATGPT("https://chatgpt.com/"),
    GEMINI("https://gemini.google.com/app"),
}

/** Lets the user pick a marketplace and hand a ready-made prompt to an AI assistant. */
@Composable
fun ListingAssistantCard(onLaunch: (Platform, AiAssistant) -> Unit, modifier: Modifier = Modifier) {
    var platform by rememberSaveable { mutableStateOf(Platform.EBAY) }
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.assistant_title), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            Text(stringResource(R.string.assistant_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Platform.entries.forEach { p ->
                    Pill(p.displayName, platform == p, MaterialTheme.colorScheme.primary, onClick = { platform = p })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AssistantButton(R.string.ai_chatgpt, Palette.ChatGpt, Modifier.weight(1f)) { onLaunch(platform, AiAssistant.CHATGPT) }
                AssistantButton(R.string.ai_gemini, Palette.Gemini, Modifier.weight(1f)) { onLaunch(platform, AiAssistant.GEMINI) }
            }
        }
    }
}

@Composable
fun AssistantButton(@StringRes label: Int, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick, modifier.height(48.dp), shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
    ) { Text(stringResource(label), fontWeight = FontWeight.Bold) }
}
