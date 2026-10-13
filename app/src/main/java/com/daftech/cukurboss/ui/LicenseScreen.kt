package com.daftech.cukurboss.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daftech.cukurboss.license.LicenseManager
import com.daftech.cukurboss.license.LicenseResult

@Composable
fun LicenseScreen(onActivated: () -> Unit) {
    val ctx = LocalContext.current
    val deviceId = remember { LicenseManager.getDeviceId(ctx) }
    var code by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BrandLogo(size = 130.dp)
        Spacer(Modifier.height(12.dp))
        Text("Aktivasi CukurBoss", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text("Device ID perangkat ini (ketuk ikon untuk salin):")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                deviceId,
                Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = { copyToClipboard(ctx, deviceId, "Device ID") }) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Salin Device ID")
            }
        }
        Spacer(Modifier.height(8.dp))
        AppField(value = code, onChange = { code = it; err = "" }, label = "Kode aktivasi (CB1-...)")
        if (err.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(err, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton("Aktivasi") {
            when (val r = LicenseManager.validateLicense(code, deviceId)) {
                is LicenseResult.Valid -> {
                    val prefs = ctx.getSharedPreferences(LicenseManager.PREFS_NAME, Context.MODE_PRIVATE)
                    LicenseManager.saveLicense(prefs, code, r.buyerName, r.expiry)
                    toast(ctx, "Aktivasi berhasil. Selamat datang, ${r.buyerName}!")
                    onActivated()
                }
                is LicenseResult.Invalid -> err = r.reason
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Minta kode aktivasi ke DAFTECH.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
