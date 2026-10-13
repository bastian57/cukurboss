package com.daftech.cukurboss.print

import android.bluetooth.BluetoothManager
import android.content.Context
import com.daftech.cukurboss.util.Fmt
import java.io.ByteArrayOutputStream
import java.util.UUID

/** Satu item di struk. */
data class ReceiptItem(
    val name: String,
    val qty: Int,
    val price: Long
)

/** Data struk CukurBoss. */
data class ReceiptData(
    val businessName: String,
    val address: String,
    val datetime: String,
    val cashier: String,
    val customerName: String,
    val items: List<ReceiptItem>,
    val total: Long,
    val paymentMethod: String, // TUNAI | TRANSFER
    val status: String, // LUNAS | BELUM_BAYAR
    val paid: Long,
    val change: Long
)

/**
 * Cetak struk ke printer thermal 58mm via Bluetooth SPP (RFCOMM).
 * ESC/POS: init, align, teks 32 kolom, feed + cut.
 */
object EscPosPrinter {
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private const val COLS = 32

    fun print(context: Context, mac: String, data: ByteArray): Result<Unit> {
        return try {
            val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bm?.adapter ?: return Result.failure(Exception("Bluetooth tidak tersedia"))
            if (!adapter.isEnabled) return Result.failure(Exception("Bluetooth mati"))
            try { adapter.cancelDiscovery() } catch (_: SecurityException) { /* abaikan */ }
            val device = try {
                adapter.getRemoteDevice(mac)
            } catch (e: IllegalArgumentException) {
                return Result.failure(Exception("Alamat printer tidak valid"))
            } catch (e: SecurityException) {
                return Result.failure(e)
            }
            val socket = try {
                device.createRfcommSocketToServiceRecord(SPP_UUID)
            } catch (e: SecurityException) {
                return Result.failure(e)
            } catch (e: Exception) {
                return Result.failure(e)
            }
            try {
                socket.connect()
                socket.outputStream.use { it.write(data); it.flush() }
                Thread.sleep(400)
            } finally {
                try { socket.close() } catch (_: Exception) { }
            }
            Result.success(Unit)
        } catch (e: SecurityException) {
            Result.failure(Exception("Izin Bluetooth ditolak"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Bangun byte ESC/POS struk penjualan. */
    fun buildReceipt(d: ReceiptData): ByteArray {
        val out = ByteArrayOutputStream()
        fun raw(vararg bs: Int) = bs.forEach { out.write(it) }
        fun txt(s: String) = out.write(san(s).toByteArray(Charsets.US_ASCII))
        fun nl() = raw(0x0A)
        fun center() = raw(0x1B, 0x61, 0x01)
        fun left() = raw(0x1B, 0x61, 0x00)
        fun bold(on: Boolean) = raw(0x1B, 0x45, if (on) 1 else 0)
        fun big(on: Boolean) = raw(0x1D, 0x21, if (on) 0x11 else 0x00)
        fun row(l: String, r: String) {
            val rr = if (r.length > COLS) r.take(COLS) else r
            var ll = l
            if (ll.length + rr.length > COLS) ll = ll.take(COLS - rr.length)
            txt(ll + " ".repeat(COLS - ll.length - rr.length) + rr); nl()
        }
        fun dash() { txt("-".repeat(COLS)); nl() }

        raw(0x1B, 0x40) // init
        center(); bold(true); big(true)
        txt(d.businessName.ifBlank { "CukurBoss" }.take(20)); nl()
        big(false); bold(false)
        if (d.address.isNotBlank()) { txt(d.address.take(COLS)); nl() }
        dash()
        left()
        row("Tgl", d.datetime.take(20))
        row("Kasir", d.cashier.take(20))
        if (d.customerName.isNotBlank()) row("Pelanggan", d.customerName.take(18))
        dash()
        for (it in d.items) {
            row("${it.name} x${it.qty}", Fmt.rp(it.price * it.qty))
        }
        dash()
        bold(true); row("TOTAL", Fmt.rp(d.total)); bold(false)
        row("Metode", d.paymentMethod)
        if (d.paymentMethod == "TUNAI") {
            row("Bayar", Fmt.rp(d.paid))
            row("Kembali", Fmt.rp(d.change))
        }
        nl()
        center(); bold(true)
        txt(if (d.status == "LUNAS") "** LUNAS **" else "** BELUM BAYAR **"); nl()
        bold(false)
        txt("Terima kasih."); nl()
        txt("apk by DAFTECH"); nl()
        left()
        raw(0x1B, 0x64, 0x04) // feed 4
        raw(0x1D, 0x56, 0x00) // cut
        return out.toByteArray()
    }

    /** Versi teks biasa: untuk preview di layar & dibagikan via WhatsApp. */
    fun buildTextReceipt(d: ReceiptData): String {
        val sb = StringBuilder()
        sb.appendLine("*${d.businessName.ifBlank { "CukurBoss" }}*")
        if (d.address.isNotBlank()) sb.appendLine(d.address)
        sb.appendLine("--------------------------------")
        sb.appendLine("Tgl      : ${d.datetime}")
        sb.appendLine("Kasir    : ${d.cashier}")
        if (d.customerName.isNotBlank()) sb.appendLine("Pelanggan: ${d.customerName}")
        sb.appendLine("--------------------------------")
        for (it in d.items) {
            sb.appendLine("${it.name} x${it.qty}: ${Fmt.rp(it.price * it.qty)}")
        }
        sb.appendLine("--------------------------------")
        sb.appendLine("*TOTAL: ${Fmt.rp(d.total)}*")
        sb.appendLine("Metode: ${d.paymentMethod}")
        if (d.paymentMethod == "TUNAI") {
            sb.appendLine("Bayar: ${Fmt.rp(d.paid)}")
            sb.appendLine("Kembali: ${Fmt.rp(d.change)}")
        }
        sb.appendLine("Status: ${if (d.status == "LUNAS") "LUNAS" else "BELUM BAYAR"}")
        sb.appendLine("Terima kasih 🙏")
        sb.appendLine("apk by DAFTECH")
        return sb.toString()
    }

    /** Hanya karakter ASCII yang aman untuk printer thermal. */
    private fun san(s: String): String {
        val sb = StringBuilder()
        for (ch in s) {
            when (ch) {
                '³' -> sb.append('3')
                '•', '·' -> sb.append('-')
                '×' -> sb.append('x')
                '−', '–', '—' -> sb.append('-')
                '★' -> sb.append('*')
                '‹', '›', '«', '»' -> {}
                else -> if (ch.code in 32..126) sb.append(ch) else sb.append('?')
            }
        }
        return sb.toString()
    }
}
