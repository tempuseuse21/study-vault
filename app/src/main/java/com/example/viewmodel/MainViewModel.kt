package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SessionManager
import com.example.data.StudyGroupRepository
import com.example.data.mongodb.MongoCloudinaryStudyGroupRepository
import com.example.model.GroupMessage
import com.example.model.PersonalMessage
import com.example.model.StudyMaterial
import com.example.model.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStream

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val sessionManager = SessionManager(context)

    // Repository instances
    val firebaseRepo: StudyGroupRepository by lazy { MongoCloudinaryStudyGroupRepository(context) }

    // Toggle repo mode: we now always target online Firebase
    private val _useFirebase = MutableStateFlow(true)
    val useFirebase: StateFlow<Boolean> = _useFirebase.asStateFlow()

    // Active Repository reference — always use secure online Firebase repository
    val repository: StudyGroupRepository
        get() = firebaseRepo

    // Auth States
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    // Navigation and UX Section Selected
    private val _currentSection = MutableStateFlow("materials")
    val currentSection: StateFlow<String> = _currentSection.asStateFlow()

    // Study Materials Section State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uploadingStatus = MutableStateFlow<String?>(null)
    val uploadingStatus: StateFlow<String?> = _uploadingStatus.asStateFlow()

    // Group Messages State
    val groupMessages: StateFlow<List<GroupMessage>> = _currentUser
        .filterNotNull()
        .flatMapLatest {
            repository.observeGroupMessages()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Personal Messaging Selection & History state
    private val _selectedChatPartner = MutableStateFlow<String?>(null)
    val selectedChatPartner: StateFlow<String?> = _selectedChatPartner.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val personalMessages: StateFlow<List<PersonalMessage>> = combine(
        _currentUser.filterNotNull(),
        _selectedChatPartner.filterNotNull(),
        _useFirebase // trigger refresh if repository swaps
    ) { user, partner, _ -> Pair(user.username, partner) }
        .flatMapLatest { (current, partner) ->
            repository.observePersonalMessages(current, partner)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Study Materials search filter
    @OptIn(ExperimentalCoroutinesApi::class)
    val studyMaterials: StateFlow<List<StudyMaterial>> = combine(
        _useFirebase, // Refreshes if repo mode changes
        _searchQuery
    ) { _, _ -> }
        .flatMapLatest {
            repository.getStudyMaterials()
        }
        .combine(_searchQuery) { list, query ->
            if (query.isBlank()) {
                list
            } else {
                list.filter {
                    it.fileName.contains(query, ignoreCase = true) ||
                    it.uploadedBy.contains(query, ignoreCase = true)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Password Change states
    private val _passwordMessage = MutableStateFlow<String?>(null)
    val passwordMessage: StateFlow<String?> = _passwordMessage.asStateFlow()

    private val _isPasswordSuccess = MutableStateFlow(false)
    val isPasswordSuccess: StateFlow<Boolean> = _isPasswordSuccess.asStateFlow()

    private val _predefinedUnameList = MutableStateFlow<List<String>>(emptyList())
    val predefinedUnameList: StateFlow<List<String>> = _predefinedUnameList.asStateFlow()

    private var notificationsJob: kotlinx.coroutines.Job? = null

    init {
        // Automatically check and use Firebase options
        _useFirebase.value = true
        try {
            firebaseRepo.reinitialize()
            Log.d("MainViewModel", "Initialized with Firebase Online Repository mode.")
        } catch (e: Exception) {
            Log.e("MainViewModel", "Firebase initialization error: ${e.message}")
        }

        // Check login session (disabled per user instruction to log out automatically when closing and restarting app)
        // val cachedUser = sessionManager.getLoggedInUser()
        // if (cachedUser != null) {
        //     _currentUser.value = User(cachedUser, "")
        // }

        // Load predefined usernames
        viewModelScope.launch {
            try {
                _predefinedUnameList.value = repository.getPredefinedUsernames()
            } catch (e: Exception) {
                // fallback
                _predefinedUnameList.value = listOf("om", "alok", "vishwa", "bhavya", "arjun", "freny", "vency", "palak", "anjli")
            }
        }

        // Setup notification listeners reactively based on who is logged in
        viewModelScope.launch {
            _currentUser.collect { user ->
                notificationsJob?.cancel()
                if (user != null) {
                    notificationsJob = launchNotificationsListener(user)
                }
            }
        }
    }

    fun saveMongoConfig(appId: String, apiKey: String, datasource: String, database: String): Boolean {
        sessionManager.setMongoConfig(appId, apiKey, datasource, database)
        return firebaseRepo.reinitialize()
    }

    fun clearMongoConfig() {
        sessionManager.clearMongoConfig()
        firebaseRepo.reinitialize()
    }

    fun getMongoConfig(): Map<String, String>? {
        return sessionManager.getMongoConfig()
    }

    fun saveCloudinaryConfig(cloudName: String, uploadPreset: String, apiKey: String?, apiSecret: String?): Boolean {
        sessionManager.setCloudinaryConfig(cloudName, uploadPreset, apiKey, apiSecret)
        return firebaseRepo.reinitialize()
    }

    fun clearCloudinaryConfig() {
        sessionManager.clearCloudinaryConfig()
        firebaseRepo.reinitialize()
    }

    fun getCloudinaryConfig(): Map<String, String>? {
        return sessionManager.getCloudinaryConfig()
    }

    fun saveCustomFirebaseConfig(projectId: String, apiKey: String, appId: String, storageBucket: String?): Boolean {
        sessionManager.setFirebaseConfig(projectId, apiKey, appId, storageBucket)
        val success = firebaseRepo.reinitialize()
        _useFirebase.value = success
        return success
    }

    fun clearCustomFirebaseConfig() {
        sessionManager.clearFirebaseConfig()
        firebaseRepo.reinitialize()
        _useFirebase.value = firebaseRepo.isFirebaseMode()
    }

    fun getCustomFirebaseConfig(): Map<String, String>? {
        return sessionManager.getFirebaseConfig()
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun login(username: String, secret: String) {
        if (username.isBlank() || secret.isBlank()) {
            _authError.value = "Please fill in all blanks."
            return
        }
        viewModelScope.launch {
            _isAuthenticating.value = true
            _authError.value = null
            val result = repository.login(username, secret)
            _isAuthenticating.value = false
            
            result.onSuccess { user ->
                sessionManager.loginUser(user.username)
                _currentUser.value = user
                _authError.value = null
            }.onFailure { error ->
                _authError.value = error.localizedMessage ?: "Login failed"
            }
        }
    }

    fun logout() {
        sessionManager.logoutUser()
        _currentUser.value = null
        _selectedChatPartner.value = null
        _currentSection.value = "materials"
        _searchQuery.value = ""
    }

    fun setSection(section: String) {
        _currentSection.value = section
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectChatPartner(partner: String?) {
        _selectedChatPartner.value = partner
    }

    fun sendGroupMessage(text: String) {
        val user = _currentUser.value ?: return
        if (text.trim().isEmpty()) return
        
        viewModelScope.launch {
            repository.sendGroupMessage(user.username, text.trim())
        }
    }

    fun sendPersonalMessage(text: String) {
        val user = _currentUser.value ?: return
        val partner = _selectedChatPartner.value ?: return
        if (text.trim().isEmpty()) return
        
        viewModelScope.launch {
            repository.sendPersonalMessage(user.username, partner, text.trim())
        }
    }

    fun uploadUri(uri: Uri, contentResolver: android.content.ContentResolver, customFileName: String?) {
        val user = _currentUser.value ?: return
        
        viewModelScope.launch {
            _uploadingStatus.value = "Preparing file upload..."
            try {
                val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                var fileName = customFileName ?: ""
                
                if (fileName.isBlank()) {
                    // query database cursor to find display name
                    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val dummyNameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (dummyNameIndex != -1 && cursor.moveToFirst()) {
                            fileName = cursor.getString(dummyNameIndex)
                        }
                    }
                }
                
                if (fileName.isBlank()) {
                    fileName = "file_" + System.currentTimeMillis()
                }

                _uploadingStatus.value = "Reading file: $fileName"
                val inputStream: InputStream? = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _uploadingStatus.value = "Error: Unable to open file stream."
                    return@launch
                }
                
                val bytes = inputStream.readBytes()
                inputStream.close()

                _uploadingStatus.value = "Uploading: $fileName..."
                val result = repository.uploadStudyMaterial(fileName, bytes, mimeType, user.username)
                
                result.onSuccess {
                    _uploadingStatus.value = "Upload complete!"
                }.onFailure { err ->
                    _uploadingStatus.value = "Upload failed: ${err.localizedMessage ?: err.message}"
                }
            } catch (e: Exception) {
                _uploadingStatus.value = "Upload failed: ${e.localizedMessage ?: e.message}"
            }
        }
    }

    fun uploadPersonalFile(uri: Uri, contentResolver: android.content.ContentResolver, partner: String) {
        val user = _currentUser.value ?: return
        
        viewModelScope.launch {
            _uploadingStatus.value = "Preparing private file..."
            try {
                val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                var fileName = ""
                
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val dummyNameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (dummyNameIndex != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(dummyNameIndex)
                    }
                }
                
                if (fileName.isBlank()) {
                    fileName = "file_" + System.currentTimeMillis()
                }

                _uploadingStatus.value = "Reading private file: $fileName"
                val inputStream: InputStream? = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _uploadingStatus.value = "Error: Unable to open file stream."
                    return@launch
                }
                
                val bytes = inputStream.readBytes()
                inputStream.close()

                _uploadingStatus.value = "Sending private file: $fileName..."
                val result = repository.uploadStudyMaterial(fileName, bytes, mimeType, user.username)
                
                result.onSuccess { material ->
                    _uploadingStatus.value = null
                    // Send message with special [FILE] format
                    repository.sendPersonalMessage(user.username, partner, "[FILE]${material.fileName}|${material.fileURL}")
                }.onFailure { err ->
                    _uploadingStatus.value = "Upload failed: ${err.localizedMessage ?: err.message}"
                }
            } catch (e: Exception) {
                _uploadingStatus.value = "Upload failed: ${e.localizedMessage ?: e.message}"
            }
        }
    }

    fun clearUploadStatus() {
        _uploadingStatus.value = null
    }

    fun deleteMaterial(material: StudyMaterial) {
        viewModelScope.launch {
            repository.deleteStudyMaterial(material)
        }
    }

    fun changePassword(currentPass: String, newPass: String, confirmPass: String) {
        val user = _currentUser.value
        if (user == null) {
            _passwordMessage.value = "Error: Authenticated profile session expired."
            _isPasswordSuccess.value = false
            return
        }

        if (currentPass.isBlank() || newPass.isBlank() || confirmPass.isBlank()) {
            _passwordMessage.value = "Please complete all password blanks."
            _isPasswordSuccess.value = false
            return
        }

        if (newPass != confirmPass) {
            _passwordMessage.value = "New passwords do not match confirmation."
            _isPasswordSuccess.value = false
            return
        }

        if (newPass.length < 4) {
            _passwordMessage.value = "Policy: password must be at least 4 characters long."
            _isPasswordSuccess.value = false
            return
        }

        viewModelScope.launch {
            _passwordMessage.value = "Updating Password..."
            val result = repository.changePassword(user.username, currentPass, newPass)
            result.onSuccess {
                _passwordMessage.value = "Password changed successfully!"
                _isPasswordSuccess.value = true
            }.onFailure { err ->
                _passwordMessage.value = err.localizedMessage ?: "Failed to update security password"
                _isPasswordSuccess.value = false
            }
        }
    }

    fun clearPasswordMessage() {
        _passwordMessage.value = null
    }

    private fun launchNotificationsListener(user: User): kotlinx.coroutines.Job {
        return viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val notifiedIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())

            // 1. Group Messages Notification Listener
            launch {
                repository.observeGroupMessages().collect { list ->
                    list.forEach { msg ->
                        if (msg.timestamp >= startTime && msg.senderName != user.username) {
                            if (notifiedIds.add(msg.id)) {
                                com.example.ui.NotificationHelper.showChatNotification(
                                    context = context,
                                    sender = msg.senderName,
                                    text = msg.messageText,
                                    isGroup = true
                                )
                            }
                        }
                    }
                }
            }

            // 2. Study Materials Notification Listener
            launch {
                repository.getStudyMaterials().collect { list ->
                    list.forEach { mat ->
                        if (mat.timestamp >= startTime && mat.uploadedBy != user.username) {
                            if (notifiedIds.add(mat.id)) {
                                com.example.ui.NotificationHelper.showMaterialNotification(
                                    context = context,
                                    uploader = mat.uploadedBy,
                                    fileName = mat.fileName
                                )
                            }
                        }
                    }
                }
            }

            // 3. Personal Messages Notification Listener (all contacts as possible senders)
            val contacts = try {
                repository.getPredefinedUsernames()
            } catch (e: Exception) {
                listOf("om", "alok", "vishwa", "bhavya", "arjun", "freny", "vency", "palak", "anjli")
            }

            contacts.forEach { partner ->
                if (partner != user.username) {
                    launch {
                        repository.observePersonalMessages(user.username, partner).collect { list ->
                            list.forEach { msg ->
                                if (msg.timestamp >= startTime && msg.sender == partner) {
                                    if (notifiedIds.add(msg.id)) {
                                        com.example.ui.NotificationHelper.showChatNotification(
                                            context = context,
                                            sender = msg.sender,
                                            text = msg.messageText,
                                            isGroup = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
