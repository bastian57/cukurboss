package com.daftech.cukurboss.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

private data class Tab(val route: String, val label: String, val icon: @Composable () -> Unit)

@Composable
private fun tabIcon(imageVector: androidx.compose.ui.graphics.vector.ImageVector, desc: String): @Composable () -> Unit =
    { Icon(imageVector, contentDescription = desc) }

/** Layar utama: bottom nav 5 tab. */
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
        Tab("kasir", "Kasir", tabIcon(Icons.Filled.PointOfSale, "Kasir")),
        Tab("produk", "Produk", tabIcon(Icons.Filled.Store, "Produk")),
        Tab("pelanggan", "Pelanggan", tabIcon(Icons.Filled.People, "Pelanggan")),
        Tab("laporan", "Laporan", tabIcon(Icons.Filled.Assessment, "Laporan")),
        Tab("pengaturan", "Pengaturan", tabIcon(Icons.Filled.Settings, "Pengaturan"))
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = selected == i,
                        onClick = {
                            selected = i
                            tick++
                            inner.navigate(t.route) {
                                popUpTo(inner.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = t.icon,
                        label = { Text(t.label) }
                    )
                }
            }
        }
    ) { pad ->
        NavHost(
            navController = inner,
            startDestination = "kasir",
            modifier = Modifier.padding(pad)
        ) {
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
                key(tick) {
                    PengaturanScreen(
                        onEditBusiness = onEditBusiness,
                        onLoggedOut = onLoggedOut
                    )
                }
            }
        }
    }
}
