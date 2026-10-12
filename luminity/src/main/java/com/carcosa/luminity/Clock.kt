package com.carcosa.luminity

import android.Manifest
import android.content.Intent
import android.app.AlarmManager

import android.content.Context
import android.content.pm.PackageManager

import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log


import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.combinedClickable

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.util.*
import kotlin.time.Duration.Companion.seconds


enum class MissionType(val label: String, val icon: ImageVector) {
    NONE("None", Icons.Default.Block),
    MEMORY_GAME("Memory", Icons.Default.GridView),
    TYPING("Typing", Icons.Default.Keyboard),
    QR_SCAN("QR Scan", Icons.Default.QrCodeScanner)
}


enum class Difficulty(val label: String) {
    VERY_EASY("Very Easy"), EASY("Easy"), NORMAL("Normal"), HARD("Hard"), VERY_HARD("Very Hard")
}

data class Alarm(
    val id: String = UUID.randomUUID().toString(),
    val hour: Int,
    val minute: Int,
    var isEnabled: Boolean = true,
    val selectedDays: Set<Int> = emptySet(), // 0=Mon, 1=Tue, ... 6=Sun
    val everyXDays: Int? = null,
    val referenceDateMillis: Long = System.currentTimeMillis(),
    val snoozeLimit: Int = 3,
    val selectedMissions: List<MissionType> = emptyList(),
    val difficulty: Difficulty = Difficulty.NORMAL,
    val typingSentences: List<String> = emptyList(),
    val typingRandomRounds: Int = 0,
    val typingUseSentences: Boolean = true,
    val typingUseRandom: Boolean = false,
    val isWakeUpCheckEnabled: Boolean = false,
    val qrCodeData: String? = null,
    val volume: Float = 0.5f,
    val isVibrationEnabled: Boolean = true
)




fun Alarm.shouldSoundToday(calendar: Calendar = Calendar.getInstance()): Boolean {
    if (!isEnabled) return false
    
    // 1. Check if it's one of the selected days
    // Calendar.DAY_OF_WEEK: Sun=1, Mon=2, ..., Sat=7
    // Convert to 0=Mon, 1=Tue, ..., 6=Sun
    val dayOfWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        Calendar.SUNDAY -> 6
        else -> 0
    }
    
    if (selectedDays.contains(dayOfWeek)) return true
    
    // 2. Check "Every X Days"
    if (everyXDays != null && everyXDays > 0) {
        val referenceCalendar = Calendar.getInstance().apply {
            timeInMillis = referenceDateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val currentCalendar = (calendar.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val diffMillis = currentCalendar.timeInMillis - referenceCalendar.timeInMillis
        val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
        
        if (diffDays >= 0 && diffDays % everyXDays == 0) return true
    }
    
    // 3. Once-off alarm (if no repeats set)
    if (selectedDays.isEmpty() && (everyXDays == null || everyXDays == 0)) {
        return true
    }
    
    return false
}

class AlarmSoundManager(private val context: Context) {
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator


    fun startAlarm(volume: Float, vibrationEnabled: Boolean) {
        stopAlarm()
        val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ringtone = RingtoneManager.getRingtone(context, notification)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ringtone?.volume = volume
        }
        ringtone?.play()

        if (vibrationEnabled) {
            val pattern = longArrayOf(0, 500, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        }
    }

    fun playPreview(volume: Float, vibrationEnabled: Boolean) {
        // Play a short sound and vibration
        val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val tempRingtone = RingtoneManager.getRingtone(context, notification)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            tempRingtone?.volume = volume
        }
        tempRingtone?.play()
        
        if (vibrationEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(200)
            }
        }
        
        // Stop sound after 1 second
        Handler(Looper.getMainLooper()).postDelayed({
            tempRingtone?.stop()
        }, 1500)
    }

    fun stopAlarm() {
        ringtone?.stop()
        vibrator?.cancel()
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmClockScreen(
    onBack: () -> Unit,
    triggerAlarmId: String? = null,
    onTriggerHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    val persistence = remember { AlarmPersistence(context) }
    val soundManager = remember { AlarmSoundManager(context) }
    var alarms by remember { mutableStateOf(persistence.loadAlarms()) }

    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }

    var isAddingAlarm by remember { mutableStateOf(false) }
    var activeMissionAlarm by remember { mutableStateOf<Alarm?>(null) }
    var currentMissionIndex by remember { mutableIntStateOf(0) }
    var wakeUpCheckAlarm by remember { mutableStateOf<Alarm?>(null) }
    
    var alarmToDelete by remember { mutableStateOf<Alarm?>(null) }

    var lastTriggeredMinute by remember { mutableIntStateOf(-1) }

    // Auto-dismiss timer (3 minutes)
    LaunchedEffect(activeMissionAlarm) {
        if (activeMissionAlarm != null) {
            delay(180.seconds)
            if (activeMissionAlarm != null) {
                activeMissionAlarm = null
                soundManager.stopAlarm()
            }
        }
    }



    // Notification permission launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* Handled by state or logic */ }
    )

    LaunchedEffect(Unit) {
        NotificationHelper.createNotificationChannel(context)
        
        // Request notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // Check for Exact Alarm permission on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                context.startActivity(intent)
            }
        }
    }


    // Handle external trigger
    LaunchedEffect(triggerAlarmId) {
        if (triggerAlarmId != null) {
            val alarm = alarms.find { it.id == triggerAlarmId }
            if (alarm != null) {
                activeMissionAlarm = alarm
                currentMissionIndex = 0
                soundManager.startAlarm(alarm.volume, alarm.isVibrationEnabled)
            }
            onTriggerHandled()
        }
    }


    LaunchedEffect(alarms) {
        persistence.saveAlarms(alarms)
    }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance()
            val currentHour = now.get(Calendar.HOUR_OF_DAY)
            val currentMinute = now.get(Calendar.MINUTE)

            if (currentMinute != lastTriggeredMinute) {
                val alarmToTrigger = alarms.find { alarm ->
                    alarm.isEnabled &&
                    alarm.hour == currentHour &&
                    alarm.minute == currentMinute &&
                    alarm.shouldSoundToday(now)
                }

                if (alarmToTrigger != null) {
                    Log.d("AlarmClock", "Triggering alarm: ${alarmToTrigger.hour}:${alarmToTrigger.minute}")
                    activeMissionAlarm = alarmToTrigger
                    currentMissionIndex = 0
                    lastTriggeredMinute = currentMinute
                    soundManager.startAlarm(alarmToTrigger.volume, alarmToTrigger.isVibrationEnabled)
                }

            }

            delay(1.seconds)
        }
    }






    if (isAddingAlarm || editingAlarm != null) {
        AddEditAlarmScreen(
            alarm = editingAlarm,
            onSave = { newAlarm ->
                if (editingAlarm != null) {
                    alarms = alarms.map { if (it.id == editingAlarm!!.id) newAlarm else it }
                } else {
                    alarms = (alarms + newAlarm).sortedWith(compareBy({ it.hour }, { it.minute }))
                }
                isAddingAlarm = false
                editingAlarm = null
            },
            onCancel = {
                isAddingAlarm = false
                editingAlarm = null
            }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Alarm Clock", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { isAddingAlarm = true },
                    containerColor = Color(0xFF00897B),
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Alarm", modifier = Modifier.size(32.dp))
                }
            },
            containerColor = Color.Black
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                if (alarms.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Alarm, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No alarms set", color = Color.Gray, fontSize = 18.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(alarms) { alarm ->
                            AlarmItem(
                                alarm = alarm,
                                onToggle = { isEnabled ->
                                    alarms = alarms.map { if (it.id == alarm.id) it.copy(isEnabled = isEnabled) else it }
                                },
                                onDelete = {
                                    alarmToDelete = alarm
                                },

                                onClick = {
                                    editingAlarm = alarm
                                },
                                onTriggerTest = {
                                    activeMissionAlarm = alarm
                                    currentMissionIndex = 0
                                }
                            )

                        }
                    }
                }
            }
        }
    }

    // Mission Dialog
    if (activeMissionAlarm != null && activeMissionAlarm!!.selectedMissions.isNotEmpty()) {
        val missions = activeMissionAlarm!!.selectedMissions
        val currentMission = missions.getOrNull(currentMissionIndex)

        if (currentMission != null) {
            Dialog(
                onDismissRequest = { },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    when (currentMission) {
                        MissionType.NONE -> {
                            if (currentMissionIndex < missions.size - 1) {
                                currentMissionIndex++
                            } else {
                                if (activeMissionAlarm!!.isWakeUpCheckEnabled) {
                                    wakeUpCheckAlarm = activeMissionAlarm
                                }
                                activeMissionAlarm = null
                            }
                        }
                        MissionType.MEMORY_GAME -> {
                            MemoryGameMission(
                                difficulty = activeMissionAlarm!!.difficulty,
                                onComplete = {
                                    if (currentMissionIndex < missions.size - 1) {
                                        currentMissionIndex++
                                    } else {
                                        if (activeMissionAlarm!!.isWakeUpCheckEnabled) {
                                            WakeUpReceiver.scheduleWakeUpCheck(context, activeMissionAlarm!!.id)
                                        }
                                        activeMissionAlarm = null
                                        soundManager.stopAlarm()
                                    }
                                }
                            )
                        }
                        MissionType.TYPING -> {
                            TypingMission(
                                sentences = if (activeMissionAlarm!!.typingUseSentences) activeMissionAlarm!!.typingSentences else emptyList(),
                                randomRounds = if (activeMissionAlarm!!.typingUseRandom) activeMissionAlarm!!.typingRandomRounds else 0,
                                onComplete = {
                                    if (currentMissionIndex < missions.size - 1) {
                                        currentMissionIndex++
                                    } else {
                                        if (activeMissionAlarm!!.isWakeUpCheckEnabled) {
                                            WakeUpReceiver.scheduleWakeUpCheck(context, activeMissionAlarm!!.id)
                                        }
                                        activeMissionAlarm = null
                                        soundManager.stopAlarm()
                                    }
                                }
                            )
                        }
                        MissionType.QR_SCAN -> {
                            QRScanMission(
                                targetQrData = activeMissionAlarm!!.qrCodeData,
                                onComplete = {
                                    if (currentMissionIndex < missions.size - 1) {
                                        currentMissionIndex++
                                    } else {
                                        if (activeMissionAlarm!!.isWakeUpCheckEnabled) {
                                            WakeUpReceiver.scheduleWakeUpCheck(context, activeMissionAlarm!!.id)
                                        }
                                        activeMissionAlarm = null
                                        soundManager.stopAlarm()
                                    }
                                }
                            )
                        }
                    }

                }
            }
        } else {
            // All missions completed
            if (activeMissionAlarm != null) {
                if (activeMissionAlarm!!.isWakeUpCheckEnabled) {
                    WakeUpReceiver.scheduleWakeUpCheck(context, activeMissionAlarm!!.id)
                }
                activeMissionAlarm = null
                soundManager.stopAlarm()
            }
        }
    } else if (activeMissionAlarm != null) {
        // No missions selected but alarm triggered
        if (activeMissionAlarm!!.isWakeUpCheckEnabled) {
            WakeUpReceiver.scheduleWakeUpCheck(context, activeMissionAlarm!!.id)
        }
        activeMissionAlarm = null
        soundManager.stopAlarm()
    }


    DisposableEffect(Unit) {
        onDispose {
            soundManager.stopAlarm()
        }
    }




    // Deletion confirmation
    if (alarmToDelete != null) {
        AlertDialog(
            onDismissRequest = { alarmToDelete = null },
            title = { Text("Delete Alarm?") },
            text = { Text("Are you sure you want to delete this alarm?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        alarms = alarms.filter { it.id != alarmToDelete!!.id }
                        alarmToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { alarmToDelete = null }) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF1A1A1A),
            titleContentColor = Color.White,
            textContentColor = Color.Gray
        )
    }

    // Wake-up Check UI (Dialog version for manual test or immediate check)
    if (wakeUpCheckAlarm != null) {
        WakeUpCheckOverlay(
            alarmId = wakeUpCheckAlarm!!.id,
            onConfirmed = { wakeUpCheckAlarm = null },
            onFailed = {
                activeMissionAlarm = wakeUpCheckAlarm
                wakeUpCheckAlarm = null
            }
        )
    }

}


@Composable
fun WakeUpCheckOverlay(
    alarmId: String,
    onConfirmed: () -> Unit,
    onFailed: () -> Unit
) {
    val context = LocalContext.current
    var timeLeft by remember { mutableIntStateOf(30) } // 30 seconds to confirm

    var showNow by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(2.seconds) // Simulating "5 minutes" delay
        showNow = true
        while (timeLeft > 0) {
            delay(1.seconds)
            timeLeft--
        }
        onFailed()
    }

    if (showNow) {
        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "Wake-up Check!",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Are you still awake?",
                    color = Color.Gray,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    "$timeLeft",
                    color = Color(0xFF00897B),
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(48.dp))
                
                Button(
                    onClick = onConfirmed,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
                ) {
                    Text("I'M AWAKE", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { 
                            WakeUpReceiver.scheduleWakeUpCheck(context, alarmId, 5 * 60 * 1000L)
                            onConfirmed() 
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        border = BorderStroke(1.dp, Color.Gray)
                    ) {
                        Text("Check in 5m", color = Color.White, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { 
                            WakeUpReceiver.scheduleWakeUpCheck(context, alarmId, 10 * 60 * 1000L)
                            onConfirmed() 
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        border = BorderStroke(1.dp, Color.Gray)
                    ) {
                        Text("Check in 10m", color = Color.White, fontSize = 12.sp)
                    }
                }


                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Dismiss now or alarm will sound again!",
                    color = Color.Red.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAlarmScreen(
    alarm: Alarm?,
    onSave: (Alarm) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var hour by remember { mutableIntStateOf(alarm?.hour ?: 8) }

    var minute by remember { mutableIntStateOf(alarm?.minute ?: 0) }
    var selectedDays by remember { mutableStateOf(alarm?.selectedDays ?: emptySet()) }
    var everyXDays by remember { mutableStateOf(alarm?.everyXDays?.toString() ?: "") }
    var selectedMissions by remember { mutableStateOf(alarm?.selectedMissions ?: emptyList()) }

    var difficulty by remember { mutableStateOf(alarm?.difficulty ?: Difficulty.NORMAL) }
    var typingSentences by remember { mutableStateOf(alarm?.typingSentences ?: emptyList<String>()) }
    var typingRandomRounds by remember { mutableIntStateOf(alarm?.typingRandomRounds ?: 0) }
    var typingUseSentences by remember { mutableStateOf(alarm?.typingUseSentences ?: true) }
    var typingUseRandom by remember { mutableStateOf(alarm?.typingUseRandom ?: false) }
    var isWakeUpCheckEnabled by remember { mutableStateOf(alarm?.isWakeUpCheckEnabled ?: false) }
    var qrCodeData by remember { mutableStateOf(alarm?.qrCodeData) }
    var referenceDateMillis by remember { mutableLongStateOf(alarm?.referenceDateMillis ?: System.currentTimeMillis()) }
    var volume by remember { mutableFloatStateOf(alarm?.volume ?: 0.5f) }
    var isVibrationEnabled by remember { mutableStateOf(alarm?.isVibrationEnabled ?: true) }

    val soundManager = remember { AlarmSoundManager(context) }


    var isScanningQR by remember { mutableStateOf(false) }
    var isShowingQR by remember { mutableStateOf(false) }
    var isTestingQR by remember { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )






    val timePickerState = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (alarm == null) "New Alarm" else "Edit Alarm") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        onSave(
                            Alarm(
                                id = alarm?.id ?: UUID.randomUUID().toString(),
                                hour = timePickerState.hour,
                                minute = timePickerState.minute,
                                isEnabled = true,
                                selectedDays = selectedDays,
                                everyXDays = everyXDays.toIntOrNull(),
                                referenceDateMillis = referenceDateMillis,
                                snoozeLimit = 3,
                                selectedMissions = selectedMissions,

                                difficulty = difficulty,
                                typingSentences = typingSentences,
                                typingRandomRounds = typingRandomRounds,
                                typingUseSentences = typingUseSentences,
                                typingUseRandom = typingUseRandom,
                                isWakeUpCheckEnabled = isWakeUpCheckEnabled,
                                qrCodeData = qrCodeData,
                                volume = volume,
                                isVibrationEnabled = isVibrationEnabled
                            )




                        )
                    }) {
                        Text("Save", color = Color(0xFF00897B), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black, titleContentColor = Color.White)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Time Picker Card
            SettingsGroup(title = "Time") {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState, colors = TimePickerDefaults.colors(
                        clockDialColor = Color(0xFF1A1A1A),
                        selectorColor = Color(0xFF00897B),
                        containerColor = Color.Transparent,
                        periodSelectorUnselectedContainerColor = Color(0xFF1A1A1A)
                    ))
                }
            }


            Spacer(modifier = Modifier.height(16.dp))

            // Repeat Card
            SettingsGroup(title = "Repeat & Sound") {
                DaySelector(selectedDays = selectedDays, onToggle = { day ->
                    selectedDays = if (selectedDays.contains(day)) selectedDays - day else selectedDays + day
                })
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = everyXDays,
                    onValueChange = { if (it.isEmpty() || it.toIntOrNull() != null) everyXDays = it },
                    label = { Text("Every X Days (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00897B),
                        unfocusedBorderColor = Color.DarkGray,
                        focusedLabelColor = Color(0xFF00897B),
                        unfocusedLabelColor = Color.Gray,
                        cursorColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.DarkGray)
                
                SectionTitle("Alarm Volume")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = Color.Gray)
                    Slider(

                        value = volume,
                        onValueChange = { 
                            volume = it
                            soundManager.playPreview(volume, isVibrationEnabled)
                        },
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color(0xFF00897B))
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { 
                        isVibrationEnabled = !isVibrationEnabled 
                        if (isVibrationEnabled) soundManager.playPreview(volume, true)
                    }
                ) {
                    Checkbox(
                        checked = isVibrationEnabled,
                        onCheckedChange = { 
                            isVibrationEnabled = it 
                            if (isVibrationEnabled) soundManager.playPreview(volume, true)
                        },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00897B))
                    )
                    Text("Vibrate", color = Color.White)
                }

            }


            Spacer(modifier = Modifier.height(16.dp))

            // Missions Card
            SettingsGroup(title = "Missions to Dismiss") {
                MissionGridSelector(selectedMissions = selectedMissions, onToggle = { mission ->
                    if (mission == MissionType.NONE) {
                        selectedMissions = listOf(MissionType.NONE)
                    } else {
                        val current = selectedMissions.toMutableList()
                        current.remove(MissionType.NONE)
                        if (current.contains(mission)) {
                            current.remove(mission)
                        } else {
                            current.add(mission)
                        }
                        selectedMissions = current
                    }
                })

                if (selectedMissions.isNotEmpty() && !selectedMissions.contains(MissionType.NONE)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.DarkGray)
                    
                    if (selectedMissions.contains(MissionType.MEMORY_GAME)) {
                        SectionTitle("Memory Game Difficulty")
                        DifficultySelector(selectedDifficulty = difficulty, onSelect = { difficulty = it })
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    if (selectedMissions.contains(MissionType.TYPING)) {
                        SectionTitle("Typing Mission Config")
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(
                                selected = typingUseSentences,
                                onClick = { typingUseSentences = !typingUseSentences },
                                label = { Text("Sentences") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF00897B),
                                    selectedLabelColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            FilterChip(
                                selected = typingUseRandom,
                                onClick = { typingUseRandom = !typingUseRandom },
                                label = { Text("Random Letters") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF00897B),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }

                        if (typingUseSentences) {
                            Spacer(modifier = Modifier.height(8.dp))
                            var newSentence by remember { mutableStateOf("") }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = newSentence,
                                    onValueChange = { newSentence = it },
                                    label = { Text("Add Sentence") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF00897B),
                                        unfocusedBorderColor = Color.DarkGray,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                                IconButton(onClick = {
                                    if (newSentence.isNotBlank()) {
                                        typingSentences = typingSentences + newSentence
                                        newSentence = ""
                                    }
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color(0xFF00897B))
                                }
                            }
                            
                            typingSentences.forEachIndexed { index, sentence ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${index + 1}. $sentence",
                                        color = Color.White,
                                        modifier = Modifier.weight(1f),
                                        fontSize = 14.sp
                                    )
                                    IconButton(onClick = {
                                        typingSentences = typingSentences.filterIndexed { i, _ -> i != index }
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Red, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        if (typingUseRandom) {
                            Spacer(modifier = Modifier.height(8.dp))
                            SectionTitle("Random Rounds")
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Slider(
                                    value = typingRandomRounds.toFloat(),
                                    onValueChange = { typingRandomRounds = it.toInt() },
                                    valueRange = 0f..10f,
                                    steps = 9,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color(0xFF00897B))
                                )
                                Text("${typingRandomRounds} rounds", color = Color.White, modifier = Modifier.padding(start = 16.dp))
                            }
                        }
                    }

                    if (selectedMissions.contains(MissionType.QR_SCAN)) {
                        SectionTitle("QR Mission Config")
                        if (qrCodeData == null) {
                            Text("No target QR code set", color = Color.Gray, fontSize = 14.sp)
                        } else {
                            Text("Target set: ${qrCodeData!!.take(10)}...", color = Color.White, fontSize = 14.sp)
                        }
                        
                        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { isScanningQR = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (qrCodeData == null) "Scan" else "Re-scan")
                            }
                            
                            Button(
                                onClick = { 
                                    qrCodeData = UUID.randomUUID().toString()
                                    isShowingQR = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Generate")
                            }
                        }
                        
                        if (qrCodeData != null) {
                            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { isShowingQR = true },
                                    modifier = Modifier.weight(1f),
                                    border = BorderStroke(1.dp, Color(0xFF00897B))
                                ) {
                                    Text("Show QR", color = Color(0xFF00897B))
                                }
                                
                                OutlinedButton(
                                    onClick = { isTestingQR = true },
                                    modifier = Modifier.weight(1f),
                                    border = BorderStroke(1.dp, Color(0xFFFFC107))
                                ) {
                                    Text("Test QR", color = Color(0xFFFFC107))
                                }
                            }
                        }
                    }
                }
            }

            if (isScanningQR) {
                Dialog(
                    onDismissRequest = { isScanningQR = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                        if (hasCameraPermission) {
                            QRScannerView { data ->
                                qrCodeData = data
                                isScanningQR = false
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "Camera permission is required to scan QR codes",
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
                                ) {
                                    Text("Grant Permission")
                                }
                            }
                        }
                        
                        IconButton(
                            onClick = { isScanningQR = false },
                            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }
            }


            if (isShowingQR && qrCodeData != null) {
                Dialog(
                    onDismissRequest = { isShowingQR = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.White
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "Luminity Alarm Key",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Scan this to dismiss missions",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 32.dp)
                            )
                            
                            val qrBitmap = remember(qrCodeData) {
                                QRGenerator.generateQRCode(qrCodeData!!)
                            }
                            
                            if (qrBitmap != null) {
                                Box(
                                    modifier = Modifier
                                        .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
                                        .padding(16.dp)
                                ) {
                                    Image(
                                        bitmap = qrBitmap.asImageBitmap(),
                                        contentDescription = "Generated QR Code",
                                        modifier = Modifier.size(300.dp)
                                    )
                                }
                            }

                            
                            Spacer(modifier = Modifier.height(48.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Button(
                                    onClick = { isShowingQR = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                    modifier = Modifier.weight(1f).height(56.dp)
                                ) {
                                    Text("Back", fontSize = 16.sp)
                                }
                                
                                Button(
                                    onClick = { QRGenerator.shareQRCode(context, qrCodeData!!) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                                    modifier = Modifier.weight(1f).height(56.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Print / Share", fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }



            if (isTestingQR && qrCodeData != null) {
                Dialog(
                    onDismissRequest = { isTestingQR = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                        QRScanMission(
                            targetQrData = qrCodeData,
                            onComplete = { isTestingQR = false }
                        )
                        
                        IconButton(
                            onClick = { isTestingQR = false },
                            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }
            }




            Spacer(modifier = Modifier.height(16.dp))

            // Wake-up Check
            SettingsGroup(title = "Smart Options") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Wake-up Check", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Verify you are awake after 5 min", color = Color.Gray, fontSize = 12.sp)
                    }
                    Switch(
                        checked = isWakeUpCheckEnabled,
                        onCheckedChange = { isWakeUpCheckEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF00897B)
                        )
                    )
                }
            }

            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            color = Color(0xFF00897B),
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Surface(
            color = Color(0xFF1A1A1A),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        color = Color.Gray,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    )
}

@Composable
fun DaySelector(selectedDays: Set<Int>, onToggle: (Int) -> Unit) {
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEachIndexed { index, day ->
            val isSelected = selectedDays.contains(index)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color(0xFF00897B) else Color.Black)
                    .clickable { onToggle(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(day, color = if (isSelected) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MissionGridSelector(selectedMissions: List<MissionType>, onToggle: (MissionType) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MissionType.entries.forEach { mission ->
            val isSelected = selectedMissions.contains(mission) || (selectedMissions.isEmpty() && mission == MissionType.NONE)
            Surface(
                onClick = { onToggle(mission) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) Color(0xFF00897B) else Color.Black,
                modifier = Modifier.weight(1f).defaultMinSize(minWidth = 80.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = mission.icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = mission.label,
                        color = if (isSelected) Color.White else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DifficultySelector(selectedDifficulty: Difficulty, onSelect: (Difficulty) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Difficulty.entries.forEach { diff ->
            val isSelected = selectedDifficulty == diff
            SuggestionChip(
                onClick = { onSelect(diff) },
                label = { Text(diff.label) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = if (isSelected) Color(0xFF00897B) else Color.Black,
                    labelColor = if (isSelected) Color.White else Color.Gray
                ),
                border = null
            )
        }
    }
}

@Composable
fun MissionBadge(label: String, icon: ImageVector, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(9.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = label,
                color = color,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlarmItem(
    alarm: Alarm,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onTriggerTest: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onDelete
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier.width(95.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Start
                    )
                }

                
                Column(modifier = Modifier.weight(1f)) {
                    val repeatText = when {
                        alarm.everyXDays != null && alarm.selectedDays.isNotEmpty() -> {
                            val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")
                            val days = alarm.selectedDays.sorted().joinToString(", ") { dayNames[it] }
                            "Ev. ${alarm.everyXDays}d + $days"
                        }
                        alarm.everyXDays != null -> "Every ${alarm.everyXDays} days"
                        alarm.selectedDays.isNotEmpty() -> {
                            val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                            alarm.selectedDays.sorted().joinToString(", ") { dayNames[it] }
                        }
                        else -> "Once"
                    }
                    
                    Text(
                        text = repeatText,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        fontWeight = FontWeight.Normal
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        alarm.selectedMissions.forEach { mission ->
                            if (mission != MissionType.NONE) {
                                MissionBadge(mission.label, mission.icon, Color(0xFF00897B))
                            }
                        }
                        
                        if (alarm.selectedMissions.contains(MissionType.QR_SCAN)) {
                            if (alarm.qrCodeData != null) {
                                MissionBadge("QR Ready", Icons.Default.QrCode, Color(0xFF4CAF50))
                            } else {
                                MissionBadge("No QR", Icons.Default.QrCodeScanner, Color(0xFFFF5252))
                            }
                        }

                        if (alarm.isWakeUpCheckEnabled) {
                            MissionBadge("Wake Check", Icons.Default.Visibility, Color(0xFFFFC107))
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onTriggerTest, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Test", tint = Color(0xFF00897B), modifier = Modifier.size(20.dp))
                }
                
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.scale(0.8f),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF00897B),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.DarkGray
                    )
                )
            }
        }
    }
}



