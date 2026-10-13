package com.daftech.cukurboss.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Akun kasir/pemilik. phone = PK. Password & PIN disimpan sebagai hash. */
@Entity(tableName = "users")
data class User(
    @PrimaryKey val phone: String,
    val name: String,
    val salt: String,
    val hash: String,
    val pinHash: String
)

/** Profil usaha — satu baris (id = 1). */
@Entity(tableName = "business")
data class BusinessProfile(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val address: String,
    val logoPath: String = "",
    val bank: String = "",
    val accountNo: String = "",
    val accountName: String = "",
    val printerMac: String = ""
)

/** Layanan / produk jual. type = "LAYANAN" | "PRODUK". */
@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val price: Long,
    val type: String,
    val active: Boolean = true
)

/** Pelanggan untuk kebutuhan broadcast promo. */
@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val promoConsent: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Transaksi penjualan.
 * paymentMethod = "TUNAI" | "TRANSFER"
 * status = "LUNAS" | "BELUM_BAYAR"
 */
@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val cashierPhone: String,
    val cashierName: String = "",
    val customerId: Long? = null,
    val customerName: String = "",
    val paymentMethod: String,
    val status: String,
    val total: Long,
    val paidAmount: Long = 0,
    val changeAmount: Long = 0
)

/** Item dalam satu transaksi (snapshot nama & harga saat jual). */
@Entity(tableName = "sale_items")
data class SaleItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val productName: String,
    val price: Long,
    val qty: Int
)
