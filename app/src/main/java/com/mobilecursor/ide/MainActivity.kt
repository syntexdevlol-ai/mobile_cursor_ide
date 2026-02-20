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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.icons.Icons
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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

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
        androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxSize()) {
            androidx.compose.foundation.lazy.items(output) { line ->
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
    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    var prompt by remember { mutableStateOf(TextFieldValue()) }
    var reply by remember { mutableStateOf("AI will reply here. Configure endpoint in code.") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val endpoint = remember { mutableStateOf("https://api.openai.com/v1/chat/completions") }
    val apiKey = remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(
            value = endpoint.value,
            onValueChange = { endpoint.value = it },
            label = { Text("Endpoint") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = apiKey.value,
            onValueChange = { apiKey.value = it },
            label = { Text("API Key (bearer)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Ask AI") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Button(enabled = !loading, onClick = {
            val body = """
                {"model":"gpt-3.5-turbo","messages":[{"role":"user","content":"${prompt.text}"}]}
            """.trimIndent()
            loading = true
            reply = ""
            scope.launch(Dispatchers.IO) {
                val req = Request.Builder()
                    .url(endpoint.value)
                    .addHeader("Authorization", "Bearer ${apiKey.value}")
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create("application/json".toMediaTypeOrNull(), body))
                    .build()
                try {
                    client.newCall(req).execute().use { resp ->
                        reply = if (resp.isSuccessful) {
                            resp.body?.string() ?: "(empty response)"
                        } else {
                            "HTTP ${resp.code}: ${resp.body?.string()}"
                        }
                    }
                } catch (e: Exception) {
                    reply = "error: ${e.message}"
                } finally {
                    loading = false
                }
            }
        }) { Text(if (loading) "Sending..." else "Send") }
        Spacer(Modifier.height(12.dp))
        Text(reply)
    }
}
