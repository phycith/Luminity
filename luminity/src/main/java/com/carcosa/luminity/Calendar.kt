package com.carcosa.luminity

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carcosa.luminity.ui.theme.DarkButtonGray
import com.carcosa.luminity.ui.theme.SelectedTeal
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun CalendarScreen(cards: List<TimerCardData>) {
    var showTracking by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance().time) }
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Main Content Area
            Box(modifier = Modifier.weight(1f)) {
                if (showTracking) {
                    DayTrackingView(selectedDate, cards)
                } else {
                    DayDetailView(selectedDate, cards)
                }
            }
            
            // Bottom Calendar Cards
            CalendarBottomBar(
                selectedDate = selectedDate,
                onDateSelected = { selectedDate = it },
                onAddClick = { /* Handle Add */ },
                onMoreClick = { option ->
                    when (option) {
                        "Current page" -> showTracking = false
                        "24h tracking" -> showTracking = true
                    }
                }
            )
        }
    }
}

@Composable
fun DayDetailView(@Suppress("UNUSED_PARAMETER") date: Date, cards: List<TimerCardData>) {
    val tasksOnly = cards.filter { it.id != "add_button_id" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        itemsIndexed(tasksOnly) { _, task ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF262626))
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(task.icon, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = task.title, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    }
                    Text(
                        text = formatTimeHistory(task.accumulatedTimeMillis),
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun DayTrackingView(date: Date, cards: List<TimerCardData>) {
    val startOfDay = getStartOfDay(date)
    val endOfDay = startOfDay + TimeUnit.DAYS.toMillis(1)
    
    val daySessions = remember(cards, date) {
        cards.flatMap { card ->
            card.sessions
                .filter { it.startTime < endOfDay && it.endTime > startOfDay }
                .map { session -> card to session }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 0.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val totalHeight = maxHeight
            val totalWidth = maxWidth
            val hourHeight = totalHeight / 24f
            
            // Timeline Lines
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val labelWidth = 50.dp.toPx()
                
                for (i in 0..24) {
                    val y = (i.toFloat() / 24f) * canvasHeight
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.2f),
                        start = Offset(labelWidth, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 0.5.dp.toPx()
                    )
                }
            }
            
            // Hour Labels
            Column(modifier = Modifier.fillMaxSize()) {
                for (i in 0..24) {
                    val hour = 24 - i
                    Box(modifier = Modifier.height(hourHeight), contentAlignment = Alignment.TopStart) {
                        Text(
                            text = String.format(Locale.getDefault(), "%02d:00", hour),
                            color = Color.Gray,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .width(50.dp)
                                .offset(y = if (i == 0) 0.dp else (-6).dp)
                        )
                    }
                }
            }
            
            // Session Blocks
            Box(modifier = Modifier.fillMaxSize().padding(start = 50.dp)) {
                val timelineWidth = totalWidth - 50.dp
                val overlappingGroups = groupOverlappingSessions(daySessions, startOfDay, endOfDay)
                
                overlappingGroups.forEach { group ->
                    group.forEachIndexed { index, (card, session) ->
                        val sessionStart = maxOf(session.startTime, startOfDay)
                        val sessionEnd = minOf(session.endTime, endOfDay)
                        
                        val startFraction = (sessionStart - startOfDay).toFloat() / TimeUnit.DAYS.toMillis(1).toFloat()
                        val endFraction = (sessionEnd - startOfDay).toFloat() / TimeUnit.DAYS.toMillis(1).toFloat()
                        
                        // Vertical positioning: 24:00 at top, 00:00 at bottom
                        val top = totalHeight * (1f - endFraction)
                        val bottom = totalHeight * (1f - startFraction)
                        val height = maxOf(bottom - top, 1.dp)
                        
                        val widthFraction = 1f / group.size
                        val xOffset = timelineWidth * (index.toFloat() * widthFraction)
                        
                        Box(
                            modifier = Modifier
                                .offset(x = xOffset, y = top)
                                .fillMaxWidth(widthFraction)
                                .height(height)
                                .padding(horizontal = 0.5.dp)
                                .background(card.color.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                                .border(0.5.dp, card.color, RoundedCornerShape(2.dp))
                                .padding(2.dp)
                        ) {
                            if (height > 12.dp) {
                                Text(
                                    text = card.title,
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun groupOverlappingSessions(
    sessions: List<Pair<TimerCardData, TaskSession>>,
    startOfDay: Long,
    endOfDay: Long
): List<List<Pair<TimerCardData, TaskSession>>> {
    if (sessions.isEmpty()) return emptyList()
    
    val sorted = sessions.sortedBy { it.second.startTime }
    val groups = mutableListOf<MutableList<Pair<TimerCardData, TaskSession>>>()
    
    sorted.forEach { sessionPair ->
        var added = false
        for (group in groups) {
            val overlaps = group.any { 
                val s1 = maxOf(it.second.startTime, startOfDay)
                val e1 = minOf(it.second.endTime, endOfDay)
                val s2 = maxOf(sessionPair.second.startTime, startOfDay)
                val e2 = minOf(sessionPair.second.endTime, endOfDay)
                s1 < e2 && s2 < e1
            }
            if (overlaps) {
                group.add(sessionPair)
                added = true
                break
            }
        }
        if (!added) {
            groups.add(mutableListOf(sessionPair))
        }
    }
    return groups
}

@Composable
fun CalendarBottomBar(
    selectedDate: Date,
    onDateSelected: (Date) -> Unit,
    onAddClick: () -> Unit,
    onMoreClick: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val dates = remember {
        val list = mutableListOf<Date>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -30)
        repeat(60) {
            list.add(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }
    
    val today = Calendar.getInstance().time
    val todayIndex = dates.indexOfFirst { isSameDay(it, today) }.coerceAtLeast(0)
    // Center today: if index 30, we want it to be 3rd card, so start at index 28
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (todayIndex - 2).coerceAtLeast(0))

    Surface(
        color = Color.Black,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp, top = 2.dp, start = 8.dp, end = 8.dp)
                .height(46.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // LazyRow area for 7 days
            LazyRow(
                state = listState,
                modifier = Modifier.width(248.dp), // 7 * 32dp + 6 * 4dp = 224 + 24 = 248
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(dates) { _, date ->
                    val isSelected = isSameDay(date, selectedDate)
                    DayCard(
                        date = date, 
                        isSelected = isSelected,
                        onClick = { onDateSelected(date) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(4.dp))
            CalendarActionCard(icon = Icons.Default.Add, onClick = onAddClick)
            Spacer(modifier = Modifier.width(4.dp))
            Box {
                CalendarActionCard(icon = Icons.Default.MoreHoriz, onClick = { expanded = true })
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(DarkButtonGray)
                ) {
                    DropdownMenuItem(
                        text = { Text("Current page", color = Color.White) },
                        onClick = { onMoreClick("Current page"); expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("24h tracking", color = Color.White) },
                        onClick = { onMoreClick("24h tracking"); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
fun DayCard(date: Date, isSelected: Boolean, onClick: () -> Unit) {
    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val dateFormat = SimpleDateFormat("d", Locale.getDefault())
    val isToday = isSameDay(date, Calendar.getInstance().time)
    
    Column(
        modifier = Modifier
            .width(32.dp)
            .height(42.dp)
            .background(
                color = if (isSelected) SelectedTeal else DarkButtonGray,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = dayFormat.format(date).uppercase(),
            color = if (isSelected) Color.White else Color.Gray,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = dateFormat.format(date),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        if (isToday) {
            Box(
                modifier = Modifier
                    .size(2.dp)
                    .background(if (isSelected) Color.White else SelectedTeal, CircleShape)
                    .offset(y = 2.dp)
            )
        }
    }
}

@Composable
fun CalendarActionCard(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(32.dp)
            .height(42.dp)
            .background(DarkButtonGray, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}

private fun isSameDay(date1: Date, date2: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = date1 }
    val cal2 = Calendar.getInstance().apply { time = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun getStartOfDay(date: Date): Long {
    val cal = Calendar.getInstance()
    cal.time = date
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun formatTimeHistory(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
}
