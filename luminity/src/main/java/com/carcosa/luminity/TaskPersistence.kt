package com.carcosa.luminity

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

class TaskPersistence(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("luminity_tasks", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val defaultColor = Color(0xFF00897B)

    data class PersistableTask(
        val id: String? = null,
        val title: String? = null,
        val iconName: String? = null,
        val colorInt: Int? = null,
        val accumulatedTimeMillis: Long? = null,
        val startTimeMillis: Long? = null,
        val sessions: List<TaskSession>? = null
    )

    fun saveTasks(tasks: List<TimerCardData>) {
        val persistableTasks = tasks.filter { it.id != "add_button_id" }.map { task ->
            PersistableTask(
                id = task.id,
                title = task.title,
                iconName = getIconName(task.icon),
                colorInt = task.color.toArgb(),
                accumulatedTimeMillis = task.accumulatedTimeMillis,
                startTimeMillis = task.startTimeMillis,
                sessions = task.sessions
            )
        }
        val json = gson.toJson(persistableTasks)
        sharedPreferences.edit().putString("tasks_key", json).apply()
    }

    fun loadTasks(): List<TimerCardData> {
        val json = sharedPreferences.getString("tasks_key", null) ?: return emptyList()
        val type = object : TypeToken<List<PersistableTask>>() {}.type
        val persistableTasks: List<PersistableTask> = try {
            gson.fromJson(json, type)
        } catch (_: Exception) {
            emptyList()
        }
        
        return persistableTasks.mapNotNull { pTask ->
            if (pTask == null) return@mapNotNull null
            
            TimerCardData(
                id = pTask.id ?: UUID.randomUUID().toString(),
                title = pTask.title ?: "Unnamed Task",
                icon = getIconFromName(pTask.iconName ?: ""),
                color = if (pTask.colorInt != null) Color(pTask.colorInt) else defaultColor,
                accumulatedTimeMillis = pTask.accumulatedTimeMillis ?: 0L,
                startTimeMillis = pTask.startTimeMillis,
                sessions = pTask.sessions ?: emptyList()
            )
        }
    }

    private fun getIconName(icon: ImageVector): String {
        return when (icon) {
            Icons.Default.LocationOn -> "LocationOn"
            Icons.Default.BusinessCenter -> "BusinessCenter"
            Icons.Default.AccessTime -> "AccessTime"
            Icons.Default.Work -> "Work"
            Icons.Default.Home -> "Home"
            Icons.Default.FitnessCenter -> "FitnessCenter"
            Icons.Default.School -> "School"
            Icons.Default.Brush -> "Brush"
            Icons.Default.Code -> "Code"
            Icons.Default.LocalCafe -> "LocalCafe"
            Icons.Default.Add -> "Add"
            Icons.Default.AddCircleOutline -> "AddCircleOutline"
            else -> "AccessTime"
        }
    }

    private fun getIconFromName(name: String): ImageVector {
        return when (name) {
            "LocationOn" -> Icons.Default.LocationOn
            "BusinessCenter" -> Icons.Default.BusinessCenter
            "AccessTime" -> Icons.Default.AccessTime
            "Work" -> Icons.Default.Work
            "Home" -> Icons.Default.Home
            "FitnessCenter" -> Icons.Default.FitnessCenter
            "School" -> Icons.Default.School
            "Brush" -> Icons.Default.Brush
            "Code" -> Icons.Default.Code
            "LocalCafe" -> Icons.Default.LocalCafe
            "Add" -> Icons.Default.Add
            "AddCircleOutline" -> Icons.Default.AddCircleOutline
            else -> Icons.Default.AccessTime
        }
    }
}
