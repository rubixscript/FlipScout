package com.rubixscript.flipscout.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubixscript.flipscout.R
import com.rubixscript.flipscout.ui.editor.AiAssistant
import com.rubixscript.flipscout.ui.editor.AssistantButton
import com.rubixscript.flipscout.ui.theme.Palette

/** Opened from a button so it takes no room on the main screen. Copies the prompt and jumps to the chosen AI. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeywordIdeasSheet(onAskAi: (topic: String, AiAssistant) -> Unit, onDismiss: () -> Unit) {
    var topic by rememberSaveable { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.keywords_title), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Text(stringResource(R.string.keywords_steps), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            OutlinedTextField(topic, { topic = it }, label = { Text(stringResource(R.string.keywords_topic_hint)) },
                singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AssistantButton(R.string.ai_chatgpt, Palette.ChatGpt, Modifier.weight(1f)) { onAskAi(topic, AiAssistant.CHATGPT) }
                AssistantButton(R.string.ai_gemini, Palette.Gemini, Modifier.weight(1f)) { onAskAi(topic, AiAssistant.GEMINI) }
            }
        }
    }
}
