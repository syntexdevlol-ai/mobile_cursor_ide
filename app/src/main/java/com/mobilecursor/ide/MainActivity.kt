package com.mobilecursor.ide

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                var tab by remember { mutableStateOf(0) }
                val tabs = listOf("Editor", "Terminal", "AI")

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = tab == 0,
                                onClick = { tab = 0 },
                                label = { Text("Editor") },
                                icon = { Icon(Icons.Default.Code, contentDescription = null) }
                            )
                            NavigationBarItem(
                                selected = tab == 1,
                                onClick = { tab = 1 },
                                label = { Text("Terminal") },
                                icon = { Icon(Icons.Default.Terminal, contentDescription = null) }
                            )
                            NavigationBarItem(
                                selected = tab == 2,
                                onClick = { tab = 2 },
                                label = { Text("AI") },
                                icon = { Icon(Icons.Default.Chat, contentDescription = null) }
                            )
                        }
                    }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        when (tab) {
                            0 -> EditorView()
                            1 -> TerminalView()
                            else -> AiView()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditorView() {
    val context = LocalContext.current
    AndroidView(
        factory = {
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.allowFileAccess = true
                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()
                loadUrl("file:///android_asset/monaco.html")
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun TerminalView() {
    var command by remember { mutableStateOf("") }
    var output by remember { mutableStateOf(listOf<String>()) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(
            value = command,
            onValueChange = { command = it },
            placeholder = { Text("Enter command (runs in /system/bin/sh)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            val cmd = command
            command = ""
            scope.launch(Dispatchers.IO) {
                val result = runShell(cmd)
                output = output + (listOf("">< $cmd") + result)
            }
        }) { Text("Run") }
        Spacer(Modifier.height(12.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(output) { line ->
                Text(line, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun runShell(cmd: String): List<String> {
    if (cmd.isBlank()) return listOf()
    return try {
        val process = ProcessBuilder("/system/bin/sh", "-c", cmd)
            .redirectErrorStream(true)
            .start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val lines = reader.readLines()
        process.waitFor()
        lines
    } catch (e: Exception) {
        listOf("error: ${e.message}")
    }
}

@Composable
fun AiView() {
    var prompt by remember { mutableStateOf(TextFieldValue()) }
    var reply by remember { mutableStateOf("AI will reply here. Wire this to your backend.") }
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Ask AI") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            reply = "(stub) You said: ${prompt.text}\nConnect this to your API and stream responses."
        }) { Text("Send") }
        Spacer(Modifier.height(12.dp))
        Text(reply)
    }
}
