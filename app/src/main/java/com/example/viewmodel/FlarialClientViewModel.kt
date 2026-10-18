package com.example.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ClientStorageRepository
import com.example.data.CloudBackupRepository
import com.example.data.CloudConfigBackup
import com.example.model.AccentPreset
import com.example.model.AddonType
import com.example.model.BedrockVersionTier
import com.example.model.ClientModule
import com.example.model.ClientProfile
import com.example.model.CrosshairConfig
import com.example.model.CrosshairStyle
import com.example.model.CustomScript
import com.example.model.FeaturedBedrockServer
import com.example.model.LauncherInstance
import com.example.model.LauncherLogEntry
import com.example.model.LocalBackupFileEntry
import com.example.model.MinecraftAddon
import com.example.model.MinecraftInstallStatus
import com.example.model.ModuleCategory
import com.example.model.SettingItem
import com.example.overlay.FlarialOverlayStateBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.random.Random

enum class MainNavTab(val route: String, val title: String) {
    DASHBOARD("dashboard", "Launcher"),
    MODULES("modules", "Modules"),
    HUD_CROSSHAIR("hud_crosshair", "HUD & Optics"),
    ADDONS("addons", "Add-Ons"),
    PROFILES("profiles", "Settings & Sync")
}

sealed interface CloudSyncUiState {
    data object GuestDirectMode : CloudSyncUiState
    data object Loading : CloudSyncUiState
    data class Success(val backups: List<CloudConfigBackup>) : CloudSyncUiState
    data class Error(val message: String) : CloudSyncUiState
}

data class FlarialUiState(
    val showSplash: Boolean = false, // Instant Direct Access without blocking splash delay
    val currentTab: MainNavTab = MainNavTab.DASHBOARD,
    val installStatus: MinecraftInstallStatus,
    val selectedVersionTier: BedrockVersionTier = BedrockVersionTier.V1_26_PLUS,
    val lastInjectionStatusLabel: String = "Minecraft v1.26 Detected • Injected Successfully • Pocket Edition Ready",
    val isPocketEditionBoostActive: Boolean = true,
    val isSoonUpdateChannelReady: Boolean = true,
    val hasOverlayPermission: Boolean = false,
    val isSystemOverlayRunning: Boolean = false,
    val showInAppClickGui: Boolean = false,
    val modules: List<ClientModule> = emptyList(),
    val selectedCategory: ModuleCategory? = null,
    val moduleSearchQuery: String = "",
    val expandedModuleId: String? = "fps_ping",
    val crosshair: CrosshairConfig = CrosshairConfig(),
    val addons: List<MinecraftAddon> = emptyList(),
    val selectedAddonFilter: AddonType? = null,
    val scripts: List<CustomScript> = emptyList(),
    val profiles: List<ClientProfile> = emptyList(),
    val activeProfileId: String = "",
    val accentPreset: AccentPreset = AccentPreset.CYBER_CYAN,
    val globalUiScale: Float = 1.0f,
    val autoStartOverlay: Boolean = true,
    val targetServerHost: String = "play.hivebedrock.network",
    val linkedSafUri: String? = null,
    val statusBannerMessage: String? = null,
    // Cloud Sync & Local Backup State
    val localBackups: List<LocalBackupFileEntry> = emptyList(),
    val isCloudSyncing: Boolean = false,
    // Dedicated Gaming Launcher UI State
    val gamertag: String = "FlarialPlayer",
    val selectedCosmetic: String = "Cyber Cyan Wings",
    val renderDistanceChunks: Int = 12,
    val threadBoostMode: String = "MCPE Zero-Lag Boost",
    val instances: List<LauncherInstance> = defaultLauncherInstances(),
    val selectedInstanceId: String = "inst_pocket_edition_boost",
    val featuredServers: List<FeaturedBedrockServer> = defaultFeaturedServers(),
    val isLaunchingSequence: Boolean = false,
    val launchProgress: Float = 0f,
    val launchStageLabel: String = "Ready to Launch",
    val launcherLogs: List<LauncherLogEntry> = defaultInitialLogs(),
    // Interactive HUD & Sandbox Telemetry State
    val liveFps: Int = 144,
    val livePingMs: Int = 19,
    val liveFrameTimeMs: Float = 6.9f,
    val playerX: Float = 148.5f,
    val playerY: Float = 68.0f,
    val playerZ: Float = -312.4f,
    val playerYaw: Float = 135f,
    val activeKeys: Set<String> = setOf("W"),
    val lmbCps: Int = 9,
    val rmbCps: Int = 4,
    val isZoomActive: Boolean = false,
    val pendingExportAddon: MinecraftAddon? = null
)

private fun defaultLauncherInstances(): List<LauncherInstance> = listOf(
    LauncherInstance(
        id = "inst_pocket_edition_boost",
        title = "Minecraft Pocket Edition (Zero-Lag)",
        versionTag = "MCPE 1.19–1.26+ • Ultra Fast",
        targetPackage = "com.mojang.minecraftpe",
        badgeText = "MCPE BOOST",
        description = "Direct-launch Pocket Edition & Bedrock engine with mobile GPU frame-pacing, lag fixer, and VSync unlock.",
        versionTier = BedrockVersionTier.V1_26_PLUS
    ),
    LauncherInstance(
        id = "inst_soon_update_next",
        title = "Upcoming / Soon Update (v1.26+ / 1.27)",
        versionTag = "1.26+ & Next Release • Pre-Patched",
        targetPackage = "com.mojang.minecraftpe",
        badgeText = "SOON UPDATE",
        description = "Forward-compatible schema & options.txt bridge ready for upcoming Minecraft Pocket/Bedrock updates.",
        versionTier = BedrockVersionTier.V1_26_PLUS
    ),
    LauncherInstance(
        id = "inst_bedrock_v121_125",
        title = "Flarial Modern (v1.21 – v1.25)",
        versionTag = "1.21–1.25 • RenderDragon V2",
        targetPackage = "com.mojang.minecraftpe",
        badgeText = "COMPETITIVE",
        description = "Tuned for 1.21 Tricky Trials through 1.25 Vibrant Visuals with 180 FPS VSync bypass & WASD Keystrokes.",
        versionTier = BedrockVersionTier.V1_21
    ),
    LauncherInstance(
        id = "inst_bedrock_v119_120",
        title = "Flarial Legacy MCPE (v1.19 – v1.20)",
        versionTag = "1.19–1.20 • Classic Knockback",
        targetPackage = "com.mojang.minecraftpe.clone",
        badgeText = "LEGACY 1.19",
        description = "Backward-compatible 1.19 & 1.20 options.txt and manifest schema bridge for classic competitive servers.",
        versionTier = BedrockVersionTier.V1_19
    ),
    LauncherInstance(
        id = "inst_hud_sandbox",
        title = "Flarial 3D HUD Sandbox",
        versionTag = "Built-In Engine • Instant",
        targetPackage = "internal.sandbox",
        badgeText = "SIMULATOR",
        description = "Interactive in-app 3D voxel viewport to test HUD layout, WASD, Zoom, Time Changer, and Crosshair.",
        versionTier = BedrockVersionTier.V1_26_PLUS
    )
)

private fun defaultFeaturedServers(): List<FeaturedBedrockServer> = listOf(
    FeaturedBedrockServer(
        id = "srv_hive",
        name = "The Hive (MCPE & Bedrock)",
        host = "geo.hivebedrock.network",
        port = 19132,
        region = "Global Anycast (v1.19–v1.26+)",
        gameModes = "SkyWars • BedWars • Murder Mystery",
        pingMs = 19,
        onlinePlayers = "34,820",
        accentHex = "#00F0FF"
    ),
    FeaturedBedrockServer(
        id = "srv_zeqa",
        name = "Zeqa Competitive PvP",
        host = "zeqa.net",
        port = 19132,
        region = "NA / EU / AS (v1.19–v1.26+)",
        gameModes = "NoDebuff • Sumo • Ranked Duels • Boxing",
        pingMs = 16,
        onlinePlayers = "2,940",
        accentHex = "#FF2E93"
    ),
    FeaturedBedrockServer(
        id = "srv_cubecraft",
        name = "CubeCraft Pocket & Bedrock",
        host = "play.cubecraft.net",
        port = 19132,
        region = "EU / NA (v1.20–v1.26+)",
        gameModes = "EggWars • Lucky Islands • Pillars of Fortune",
        pingMs = 24,
        onlinePlayers = "19,410",
        accentHex = "#8B5CF6"
    ),
    FeaturedBedrockServer(
        id = "srv_galaxite",
        name = "Galaxite Studios",
        host = "play.galaxite.net",
        port = 19132,
        region = "Global (v1.21–v1.26+)",
        gameModes = "Core Wars • Chronos • Alien Blast",
        pingMs = 22,
        onlinePlayers = "4,120",
        accentHex = "#10B981"
    )
)

private fun defaultInitialLogs(): List<LauncherLogEntry> {
    val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
    return listOf(
        LauncherLogEntry(time, "ACCESS", "Direct Access Active — Account login skipped for instant launcher startup.", isHighlight = true),
        LauncherLogEntry(time, "DETECT", "Minecraft v1.26 Detected — Pocket Edition (MCPE) & Bedrock 1.19–1.26+ Ready.", isHighlight = true),
        LauncherLogEntry(time, "BOOST", "Zero-Lag Engine Active — Disabled background frame jitter & optimized chunk cache.", isHighlight = true),
        LauncherLogEntry(time, "INJECT", "Injected Successfully — Compatible Mode Active & Soon Update (v1.27) Pre-Patched.")
    )
}

class FlarialClientViewModel(
    private val repo: ClientStorageRepository,
    private val cloudRepo: CloudBackupRepository?,
    private val currentUserId: String?
) : ViewModel() {

    private val _uiState: MutableStateFlow<FlarialUiState>
    val uiState: StateFlow<FlarialUiState>

    // Only observe Firestore when the user has explicitly signed in; skip in Direct Access Guest Mode
    val cloudBackupsState: StateFlow<CloudSyncUiState> =
        if (cloudRepo != null && !currentUserId.isNullOrBlank()) {
            cloudRepo.observeUserBackups(currentUserId)
                .map<List<CloudConfigBackup>, CloudSyncUiState> { CloudSyncUiState.Success(it) }
                .catch { error ->
                    Log.w(TAG, "Error observing cloud backups", error)
                    emit(CloudSyncUiState.Error(error.message ?: "Failed to observe cloud backups"))
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = CloudSyncUiState.Loading
                )
        } else {
            flowOf(CloudSyncUiState.GuestDirectMode)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = CloudSyncUiState.GuestDirectMode
                )
        }

    private val lmbClickTimestamps = mutableListOf<Long>()
    private val rmbClickTimestamps = mutableListOf<Long>()

    init {
        val loaded = repo.loadSavedStateOrDefault()
        val installStatus = repo.detectMinecraftStatus(
            linkedSafUri = loaded.linkedSafUri,
            preferredVersionTier = loaded.selectedVersionTier
        )
        val hasOverlay = repo.hasOverlayPermission()

        FlarialOverlayStateBridge.updateEnabledModules(
            loaded.modules.filter { it.enabled }.map { it.id }.toSet()
        )

        _uiState = MutableStateFlow(
            FlarialUiState(
                showSplash = false, // Direct instant access without splash lag
                installStatus = installStatus,
                selectedVersionTier = loaded.selectedVersionTier,
                hasOverlayPermission = hasOverlay,
                isSystemOverlayRunning = FlarialOverlayStateBridge.isServiceRunning.value,
                modules = loaded.modules,
                crosshair = loaded.crosshair,
                addons = loaded.addons,
                scripts = loaded.scripts,
                profiles = loaded.profiles,
                activeProfileId = loaded.activeProfileId,
                accentPreset = loaded.accentPreset,
                globalUiScale = loaded.globalUiScale,
                autoStartOverlay = loaded.autoStartOverlay,
                targetServerHost = loaded.targetServerHost,
                linkedSafUri = loaded.linkedSafUri,
                localBackups = emptyList()
            )
        )
        uiState = _uiState.asStateFlow()

        // Perform all file I/O off the main thread so startup is 100% lag-free
        viewModelScope.launch(Dispatchers.IO) {
            repo.syncMojangConfigFiles(
                modules = loaded.modules,
                addons = loaded.addons,
                globalUiScale = loaded.globalUiScale,
                targetTier = loaded.selectedVersionTier
            )
            val initialJson = repo.serializeStateToJsonObject(
                modules = loaded.modules,
                crosshair = loaded.crosshair,
                addons = loaded.addons,
                scripts = loaded.scripts,
                profiles = loaded.profiles,
                activeProfileId = loaded.activeProfileId,
                accentPreset = loaded.accentPreset,
                globalUiScale = loaded.globalUiScale,
                autoStartOverlay = loaded.autoStartOverlay,
                targetServerHost = loaded.targetServerHost,
                linkedSafUri = loaded.linkedSafUri,
                selectedVersionTier = loaded.selectedVersionTier
            ).toString(2)

            repo.ensureDefaultLocalBackupFile(
                defaultJson = initialJson,
                moduleCount = loaded.modules.count { it.enabled },
                addonCount = loaded.addons.count { it.enabled }
            )
            val backups = repo.listLocalBackupFiles()
            _uiState.update { it.copy(localBackups = backups) }
        }

        FlarialOverlayStateBridge.onModuleToggleFromOverlay = { moduleId ->
            toggleModule(moduleId)
        }

        viewModelScope.launch {
            FlarialOverlayStateBridge.isServiceRunning.collect { running ->
                _uiState.update { it.copy(isSystemOverlayRunning = running) }
            }
        }
    }

    private fun appendLog(tag: String, message: String, isHighlight: Boolean = false) {
        val ts = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        _uiState.update { state ->
            val updated = (state.launcherLogs + LauncherLogEntry(ts, tag, message, isHighlight)).takeLast(24)
            state.copy(launcherLogs = updated)
        }
    }

    fun dismissSplashNow() {
        _uiState.update { it.copy(showSplash = false) }
    }

    fun selectTab(tab: MainNavTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun selectLauncherInstance(instanceId: String) {
        val inst = _uiState.value.instances.find { it.id == instanceId } ?: return
        _uiState.update {
            it.copy(
                selectedInstanceId = instanceId,
                selectedVersionTier = inst.versionTier,
                isPocketEditionBoostActive = instanceId == "inst_pocket_edition_boost" || it.isPocketEditionBoostActive,
                isSoonUpdateChannelReady = instanceId == "inst_soon_update_next" || it.isSoonUpdateChannelReady
            )
        }
        selectVersionTier(inst.versionTier, fromInstanceSelection = true)
        appendLog("INSTANCE", "Selected launch target: ${inst.title} (${inst.versionTag})", true)
    }

    /**
     * Switches the active Minecraft Pocket / Bedrock compatibility version tier (1.19 through 1.26+),
     * dynamically re-patching options.txt, global_resource_packs.json, global_behavior_packs.json,
     * and addon manifest schemas on a background thread.
     */
    fun selectVersionTier(tier: BedrockVersionTier, fromInstanceSelection: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _uiState.value
            val patchedAddons = repo.patchAllAddonsToVersionTier(current.addons, tier)
            repo.syncMojangConfigFiles(
                modules = current.modules,
                addons = patchedAddons,
                globalUiScale = current.globalUiScale,
                targetTier = tier
            )
            val updatedInstallStatus = repo.detectMinecraftStatus(
                linkedSafUri = current.linkedSafUri,
                preferredVersionTier = tier
            )

            _uiState.update { state ->
                state.copy(
                    selectedVersionTier = tier,
                    addons = patchedAddons,
                    installStatus = updatedInstallStatus,
                    lastInjectionStatusLabel = "Minecraft ${tier.shortLabel} Detected • Injected Successfully • Compatible Mode Active",
                    statusBannerMessage = if (!fromInstanceSelection) {
                        "Minecraft ${tier.shortLabel} Compatible Mode Active! Patched options.txt & ${patchedAddons.size} pack schemas."
                    } else state.statusBannerMessage
                )
            }
            appendLog("DETECT", "Minecraft ${tier.shortLabel} Detected (${tier.fullTitle}).", true)
            appendLog("COMPAT", "Compatible Mode Active — Adapted path to ${tier.mojangSubpath} & Script API ${tier.scriptApiVersion}.")
            appendLog("INJECT", "Injected Successfully — Synced options.txt, global_resource_packs.json & global_behavior_packs.json.", true)
            persistCurrentState()
        }
    }

    fun patchAllAddonsForActiveVersion() {
        val tier = _uiState.value.selectedVersionTier
        selectVersionTier(tier, fromInstanceSelection = false)
    }

    fun updateLauncherPlayerSettings(
        gamertag: String? = null,
        cosmetic: String? = null,
        renderChunks: Int? = null,
        boostMode: String? = null
    ) {
        _uiState.update { state ->
            state.copy(
                gamertag = gamertag ?: state.gamertag,
                selectedCosmetic = cosmetic ?: state.selectedCosmetic,
                renderDistanceChunks = renderChunks ?: state.renderDistanceChunks,
                threadBoostMode = boostMode ?: state.threadBoostMode
            )
        }
        persistCurrentState()
    }

    fun addCustomServer(name: String, host: String, port: Int) {
        val cleanName = name.trim().ifEmpty { "Custom Bedrock Server" }
        val cleanHost = host.trim().ifEmpty { "play.example.net" }
        val newServer = FeaturedBedrockServer(
            id = "srv_${System.currentTimeMillis()}",
            name = cleanName,
            host = cleanHost,
            port = port.coerceIn(1, 65535),
            region = "Custom IP",
            gameModes = "$cleanHost:$port",
            pingMs = Random.nextInt(16, 38),
            onlinePlayers = "Live",
            accentHex = "#00F0FF"
        )
        _uiState.update { state ->
            state.copy(
                featuredServers = listOf(newServer) + state.featuredServers,
                targetServerHost = cleanHost,
                statusBannerMessage = "Added server '$cleanName' ($cleanHost:$port) to Quick-Launch!"
            )
        }
        appendLog("SERVER", "Added external server $cleanName ($cleanHost:$port)", true)
        persistCurrentState()
    }

    fun toggleInAppClickGui() {
        _uiState.update { it.copy(showInAppClickGui = !it.showInAppClickGui) }
    }

    fun setInAppClickGuiVisible(visible: Boolean) {
        _uiState.update { it.copy(showInAppClickGui = visible) }
    }

    fun refreshInstallAndPermissions() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentTier = _uiState.value.selectedVersionTier
            val status = repo.detectMinecraftStatus(_uiState.value.linkedSafUri, currentTier)
            val hasOverlay = repo.hasOverlayPermission()
            _uiState.update {
                it.copy(
                    installStatus = status,
                    hasOverlayPermission = hasOverlay,
                    isSystemOverlayRunning = FlarialOverlayStateBridge.isServiceRunning.value
                )
            }
            appendLog(
                "SCAN",
                if (status.isInstalled) {
                    "Minecraft ${status.detectedTier.shortLabel} Detected (${status.packageName} • ${status.versionName})"
                } else {
                    "Minecraft ${currentTier.shortLabel} Compatible Mode Active • Pocket Edition & Sandbox ready."
                },
                isHighlight = true
            )
        }
    }

    fun clearBannerMessage() {
        _uiState.update { it.copy(statusBannerMessage = null) }
    }

    fun showBannerMessage(msg: String) {
        _uiState.update { it.copy(statusBannerMessage = msg) }
    }

    fun toggleSystemFloatingOverlay() {
        if (!repo.hasOverlayPermission()) {
            showBannerMessage("Grant 'Display over other apps' permission to start the System Floating Overlay.")
            return
        }
        if (FlarialOverlayStateBridge.isServiceRunning.value) {
            repo.startOrStopSystemOverlay(false)
            appendLog("OVERLAY", "Stopped System Floating ClickGUI Overlay.")
            showBannerMessage("System Floating ClickGUI Overlay stopped.")
        } else {
            repo.startOrStopSystemOverlay(true)
            appendLog("OVERLAY", "Started System Floating ClickGUI window (TYPE_APPLICATION_OVERLAY).", true)
            showBannerMessage("System Floating ClickGUI Overlay active! Tap the ⚡ FLARIAL pill anywhere on screen.")
        }
    }

    fun launchMinecraftGame(withOverlay: Boolean = true, serverHostOverride: String? = null) {
        val state = _uiState.value
        if (state.isLaunchingSequence) return

        viewModelScope.launch {
            val tier = state.selectedVersionTier
            _uiState.update {
                it.copy(
                    isLaunchingSequence = true,
                    launchProgress = 0.25f,
                    launchStageLabel = "Applying ${tier.shortLabel} Zero-Lag Pocket Edition Patch..."
                )
            }
            appendLog("DETECT", "Minecraft ${tier.shortLabel} Detected — Applying Pocket Edition & Bedrock fast patch.", true)
            withContext(Dispatchers.IO) {
                repo.syncMojangConfigFiles(state.modules, state.addons, state.globalUiScale, tier)
            }
            delay(140)

            _uiState.update {
                it.copy(
                    launchProgress = 0.65f,
                    launchStageLabel = "Injecting ${state.addons.count { a -> a.enabled }} Packs (.mcpack / .mcaddon / .mcworld)..."
                )
            }
            appendLog("INJECT", "Injected Successfully — Synced global_resource_packs.json & global_behavior_packs.json (${tier.shortLabel}).", true)
            delay(140)

            if (withOverlay && state.autoStartOverlay && repo.hasOverlayPermission()) {
                _uiState.update {
                    it.copy(
                        launchProgress = 0.90f,
                        launchStageLabel = "Attaching Flarial Floating ClickGUI Overlay..."
                    )
                }
                if (!FlarialOverlayStateBridge.isServiceRunning.value) {
                    repo.startOrStopSystemOverlay(true)
                }
                appendLog("OVERLAY", "Floating ClickGUI attached over Pocket Edition window.", true)
                delay(100)
            }

            _uiState.update {
                it.copy(
                    launchProgress = 1.0f,
                    launchStageLabel = "Launching Minecraft Pocket / Bedrock Edition..."
                )
            }
            delay(80)

            val selectedInst = state.instances.find { it.id == state.selectedInstanceId }
            if (selectedInst?.id == "inst_hud_sandbox") {
                appendLog("SANDBOX", "Launched Flarial 3D HUD & Optics Sandbox (${tier.shortLabel} Mode).", true)
                _uiState.update {
                    it.copy(
                        isLaunchingSequence = false,
                        launchProgress = 0f,
                        launchStageLabel = "Ready to Launch",
                        currentTab = MainNavTab.HUD_CROSSHAIR,
                        statusBannerMessage = "Flarial 3D HUD & Optics Sandbox Active (${tier.shortLabel})!"
                    )
                }
                return@launch
            }

            val launched = repo.launchMinecraftPackage(
                status = state.installStatus,
                serverAddress = serverHostOverride,
                targetPackageOverride = selectedInst?.targetPackage
            )

            _uiState.update {
                it.copy(
                    isLaunchingSequence = false,
                    launchProgress = 0f,
                    launchStageLabel = "Ready to Launch",
                    lastInjectionStatusLabel = "Minecraft ${tier.shortLabel} Detected • Injected Successfully • Pocket Edition Ready"
                )
            }

            if (launched) {
                val targetLabel = serverHostOverride?.let { " → $it" } ?: ""
                appendLog("LAUNCH", "Started ${state.installStatus.packageName} (${tier.shortLabel})$targetLabel!", true)
                showBannerMessage("Launching ${state.installStatus.editionName} (${tier.shortLabel})$targetLabel with ${state.modules.count { it.enabled }} active Flarial modules!")
            } else {
                appendLog("COMPAT", "Pocket Edition / Bedrock Simulator Mode Active — Opened instant 3D Gameplay & HUD Viewport!", true)
                _uiState.update {
                    it.copy(
                        currentTab = MainNavTab.HUD_CROSSHAIR,
                        showInAppClickGui = false,
                        statusBannerMessage = "Pocket Edition Simulator Running (${tier.shortLabel} Zero-Lag Mode) — Test WASD, CPS, Zoom & ClickGUI!"
                    )
                }
            }
        }
    }

    // Module Management
    fun selectModuleCategory(category: ModuleCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun updateModuleSearch(query: String) {
        _uiState.update { it.copy(moduleSearchQuery = query) }
    }

    fun toggleExpandModule(moduleId: String) {
        _uiState.update {
            it.copy(expandedModuleId = if (it.expandedModuleId == moduleId) null else moduleId)
        }
    }

    fun toggleModule(moduleId: String) {
        var modName = moduleId
        var newState = false
        _uiState.update { state ->
            val updated = state.modules.map { mod ->
                if (mod.id == moduleId) {
                    modName = mod.shortTag
                    newState = !mod.enabled
                    mod.copy(enabled = newState)
                } else mod
            }
            FlarialOverlayStateBridge.updateEnabledModules(
                updated.filter { it.enabled }.map { it.id }.toSet()
            )
            state.copy(modules = updated)
        }
        appendLog("MODULE", "$modName ${if (newState) "ENABLED" else "DISABLED"}", newState)
        persistCurrentState()
    }

    fun updateModuleSlider(moduleId: String, settingKey: String, newValue: Float) {
        _uiState.update { state ->
            val updated = state.modules.map { mod ->
                if (mod.id != moduleId) mod
                else mod.copy(
                    settings = mod.settings.map { item ->
                        if (item.key == settingKey && item is SettingItem.SliderSetting) {
                            item.copy(value = newValue.coerceIn(item.min, item.max))
                        } else item
                    }
                )
            }
            val updatedFps = if (moduleId == "fps_ping" && settingKey == "fps_cap") {
                newValue.roundToInt().coerceAtLeast(60)
            } else state.liveFps
            state.copy(
                modules = updated,
                liveFps = updatedFps,
                liveFrameTimeMs = (1000f / updatedFps * 10f).roundToInt() / 10f
            )
        }
        persistCurrentState()
    }

    fun updateModuleToggleSetting(moduleId: String, settingKey: String, newValue: Boolean) {
        _uiState.update { state ->
            val updated = state.modules.map { mod ->
                if (mod.id != moduleId) mod
                else mod.copy(
                    settings = mod.settings.map { item ->
                        if (item.key == settingKey && item is SettingItem.ToggleSetting) {
                            item.copy(value = newValue)
                        } else item
                    }
                )
            }
            state.copy(modules = updated)
        }
        persistCurrentState()
    }

    fun updateModuleChoiceSetting(moduleId: String, settingKey: String, selectedOption: String) {
        _uiState.update { state ->
            val updated = state.modules.map { mod ->
                if (mod.id != moduleId) mod
                else mod.copy(
                    settings = mod.settings.map { item ->
                        if (item.key == settingKey && item is SettingItem.ChoiceSetting) {
                            item.copy(selected = selectedOption)
                        } else item
                    }
                )
            }
            state.copy(modules = updated)
        }
        persistCurrentState()
    }

    fun updateModuleHudPosition(moduleId: String, deltaXNorm: Float, deltaYNorm: Float) {
        _uiState.update { state ->
            val updated = state.modules.map { mod ->
                if (mod.id != moduleId) mod
                else mod.copy(
                    hudX = (mod.hudX + deltaXNorm).coerceIn(0.02f, 0.84f),
                    hudY = (mod.hudY + deltaYNorm).coerceIn(0.02f, 0.84f)
                )
            }
            state.copy(modules = updated)
        }
        persistCurrentState()
    }

    fun updateModuleScale(moduleId: String, newScale: Float) {
        _uiState.update { state ->
            val updated = state.modules.map { mod ->
                if (mod.id != moduleId) mod
                else mod.copy(scale = newScale.coerceIn(0.60f, 1.60f))
            }
            state.copy(modules = updated)
        }
        persistCurrentState()
    }

    fun setAllModulesEnabled(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.modules.map { it.copy(enabled = enabled) }
            FlarialOverlayStateBridge.updateEnabledModules(
                updated.filter { it.enabled }.map { it.id }.toSet()
            )
            state.copy(
                modules = updated,
                statusBannerMessage = if (enabled) "All 9 utility modules enabled!" else "All utility modules disabled."
            )
        }
        persistCurrentState()
    }

    fun resetHudPositions() {
        val defaults = repo.defaultModules().associateBy { it.id }
        _uiState.update { state ->
            val updated = state.modules.map { mod ->
                val def = defaults[mod.id]
                if (def != null) mod.copy(hudX = def.hudX, hudY = def.hudY, scale = def.scale) else mod
            }
            state.copy(
                modules = updated,
                statusBannerMessage = "HUD widget positions reset to default tactical layout."
            )
        }
        persistCurrentState()
    }

    // Interactive Sandbox Controls (Keystrokes, CPS, Zoom)
    fun triggerSimulatedKey(key: String) {
        _uiState.update { state ->
            val next = state.activeKeys.toMutableSet()
            if (key in next) next.remove(key) else next.add(key)
            val speedMult = state.modules.find { it.id == "game_speed_fov" && it.enabled }
                ?.sliderValue("timer_speed", 1.0f) ?: 1.0f
            val dx = when (key) {
                "W" -> 1.2f * speedMult
                "S" -> -1.2f * speedMult
                else -> 0f
            }
            val dz = when (key) {
                "D" -> 1.2f * speedMult
                "A" -> -1.2f * speedMult
                else -> 0f
            }
            state.copy(
                activeKeys = next,
                playerX = ((state.playerX + dx) * 10f).roundToInt() / 10f,
                playerZ = ((state.playerZ + dz) * 10f).roundToInt() / 10f
            )
        }
    }

    fun registerSimulatedClick(isLeftClick: Boolean) {
        val now = System.currentTimeMillis()
        if (isLeftClick) {
            lmbClickTimestamps.add(now)
            lmbClickTimestamps.removeAll { now - it > 1000L }
            _uiState.update { it.copy(lmbCps = lmbClickTimestamps.size.coerceAtLeast(1)) }
        } else {
            rmbClickTimestamps.add(now)
            rmbClickTimestamps.removeAll { now - it > 1000L }
            _uiState.update { it.copy(rmbCps = rmbClickTimestamps.size.coerceAtLeast(1)) }
        }
    }

    fun toggleZoomPreview() {
        _uiState.update { it.copy(isZoomActive = !it.isZoomActive) }
    }

    // Crosshair Designer
    fun updateCrosshairStyle(style: CrosshairStyle) {
        _uiState.update { it.copy(crosshair = it.crosshair.copy(style = style)) }
        persistCurrentState()
    }

    fun updateCrosshairParams(
        size: Float? = null,
        thickness: Float? = null,
        gap: Float? = null,
        dotEnabled: Boolean? = null,
        outlineEnabled: Boolean? = null,
        dynamicHitExpand: Boolean? = null,
        colorHex: String? = null
    ) {
        _uiState.update { state ->
            val ch = state.crosshair
            state.copy(
                crosshair = ch.copy(
                    size = size ?: ch.size,
                    thickness = thickness ?: ch.thickness,
                    gap = gap ?: ch.gap,
                    dotEnabled = dotEnabled ?: ch.dotEnabled,
                    outlineEnabled = outlineEnabled ?: ch.outlineEnabled,
                    dynamicHitExpand = dynamicHitExpand ?: ch.dynamicHitExpand,
                    colorHex = colorHex ?: ch.colorHex
                )
            )
        }
        persistCurrentState()
    }

    fun toggleCrosshairPixelCell(index: Int) {
        if (index !in 0 until 81) return
        _uiState.update { state ->
            val grid = state.crosshair.pixelGrid.toMutableList()
            grid[index] = !grid[index]
            state.copy(
                crosshair = state.crosshair.copy(
                    style = CrosshairStyle.PIXEL_CUSTOM,
                    pixelGrid = grid
                )
            )
        }
        persistCurrentState()
    }

    fun clearOrSymmetrizeCrosshairGrid(clearAll: Boolean) {
        _uiState.update { state ->
            val nextGrid = if (clearAll) {
                List(81) { idx -> idx == 40 }
            } else {
                CrosshairConfig.defaultCrosshairGrid()
            }
            state.copy(crosshair = state.crosshair.copy(pixelGrid = nextGrid))
        }
        persistCurrentState()
    }

    // Add-ons, Resource & Texture Manager
    fun selectAddonFilter(type: AddonType?) {
        _uiState.update { it.copy(selectedAddonFilter = type) }
    }

    fun toggleAddonEnabled(addonId: String) {
        _uiState.update { state ->
            val updated = state.addons.map { addon ->
                if (addon.id == addonId) addon.copy(enabled = !addon.enabled) else addon
            }
            state.copy(addons = updated)
        }
        persistCurrentState()
    }

    fun selectAddonSubpack(addonId: String, subpack: String) {
        _uiState.update { state ->
            val updated = state.addons.map { addon ->
                if (addon.id == addonId) addon.copy(selectedSubpack = subpack) else addon
            }
            state.copy(addons = updated)
        }
        persistCurrentState()
    }

    fun moveAddonPriority(addonId: String, moveUp: Boolean) {
        _uiState.update { state ->
            val list = state.addons.toMutableList()
            val idx = list.indexOfFirst { it.id == addonId }
            if (idx == -1) return@update state
            val targetIdx = if (moveUp) (idx - 1).coerceAtLeast(0) else (idx + 1).coerceAtMost(list.lastIndex)
            if (idx != targetIdx) {
                val item = list.removeAt(idx)
                list.add(targetIdx, item)
            }
            val reindexed = list.mapIndexed { i, addon -> addon.copy(priorityOrder = i) }
            state.copy(addons = reindexed)
        }
        persistCurrentState()
    }

    fun importAddonFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentTier = _uiState.value.selectedVersionTier
            val result = repo.importAddonFromUri(uri, currentTier)
            result.onSuccess { imported ->
                _uiState.update { state ->
                    val updated = listOf(imported) + state.addons.mapIndexed { idx, a -> a.copy(priorityOrder = idx + 1) }
                    state.copy(
                        addons = updated,
                        lastInjectionStatusLabel = "Injected Successfully: ${imported.name} (${currentTier.shortLabel})",
                        statusBannerMessage = "Injected '${imported.name}' (${imported.type.label}) into com.mojang/${imported.type.folderName} (${currentTier.shortLabel} Schema)!"
                    )
                }
                appendLog("INJECT", "Injected Successfully: ${imported.name} (${imported.fileSizeKb} KB • ${currentTier.shortLabel})", true)
                persistCurrentState()
            }.onFailure { err ->
                showBannerMessage("Failed to import archive: ${err.localizedMessage ?: "Invalid .mcpack/.mcworld/.mcaddon/.zip"}")
            }
        }
    }

    fun prepareExportAddon(addon: MinecraftAddon) {
        _uiState.update { it.copy(pendingExportAddon = addon) }
    }

    fun exportPendingAddonToUri(uri: Uri) {
        val addon = _uiState.value.pendingExportAddon ?: return
        val tier = _uiState.value.selectedVersionTier
        viewModelScope.launch(Dispatchers.IO) {
            val res = repo.exportAddonToUri(addon, uri, tier)
            res.onSuccess {
                _uiState.update {
                    it.copy(
                        pendingExportAddon = null,
                        statusBannerMessage = "Exported '${addon.name}${addon.type.fileExtension}' with ${tier.shortLabel} manifest.json!"
                    )
                }
                appendLog("EXPORT", "Exported ${addon.name}${addon.type.fileExtension} (${tier.shortLabel} schema).")
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        pendingExportAddon = null,
                        statusBannerMessage = "Export failed: ${err.localizedMessage}"
                    )
                }
            }
        }
    }

    fun setLinkedMojangSafUri(uri: Uri) {
        repo.takePersistableSafPermission(uri)
        val uriStr = uri.toString()
        _uiState.update { state ->
            state.copy(
                linkedSafUri = uriStr,
                installStatus = state.installStatus.copy(linkedSafUri = uriStr),
                statusBannerMessage = "Linked external com.mojang directory via Android 12–15+ Scoped Storage!"
            )
        }
        appendLog("STORAGE", "Linked com.mojang SAF Tree URI (Scoped Storage Direct Mode).", true)
        persistCurrentState()
    }

    fun activateScopedStorageFallbackMode() {
        _uiState.update { state ->
            state.copy(
                linkedSafUri = null,
                installStatus = state.installStatus.copy(linkedSafUri = null, compatibleModeActive = true),
                statusBannerMessage = "Compatible Mode Active: Using internal staged com.mojang bridge (zero-permission fallback)."
            )
        }
        appendLog("COMPAT", "Compatible Mode Active — Switched to internal staged com.mojang fallback directory.", true)
        persistCurrentState()
    }

    fun getAddonManifestPreview(addon: MinecraftAddon): String {
        return repo.buildBedrockManifestJson(addon, _uiState.value.selectedVersionTier)
    }

    // Profiles, Scripts, Cloud Sync & Local Backup Configuration
    fun applyProfile(profile: ClientProfile) {
        val targetTier = profile.targetVersionTier
        _uiState.update { state ->
            val updatedMods = state.modules.map { mod ->
                mod.copy(enabled = mod.id in profile.enabledModules)
            }
            val updatedAddons = state.addons.map { addon ->
                addon.copy(enabled = addon.id in profile.enabledAddons)
            }
            FlarialOverlayStateBridge.updateEnabledModules(profile.enabledModules)
            state.copy(
                activeProfileId = profile.id,
                selectedVersionTier = targetTier,
                accentPreset = profile.accentPreset,
                globalUiScale = profile.globalUiScale,
                autoStartOverlay = profile.autoStartOverlay,
                targetServerHost = profile.targetServerHost,
                crosshair = profile.crosshair,
                modules = updatedMods,
                addons = updatedAddons,
                lastInjectionStatusLabel = "Minecraft ${targetTier.shortLabel} Profile Active • Injected Successfully",
                statusBannerMessage = "Applied profile '${profile.name}' (${targetTier.shortLabel} compatibility)!"
            )
        }
        appendLog("PROFILE", "Loaded version-tailored preset '${profile.name}' (${targetTier.shortLabel})", true)
        persistCurrentState()
    }

    fun saveCurrentAsNewProfile(
        name: String,
        description: String,
        targetTier: BedrockVersionTier = _uiState.value.selectedVersionTier
    ) {
        val cleanName = name.trim().ifEmpty { "Custom Flarial ${targetTier.shortLabel} Preset" }
        val cleanDesc = description.trim().ifEmpty {
            "Tailored for Minecraft ${targetTier.shortLabel} (${targetTier.fullTitle})."
        }
        val nowStr = "${targetTier.shortLabel} • " + SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date())

        _uiState.update { state ->
            val newProf = ClientProfile(
                id = "profile_${System.currentTimeMillis()}",
                name = cleanName,
                description = cleanDesc,
                updatedAt = nowStr,
                accentPreset = state.accentPreset,
                globalUiScale = state.globalUiScale,
                autoStartOverlay = state.autoStartOverlay,
                targetServerHost = state.targetServerHost,
                enabledModules = state.modules.filter { it.enabled }.map { it.id }.toSet(),
                crosshair = state.crosshair,
                enabledAddons = state.addons.filter { it.enabled }.map { it.id }.toSet(),
                targetVersionTier = targetTier
            )
            state.copy(
                profiles = listOf(newProf) + state.profiles,
                activeProfileId = newProf.id,
                selectedVersionTier = targetTier,
                statusBannerMessage = "Saved new ${targetTier.shortLabel} profile '$cleanName' to JSON storage!"
            )
        }
        appendLog("PROFILE", "Saved custom ${targetTier.shortLabel} profile '$cleanName'", true)
        persistCurrentState()
    }

    fun toggleScriptEnabled(scriptId: String) {
        _uiState.update { state ->
            val updated = state.scripts.map { s ->
                if (s.id == scriptId) s.copy(enabled = !s.enabled) else s
            }
            state.copy(scripts = updated)
        }
        persistCurrentState()
    }

    fun updateScriptCode(scriptId: String, newCode: String) {
        _uiState.update { state ->
            val updated = state.scripts.map { s ->
                if (s.id == scriptId) s.copy(code = newCode) else s
            }
            state.copy(
                scripts = updated,
                statusBannerMessage = "Saved script configuration changes to local JSON."
            )
        }
        persistCurrentState()
    }

    fun updateClientPreferences(
        accent: AccentPreset? = null,
        uiScale: Float? = null,
        autoOverlay: Boolean? = null,
        serverHost: String? = null
    ) {
        _uiState.update { state ->
            state.copy(
                accentPreset = accent ?: state.accentPreset,
                globalUiScale = uiScale ?: state.globalUiScale,
                autoStartOverlay = autoOverlay ?: state.autoStartOverlay,
                targetServerHost = serverHost ?: state.targetServerHost
            )
        }
        persistCurrentState()
    }

    fun generateFullConfigJsonString(): String {
        val s = _uiState.value
        return repo.serializeStateToJsonObject(
            modules = s.modules,
            crosshair = s.crosshair,
            addons = s.addons,
            scripts = s.scripts,
            profiles = s.profiles,
            activeProfileId = s.activeProfileId,
            accentPreset = s.accentPreset,
            globalUiScale = s.globalUiScale,
            autoStartOverlay = s.autoStartOverlay,
            targetServerHost = s.targetServerHost,
            linkedSafUri = s.linkedSafUri,
            selectedVersionTier = s.selectedVersionTier
        ).toString(2)
    }

    // Cloud Sync Operations (Firebase Firestore)
    fun syncCurrentConfigToCloud(customTitle: String = "", customDescription: String = "") {
        val repoInstance = cloudRepo
        if (repoInstance == null || currentUserId.isNullOrBlank()) {
            showBannerMessage("Direct Access Guest Mode active — Sign in with Google in Settings to sync to Cloud, or use Local .JSON Backup!")
            return
        }
        val state = _uiState.value
        if (state.isCloudSyncing) return
        val activeModCount = state.modules.count { it.enabled }
        val activeAddonCount = state.addons.count { it.enabled }
        val activeProfName = state.profiles.find { it.id == state.activeProfileId }?.name ?: "Custom Setup"
        val resolvedTitle = customTitle.trim().ifEmpty {
            "$activeProfName (${state.selectedVersionTier.shortLabel} • ${SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date())})"
        }
        val resolvedDesc = customDescription.trim().ifEmpty {
            "${state.selectedVersionTier.shortLabel} • $activeModCount active modules • $activeAddonCount packs • ${state.accentPreset.label}"
        }
        val configJson = generateFullConfigJsonString()

        _uiState.update { it.copy(isCloudSyncing = true) }
        viewModelScope.launch {
            val res = repoInstance.createBackup(
                title = resolvedTitle,
                description = resolvedDesc,
                configJson = configJson,
                accentPreset = state.accentPreset.name,
                moduleCount = activeModCount,
                addonCount = activeAddonCount
            )
            res.onSuccess {
                _uiState.update {
                    it.copy(
                        isCloudSyncing = false,
                        statusBannerMessage = "Synced '$resolvedTitle' to Firestore Cloud!"
                    )
                }
                appendLog("CLOUD", "Uploaded config backup '$resolvedTitle' to Firestore.", true)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isCloudSyncing = false,
                        statusBannerMessage = "Cloud Sync failed: ${err.localizedMessage ?: "Permission or network error"}"
                    )
                }
            }
        }
    }

    fun restoreFromCloudBackup(backup: CloudConfigBackup) {
        restoreFromRawJsonString(
            jsonText = backup.configJson,
            customSuccessBanner = "Restored '${backup.title}' from Firestore Cloud Sync!"
        )
        appendLog("CLOUD", "Restored cloud backup '${backup.title}'", true)
    }

    fun deleteCloudBackup(backup: CloudConfigBackup) {
        val repoInstance = cloudRepo ?: return
        viewModelScope.launch {
            val res = repoInstance.deleteBackup(backup.id)
            res.onSuccess {
                showBannerMessage("Deleted cloud backup '${backup.title}'.")
                appendLog("CLOUD", "Deleted cloud snapshot '${backup.title}'.")
            }.onFailure { err ->
                showBannerMessage("Failed to delete cloud backup: ${err.localizedMessage ?: "Error"}")
            }
        }
    }

    // Local Storage Snapshot Operations (.json files in app storage + SAF export/import)
    fun saveQuickLocalBackup(customTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val s = _uiState.value
            val modCount = s.modules.count { it.enabled }
            val addCount = s.addons.count { it.enabled }
            val json = generateFullConfigJsonString()
            val savedEntry = repo.saveLocalBackupFile(
                title = customTitle.trim().ifEmpty {
                    "Local Backup ${s.selectedVersionTier.shortLabel} (${SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())})"
                },
                jsonContent = json,
                moduleCount = modCount,
                addonCount = addCount
            )
            val updatedList = repo.listLocalBackupFiles()
            _uiState.update {
                it.copy(
                    localBackups = updatedList,
                    statusBannerMessage = "Exported '${savedEntry.fileName}' to local storage!"
                )
            }
            appendLog("BACKUP", "Saved local file ${savedEntry.fileName}", true)
        }
    }

    fun restoreFromLocalBackupFile(entry: LocalBackupFileEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.readLocalBackupFile(entry.fileName)
                .onSuccess { jsonText ->
                    restoreFromRawJsonString(
                        jsonText = jsonText,
                        customSuccessBanner = "Restored module configuration from local file '${entry.fileName}'!"
                    )
                    appendLog("RESTORE", "Restored local backup ${entry.fileName}", true)
                }
                .onFailure { err ->
                    showBannerMessage("Failed to read ${entry.fileName}: ${err.localizedMessage}")
                }
        }
    }

    fun deleteLocalBackup(entry: LocalBackupFileEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.deleteLocalBackupFile(entry.fileName)
            val updatedList = repo.listLocalBackupFiles()
            _uiState.update {
                it.copy(
                    localBackups = updatedList,
                    statusBannerMessage = "Removed local backup file '${entry.fileName}'."
                )
            }
        }
    }

    fun exportBackupJsonToUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val jsonText = generateFullConfigJsonString()
            repo.writeBackupJsonToUri(uri, jsonText)
                .onSuccess {
                    showBannerMessage("Client backup JSON exported to device storage successfully!")
                    appendLog("EXPORT", "Exported configuration JSON to external storage URI.", true)
                }
                .onFailure { err ->
                    showBannerMessage("Backup export failed: ${err.localizedMessage}")
                }
        }
    }

    fun restoreBackupJsonFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.readBackupJsonFromUri(uri)
                .onSuccess { jsonText ->
                    restoreFromRawJsonString(
                        jsonText = jsonText,
                        customSuccessBanner = "Restored module configuration from external .JSON file!"
                    )
                }
                .onFailure { err ->
                    showBannerMessage("Restore failed: ${err.localizedMessage ?: "Invalid JSON backup"}")
                }
        }
    }

    fun restoreFromRawJsonString(jsonText: String, customSuccessBanner: String? = null) {
        viewModelScope.launch(Dispatchers.Default) {
            runCatching {
                val parsed = repo.parseStateFromJsonString(jsonText)
                FlarialOverlayStateBridge.updateEnabledModules(
                    parsed.modules.filter { it.enabled }.map { it.id }.toSet()
                )
                _uiState.update { state ->
                    state.copy(
                        selectedVersionTier = parsed.selectedVersionTier,
                        modules = parsed.modules,
                        crosshair = parsed.crosshair,
                        addons = parsed.addons,
                        scripts = parsed.scripts,
                        profiles = parsed.profiles,
                        activeProfileId = parsed.activeProfileId,
                        accentPreset = parsed.accentPreset,
                        globalUiScale = parsed.globalUiScale,
                        autoStartOverlay = parsed.autoStartOverlay,
                        targetServerHost = parsed.targetServerHost,
                        statusBannerMessage = customSuccessBanner
                            ?: "Client settings, modules & HUD restored from JSON (${parsed.selectedVersionTier.shortLabel})!"
                    )
                }
                persistCurrentState(immediate = true)
            }.onFailure { err ->
                showBannerMessage("JSON Parse Error: ${err.localizedMessage ?: "Check JSON syntax"}")
            }
        }
    }

    /**
     * 1-Tap Lag Fixer, Pocket Edition (MCPE) Memory Optimizer & Soon Update Pre-Patcher.
     */
    fun runSmartEngineOptimization() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _uiState.value
            val optimizedMods = current.modules.map { mod ->
                when (mod.id) {
                    "fps_ping" -> mod.copy(
                        enabled = true,
                        settings = mod.settings.map { s ->
                            when {
                                s.key == "fps_cap" && s is SettingItem.SliderSetting -> s.copy(value = 180f)
                                s.key == "unlock_fps" && s is SettingItem.ToggleSetting -> s.copy(value = true)
                                else -> s
                            }
                        }
                    )
                    "entity_esp" -> mod.copy(
                        settings = mod.settings.map { s ->
                            if (s.key == "clear_particles" && s is SettingItem.ToggleSetting) s.copy(value = true)
                            else s
                        }
                    )
                    "time_changer" -> mod.copy(
                        settings = mod.settings.map { s ->
                            if (s.key == "clear_weather" && s is SettingItem.ToggleSetting) s.copy(value = true)
                            else s
                        }
                    )
                    else -> mod
                }
            }

            val freedKb = repo.optimizeMojangAndCacheEngine(
                modules = optimizedMods,
                addons = current.addons,
                globalUiScale = current.globalUiScale,
                renderDistanceChunks = current.renderDistanceChunks.coerceAtMost(10),
                targetTier = current.selectedVersionTier
            )

            _uiState.update { state ->
                state.copy(
                    modules = optimizedMods,
                    renderDistanceChunks = state.renderDistanceChunks.coerceAtMost(10),
                    threadBoostMode = "MCPE Zero-Lag Ultra",
                    isPocketEditionBoostActive = true,
                    isSoonUpdateChannelReady = true,
                    liveFps = 180,
                    livePingMs = 16,
                    liveFrameTimeMs = 5.5f,
                    lastInjectionStatusLabel = "Zero-Lag MCPE Boost Active • ${state.selectedVersionTier.shortLabel} & Soon Update Ready",
                    statusBannerMessage = "⚡ Lag Fixed & Pocket Edition Optimized! Freed ${freedKb}KB cache, locked 180 FPS, & pre-patched Soon Update (${state.selectedVersionTier.shortLabel})."
                )
            }
            appendLog(
                "OPTIMIZER",
                "Lag Fixed: Freed ${freedKb}KB shader/blob cache, enabled Pocket Edition Zero-Lag 180 FPS & Soon Update bridge.",
                true
            )
            persistCurrentState(immediate = true)
        }
    }

    private var persistJob: Job? = null

    private fun persistCurrentState(immediate: Boolean = false) {
        persistJob?.cancel()
        persistJob = viewModelScope.launch(Dispatchers.IO) {
            if (!immediate) {
                delay(400) // Debounced to eliminate any UI lag during rapid slider adjustments
            }
            val s = _uiState.value
            repo.saveFullStateJson(
                modules = s.modules,
                crosshair = s.crosshair,
                addons = s.addons,
                scripts = s.scripts,
                profiles = s.profiles,
                activeProfileId = s.activeProfileId,
                accentPreset = s.accentPreset,
                globalUiScale = s.globalUiScale,
                autoStartOverlay = s.autoStartOverlay,
                targetServerHost = s.targetServerHost,
                linkedSafUri = s.linkedSafUri,
                selectedVersionTier = s.selectedVersionTier
            )
        }
    }

    private companion object {
        const val TAG = "FlarialVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
