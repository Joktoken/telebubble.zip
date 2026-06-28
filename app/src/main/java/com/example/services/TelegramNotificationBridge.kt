package com.example.services

import android.app.PendingIntent
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class PendingNotification(
    val sender: String,
    val originalSender: String,
    val contentIntent: PendingIntent,
    val timestamp: Long = System.currentTimeMillis()
)

object TelegramNotificationBridge {
    private val _activeNotifications = MutableStateFlow<List<PendingNotification>>(emptyList())
    val activeNotifications: StateFlow<List<PendingNotification>> = _activeNotifications

    fun addNotification(context: Context, sender: String, contentIntent: PendingIntent) {
        val sharedPrefs = context.getSharedPreferences("telebubble_settings", Context.MODE_PRIVATE)
        val strictEncryption = sharedPrefs.getBoolean("strict_in_memory_encryption", false)

        // If encryption is active, we completely mask and hash the sender's identifier in memory
        val finalSender = if (strictEncryption) {
            val hash = sender.hashCode().toString(16).uppercase()
            "Cipher_$hash"
        } else {
            sender
        }

        val currentList = _activeNotifications.value.toMutableList()
        
        // Match either original or encrypted name to remove duplicate entries safely
        currentList.removeAll { it.originalSender == sender || it.sender == finalSender }
        currentList.add(0, PendingNotification(finalSender, sender, contentIntent))
        _activeNotifications.value = currentList
    }

    fun removeNotification(sender: String) {
        val currentList = _activeNotifications.value.toMutableList()
        currentList.removeAll { it.originalSender == sender || it.sender == sender }
        _activeNotifications.value = currentList
    }

    fun getLatestNotification(): PendingNotification? {
        return _activeNotifications.value.firstOrNull()
    }

    fun getNotificationsCount(): Int {
        return _activeNotifications.value.size
    }

    fun clearAll() {
        _activeNotifications.value = emptyList()
    }
}
