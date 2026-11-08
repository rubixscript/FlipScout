package com.rubixscript.flipscout.ui.sellers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rubixscript.flipscout.R
import com.rubixscript.flipscout.data.Seller
import com.rubixscript.flipscout.ui.components.GradientButton
import com.rubixscript.flipscout.ui.components.Pill
import com.rubixscript.flipscout.ui.components.SoftTextField
import com.rubixscript.flipscout.ui.theme.Palette
import com.rubixscript.flipscout.util.openUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerEditor(seller: Seller, isNew: Boolean, onDismiss: () -> Unit, onSave: (Seller) -> Unit, onDelete: () -> Unit) {
    val ctx = LocalContext.current
    var name by rememberSaveable(seller.id) { mutableStateOf(seller.name) }
    var url by rememberSaveable(seller.id) { mutableStateOf(seller.url) }
    var notes by rememberSaveable(seller.id) { mutableStateOf(seller.notes) }
    var favorite by rememberSaveable(seller.id) { mutableStateOf(seller.favorite) }
    val draft = seller.copy(name = name.trim(), url = url.trim(), notes = notes, favorite = favorite)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).imePadding().navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(if (isNew) R.string.seller_editor_new else R.string.seller_editor_edit),
                style = MaterialTheme.typography.headlineSmall)
            SoftTextField(name, { name = it }, stringResource(R.string.field_seller_name),
                singleLine = true, modifier = Modifier.fillMaxWidth())
            SoftTextField(url, { url = it }, stringResource(R.string.field_seller_url),
                singleLine = true, modifier = Modifier.fillMaxWidth())
            if (url.isNotBlank()) {
                TextButton(onClick = { ctx.openUrl(url.trim()) }) {
                    Text(stringResource(R.string.open_link), fontWeight = FontWeight.Bold)
                }
            }
            SoftTextField(notes, { notes = it }, stringResource(R.string.field_seller_notes),
                modifier = Modifier.fillMaxWidth())
            Row {
                Pill(
                    stringResource(if (favorite) R.string.seller_favorite_on else R.string.seller_favorite_off),
                    favorite, Palette.Amber, onClick = { favorite = !favorite },
                )
            }

            GradientButton(stringResource(R.string.save), onClick = { onSave(draft) }, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onDismiss) { Text(stringResource(R.string.cancel), fontWeight = FontWeight.SemiBold) }
                Spacer(Modifier.weight(1f))
                if (!isNew) {
                    TextButton(onDelete) { Text(stringResource(R.string.delete), color = Palette.Loss, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}
