package com.rubixscript.flipscout.ui.editor

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import com.rubixscript.flipscout.ui.components.PhotoImage
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubixscript.flipscout.R
import com.rubixscript.flipscout.data.Item
import com.rubixscript.flipscout.data.Seller
import com.rubixscript.flipscout.data.Status
import com.rubixscript.flipscout.domain.SearchTarget
import com.rubixscript.flipscout.domain.listingPrompt
import com.rubixscript.flipscout.domain.searchUrl
import com.rubixscript.flipscout.ui.components.GradientButton
import com.rubixscript.flipscout.ui.components.MoneyField
import com.rubixscript.flipscout.ui.components.Pill
import com.rubixscript.flipscout.ui.components.SoftTextField
import com.rubixscript.flipscout.ui.theme.Palette
import com.rubixscript.flipscout.ui.theme.color
import com.rubixscript.flipscout.ui.theme.labelRes
import com.rubixscript.flipscout.util.Photos
import com.rubixscript.flipscout.util.copyToClipboard
import com.rubixscript.flipscout.util.formatDate
import com.rubixscript.flipscout.util.money
import com.rubixscript.flipscout.util.openUrl
import com.rubixscript.flipscout.util.toAmount
import com.rubixscript.flipscout.util.toFieldText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** Platforms offered as one-tap choices; anything already on an item stays selectable too. */
private val KNOWN_PLATFORMS = listOf("eBay", "Facebook Marketplace", "Etsy", "Mercari", "Poshmark", "Depop")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditor(
    item: Item,
    isNew: Boolean,
    categories: List<String>,
    sellers: List<Seller>,
    onDismiss: () -> Unit,
    onSave: (Item) -> Unit,
    onDelete: () -> Unit,
) {
    val ctx = LocalContext.current
    var title by rememberSaveable(item.id) { mutableStateOf(item.title) }
    var buy by rememberSaveable(item.id) { mutableStateOf(item.buyPrice.toFieldText()) }
    var sell by rememberSaveable(item.id) { mutableStateOf(item.sellPrice.toFieldText()) }
    var fees by rememberSaveable(item.id) { mutableStateOf(item.fees.toFieldText()) }
    var link by rememberSaveable(item.id) { mutableStateOf(item.link) }
    var notes by rememberSaveable(item.id) { mutableStateOf(item.notes) }
    var status by rememberSaveable(item.id) { mutableStateOf(item.status) }
    var shipping by rememberSaveable(item.id) { mutableStateOf(item.shipping.toFieldText()) }
    var quantity by rememberSaveable(item.id) { mutableStateOf(if (item.quantity > 1) item.quantity.toString() else "") }
    var category by rememberSaveable(item.id) { mutableStateOf(item.category) }
    var platform by rememberSaveable(item.id) { mutableStateOf(item.platform) }
    var tagsText by rememberSaveable(item.id) { mutableStateOf(item.tags.joinToString(", ")) }
    var sellerId by rememberSaveable(item.id) { mutableStateOf(item.sellerId) }
    var soldAt by rememberSaveable(item.id) { mutableStateOf(item.soldAt) }
    var photo by rememberSaveable(item.id) { mutableStateOf(item.photo) }
    val scope = rememberCoroutineScope()
    // The system photo picker needs no permission; the pick is copied into app storage straight away.
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val saved = withContext(Dispatchers.IO) { Photos.import(ctx, uri, item.id) }
            if (saved != null) photo = saved else Toast.makeText(ctx, R.string.photo_failed, Toast.LENGTH_SHORT).show()
        }
    }
    // Expanded straight away when the item already carries details; collapsed for a quick new flip.
    var detailsOpen by rememberSaveable(item.id) {
        mutableStateOf(!isNew && (item.category.isNotBlank() || item.platform.isNotBlank() || item.tags.isNotEmpty() ||
            item.sellerId.isNotBlank() || item.shipping > 0 || item.quantity > 1 || item.soldAt > 0))
    }
    val draft = item.copy(
        title = title, buyPrice = buy.toAmount(), sellPrice = sell.toAmount(), fees = fees.toAmount(),
        shipping = shipping.toAmount(), quantity = quantity.toIntOrNull()?.coerceAtLeast(1) ?: 1,
        status = status, category = category.trim(), platform = platform, link = link.trim(), notes = notes,
        tags = tagsText.split(',').map { it.trim() }.filter { it.isNotBlank() }.distinct(),
        sellerId = sellerId, soldAt = if (status == Status.SOLD) soldAt else 0L, photo = photo,
    )

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
            Text(stringResource(if (isNew) R.string.editor_new else R.string.editor_edit),
                style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                PhotoTile(
                    photo = photo,
                    onPick = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onRemove = { photo = "" },
                )
                SoftTextField(title, { title = it }, stringResource(R.string.field_title),
                    singleLine = true, modifier = Modifier.weight(1f))
            }

            if (title.isNotBlank()) {
                TextButton(onClick = { ctx.openUrl(searchUrl(SearchTarget.EBAY_SOLD, title)) }) {
                    Text(stringResource(R.string.ebay_sold_prices), fontWeight = FontWeight.Bold)
                }
            }

            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Status.entries.forEach { s -> Pill(stringResource(s.labelRes), status == s, s.color(), onClick = { status = s }) }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MoneyField(buy, { buy = it }, stringResource(R.string.field_buy), Modifier.weight(1f))
                MoneyField(sell, { sell = it },
                    stringResource(if (status == Status.SOLD) R.string.field_sold else R.string.field_target), Modifier.weight(1f))
                MoneyField(fees, { fees = it }, stringResource(R.string.field_fees), Modifier.weight(1f))
            }

            if (draft.sellPrice > 0) ProfitSummary(draft)

            SoftTextField(link, { link = it }, stringResource(R.string.field_link),
                singleLine = true, modifier = Modifier.fillMaxWidth())
            if (link.isNotBlank()) {
                TextButton(onClick = { ctx.openUrl(link.trim()) }) {
                    Text(stringResource(R.string.open_link), fontWeight = FontWeight.Bold)
                }
            }
            SoftTextField(notes, { notes = it }, stringResource(R.string.field_notes),
                modifier = Modifier.fillMaxWidth())

            DetailsSection(
                open = detailsOpen,
                onToggle = { detailsOpen = !detailsOpen },
                draft = draft,
                categories = categories,
                sellers = sellers,
                shipping = shipping, onShipping = { shipping = it },
                quantity = quantity, onQuantity = { quantity = it.filter { c -> c.isDigit() } },
                category = category, onCategory = { category = it },
                platform = platform, onPlatform = { platform = if (platform == it) "" else it },
                tagsText = tagsText, onTags = { tagsText = it },
                sellerId = sellerId, onSeller = { sellerId = it },
                soldAt = draft.soldAt, onSoldAt = { soldAt = it },
            )

            ListingAssistantCard(onLaunch = { assistantPlatform, assistant ->
                ctx.copyToClipboard(ctx.getString(R.string.prompt_clip_label), listingPrompt(draft, assistantPlatform))
                Toast.makeText(ctx, R.string.prompt_copied, Toast.LENGTH_LONG).show()
                ctx.openUrl(assistant.url)
            })

            // One loud save, quiet secondaries — the sheet's single primary action.
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

/** Square photo slot beside the title: one tap picks, a small x clears. Empty slot shows a plus. */
@Composable
private fun PhotoTile(photo: String, onPick: () -> Unit, onRemove: () -> Unit) {
    Box(Modifier.size(64.dp)) {
        Box(
            Modifier.matchParentSize().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onPick),
            contentAlignment = Alignment.Center,
        ) {
            if (photo.isBlank()) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.photo_add),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                PhotoImage(photo, Modifier.matchParentSize(), targetPx = 320)
            }
        }
        if (photo.isNotBlank()) {
            Box(
                Modifier.align(Alignment.TopEnd).size(22.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onBackground).clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.photo_remove),
                    tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(14.dp))
            }
        }
    }
}

/** Optional fields kept behind one toggle so a quick flip stays a two-line form. */
@Composable
private fun DetailsSection(
    open: Boolean,
    onToggle: () -> Unit,
    draft: Item,
    categories: List<String>,
    sellers: List<Seller>,
    shipping: String, onShipping: (String) -> Unit,
    quantity: String, onQuantity: (String) -> Unit,
    category: String, onCategory: (String) -> Unit,
    platform: String, onPlatform: (String) -> Unit,
    tagsText: String, onTags: (String) -> Unit,
    sellerId: String, onSeller: (String) -> Unit,
    soldAt: Long, onSoldAt: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(if (open) R.string.details_hide else R.string.details_show) + "  " + if (open) "▴" else "▾",
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onToggle).padding(vertical = 4.dp),
        )
        AnimatedVisibility(open) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MoneyField(shipping, onShipping, stringResource(R.string.field_shipping), Modifier.weight(1f))
                    SoftTextField(
                        quantity, onQuantity, stringResource(R.string.field_quantity),
                        singleLine = true, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f),
                    )
                }
                SoftTextField(
                    category, onCategory, stringResource(R.string.field_category), singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (categories.isNotEmpty()) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.take(8).forEach { c ->
                            Pill(c, c == category, MaterialTheme.colorScheme.primary, onClick = { onCategory(c) })
                        }
                    }
                }
                if (draft.status == Status.SOLD) SoldDateRow(soldAt, onSoldAt)
                Text(stringResource(R.string.field_platform), color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (listOf(platform).filter { it.isNotBlank() } + KNOWN_PLATFORMS).distinct().forEach { p ->
                        Pill(p, p == platform, Palette.Blue, onClick = { onPlatform(p) })
                    }
                }
                SoftTextField(
                    tagsText, onTags, stringResource(R.string.field_tags), singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (sellers.isNotEmpty()) {
                    Text(stringResource(R.string.field_seller), color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill(stringResource(R.string.no_seller), sellerId.isBlank(), Palette.Amber, onClick = { onSeller("") })
                        sellers.forEach { s ->
                            Pill(
                                s.name.ifBlank { stringResource(R.string.seller_fallback_name) },
                                s.id == sellerId, Palette.Amber, onClick = { onSeller(s.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SoldDateRow(soldAt: Long, onSoldAt: (Long) -> Unit) {
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(stringResource(R.string.field_sold_date), color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(formatDate(if (soldAt > 0) soldAt else System.currentTimeMillis()),
                fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = { pickerOpen = true }) { Text(stringResource(R.string.change_date)) }
    }
    if (pickerOpen) {
        // The picker speaks UTC; convert at the edges so the local date never shifts a day.
        val state = rememberDatePickerState(
            initialSelectedDateMillis = localToPickerMillis(if (soldAt > 0) soldAt else System.currentTimeMillis()),
        )
        DatePickerDialog(
            onDismissRequest = { pickerOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onSoldAt(pickerToLocalMillis(it)) }
                    pickerOpen = false
                }) { Text(stringResource(R.string.set_date)) }
            },
            dismissButton = { TextButton(onClick = { pickerOpen = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state = state) }
    }
}

private fun localToPickerMillis(millis: Long): Long =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun pickerToLocalMillis(millis: Long): Long =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

/** Dark graphite feature card: projected/actual profit in white, with ROI on the right. */
@Composable
private fun ProfitSummary(draft: Item) {
    val good = draft.profit >= 0
    val c = if (good) Palette.Emerald else Palette.Loss
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Palette.GraphiteBrush).padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(if (draft.status == Status.SOLD) R.string.profit else R.string.projected_profit),
                color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.Medium,
            )
            Text((if (good) "+" else "") + money(draft.profit), color = Color.White, fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp)
        }
        Text(stringResource(R.string.roi_format, "%.0f".format(draft.roi)), color = c, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}
