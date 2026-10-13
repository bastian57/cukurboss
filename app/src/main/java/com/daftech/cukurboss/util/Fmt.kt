package com.daftech.cukurboss.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Format tampilan: rupiah, tanggal, dsb. */
object Fmt {
    private val id = Locale("in", "ID")

    /** 25000 -> "Rp25.000" */
    fun rp(v: Long): String {
        val nf = NumberFormat.getNumberInstance(id)
        return "Rp" + nf.format(v)
    }

    fun dateTimeStr(ts: Long): String =
        SimpleDateFormat("dd MMM yyyy HH:mm", id).format(Date(ts))

    fun dateStr(ts: Long): String =
        SimpleDateFormat("dd MMM yyyy", id).format(Date(ts))

    fun fileStamp(): String =
        SimpleDateFormat("yyyyMMdd-HHmmss", id).format(Date())

    fun digitsOnly(s: String): String = s.filter { it.isDigit() }
}
