package com.nuvio.app.teldrive

import android.content.Context
import android.content.SharedPreferences

object TelDriveConfigRepository {
    private const val PREFS_NAME = "TelDrivePrefs"
    
    private const val KEY_DSN = "supabase_dsn"
    private const val KEY_JWT = "jwt_secret"
    private const val KEY_PORT = "server_port"
    private const val KEY_APP_ID = "tg_app_id"
    private const val KEY_APP_HASH = "tg_app_hash"
    private const val KEY_ENCRYPTION_KEY = "tg_uploads_encryption_key"
    private const val KEY_FIRST_LAUNCH = "is_first_launch"

    private lateinit var prefs: SharedPreferences

    fun initialize(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_FIRST_LAUNCH, true)) {
            // Populate defaults from Gradle buildConfig variables on first launch
            prefs.edit().apply {
                putString(KEY_DSN, BuildConfig.DEFAULT_SUPABASE_DSN)
                putString(KEY_JWT, BuildConfig.DEFAULT_JWT_SECRET)
                putInt(KEY_PORT, BuildConfig.DEFAULT_PORT.toIntOrNull() ?: 8080)
                putString(KEY_APP_ID, BuildConfig.DEFAULT_TG_APP_ID)
                putString(KEY_APP_HASH, BuildConfig.DEFAULT_TG_APP_HASH)
                putString(KEY_ENCRYPTION_KEY, BuildConfig.DEFAULT_TG_UPLOADS_ENCRYPTION_KEY)
                putBoolean(KEY_FIRST_LAUNCH, false)
                apply()
            }
        }
    }

    var supabaseDsn: String
        get() = prefs.getString(KEY_DSN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DSN, value.trim()).apply()

    var jwtSecret: String
        get() = prefs.getString(KEY_JWT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_JWT, value.trim()).apply()

    var port: Int
        get() = prefs.getInt(KEY_PORT, 8080)
        set(value) = prefs.edit().putInt(KEY_PORT, value).apply()

    var tgAppId: String
        get() = prefs.getString(KEY_APP_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_APP_ID, value.trim()).apply()

    var tgAppHash: String
        get() = prefs.getString(KEY_APP_HASH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_APP_HASH, value.trim()).apply()

    var tgUploadsEncryptionKey: String
        get() = prefs.getString(KEY_ENCRYPTION_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ENCRYPTION_KEY, value.trim()).apply()

    fun isConfigured(): Boolean {
        return supabaseDsn.isNotBlank() && 
               jwtSecret.isNotBlank() && 
               tgAppId.isNotBlank() && 
               tgAppHash.isNotBlank()
    }
}
