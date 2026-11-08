package com.rubixscript.flipscout.ui

import android.content.pm.ApplicationInfo
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubixscript.flipscout.ui.theme.Dimens
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rubixscript.flipscout.R
import com.rubixscript.flipscout.data.Backup
import com.rubixscript.flipscout.domain.csvToItems
import com.rubixscript.flipscout.domain.itemsToCsv
import com.rubixscript.flipscout.util.shareFile
import java.io.File
import com.rubixscript.flipscout.ui.home.HomeScreen
import com.rubixscript.flipscout.ui.home.HomeViewModel
import com.rubixscript.flipscout.ui.insights.InsightsScreen
import com.rubixscript.flipscout.ui.search.SearchScreen
import com.rubixscript.flipscout.ui.search.SearchViewModel
import com.rubixscript.flipscout.ui.sellers.SellersScreen
import com.rubixscript.flipscout.ui.sellers.SellersViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Tab(@StringRes val label: Int) {
    ITEMS(R.string.tab_items),
    SEARCH(R.string.tab_search),
    INSIGHTS(R.string.tab_insights),
}

/** App shell: bottom navigation, the "what did you share?" chooser, and backup import. */
@Composable
fun FlipApp(
    homeViewModel: HomeViewModel,
    sellersViewModel: SellersViewModel,
    searchViewModel: SearchViewModel,
    sharedText: String?,
) {
    var tab by rememberSaveable { mutableStateOf(Tab.ITEMS) }
    // Sellers is a secondary screen opened from the Items header, not a tab.
    var showSellers by rememberSaveable { mutableStateOf(false) }
    BackHandler(showSellers) { showSellers = false }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    // Mock-data tools are for non-prod builds only; Reset all data is available everywhere.
    // Saved across rotation, so a share is offered once and stays dismissed afterwards.
    var pendingShare by rememberSaveable { mutableStateOf(sharedText) }
    val context = LocalContext.current
    val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    val scope = rememberCoroutineScope()
    val homeState by homeViewModel.state.collectAsStateWithLifecycle()
    val sellersState by sellersViewModel.state.collectAsStateWithLifecycle()

    // One tap on Import opens the picker; the file is read, parsed and merged back into both stores.
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { importBackup(context, uri, homeViewModel, sellersViewModel) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { if (!showSellers) FloatingTabBar(tab, onSelect = { tab = it }) },
    ) { pad ->
        val content = Modifier.padding(pad)
        AnimatedContent(
            targetState = showSellers,
            transitionSpec = {
                if (targetState) (slideInHorizontally { it / 4 } + fadeIn()) togetherWith (fadeOut(tween(120)))
                else fadeIn(tween(200)) togetherWith (slideOutHorizontally { it / 4 } + fadeOut())
            },
            label = "sellersOverlay",
        ) { sellersOpen ->
            if (sellersOpen) {
                SellersScreen(sellersViewModel, items = homeState.items, onBack = { showSellers = false }, modifier = content)
            } else when (tab) {
                Tab.ITEMS -> HomeScreen(
                    viewModel = homeViewModel,
                    sellers = sellersState.sellers,
                    onOpenSellers = { showSellers = true },
                    onOpenSettings = { showSettings = true },
                    onUndoReset = {
                        homeViewModel.onUndoReset(); sellersViewModel.onUndoReset(); searchViewModel.onUndoReset()
                    },
                    modifier = content,
                )
                Tab.INSIGHTS -> InsightsScreen(items = homeState.items, modifier = content)
                Tab.SEARCH -> SearchScreen(
                    searchViewModel,
                    interests = homeState.items.map { it.category.trim() }.filter { it.isNotEmpty() }.distinct(),
                    modifier = content,
                )
            }
        }
    }

    // Exports are built off the main thread, then handed to the share sheet in one tap.
    fun export(fileName: String, mime: String, content: () -> String) {
        showSettings = false
        scope.launch {
            val file = withContext(Dispatchers.IO) {
                val dir = File(context.cacheDir, "exports").apply { mkdirs() }
                File(dir, fileName).apply { writeText(content()) }
            }
            context.shareFile(file, mime)
        }
    }

    if (showSettings) {
        SettingsSheet(
            showDeveloperTools = isDebuggable,
            onDismiss = { showSettings = false },
            onExportCsv = { export("flipscout-items.csv", "text/csv") { itemsToCsv(homeState.items) } },
            onExportBackup = { export("flipscout-backup.json", "application/json") { Backup.toJson(homeState.items, sellersState.sellers) } },
            onImport = {
                showSettings = false
                importLauncher.launch(arrayOf("application/json", "text/csv", "text/plain", "application/octet-stream"))
            },
            onReset = {
                homeViewModel.onResetAll(); sellersViewModel.onResetAll(); searchViewModel.onResetAll()
                showSettings = false
            },
            onAddMock = {
                val now = System.currentTimeMillis()
                val n = homeViewModel.onAddMockData(now)
                sellersViewModel.onAddMockData(now)
                searchViewModel.onAddMockData(now)
                Toast.makeText(context, context.getString(R.string.mock_added, n), Toast.LENGTH_SHORT).show()
            },
            onDeleteMock = {
                val n = homeViewModel.onDeleteMockData()
                sellersViewModel.onDeleteMockData()
                searchViewModel.onDeleteMockData()
                Toast.makeText(context, context.getString(R.string.mock_deleted, n), Toast.LENGTH_SHORT).show()
            },
        )
    }

    pendingShare?.let { text ->
        AlertDialog(
            onDismissRequest = { pendingShare = null },
            title = { Text(stringResource(R.string.share_title)) },
            text = { Text(text.take(160), maxLines = 3) },
            confirmButton = {
                TextButton(onClick = {
                    homeViewModel.onSharedLink(text); tab = Tab.ITEMS; pendingShare = null
                }) { Text(stringResource(R.string.share_as_listing)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    sellersViewModel.onSharedSeller(text); showSellers = true; pendingShare = null
                }) { Text(stringResource(R.string.share_as_seller)) }
            },
        )
    }
}

/** Floating pill navigation: the selected tab is an ink-black pill that fades in; the bar lifts off the canvas with a soft shadow. */
@Composable
private fun FloatingTabBar(selected: Tab, onSelect: (Tab) -> Unit) {
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Dimens.screen, vertical = 12.dp)) {
        Row(
            Modifier.fillMaxWidth()
                .shadow(14.dp, RoundedCornerShape(32.dp), ambientColor = Color.Black.copy(alpha = 0.10f), spotColor = Color.Black.copy(alpha = 0.10f))
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.background)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Tab.entries.forEach { t ->
                val on = t == selected
                val bg by animateColorAsState(
                    if (on) MaterialTheme.colorScheme.primary else Color.Transparent, tween(240), label = "tabBg")
                val fg by animateColorAsState(
                    if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    tween(240), label = "tabFg")
                Box(
                    Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(26.dp)).background(bg)
                        .clickable(onClick = { onSelect(t) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(t.label), color = fg, fontSize = 15.sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium)
                }
            }
        }
    }
}

/** Detects a JSON backup vs a CSV export and merges whichever it is by id, then reports counts. */
private suspend fun importBackup(
    context: android.content.Context,
    uri: android.net.Uri,
    homeViewModel: HomeViewModel,
    sellersViewModel: SellersViewModel,
) {
    val text = withContext(Dispatchers.IO) {
        runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
    } ?: run {
        Toast.makeText(context, R.string.import_failed, Toast.LENGTH_SHORT).show()
        return
    }
    if (text.trim().startsWith("{")) {
        val parsed = Backup.parse(text)
        if (parsed == null || (parsed.items.isEmpty() && parsed.sellers.isEmpty())) {
            Toast.makeText(context, R.string.import_nothing, Toast.LENGTH_SHORT).show()
            return
        }
        homeViewModel.onImportItems(parsed.items)
        sellersViewModel.onImportSellers(parsed.sellers)
        homeViewModel.reportImport(parsed.items.size, parsed.sellers.size)
    } else {
        val items = csvToItems(text)
        if (items.isEmpty()) {
            Toast.makeText(context, R.string.import_nothing, Toast.LENGTH_SHORT).show()
            return
        }
        homeViewModel.onImportItems(items)
        homeViewModel.reportImport(items.size, 0)
    }
}
