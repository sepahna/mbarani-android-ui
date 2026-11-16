package com.v2ray.ang.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** Static, inexpensive app background. */
@Composable
internal fun BaranAtmosphere(
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    val background = if (darkTheme) Color(0xFF101820) else Color(0xFFF1F7F4)
    Box(modifier.background(background))
}
