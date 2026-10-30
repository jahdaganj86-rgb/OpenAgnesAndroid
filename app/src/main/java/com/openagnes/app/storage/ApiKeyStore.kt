package com.openagnes.app.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object ApiKeyStore {
    private const val PREFS_NAME = "openagnes_prefs"
    private const val KEY_API_KEY = "api_key"

    private var prefs: EncryptedSharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveApiKey(key: String): Boolean {
        return prefs?.edit()?.putString(KEY_API_KEY, key)?.apply { commit() } ?: false
    }

    fun getApiKey(): String? = prefs?.getString(KEY_API_KEY, null)

    fun clearApiKey(): Boolean {
        return prefs?.edit()?.remove(KEY_API_KEY)?.apply { commit() } ?: false
    }
}
