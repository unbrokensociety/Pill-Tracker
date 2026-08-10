package com.example.util

import java.security.MessageDigest

object PasswordHasher {
    private const val SALT = "PillTrackerSalt_v1"

    fun hashPassword(password: String): String {
        if (password.isEmpty()) return ""
        val bytes = (password + SALT).toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
