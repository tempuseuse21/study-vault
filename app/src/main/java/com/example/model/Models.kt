package com.example.model

import java.io.Serializable

data class User(
    val username: String,
    val passwordHash: String // Stored as simple string for match, password field in firestore
) : Serializable

data class StudyMaterial(
    val id: String = "",
    val fileName: String = "",
    val uploadedBy: String = "",
    val timestamp: Long = 0L,
    val fileURL: String = ""
) : Serializable

data class GroupMessage(
    val id: String = "",
    val senderName: String = "",
    val messageText: String = "",
    val timestamp: Long = 0L
) : Serializable

data class PersonalMessage(
    val id: String = "",
    val sender: String = "",
    val receiver: String = "",
    val messageText: String = "",
    val timestamp: Long = 0L
) : Serializable
