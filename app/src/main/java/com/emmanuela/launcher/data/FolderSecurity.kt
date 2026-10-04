package com.emmanuela.launcher.data

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object FolderPassword {
    private const val ITERATIONS = 120_000
    fun create(password: String): Pair<String, String> {
        require(password.length in 4..128)
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return Base64.getEncoder().encodeToString(salt) to Base64.getEncoder().encodeToString(derive(password, salt))
    }
    fun matches(password: String, salt: String, hash: String): Boolean = runCatching {
        MessageDigest.isEqual(derive(password, Base64.getDecoder().decode(salt)), Base64.getDecoder().decode(hash))
    }.getOrDefault(false)
    private fun derive(password: String, salt: ByteArray): ByteArray {
        val key = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(key).encoded } finally { key.clearPassword() }
    }
}
object FolderPlacement {
    fun cells(folders: List<AppFolder>): Map<Int, AppFolder> {
        val placed = mutableMapOf<Int, AppFolder>()
        folders.filter { it.gridCell != null }.forEach { f -> if (!placed.containsKey(f.gridCell)) placed[f.gridCell!!] = f }
        folders.filter { f -> placed.values.none { it.id == f.id } }.forEach { f ->
            var cell = 0
            while (cell in placed) cell++
            placed[cell] = f
        }
        return placed
    }
}
