package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.services.FloatingBubbleService
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.SoundPlayer
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFF0F172A) // Slate background
                ) { innerPadding ->
                    DashboardScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermissions()
    }
}

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isNotificationGranted by viewModel.isNotificationPermissionGranted.collectAsState()
    val isOverlayGranted by viewModel.isOverlayPermissionGranted.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp, top = 16.dp)
    ) {
        // App Header Banner
        item {
            HeaderSection()
        }

        // Privacy Badge (No Data is collected or sent online)
        item {
            PrivacyBadgeCard()
        }

        // Smart Filtering Customization Settings
        item {
            FilteringSettingsSection(viewModel = viewModel)
        }

        // Advanced Privacy & Strict Encryption Settings
        item {
            PrivacyFeaturesSection(viewModel = viewModel)
        }

        // Alert Sound Configuration Settings
        item {
            SoundSettingsSection(viewModel = viewModel)
        }

        // Permissions Status Section Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = stringResource(id = R.string.settings_section_title),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                )
            }
        }

        // Permission Card 1: Notification Access
        item {
            val toastMsg = stringResource(id = R.string.permission_toast_notification)
            PermissionCard(
                title = stringResource(id = R.string.notification_permission_title),
                description = stringResource(id = R.string.notification_permission_description),
                isGranted = isNotificationGranted,
                onClick = {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    context.startActivity(intent)
                    Toast.makeText(context, toastMsg, Toast.LENGTH_LONG).show()
                },
                testTag = "notification_permission_button"
            )
        }

        // Permission Card 2: Overlay Access
        item {
            val toastMsg = stringResource(id = R.string.permission_toast_overlay)
            PermissionCard(
                title = stringResource(id = R.string.overlay_permission_title),
                description = stringResource(id = R.string.overlay_permission_description),
                isGranted = isOverlayGranted,
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                    Toast.makeText(context, toastMsg, Toast.LENGTH_LONG).show()
                },
                testTag = "overlay_permission_button"
            )
        }

        // Interactive Bubble Test Actions
        item {
            val successMsg = stringResource(id = R.string.test_bubble_toast)
            val closeMsg = stringResource(id = R.string.dismiss_bubbles_toast)
            ControlCenterSection(
                onSendTest = {
                    viewModel.triggerTestBubble()
                    Toast.makeText(context, successMsg, Toast.LENGTH_LONG).show()
                },
                onStopService = {
                    context.stopService(Intent(context, FloatingBubbleService::class.java))
                    Toast.makeText(context, closeMsg, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun HeaderSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(28.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Icon Graphic
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF24A1DE), Color(0xFF1B82B5))
                        )
                    )
                    .shadow(16.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "App Logo",
                    tint = Color.White,
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = R.string.app_name),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(id = R.string.app_header_desc),
                color = Color(0xFF24A1DE),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(id = R.string.app_header_body),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun PrivacyBadgeCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF047857).copy(alpha = 0.15f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF059669).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Privacy Verified",
                tint = Color(0xFF34D399),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(id = R.string.privacy_badge_title),
                    color = Color(0xFF34D399),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.privacy_badge_description),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Start,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun FilteringSettingsSection(
    viewModel: MainViewModel
) {
    val onlyDirectMessages by viewModel.onlyDirectMessages.collectAsState()
    val blockGroups by viewModel.blockGroups.collectAsState()
    val blockChannels by viewModel.blockChannels.collectAsState()
    val blockBots by viewModel.blockBots.collectAsState()

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter List",
                    tint = Color(0xFF24A1DE),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.filtering_section_title),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = stringResource(id = R.string.filtering_section_description),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                textAlign = TextAlign.Start,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Main filter toggle
            FilterToggleRow(
                title = stringResource(id = R.string.toggle_only_dm_title),
                description = stringResource(id = R.string.toggle_only_dm_desc),
                checked = onlyDirectMessages,
                onCheckedChange = { viewModel.setOnlyDirectMessages(it) },
                testTag = "toggle_only_direct_messages"
            )

            if (onlyDirectMessages) {
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.05f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Block Groups sub-toggle
                FilterToggleRow(
                    title = stringResource(id = R.string.toggle_block_groups_title),
                    description = stringResource(id = R.string.toggle_block_groups_desc),
                    checked = blockGroups,
                    onCheckedChange = { viewModel.setBlockGroups(it) },
                    testTag = "toggle_block_groups"
                )

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.05f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Block Channels sub-toggle
                FilterToggleRow(
                    title = stringResource(id = R.string.toggle_block_channels_title),
                    description = stringResource(id = R.string.toggle_block_channels_desc),
                    checked = blockChannels,
                    onCheckedChange = { viewModel.setBlockChannels(it) },
                    testTag = "toggle_block_channels"
                )

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.05f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Block Bots sub-toggle
                FilterToggleRow(
                    title = stringResource(id = R.string.toggle_block_bots_title),
                    description = stringResource(id = R.string.toggle_block_bots_desc),
                    checked = blockBots,
                    onCheckedChange = { viewModel.setBlockBots(it) },
                    testTag = "toggle_block_bots"
                )
            }
        }
    }
}

@Composable
fun PrivacyFeaturesSection(
    viewModel: MainViewModel
) {
    val autoClearOnOpen by viewModel.autoClearOnOpen.collectAsState()
    val strictInMemoryEncryption by viewModel.strictInMemoryEncryption.collectAsState()

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Privacy Lock",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.privacy_features_title),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = stringResource(id = R.string.privacy_features_desc),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                textAlign = TextAlign.Start,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Auto Clear Toggle
            FilterToggleRow(
                title = stringResource(id = R.string.toggle_auto_clear_title),
                description = stringResource(id = R.string.toggle_auto_clear_desc),
                checked = autoClearOnOpen,
                onCheckedChange = { viewModel.setAutoClearOnOpen(it) },
                testTag = "toggle_auto_clear"
            )

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.05f),
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Strict In-Memory Encryption Masking Toggle
            FilterToggleRow(
                title = stringResource(id = R.string.toggle_encrypt_ram_title),
                description = stringResource(id = R.string.toggle_encrypt_ram_desc),
                checked = strictInMemoryEncryption,
                onCheckedChange = { viewModel.setStrictInMemoryEncryption(it) },
                testTag = "toggle_encrypt_ram"
            )
        }
    }
}

@Composable
fun SoundSettingsSection(
    viewModel: MainViewModel
) {
    val selectedOption by viewModel.bubbleSoundOption.collectAsState()
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Sound Settings",
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.sound_settings_title),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = stringResource(id = R.string.sound_settings_desc),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                textAlign = TextAlign.Start,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Option 1: Silent
            SoundOptionRow(
                optionId = 1,
                label = stringResource(id = R.string.sound_option_silent),
                isSelected = selectedOption == 1,
                onClick = {
                    viewModel.setBubbleSoundOption(1)
                }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.03f), modifier = Modifier.padding(vertical = 8.dp))

            // Option 2: Default System Sound
            SoundOptionRow(
                optionId = 2,
                label = stringResource(id = R.string.sound_option_default),
                isSelected = selectedOption == 2,
                onClick = {
                    viewModel.setBubbleSoundOption(2)
                    SoundPlayer.playSound(context, 2)
                }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.03f), modifier = Modifier.padding(vertical = 8.dp))

            // Option 3: Bubble Pop
            SoundOptionRow(
                optionId = 3,
                label = stringResource(id = R.string.sound_option_pop),
                isSelected = selectedOption == 3,
                onClick = {
                    viewModel.setBubbleSoundOption(3)
                    SoundPlayer.playSound(context, 3)
                }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.03f), modifier = Modifier.padding(vertical = 8.dp))

            // Option 4: Classic Chime
            SoundOptionRow(
                optionId = 4,
                label = stringResource(id = R.string.sound_option_chime),
                isSelected = selectedOption == 4,
                onClick = {
                    viewModel.setBubbleSoundOption(4)
                    SoundPlayer.playSound(context, 4)
                }
            )
        }
    }
}

@Composable
fun SoundOptionRow(
    optionId: Int,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFF24A1DE).copy(alpha = 0.1f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xFF24A1DE),
                unselectedColor = Color.White.copy(alpha = 0.4f)
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = if (isSelected) Color(0xFF24A1DE) else Color.White,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (isSelected && optionId != 1) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play Test",
                tint = Color(0xFF24A1DE),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun FilterToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF24A1DE),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF0F172A)
            ),
            modifier = Modifier.testTag(testTag)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp,
                textAlign = TextAlign.Start,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isGranted) Color(0xFF10B981).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f),
                RoundedCornerShape(20.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Action button or check icon
            if (isGranted) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Enabled",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF24A1DE),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag(testTag)
                ) {
                    Text(stringResource(id = R.string.permission_enable_btn), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isGranted) stringResource(id = R.string.permission_enabled) else stringResource(id = R.string.permission_disabled),
                        color = if (isGranted) Color(0xFF10B981) else Color(0xFFEF4444),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Start,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun ControlCenterSection(
    onSendTest: () -> Unit,
    onStopService: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.control_center_title),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Stop service button
                OutlinedButton(
                    onClick = onStopService,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFEF4444)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("stop_bubbles_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Stop Bubbles",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(id = R.string.btn_close_bubbles), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Send test bubble button
                Button(
                    onClick = onSendTest,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF24A1DE),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("test_bubble_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Test Bubble",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(id = R.string.btn_test_bubble), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(id = R.string.control_center_note),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}
