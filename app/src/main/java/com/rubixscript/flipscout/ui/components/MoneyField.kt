package com.rubixscript.flipscout.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

/** Money input: same quiet filled pill as [SoftTextField], with the currency prefix built in. */
@Composable
fun MoneyField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    SoftTextField(
        value = value,
        onValueChange = onChange,
        label = label,
        modifier = modifier,
        singleLine = true,
        keyboardType = KeyboardType.Decimal,
        prefix = "$",
    )
}
