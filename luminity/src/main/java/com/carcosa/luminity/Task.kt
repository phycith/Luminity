package com.carcosa.luminity

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCreationScreen(
    onTaskCreated: (TimerCardData) -> Unit,
    onBack: () -> Unit
) {
    var taskName by remember { mutableStateOf("") }
    val icons = listOf(
        Icons.Default.Work,
        Icons.Default.Home,
        Icons.Default.FitnessCenter,
        Icons.Default.School,
        Icons.Default.Brush,
        Icons.Default.Code,
        Icons.Default.LocalCafe
    )
    
    val colors = listOf(
        Color(0xFF00897B), // SelectedTeal
        Color(0xFFE53935), // Red
        Color(0xFFD81B60), // Pink
        Color(0xFF8E24AA), // Purple
        Color(0xFF3949AB), // Indigo
        Color(0xFF1E88E5), // Blue
        Color(0xFF039BE5), // Light Blue
        Color(0xFF00ACC1), // Cyan
        Color(0xFF43A047), // Green
        Color(0xFF7CB342), // Light Green
        Color(0xFFFDD835), // Yellow
        Color(0xFFFFB300), // Amber
        Color(0xFFFB8C00), // Orange
        Color(0xFFF4511E), // Deep Orange
    )

    var selectedIcon by remember { mutableStateOf(icons.first()) }
    var selectedColor by remember { mutableStateOf(colors.first()) }
    var isCreating by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create New Task") },
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
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextField(
                value = taskName,
                onValueChange = { taskName = it },
                label = { Text("Task Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.DarkGray,
                    unfocusedContainerColor = Color.DarkGray,
                    cursorColor = Color.White,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Select Icon", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(icons) { icon ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (selectedIcon == icon) Color.White.copy(alpha = 0.2f) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                if (selectedIcon == icon) Color.White else Color.Gray,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedIcon = icon },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Select Color", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(colors) { color ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(color, CircleShape)
                            .border(
                                2.dp,
                                if (selectedColor == color) Color.White else Color.Transparent,
                                CircleShape
                            )
                            .clickable { selectedColor = color }
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    if (taskName.isNotBlank() && !isCreating) {
                        isCreating = true
                        onTaskCreated(
                            TimerCardData(
                                id = UUID.randomUUID().toString(),
                                title = taskName,
                                icon = selectedIcon,
                                color = selectedColor
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = taskName.isNotBlank() && !isCreating,
                colors = ButtonDefaults.buttonColors(containerColor = selectedColor)
            ) {
                Text(
                    text = if (isCreating) "Creating..." else "Create Task",
                    color = Color.White
                )
            }
        }
    }
}
