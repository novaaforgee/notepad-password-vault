package com.notepadvault.app

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom

class SecurityManager(private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "vault_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_PASSWORD_HASH = "vault_pwd_hash"
        private const val KEY_SALT = "vault_salt"
        private const val KEY_NOTES_DATA = "vault_encrypted_notes"
    }

    fun isPasswordSet(): Boolean {
        return sharedPreferences.contains(KEY_PASSWORD_HASH)
    }

    fun setPassword(password: String) {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val saltHex = bytesToHex(salt)
        val hash = hashPassword(password, saltHex)

        sharedPreferences.edit()
            .putString(KEY_SALT, saltHex)
            .putString(KEY_PASSWORD_HASH, hash)
            .apply()
    }

    fun verifyPassword(password: String): Boolean {
        val savedHash = sharedPreferences.getString(KEY_PASSWORD_HASH, null) ?: return false
        val salt = sharedPreferences.getString(KEY_SALT, "") ?: ""
        val computedHash = hashPassword(password, salt)
        return savedHash == computedHash
    }

    fun changePassword(oldPass: String, newPass: String): Boolean {
        if (!verifyPassword(oldPass)) return false
        setPassword(newPass)
        return true
    }

    private fun hashPassword(password: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val input = (salt + password).toByteArray(Charsets.UTF_8)
        val digest = md.digest(input)
        return bytesToHex(digest)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    fun saveNotes(notes: List<Note>) {
        val jsonArray = JSONArray()
        for (note in notes) {
            val obj = JSONObject()
            obj.put("id", note.id)
            obj.put("title", note.title)
            obj.put("content", note.content)
            obj.put("updatedAt", note.updatedAt)
            obj.put("category", note.category)
            jsonArray.put(obj)
        }
        sharedPreferences.edit()
            .putString(KEY_NOTES_DATA, jsonArray.toString())
            .apply()
    }

    fun loadNotes(): List<Note> {
        val raw = sharedPreferences.getString(KEY_NOTES_DATA, null) ?: return emptyList()
        val list = mutableListOf<Note>()
        try {
            val jsonArray = JSONArray(raw)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    Note(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        content = obj.getString("content"),
                        updatedAt = obj.getLong("updatedAt"),
                        category = obj.optString("category", "General")
                    )
                )
            }
        } catch (_: Exception) { }
        return list
    }
}
