package com.daftech.cukurboss.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daftech.cukurboss.Session
import com.daftech.cukurboss.data.Db
import com.daftech.cukurboss.license.LicenseManager

/** Cek berurutan: lisensi → user → sesi login → profil usaha → main. */
@Composable
fun SplashScreen(onDone: (String) -> Unit) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        Db.ensureSeeded(ctx)
        val licPrefs = ctx.getSharedPreferences(LicenseManager.PREFS_NAME, Context.MODE_PRIVATE)
        val dest = when {
            !LicenseManager.isActivated(licPrefs) -> "license"
            Db.get(ctx).userDao().count() == 0 -> "register"
            Session.currentPhone(ctx) == null -> "login"
            Db.get(ctx).businessDao().get() == null -> "setup"
            else -> "main"
        }
        onDone(dest)
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "CukurBoss",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}
