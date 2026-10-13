package com.daftech.cukurboss.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daftech.cukurboss.data.Db
import com.daftech.cukurboss.data.entity.BusinessProfile
import com.daftech.cukurboss.qr.QrBitmap
import com.daftech.cukurboss.util.Fmt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val BANKS = listOf("BCA", "BRI", "Mandiri", "BNI", "Lainnya")

/**
 * Isi / ubah profil usaha. Mode edit terdeteksi otomatis bila profil sudah ada.
 * onSetupDone: dipanggil setelah simpan saat setup pertama kali.
 * onBack: tombol kembali (hanya tampil di mode edit).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(onSetupDone: () -> Unit, onBack: (() -> Unit)? = null) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(false) }
    var isEdit by remember { mutableStateOf(false) }
    var existing by remember { mutableStateOf<BusinessProfile?>(null) }

    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var logoPath by remember { mutableStateOf("") }
    var bank by remember { mutableStateOf("BCA") }
    var bankExpanded by remember { mutableStateOf(false) }
    var accountNo by remember { mutableStateOf("") }
    var accountName by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val b = Db.get(ctx).businessDao().get()
        if (b != null) {
            isEdit = true
            existing = b
            name = b.name
            address = b.address
            logoPath = b.logoPath
            bank = b.bank.ifBlank { "BCA" }
            accountNo = b.accountNo
            accountName = b.accountName
        }
        loaded = true
    }

    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            try {
                val dir = File(ctx.filesDir, "logos").apply { mkdirs() }
                val dst = File(dir, "logo_${System.currentTimeMillis()}.jpg")
                ctx.contentResolver.openInputStream(uri)?.use { ins ->
                    dst.outputStream().use { out -> ins.copyTo(out) }
                }
                withContext(Dispatchers.Main) { logoPath = dst.absolutePath }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { toast(ctx, "Gagal menyimpan logo") }
            }
        }
    }

    val qrPayload = "CUKURBOSS TRANSFER\nBank: $bank\nNo. Rek: $accountNo\nA.n: $accountName"
    val qrBmp = remember(qrPayload) { QrBitmap.generate(qrPayload, 480)?.asImageBitmap() }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(if (isEdit) "Ubah Profil Usaha" else "Setup Usaha", onBack = if (isEdit) onBack else null)
        if (!loaded) return@Column
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isEdit) {
                BrandLogo(size = 120.dp)
                Spacer(Modifier.height(4.dp))
            }
            AppField(name, { name = it }, "Nama usaha")
            AppField(address, { address = it }, "Alamat")

            Row(verticalAlignment = Alignment.CenterVertically) {
                val bmp = remember(logoPath) {
                    if (logoPath.isNotBlank()) {
                        try { BitmapFactory.decodeFile(logoPath)?.asImageBitmap() } catch (_: Exception) { null }
                    } else null
                }
                if (bmp != null) {
                    Image(bmp, "Logo", Modifier.size(72.dp))
                } else {
                    Text("Belum ada logo", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = { logoPicker.launch("image/*") }) { Text("Pilih Logo") }
            }

            ExposedDropdownMenuBox(
                expanded = bankExpanded,
                onExpandedChange = { bankExpanded = it }
            ) {
                OutlinedTextField(
                    value = bank,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Bank") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(bankExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = bankExpanded,
                    onDismissRequest = { bankExpanded = false }
                ) {
                    BANKS.forEach { b ->
                        DropdownMenuItem(
                            text = { Text(b) },
                            onClick = { bank = b; bankExpanded = false }
                        )
                    }
                }
            }

            AppField(accountNo, { accountNo = Fmt.digitsOnly(it) }, "No. rekening", keyboard = androidx.compose.ui.text.input.KeyboardType.Number)
            AppField(accountName, { accountName = it }, "Nama pemilik rekening")

            Text("Preview QR transfer:", fontWeight = FontWeight.Bold)
            if (qrBmp != null) {
                Image(qrBmp, "QR Rekening", Modifier.size(200.dp).align(Alignment.CenterHorizontally))
            }

            if (err.isNotEmpty()) Text(err, color = MaterialTheme.colorScheme.error)
            PrimaryButton(if (isEdit) "Simpan Perubahan" else "Simpan & Mulai") {
                err = when {
                    name.isBlank() -> "Nama usaha wajib diisi"
                    accountNo.isNotBlank() && accountNo.length < 8 -> "No. rekening tidak valid"
                    else -> ""
                }
                if (err.isNotEmpty()) return@PrimaryButton
                scope.launch {
                    Db.get(ctx).businessDao().upsert(
                        BusinessProfile(
                            id = 1,
                            name = name.trim(),
                            address = address.trim(),
                            logoPath = logoPath,
                            bank = bank,
                            accountNo = accountNo,
                            accountName = accountName.trim(),
                            printerMac = existing?.printerMac ?: ""
                        )
                    )
                    toast(ctx, "Profil usaha tersimpan")
                    if (isEdit) onBack?.invoke() ?: onSetupDone() else onSetupDone()
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
