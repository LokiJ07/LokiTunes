package com.lokij.lokitunes

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.lokij.lokitunes.ui.LokiApp
import com.lokij.lokitunes.ui.LokiViewModel
import com.lokij.lokitunes.ui.theme.LokiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LokiTheme {
                val vm: LokiViewModel = viewModel()
                val state by vm.state.collectAsState()
                val context = LocalContext.current
                val audioPermission = if (Build.VERSION.SDK_INT >= 33) {
                    Manifest.permission.READ_MEDIA_AUDIO
                } else Manifest.permission.READ_EXTERNAL_STORAGE
                var permitted by remember {
                    mutableStateOf(ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED)
                }
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            permitted = ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }
                val requestAudioPermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted -> permitted = granted }

                LaunchedEffect(permitted) {
                    if (permitted) vm.refresh()
                }

                if (permitted) {
                    LokiApp(state = state, vm = vm)
                } else {
                    PermissionPage(
                        onRequest = { requestAudioPermission.launch(audioPermission) },
                        onOpenSettings = {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionPage(onRequest: () -> Unit, onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("♫", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("Your music. Your rules.", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(12.dp))
        Text(
            "LokiTunes needs permission to read the songs already on your device. Your files stay on your phone — no account or internet required.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(28.dp))
        Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) { Text("Allow access to music") }
        TextButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Text("Open app settings if permission was denied")
        }
    }
}
