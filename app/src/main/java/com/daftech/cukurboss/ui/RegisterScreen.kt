package com.daftech.cukurboss.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.daftech.cukurboss.Session
import com.daftech.cukurboss.data.Db
import com.daftech.cukurboss.data.entity.User
import com.daftech.cukurboss.util.Crypto
import com.daftech.cukurboss.util.Fmt
import kotlinx.coroutines.launch

/** Pendaftaran akun pertama (pemilik). */
@Composable
fun RegisterScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }
    var pw2 by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BrandLogo(size = 130.dp)
        Spacer(Modifier.height(12.dp))
        Text("Daftar Akun Pemilik", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Akun ini dipakai untuk login kasir.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))
        AppField(name, { name = it }, "Nama")
        Spacer(Modifier.height(8.dp))
        AppField(phone, { phone = Fmt.digitsOnly(it) }, "No. HP", keyboard = KeyboardType.Phone)
        Spacer(Modifier.height(8.dp))
        AppField(pw, { pw = it }, "Password", password = true)
        Spacer(Modifier.height(8.dp))
        AppField(pw2, { pw2 = it }, "Konfirmasi password", password = true)
        Spacer(Modifier.height(8.dp))
        AppField(
            pin, { pin = Fmt.digitsOnly(it).take(6) }, "PIN darurat (6 digit)",
            keyboard = KeyboardType.NumberPassword, password = true,
            supporting = "Untuk reset password bila lupa. Jangan sampai hilang."
        )
        if (err.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(err, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton("Daftar & Masuk") {
            err = when {
                name.isBlank() -> "Nama wajib diisi"
                phone.length < 9 -> "No. HP tidak valid"
                pw.length < 4 -> "Password minimal 4 karakter"
                pw != pw2 -> "Konfirmasi password tidak sama"
                pin.length != 6 -> "PIN darurat harus 6 digit"
                else -> ""
            }
            if (err.isNotEmpty()) return@PrimaryButton
            scope.launch {
                val salt = Crypto.newSalt()
                Db.get(ctx).userDao().insert(
                    User(
                        phone = phone,
                        name = name.trim(),
                        salt = salt,
                        hash = Crypto.hashPassword(pw, salt),
                        pinHash = Crypto.hashPassword(pin, salt)
                    )
                )
                Session.login(ctx, phone)
                toast(ctx, "Selamat datang, ${name.trim()}!")
                onDone()
            }
        }
    }
}
