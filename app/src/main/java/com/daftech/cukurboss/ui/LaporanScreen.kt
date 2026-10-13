package com.daftech.cukurboss.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daftech.cukurboss.data.Db
import com.daftech.cukurboss.data.dao.TopItem
import com.daftech.cukurboss.data.entity.Sale
import com.daftech.cukurboss.util.Fmt
import kotlinx.coroutines.launch
import java.util.Calendar

private const val DAY_MS = 24 * 60 * 60 * 1000L

private fun startOfDay(ts: Long): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = ts
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

private data class Report(
    val omzet: Long = 0,
    val count: Int = 0,
    val tunai: Long = 0,
    val transfer: Long = 0,
    val unpaid: Long = 0,
    val sales: List<Sale> = emptyList(),
    val top: List<TopItem> = emptyList()
)

@Composable
fun LaporanScreen(onReceipt: (Long) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var dayTs by remember { mutableStateOf(startOfDay(System.currentTimeMillis())) }
    var report by remember { mutableStateOf(Report()) }

    fun reload() {
        scope.launch {
            val dao = Db.get(ctx).saleDao()
            val from = dayTs
            val to = dayTs + DAY_MS - 1
            report = Report(
                omzet = dao.sumOmzet(from, to),
                count = dao.countSales(from, to),
                tunai = dao.sumTunai(from, to),
                transfer = dao.sumTransfer(from, to),
                unpaid = dao.sumUnpaid(from, to),
                sales = dao.listByDate(from, to),
                top = dao.topItems(from, to)
            )
        }
    }
    LaunchedEffect(dayTs) { reload() }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("Laporan")
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { dayTs -= DAY_MS }) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Hari sebelumnya")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Fmt.dateStr(dayTs), fontWeight = FontWeight.Bold)
                TextButton(onClick = { dayTs = startOfDay(System.currentTimeMillis()) }) {
                    Text("Hari ini")
                }
            }
            IconButton(onClick = { dayTs += DAY_MS }) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Hari berikutnya")
            }
        }

        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Omzet", color = MaterialTheme.colorScheme.onPrimary)
                        Text(
                            Fmt.rp(report.omzet),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            "${report.count} transaksi",
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Tunai", Fmt.rp(report.tunai), Modifier.weight(1f))
                    StatCard("Transfer", Fmt.rp(report.transfer), Modifier.weight(1f))
                    StatCard("Belum bayar", Fmt.rp(report.unpaid), Modifier.weight(1f))
                }
            }
            if (report.top.isNotEmpty()) {
                item {
                    Text("Terlaris", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                }
                items(report.top.take(5)) { t ->
                    ListItem(
                        headlineContent = { Text(t.productName) },
                        trailingContent = { Text("${t.qty}x") }
                    )
                    HorizontalDivider()
                }
            }
            if (report.sales.isNotEmpty()) {
                item {
                    Text("Transaksi", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                }
                items(report.sales, key = { it.id }) { s ->
                    ListItem(
                        headlineContent = { Text(Fmt.rp(s.total)) },
                        supportingContent = {
                            Text(
                                "${Fmt.dateTimeStr(s.createdAt)} • ${s.paymentMethod} • " +
                                    if (s.customerName.isNotBlank()) s.customerName else "Tanpa nama"
                            )
                        },
                        trailingContent = {
                            Text(
                                s.status,
                                color = if (s.status == "LUNAS") MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        modifier = Modifier.clickable { onReceipt(s.id) }
                    )
                    HorizontalDivider()
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(value, fontWeight = FontWeight.Bold)
        }
    }
}
