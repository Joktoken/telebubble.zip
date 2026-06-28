package com.example.services

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class TelegramNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        
        // Match Telegram variants or our test triggers
        if (packageName != "org.telegram.messenger" && 
            packageName != "org.telegram.messenger.web" &&
            packageName != "org.telegram.plus" &&
            packageName != "com.example" &&
            packageName != "com.aistudio.telebubble.wqnzp"
        ) {
            return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return
        
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        if (title.isEmpty() || text.isEmpty()) return

        // Skip background service notifications
        if (title.contains("Telegram", ignoreCase = true) && text.contains("running", ignoreCase = true)) {
            return
        }

        // Apply advanced filtering to identify personal direct messages only (skip groups, channels, bots)
        if (!shouldAllowNotification(title, text, extras, notification)) {
            Log.d("NotificationListener", "Notification ignored due to filter settings: Sender='$title', Msg='$text'")
            return
        }

        // Retrieve the Content Intent (action when tapped)
        val contentIntent = notification.contentIntent
        if (contentIntent != null) {
            TelegramNotificationBridge.addNotification(this, title, contentIntent)

            // Notify or update FloatingBubbleService
            val serviceIntent = Intent(this, FloatingBubbleService::class.java).apply {
                putExtra("action", "UPDATE_COUNT")
                putExtra("sender", title)
                putExtra("messageText", text)
            }
            try {
                startService(serviceIntent)
            } catch (e: Exception) {
                Log.e("NotificationListener", "Failed to start bubble service", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        if (packageName != "org.telegram.messenger" && 
            packageName != "org.telegram.messenger.web" &&
            packageName != "org.telegram.plus" &&
            packageName != "com.example" &&
            packageName != "com.aistudio.telebubble.wqnzp"
        ) {
            return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""

        if (title.isNotEmpty()) {
            TelegramNotificationBridge.removeNotification(title)
            
            // Send update to bubble service to refresh count
            val serviceIntent = Intent(this, FloatingBubbleService::class.java).apply {
                putExtra("action", "UPDATE_COUNT")
            }
            try {
                startService(serviceIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Helper to verify if the notification is a direct message and not a group, channel, or bot.
     */
    private fun shouldAllowNotification(
        title: String,
        text: String,
        extras: android.os.Bundle,
        notification: Notification
    ): Boolean {
        val sharedPrefs = getSharedPreferences("telebubble_settings", Context.MODE_PRIVATE)
        val onlyDirectMessages = sharedPrefs.getBoolean("only_direct_messages", true)
        val blockGroups = sharedPrefs.getBoolean("block_groups", true)
        val blockChannels = sharedPrefs.getBoolean("block_channels", true)
        val blockBots = sharedPrefs.getBoolean("block_bots", true)

        if (!onlyDirectMessages) {
            return true // Filter is disabled, allow all
        }

        // 1. Identify Group Chat Conversation
        val isGroupConversation = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)
        val conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
        
        // Common formats: conversationTitle is set and distinct from sender name, or title has group patterns like 'Sender @ Group' or 'GroupName (Sender)'
        val isGroup = isGroupConversation || 
                      (conversationTitle != null && conversationTitle.isNotEmpty() && conversationTitle != title) ||
                      title.contains(" @ ") || 
                      (title.contains("(") && title.contains(")"))

        if (isGroup && blockGroups) {
            Log.d("NotificationFilter", "Blocked Group Notification: Title='$title'")
            return false
        }

        // 2. Identify Channel Broadcast (Channels in Telegram do not have Reply Action / RemoteInputs)
        var hasReplyAction = false
        notification.actions?.forEach { action ->
            if (action.remoteInputs != null && action.remoteInputs.isNotEmpty()) {
                hasReplyAction = true
            }
        }

        // If it's not a group, and lacks direct reply ability, it's a channel broadcast
        val isChannel = !isGroup && !hasReplyAction
        if (isChannel && blockChannels) {
            Log.d("NotificationFilter", "Blocked Channel Notification: Title='$title'")
            return false
        }

        // 3. Identify Bot Message (usually ends with 'bot' or 'Bot')
        val isBot = title.endsWith("bot", ignoreCase = true) || 
                    title.endsWith("Bot", ignoreCase = true) || 
                    title.contains(" bot ", ignoreCase = true) ||
                    title.contains(" Bot ", ignoreCase = true)

        if (isBot && blockBots) {
            Log.d("NotificationFilter", "Blocked Bot Notification: Title='$title'")
            return false
        }

        return true
    }
}
