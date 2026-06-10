package com.example.data

import com.example.model.GroupMessage
import com.example.model.PersonalMessage
import com.example.model.StudyMaterial
import com.example.model.User
import kotlinx.coroutines.flow.Flow

interface StudyGroupRepository {
    // Mode information
    fun isFirebaseMode(): Boolean
    fun getFirebaseConfigError(): String?
    fun reinitialize(): Boolean

    // Auth actions
    suspend fun login(username: String, secret: String): Result<User>
    suspend fun changePassword(username: String, currentPass: String, newPass: String): Result<Unit>
    suspend fun getPredefinedUsernames(): List<String>
    
    // Study files
    suspend fun uploadStudyMaterial(fileName: String, fileBytes: ByteArray, fileMimeType: String, uploadedBy: String): Result<StudyMaterial>
    fun getStudyMaterials(): Flow<List<StudyMaterial>>
    suspend fun deleteStudyMaterial(material: StudyMaterial): Result<Unit>

    // Chat operations
    suspend fun sendGroupMessage(senderName: String, text: String): Result<Unit>
    fun observeGroupMessages(): Flow<List<GroupMessage>>

    suspend fun sendPersonalMessage(sender: String, receiver: String, text: String): Result<Unit>
    fun observePersonalMessages(user1: String, user2: String): Flow<List<PersonalMessage>>
}
