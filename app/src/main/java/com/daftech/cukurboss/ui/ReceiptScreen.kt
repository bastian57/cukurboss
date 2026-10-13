package com.daftech.cukurboss.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.daftech.cukurboss.data.Db
import com.daftech.cukurboss.print.EscPosPrinter
import com.daftech.cukurboss.print.ReceiptData
import com.daftech.cukurboss.print.ReceiptItem
import com.daftech.cukurboss.util.Fmt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Preview struk + cetak thermal + bagikan via WhatsApp. */
@Composable
fun ReceiptScreen(saleId: Long, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf<ReceiptData?>(null) }
    var msg by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(saleId) {
        val db = Db.get(ctx)
        val s = db.saleDao().get(saleId) ?: return@LaunchedEffect
        val items = db.saleItemDao().bySale(saleId)
        val b = db.businessDao().get()
        data = ReceiptData(
            businessName = b?.name.orEmpty(),
            address = b?.address.orEmpty(),
            datetime = Fmt.dateTimeStr(s.createdAt),
            cashier = s.cashierName.ifBlank { s.cashierPhone },
            customerName = s.customerName,
            items = items.map { ReceiptItem(it.productName, it.qty, it.price) },
            total = s.total,
            paymentMethod = s.paymentMethod,
            status = s.status,
            paid = s.paidAmount,
            change = s.changeAmount
        )
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("Struk", onBack = onBack)
        val d = data
        if (d == null) {
            Text("Memuat…", Modifier.padding(16.dp))
            return@Column
        }
        val text = remember(d) { EscPosPrinter.buildTextReceipt(d) }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card {
                Text(
                    text,
                    Modifier.padding(16.dp),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            PrimaryButton("Cetak ke printer", enabled = !busy) {
                busy = true
                msg = ""
                scope.launch(Dispatchers.IO) {
                    val mac = Db.get(ctx).businessDao().get()?.printerMac.orEmpty()
                    val res = if (mac.isBlank()) {
                        Result.failure(Exception("Printer belum diatur. Buka Pengaturan > Printer."))
                    } else {
                        EscPosPrinter.print(ctx, mac, EscPosPrinter.buildReceipt(d))
                    }
                    withContext(Dispatchers.Main) {
                        busy = false
                        msg = res.fold(
                            { "Struk terkirim ke printer." },
                            { "Gagal mencetak: ${it.message}" }
                        )
                    }
                }
            }
            OutlinedButton(
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    ctx.startActivity(Intent.createChooser(send, "Bagikan via WhatsApp"))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Share, null)
                Spacer(Modifier.padding(4.dp))
                Text("Bagikan via WhatsApp")
            }
            if (msg.isNotEmpty()) {
                Text(
                    msg,
                    color = if (msg.startsWith("Gagal")) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }
            Watermark()
            Spacer(Modifier.height(8.dp))
        }
    }
}
