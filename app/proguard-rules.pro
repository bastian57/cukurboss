# CukurBoss ProGuard rules
-keep class com.daftech.cukurboss.data.entity.** { *; }
-keep class com.daftech.cukurboss.data.dao.** { *; }
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keepnames class * extends androidx.room.RoomDatabase
-keep class com.daftech.cukurboss.license.LicenseManager { *; }
-keep class com.daftech.cukurboss.print.** { *; }
-keep class com.daftech.cukurboss.qr.** { *; }
