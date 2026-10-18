package com.example.model

enum class ModuleCategory(val displayName: String) {
    HUD("HUD & Telemetry"),
    VISUAL("Visuals & Optics"),
    MOVEMENT("Input & Speed"),
    UTILITY("World & ESP")
}

/**
 * Dynamic multi-version compatibility matrix ranging from Minecraft Bedrock 1.19 up to 1.26+.
 * Controls options.txt keys, manifest.json min_engine_version arrays, Script API versions,
 * and com.mojang directory structure adaptation.
 */
enum class BedrockVersionTier(
    val shortLabel: String,
    val fullTitle: String,
    val minEngineVersion: List<Int>,
    val schemaFormatVersion: Int,
    val scriptApiVersion: String,
    val supportsDeferredPipeline: Boolean,
    val usesLegacy119OptionsKeys: Boolean,
    val mojangSubpath: String
) {
    V1_19(
        shortLabel = "v1.19",
        fullTitle = "1.19.83 • Wild Update (Legacy Engine)",
        minEngineVersion = listOf(1, 19, 0),
        schemaFormatVersion = 2,
        scriptApiVersion = "1.2.0-beta",
        supportsDeferredPipeline = false,
        usesLegacy119OptionsKeys = true,
        mojangSubpath = "games/com.mojang"
    ),
    V1_20(
        shortLabel = "v1.20",
        fullTitle = "1.20.81 • Trails & Tales",
        minEngineVersion = listOf(1, 20, 0),
        schemaFormatVersion = 2,
        scriptApiVersion = "1.8.0",
        supportsDeferredPipeline = false,
        usesLegacy119OptionsKeys = false,
        mojangSubpath = "files/games/com.mojang"
    ),
    V1_21(
        shortLabel = "v1.21",
        fullTitle = "1.21.50 • Tricky Trials",
        minEngineVersion = listOf(1, 21, 0),
        schemaFormatVersion = 2,
        scriptApiVersion = "1.14.0",
        supportsDeferredPipeline = true,
        usesLegacy119OptionsKeys = false,
        mojangSubpath = "files/games/com.mojang"
    ),
    V1_22(
        shortLabel = "v1.22",
        fullTitle = "1.22.20 • Combat & Parity Update",
        minEngineVersion = listOf(1, 22, 0),
        schemaFormatVersion = 2,
        scriptApiVersion = "1.16.0",
        supportsDeferredPipeline = true,
        usesLegacy119OptionsKeys = false,
        mojangSubpath = "files/games/com.mojang"
    ),
    V1_23(
        shortLabel = "v1.23",
        fullTitle = "1.23.10 • RenderDragon V2 Engine",
        minEngineVersion = listOf(1, 23, 0),
        schemaFormatVersion = 2,
        scriptApiVersion = "1.17.0",
        supportsDeferredPipeline = true,
        usesLegacy119OptionsKeys = false,
        mojangSubpath = "files/games/com.mojang"
    ),
    V1_24(
        shortLabel = "v1.24",
        fullTitle = "1.24.30 • Vibrant Visuals Pipeline",
        minEngineVersion = listOf(1, 24, 0),
        schemaFormatVersion = 2,
        scriptApiVersion = "1.18.0",
        supportsDeferredPipeline = true,
        usesLegacy119OptionsKeys = false,
        mojangSubpath = "files/games/com.mojang"
    ),
    V1_25(
        shortLabel = "v1.25",
        fullTitle = "1.25.15 • Next-Gen PBR & Shaders",
        minEngineVersion = listOf(1, 25, 0),
        schemaFormatVersion = 2,
        scriptApiVersion = "2.0.0",
        supportsDeferredPipeline = true,
        usesLegacy119OptionsKeys = false,
        mojangSubpath = "files/games/com.mojang"
    ),
    V1_26_PLUS(
        shortLabel = "v1.26+",
        fullTitle = "1.26.0+ • Universal Latest Release",
        minEngineVersion = listOf(1, 26, 0),
        schemaFormatVersion = 2,
        scriptApiVersion = "2.1.0",
        supportsDeferredPipeline = true,
        usesLegacy119OptionsKeys = false,
        mojangSubpath = "files/games/com.mojang"
    );

    val engineVersionDotString: String
        get() = minEngineVersion.joinToString(".")

    companion object {
        fun fromVersionString(rawVersion: String): BedrockVersionTier {
            val cleaned = rawVersion.lowercase().removePrefix("v").trim()
            return when {
                cleaned.startsWith("1.19") -> V1_19
                cleaned.startsWith("1.20") -> V1_20
                cleaned.startsWith("1.21") -> V1_21
                cleaned.startsWith("1.22") -> V1_22
                cleaned.startsWith("1.23") -> V1_23
                cleaned.startsWith("1.24") -> V1_24
                cleaned.startsWith("1.25") -> V1_25
                cleaned.startsWith("1.26") || cleaned.startsWith("1.27") -> V1_26_PLUS
                else -> V1_26_PLUS
            }
        }
    }
}

sealed class SettingItem {
    abstract val key: String
    abstract val label: String

    data class SliderSetting(
        override val key: String,
        override val label: String,
        val value: Float,
        val min: Float,
        val max: Float,
        val unit: String = "",
        val step: Float = 0f
    ) : SettingItem()

    data class ToggleSetting(
        override val key: String,
        override val label: String,
        val value: Boolean,
        val subtitle: String = ""
    ) : SettingItem()

    data class ChoiceSetting(
        override val key: String,
        override val label: String,
        val selected: String,
        val options: List<String>
    ) : SettingItem()
}

data class ClientModule(
    val id: String,
    val name: String,
    val shortTag: String,
    val description: String,
    val category: ModuleCategory,
    val enabled: Boolean,
    val keybind: String,
    val hudX: Float = 0.05f, // Normalized 0f..1f on HUD canvas
    val hudY: Float = 0.08f,
    val scale: Float = 1.0f,
    val settings: List<SettingItem> = emptyList()
) {
    fun sliderValue(key: String, default: Float): Float =
        (settings.find { it.key == key } as? SettingItem.SliderSetting)?.value ?: default

    fun toggleValue(key: String, default: Boolean): Boolean =
        (settings.find { it.key == key } as? SettingItem.ToggleSetting)?.value ?: default

    fun choiceValue(key: String, default: String): String =
        (settings.find { it.key == key } as? SettingItem.ChoiceSetting)?.selected ?: default
}

enum class CrosshairStyle(val label: String) {
    CROSS("Tactical Cross"),
    CIRCLE_DOT("Circle + Dot"),
    CHEVRON("Apex Chevron"),
    DIAMOND("Neon Diamond"),
    PIXEL_CUSTOM("Custom Pixel Grid")
}

data class CrosshairConfig(
    val style: CrosshairStyle = CrosshairStyle.CROSS,
    val size: Float = 14f,
    val thickness: Float = 2.5f,
    val gap: Float = 4f,
    val dotEnabled: Boolean = true,
    val outlineEnabled: Boolean = true,
    val dynamicHitExpand: Boolean = true,
    val colorHex: String = "#00F0FF",
    val pixelGrid: List<Boolean> = defaultCrosshairGrid() // 9x9 = 81 cells
) {
    companion object {
        fun defaultCrosshairGrid(): List<Boolean> {
            val grid = MutableList(81) { false }
            val center = 4
            for (i in 1..7) {
                if (i != center) {
                    grid[center * 9 + i] = true
                    grid[i * 9 + center] = true
                }
            }
            grid[center * 9 + center] = true
            return grid
        }
    }
}

enum class AddonType(
    val label: String,
    val folderName: String,
    val badgeColorHex: String,
    val fileExtension: String
) {
    RESOURCE_PACK("Resource Pack (.mcpack)", "resource_packs", "#00F0FF", ".mcpack"),
    TEXTURE_PACK("Texture / Shader (.mcpack)", "resource_packs", "#FF2E93", ".mcpack"),
    BEHAVIOR_PACK("Behavior Pack (.mcpack)", "behavior_packs", "#8B5CF6", ".mcpack"),
    MCADDON_BUNDLE("McAddon Bundle (.mcaddon)", "development_resource_packs", "#10B981", ".mcaddon"),
    MCWORLD_TEMPLATE("McWorld Map (.mcworld)", "minecraftWorlds", "#F59E0B", ".mcworld"),
    ZIP_ARCHIVE("Universal Archive (.zip)", "resource_packs", "#06B6D4", ".zip")
}

data class MinecraftAddon(
    val id: String,
    val uuid: String,
    val name: String,
    val description: String,
    val version: String,
    val type: AddonType,
    val enabled: Boolean,
    val fileSizeKb: Int,
    val author: String,
    val subpacks: List<String> = emptyList(),
    val selectedSubpack: String = "",
    val relativePath: String = "",
    val priorityOrder: Int = 0,
    val minEngineVersion: String = "1.19.0",
    val maxCompatibleVersion: String = "1.26+"
)

data class CustomScript(
    val id: String,
    val name: String,
    val description: String,
    val trigger: String,
    val enabled: Boolean,
    val code: String
)

enum class AccentPreset(val label: String, val primaryHex: String, val secondaryHex: String) {
    CYBER_CYAN("Flarial Cyan", "#00F0FF", "#8B5CF6"),
    ELECTRIC_VIOLET("Void Violet", "#A855F7", "#00F0FF"),
    CRIMSON_PULSE("Crimson Pulse", "#FF2E93", "#F59E0B"),
    EMERALD_MATRIX("Emerald Matrix", "#10B981", "#06B6D4")
}

data class ClientProfile(
    val id: String,
    val name: String,
    val description: String,
    val updatedAt: String,
    val accentPreset: AccentPreset,
    val globalUiScale: Float,
    val autoStartOverlay: Boolean,
    val targetServerHost: String,
    val enabledModules: Set<String>,
    val crosshair: CrosshairConfig,
    val enabledAddons: Set<String>,
    val targetVersionTier: BedrockVersionTier = BedrockVersionTier.V1_26_PLUS
)

data class DetectedBedrockPackage(
    val packageName: String,
    val label: String,
    val isInstalled: Boolean,
    val versionName: String,
    val versionTier: BedrockVersionTier
)

data class MinecraftInstallStatus(
    val isInstalled: Boolean,
    val packageName: String,
    val editionName: String,
    val versionName: String,
    val versionCode: Long,
    val deepLinkSupported: Boolean,
    val stagedMojangPath: String,
    val linkedSafUri: String? = null,
    val detectedTier: BedrockVersionTier = BedrockVersionTier.V1_26_PLUS,
    val compatibleModeActive: Boolean = true,
    val packageVariants: List<DetectedBedrockPackage> = emptyList()
)

data class LauncherInstance(
    val id: String,
    val title: String,
    val versionTag: String,
    val targetPackage: String,
    val badgeText: String,
    val description: String,
    val versionTier: BedrockVersionTier = BedrockVersionTier.V1_26_PLUS
)

data class FeaturedBedrockServer(
    val id: String,
    val name: String,
    val host: String,
    val port: Int = 19132,
    val region: String,
    val gameModes: String,
    val pingMs: Int,
    val onlinePlayers: String,
    val accentHex: String
)

data class LauncherLogEntry(
    val timestamp: String,
    val tag: String,
    val message: String,
    val isHighlight: Boolean = false
)

data class LocalBackupFileEntry(
    val fileName: String,
    val title: String,
    val filePath: String,
    val sizeBytes: Long,
    val lastModifiedLabel: String,
    val moduleCount: Int,
    val addonCount: Int
)
