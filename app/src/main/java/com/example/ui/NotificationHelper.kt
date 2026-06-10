package com.example.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object NotificationHelper {
    private const val CHANNEL_CHAT_ID = "channel_chat"
    private const val CHANNEL_MATERIALS_ID = "channel_materials"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nameChat = "Chat Messages"
            val descChat = "Notifications for group and personal chats"
            val importanceChat = NotificationManager.IMPORTANCE_HIGH
            val channelChat = NotificationChannel(CHANNEL_CHAT_ID, nameChat, importanceChat).apply {
                description = descChat
            }

            val nameMaterials = "Study Materials"
            val descMaterials = "Notifications for new uploads and study group materials"
            val importanceMaterials = NotificationManager.IMPORTANCE_DEFAULT
            val channelMaterials = NotificationChannel(CHANNEL_MATERIALS_ID, nameMaterials, importanceMaterials).apply {
                description = descMaterials
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channelChat)
            notificationManager.createNotificationChannel(channelMaterials)
        }
    }

    fun showChatNotification(context: Context, sender: String, text: String, isGroup: Boolean) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = if (isGroup) "Group Buzz: $sender" else "Direct Memo: $sender"
        val cleanText = if (text.startsWith("[FILE]")) "Shared a file attachment 📎" else text

        val builder = NotificationCompat.Builder(context, CHANNEL_CHAT_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(cleanText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(sender.hashCode() + text.hashCode(), builder.build())
            }
        } catch (_: SecurityException) {
            // Permission not granted or missing
        }
    }

    fun showMaterialNotification(context: Context, uploader: String, fileName: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_MATERIALS_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("New Vault Material 📁")
            .setContentText("$uploader uploaded \"$fileName\"")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(fileName.hashCode() + uploader.hashCode(), builder.build())
            }
        } catch (_: SecurityException) {
            // Permission not granted or missing
        }
    }
}
