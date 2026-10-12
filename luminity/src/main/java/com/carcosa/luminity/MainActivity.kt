package com.carcosa.luminity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.carcosa.luminity.ui.theme.LuminityTheme
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private lateinit var persistence: TaskPersistence

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        persistence = TaskPersistence(this)
        enableEdgeToEdge()
        setContent {
            LuminityTheme {
                MainScreen(persistence)
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Timer", Icons.Default.AccessTime)
    object History : Screen("calendar", "Calendar", Icons.Default.CalendarMonth)
    object Analytics : Screen("analytics", "Stats", Icons.Default.BarChart)
    object Social : Screen("social", "Social", Icons.Default.Public)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object CreateTask : Screen("create_task", "Create Task", Icons.Default.Add)
    object Clock : Screen("clock", "Clock", Icons.Default.Alarm)
}

@Composable
fun MainScreen(persistence: TaskPersistence) {
    val context = LocalContext.current
    var triggerAlarmId by remember { mutableStateOf<String?>(null) }


    // Check if app was launched to trigger an alarm
    val activity = context as? ComponentActivity
    LaunchedEffect(activity?.intent) {
        val id = activity?.intent?.getStringExtra("TRIGGER_ALARM_ID")
        if (id != null) {
            triggerAlarmId = id
            activity.intent.removeExtra("TRIGGER_ALARM_ID")
        }
    }

    val navController = rememberNavController()

    val items = listOf(
        Screen.Home,
        Screen.History,
        Screen.Analytics,
        Screen.Social,
        Screen.Settings
    )

    val addButton = TimerCardData("add_button_id", "Add", Icons.Default.AddCircleOutline)
    val sampleTask = TimerCardData("sample_task_id", "Focus", Icons.Default.FilterCenterFocus, Color(0xFF00897B))
    
    var cards by remember {
        val loaded = persistence.loadTasks()
        val base = if (loaded.isEmpty()) listOf(sampleTask) else loaded
        mutableStateOf(base + addButton)
    }

    
    var activeTaskIds by remember { mutableStateOf(setOf<String>()) }

    // Save cards whenever they change
    LaunchedEffect(cards) {
        persistence.saveTasks(cards)
    }

    Scaffold(
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Yellow)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .height(48.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    items.forEach { screen ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                tint = if (currentRoute == screen.route) Color(0xFF00897B) else Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    cards = cards,
                    activeTaskIds = activeTaskIds,
                    onTaskClick = { taskId ->
                        if (taskId == "add_button_id") {
                            navController.navigate(Screen.CreateTask.route)
                        } else {
                            if (activeTaskIds.contains(taskId)) {
                                activeTaskIds = activeTaskIds - taskId
                                // Stop task and record session
                                cards = cards.map {
                                    if (it.id == taskId && it.startTimeMillis != null) {
                                        val newSession = TaskSession(it.startTimeMillis!!, System.currentTimeMillis())
                                        it.copy(
                                            startTimeMillis = null,
                                            sessions = it.sessions + newSession
                                        )
                                    } else it
                                }
                            } else {
                                // Start task
                                cards = cards.map {
                                    if (it.id == taskId) {
                                        it.copy(
                                            startTimeMillis = System.currentTimeMillis()
                                        )
                                    } else it
                                }
                                activeTaskIds = activeTaskIds + taskId
                            }
                        }
                    },
                    onBannerDismiss = { taskId ->
                        activeTaskIds = activeTaskIds - taskId
                        cards = cards.map {
                            if (it.id == taskId && it.startTimeMillis != null) {
                                val newSession = TaskSession(it.startTimeMillis!!, System.currentTimeMillis())
                                it.copy(
                                    startTimeMillis = null,
                                    sessions = it.sessions + newSession
                                )
                            } else it
                        }
                    }
                )
            }
            composable(Screen.History.route) { CalendarScreen(cards) }
            composable(Screen.Analytics.route) { BlankScreen("Analytics") }
            composable(Screen.Social.route) { BlankScreen("Social") }
            composable(Screen.Settings.route) { 
                SettingsScreen(onClockClick = { navController.navigate(Screen.Clock.route) }) 
            }
            composable(Screen.Clock.route) {
                AlarmClockScreen(
                    onBack = { navController.popBackStack() },
                    triggerAlarmId = triggerAlarmId,
                    onTriggerHandled = { triggerAlarmId = null }
                )
            }

            composable(Screen.CreateTask.route) {
                TaskCreationScreen(
                    onTaskCreated = { newCard ->
                        val addIndex = cards.indexOfFirst { it.id == "add_button_id" }
                        cards = if (addIndex != -1) {
                            cards.toMutableList().apply { add(addIndex, newCard) }
                        } else {
                            cards + newCard
                        }
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    cards: List<TimerCardData>,
    activeTaskIds: Set<String>,
    onTaskClick: (String) -> Unit,
    onBannerDismiss: (String) -> Unit
) {
    val activeTasks = cards
        .filter { activeTaskIds.contains(it.id) }
        .sortedByDescending { it.accumulatedTimeMillis }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .animateContentSize(animationSpec = tween(durationMillis = 500)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            activeTasks.forEach { task ->
                key(task.id) {
                    AnimatedVisibility(
                        visible = true,
                        enter = expandVertically(animationSpec = tween(500)),
                        exit = shrinkVertically(animationSpec = tween(500))
                    ) {
                        ActiveTaskBanner(
                            cardData = task,
                            onDismiss = { onBannerDismiss(task.id) }
                        )
                    }
                }
            }
        }

        val hintText = if (activeTaskIds.isNotEmpty()) "Click on card to stop tracking" else "Click on card to start tracking"
        
        Text(
            text = hintText,
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        )

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 5
        ) {
            cards.forEach { card ->
                TaskButton(
                    cardData = card,
                    isSelected = activeTaskIds.contains(card.id),
                    onClick = { onTaskClick(card.id) }
                )
            }
        }
    }
}




@Composable
fun BlankScreen(name: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "$name Content", color = Color.White)
    }
}
