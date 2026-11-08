package com.rubixscript.flipscout.ui.theme

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.rubixscript.flipscout.R
import com.rubixscript.flipscout.data.Status

/** Presentation concerns for [Status] live here so the data layer stays UI-free. */
fun Status.color(): Color = when (this) {
    Status.WATCHING -> Palette.Blue
    Status.BOUGHT -> Palette.Violet
    Status.LISTED -> Palette.Amber
    Status.SOLD -> Palette.Emerald
}

@get:StringRes
val Status.labelRes: Int
    get() = when (this) {
        Status.WATCHING -> R.string.status_watching
        Status.BOUGHT -> R.string.status_bought
        Status.LISTED -> R.string.status_listed
        Status.SOLD -> R.string.status_sold
    }
