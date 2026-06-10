package com.example.data

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("StudyGroupPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LOGGED_IN_USER = "logged_in_user"
        private const val KEY_FB_PROJECT_ID = "fb_project_id"
        private const val KEY_FB_API_KEY = "fb_api_key"
        private const val KEY_FB_APP_ID = "fb_app_id"
        private const val KEY_FB_STORAGE_BUCKET = "fb_storage_bucket"

        private const val KEY_MONGO_APP_ID = "mongo_app_id"
        private const val KEY_MONGO_API_KEY = "mongo_api_key"
        private const val KEY_MONGO_DATASOURCE = "mongo_datasource"
        private const val KEY_MONGO_DATABASE = "mongo_database"

        private const val KEY_CLOUDINARY_CLOUD_NAME = "cloudinary_cloud_name"
        private const val KEY_CLOUDINARY_UPLOAD_PRESET = "cloudinary_upload_preset"
        private const val KEY_CLOUDINARY_API_KEY = "cloudinary_api_key"
        private const val KEY_CLOUDINARY_API_SECRET = "cloudinary_api_secret"
    }

    fun loginUser(username: String) {
        prefs.edit().putString(KEY_LOGGED_IN_USER, username.trim().lowercase()).apply()
    }

    fun getLoggedInUser(): String? {
        return prefs.getString(KEY_LOGGED_IN_USER, null)
    }

    fun logoutUser() {
        prefs.edit().remove(KEY_LOGGED_IN_USER).apply()
    }

    fun isLoggedIn(): Boolean {
        return getLoggedInUser() != null
    }

    fun setMongoConfig(appId: String, apiKey: String, datasource: String, database: String) {
        prefs.edit()
            .putString(KEY_MONGO_APP_ID, appId.trim())
            .putString(KEY_MONGO_API_KEY, apiKey.trim())
            .putString(KEY_MONGO_DATASOURCE, datasource.trim())
            .putString(KEY_MONGO_DATABASE, database.trim())
            .apply()
    }

    fun getMongoConfig(): Map<String, String>? {
        val appId = prefs.getString(KEY_MONGO_APP_ID, null)
        val apiKey = prefs.getString(KEY_MONGO_API_KEY, null)
        val datasource = prefs.getString(KEY_MONGO_DATASOURCE, "Cluster0") ?: "Cluster0"
        val database = prefs.getString(KEY_MONGO_DATABASE, "study_group_vault") ?: "study_group_vault"

        if (appId.isNullOrBlank() || apiKey.isNullOrBlank()) {
            return null
        }
        return mapOf(
            "appId" to appId,
            "apiKey" to apiKey,
            "datasource" to datasource,
            "database" to database
        )
    }

    fun clearMongoConfig() {
        prefs.edit()
            .remove(KEY_MONGO_APP_ID)
            .remove(KEY_MONGO_API_KEY)
            .remove(KEY_MONGO_DATASOURCE)
            .remove(KEY_MONGO_DATABASE)
            .apply()
    }

    fun setCloudinaryConfig(cloudName: String, uploadPreset: String, apiKey: String?, apiSecret: String?) {
        prefs.edit()
            .putString(KEY_CLOUDINARY_CLOUD_NAME, cloudName.trim())
            .putString(KEY_CLOUDINARY_UPLOAD_PRESET, uploadPreset.trim())
            .putString(KEY_CLOUDINARY_API_KEY, apiKey?.trim())
            .putString(KEY_CLOUDINARY_API_SECRET, apiSecret?.trim())
            .apply()
    }

    fun getCloudinaryConfig(): Map<String, String>? {
        val cloudName = prefs.getString(KEY_CLOUDINARY_CLOUD_NAME, null)
        val uploadPreset = prefs.getString(KEY_CLOUDINARY_UPLOAD_PRESET, null)
        val apiKey = prefs.getString(KEY_CLOUDINARY_API_KEY, "") ?: ""
        val apiSecret = prefs.getString(KEY_CLOUDINARY_API_SECRET, "") ?: ""

        if (cloudName.isNullOrBlank() || uploadPreset.isNullOrBlank()) {
            return null
        }
        return mapOf(
            "cloudName" to cloudName,
            "uploadPreset" to uploadPreset,
            "apiKey" to apiKey,
            "apiSecret" to apiSecret
        )
    }

    fun clearCloudinaryConfig() {
        prefs.edit()
            .remove(KEY_CLOUDINARY_CLOUD_NAME)
            .remove(KEY_CLOUDINARY_UPLOAD_PRESET)
            .remove(KEY_CLOUDINARY_API_KEY)
            .remove(KEY_CLOUDINARY_API_SECRET)
            .apply()
    }

    fun setFirebaseConfig(projectId: String, apiKey: String, appId: String, storageBucket: String?) {
        prefs.edit()
            .putString(KEY_FB_PROJECT_ID, projectId.trim())
            .putString(KEY_FB_API_KEY, apiKey.trim())
            .putString(KEY_FB_APP_ID, appId.trim())
            .putString(KEY_FB_STORAGE_BUCKET, storageBucket?.trim())
            .apply()
    }

    fun getFirebaseConfig(): Map<String, String>? {
        val projId = prefs.getString(KEY_FB_PROJECT_ID, null)
        val apiKey = prefs.getString(KEY_FB_API_KEY, null)
        val appId = prefs.getString(KEY_FB_APP_ID, null)
        val bucket = prefs.getString(KEY_FB_STORAGE_BUCKET, null) ?: ""

        if (projId != null && apiKey != null && appId != null) {
            return mapOf(
                "projectId" to projId,
                "apiKey" to apiKey,
                "appId" to appId,
                "storageBucket" to bucket
            )
        }

        // Return default online credentials provided for study-vault
        return mapOf(
            "projectId" to "study-vault-43f06",
            "apiKey" to "AIzaSyDcLCArSYUzgaUyn3EdxSZy7GmRdKXVV3Y",
            "appId" to "1:11567534299:android:b3fb391c1b936708c37cfe",
            "storageBucket" to "study-vault-43f06.firebasestorage.app"
        )
    }

    fun clearFirebaseConfig() {
        prefs.edit()
            .remove(KEY_FB_PROJECT_ID)
            .remove(KEY_FB_API_KEY)
            .remove(KEY_FB_APP_ID)
            .remove(KEY_FB_STORAGE_BUCKET)
            .apply()
    }
}
