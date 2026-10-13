package com.daftech.cukurboss.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import com.daftech.cukurboss.data.entity.Product
import com.daftech.cukurboss.util.Fmt
import kotlinx.coroutines.launch

private val TYPES = listOf("LAYANAN", "PRODUK")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProdukScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf(listOf<Product>()) }
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Product?>(null) }
    var confirmDelete by remember { mutableStateOf<Product?>(null) }

    fun reload() {
        scope.launch { list = Db.get(ctx).productDao().all() }
    }
    LaunchedEffect(Unit) { reload() }

    Scaffold(
        topBar = { AppTopBar("Produk & Layanan") },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah")
            }
        }
    ) { _ ->
        LazyColumn(Modifier.fillMaxSize()) {
            items(list, key = { it.id }) { p ->
                ListItem(
                    headlineContent = { Text(p.name) },
                    supportingContent = { Text("${p.type} • ${Fmt.rp(p.price)}") },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = p.active,
                                onCheckedChange = {
                                    scope.launch {
                                        Db.get(ctx).productDao().update(p.copy(active = it))
                                        reload()
                                    }
                                }
                            )
                            IconButton(onClick = { confirmDelete = p }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Hapus")
                            }
                        }
                    },
                    modifier = Modifier.clickable { editing = p; showDialog = true }
                )
                HorizontalDivider()
            }
        }
    }

    if (showDialog) {
        var name by remember(editing) { mutableStateOf(editing?.name ?: "") }
        var priceStr by remember(editing) { mutableStateOf(editing?.price?.toString() ?: "") }
        var type by remember(editing) { mutableStateOf(editing?.type ?: "LAYANAN") }
        var typeExpanded by remember { mutableStateOf(false) }
        var err by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editing == null) "Tambah Produk" else "Ubah Produk") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppField(name, { name = it }, "Nama")
                    AppField(
                        priceStr, { priceStr = Fmt.digitsOnly(it) }, "Harga (Rp)",
                        keyboard = KeyboardType.Number
                    )
                    ExposedDropdownMenuBox(
                        expanded = typeExpanded,
                        onExpandedChange = { typeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = type, onValueChange = {}, readOnly = true,
                            label = { Text("Tipe") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false }
                        ) {
                            TYPES.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t) },
                                    onClick = { type = t; typeExpanded = false }
                                )
                            }
                        }
                    }
                    if (err.isNotEmpty()) Text(err, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val price = priceStr.toLongOrNull() ?: 0L
                    err = when {
                        name.isBlank() -> "Nama wajib diisi"
                        price <= 0 -> "Harga harus lebih dari 0"
                        else -> ""
                    }
                    if (err.isNotEmpty()) return@TextButton
                    scope.launch {
                        val dao = Db.get(ctx).productDao()
                        val e = editing
                        if (e == null) {
                            dao.insert(Product(name = name.trim(), price = price, type = type))
                        } else {
                            dao.update(e.copy(name = name.trim(), price = price, type = type))
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

    confirmDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Hapus produk?") },
            text = { Text("\"${p.name}\" akan dihapus permanen.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        Db.get(ctx).productDao().delete(p)
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
}
