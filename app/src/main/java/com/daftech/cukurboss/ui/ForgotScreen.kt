package com.daftech.cukurboss.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.daftech.cukurboss.data.Db
import com.daftech.cukurboss.util.Crypto
import com.daftech.cukurboss.util.Fmt
import kotlinx.coroutines.launch

/** Reset password memakai PIN darurat (tetap offline). */
@Composable
fun ForgotScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var phone by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }
    var pw2 by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("Lupa Password", onBack = onDone)
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text("Masukkan no. HP dan PIN darurat untuk membuat password baru.")
            Spacer(Modifier.height(16.dp))
            AppField(phone, { phone = Fmt.digitsOnly(it) }, "No. HP", keyboard = KeyboardType.Phone)
            Spacer(Modifier.height(8.dp))
            AppField(pin, { pin = Fmt.digitsOnly(it).take(6) }, "PIN darurat (6 digit)", keyboard = KeyboardType.NumberPassword, password = true)
            Spacer(Modifier.height(8.dp))
            AppField(pw, { pw = it }, "Password baru", password = true)
            Spacer(Modifier.height(8.dp))
            AppField(pw2, { pw2 = it }, "Konfirmasi password baru", password = true)
            if (err.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(err, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(12.dp))
            PrimaryButton("Reset Password") {
                scope.launch {
                    val u = Db.get(ctx).userDao().get(phone)
                    err = when {
                        u == null -> "No. HP tidak terdaftar"
                        Crypto.hashPassword(pin, u.salt) != u.pinHash -> "PIN darurat salah"
                        pw.length < 4 -> "Password minimal 4 karakter"
                        pw != pw2 -> "Konfirmasi password tidak sama"
                        else -> ""
                    }
                    if (err.isNotEmpty()) return@launch
                    val user = u!!
                    Db.get(ctx).userDao().update(user.copy(hash = Crypto.hashPassword(pw, user.salt)))
                    toast(ctx, "Password berhasil direset. Silakan login.")
                    onDone()
                }
            }
        }
    }
}
