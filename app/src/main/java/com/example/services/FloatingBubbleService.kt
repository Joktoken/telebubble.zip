package com.example.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

class FloatingBubbleService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    // Lifecycle variables to support ComposeView inside Service
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var windowManager: WindowManager
    private lateinit var bubbleView: ComposeView
    private lateinit var bubbleParams: WindowManager.LayoutParams

    // State flows/states
    private var bubbleTooltip = mutableStateOf<String?>(null)

    private val handler = Handler(Looper.getMainLooper())
    private var hideTooltipRunnable: Runnable? = null

    // Touch positions for dragging
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private var isNearDismissZone = mutableStateOf(false)

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        setupFloatingView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        if (intent != null) {
            val action = intent.getStringExtra("action")
            val sender = intent.getStringExtra("sender")
            val messageText = intent.getStringExtra("messageText")

            if (action == "UPDATE_COUNT" && sender != null && messageText != null) {
                showTooltip(sender, messageText)

                // Play custom selected alert sound
                val sharedPrefs = getSharedPreferences("telebubble_settings", Context.MODE_PRIVATE)
                val soundOption = sharedPrefs.getInt("bubble_sound_option", 3) // 3 is Bubble Pop default
                com.example.utils.SoundPlayer.playSound(this, soundOption)
            }
        }

        return START_STICKY
    }

    private fun showTooltip(sender: String, text: String) {
        val sharedPrefs = getSharedPreferences("telebubble_settings", Context.MODE_PRIVATE)
        val strictEncryption = sharedPrefs.getBoolean("strict_in_memory_encryption", false)

        val finalSender = if (strictEncryption) {
            val hash = sender.hashCode().toString(16).uppercase()
            "Cipher_$hash"
        } else {
            sender
        }

        val finalText = if (strictEncryption) {
            "••••••"
        } else {
            text
        }

        bubbleTooltip.value = "$finalSender: $finalText"
        hideTooltipRunnable?.let { handler.removeCallbacks(it) }
        hideTooltipRunnable = Runnable {
            bubbleTooltip.value = null
        }
        handler.postDelayed(hideTooltipRunnable!!, 4000)
    }

    private fun setupFloatingView() {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        bubbleParams = WindowManager.LayoutParams(
            dpToPx(72),
            dpToPx(72),
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - dpToPx(85) // Right edge
            y = screenHeight / 3
        }

        bubbleView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingBubbleService)
            setViewTreeViewModelStoreOwner(this@FloatingBubbleService)
            setViewTreeSavedStateRegistryOwner(this@FloatingBubbleService)
            setContent {
                BubbleContent()
            }
        }

        // Custom touch listener for drag-to-dismiss or click
        bubbleView.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val dm = DisplayMetrics()
                windowManager.defaultDisplay.getMetrics(dm)
                val sWidth = dm.widthPixels
                val sHeight = dm.heightPixels

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = bubbleParams.x
                        initialY = bubbleParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        isNearDismissZone.value = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = (event.rawX - initialTouchX).toInt()
                        val deltaY = (event.rawY - initialTouchY).toInt()

                        if (Math.abs(deltaX) > 15 || Math.abs(deltaY) > 15) {
                            isDragging = true
                        }

                        if (isDragging) {
                            bubbleParams.x = initialX + deltaX
                            bubbleParams.y = initialY + deltaY

                            // Check if dragged near bottom center for dismissal
                            val dismissZoneY = sHeight - dpToPx(130)
                            val dismissZoneXMin = (sWidth / 2) - dpToPx(80)
                            val dismissZoneXMax = (sWidth / 2) + dpToPx(80)

                            isNearDismissZone.value = bubbleParams.y > dismissZoneY && 
                                                    bubbleParams.x in dismissZoneXMin..dismissZoneXMax

                            windowManager.updateViewLayout(bubbleView, bubbleParams)
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            handleBubbleTap()
                        } else {
                            if (isNearDismissZone.value) {
                                stopSelf()
                            } else {
                                // Snap to nearest side edge
                                val targetX = if (bubbleParams.x < sWidth / 2) {
                                    dpToPx(10)
                                } else {
                                    sWidth - dpToPx(82)
                                }
                                animateBubbleToX(targetX)
                            }
                        }
                        isDragging = false
                        isNearDismissZone.value = false
                        return true
                    }
                }
                return false
            }
        })

        try {
            windowManager.addView(bubbleView, bubbleParams)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "فشل بدء الفقاعة العائمة. يرجى تفعيل إذن الظهور فوق التطبيقات.", Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

    private fun handleBubbleTap() {
        val latest = TelegramNotificationBridge.getLatestNotification()
        if (latest != null) {
            try {
                latest.contentIntent.send()
                
                val sharedPrefs = getSharedPreferences("telebubble_settings", Context.MODE_PRIVATE)
                val autoClear = sharedPrefs.getBoolean("auto_clear_on_open", true)
                if (autoClear) {
                    TelegramNotificationBridge.removeNotification(latest.sender)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                launchTelegramApp()
            }
        } else {
            launchTelegramApp()
        }

        if (TelegramNotificationBridge.getNotificationsCount() == 0) {
            stopSelf()
        }
    }

    private fun launchTelegramApp() {
        val pm = packageManager
        var intent = pm.getLaunchIntentForPackage("org.telegram.messenger")
        if (intent == null) {
            intent = pm.getLaunchIntentForPackage("org.telegram.messenger.web")
        }
        if (intent == null) {
            intent = pm.getLaunchIntentForPackage("org.telegram.plus")
        }
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } else {
            Toast.makeText(this, "تطبيق تيليجرام غير مثبت على جهازك", Toast.LENGTH_SHORT).show()
        }
    }

    private fun animateBubbleToX(targetX: Int) {
        val startX = bubbleParams.x
        val steps = 10
        val delayMs = 15L
        var currentStep = 0

        val runnable = object : Runnable {
            override fun run() {
                if (currentStep < steps) {
                    currentStep++
                    val fraction = currentStep.toFloat() / steps
                    bubbleParams.x = startX + ((targetX - startX) * fraction).toInt()
                    try {
                        windowManager.updateViewLayout(bubbleView, bubbleParams)
                        handler.postDelayed(this, delayMs)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        handler.post(runnable)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        
        try {
            windowManager.removeView(bubbleView)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @Composable
    fun BubbleContent() {
        val activeNotifications by TelegramNotificationBridge.activeNotifications.collectAsState()
        val count = activeNotifications.size
        val tooltip by bubbleTooltip
        val nearDismiss by isNearDismissZone

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Tooltip Popup Banner
            AnimatedVisibility(
                visible = tooltip != null,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally(),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 80.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.widthIn(max = 200.dp).shadow(8.dp, RoundedCornerShape(16.dp))
                ) {
                    Text(
                        text = tooltip ?: "",
                        color = Color.White,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            // Floating Circle Bubble
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = if (nearDismiss) {
                                listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                            } else {
                                listOf(Color(0xFF24A1DE), Color(0xFF1B82B5))
                            }
                        )
                    )
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "Telegram Bubble",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )

                // Unread notification badge
                if (count > 0) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.TopEnd)
                            .background(Color.Red, CircleShape)
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = count.coerceAtMost(99).toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
