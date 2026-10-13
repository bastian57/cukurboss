package com.daftech.cukurboss.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.daftech.cukurboss.data.entity.BusinessProfile
import com.daftech.cukurboss.data.entity.Customer
import com.daftech.cukurboss.data.entity.Product
import com.daftech.cukurboss.data.entity.Sale
import com.daftech.cukurboss.data.entity.SaleItem
import com.daftech.cukurboss.data.entity.User

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(u: User)

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun get(phone: String): User?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int

    @Update
    suspend fun update(u: User)
}

@Dao
interface BusinessDao {
    @Query("SELECT * FROM business WHERE id = 1")
    suspend fun get(): BusinessProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(b: BusinessProfile)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE active = 1 ORDER BY type, name")
    suspend fun active(): List<Product>

    @Query("SELECT * FROM products ORDER BY type, name")
    suspend fun all(): List<Product>

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int

    @Insert
    suspend fun insert(p: Product): Long

    @Update
    suspend fun update(p: Product)

    @Delete
    suspend fun delete(p: Product)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    suspend fun all(): List<Customer>

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :q || '%' OR phone LIKE '%' || :q || '%' ORDER BY name ASC")
    suspend fun search(q: String): List<Customer>

    @Query("SELECT * FROM customers WHERE promoConsent = 1 ORDER BY name ASC")
    suspend fun consenting(): List<Customer>

    @Insert
    suspend fun insert(c: Customer): Long

    @Update
    suspend fun update(c: Customer)

    @Delete
    suspend fun delete(c: Customer)
}

/** Agregat item terlaris. */
data class TopItem(
    val productName: String,
    val qty: Int
)

@Dao
interface SaleDao {
    @Insert
    suspend fun insert(s: Sale): Long

    @Update
    suspend fun update(s: Sale)

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun get(id: Long): Sale?

    @Query("SELECT * FROM sales WHERE createdAt BETWEEN :from AND :to ORDER BY createdAt DESC")
    suspend fun listByDate(from: Long, to: Long): List<Sale>

    @Query("SELECT IFNULL(SUM(total),0) FROM sales WHERE createdAt BETWEEN :from AND :to AND status = 'LUNAS'")
    suspend fun sumOmzet(from: Long, to: Long): Long

    @Query("SELECT COUNT(*) FROM sales WHERE createdAt BETWEEN :from AND :to")
    suspend fun countSales(from: Long, to: Long): Int

    @Query("SELECT IFNULL(SUM(total),0) FROM sales WHERE createdAt BETWEEN :from AND :to AND paymentMethod = 'TUNAI' AND status = 'LUNAS'")
    suspend fun sumTunai(from: Long, to: Long): Long

    @Query("SELECT IFNULL(SUM(total),0) FROM sales WHERE createdAt BETWEEN :from AND :to AND paymentMethod = 'TRANSFER' AND status = 'LUNAS'")
    suspend fun sumTransfer(from: Long, to: Long): Long

    @Query("SELECT IFNULL(SUM(total),0) FROM sales WHERE createdAt BETWEEN :from AND :to AND status = 'BELUM_BAYAR'")
    suspend fun sumUnpaid(from: Long, to: Long): Long

    @Query(
        "SELECT productName, SUM(qty) AS qty FROM sale_items " +
            "WHERE saleId IN (SELECT id FROM sales WHERE createdAt BETWEEN :from AND :to) " +
            "GROUP BY productName ORDER BY qty DESC"
    )
    suspend fun topItems(from: Long, to: Long): List<TopItem>
}

@Dao
interface SaleItemDao {
    @Insert
    suspend fun insertAll(items: List<SaleItem>)

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun bySale(saleId: Long): List<SaleItem>
}
