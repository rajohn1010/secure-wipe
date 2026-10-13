package com.example.wipe

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Stores only a salted PBKDF2 hash of the password, never the password itself. */
object PasswordStore {
    private const val PREFS = "wipe_prefs"
    private const val ITERATIONS = 150_000

    private fun hash(password: CharArray, salt: ByteArray): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(password, salt, ITERATIONS, 256)).encoded

    fun isSet(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains("hash")

    fun set(ctx: Context, password: CharArray) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("hash", Base64.encodeToString(hash(password, salt), Base64.NO_WRAP))
            .apply()
    }

    fun verify(ctx: Context, password: CharArray): Boolean {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val salt = Base64.decode(p.getString("salt", null) ?: return false, Base64.NO_WRAP)
        val expected = Base64.decode(p.getString("hash", null) ?: return false, Base64.NO_WRAP)
        return MessageDigest.isEqual(expected, hash(password, salt))
    }
}
