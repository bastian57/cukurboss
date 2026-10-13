package com.daftech.cukurboss.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.daftech.cukurboss.data.entity.Product

/** Singleton database + seed produk bawaan. */
object Db {
    @Volatile
    private var inst: AppDatabase? = null

    fun get(ctx: Context): AppDatabase = inst ?: synchronized(this) {
        inst ?: Room.databaseBuilder(
            ctx.applicationContext,
            AppDatabase::class.java,
            "cukurboss.db"
        )
            // Journal TRUNCATE = database 1 file (memudahkan backup copy).
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .fallbackToDestructiveMigration()
            .build()
            .also { inst = it }
    }

    /** Isi produk default saat pertama kali (hanya bila tabel kosong). */
    suspend fun ensureSeeded(ctx: Context) {
        val dao = get(ctx).productDao()
        if (dao.count() > 0) return
        val seeds = listOf(
            Product(name = "Potong Rambut Dewasa", price = 25000, type = "LAYANAN"),
            Product(name = "Potong Rambut Anak", price = 15000, type = "LAYANAN"),
            Product(name = "Cukur Jenggot/Kumis", price = 10000, type = "LAYANAN"),
            Product(name = "Creambath", price = 30000, type = "LAYANAN"),
            Product(name = "Semir Rambut", price = 35000, type = "LAYANAN"),
            Product(name = "Pomade", price = 50000, type = "PRODUK")
        )
        seeds.forEach { dao.insert(it) }
    }
}
