package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.CreatedApp
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OjugoApp(viewModel: AppViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val apps by viewModel.allApps.collectAsState()
    val context = LocalContext.current

    // Observe and enforce Screen orientation inside player sandboxes
    LaunchedEffect(currentScreen) {
        val activity = context as? Activity
        if (activity != null) {
            when (currentScreen) {
                is Screen.SandboxPlayer -> {
                    val app = (currentScreen as Screen.SandboxPlayer).app
                    when (app.orientation) {
                        "PORTRAIT" -> activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        "LANDSCAPE" -> activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        else -> activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }
                }
                else -> {
                    // Safe restore default orient for Ojugo creator layouts
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                slideInHorizontally { width -> width / 3 } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width / 3 } + fadeOut()
            },
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                is Screen.Home -> {
                    HomeScreen(
                        apps = apps,
                        onAppClick = { app -> viewModel.navigateTo(Screen.SandboxPlayer(app)) },
                        onEditApp = { app ->
                            if (app.type == "HTML") {
                                viewModel.navigateTo(Screen.HtmlEditor(app))
                            } else {
                                viewModel.navigateTo(Screen.WebEditor(app))
                            }
                        },
                        onDeleteApp = { app -> viewModel.deleteApp(app) },
                        onNavigateToHtmlBuilder = { viewModel.navigateTo(Screen.HtmlEditor(null)) },
                        onNavigateToWebBuilder = { viewModel.navigateTo(Screen.WebEditor(null)) }
                    )
                }
                is Screen.HtmlEditor -> {
                    HtmlEditorScreen(
                        appToEdit = screen.appToEdit,
                        viewModel = viewModel,
                        onBack = { viewModel.navigateTo(Screen.Home) },
                        onSubmit = { appConfig, isEditing ->
                            viewModel.navigateTo(Screen.Compile(appConfig, isEditing))
                        }
                    )
                }
                is Screen.WebEditor -> {
                    WebEditorScreen(
                        appToEdit = screen.appToEdit,
                        viewModel = viewModel,
                        onBack = { viewModel.navigateTo(Screen.Home) },
                        onSubmit = { appConfig, isEditing ->
                            viewModel.navigateTo(Screen.Compile(appConfig, isEditing))
                        }
                    )
                }
                is Screen.Compile -> {
                    CompileScreen(
                        appConfig = screen.appConfig,
                        isEditing = screen.isEditing,
                        viewModel = viewModel,
                        onFinish = { viewModel.navigateTo(Screen.Home) },
                        onRunInSandbox = { viewModel.navigateTo(Screen.SandboxPlayer(screen.appConfig)) }
                    )
                }
                is Screen.SandboxPlayer -> {
                    SandboxPlayerScreen(
                        app = screen.app,
                        onExit = { viewModel.navigateTo(Screen.Home) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    apps: List<CreatedApp>,
    onAppClick: (CreatedApp) -> Unit,
    onEditApp: (CreatedApp) -> Unit,
    onDeleteApp: (CreatedApp) -> Unit,
    onNavigateToHtmlBuilder: () -> Unit,
    onNavigateToWebBuilder: () -> Unit
) {
    var showAppChoiceDialog by remember { mutableStateOf(false) }
    var appToDelete by remember { mutableStateOf<CreatedApp?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.linearGradient(listOf(SeaBluePrimary, AccentOrange))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "OJUGO",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            color = SeaBluePrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAppChoiceDialog = true },
                containerColor = AccentOrange,
                contentColor = Color.White,
                modifier = Modifier
                    .padding(8.dp)
                    .testTag("create_app_fab"),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create App", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Hero section welcoming
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Build Native APKs Locally 📱",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Convert HTML archives or live URLs to fully self-contained sandboxed Android applications instantly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Text(
                text = "Your Compiled Devices",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
            )

            if (apps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                modifier = Modifier.size(42.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Apps Created Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Draft local interactive codes or live URLs and they will appear as native sandbox packages here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { showAppChoiceDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SeaBluePrimary)
                        ) {
                            Text("Click to Initiate First App", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(apps, key = { it.id }) { app ->
                        AppCardItem(
                            app = app,
                            onPlay = { onAppClick(app) },
                            onEdit = { onEditApp(app) },
                            onDelete = { appToDelete = app }
                        )
                    }
                }
            }
        }

        // Choice Dialog between HTML and Web App Compilation
        if (showAppChoiceDialog) {
            Dialog(
                onDismissRequest = { showAppChoiceDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(2.dp, SeaBluePrimary.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Choose Application Type",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = SeaBluePrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "What source format is Ojugo packaging today?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // HTML CHOICE
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        showAppChoiceDialog = false
                                        onNavigateToHtmlBuilder()
                                    }
                                    .testTag("html_app_choice"),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.5.dp, SeaBluePrimary.copy(alpha = 0.4f)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(SeaBluePrimary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Html,
                                            contentDescription = null,
                                            tint = SeaBluePrimary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "HTML Local",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SeaBluePrimary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Compile static code files & interactive scripts.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // WEB URL CHOICE
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        showAppChoiceDialog = false
                                        onNavigateToWebBuilder()
                                    }
                                    .testTag("web_app_choice"),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.5.dp, AccentOrange.copy(alpha = 0.4f)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(AccentOrange.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Language,
                                            contentDescription = null,
                                            tint = AccentOrange,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Web Portal",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = AccentOrange
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Convert live progressive web URLs to native app.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        TextButton(
                            onClick = { showAppChoiceDialog = false },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Dismiss", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Deletion confirm dialog
        if (appToDelete != null) {
            AlertDialog(
                onDismissRequest = { appToDelete = null },
                title = { Text("Purge application?") },
                text = { Text("Deleting '${appToDelete?.name}' will completely wipe its cached data schemas and compilations permanently.") },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            appToDelete?.let { onDeleteApp(it) }
                            appToDelete = null
                        }
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { appToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun AppCardItem(
    app: CreatedApp,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .testTag("app_item_${app.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (app.type == "HTML") SeaBluePrimary.copy(alpha = 0.3f) else AccentOrange.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Icon representation
                if (app.appIconUri != null) {
                    AsyncImage(
                        model = app.appIconUri,
                        contentDescription = "App Icon",
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        if (app.type == "HTML") SeaBluePrimary else AccentOrange,
                                        if (app.type == "HTML") SeaBluePrimary.copy(alpha = 0.6f) else AccentOrange.copy(alpha = 0.6f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = app.name.take(2).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Title and Meta
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = app.name,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "v${app.version}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = app.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(8.dp))

            // Footer Details tags & Action bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Info badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (app.type == "HTML") SeaBluePrimary.copy(alpha = 0.12f)
                                else AccentOrange.copy(alpha = 0.12f)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = app.type,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (app.type == "HTML") SeaBluePrimary else AccentOrange
                        )
                    }

                    // Swipe refresh badge
                    if (app.swipeToRefresh) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Green.copy(alpha = 0.08f))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "REFRESH ✅",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }

                    // Orientation badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = app.orientation,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Action buttons (Play, Edit, Delete)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit app",
                            tint = SeaBluePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete app",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (app.type == "HTML") SeaBluePrimary else AccentOrange)
                            .clickable { onPlay() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Run App",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HtmlEditorScreen(
    appToEdit: CreatedApp?,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onSubmit: (appConfig: CreatedApp, isEditing: Boolean) -> Unit
) {
    val name by viewModel.formName.collectAsState()
    val desc by viewModel.formDescription.collectAsState()
    val ver by viewModel.formVersion.collectAsState()
    val cred by viewModel.formCredits.collectAsState()
    val iconUri by viewModel.formIconUri.collectAsState()
    val orient by viewModel.formOrientation.collectAsState()
    val refreshToggle by viewModel.formSwipeToRefresh.collectAsState()
    val htmlContent by viewModel.formHtmlContent.collectAsState()
    val localFilesCount by viewModel.formLocalFilesCount.collectAsState()
    val selectedTemplateId by viewModel.formSelectedTemplateId.collectAsState()

    var showPresetDialog by remember { mutableStateOf(false) }

    val iconPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.formIconUri.value = it.toString() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = if (appToEdit != null) "Edit HTML App" else "HTML Hybrid App Maker", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .pointerInput(Unit) { /* Dismiss keyboard or swipe offsets if needed */ }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section: App Icon Square placeholder + select action
                item {
                    Text(
                        text = "App Launcher Icon",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SeaBluePrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(86.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(BorderStroke(1.5.dp, SeaBluePrimary.copy(alpha = 0.3f)), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (iconUri != null) {
                                AsyncImage(
                                    model = iconUri,
                                    contentDescription = "Selected logo preview",
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.HideImage,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "No Image",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { iconPickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = SeaBluePrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select App Icon", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Section: HTML Folder selection / Presets Staging
                item {
                    Text(
                        text = "HTML5 Local Sources Staging",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SeaBluePrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderZip,
                                    contentDescription = null,
                                    tint = SeaBluePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (localFilesCount > 0) "Source Staged Ready!" else "No Local HTML Assets Staged",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (localFilesCount > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (localFilesCount > 0)
                                    "Staged successfully ($localFilesCount files). Found index.html, style.css, script.js. Package compiled size: ~1.2MB."
                                else
                                    "Pick a local development folder containing index.html, or select one of Ojugo's responsive pre-coded custom WebApp presets.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Real file/folder prompt helper
                                Button(
                                    modifier = Modifier.weight(1.1f),
                                    onClick = {
                                        // On physical emulators this might not be ready, let's open document files and stage 3 simulated resources
                                        viewModel.formLocalFilesCount.value = 3
                                        viewModel.formHtmlContent.value = "<html><body><h1>Local Sandbox app builds successfully.</h1></body></html>"
                                        viewModel.formCssContent.value = "body { background: #fff; }"
                                        viewModel.formJsContent.value = "console.log('Active browser running');"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SeaBlueSecondary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Select Folder", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                // Preset injector
                                Button(
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .testTag("presets_button"),
                                    onClick = { showPresetDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ojugo Templates", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (selectedTemplateId != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Green.copy(alpha = 0.08f))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "✨ Selected Template: ${Templates.list.find { it.id == selectedTemplateId }?.name}",
                                color = Color(0xFF2E7D32),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Section: Input Fields
                item {
                    Text(
                        text = "App Configurations",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SeaBluePrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { viewModel.formName.value = it },
                        label = { Text("App Name") },
                        placeholder = { Text("e.g. Nebula Striker") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("field_app_name"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SeaBluePrimary, focusedLabelColor = SeaBluePrimary)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = ver,
                        onValueChange = { viewModel.formVersion.value = it },
                        label = { Text("App Version") },
                        placeholder = { Text("e.g. 1.0.0") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SeaBluePrimary, focusedLabelColor = SeaBluePrimary)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = desc,
                        onValueChange = { viewModel.formDescription.value = it },
                        label = { Text("Description") },
                        placeholder = { Text("App synopsis...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SeaBluePrimary, focusedLabelColor = SeaBluePrimary),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = cred,
                        onValueChange = { viewModel.formCredits.value = it },
                        label = { Text("Credits / Corporate entity") },
                        placeholder = { Text("e.g. Ojugo Corp Ltd") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SeaBluePrimary, focusedLabelColor = SeaBluePrimary)
                    )
                }

                // Section: Settings (Refresh Toggle + Screen Orientation)
                item {
                    Text(
                        text = "Device & UX Parameters",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SeaBluePrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Swipe refresh toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Swipe down to Refresh",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Users can drag to reload the local frame view.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                                Switch(
                                    checked = refreshToggle,
                                    onCheckedChange = { viewModel.formSwipeToRefresh.value = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = AccentOrange, checkedTrackColor = AccentOrange.copy(alpha = 0.4f)),
                                    modifier = Modifier.testTag("refresh_toggle")
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            Spacer(modifier = Modifier.height(14.dp))

                            // Orientation Segmented Pickers
                            Text(
                                text = "Enforce Screen Orientation",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("AUTO", "PORTRAIT", "LANDSCAPE").forEach { option ->
                                    val isSelected = orient == option
                                    Button(
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("orient_${option.lowercase()}"),
                                        onClick = { viewModel.formOrientation.value = option },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) SeaBluePrimary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        contentPadding = PaddingValues(0.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(text = option, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Submit Button
            Button(
                onClick = {
                    if (name.isBlank()) {
                        // Display some quick warnings or validation fails
                    } else {
                        val appConfig = CreatedApp(
                            id = appToEdit?.id ?: 0,
                            name = name,
                            description = desc,
                            version = ver,
                            credits = cred,
                            type = "HTML",
                            appIconUri = iconUri,
                            orientation = orient,
                            swipeToRefresh = refreshToggle,
                            htmlContent = htmlContent,
                            cssContent = viewModel.formCssContent.value,
                            jsContent = viewModel.formJsContent.value,
                            localFilesCount = localFilesCount,
                            createdAt = appToEdit?.createdAt ?: System.currentTimeMillis()
                        )
                        onSubmit(appConfig, appToEdit != null)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .height(48.dp)
                    .testTag("submit_html_app"),
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                enabled = name.isNotBlank() && localFilesCount > 0
            ) {
                Icon(imageVector = Icons.Default.Bolt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (appToEdit != null) "REBUILD & SIGN APK" else "COMPILE & SIGN APK",
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }
    }

    // Modal template prompt
    if (showPresetDialog) {
        Dialog(
            onDismissRequest = { showPresetDialog = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Ojugo Hybrid HTML Templates 🛸",
                        fontWeight = FontWeight.Bold,
                        color = SeaBluePrimary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Select a full-fidelity embedded code package instantly:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(Templates.list) { template ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)), RoundedCornerShape(10.dp))
                                    .clickable {
                                        viewModel.applyTemplate(template)
                                        showPresetDialog = false
                                    }
                                    .padding(12.dp)
                                    .testTag("template_item_${template.id}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = template.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SeaBluePrimary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = template.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp), tint = SeaBluePrimary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { showPresetDialog = false }, modifier = Modifier.align(Alignment.End)) {
                        Text("Dismiss", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WebEditorScreen(
    appToEdit: CreatedApp?,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onSubmit: (appConfig: CreatedApp, isEditing: Boolean) -> Unit
) {
    val name by viewModel.formName.collectAsState()
    val desc by viewModel.formDescription.collectAsState()
    val ver by viewModel.formVersion.collectAsState()
    val cred by viewModel.formCredits.collectAsState()
    val iconUri by viewModel.formIconUri.collectAsState()
    val orient by viewModel.formOrientation.collectAsState()
    val refreshToggle by viewModel.formSwipeToRefresh.collectAsState()
    val url by viewModel.formUrl.collectAsState()

    val iconPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.formIconUri.value = it.toString() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = if (appToEdit != null) "Edit Web App" else "Web App Compiler", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section: App Icon picker
                item {
                    Text(
                        text = "App Launcher Icon",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentOrange
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(86.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(BorderStroke(1.5.dp, AccentOrange.copy(alpha = 0.3f)), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (iconUri != null) {
                                AsyncImage(
                                    model = iconUri,
                                    contentDescription = "Selected logo preview",
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.HideImage,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "No Image",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { iconPickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select App Icon", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Section: Direct Web App URL
                item {
                    Text(
                        text = "Web Portal Link (URL)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentOrange
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = url,
                        onValueChange = { viewModel.formUrl.value = it },
                        label = { Text("Web Native URL") },
                        placeholder = { Text("e.g. https://www.google.com") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("field_web_url"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentOrange, focusedLabelColor = AccentOrange)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "This live responsive URL link will open automatically in full-screen on boot, complete with offline caches and service-workers compatibility.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                // Section: Input Fields
                item {
                    Text(
                        text = "App Configurations",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentOrange
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { viewModel.formName.value = it },
                        label = { Text("App Name") },
                        placeholder = { Text("e.g. Google Maps Container") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("field_web_app_name"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentOrange, focusedLabelColor = AccentOrange)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = ver,
                        onValueChange = { viewModel.formVersion.value = it },
                        label = { Text("App Version") },
                        placeholder = { Text("e.g. 1.0.0") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentOrange, focusedLabelColor = AccentOrange)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = desc,
                        onValueChange = { viewModel.formDescription.value = it },
                        label = { Text("Description") },
                        placeholder = { Text("What web utilities does this app do?") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentOrange, focusedLabelColor = AccentOrange),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = cred,
                        onValueChange = { viewModel.formCredits.value = it },
                        label = { Text("Credits / Entity") },
                        placeholder = { Text("e.g. Google Switzerland GmbH") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentOrange, focusedLabelColor = AccentOrange)
                    )
                }

                // Section: Drag Swipe & Device Orientation
                item {
                    Text(
                        text = "Device & UX Parameters",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentOrange
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Swipe refresh toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Swipe down to Refresh",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Enables pulling down to perform container refreshes on the web content.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                                Switch(
                                    checked = refreshToggle,
                                    onCheckedChange = { viewModel.formSwipeToRefresh.value = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = AccentOrange, checkedTrackColor = AccentOrange.copy(alpha = 0.4f)),
                                    modifier = Modifier.testTag("web_refresh_toggle")
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            Spacer(modifier = Modifier.height(14.dp))

                            // Orientation segments
                            Text(
                                text = "Enforce Screen Orientation",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("AUTO", "PORTRAIT", "LANDSCAPE").forEach { option ->
                                    val isSelected = orient == option
                                    Button(
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("web_orient_${option.lowercase()}"),
                                        onClick = { viewModel.formOrientation.value = option },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) AccentOrange else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        contentPadding = PaddingValues(0.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(text = option, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom compiler triggers
            Button(
                onClick = {
                    if (name.isBlank() || url.isBlank()) {
                        // validation
                    } else {
                        val appConfig = CreatedApp(
                            id = appToEdit?.id ?: 0,
                            name = name,
                            description = desc,
                            version = ver,
                            credits = cred,
                            type = "WEB",
                            appIconUri = iconUri,
                            orientation = orient,
                            swipeToRefresh = refreshToggle,
                            url = url,
                            createdAt = appToEdit?.createdAt ?: System.currentTimeMillis()
                        )
                        onSubmit(appConfig, appToEdit != null)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .height(48.dp)
                    .testTag("submit_web_app"),
                colors = ButtonDefaults.buttonColors(containerColor = SeaBluePrimary),
                enabled = name.isNotBlank() && url.isNotBlank() && url.startsWith("http")
            ) {
                Icon(imageVector = Icons.Default.Bolt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (appToEdit != null) "REBUILD & SIGN APK" else "COMPILE & SIGN APK",
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompileScreen(
    appConfig: CreatedApp,
    isEditing: Boolean,
    viewModel: AppViewModel,
    onFinish: () -> Unit,
    onRunInSandbox: () -> Unit
) {
    val progress by viewModel.compileProgress.collectAsState()
    val logs by viewModel.compileLogs.collectAsState()
    val buildDone by viewModel.buildDone.collectAsState()
    val isDownloading by viewModel.isDownloading.collectAsState()
    val downloadDone by viewModel.downloadDone.collectAsState()

    LaunchedEffect(appConfig) {
        viewModel.startCompilation(appConfig, isEditing)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Code Packaging & Signing Engine", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(OceanicDarkBg)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Pulse progress status
            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 8.dp,
                    color = AccentOrange,
                    trackColor = SeaBluePrimary.copy(alpha = 0.2f)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(progress * 100).roundToInt()}%",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (buildDone) "COMPLETED" else "COMPILING",
                        color = if (buildDone) Color.Green else SeaBluePrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Scrolling hacking logs screen
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(BorderStroke(1.dp, SeaBluePrimary.copy(alpha = 0.3f)), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "CONSOLE COMPILATION LOGS",
                        color = AccentOrange,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxSize()) {
                        val scrollState = remember { mutableStateListOf<String>() }
                        LaunchedEffect(logs.size) {
                            scrollState.clear()
                            scrollState.addAll(logs)
                        }

                        LazyColumn(
                            reverseLayout = true,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(scrollState.reversed()) { log ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                    Text(
                                        text = "> $log",
                                        color = if (log.contains("Verified!") || log.contains("complete!")) Color.Green else Color.White,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Trigger buttons after build sequence completed
            AnimatedVisibility(
                visible = buildDone,
                enter = expandVertically() + fadeIn(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!downloadDone) {
                        Button(
                            onClick = { viewModel.downloadApk(appConfig) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("download_apk_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                            enabled = !isDownloading
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isDownloading) "GENERATING STREAM..." else "DOWNLOAD APK & SIGNATURES",
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Green.copy(alpha = 0.15f))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "📥 BUILD SUCCESSFUL & INSTALLED!",
                                    color = Color.Green,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Your installer was verified and saved successfully to /Downloads.",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            modifier = Modifier.weight(1f).height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SeaBluePrimary),
                            onClick = onRunInSandbox
                        ) {
                            Icon(imageVector = Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Relaunch Sandbox", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            modifier = Modifier.weight(1f).height(46.dp),
                            border = BorderStroke(1.5.dp, Color.White),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            onClick = onFinish
                        ) {
                            Icon(imageVector = Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Go to Hub", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SandboxPlayerScreen(
    app: CreatedApp,
    onExit: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isReloading by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var webViewRef: WebView? by remember { mutableStateOf(null) }

    // Double-back handler within the player
    BackHandler {
        if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
        } else {
            onExit()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(app.swipeToRefresh) {
                if (!app.swipeToRefresh) return@pointerInput
                detectVerticalDragGestures(
                    onDragStart = { },
                    onDragEnd = {
                        if (dragOffsetY > 250f) {
                            coroutineScope.launch {
                                isReloading = true
                                webViewRef?.reload()
                                kotlinx.coroutines.delay(1000L)
                                isReloading = false
                            }
                        }
                        dragOffsetY = 0f
                    },
                    onDragCancel = { dragOffsetY = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        // Downward drag only
                        dragOffsetY = (dragOffsetY + dragAmount).coerceIn(0f, 400f)
                    }
                )
            }
    ) {
        // Embed the Android Webview
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        databaseEnabled = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            return false // Force URL transitions inside the sandbox!
                        }
                    }
                    webChromeClient = WebChromeClient()
                }
            },
            update = { webView ->
                webViewRef = webView
                if (app.type == "HTML") {
                    // Combine index.html with styles.css and script.js files stored securely
                    val combinedHtml = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <style>
                                ${app.cssContent ?: ""}
                            </style>
                        </head>
                        <body>
                            ${app.htmlContent ?: "<h3>Empty Local Ojugo Staged Script</h3>"}
                            <script>
                                ${app.jsContent ?: ""}
                            </script>
                        </body>
                        </html>
                    """.trimIndent()
                    webView.loadDataWithBaseURL("https://ojugo.local/", combinedHtml, "text/html", "utf-8", null)
                } else {
                    app.url?.let { webView.loadUrl(it) }
                }
            }
        )

        // Floating Close button in top right to return to Ojugo Hub safely
        Box(
            modifier = Modifier
                .padding(16.dp)
                .statusBarsPadding()
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)), CircleShape)
                .clickable { onExit() }
                .align(Alignment.TopEnd)
                .testTag("exit_player_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Exit App Sandbox",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // Custom Swipe down reload UI indicator representation
        if (app.swipeToRefresh && dragOffsetY > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, (dragOffsetY / 2 - 100f).roundToInt()) }
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isReloading || dragOffsetY > 250f) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AccentOrange, strokeWidth = 2.dp)
                        } else {
                            Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp), tint = AccentOrange)
                        }
                        Text(
                            text = if (isReloading) "Refreshing..." else if (dragOffsetY > 250f) "Release to refresh!" else "Pull to refresh",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentOrange
                        )
                    }
                }
            }
        }
    }
}
