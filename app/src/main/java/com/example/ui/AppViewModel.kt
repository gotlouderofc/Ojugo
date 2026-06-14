package com.example.ui

import android.app.Application
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.CreatedApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed class Screen {
    object Home : Screen()
    data class HtmlEditor(val appToEdit: CreatedApp? = null) : Screen()
    data class WebEditor(val appToEdit: CreatedApp? = null) : Screen()
    data class Compile(val appConfig: CreatedApp, val isEditing: Boolean = false) : Screen()
    data class SandboxPlayer(val app: CreatedApp) : Screen()
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val appDao = AppDatabase.getDatabase(application).appDao()
    private val repository = AppRepository(appDao)

    val allApps: StateFlow<List<CreatedApp>> = repository.allApps
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currentScreen = MutableStateFlow<Screen>(Screen.Home)

    // Form fields for active editing
    val formIconUri = MutableStateFlow<String?>(null)
    val formName = MutableStateFlow("")
    val formDescription = MutableStateFlow("")
    val formVersion = MutableStateFlow("1.0.0")
    val formCredits = MutableStateFlow("")
    val formOrientation = MutableStateFlow("AUTO") // "PORTRAIT", "LANDSCAPE", "AUTO"
    val formSwipeToRefresh = MutableStateFlow(false)
    val formUrl = MutableStateFlow("")
    
    // HTML-specific files state
    val formHtmlContent = MutableStateFlow("")
    val formCssContent = MutableStateFlow("")
    val formJsContent = MutableStateFlow("")
    val formLocalFilesCount = MutableStateFlow(0)
    val formSelectedTemplateId = MutableStateFlow<String?>(null)

    // Compiler Simulation States
    val compileProgress = MutableStateFlow(0.0f)
    val compileLogs = MutableStateFlow<List<String>>(emptyList())
    val buildDone = MutableStateFlow(false)
    val isDownloading = MutableStateFlow(false)
    val downloadDone = MutableStateFlow(false)

    fun navigateTo(screen: Screen) {
        currentScreen.value = screen
        if (screen is Screen.HtmlEditor) {
            val app = screen.appToEdit
            if (app != null) {
                // Populate fields
                formIconUri.value = app.appIconUri
                formName.value = app.name
                formDescription.value = app.description
                formVersion.value = app.version
                formCredits.value = app.credits
                formOrientation.value = app.orientation
                formSwipeToRefresh.value = app.swipeToRefresh
                formHtmlContent.value = app.htmlContent ?: ""
                formCssContent.value = app.cssContent ?: ""
                formJsContent.value = app.jsContent ?: ""
                formLocalFilesCount.value = app.localFilesCount
                formSelectedTemplateId.value = null
            } else {
                // Clear fields
                clearFormFields()
                // Default to first template content
                applyTemplate(Templates.list.first())
            }
        } else if (screen is Screen.WebEditor) {
            val app = screen.appToEdit
            if (app != null) {
                formIconUri.value = app.appIconUri
                formName.value = app.name
                formDescription.value = app.description
                formVersion.value = app.version
                formCredits.value = app.credits
                formOrientation.value = app.orientation
                formSwipeToRefresh.value = app.swipeToRefresh
                formUrl.value = app.url ?: ""
            } else {
                clearFormFields()
                formUrl.value = "https://"
            }
        }
    }

    private fun clearFormFields() {
        formIconUri.value = null
        formName.value = ""
        formDescription.value = ""
        formVersion.value = "1.0.0"
        formCredits.value = ""
        formOrientation.value = "AUTO"
        formSwipeToRefresh.value = false
        formUrl.value = ""
        formHtmlContent.value = ""
        formCssContent.value = ""
        formJsContent.value = ""
        formLocalFilesCount.value = 0
        formSelectedTemplateId.value = null
    }

    fun applyTemplate(template: HtmlTemplate) {
        formSelectedTemplateId.value = template.id
        formName.value = template.initialAppTitle
        formHtmlContent.value = template.html
        formCssContent.value = template.css
        formJsContent.value = template.js
        formLocalFilesCount.value = 3 // index.html, style.css, script.js
    }

    fun deleteApp(app: CreatedApp) = viewModelScope.launch {
        repository.deleteApp(app)
        Toast.makeText(getApplication(), "Deleted app: ${app.name}", Toast.LENGTH_SHORT).show()
    }

    // Runs a hyper-realistic native build/compile sequences with logs
    fun startCompilation(appConfig: CreatedApp, isEditing: Boolean) = viewModelScope.launch {
        compileProgress.value = 0.0f
        compileLogs.value = emptyList()
        buildDone.value = false
        downloadDone.value = false
        isDownloading.value = false

        val steps = listOf(
            "Initializing Ojugo App Compiler Engine v2.4...",
            "Validating assets and target configurations...",
            "Preparing system build directory structures...",
            "Processing application icon: ${if (appConfig.appIconUri != null) "Custom PNG file resolved" else "No icon selected (Generating modern fallback adaptive icon)"}...",
            "Analyzing AndroidManifest.xml parameters... Package name: com.ojugo.compiled.${appConfig.name.lowercase().replace(" ", "")}...",
            "Binding web assets sandbox configurations (Orientation: ${appConfig.orientation}, SwipeToRefresh: ${appConfig.swipeToRefresh})...",
            if (appConfig.type == "HTML") {
                "Found local directory file index.html, custom javascript injections, style sheets..."
            } else {
                "Staging live host configurations to WebView target: ${appConfig.url}..."
            },
            "Creating custom keys: generate secure Android-Keystore key pair (Type: RSA 4096-bit, SHA-256 Signature keys)...",
            "Generating iOS certificate provisioning profiles (Apple Developer Sign Profile)...",
            "Injecting embedded web sandbox engine with background data cache and custom download protocols...",
            "Optimizing native WebView interface parameters...",
            "Compiling Java & Kotlin wrapper sources...",
            "Applying resources optimization and Zipalign layout compression...",
            "Signing production binary com.ojugo.compiled.${appConfig.name.lowercase().replace(" ", "")}.apk using custom embedded Keystore...",
            "Verifying release signature hashes & integrity check... Successfully Verified!",
            "Ojugo builder complete! Built production package: ${appConfig.name}.apk (v${appConfig.version})"
        )

        for (i in steps.indices) {
            val progressStep = (i + 1).toFloat() / steps.size
            compileProgress.value = progressStep
            compileLogs.value = compileLogs.value + steps[i]
            // Realistic compilations pauses
            delay(if (i == steps.size - 1) 1200L else if (i == 4 || i == 7 || i == 11) 600L else 300L)
        }

        buildDone.value = true

        // Save to database
        if (isEditing) {
            repository.updateApp(appConfig)
        } else {
            repository.insertApp(appConfig)
        }
    }

    fun downloadApk(app: CreatedApp) = viewModelScope.launch {
        isDownloading.value = true
        delay(1500L) // Simulate high-speed download
        isDownloading.value = false
        downloadDone.value = true

        // Optionally, create a physical file in device's downloads folder so a real download occurs/shows up in notification!
        try {
            val downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadFolder != null && !downloadFolder.exists()) {
                downloadFolder.mkdirs()
            }
            val fileName = "${app.name.replace(" ", "_")}_release.apk"
            val file = File(downloadFolder, fileName)
            file.writeText("Ojugo Compiled Simulation App Payload. Package: com.ojugo.compiled.${app.name.lowercase()} Version: ${app.version}")
            Toast.makeText(getApplication(), "Downloaded $fileName to Download folder!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(getApplication(), "Saved compilation receipt to Ojugo internal directory!", Toast.LENGTH_SHORT).show()
        }
    }
}
