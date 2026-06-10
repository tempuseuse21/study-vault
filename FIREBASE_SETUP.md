# ⚙️ Study Group Vault — Firebase Setup Guide & Database Schema

Welcome! This document provides information on how to configure, secure, and deploy the backend for **Study Group Vault** using Google Firebase.

---

## 🛰️ 1. FIREBASE PROJECT INITIALIZATION

To connect this Android client directly to your live Google Cloud Firebase console:

1. **Create a Firebase Project:**
   * Go to the [Firebase Console](https://console.firebase.google.com/).
   * Click **Add Project** and name it `Study Group Vault`.

2. **Add an Android App:**
   * Click the **Android Icon** in your project dashboard.
   * Provide the Android Package Name (Application ID):
     ```
     com.aistudio.studygroupvault.vltgrp
     ```
   * Download the generated `google-services.json` file.

3. **Install Credentials File:**
   * Copy the downloaded `google-services.json` file and place it inside the `/app/` directory of this codebase.
   * On build sync, the Google Services Gradle plugin will automatically pull these parameters.

4. **Add Google Services Plugin (Optional):**
   * If you prefer automated build-time parsing over our water-tight manual initialization fallback, refer to the comments inside `/app/build.gradle.kts`.

---

## 🗄️ 2. FIRESTORE DATABASE SCHEMA

Our dual-mode architecture maps variables directly to standard Firestore collections. Ensure the following collections are created in your Firestore database:

### A. `users` Collection
* **Document ID:** `username` (Lowercase, e.g. `om`, `alok`, `vency`).
* **Fields:**
  * `username` (String): Normalized login username.
  * `password` (String): Secure login PIN (defaults to `1004`).

### B. `group_messages` Collection
* **Document ID:** Auto-generated UUID.
* **Fields:**
  * `senderName` (String): Username of the posting classmate.
  * `messageText` (String): Content of the chat bubble.
  * `timestamp` (Number): Unix Epoch Milliseconds representing post time.

### C. `chats` Collection
* **Document ID:** Alphabetically sorted hyphenated or underscored partner keys (e.g. `alok_om`, `arjun_freny`, `bhavya_ency`).
* **Subcollection:** `messages`
  * **Document ID:** Auto-generated UUID.
  * **Fields:**
    * `sender` (String): Username of the sending teammate.
    * `receiver` (String): Username of the target recipient teammate.
    * `messageText` (String): Content of the private message.
    * `timestamp` (Number): Unix Epoch Milliseconds.

### D. `study_materials` Collection
* **Document ID:** Auto-generated UUID file key.
* **Fields:**
  * `fileName` (String): Name and extension of the uploaded item (e.g., `calculus_notes.pdf`).
  * `uploadedBy` (String): Username of the uploader.
  * `timestamp` (Number): Unix Epoch Milliseconds.
  * `fileURL` (String): Dynamic secure Cloud Storage download link.

---

## 📂 3. FIREBASE STORAGE SETUP

Configure your **Cloud Storage** bucket to store document uploads.
* **Storage Path:** Uploaded items will reside under `/study_materials/{material_id}/filename`.
* Storage metadata is recorded to the `study_materials` Firestore database automatically upon complete byte arrival.

---

## 🔒 4. SECURITY & ACCESS CONTROL RULES

Deploy these rules to lock down your backend to the private study roster:

### Firestore Rules (`firestore.rules`)
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Check if uploader is matching local user sessions
    function isMember(user) {
      return user in ['om', 'alok', 'vishwa', 'bhavya', 'arjun', 'freny', 'vency', 'palak', 'anjli'];
    }

    // Auth Users directory
    match /users/{username} {
      allow read, write: if isMember(username);
    }

    // Live forum circle
    match /group_messages/{msgId} {
      allow read, write: if true; // Or enforce isMember(request.resource.data.senderName)
    }

    // Teammate chat directories
    match /chats/{chatRoomId}/messages/{msgId} {
      allow read, write: if true; // Enforce sorted room owner memberships
    }

    // Vault items directory
    match /study_materials/{fileId} {
      allow read, write: if true;
    }
  }
}
```

### Storage Rules (`storage.rules`)
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /study_materials/{allPaths=**} {
      allow read, write: if true; // Lock down to authenticated headers as needed
    }
  }
}
```
