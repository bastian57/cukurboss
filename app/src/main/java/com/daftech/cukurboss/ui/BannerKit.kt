package com.daftech.cukurboss.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daftech.cukurboss.ui.theme.BlackGold
import com.daftech.cukurboss.ui.theme.PureBlack
import com.daftech.cukurboss.ui.theme.PureWhite
import kotlinx.coroutines.delay

/**
 * Kit banner + teks animasi "Black Gold" CukurBoss.
 *
 * Banner disimpan sebagai base64 di assets/banners/*.webp.b64
 * (agar lolos push tanpa file binary), di-decode sekali lalu di-cache.
 */

/** Nama file banner (tanpa ekstensi): banner_dashboard, banner_kasir, banner_produk, banner_pelanggan */
object Banners {
    const val DASHBOARD = "banner_dashboard"
    const val KASIR = "banner_kasir"
    const val PRODUK = "banner_produk"
    const val PELANGGAN = "banner_pelanggan"
}

private val bannerCache = mutableMapOf<String, androidx.compose.ui.graphics.ImageBitmap?>()

private fun loadBanner(ctx: android.content.Context, name: String): androidx.compose.ui.graphics.ImageBitmap? {
    return bannerCache.getOrPut(name) {
        try {
            ctx.assets.open("banners/$name.webp.b64").use { input ->
                val bytes = Base64.decode(input.bufferedReader().readText(), Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }
        } catch (_: Exception) {
            null
        }
    }
}

/** Gambar banner dari assets; fallback gradasi emas bila gagal dimuat. */
@Composable
fun BannerImage(
    name: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val ctx = LocalContext.current
    val bmp = remember(name) { loadBanner(ctx, name) }
    if (bmp != null) {
        Image(
            bitmap = bmp,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier.background(
                Brush.linearGradient(
                    listOf(Color(0xFF3A2A00), PureBlack)
                )
            )
        )
    }
}

/**
 * Header banner premium: foto + gradasi hitam + judul/subjudul overlay.
 * Dipakai di setiap layer (Kasir, Produk, Pelanggan, Beranda).
 */
@Composable
fun PremiumBanner(
    banner: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    height: Dp = 170.dp,
    corner: Dp = 0.dp
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(corner))
    ) {
        BannerImage(banner, Modifier.matchParentSize())
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xDD000000))
                    )
                )
        )
        // garis emas bawah
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(3.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, BlackGold, Color.Transparent)
                    )
                )
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = PureWhite,
                letterSpacing = 1.sp
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = BlackGold,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** Teks mengetik huruf per huruf (typewriter). */
@Composable
fun TypewriterText(
    fullText: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = PureWhite,
    charDelayMs: Long = 45L
) {
    var shown by remember(fullText) { mutableStateOf("") }
    LaunchedEffect(fullText) {
        shown = ""
        for (i in fullText.indices) {
            shown = fullText.substring(0, i + 1)
            delay(charDelayMs)
        }
    }
    Text(shown, modifier, style = style, color = color)
}

/** Teks dengan kilau emas berjalan (shimmer). */
@Composable
fun ShimmerText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineMedium,
    baseColor: Color = BlackGold
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing)
        ),
        label = "shimmerProgress"
    )
    val brush = Brush.linearGradient(
        colors = listOf(baseColor, Color(0xFFFFF3C4), baseColor),
        start = Offset(progress * 900f - 250f, 0f),
        end = Offset(progress * 900f + 250f, 0f)
    )
    Text(
        text,
        modifier,
        style = style.copy(brush = brush),
        fontWeight = FontWeight.Black
    )
}

/** Teks promo berjalan (marquee). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PromoMarquee(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text,
        modifier = modifier.basicMarquee(iterations = Int.MAX_VALUE),
        style = MaterialTheme.typography.bodySmall,
        color = BlackGold,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1
    )
}
