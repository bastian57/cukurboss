package com.daftech.cukurboss.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.daftech.cukurboss.ui.theme.BlackCard
import com.daftech.cukurboss.ui.theme.BlackGold

private data class Tab(val route: String, val label: String, val icon: ImageVector)

/** Layar utama Black Gold: Beranda + 4 tab, bottom nav floating. */
@Composable
fun MainScreen(
    onReceipt: (Long) -> Unit,
    onEditBusiness: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val inner = rememberNavController()
    var selected by remember { mutableIntStateOf(0) }
    var tick by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        Tab("beranda", "Beranda", Icons.Filled.Home),
        Tab("kasir", "Kasir", Icons.Filled.PointOfSale),
        Tab("produk", "Produk", Icons.Filled.Store),
        Tab("pelanggan", "Pelanggan", Icons.Filled.People),
        Tab("laporan", "Laporan", Icons.Filled.Assessment)
    )

    fun goTab(i: Int) {
        selected = i
        tick++
        inner.navigate(tabs[i].route) {
            popUpTo(inner.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = Color(0xFF000000),
        bottomBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(30.dp),
                    color = BlackCard,
                    shadowElevation = 16.dp,
                    border = BorderStroke(1.dp, Color(0x44FFB300))
                ) {
                    Row(
                        Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        tabs.forEachIndexed { i, t ->
                            val active = selected == i
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(22.dp))
                                    .clickable { goTab(i) }
                                    .padding(horizontal = 13.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    t.icon,
                                    contentDescription = t.label,
                                    tint = if (active) BlackGold else Color(0xFF777777)
                                )
                                Text(
                                    t.label,
                                    fontSize = 10.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                    color = if (active) BlackGold else Color(0xFF777777)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { pad ->
        NavHost(
            navController = inner,
            startDestination = "beranda",
            modifier = Modifier.padding(pad)
        ) {
            composable("beranda") {
                key(tick) {
                    DashboardScreen(
                        onOpenTab = { goTab(it) },
                        onOpenSettings = { inner.navigate("pengaturan") }
                    )
                }
            }
            composable("kasir") {
                key(tick) { KasirScreen(onReceipt = onReceipt) }
            }
            composable("produk") {
                key(tick) { ProdukScreen() }
            }
            composable("pelanggan") {
                key(tick) { PelangganScreen() }
            }
            composable("laporan") {
                key(tick) { LaporanScreen(onReceipt = onReceipt) }
            }
            composable("pengaturan") {
                PengaturanScreen(
                    onEditBusiness = onEditBusiness,
                    onLoggedOut = onLoggedOut,
                    onBack = { inner.popBackStack() }
                )
            }
        }
    }
}
