package com.daftech.cukurboss.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.daftech.cukurboss.Session
import com.daftech.cukurboss.data.Db
import com.daftech.cukurboss.data.entity.BusinessProfile
import com.daftech.cukurboss.data.entity.Customer
import com.daftech.cukurboss.data.entity.Product
import com.daftech.cukurboss.data.entity.Sale
import com.daftech.cukurboss.data.entity.SaleItem
import com.daftech.cukurboss.qr.QrBitmap
import com.daftech.cukurboss.util.Fmt
import kotlinx.coroutines.launch

private enum class CkStage { CUSTOMER, METHOD, TUNAI, TRANSFER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KasirScreen(onReceipt: (Long) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var products by remember { mutableStateOf(listOf<Product>()) }
    var query by remember { mutableStateOf("") }
    val cart = remember { mutableStateMapOf<Long, Int>() }
    var showSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        products = Db.get(ctx).productDao().active()
    }

    val filtered = products.filter { it.name.contains(query, ignoreCase = true) }
    val cartItems: List<Pair<Product, Int>> = cart.entries.mapNotNull { (id, qty) ->
        products.find { it.id == id }?.let { it to qty }
    }
    val total = cartItems.sumOf { (p, q) -> p.price * q }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("Kasir")
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            label = { Text("Cari layanan / produk") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true
        )
        LazyColumn(Modifier.weight(1f)) {
            items(filtered, key = { it.id }) { p ->
                val qty = cart[p.id] ?: 0
                ListItem(
                    headlineContent = { Text(p.name) },
                    supportingContent = { Text("${p.type} • ${Fmt.rp(p.price)}") },
                    trailingContent = {
                        if (qty == 0) {
                            IconButton(onClick = { cart[p.id] = 1 }) {
                                Icon(Icons.Filled.AddCircle, contentDescription = "Tambah")
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    if (qty <= 1) cart.remove(p.id) else cart[p.id] = qty - 1
                                }) { Icon(Icons.Filled.RemoveCircle, contentDescription = "Kurang") }
                                Text("$qty", fontWeight = FontWeight.Bold)
                                IconButton(onClick = { cart[p.id] = qty + 1 }) {
                                    Icon(Icons.Filled.AddCircle, contentDescription = "Tambah")
                                }
                            }
                        }
                    },
                    modifier = Modifier.clickable { cart[p.id] = qty + 1 }
                )
                HorizontalDivider()
            }
        }
        if (cartItems.isNotEmpty()) {
            Surface(shadowElevation = 8.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("${cartItems.sumOf { it.second }} item")
                        Text(
                            Fmt.rp(total),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Button(onClick = { showSheet = true }) { Text("Bayar") }
                }
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
            CheckoutSheetContent(
                items = cartItems,
                total = total,
                onFinished = { saleId ->
                    showSheet = false
                    cart.clear()
                    onReceipt(saleId)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckoutSheetContent(
    items: List<Pair<Product, Int>>,
    total: Long,
    onFinished: (Long) -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var stage by remember { mutableStateOf(CkStage.CUSTOMER) }

    var customer by remember { mutableStateOf<Customer?>(null) }
    var custQuery by remember { mutableStateOf("") }
    var custResults by remember { mutableStateOf(listOf<Customer>()) }
    var showQuick by remember { mutableStateOf(false) }
    var quickName by remember { mutableStateOf("") }
    var quickPhone by remember { mutableStateOf("") }
    var quickConsent by remember { mutableStateOf(false) }

    var paidStr by remember { mutableStateOf("") }
    var business by remember { mutableStateOf<BusinessProfile?>(null) }
    var err by remember { mutableStateOf("") }

    var savedSale by remember { mutableStateOf<Sale?>(null) }
    var savedWithCustomer by remember { mutableStateOf(false) }
    var askSaveCustomer by remember { mutableStateOf(false) }
    var dlgName by remember { mutableStateOf("") }
    var dlgPhone by remember { mutableStateOf("") }
    var dlgConsent by remember { mutableStateOf(false) }

    fun searchCust() {
        scope.launch {
            val dao = Db.get(ctx).customerDao()
            custResults = if (custQuery.isBlank()) dao.all() else dao.search(custQuery)
        }
    }

    LaunchedEffect(Unit) {
        business = Db.get(ctx).businessDao().get()
        searchCust()
    }

    /** Simpan transaksi; mengembalikan Sale yang tersimpan. */
    suspend fun persistSale(method: String, status: String, paid: Long, change: Long): Sale {
        val db = Db.get(ctx)
        var custId = customer?.id
        var custName = customer?.name ?: ""
        var withCust = custId != null
        if (custId == null && showQuick && quickName.isNotBlank()) {
            custId = db.customerDao().insert(
                Customer(
                    name = quickName.trim(),
                    phone = quickPhone.trim(),
                    promoConsent = quickConsent
                )
            )
            custName = quickName.trim()
            withCust = true
        }
        val phone = Session.currentPhone(ctx) ?: ""
        val cashierName = db.userDao().get(phone)?.name ?: ""
        val saleId = db.saleDao().insert(
            Sale(
                cashierPhone = phone,
                cashierName = cashierName,
                customerId = custId,
                customerName = custName,
                paymentMethod = method,
                status = status,
                total = total,
                paidAmount = paid,
                changeAmount = change
            )
        )
        db.saleItemDao().insertAll(
            items.map { (p, q) ->
                SaleItem(saleId = saleId, productName = p.name, price = p.price, qty = q)
            }
        )
        val sale = db.saleDao().get(saleId)!!
        savedSale = sale
        savedWithCustomer = withCust
        return sale
    }

    fun afterSave() {
        if (savedWithCustomer) {
            onFinished(savedSale!!.id)
        } else {
            askSaveCustomer = true
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Total: ${Fmt.rp(total)}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        when (stage) {
            CkStage.CUSTOMER -> {
                Text("Pelanggan (opsional)", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    custQuery, { custQuery = it; searchCust() },
                    Modifier.fillMaxWidth(), label = { Text("Cari nama / no. HP") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) }, singleLine = true
                )
                custResults.take(5).forEach { c ->
                    ListItem(
                        headlineContent = { Text(c.name) },
                        supportingContent = { Text(c.phone) },
                        trailingContent = {
                            if (customer?.id == c.id) Icon(Icons.Filled.Check, null)
                        },
                        modifier = Modifier.clickable {
                            customer = c
                            showQuick = false
                        }
                    )
                }
                if (!showQuick) {
                    OutlinedButton(onClick = { showQuick = true }, Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Person, null); Spacer(Modifier.size(8.dp)); Text("Tambah cepat")
                    }
                } else {
                    AppField(quickName, { quickName = it }, "Nama pelanggan")
                    AppField(
                        quickPhone, { quickPhone = Fmt.digitsOnly(it) }, "No. HP",
                        keyboard = KeyboardType.Phone
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(quickConsent, { quickConsent = it })
                        Text("Bersedia terima info promo")
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { customer = null; showQuick = false; stage = CkStage.METHOD },
                        modifier = Modifier.weight(1f)
                    ) { Text("Lewati") }
                    Button(
                        onClick = { stage = CkStage.METHOD },
                        modifier = Modifier.weight(1f)
                    ) { Text("Lanjut Bayar") }
                }
            }

            CkStage.METHOD -> {
                Text("Metode pembayaran", fontWeight = FontWeight.Bold)
                Button(onClick = { stage = CkStage.TUNAI }, Modifier.fillMaxWidth()) {
                    Text("TUNAI / Cash")
                }
                OutlinedButton(onClick = { stage = CkStage.TRANSFER }, Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.QrCode, null); Spacer(Modifier.size(8.dp)); Text("TRANSFER via QR")
                }
                TextButton(onClick = { stage = CkStage.CUSTOMER }) { Text("Kembali") }
            }

            CkStage.TUNAI -> {
                Text("Tunai", fontWeight = FontWeight.Bold)
                AppField(
                    paidStr, { paidStr = Fmt.digitsOnly(it) }, "Jumlah dibayar",
                    keyboard = KeyboardType.Number
                )
                val paid = paidStr.toLongOrNull() ?: 0L
                if (paidStr.isNotEmpty()) {
                    Text("Kembalian: ${Fmt.rp((paid - total).coerceAtLeast(0))}")
                }
                if (err.isNotEmpty()) Text(err, color = MaterialTheme.colorScheme.error)
                PrimaryButton("Simpan", enabled = paid >= total && total > 0) {
                    scope.launch {
                        persistSale("TUNAI", "LUNAS", paid, paid - total)
                        afterSave()
                    }
                }
                TextButton(onClick = { stage = CkStage.METHOD }) { Text("Kembali") }
            }

            CkStage.TRANSFER -> {
                Text("Transfer via QR", fontWeight = FontWeight.Bold)
                val b = business
                val payload = "CUKURBOSS TRANSFER\nBank: ${b?.bank.orEmpty()}\n" +
                    "No. Rek: ${b?.accountNo.orEmpty()}\nA.n: ${b?.accountName.orEmpty()}\n" +
                    "Nominal: ${Fmt.rp(total)}"
                val qrBmp = remember(payload) { QrBitmap.generate(payload, 640)?.asImageBitmap() }
                if (qrBmp != null) {
                    Image(
                        qrBmp, "QR Transfer",
                        Modifier
                            .size(240.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                }
                Text(
                    "${b?.bank.orEmpty()} • ${b?.accountNo.orEmpty()}\nA.n. ${b?.accountName.orEmpty()}\nNominal: ${Fmt.rp(total)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Pelanggan scan QR lalu transfer manual via m-banking. " +
                        "Kasir cek mutasi sebelum konfirmasi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PrimaryButton("Konfirmasi pembayaran diterima") {
                    scope.launch {
                        persistSale("TRANSFER", "LUNAS", total, 0)
                        afterSave()
                    }
                }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            persistSale("TRANSFER", "BELUM_BAYAR", 0, 0)
                            afterSave()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Simpan sebagai BELUM BAYAR") }
                TextButton(onClick = { stage = CkStage.METHOD }) { Text("Kembali") }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    // Dialog: tawarkan simpan data pelanggan bila transaksi tanpa pelanggan.
    if (askSaveCustomer && savedSale != null) {
        AlertDialog(
            onDismissRequest = {
                askSaveCustomer = false
                onFinished(savedSale!!.id)
            },
            title = { Text("Simpan data pelanggan?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Untuk kebutuhan broadcast promo.")
                    AppField(dlgName, { dlgName = it }, "Nama pelanggan")
                    AppField(
                        dlgPhone, { dlgPhone = Fmt.digitsOnly(it) }, "No. HP",
                        keyboard = KeyboardType.Phone
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(dlgConsent, { dlgConsent = it })
                        Text("Bersedia terima info promo")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val sale = savedSale!!
                        if (dlgName.isNotBlank()) {
                            val db = Db.get(ctx)
                            val cid = db.customerDao().insert(
                                Customer(
                                    name = dlgName.trim(),
                                    phone = dlgPhone.trim(),
                                    promoConsent = dlgConsent
                                )
                            )
                            db.saleDao().update(sale.copy(customerId = cid, customerName = dlgName.trim()))
                        }
                        askSaveCustomer = false
                        onFinished(sale.id)
                    }
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = {
                    askSaveCustomer = false
                    onFinished(savedSale!!.id)
                }) { Text("Lewati") }
            }
        )
    }
}
