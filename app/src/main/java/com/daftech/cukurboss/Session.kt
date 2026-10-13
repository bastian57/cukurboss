package com.daftech.cukurboss

import android.content.Context

/** Sesi login sederhana: no HP yang sedang login. */
object Session {
    private const val PREFS = "cukurboss_session"
    private const val KEY_PHONE = "phone"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun login(ctx: Context, phone: String) {
        prefs(ctx).edit().putString(KEY_PHONE, phone).apply()
    }

    fun logout(ctx: Context) {
        prefs(ctx).edit().remove(KEY_PHONE).apply()
    }

    fun currentPhone(ctx: Context): String? = prefs(ctx).getString(KEY_PHONE, null)
}
