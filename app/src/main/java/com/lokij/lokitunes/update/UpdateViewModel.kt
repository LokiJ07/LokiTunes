package com.lokij.lokitunes.update

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class UpdatePhase { IDLE, CHECKING, CURRENT, AVAILABLE, DOWNLOADING, READY, ERROR }

data class UpdateUiState(
    val phase: UpdatePhase = UpdatePhase.IDLE,
    val release: LokiRelease? = null,
    val progress: Int = 0,
    val apk: File? = null,
    val message: String = ""
)

class UpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UpdateRepository(application)
    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()
    private var checkedOnStartup = false

    fun checkForUpdates(silent: Boolean = false) {
        if (silent) {
            if (checkedOnStartup) return
            checkedOnStartup = true
        }
        if (_state.value.phase in listOf(UpdatePhase.CHECKING, UpdatePhase.DOWNLOADING)) return
        viewModelScope.launch {
            _state.update { it.copy(phase = UpdatePhase.CHECKING, message = "") }
            runCatching { withContext(Dispatchers.IO) { repository.latestRelease() } }
                .onSuccess { release ->
                    _state.value = if (release == null) UpdateUiState(UpdatePhase.CURRENT, message = "No newer signed release is available.")
                    else UpdateUiState(UpdatePhase.AVAILABLE, release = release)
                }
                .onFailure {
                    // Offline users keep playing music without interruptions.
                    _state.value = if (silent) UpdateUiState() else UpdateUiState(
                        UpdatePhase.ERROR, message = "Cannot check updates. Connect to the internet and try again."
                    )
                }
        }
    }

    fun downloadRelease() {
        val release = _state.value.release ?: return
        if (_state.value.phase == UpdatePhase.DOWNLOADING) return
        viewModelScope.launch {
            _state.update { it.copy(phase = UpdatePhase.DOWNLOADING, progress = 0, message = "") }
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.download(release) { percent ->
                        _state.update { it.copy(progress = percent) }
                    }
                }
            }.onSuccess { apk ->
                _state.update { it.copy(phase = UpdatePhase.READY, progress = 100, apk = apk) }
            }.onFailure { error ->
                _state.update { it.copy(phase = UpdatePhase.ERROR,
                    message = error.message?.take(160) ?: "Could not download update.") }
            }
        }
    }

    /** Android always asks the person to approve installation. No silent install. */
    fun requestInstallation(context: Context) {
        val file = _state.value.apk ?: return
        if (!file.isFile) {
            _state.update { it.copy(phase = UpdatePhase.ERROR, message = "Downloaded APK not found. Try again.") }
            return
        }
        try {
            if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
                context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                _state.update { it.copy(message = "Enable 'Allow from this source', return, then tap Install again.") }
                return
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: ActivityNotFoundException) {
            _state.update { it.copy(message = "No APK installer found on this device.") }
        } catch (e: Exception) {
            _state.update { it.copy(message = "Could not open the installer: ${e.message?.take(100)}") }
        }
    }
}
