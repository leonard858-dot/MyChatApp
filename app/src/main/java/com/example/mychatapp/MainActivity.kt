package com.example.mychatapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mychatapp.network.RetrofitClient
import com.example.mychatapp.security.TokenManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tokenManager = TokenManager(this)
        setContent {
            ChatApp(tokenManager)
        }
    }
}

@Composable
fun ChatApp(tokenManager: TokenManager) {
    var isLoggedIn by remember { mutableStateOf(tokenManager.getToken() != null) }

    if (isLoggedIn) {
        ChatScreen(tokenManager) { isLoggedIn = false }
    } else {
        LoginScreen(tokenManager) { isLoggedIn = true }
    }
}

@Composable
fun LoginScreen(tokenManager: TokenManager, onLoginSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                scope.launch {
                    try {
                        val response = RetrofitClient.api.authenticate(username, password)
                        tokenManager.saveToken(response.access_token)
                        onLoginSuccess()
                    } catch (e: Exception) {
                        error = "Login failed: ${e.message}"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }
        if (error.isNotEmpty()) {
            Text(text = error, color = MaterialTheme.colorScheme.error)
        }
    }
}