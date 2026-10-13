package com.daftech.cukurboss

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.daftech.cukurboss.ui.AppNav
import com.daftech.cukurboss.ui.theme.CukurBossTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CukurBossTheme {
                AppNav()
            }
        }
    }
}
