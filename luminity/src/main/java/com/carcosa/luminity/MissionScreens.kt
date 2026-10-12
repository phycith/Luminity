package com.carcosa.luminity

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlin.random.Random


@Composable
fun MemoryGameMission(
    difficulty: Difficulty,
    onComplete: () -> Unit
) {
    val gridSize = when (difficulty) {
        Difficulty.VERY_EASY -> 2
        Difficulty.EASY -> 3
        Difficulty.NORMAL -> 4
        Difficulty.HARD -> 5
        Difficulty.VERY_HARD -> 6
    }
    val squareCount = when (difficulty) {
        Difficulty.VERY_EASY -> 2
        Difficulty.EASY -> 3
        Difficulty.NORMAL -> 5
        Difficulty.HARD -> 7
        Difficulty.VERY_HARD -> 10
    }

    var sequence by remember { mutableStateOf(emptyList<Int>()) }
    var userSequence by remember { mutableStateOf(emptyList<Int>()) }
    var isShowingSequence by remember { mutableStateOf(true) }
    var currentShowingIndex by remember { mutableStateOf(-1) }
    var message by remember { mutableStateOf("Memorize the sequence!") }

    LaunchedEffect(Unit) {
        val newSequence = mutableListOf<Int>()
        repeat(squareCount) {
            newSequence.add(Random.nextInt(gridSize * gridSize))
        }
        sequence = newSequence
        
        delay(1000)
        for (i in sequence.indices) {
            currentShowingIndex = sequence[i]
            delay(800)
            currentShowingIndex = -1
            delay(200)
        }
        isShowingSequence = false
        message = "Your turn!"
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(gridSize),
            modifier = Modifier.size(300.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(gridSize * gridSize) { index ->
                val isHighlighted = currentShowingIndex == index
                val isSelected = userSequence.contains(index)
                
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .background(
                            when {
                                isHighlighted -> Color.White
                                isSelected -> Color(0xFF00897B)
                                else -> Color(0xFF1A1A1A)
                            },
                            RoundedCornerShape(8.dp)
                        )
                        .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp))
                        .clickable(enabled = !isShowingSequence) {
                            if (!isSelected) {
                                userSequence = userSequence + index
                                if (userSequence.size == sequence.size) {
                                    if (userSequence == sequence) {
                                        message = "Correct!"
                                        onComplete()
                                    } else {
                                        message = "Wrong! Try again."
                                        userSequence = emptyList()
                                        // Simple reset for this UI-only mission
                                    }
                                }
                            }
                        }
                )
            }
        }
    }
}

@Composable
fun TypingMission(
    sentences: List<String>,
    randomRounds: Int = 0,
    onComplete: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var targetText by remember { mutableStateOf("") }
    var userInput by remember { mutableStateOf("") }
    
    val effectiveSentences = remember(sentences, randomRounds) {
        val list = sentences.toMutableList()
        if (randomRounds > 0) {
            val charPool = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            repeat(randomRounds) {
                val randomString = (1..7)
                    .map { Random.nextInt(0, charPool.length).let { charPool[it] } }
                    .joinToString("")
                list.add(randomString)
            }
        }
        if (list.isEmpty()) list.add("LUMINITY")
        list
    }

    LaunchedEffect(currentIndex) {
        targetText = effectiveSentences[currentIndex].uppercase()
        userInput = ""
    }


    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Sentence ${currentIndex + 1} of ${effectiveSentences.size}", color = Color.Gray, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = targetText,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = userInput,
            onValueChange = { 
                userInput = it.uppercase()
                if (userInput == targetText) {
                    if (currentIndex < effectiveSentences.size - 1) {
                        currentIndex++
                    } else {
                        onComplete()
                    }
                }
            },
            label = { Text("Type exactly as above") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00897B),
                unfocusedBorderColor = Color.DarkGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
    }
}






