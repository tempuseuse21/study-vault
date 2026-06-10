package com.example.data.mongodb

import android.content.Context
import android.util.Log
import com.example.data.SessionManager
import com.example.data.StudyGroupRepository
import com.example.model.GroupMessage
import com.example.model.PersonalMessage
import com.example.model.StudyMaterial
import com.example.model.User
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url
import java.util.UUID
import java.util.concurrent.TimeUnit

// Retrofit API Interfaces for MongoDB Data API and Cloudinary
interface MongoApiService {
    @POST
    suspend fun findWord(
        @Url url: String,
        @Header("api-key") apiKey: String,
        @Body requestBody: Map<String, Any?>
    ): Map<String, Any?>
}

interface CloudinaryApiService {
    @POST
    suspend fun uploadFile(
        @Url url: String,
        @Body body: MultipartBody
    ): Map<String, Any?>
}

class MongoCloudinaryStudyGroupRepository(
    private val context: Context
) : StudyGroupRepository {

    private val sessionManager = SessionManager(context)

    // Predefined users support
    private val predefinedUsers = mapOf(
        "om" to "1004",
        "alok" to "1004",
        "vishwa" to "1004",
        "bhavya" to "1004",
        "arjun" to "1004",
        "freny" to "1004",
        "vency" to "1004",
        "palak" to "1004",
        "anjli" to "1004"
    )

    // Fallback Local Lists for offline/unconfigured mode
    private val localUsers = mutableMapOf<String, String>().apply { putAll(predefinedUsers) }
    private val localGroupMessages = MutableStateFlow<List<GroupMessage>>(emptyList())
    private val localPersonalMessages = MutableStateFlow<List<PersonalMessage>>(emptyList())
    private val localStudyMaterials = MutableStateFlow<List<StudyMaterial>>(emptyList())

    // Retrofit Instance Creators
    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    private val mongoRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://data.mongodb-api.com/") // Endpoint is fully dynamic
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    private val mongoService by lazy { mongoRetrofit.create(MongoApiService::class.java) }
    private val cloudinaryService by lazy { mongoRetrofit.create(CloudinaryApiService::class.java) }

    // Helpers to quickly determine if Mongo is configured
    private fun getMongoKeys(): Map<String, String>? {
        val configured = sessionManager.getMongoConfig()
        if (configured != null) return configured
        return mapOf(
            "appId" to "al-t3OP_hbvd10LXKAnMH0VuEhmqiwZLsucsJFmRgtD5X6",
            "apiKey" to "al-t3OP_hbvd10LXKAnMH0VuEhmqiwZLsucsJFmRgtD5X6",
            "datasource" to "studyvault",
            "database" to "studyvault"
        )
    }

    private fun getCloudinaryKeys(): Map<String, String>? {
        val configured = sessionManager.getCloudinaryConfig()
        if (configured != null) return configured
        return mapOf(
            "cloudName" to "dt1vudtwp",
            "uploadPreset" to "study vault",
            "apiKey" to "",
            "apiSecret" to ""
        )
    }

    override fun isFirebaseMode(): Boolean = false // Set false to indicate we are using MongoDB Architecture
    override fun getFirebaseConfigError(): String? {
        val mongoOk = getMongoKeys() != null
        val cloudOk = getCloudinaryKeys() != null
        return if (mongoOk && cloudOk) null else "Not fully configured. Please check Profile settings to configure MongoDB & Cloudinary."
    }

    override fun reinitialize(): Boolean {
        Log.d("MongoRepo", "Reinitialized database client.")
        return true
    }

    // --- HELPER WRAPPER TO MAKE REQUEST INTELLIGENTLY ---
    private suspend fun runMongoAction(
        collection: String,
        action: String,
        payload: Map<String, Any?>
    ): Map<String, Any?>? {
        val config = getMongoKeys() ?: return null
        val appId = config["appId"] ?: return null
        val apiKey = config["apiKey"] ?: return null
        val dataSource = config["datasource"] ?: "Cluster0"
        val database = config["database"] ?: "study_group_vault"

        val requestUrl = "https://data.mongodb-api.com/app/$appId/endpoint/data/v1/action/$action"
        
        val fullPayload = mutableMapOf<String, Any?>(
            "dataSource" to dataSource,
            "database" to database,
            "collection" to collection
        ).apply {
            putAll(payload)
        }

        return withContext(Dispatchers.IO) {
            try {
                mongoService.findWord(requestUrl, apiKey, fullPayload)
            } catch (e: Exception) {
                Log.e("MongoRepo", "Mongo REST call failed: ${e.message}", e)
                null
            }
        }
    }

    // --- AUTH ACTIONS ---
    override suspend fun login(username: String, secret: String): Result<User> {
        val sanitized = username.trim().lowercase()
        val defaultPassword = predefinedUsers[sanitized]

        val config = getMongoKeys()
        if (config == null) {
            // Unconfigured: fall back to predefined local users
            if (defaultPassword == secret.trim()) {
                return Result.success(User(username = sanitized, passwordHash = secret.trim()))
            }
            val storedPass = localUsers[sanitized]
            if (storedPass != null && storedPass == secret.trim()) {
                return Result.success(User(username = sanitized, passwordHash = secret.trim()))
            }
            return Result.failure(Exception("MongoDB unconfigured. Local verification failed."))
        }

        return runCatching {
            // Query MongoDB Atlas collection 'users'
            val result = runMongoAction(
                "users",
                "findOne",
                mapOf("filter" to mapOf("username" to sanitized))
            ) ?: throw Exception("Service unavailable or failed.")

            val document = result["document"] as? Map<String, Any?>
            if (document != null) {
                val passwordInDb = (document["password"] ?: document["passwordHash"] ?: "").toString()
                if (passwordInDb != secret.trim()) {
                    throw Exception("Incorrect password PIN.")
                }
                User(username = sanitized, passwordHash = passwordInDb)
            } else {
                // If the user document does not exist, check local default
                if (defaultPassword == secret.trim()) {
                    // Try to save to MongoDB so it persists
                    runMongoAction(
                        "users",
                        "insertOne",
                        mapOf(
                            "document" to mapOf(
                                "_id" to UUID.randomUUID().toString(),
                                "username" to sanitized,
                                "password" to secret.trim()
                            )
                        )
                    )
                    User(username = sanitized, passwordHash = secret.trim())
                } else {
                    throw Exception("Username not registered in system.")
                }
            }
        }
    }

    override suspend fun changePassword(username: String, currentPass: String, newPass: String): Result<Unit> {
        val sanitized = username.trim().lowercase()
        val config = getMongoKeys()
        if (config == null) {
            val stored = localUsers[sanitized] ?: predefinedUsers[sanitized]
            if (stored != currentPass.trim()) {
                return Result.failure(Exception("PIN validation failed."))
            }
            localUsers[sanitized] = newPass.trim()
            return Result.success(Unit)
        }

        return runCatching {
            val result = runMongoAction(
                "users",
                "findOne",
                mapOf("filter" to mapOf("username" to sanitized))
            ) ?: throw Exception("Unable to connect to database.")

            val doc = result["document"] as? Map<String, Any?> ?: throw Exception("Account not found.")
            val passwordInDb = (doc["password"] ?: doc["passwordHash"] ?: "").toString()
            if (passwordInDb != currentPass.trim()) {
                throw Exception("Invalid current password PIN.")
            }

            runMongoAction(
                "users",
                "updateOne",
                mapOf(
                    "filter" to mapOf("username" to sanitized),
                    "update" to mapOf("\$set" to mapOf("password" to newPass.trim()))
                )
            ) ?: throw Exception("Failed to update PIN.")
        }
    }

    override suspend fun getPredefinedUsernames(): List<String> = predefinedUsers.keys.toList()

    // --- CLOUDINARY FILE UPLOADS ---
    override suspend fun uploadStudyMaterial(
        fileName: String,
        fileBytes: ByteArray,
        fileMimeType: String,
        uploadedBy: String
    ): Result<StudyMaterial> {
        val cloudConfig = getCloudinaryKeys()
        val id = UUID.randomUUID().toString()

        if (cloudConfig == null) {
            // fall back to locally saved representation
            val material = StudyMaterial(
                id = id,
                fileName = fileName,
                uploadedBy = uploadedBy,
                timestamp = System.currentTimeMillis(),
                fileURL = "https://res.cloudinary.com/demo/image/upload/sample.jpg" // Fake placeholderurl
            )
            localStudyMaterials.value = localStudyMaterials.value + material
            return Result.success(material)
        }

        val cloudName = cloudConfig["cloudName"] ?: ""
        val uploadPreset = cloudConfig["uploadPreset"] ?: ""

        val requestUrl = "https://api.cloudinary.com/v1_1/$cloudName/auto/upload"

        return runCatching {
            val partFile = MultipartBody.Part.createFormData(
                "file",
                fileName,
                fileBytes.toRequestBody(fileMimeType.toMediaType())
            )

            val partPreset = MultipartBody.Part.createFormData("upload_preset", uploadPreset)

            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addPart(partFile)
                .addPart(partPreset)
                .build()

            val response = withContext(Dispatchers.IO) {
                cloudinaryService.uploadFile(requestUrl, multipartBody)
            }

            val fileUrl = response["secure_url"]?.toString() ?: response["url"]?.toString()
                ?: throw Exception("Cloudinary upload did not return URL")

            val material = StudyMaterial(
                id = id,
                fileName = fileName,
                uploadedBy = uploadedBy,
                timestamp = System.currentTimeMillis(),
                fileURL = fileUrl
            )

            // Save metadata to MongoDB
            if (getMongoKeys() != null) {
                runMongoAction(
                    "study_materials",
                    "insertOne",
                    mapOf(
                        "document" to mapOf(
                            "_id" to material.id,
                            "fileName" to material.fileName,
                            "uploadedBy" to material.uploadedBy,
                            "timestamp" to material.timestamp,
                            "fileURL" to material.fileURL
                        )
                    )
                )
            } else {
                localStudyMaterials.value = localStudyMaterials.value + material
            }

            material
        }
    }

    override fun getStudyMaterials(): Flow<List<StudyMaterial>> = flow {
        while (true) {
            val config = getMongoKeys()
            if (config != null) {
                try {
                    val result = runMongoAction(
                        "study_materials",
                        "find",
                        mapOf(
                            "sort" to mapOf("timestamp" to -1),
                            "limit" to 50
                        )
                    )
                    val docs = result?.get("documents") as? List<Map<String, Any?>>
                    if (docs != null) {
                        val materialsList = docs.map { map ->
                            StudyMaterial(
                                id = (map["_id"] ?: map["id"] ?: "").toString(),
                                fileName = (map["fileName"] ?: "File").toString(),
                                uploadedBy = (map["uploadedBy"] ?: "Unknown").toString(),
                                timestamp = (map["timestamp"] as? Number)?.toLong() ?: 0L,
                                fileURL = (map["fileURL"] ?: "").toString()
                            )
                        }
                        emit(materialsList)
                    } else {
                        emit(localStudyMaterials.value)
                    }
                } catch (e: Exception) {
                    Log.e("MongoRepo", "Could not fetch materials from MongoDB, emitting cached: ${e.message}")
                    emit(localStudyMaterials.value)
                }
            } else {
                emit(localStudyMaterials.value)
            }
            delay(4000) // Poll every 4 seconds
        }
    }.catch {
        emit(localStudyMaterials.value)
    }

    override suspend fun deleteStudyMaterial(material: StudyMaterial): Result<Unit> {
        val config = getMongoKeys()
        if (config == null) {
            localStudyMaterials.value = localStudyMaterials.value.filter { it.id != material.id }
            return Result.success(Unit)
        }

        return runCatching {
            runMongoAction(
                "study_materials",
                "deleteOne",
                mapOf("filter" to mapOf("_id" to material.id))
            ) ?: throw Exception("Delete REST call failed")
        }
    }

    // --- CHAT OPERATIONS ---
    override suspend fun sendGroupMessage(senderName: String, text: String): Result<Unit> {
        val id = UUID.randomUUID().toString()
        val message = GroupMessage(
            id = id,
            senderName = senderName,
            messageText = text.trim(),
            timestamp = System.currentTimeMillis()
        )

        val config = getMongoKeys()
        if (config == null) {
            localGroupMessages.value = localGroupMessages.value + message
            return Result.success(Unit)
        }

        return runCatching {
            runMongoAction(
                "group_messages",
                "insertOne",
                mapOf(
                    "document" to mapOf(
                        "_id" to message.id,
                        "senderName" to message.senderName,
                        "messageText" to message.messageText,
                        "timestamp" to message.timestamp
                    )
                )
            ) ?: throw Exception("Send operation failed.")
        }
    }

    override fun observeGroupMessages(): Flow<List<GroupMessage>> = flow {
        while (true) {
            val config = getMongoKeys()
            if (config != null) {
                try {
                    val result = runMongoAction(
                        "group_messages",
                        "find",
                        mapOf(
                            "sort" to mapOf("timestamp" to 1),
                            "limit" to 100
                        )
                    )
                    val docs = result?.get("documents") as? List<Map<String, Any?>>
                    if (docs != null) {
                        val messagesList = docs.map { map ->
                            GroupMessage(
                                id = (map["_id"] ?: map["id"] ?: "").toString(),
                                senderName = (map["senderName"] ?: "om").toString(),
                                messageText = (map["messageText"] ?: "").toString(),
                                timestamp = (map["timestamp"] as? Number)?.toLong() ?: 0L
                            )
                        }
                        emit(messagesList)
                    } else {
                        emit(localGroupMessages.value)
                    }
                } catch (e: Exception) {
                    Log.e("MongoRepo", "Observe group message query error: ${e.message}")
                    emit(localGroupMessages.value)
                }
            } else {
                emit(localGroupMessages.value)
            }
            delay(2500) // Poll every 2.5 seconds
        }
    }.catch {
        emit(localGroupMessages.value)
    }

    override suspend fun sendPersonalMessage(sender: String, receiver: String, text: String): Result<Unit> {
        val id = UUID.randomUUID().toString()
        val message = PersonalMessage(
            id = id,
            sender = sender,
            receiver = receiver,
            messageText = text.trim(),
            timestamp = System.currentTimeMillis()
        )

        val config = getMongoKeys()
        if (config == null) {
            localPersonalMessages.value = localPersonalMessages.value + message
            return Result.success(Unit)
        }

        return runCatching {
            runMongoAction(
                "personal_messages",
                "insertOne",
                mapOf(
                    "document" to mapOf(
                        "_id" to message.id,
                        "sender" to message.sender,
                        "receiver" to message.receiver,
                        "messageText" to message.messageText,
                        "timestamp" to message.timestamp
                    )
                )
            ) ?: throw Exception("Send personal message failed.")
        }
    }

    override fun observePersonalMessages(user1: String, user2: String): Flow<List<PersonalMessage>> = flow {
        while (true) {
            val config = getMongoKeys()
            if (config != null) {
                try {
                    // For direct messages, fetch messages between user1 and user2
                    // i.e. (sender = user1 AND receiver = user2) OR (sender = user2 AND receiver = user1)
                    val filterPayload = mapOf(
                        "\$or" to listOf(
                            mapOf("sender" to user1, "receiver" to user2),
                            mapOf("sender" to user2, "receiver" to user1)
                        )
                    )
                    val result = runMongoAction(
                        "personal_messages",
                        "find",
                        mapOf(
                            "filter" to filterPayload,
                            "sort" to mapOf("timestamp" to 1),
                            "limit" to 100
                        )
                    )
                    val docs = result?.get("documents") as? List<Map<String, Any?>>
                    if (docs != null) {
                        val messagesList = docs.map { map ->
                            PersonalMessage(
                                id = (map["_id"] ?: map["id"] ?: "").toString(),
                                sender = (map["sender"] ?: "").toString(),
                                receiver = (map["receiver"] ?: "").toString(),
                                messageText = (map["messageText"] ?: "").toString(),
                                timestamp = (map["timestamp"] as? Number)?.toLong() ?: 0L
                            )
                        }
                        emit(messagesList)
                    } else {
                        // fallback to local filtered
                        emit(filterLocalPersonal(user1, user2))
                    }
                } catch (e: Exception) {
                    Log.e("MongoRepo", "Query personal messages error: ${e.message}")
                    emit(filterLocalPersonal(user1, user2))
                }
            } else {
                emit(filterLocalPersonal(user1, user2))
            }
            delay(2500) // Poll every 2.5 seconds
        }
    }.catch {
        emit(filterLocalPersonal(user1, user2))
    }

    private fun filterLocalPersonal(u1: String, u2: String): List<PersonalMessage> {
        return localPersonalMessages.value.filter {
            (it.sender == u1 && it.receiver == u2) || (it.sender == u2 && it.receiver == u1)
        }
    }
}
