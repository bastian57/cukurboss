package com.daftech.cukurboss.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.daftech.cukurboss.Session
import com.daftech.cukurboss.data.Db
import com.daftech.cukurboss.util.Crypto
import com.daftech.cukurboss.util.Fmt
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoggedIn: () -> Unit, onForgot: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var phone by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BrandLogo(size = 170.dp)
        Spacer(Modifier.height(16.dp))
        AppField(phone, { phone = Fmt.digitsOnly(it) }, "No. HP", keyboard = KeyboardType.Phone)
        Spacer(Modifier.height(8.dp))
        AppField(pw, { pw = it }, "Password", password = true)
        if (err.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(err, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton("Masuk") {
            scope.launch {
                val u = Db.get(ctx).userDao().get(phone)
                if (u == null || Crypto.hashPassword(pw, u.salt) != u.hash) {
                    err = "No. HP atau password salah"
                } else {
                    Session.login(ctx, phone)
                    onLoggedIn()
                }
            }
        }
        TextButton(onClick = onForgot) { Text("Lupa password?") }
        Spacer(Modifier.height(24.dp))
        Watermark()
    }
}
