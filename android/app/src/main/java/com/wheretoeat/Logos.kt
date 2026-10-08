package com.wheretoeat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

private fun bankLogoRes(bank: String): Int? = when (bank.lowercase()) {
    "metrobank" -> R.drawable.bank_metrobank
    "bdo" -> R.drawable.bank_bdo
    "unionbank" -> R.drawable.bank_unionbank
    else -> null
}

/** The bank's logo on a small white tile, so dark logos stay visible in dark mode. */
@Composable
fun BankLogo(bank: String, size: Dp = 20.dp) {
    val res = bankLogoRes(bank)
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size / 4)).background(Color.White)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(size / 4)),
        contentAlignment = Alignment.Center,
    ) {
        if (res != null) {
            Image(painterResource(res), "$bank logo", Modifier.padding(size / 10).size(size))
        } else {
            Text(bank.take(1), color = Color.Black, fontSize = (size.value * 0.55f).sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * The restaurant's logo (already cut out of the bank's promo panel by the feed), shown whole on a white tile.
 * While it loads a shimmer shows; without an image (or if it fails), a letter badge stands in.
 */
@Composable
fun PlaceLogo(name: String, image: String?, size: Dp = 60.dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(size / 4)
    // Takes its own modifier: as the image's loading/error placeholder it must not repeat the image's modifier.
    val letter = @Composable { m: Modifier ->
        Box(
            m.size(size).clip(shape).background(MaterialTheme.colorScheme.tertiaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                name.trim().firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?",
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                fontSize = (size.value * 0.42f).sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = Serif,
            )
        }
    }
    if (image == null) {
        letter(modifier)
        return
    }
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(image).crossfade(true).build(),
        contentDescription = "$name logo",
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size).clip(shape).background(Color.White)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape), // keeps white tiles visible on ivory
        loading = { Box(Modifier.size(size).clip(shape).shimmer()) },
        error = { letter(Modifier) },
    )
}
