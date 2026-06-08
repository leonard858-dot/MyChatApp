package com.example.mychatapp

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mychatapp.network.RetrofitClient
import com.example.mychatapp.security.TokenManager
import kotlinx.coroutines.*
import okhttp3.ResponseBody
import java.io.BufferedReader
import java.io.InputStreamReader

val DarkAIPalette = darkColorScheme(
    background = Color(0xFF080B10),         // Deeper pitch dark
    surface = Color(0xFF111622),            // Card and Header dark grey
    primaryContainer = Color(0xFF00ADB5),   // Glowing Cyan
    onPrimaryContainer = Color(0xFF080B10), // Deep text on Cyan
    secondaryContainer = Color(0xFF181E2C), // Sleek AI bubble grey
    onSecondaryContainer = Color(0xFFE2E8F0),// Clean white/grey text
    error = Color(0xFFFF4C4C)
)

@Composable
fun ChatScreen(tokenManager: TokenManager, onLogout: () -> Unit) {
    val messages = remember { mutableStateListOf<String>() }
    var inputText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, messages.lastOrNull()) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    MaterialTheme(colorScheme = DarkAIPalette) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {

                // --- PREMIUM TOP APP BAR ---
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // AI Avatar Circle
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E2638)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🤖", fontSize = 20.sp)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Title and Node Status
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Jarvis Core",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Glowing online status dot
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00E676))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Local Node Online",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Minimalist Logout Action Button
                        IconButton(
                            onClick = {
                                tokenManager.clearToken()
                                onLogout()
                            }
                        ) {
                            Text("🚪", fontSize = 20.sp) // Accessible icon fallback
                        }
                    }
                }

                // --- CHAT STREAM AREA ---
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    state = listState,
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(messages) { message ->
                        val isUser = message.startsWith("You:")
                        val isThinking = message == "🤖 AI: ..."
                        val displayText = if (isUser) message.removePrefix("You: ").trim() else message.removePrefix("🤖 AI: ").trim()

                        if (isThinking) {
                            ThinkingBubble()
                        } else {
                            MessageBubble(text = displayText, isUser = isUser)
                        }
                    }
                }

                // --- POLISHED INPUT INTERFACE ---
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Send a command...", color = Color.Gray, fontSize = 15.sp) },
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                focusedContainerColor = Color(0xFF080B10),
                                unfocusedContainerColor = Color(0xFF080B10),
                                focusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Circular Sleek Send Button
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (inputText.isNotBlank()) MaterialTheme.colorScheme.primaryContainer
                                    else Color(0xFF1E2638)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (inputText.isNotBlank()) Color.Transparent else Color.DarkGray,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = {
                                    if (inputText.isNotBlank()) {
                                        messages.add("You: $inputText")
                                        val userMessage = inputText
                                        inputText = ""

                                        messages.add("🤖 AI: ...")
                                        val aiMessageIndex = messages.size - 1

                                        scope.launch {
                                            try {
                                                val response = RetrofitClient.api.chatStream(userMessage)
                                                if (response.isSuccessful) {
                                                    response.body()?.let { body ->
                                                        readStream(body, messages, aiMessageIndex)
                                                    }
                                                } else {
                                                    messages[aiMessageIndex] = "🤖 AI: Error ${response.code()}"
                                                }
                                            } catch (e: Exception) {
                                                messages[aiMessageIndex] = "🤖 AI: Connection failed."
                                            }
                                        }
                                    }
                                }
                            ) {
                                Text(
                                    text = "➔",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (inputText.isNotBlank()) MaterialTheme.colorScheme.onPrimaryContainer else Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(text: String, isUser: Boolean) {
    // Premium asymmetric corners setup
    val bubbleShape = if (isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                    shape = bubbleShape
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp,
                color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun ThinkingBubble() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        )
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .alpha(alpha)
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text("Analyzing Command...", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
    }
}

private suspend fun readStream(body: ResponseBody, messages: MutableList<String>, index: Int) {
    withContext(Dispatchers.IO) {
        val reader = BufferedReader(InputStreamReader(body.byteStream()))
        val responseBuilder = StringBuilder()
        var line: String?

        while (reader.readLine().also { line = it } != null) {
            responseBuilder.append(line).append(" ")
            withContext(Dispatchers.Main) {
                messages[index] = "🤖 AI: ${responseBuilder.toString().trim()}"
            }
            delay(40)
        }
    }
}