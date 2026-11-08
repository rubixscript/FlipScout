package com.rubixscript.flipscout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubixscript.flipscout.R
import com.rubixscript.flipscout.ui.components.GradientButton
import com.rubixscript.flipscout.ui.theme.Palette

/**
 * Settings sheet. "Reset all data" is in every build (it wipes at once, with Undo on the home snackbar);
 * the mock-data tools only appear when [showDeveloperTools] is true (debuggable builds).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    showDeveloperTools: Boolean,
    onDismiss: () -> Unit,
    onExportCsv: () -> Unit,
    onExportBackup: () -> Unit,
    onImport: () -> Unit,
    onReset: () -> Unit,
    onAddMock: () -> Unit,
    onDeleteMock: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall)

            SectionLabel(stringResource(R.string.settings_data_section))
            SoftButton(stringResource(R.string.menu_export_csv), onExportCsv)
            SoftButton(stringResource(R.string.menu_export_backup), onExportBackup)
            SoftButton(stringResource(R.string.menu_import), onImport)
            GradientButton(
                stringResource(R.string.reset_all), onClick = onReset, modifier = Modifier.fillMaxWidth(),
                container = Palette.Loss, content = Color.White,
            )
            Hint(stringResource(R.string.reset_all_hint))

            if (showDeveloperTools) {
                SectionLabel(stringResource(R.string.settings_dev_section))
                GradientButton(stringResource(R.string.mock_add), onClick = onAddMock, modifier = Modifier.fillMaxWidth())
                Hint(stringResource(R.string.mock_add_hint))
                SoftButton(stringResource(R.string.mock_delete), onDeleteMock)
                Hint(stringResource(R.string.mock_delete_hint))
            }
        }
    }
}

/** Quiet grey pill for non-destructive secondary actions. */
@Composable
private fun SoftButton(text: String, onClick: () -> Unit) = GradientButton(
    text, onClick = onClick, modifier = Modifier.fillMaxWidth(),
    container = MaterialTheme.colorScheme.surfaceVariant, content = MaterialTheme.colorScheme.onSurface,
)

@Composable
private fun SectionLabel(text: String) =
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp))

@Composable
private fun Hint(text: String) =
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
