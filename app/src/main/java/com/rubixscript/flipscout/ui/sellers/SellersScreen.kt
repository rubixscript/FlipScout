package com.rubixscript.flipscout.ui.sellers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rubixscript.flipscout.R
import com.rubixscript.flipscout.data.Item
import com.rubixscript.flipscout.data.Seller
import com.rubixscript.flipscout.domain.sellerStats
import com.rubixscript.flipscout.ui.components.AddPill
import com.rubixscript.flipscout.ui.components.CircleButton
import com.rubixscript.flipscout.ui.theme.Dimens
import com.rubixscript.flipscout.ui.theme.Palette
import com.rubixscript.flipscout.util.money
import kotlinx.coroutines.launch

@Composable
fun SellersScreen(viewModel: SellersViewModel, items: List<Item>, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SellersEvent.SellerDeleted -> launch {
                    val name = event.name.ifBlank { context.getString(R.string.seller_fallback_name) }
                    snackbar.currentSnackbarData?.dismiss()
                    val result = snackbar.showSnackbar(
                        message = context.getString(R.string.deleted_message, name),
                        actionLabel = context.getString(R.string.undo),
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.onUndoDelete()
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = Dimens.screen, end = Dimens.screen, top = 8.dp, bottom = Dimens.bottomBarSpace),
            verticalArrangement = Arrangement.spacedBy(Dimens.listGap),
        ) {
            item {
                Row(Modifier.padding(top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircleButton(onBack, size = 48.dp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.sellers_title), style = MaterialTheme.typography.headlineMedium)
                        Text(stringResource(R.string.sellers_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
                    }
                    AddPill(viewModel::onNewSeller)
                }
            }
            if (!state.isLoading && state.sellers.isEmpty()) item { SellersEmptyState() }
            items(state.visibleSellers, key = { it.id }) { seller ->
                SellerRow(
                    seller,
                    items = items,
                    onClick = { viewModel.onEditSeller(seller) },
                    onToggleFavorite = { viewModel.onToggleFavorite(seller) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }

    state.editor?.let { editor ->
        SellerEditor(
            seller = editor.seller,
            isNew = editor.isNew,
            onDismiss = viewModel::onEditorDismissed,
            onSave = viewModel::onSave,
            onDelete = { viewModel.onDelete(editor.seller) },
        )
    }
}

@Composable
private fun SellerRow(
    seller: Seller,
    items: List<Item>,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stats = remember(seller.id, items) { sellerStats(seller.id, items) }
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(Dimens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(Dimens.rowPadding), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.onBackground), contentAlignment = Alignment.Center) {
                Text(seller.name.trim().firstOrNull()?.uppercase() ?: "?", color = MaterialTheme.colorScheme.background,
                    fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(seller.name.ifBlank { stringResource(R.string.item_untitled) }, fontWeight = FontWeight.Bold,
                    fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val sub = listOf(seller.host, seller.notes.lineSequence().firstOrNull().orEmpty()).filter { it.isNotBlank() }
                if (sub.isNotEmpty()) {
                    Text(sub.joinToString("  ·  "), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (stats.itemsBought > 0) {
                    Row {
                        Text(
                            stringResource(R.string.seller_stats, stats.itemsBought, stats.soldCount),
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            (if (stats.profit >= 0) "+" else "") + money(stats.profit),
                            color = if (stats.profit >= 0) Palette.Emerald else Palette.Loss,
                            fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Text(
                if (seller.favorite) "★" else "☆",
                color = if (seller.favorite) Palette.Amber else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 24.sp,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onToggleFavorite).padding(8.dp),
            )
        }
    }
}

@Composable
private fun SellersEmptyState() {
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🏪", fontSize = 48.sp)
        Text(stringResource(R.string.sellers_empty_title), fontSize = 18.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp))
        Text(stringResource(R.string.sellers_empty_hint), color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, start = 24.dp, end = 24.dp))
    }
}
