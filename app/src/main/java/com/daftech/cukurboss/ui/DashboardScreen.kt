package com.daftech.cukurboss.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daftech.cukurboss.data.Db
import com.daftech.cukurboss.ui.theme.BlackCard
import com.daftech.cukurboss.ui.theme.BlackGold
import com.daftech.cukurboss.ui.theme.PureBlack
import com.daftech.cukurboss.ui.theme.PureWhite
import com.daftech.cukurboss.util.Fmt
import java.util.Calendar

private fun startOfDay(ts: Long): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = ts
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

/** Beranda: hero banner + teks animasi + statistik + aksi cepat. */
@Composable
fun DashboardScreen(
    onOpenTab: (Int) -> Unit,
    onOpenSettings: () -> Unit
) {
    val ctx = LocalContext.current
    var omzet by remember { mutableLongStateOf(0L) }
    var trxCount by remember { mutableLongStateOf(0L) }
    var custCount by remember { mutableStateOf(0) }
    var prodCount by remember { mutableStateOf(0) }
    var bizName by remember { mutableStateOf("Bos") }

    LaunchedEffect(Unit) {
        val db = Db.get(ctx)
        val from = startOfDay(System.currentTimeMillis())
        val to = from + 24 * 60 * 60 * 1000L - 1
        omzet = db.saleDao().sumOmzet(from, to)
        trxCount = db.saleDao().countSales(from, to).toLong()
        custCount = db.customerDao().all().size
        prodCount = db.productDao().count()
        db.businessDao().get()?.let { bizName = it.name.ifBlank { "Bos" } }
    }

    val greeting = "Selamat datang kembali, $bizName ✂"

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ===== HERO =====
        Box(Modifier.fillMaxWidth().height(250.dp)) {
            BannerImage(Banners.DASHBOARD, Modifier.matchParentSize())
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0x55000000), Color(0xE6000000))
                        )
                    )
            )
            // tombol pengaturan
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(Icons.Filled.Settings, "Pengaturan", tint = PureWhite)
            }
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(18.dp)
            ) {
                TypewriterText(
                    greeting,
                    style = MaterialTheme.typography.titleMedium,
                    color = PureWhite
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Omzet hari ini",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB3B3B3)
                )
                ShimmerText(
                    Fmt.rp(omzet),
                    style = MaterialTheme.typography.displaySmall
                )
                Spacer(Modifier.height(6.dp))
                PromoMarquee(
                    "🎉 Tambah pelanggan baru untuk broadcast promo • 💈 Jaga kualitas, pelanggan balik lagi • "
                )
            }
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
        }

        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ===== STATISTIK =====
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard("Transaksi", "$trxCount", Modifier.weight(1f))
                StatCard("Pelanggan", "$custCount", Modifier.weight(1f))
                StatCard("Produk", "$prodCount", Modifier.weight(1f))
            }

            Text(
                "Aksi Cepat",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PureWhite
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickAction("Kasir", Icons.Filled.PointOfSale, Modifier.weight(1f)) { onOpenTab(1) }
                QuickAction("Produk", Icons.Filled.Store, Modifier.weight(1f)) { onOpenTab(2) }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickAction("Pelanggan", Icons.Filled.People, Modifier.weight(1f)) { onOpenTab(3) }
                QuickAction("Laporan", Icons.Filled.Assessment, Modifier.weight(1f)) { onOpenTab(4) }
            }

            Watermark()
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BlackCard),
        border = BorderStroke(1.dp, Color(0x33FFB300))
    ) {
        Column(
            Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = BlackGold
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB3B3B3)
            )
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = BlackGold),
        border = BorderStroke(1.dp, Color(0xFFFFD54F))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, label, tint = PureBlack, modifier = Modifier.size(26.dp))
            Text(
                label,
                fontWeight = FontWeight.Bold,
                color = PureBlack,
                fontSize = 15.sp
            )
        }
    }
}
