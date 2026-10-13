package com.daftech.cukurboss.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.daftech.cukurboss.data.dao.BusinessDao
import com.daftech.cukurboss.data.dao.CustomerDao
import com.daftech.cukurboss.data.dao.ProductDao
import com.daftech.cukurboss.data.dao.SaleDao
import com.daftech.cukurboss.data.dao.SaleItemDao
import com.daftech.cukurboss.data.dao.UserDao
import com.daftech.cukurboss.data.entity.BusinessProfile
import com.daftech.cukurboss.data.entity.Customer
import com.daftech.cukurboss.data.entity.Product
import com.daftech.cukurboss.data.entity.Sale
import com.daftech.cukurboss.data.entity.SaleItem
import com.daftech.cukurboss.data.entity.User

@Database(
    entities = [
        User::class,
        BusinessProfile::class,
        Product::class,
        Customer::class,
        Sale::class,
        SaleItem::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun businessDao(): BusinessDao
    abstract fun productDao(): ProductDao
    abstract fun customerDao(): CustomerDao
    abstract fun saleDao(): SaleDao
    abstract fun saleItemDao(): SaleItemDao
}
