package com.elementa.wallet.ui.components

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.elementa.wallet.domain.model.Chain

/**
 * Renders a logo for the given [chain].
 *
 * First attempts to load a local drawable named `image_<chainName>` (e.g. "image_bitcoin").
 * If the resource cannot be found and [logoUrl] is provided, it will fall back to loading
 * the remote URL via Coil's [AsyncImage].  When both resources are missing a simple
 * single-character fallback is shown.
 */
@Composable
fun ChainLogo(
    chain: Chain,
    logoUrl: String?,
    modifier: Modifier = Modifier.size(36.dp)
) {
    val context = LocalContext.current
    // Compose remembers the resolved resource id for performance.
    val resName = remember(chain) { "image_${chain.name.lowercase()}" }
    val resId = remember(resName) { context.getDrawableId(resName) }

    when {
        resId != 0 -> {
            Image(
                painter = painterResource(id = resId),
                contentDescription = chain.name,
                modifier = modifier
            )
        }
        !logoUrl.isNullOrBlank() -> {
            AsyncImage(
                model = logoUrl,
                contentDescription = chain.name,
                modifier = modifier
            )
        }
        else -> {
            // fallback: single-letter symbol
            Surface(
                shape = CircleShape,
                modifier = modifier
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        chain.name.take(1),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Extension helper to find drawable id by name in the current package
private fun Context.getDrawableId(name: String): Int {
    return resources.getIdentifier(name, "drawable", packageName)
}
