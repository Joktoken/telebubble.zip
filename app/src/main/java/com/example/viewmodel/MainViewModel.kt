package com.example.viewmodel

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MainActivity
import com.example.services.FloatingBubbleService
import com.example.services.TelegramNotificationBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("telebubble_settings", Context.MODE_PRIVATE)

    private val _isNotificationPermissionGranted = MutableStateFlow(false)
    val isNotificationPermissionGranted: StateFlow<Boolean> = _isNotificationPermissionGranted.asStateFlow()

    private val _isOverlayPermissionGranted = MutableStateFlow(false)
    val isOverlayPermissionGranted: StateFlow<Boolean> = _isOverlayPermissionGranted.asStateFlow()

    // Smart Filtering Settings
    private val _onlyDirectMessages = MutableStateFlow(sharedPrefs.getBoolean("only_direct_messages", true))
    val onlyDirectMessages: StateFlow<Boolean> = _onlyDirectMessages.asStateFlow()

    private val _blockGroups = MutableStateFlow(sharedPrefs.getBoolean("block_groups", true))
    val blockGroups: StateFlow<Boolean> = _blockGroups.asStateFlow()

    private val _blockChannels = MutableStateFlow(sharedPrefs.getBoolean("block_channels", true))
    val blockChannels: StateFlow<Boolean> = _blockChannels.asStateFlow()

    private val _blockBots = MutableStateFlow(sharedPrefs.getBoolean("block_bots", true))
    val blockBots: StateFlow<Boolean> = _blockBots.asStateFlow()

    // Enhanced Privacy Settings
    private val _autoClearOnOpen = MutableStateFlow(sharedPrefs.getBoolean("auto_clear_on_open", true))
    val autoClearOnOpen: StateFlow<Boolean> = _autoClearOnOpen.asStateFlow()

    private val _strictInMemoryEncryption = MutableStateFlow(sharedPrefs.getBoolean("strict_in_memory_encryption", false))
    val strictInMemoryEncryption: StateFlow<Boolean> = _strictInMemoryEncryption.asStateFlow()

    // Sound alert option: 1 = Silent, 2 = Default System, 3 = Pop (Synthesized), 4 = Chime (Synthesized)
    private val _bubbleSoundOption = MutableStateFlow(sharedPrefs.getInt("bubble_sound_option", 3))
    val bubbleSoundOption: StateFlow<Int> = _bubbleSoundOption.asStateFlow()

    init {
        checkPermissions()
    }

    fun checkPermissions() {
        val context = getApplication<Application>().applicationContext
        
        // Check Notification Listener Permission
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        val notificationEnabled = flat != null && flat.contains(context.packageName)
        _isNotificationPermissionGranted.value = notificationEnabled

        // Check Overlay Permission
        val overlayEnabled = Settings.canDrawOverlays(context)
        _isOverlayPermissionGranted.value = overlayEnabled
    }

    // Settings modifiers
    fun setOnlyDirectMessages(value: Boolean) {
        sharedPrefs.edit().putBoolean("only_direct_messages", value).apply()
        _onlyDirectMessages.value = value
    }

    fun setBlockGroups(value: Boolean) {
        sharedPrefs.edit().putBoolean("block_groups", value).apply()
        _blockGroups.value = value
    }

    fun setBlockChannels(value: Boolean) {
        sharedPrefs.edit().putBoolean("block_channels", value).apply()
        _blockChannels.value = value
    }

    fun setBlockBots(value: Boolean) {
        sharedPrefs.edit().putBoolean("block_bots", value).apply()
        _blockBots.value = value
    }

    fun setAutoClearOnOpen(value: Boolean) {
        sharedPrefs.edit().putBoolean("auto_clear_on_open", value).apply()
        _autoClearOnOpen.value = value
    }

    fun setStrictInMemoryEncryption(value: Boolean) {
        sharedPrefs.edit().putBoolean("strict_in_memory_encryption", value).apply()
        _strictInMemoryEncryption.value = value
    }

    fun setBubbleSoundOption(value: Int) {
        sharedPrefs.edit().putInt("bubble_sound_option", value).apply()
        _bubbleSoundOption.value = value
    }

    fun triggerTestBubble() {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            
            // Generate a secure simulation PendingIntent (pointing back to MainActivity)
            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getActivity(context, 999, intent, flags)

            // Register in our safe in-memory bridge
            TelegramNotificationBridge.addNotification(context, "مُرسل تجريبي 🎈", pendingIntent)

            // Trigger floating bubble service
            val serviceIntent = Intent(context, FloatingBubbleService::class.java).apply {
                putExtra("action", "UPDATE_COUNT")
                putExtra("sender", "مُرسل تجريبي 🎈")
                putExtra("messageText", "مرحباً! هذه رسالة تجريبية لتجربة فقاعة TeleBubble العائمة. اضغط عليها لفتح التطبيق.")
            }
            try {
                context.startService(serviceIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
