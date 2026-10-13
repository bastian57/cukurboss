package com.daftech.cukurboss.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.daftech.cukurboss.Session
import com.daftech.cukurboss.data.Db
import kotlinx.coroutines.launch

/** Graf navigasi utama CukurBoss. */
@Composable
fun AppNav() {
    val nav = rememberNavController()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    /** Setelah auth selesai: ke setup bila profil usaha belum ada, else main.
     *  Backstack dibersihkan total supaya tombol back keluar aplikasi. */
    fun afterAuth() {
        scope.launch {
            val dest = if (Db.get(ctx).businessDao().get() == null) "setup" else "main"
            nav.navigate(dest) {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    NavHost(navController = nav, startDestination = "splash") {
        composable("splash") {
            SplashScreen(onDone = { dest ->
                nav.navigate(dest) {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }
        composable("license") {
            LicenseScreen(onActivated = {
                nav.navigate("splash") {
                    popUpTo("license") { inclusive = true }
                }
            })
        }
        composable("register") {
            RegisterScreen(onDone = { afterAuth() })
        }
        composable("login") {
            LoginScreen(
                onLoggedIn = { afterAuth() },
                onForgot = { nav.navigate("forgot") }
            )
        }
        composable("forgot") {
            ForgotScreen(onDone = { nav.popBackStack() })
        }
        composable("setup") {
            SetupScreen(
                onSetupDone = {
                    nav.navigate("main") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                onBack = { nav.popBackStack() }
            )
        }
        composable("main") {
            MainScreen(
                onReceipt = { saleId -> nav.navigate("receipt/$saleId") },
                onEditBusiness = { nav.navigate("setup") },
                onLoggedOut = {
                    nav.navigate("splash") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = "receipt/{saleId}",
            arguments = listOf(navArgument("saleId") { type = NavType.LongType })
        ) { entry ->
            val saleId = entry.arguments?.getLong("saleId") ?: 0L
            ReceiptScreen(saleId = saleId, onBack = { nav.popBackStack() })
        }
    }
}
