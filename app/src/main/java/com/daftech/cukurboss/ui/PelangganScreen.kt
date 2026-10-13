package com.daftech.cukurboss.ui

import android.content.Intent
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.daftech.cukurboss.data.Db
import com.daftech.cukurboss.data.entity.Customer
import com.daftech.cukurboss.util.Fmt
import kotlinx.coroutines.launch

@Composable
fun PelangganScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf(listOf<Customer>()) }
    var query by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Customer?>(null) }
    var confirmDelete by remember { mutableStateOf<Customer?>(null) }
    var showBroadcast by remember { mutableStateOf(false) }

    fun reload() {
        scope.launch {
            val dao = Db.get(ctx).customerDao()
            list = if (query.isBlank()) dao.all() else dao.search(query)
        }
    }
    LaunchedEffect(Unit) { reload() }

    Scaffold(
        topBar = {
            PremiumBanner(
                banner = Banners.PELANGGAN,
                title = "PELANGGAN",
                subtitle = "Data & broadcast promo",
                height = 150.dp
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah")
            }
        }
    ) { _ ->
        Column(Modifier.fillMaxSize()) {
            OutlinedTextField(
                query, { query = it; reload() },
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                label = { Text("Cari nama / no. HP") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true
            )
            Button(
                onClick = { showBroadcast = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Icon(Icons.Filled.Campaign, null)
                Spacer(Modifier.padding(4.dp))
                Text("Broadcast Promo")
            }
            Spacer(Modifier.height(4.dp))
            LazyColumn(Modifier.weight(1f)) {
                items(list, key = { it.id }) { c ->
                    ListItem(
                        headlineContent = { Text(c.name) },
                        supportingContent = {
                            Text(c.phone + if (c.promoConsent) " • mau promo" else "")
                        },
                        trailingContent = {
                            IconButton(onClick = { confirmDelete = c }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Hapus")
                            }
                        },
                        modifier = Modifier.clickable { editing = c; showDialog = true }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showDialog) {
        var name by remember(editing) { mutableStateOf(editing?.name ?: "") }
        var phone by remember(editing) { mutableStateOf(editing?.phone ?: "") }
        var consent by remember(editing) { mutableStateOf(editing?.promoConsent ?: false) }
        var err by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editing == null) "Tambah Pelanggan" else "Ubah Pelanggan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppField(name, { name = it }, "Nama")
                    AppField(
                        phone, { phone = Fmt.digitsOnly(it) }, "No. HP",
                        keyboard = KeyboardType.Phone
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(consent, { consent = it })
                        Text("Bersedia terima info promo")
                    }
                    if (err.isNotEmpty()) Text(err, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    err = when {
                        name.isBlank() -> "Nama wajib diisi"
                        phone.length < 9 -> "No. HP tidak valid"
                        else -> ""
                    }
                    if (err.isNotEmpty()) return@TextButton
                    scope.launch {
                        val dao = Db.get(ctx).customerDao()
                        val e = editing
                        if (e == null) {
                            dao.insert(Customer(name = name.trim(), phone = phone, promoConsent = consent))
                        } else {
                            dao.update(e.copy(name = name.trim(), phone = phone, promoConsent = consent))
                        }
                        showDialog = false
                        reload()
                    }
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Batal") }
            }
        )
    }

    confirmDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Hapus pelanggan?") },
            text = { Text("\"${c.name}\" akan dihapus permanen.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        Db.get(ctx).customerDao().delete(c)
                        confirmDelete = null
                        reload()
                    }
                }) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("Batal") }
            }
        )
    }

    if (showBroadcast) {
        var message by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showBroadcast = false },
            title = { Text("Broadcast Promo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Pesan dibagikan via WhatsApp. Daftar nomor yang bersedia " +
                            "terima promo bisa disalin terpisah.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    AppField(
                        message, { message = it }, "Isi pesan promo",
                        singleLine = false
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, message)
                        }
                        ctx.startActivity(Intent.createChooser(send, "Bagikan via WhatsApp"))
                        showBroadcast = false
                    },
                    enabled = message.isNotBlank()
                ) {
                    Icon(Icons.Filled.Share, null)
                    Spacer(Modifier.padding(4.dp))
                    Text("Bagikan")
                }
            },
            dismissButton = {
                Column {
                    TextButton(onClick = {
                        scope.launch {
                            val phones = Db.get(ctx).customerDao().consenting().map { it.phone }
                            if (phones.isEmpty()) {
                                toast(ctx, "Belum ada pelanggan yang bersedia terima promo")
                            } else {
                                copyToClipboard(ctx, phones.joinToString(", "), "Daftar nomor")
                            }
                        }
                    }) { Text("Salin daftar nomor") }
                    TextButton(onClick = { showBroadcast = false }) { Text("Tutup") }
                }
            }
        )
    }
}
