package com.rubixscript.flipscout.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.rubixscript.flipscout.util.Photos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Draws an item photo cropped to fill its bounds; draws nothing until decoded (or if the file is gone). */
@Composable
fun PhotoImage(name: String, modifier: Modifier = Modifier, targetPx: Int = 256) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, name, targetPx) {
        value = withContext(Dispatchers.IO) { Photos.thumb(context, name, targetPx)?.asImageBitmap() }
    }
    bitmap?.let { Image(it, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop) }
}
