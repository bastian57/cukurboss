package com.daftech.cukurboss.ui

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.daftech.cukurboss.Session
import com.daftech.cukurboss.data.Db
import com.daftech.cukurboss.data.entity.BusinessProfile
import com.daftech.cukurboss.print.EscPosPrinter
import com.daftech.cukurboss.print.ReceiptData
import com.daftech.cukurboss.util.Crypto
import com.daftech.cukurboss.util.Fmt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private fun appVersion(ctx: Context): String {
    return try {
        val pm = ctx.packageManager
        val pi = if (Build.VERSION.SDK_INT >= 33) {
            pm.getPackageInfo(ctx.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION") pm.getPackageInfo(ctx.packageName, 0)
        }
        pi.versionName ?: "1.0.0"
    } catch (_: Exception) {
        "1.0.0"
    }
}

@Composable
fun PengaturanScreen(onEditBusiness: () -> Unit, onLoggedOut: () -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var business by remember { mutableStateOf<BusinessProfile?>(null) }
    var msg by remember { mutableStateOf("") }
    var showPrinterPicker by remember { mutableStateOf(false) }
    var showPwDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    var pendingBtAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun reload() {
        scope.launch { business = Db.get(ctx).businessDao().get() }
    }
    LaunchedEffect(Unit) { reload() }

    val btPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pendingBtAction?.invoke()
        else msg = "Izin Bluetooth ditolak"
        pendingBtAction = null
    }

    fun withBtPermission(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= 31 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingBtAction = action
            btPermLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            action()
        }
    }

    fun testPrint() {
        withBtPermission {
            val mac = business?.printerMac.orEmpty()
            if (mac.isBlank()) {
                msg = "Pilih printer dulu."
                return@withBtPermission
            }
            scope.launch(Dispatchers.IO) {
                val sample = ReceiptData(
                    businessName = business?.name.orEmpty(),
                    address = business?.address.orEmpty(),
                    datetime = Fmt.dateTimeStr(System.currentTimeMillis()),
                    cashier = "Test",
                    customerName = "",
                    items = emptyList(),
                    total = 0,
                    paymentMethod = "TUNAI",
                    status = "LUNAS",
                    paid = 0,
                    change = 0
                )
                val res = EscPosPrinter.print(ctx, mac, EscPosPrinter.buildReceipt(sample))
                withContext(Dispatchers.Main) {
                    msg = res.fold({ "Test print terkirim." }, { "Gagal: ${it.message}" })
                }
            }
        }
    }

    fun backupDb() {
        scope.launch(Dispatchers.IO) {
            try {
                val src = ctx.getDatabasePath("cukurboss.db")
                if (!src.exists()) throw Exception("File database tidak ditemukan")
                val dst = File(
                    ctx.getExternalFilesDir(null),
                    "backup-cukurboss-${Fmt.fileStamp()}.db"
                )
                src.copyTo(dst, overwrite = true)
                val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", dst)
                withContext(Dispatchers.Main) {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "application/octet-stream"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    ctx.startActivity(Intent.createChooser(send, "Bagikan backup"))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { msg = "Backup gagal: ${e.message}" }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("Pengaturan", onBack = onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingRow("Profil usaha", business?.name ?: "-") { onEditBusiness() }

            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Bluetooth, null)
                        Spacer(Modifier.padding(4.dp))
                        Text("Printer Thermal", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        if (business?.printerMac.isNullOrBlank()) "Belum dipilih"
                        else "Terhubung: ${business?.printerMac}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { withBtPermission { showPrinterPicker = true } },
                            modifier = Modifier.weight(1f)
                        ) { Text("Pilih Printer") }
                        OutlinedButton(
                            onClick = { testPrint() },
                            modifier = Modifier.weight(1f)
                        ) { Text("Test Print") }
                    }
                }
            }

            SettingRow("Ubah password", "Ganti password login") { showPwDialog = true }
            SettingRow("Ubah PIN darurat", "Ganti PIN 6 digit") { showPinDialog = true }
            SettingRow("Backup data", "Salin file database") { backupDb() }
            SettingRow("Tentang", "Versi & info aplikasi") { showAbout = true }
            SettingRow("Keluar", "Kembali ke layar login", danger = true) { confirmLogout = true }

            if (msg.isNotEmpty()) {
                Text(
                    msg,
                    color = if (msg.startsWith("Gagal") || msg.contains("ditolak")) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showPrinterPicker) {
        var devices by remember { mutableStateOf(listOf<Pair<String, String>>()) }
        LaunchedEffect(Unit) {
            withBtPermission {
                try {
                    val bm = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                    devices = bm.adapter?.bondedDevices
                        ?.map { (it.name ?: "Tanpa nama") to it.address }
                        ?: emptyList()
                } catch (_: SecurityException) {
                    msg = "Izin Bluetooth ditolak"
                }
            }
        }
        AlertDialog(
            onDismissRequest = { showPrinterPicker = false },
            title = { Text("Pilih Printer Bluetooth") },
            text = {
                if (devices.isEmpty()) {
                    Text("Tidak ada perangkat ter-pairing. Pairing dulu via Pengaturan Bluetooth HP.")
                } else {
                    LazyColumn {
                        items(devices) { (name, mac) ->
                            ListItem(
                                headlineContent = { Text(name) },
                                supportingContent = { Text(mac) },
                                trailingContent = {
                                    if (business?.printerMac == mac) Icon(Icons.Filled.Check, null)
                                },
                                modifier = Modifier.clickable {
                                    scope.launch {
                                        val b = Db.get(ctx).businessDao().get()
                                        if (b != null) {
                                            Db.get(ctx).businessDao().upsert(b.copy(printerMac = mac))
                                        }
                                        reload()
                                        showPrinterPicker = false
                                        toast(ctx, "Printer tersimpan")
                                    }
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrinterPicker = false }) { Text("Tutup") }
            }
        )
    }

    if (showPwDialog) {
        var oldPw by remember { mutableStateOf("") }
        var newPw by remember { mutableStateOf("") }
        var newPw2 by remember { mutableStateOf("") }
        var err by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPwDialog = false },
            title = { Text("Ubah Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppField(oldPw, { oldPw = it }, "Password lama", password = true)
                    AppField(newPw, { newPw = it }, "Password baru", password = true)
                    AppField(newPw2, { newPw2 = it }, "Konfirmasi password baru", password = true)
                    if (err.isNotEmpty()) Text(err, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val phone = Session.currentPhone(ctx) ?: ""
                        val u = Db.get(ctx).userDao().get(phone)
                        err = when {
                            u == null -> "Sesi tidak valid"
                            Crypto.hashPassword(oldPw, u.salt) != u.hash -> "Password lama salah"
                            newPw.length < 4 -> "Password minimal 4 karakter"
                            newPw != newPw2 -> "Konfirmasi tidak sama"
                            else -> ""
                        }
                        if (err.isNotEmpty()) return@launch
                        Db.get(ctx).userDao().update(u!!.copy(hash = Crypto.hashPassword(newPw, u.salt)))
                        showPwDialog = false
                        toast(ctx, "Password berhasil diubah")
                    }
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showPwDialog = false }) { Text("Batal") }
            }
        )
    }

    if (showPinDialog) {
        var oldPin by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        var newPin2 by remember { mutableStateOf("") }
        var err by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Ubah PIN Darurat") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppField(
                        oldPin, { oldPin = Fmt.digitsOnly(it).take(6) }, "PIN lama",
                        keyboard = KeyboardType.NumberPassword, password = true
                    )
                    AppField(
                        newPin, { newPin = Fmt.digitsOnly(it).take(6) }, "PIN baru (6 digit)",
                        keyboard = KeyboardType.NumberPassword, password = true
                    )
                    AppField(
                        newPin2, { newPin2 = Fmt.digitsOnly(it).take(6) }, "Konfirmasi PIN baru",
                        keyboard = KeyboardType.NumberPassword, password = true
                    )
                    if (err.isNotEmpty()) Text(err, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val phone = Session.currentPhone(ctx) ?: ""
                        val u = Db.get(ctx).userDao().get(phone)
                        err = when {
                            u == null -> "Sesi tidak valid"
                            Crypto.hashPassword(oldPin, u.salt) != u.pinHash -> "PIN lama salah"
                            newPin.length != 6 -> "PIN harus 6 digit"
                            newPin != newPin2 -> "Konfirmasi tidak sama"
                            else -> ""
                        }
                        if (err.isNotEmpty()) return@launch
                        Db.get(ctx).userDao().update(u!!.copy(pinHash = Crypto.hashPassword(newPin, u.salt)))
                        showPinDialog = false
                        toast(ctx, "PIN darurat berhasil diubah")
                    }
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text("Batal") }
            }
        )
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("Tentang CukurBoss") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CukurBoss", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    Text("Kasir barbershop offline • v${appVersion(ctx)}")
                    Spacer(Modifier.height(8.dp))
                    Watermark()
                }
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text("Tutup") }
            }
        )
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Keluar?") },
            text = { Text("Kembali ke layar login.") },
            confirmButton = {
                TextButton(onClick = {
                    Session.logout(ctx)
                    confirmLogout = false
                    onLoggedOut()
                }) { Text("Keluar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    Card(onClick = onClick) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    color = if (danger) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}
