package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.authStateFlow
import com.example.auth.signOutFromGoogle
import com.example.data.ClientStorageRepository
import com.example.data.CloudBackupRepository
import com.example.ui.components.FlarialSplashScreen
import com.example.ui.components.FloatingClickGuiModal
import com.example.ui.screens.AddonManagerScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HudAndCrosshairScreen
import com.example.ui.screens.ModulesScreen
import com.example.ui.screens.ProfilesAndScriptsScreen
import com.example.ui.theme.FlarialClientTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextSecondary
import com.example.util.MinecraftLauncherHelper
import com.example.viewmodel.CloudSyncUiState
import com.example.viewmodel.FlarialClientViewModel
import com.example.viewmodel.FlarialUiState
import com.example.viewmodel.MainNavTab
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlarialAppNavigation()
        }
    }
}

@Composable
fun FlarialAppNavigation(auth: FirebaseAuth = Firebase.auth) {
    // Direct Access Mode: Account gate is skipped by default so the user enters the
    // Pocket Edition & Multi-Version Launcher immediately with zero startup lag.
    // If the user signs in with Google (for Firestore Cloud Sync), currentUser updates reactively.
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    val viewModelKey = user?.uid ?: "direct_access_local_session"

    val clientViewModel: FlarialClientViewModel = viewModel(
        key = viewModelKey,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val cloudRepo = runCatching {
                    val databaseId = app.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(databaseId)
                    CloudBackupRepository(db)
                }.getOrNull()
                FlarialClientViewModel(
                    repo = ClientStorageRepository(app.applicationContext),
                    cloudRepo = cloudRepo,
                    currentUserId = user?.uid
                )
            }
        }
    )

    val state by clientViewModel.uiState.collectAsStateWithLifecycle()
    val cloudSyncState by clientViewModel.cloudBackupsState.collectAsStateWithLifecycle()

    val primaryAccent = remember(state.accentPreset) {
        runCatching {
            Color(android.graphics.Color.parseColor(state.accentPreset.primaryHex))
        }.getOrDefault(NeonCyan)
    }
    val secondaryAccent = remember(state.accentPreset) {
        runCatching {
            Color(android.graphics.Color.parseColor(state.accentPreset.secondaryHex))
        }.getOrDefault(NeonViolet)
    }

    FlarialClientTheme(
        accentColor = primaryAccent,
        secondaryAccent = secondaryAccent
    ) {
        FlarialClientRootApp(
            state = state,
            cloudSyncState = cloudSyncState,
            userEmail = user?.email ?: user?.displayName,
            viewModel = clientViewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlarialClientRootApp(
    state: FlarialUiState,
    cloudSyncState: CloudSyncUiState,
    userEmail: String?,
    viewModel: FlarialClientViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Refresh package detection & SYSTEM_ALERT_WINDOW permission on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshInstallAndPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // BackHandler for non-home tabs or ClickGUI modal
    BackHandler(enabled = state.showInAppClickGui || state.currentTab != MainNavTab.DASHBOARD) {
        if (state.showInAppClickGui) {
            viewModel.setInAppClickGuiVisible(false)
        } else {
            viewModel.selectTab(MainNavTab.DASHBOARD)
        }
    }

    // Activity Result Launchers for Scoped Storage & Overlay Permission
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshInstallAndPermissions()
        if (MinecraftLauncherHelper.hasOverlayPermission(context)) {
            viewModel.showBannerMessage("Floating Overlay permission granted! Tap 'Start Overlay' to launch ClickGUI.")
        }
    }

    val importAddonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importAddonFromUri(uri)
        }
    }

    val exportAddonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            viewModel.exportPendingAddonToUri(uri)
        }
    }

    val linkMojangFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.setLinkedMojangSafUri(uri)
        }
    }

    val exportBackupJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackupJsonToUri(uri)
        }
    }

    val restoreBackupJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.restoreBackupJsonFromUri(uri)
        }
    }

    if (state.showSplash) {
        FlarialSplashScreen(
            onSkipSplash = { viewModel.dismissSplashNow() }
        )
        return
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        val isWideTablet = maxWidth >= 680.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = ObsidianBg,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ObsidianSurface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "FLARIAL CLIENT",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = NeonEmerald.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = "DIRECT ACCESS",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeonEmerald,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${state.liveFps} FPS • MC ${state.selectedVersionTier.shortLabel} • ${state.modules.count { it.enabled }} Mods",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    actions = {
                        // Quick ClickGUI Pill in TopAppBar
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                                .clickable { viewModel.toggleInAppClickGui() }
                                .testTag("topbar_clickgui_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Open ClickGUI",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ClickGUI",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Quick Launch Game Icon
                        IconButton(
                            onClick = { viewModel.launchMinecraftGame(true) },
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .size(36.dp)
                                .testTag("topbar_launch_game_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Launch Minecraft",
                                tint = ObsidianBg
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!isWideTablet) {
                    NavigationBar(
                        containerColor = ObsidianSurface,
                        tonalElevation = 8.dp
                    ) {
                        MainNavTab.entries.forEach { tab ->
                            val selected = state.currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon(),
                                        contentDescription = tab.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ObsidianBg,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.toggleInAppClickGui() },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = Color.White,
                    icon = {
                        Icon(Icons.Default.Bolt, contentDescription = "Toggle Floating ClickGUI")
                    },
                    text = {
                        Text(
                            text = if (state.isSystemOverlayRunning) "Overlay Active" else "ClickGUI",
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    modifier = Modifier.testTag("fab_floating_clickgui")
                )
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideTablet) {
                    NavigationRail(
                        containerColor = ObsidianSurface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        MainNavTab.entries.forEach { tab ->
                            val selected = state.currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon(),
                                        contentDescription = tab.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.title,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ObsidianBg,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("rail_tab_${tab.route}")
                            )
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Status / Notification Banner
                    AnimatedVisibility(visible = state.statusBannerMessage != null) {
                        state.statusBannerMessage?.let { msg ->
                            Surface(
                                color = ObsidianCard,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .border(1.dp, NeonEmerald, RoundedCornerShape(12.dp))
                                    .clip(RoundedCornerShape(12.dp))
                                    .testTag("status_banner")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NeonEmerald,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.clearBannerMessage() },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss notification",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Main Tab Content
                    AnimatedContent(
                        targetState = state.currentTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "main_tab_transition",
                        modifier = Modifier.fillMaxSize()
                    ) { tab ->
                        when (tab) {
                            MainNavTab.DASHBOARD -> DashboardScreen(
                                state = state,
                                onLaunchMinecraft = { withOverlay, serverHost ->
                                    viewModel.launchMinecraftGame(withOverlay, serverHost)
                                },
                                onSelectInstance = { viewModel.selectLauncherInstance(it) },
                                onSelectVersionTier = { viewModel.selectVersionTier(it) },
                                onOptimizeZeroLag = { viewModel.runSmartEngineOptimization() },
                                onUpdatePlayerSettings = { tag, cosm, chunks, boost ->
                                    viewModel.updateLauncherPlayerSettings(tag, cosm, chunks, boost)
                                },
                                onAddCustomServer = { name, host, port ->
                                    viewModel.addCustomServer(name, host, port)
                                },
                                onOpenPlayStore = { MinecraftLauncherHelper.openPlayStoreForMinecraft(context) },
                                onRefreshDetection = { viewModel.refreshInstallAndPermissions() },
                                onRequestOverlayPermission = {
                                    runCatching {
                                        overlayPermissionLauncher.launch(
                                            MinecraftLauncherHelper.buildOverlayPermissionIntent(context)
                                        )
                                    }.onFailure {
                                        viewModel.showBannerMessage("Enable 'Display over other apps' for Flarial Client in Android Settings.")
                                    }
                                },
                                onToggleSystemOverlay = { viewModel.toggleSystemFloatingOverlay() },
                                onOpenClickGui = { viewModel.setInAppClickGuiVisible(true) },
                                onToggleModule = { viewModel.toggleModule(it) },
                                onNavigateTab = { viewModel.selectTab(it) }
                            )

                            MainNavTab.MODULES -> ModulesScreen(
                                state = state,
                                onSelectCategory = { viewModel.selectModuleCategory(it) },
                                onSearchQueryChange = { viewModel.updateModuleSearch(it) },
                                onToggleModule = { viewModel.toggleModule(it) },
                                onToggleExpandModule = { viewModel.toggleExpandModule(it) },
                                onSliderChange = { modId, key, v -> viewModel.updateModuleSlider(modId, key, v) },
                                onScaleChange = { modId, scaleVal -> viewModel.updateModuleScale(modId, scaleVal) },
                                onToggleSettingChange = { modId, key, v -> viewModel.updateModuleToggleSetting(modId, key, v) },
                                onChoiceSettingChange = { modId, key, opt -> viewModel.updateModuleChoiceSetting(modId, key, opt) },
                                onSetAllModulesEnabled = { viewModel.setAllModulesEnabled(it) },
                                onResetHudPositions = { viewModel.resetHudPositions() },
                                onOptimizeEngine = { viewModel.runSmartEngineOptimization() }
                            )

                            MainNavTab.HUD_CROSSHAIR -> HudAndCrosshairScreen(
                                state = state,
                                onDragHudWidget = { modId, dx, dy -> viewModel.updateModuleHudPosition(modId, dx, dy) },
                                onResetHudLayout = { viewModel.resetHudPositions() },
                                onSimulateKey = { viewModel.triggerSimulatedKey(it) },
                                onSimulateClick = { viewModel.registerSimulatedClick(it) },
                                onToggleZoomPreview = { viewModel.toggleZoomPreview() },
                                onUpdateCrosshairStyle = { viewModel.updateCrosshairStyle(it) },
                                onUpdateCrosshairParams = { sz, th, gp, dot, out, dyn, hex ->
                                    viewModel.updateCrosshairParams(sz, th, gp, dot, out, dyn, hex)
                                },
                                onTogglePixelCell = { viewModel.toggleCrosshairPixelCell(it) },
                                onResetOrClearGrid = { viewModel.clearOrSymmetrizeCrosshairGrid(it) }
                            )

                            MainNavTab.ADDONS -> AddonManagerScreen(
                                state = state,
                                onSelectFilter = { viewModel.selectAddonFilter(it) },
                                onToggleAddon = { viewModel.toggleAddonEnabled(it) },
                                onSelectSubpack = { id, sub -> viewModel.selectAddonSubpack(id, sub) },
                                onMoveAddonPriority = { id, up -> viewModel.moveAddonPriority(id, up) },
                                onImportAddonClick = {
                                    importAddonLauncher.launch(arrayOf("*/*"))
                                },
                                onLinkMojangFolderClick = {
                                    linkMojangFolderLauncher.launch(null)
                                },
                                onExportAddonClick = { addon ->
                                    viewModel.prepareExportAddon(addon)
                                    val safeFileName = addon.name.lowercase().replace(" ", "_") + addon.type.fileExtension
                                    exportAddonLauncher.launch(safeFileName)
                                },
                                onInspectManifest = { addon -> viewModel.getAddonManifestPreview(addon) },
                                onSelectVersionTier = { viewModel.selectVersionTier(it) },
                                onPatchAllAddons = { viewModel.patchAllAddonsForActiveVersion() }
                            )

                            MainNavTab.PROFILES -> ProfilesAndScriptsScreen(
                                state = state,
                                cloudSyncState = cloudSyncState,
                                userEmail = userEmail,
                                onSignOutClick = {
                                    signOutFromGoogle(
                                        context = context,
                                        credentialManager = credentialManager,
                                        onSignOutComplete = {
                                            viewModel.showBannerMessage("Switched to Direct Access Local Mode (Account Skipped).")
                                        },
                                        scope = coroutineScope
                                    )
                                },
                                onSyncToCloud = { title, desc ->
                                    viewModel.syncCurrentConfigToCloud(title, desc)
                                },
                                onRestoreCloudBackup = { backup ->
                                    viewModel.restoreFromCloudBackup(backup)
                                },
                                onDeleteCloudBackup = { backup ->
                                    viewModel.deleteCloudBackup(backup)
                                },
                                onSaveQuickLocalBackup = { title ->
                                    viewModel.saveQuickLocalBackup(title)
                                },
                                onRestoreLocalBackupFile = { entry ->
                                    viewModel.restoreFromLocalBackupFile(entry)
                                },
                                onDeleteLocalBackupFile = { entry ->
                                    viewModel.deleteLocalBackup(entry)
                                },
                                onApplyProfile = { viewModel.applyProfile(it) },
                                onSaveNewProfile = { n, d, tier -> viewModel.saveCurrentAsNewProfile(n, d, tier) },
                                onToggleScript = { viewModel.toggleScriptEnabled(it) },
                                onUpdateScriptCode = { id, code -> viewModel.updateScriptCode(id, code) },
                                onUpdatePreferences = { acc, scale, autoOv, host ->
                                    viewModel.updateClientPreferences(acc, scale, autoOv, host)
                                },
                                onExportBackupClick = {
                                    exportBackupJsonLauncher.launch("flarial_client_backup.json")
                                },
                                onRestoreBackupClick = {
                                    restoreBackupJsonLauncher.launch(arrayOf("application/json", "*/*"))
                                },
                                onGenerateRawJson = { viewModel.generateFullConfigJsonString() },
                                onApplyRawJson = { viewModel.restoreFromRawJsonString(it) },
                                onAuthSuccessMessage = { msg -> viewModel.showBannerMessage(msg) }
                            )
                        }
                    }
                }
            }
        }

        // In-App Floating ClickGUI Modal Overlay
        FloatingClickGuiModal(
            visible = state.showInAppClickGui,
            modules = state.modules,
            liveFps = state.liveFps,
            livePingMs = state.livePingMs,
            globalUiScale = state.globalUiScale,
            hasOverlayPermission = state.hasOverlayPermission,
            isSystemOverlayRunning = state.isSystemOverlayRunning,
            onDismiss = { viewModel.setInAppClickGuiVisible(false) },
            onToggleModule = { viewModel.toggleModule(it) },
            onUpdateUiScale = { viewModel.updateClientPreferences(null, it, null, null) },
            onToggleSystemOverlay = { viewModel.toggleSystemFloatingOverlay() },
            onRequestOverlayPermission = {
                runCatching {
                    overlayPermissionLauncher.launch(
                        MinecraftLauncherHelper.buildOverlayPermissionIntent(context)
                    )
                }
            },
            onLaunchMinecraft = { viewModel.launchMinecraftGame(true) }
        )
    }
}

private fun MainNavTab.icon(): ImageVector = when (this) {
    MainNavTab.DASHBOARD -> Icons.Default.Dashboard
    MainNavTab.MODULES -> Icons.Default.Tune
    MainNavTab.HUD_CROSSHAIR -> Icons.Default.CenterFocusStrong
    MainNavTab.ADDONS -> Icons.Default.Extension
    MainNavTab.PROFILES -> Icons.Default.CloudSync
}
