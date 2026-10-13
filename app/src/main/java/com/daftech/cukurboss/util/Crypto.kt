package com.daftech.cukurboss.util

import java.security.MessageDigest
import java.security.SecureRandom

/** Hash password & PIN: SHA-256(salt + secret), hex. Tidak ada plaintext. */
object Crypto {
    fun newSalt(): String {
        val b = ByteArray(16)
        SecureRandom().nextBytes(b)
        return b.joinToString("") { "%02x".format(it) }
    }

    fun sha256Hex(s: String): String {
        val d = MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8))
        return d.joinToString("") { "%02x".format(it) }
    }

    fun hashPassword(password: String, salt: String): String = sha256Hex(salt + password)
}
