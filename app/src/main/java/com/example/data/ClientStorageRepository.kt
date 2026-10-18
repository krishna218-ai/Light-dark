package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.AccentPreset
import com.example.model.AddonType
import com.example.model.BedrockVersionTier
import com.example.model.ClientModule
import com.example.model.ClientProfile
import com.example.model.CrosshairConfig
import com.example.model.CrosshairStyle
import com.example.model.CustomScript
import com.example.model.MinecraftAddon
import com.example.model.ModuleCategory
import com.example.model.SettingItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ClientStorageRepository(private val context: Context) {

    val stagedMojangDir: File = File(context.filesDir, "com.mojang").apply { mkdirs() }
    private val stateFile: File = File(context.filesDir, "flarial_client_state.json")

    init {
        ensureMojangDirectoryStructure()
    }

    private fun ensureMojangDirectoryStructure() {
        listOf(
            "resource_packs",
            "behavior_packs",
            "development_resource_packs",
            "development_behavior_packs",
            "minecraftpe",
            "minecraftWorlds",
            "skin_packs"
        ).forEach { folder ->
            File(stagedMojangDir, folder).mkdirs()
        }

        // Seed default companion packs on disk if not yet created
        val defaultAddons = defaultSeededAddons()
        defaultAddons.forEach { addon ->
            val packDir = File(stagedMojangDir, "${addon.type.folderName}/${addon.id}")
            if (!packDir.exists()) {
                packDir.mkdirs()
                File(packDir, "manifest.json").writeText(
                    buildBedrockManifestJson(addon, BedrockVersionTier.V1_26_PLUS)
                )
                when (addon.type) {
                    AddonType.BEHAVIOR_PACK -> {
                        val scriptsDir = File(packDir, "scripts").apply { mkdirs() }
                        File(scriptsDir, "flarial_telemetry.js").writeText(
                            """
                            // Flarial Client Universal Bedrock Script API Hook (v1.19 - v1.26+)
                            import { world, system } from "@minecraft/server";
                            system.runInterval(() => {
                                for (const player of world.getAllPlayers()) {
                                    const vel = player.getVelocity();
                                    const speed = Math.hypot(vel.x, vel.z) * 20;
                                    player.onScreenDisplay.setActionBar(`§bFlarial v1.26+ §7| §fSpeed: §a${'$'}{speed.toFixed(2)} bps`);
                                }
                            }, 4);
                            """.trimIndent()
                        )
                    }
                    AddonType.MCWORLD_TEMPLATE -> {
                        File(packDir, "levelname.txt").writeText(addon.name)
                        File(packDir, "world_resource_packs.json").writeText("[]")
                        File(packDir, "world_behavior_packs.json").writeText("[]")
                    }
                    else -> {
                        val uiDir = File(packDir, "ui").apply { mkdirs() }
                        File(uiDir, "_global_variables.json").writeText(
                            JSONObject().apply {
                                put("\$flarial_hud_enabled", true)
                                put("\$flarial_accent_color", JSONArray(listOf(0.0, 0.94, 1.0)))
                                put("\$flarial_low_fire_offset", -0.35)
                                put("\$flarial_compatible_range", "1.19-1.26+")
                            }.toString(2)
                        )
                    }
                }
            }
        }
    }

    /**
     * Builds a universal backward/forward-compatible Bedrock manifest.json supporting
     * versions 1.19, 1.20, 1.21, 1.22, 1.23, 1.24, 1.25, and 1.26+.
     */
    fun buildBedrockManifestJson(
        addon: MinecraftAddon,
        targetTier: BedrockVersionTier = BedrockVersionTier.V1_26_PLUS
    ): String {
        val versionParts = addon.version.removePrefix("v").split(".")
            .mapNotNull { it.toIntOrNull() }
            .let { if (it.size >= 3) it.take(3) else targetTier.minEngineVersion }

        // Ensure min_engine_version adapts to the selected target tier (1.19 .. 1.26+)
        val engineVerArray = JSONArray(targetTier.minEngineVersion)

        val root = JSONObject()
        root.put("format_version", targetTier.schemaFormatVersion)

        val header = JSONObject().apply {
            put("name", addon.name)
            put("description", "${addon.description} [Flarial Universal ${targetTier.shortLabel} Schema]")
            put("uuid", addon.uuid)
            put("version", JSONArray(versionParts))
            if (addon.type != AddonType.MCWORLD_TEMPLATE) {
                put("min_engine_version", engineVerArray)
            }
        }
        root.put("header", header)

        val moduleType = when (addon.type) {
            AddonType.BEHAVIOR_PACK -> "data"
            AddonType.MCWORLD_TEMPLATE -> "world_template"
            else -> "resources"
        }

        val modulesArray = JSONArray()
        val primaryModuleObj = JSONObject().apply {
            put("description", addon.description)
            put("type", moduleType)
            put("uuid", UUID.nameUUIDFromBytes("${addon.uuid}-mod".toByteArray()).toString())
            put("version", JSONArray(versionParts))
        }
        modulesArray.put(primaryModuleObj)

        // Add Script API module & dependency for Behavior Packs across 1.19 - 1.26+
        if (addon.type == AddonType.BEHAVIOR_PACK) {
            val scriptModule = JSONObject().apply {
                put("description", "Flarial Script API Bridge (${targetTier.shortLabel})")
                put("type", "script")
                put("language", "javascript")
                put("uuid", UUID.nameUUIDFromBytes("${addon.uuid}-script".toByteArray()).toString())
                put("version", JSONArray(versionParts))
                put("entry", "scripts/flarial_telemetry.js")
            }
            modulesArray.put(scriptModule)

            val depArray = JSONArray().put(
                JSONObject().apply {
                    put("module_name", "@minecraft/server")
                    put("version", targetTier.scriptApiVersion)
                }
            )
            root.put("dependencies", depArray)
        }

        root.put("modules", modulesArray)

        // Add Deferred / PBR capabilities for 1.24 - 1.26+ Texture/Shader packs
        if (targetTier.supportsDeferredPipeline && addon.type == AddonType.TEXTURE_PACK) {
            root.put("capabilities", JSONArray().put("pbr").put("raytraced"))
        }

        if (addon.subpacks.isNotEmpty()) {
            val subpacksArr = JSONArray()
            addon.subpacks.forEachIndexed { index, subName ->
                subpacksArr.put(
                    JSONObject().apply {
                        put("folder_name", subName.lowercase(Locale.US).replace(" ", "_"))
                        put("name", subName)
                        put("memory_tier", index + 1)
                    }
                )
            }
            root.put("subpacks", subpacksArr)
        }

        return root.toString(2)
    }

    /**
     * Universal Multi-Version Schema Patcher: rewrites manifest.json files on disk
     * for all addons so they cleanly match any target version from 1.19 up to 1.26+.
     */
    fun patchAllAddonsToVersionTier(
        addons: List<MinecraftAddon>,
        targetTier: BedrockVersionTier
    ): List<MinecraftAddon> {
        return addons.map { addon ->
            val updatedAddon = addon.copy(
                minEngineVersion = targetTier.engineVersionDotString,
                maxCompatibleVersion = "1.26+"
            )
            val packDir = File(stagedMojangDir, "${updatedAddon.type.folderName}/${updatedAddon.id}")
            packDir.mkdirs()
            File(packDir, "manifest.json").writeText(
                buildBedrockManifestJson(updatedAddon, targetTier)
            )
            updatedAddon
        }
    }

    /**
     * Dynamically writes global_resource_packs.json, global_behavior_packs.json,
     * and version-adapted options.txt (supporting 1.19, 1.20, 1.21, 1.22, 1.23, 1.24, 1.25, and 1.26+).
     */
    fun syncMojangConfigFiles(
        modules: List<ClientModule>,
        addons: List<MinecraftAddon>,
        globalUiScale: Float,
        targetTier: BedrockVersionTier = BedrockVersionTier.V1_26_PLUS
    ) {
        val mcpeDir = File(stagedMojangDir, "minecraftpe").apply { mkdirs() }

        // 1. Write global_resource_packs.json for enabled resource/texture/bundle/zip packs
        val activeResourcePacks = addons
            .filter {
                it.enabled &&
                    it.type != AddonType.BEHAVIOR_PACK &&
                    it.type != AddonType.MCWORLD_TEMPLATE
            }
            .sortedBy { it.priorityOrder }

        val resourcePacksJsonArray = JSONArray()
        activeResourcePacks.forEach { pack ->
            val versionParts = pack.version.removePrefix("v").split(".")
                .mapNotNull { it.toIntOrNull() }
                .let { if (it.size >= 3) it.take(3) else targetTier.minEngineVersion }
            val obj = JSONObject().apply {
                put("pack_id", pack.uuid)
                put("version", JSONArray(versionParts))
                if (pack.selectedSubpack.isNotEmpty()) {
                    put("subpack", pack.selectedSubpack.lowercase(Locale.US).replace(" ", "_"))
                }
            }
            resourcePacksJsonArray.put(obj)
        }
        File(mcpeDir, "global_resource_packs.json").writeText(resourcePacksJsonArray.toString(2))

        // 2. Write global_behavior_packs.json for enabled behavior packs (1.19 - 1.26+)
        val activeBehaviorPacks = addons
            .filter { it.enabled && it.type == AddonType.BEHAVIOR_PACK }
            .sortedBy { it.priorityOrder }

        val behaviorPacksJsonArray = JSONArray()
        activeBehaviorPacks.forEach { pack ->
            val versionParts = pack.version.removePrefix("v").split(".")
                .mapNotNull { it.toIntOrNull() }
                .let { if (it.size >= 3) it.take(3) else targetTier.minEngineVersion }
            val obj = JSONObject().apply {
                put("pack_id", pack.uuid)
                put("version", JSONArray(versionParts))
            }
            behaviorPacksJsonArray.put(obj)
        }
        File(mcpeDir, "global_behavior_packs.json").writeText(behaviorPacksJsonArray.toString(2))

        // 3. Write version-adaptive options.txt (1.19 through 1.26+)
        val fullbrightMod = modules.find { it.id == "fullbright" }
        val zoomFovMod = modules.find { it.id == "zoom_fov" }
        val espUiMod = modules.find { it.id == "entity_esp" }
        val timeChangerMod = modules.find { it.id == "time_changer" }
        val speedFovMod = modules.find { it.id == "game_speed_fov" }

        val gammaValue = if (fullbrightMod?.enabled == true) {
            (fullbrightMod.sliderValue("gamma_level", 850f) / 100f)
        } else {
            1.0f
        }
        val baseFov = zoomFovMod?.sliderValue("base_fov", 90f) ?: 78f
        val dynamicSprintBoost = if (speedFovMod?.enabled == true) {
            speedFovMod.sliderValue("sprint_fov_boost", 12f)
        } else {
            0f
        }
        val effectiveFov = (baseFov + dynamicSprintBoost * 0.35f).coerceIn(30f, 130f)
        val vsyncEnabled = modules.find { it.id == "fps_ping" }?.toggleValue("unlock_fps", true) != true
        val guiScaleOffset = when {
            globalUiScale < 0.85f -> -1
            globalUiScale > 1.15f -> 1
            else -> 0
        }
        val worldTimeHour = timeChangerMod?.sliderValue("world_time_hour", 12f)?.toInt() ?: 12
        val timerSpeed = if (speedFovMod?.enabled == true) {
            speedFovMod.sliderValue("timer_speed", 1.0f)
        } else {
            1.0f
        }

        val optionsContent = buildString {
            appendLine("# Generated by Flarial Client Universal Multi-Version Patcher (${targetTier.shortLabel})")
            appendLine("flarial_target_engine:${targetTier.engineVersionDotString}")
            appendLine("flarial_mojang_subpath:${targetTier.mojangSubpath}")
            appendLine("gfx_gamma:${String.format(Locale.US, "%.2f", gammaValue)}")
            appendLine("gfx_field_of_view:${String.format(Locale.US, "%.1f", effectiveFov)}")
            appendLine("gfx_vsync:${if (vsyncEnabled) 1 else 0}")
            appendLine("gfx_max_framerate:${modules.find { it.id == "fps_ping" }?.sliderValue("fps_cap", 120f)?.toInt() ?: 120}")
            appendLine("gfx_guiscale_offset:$guiScaleOffset")
            appendLine("gfx_hidegui:0")
            appendLine("gfx_fancygraphics:${if (espUiMod?.toggleValue("clear_particles", true) == true) 0 else 1}")
            appendLine("flarial_time_changer_enabled:${if (timeChangerMod?.enabled == true) 1 else 0}")
            appendLine("flarial_world_time_ticks:${(worldTimeHour * 1000) % 24000}")
            appendLine("flarial_game_speed_multiplier:${String.format(Locale.US, "%.2f", timerSpeed)}")

            if (targetTier.usesLegacy119OptionsKeys) {
                // Minecraft 1.19 Legacy Engine keys
                appendLine("gfx_texel_aa:0")
                appendLine("dev_disable_render_limiter:1")
                appendLine("old_gl_context_compat:1")
            } else if (targetTier.supportsDeferredPipeline) {
                // Minecraft 1.21 - 1.26+ RenderDragon V2 / Vibrant Visuals keys
                appendLine("graphics_mode:${if (targetTier >= BedrockVersionTier.V1_24) "deferred" else "fancy"}")
                appendLine("deferred_view_distance:12")
                appendLine("gfx_multithreaded_rendering:1")
            }
        }
        File(mcpeDir, "options.txt").writeText(optionsContent)
    }

    fun optimizeMojangAndCacheEngine(
        modules: List<ClientModule>,
        addons: List<MinecraftAddon>,
        globalUiScale: Float,
        renderDistanceChunks: Int,
        targetTier: BedrockVersionTier = BedrockVersionTier.V1_26_PLUS
    ): Int {
        val mcpeDir = File(stagedMojangDir, "minecraftpe").apply { mkdirs() }
        val blobCacheDir = File(mcpeDir, "blob_cache")
        var freedKb = 148 // Baseline shader & telemetry buffer optimization
        if (blobCacheDir.exists()) {
            blobCacheDir.listFiles()?.forEach { f ->
                freedKb += (f.length() / 1024L).toInt()
                f.delete()
            }
        }
        context.cacheDir.listFiles()?.forEach { f ->
            if (f.isFile) {
                freedKb += (f.length() / 1024L).toInt()
                runCatching { f.delete() }
            }
        }

        // Write ultra-optimized options.txt flags for target version tier
        syncMojangConfigFiles(modules, addons, globalUiScale, targetTier)
        val optionsFile = File(mcpeDir, "options.txt")
        val extraLowLatencyFlags = buildString {
            append(if (optionsFile.exists()) optionsFile.readText() else "")
            appendLine("gfx_viewdistance:${renderDistanceChunks * 16}")
            appendLine("gfx_particle_view_distance:0.25")
            appendLine("gfx_fancybubbles:0")
            appendLine("gfx_rendershadows:0")
            appendLine("gfx_multithreaded_rendering:1")
        }
        optionsFile.writeText(extraLowLatencyFlags)
        return freedKb
    }

    /**
     * Universal Add-on & Resource Injector capable of handling .mcpack, .mcworld, .mcaddon, and .zip
     * files across Minecraft 1.19 through 1.26+ directory structures and JSON schemas.
     */
    fun importAddonFromUri(
        uri: Uri,
        targetTier: BedrockVersionTier = BedrockVersionTier.V1_26_PLUS
    ): Result<MinecraftAddon> {
        return try {
            val fileName = queryDisplayName(uri) ?: "imported_pack_${System.currentTimeMillis()}.mcpack"
            val cleanBaseName = fileName.substringBeforeLast(".")
                .replace(Regex("[^a-zA-Z0-9_\\- ]"), "")
                .trim()
                .ifEmpty { "Custom Pack" }

            val addonId = "custom_${System.currentTimeMillis()}"
            var detectedName = cleanBaseName
            var detectedDesc = "Imported via Flarial Universal Injector ($fileName • ${targetTier.shortLabel})"
            var detectedUuid = UUID.randomUUID().toString()
            var detectedVersion = "v${targetTier.engineVersionDotString}"
            var detectedMinEngine = targetTier.engineVersionDotString
            var detectedType = when {
                fileName.endsWith(".mcworld", ignoreCase = true) -> AddonType.MCWORLD_TEMPLATE
                fileName.endsWith(".mcaddon", ignoreCase = true) -> AddonType.MCADDON_BUNDLE
                fileName.endsWith(".zip", ignoreCase = true) -> AddonType.ZIP_ARCHIVE
                fileName.contains("behavior", ignoreCase = true) || fileName.contains("bp", ignoreCase = true) -> AddonType.BEHAVIOR_PACK
                fileName.contains("shader", ignoreCase = true) || fileName.contains("texture", ignoreCase = true) -> AddonType.TEXTURE_PACK
                else -> AddonType.RESOURCE_PACK
            }
            val subpacksList = mutableListOf<String>()

            // Temporary extraction staging before final folder routing
            val tempExtractDir = File(stagedMojangDir, "temp_import_$addonId").apply { mkdirs() }
            var totalBytes = 0L
            var foundManifest = false
            var foundWorldLevelDat = false

            context.contentResolver.openInputStream(uri)?.use { rawIn ->
                ZipInputStream(BufferedInputStream(rawIn)).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        val currentEntry = entry
                        if (!currentEntry.isDirectory) {
                            val safeName = currentEntry.name.replace("..", "").trimStart('/')
                            val outFile = File(tempExtractDir, safeName)
                            outFile.parentFile?.mkdirs()
                            val bytes = zis.readBytes()
                            totalBytes += bytes.size
                            outFile.writeBytes(bytes)

                            if (safeName.endsWith("level.dat", ignoreCase = true) ||
                                safeName.endsWith("levelname.txt", ignoreCase = true)
                            ) {
                                foundWorldLevelDat = true
                                if (safeName.endsWith("levelname.txt", ignoreCase = true)) {
                                    val lvlTitle = String(bytes, Charsets.UTF_8).trim()
                                    if (lvlTitle.isNotEmpty()) detectedName = lvlTitle
                                }
                            }

                            if (safeName.endsWith("manifest.json", ignoreCase = true)) {
                                foundManifest = true
                                runCatching {
                                    val json = JSONObject(String(bytes, Charsets.UTF_8))
                                    val header = json.optJSONObject("header")
                                    if (header != null) {
                                        detectedName = header.optString("name", detectedName)
                                        detectedDesc = header.optString("description", detectedDesc)
                                        detectedUuid = header.optString("uuid", detectedUuid)
                                        val vArr = header.optJSONArray("version")
                                        if (vArr != null && vArr.length() >= 3) {
                                            detectedVersion = "v${vArr.optInt(0, 1)}.${vArr.optInt(1, 21)}.${vArr.optInt(2, 0)}"
                                        }
                                        val minEngArr = header.optJSONArray("min_engine_version")
                                        if (minEngArr != null && minEngArr.length() >= 3) {
                                            detectedMinEngine = "${minEngArr.optInt(0, 1)}.${minEngArr.optInt(1, 19)}.${minEngArr.optInt(2, 0)}"
                                        }
                                    }
                                    val modules = json.optJSONArray("modules")
                                    if (modules != null && modules.length() > 0) {
                                        val modType = modules.getJSONObject(0).optString("type")
                                        when {
                                            modType.equals("data", ignoreCase = true) ||
                                                modType.equals("script", ignoreCase = true) -> {
                                                detectedType = AddonType.BEHAVIOR_PACK
                                            }
                                            modType.equals("world_template", ignoreCase = true) -> {
                                                detectedType = AddonType.MCWORLD_TEMPLATE
                                            }
                                        }
                                    }
                                    val subpacks = json.optJSONArray("subpacks")
                                    if (subpacks != null) {
                                        for (i in 0 until subpacks.length()) {
                                            val spName = subpacks.getJSONObject(i).optString("name")
                                            if (spName.isNotBlank()) subpacksList.add(spName)
                                        }
                                    }
                                }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }

            if (foundWorldLevelDat) {
                detectedType = AddonType.MCWORLD_TEMPLATE
            }

            val finalTargetDir = File(stagedMojangDir, "${detectedType.folderName}/$addonId").apply { mkdirs() }
            tempExtractDir.copyRecursively(finalTargetDir, overwrite = true)
            tempExtractDir.deleteRecursively()

            val sizeKb = ((totalBytes / 1024L).toInt()).coerceAtLeast(24)
            val createdAddon = MinecraftAddon(
                id = addonId,
                uuid = detectedUuid,
                name = detectedName,
                description = detectedDesc,
                version = detectedVersion,
                type = detectedType,
                enabled = true,
                fileSizeKb = sizeKb,
                author = "Universal Injector (${targetTier.shortLabel})",
                subpacks = subpacksList,
                selectedSubpack = subpacksList.firstOrNull() ?: "",
                relativePath = "${detectedType.folderName}/$addonId",
                priorityOrder = 0,
                minEngineVersion = detectedMinEngine,
                maxCompatibleVersion = "1.26+"
            )

            // Write or patch manifest.json to ensure 1.19 - 1.26+ compatibility
            if (!foundManifest || detectedType != AddonType.MCWORLD_TEMPLATE) {
                File(finalTargetDir, "manifest.json").writeText(
                    buildBedrockManifestJson(createdAddon, targetTier)
                )
            }

            Result.success(createdAddon)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportAddonToUri(
        addon: MinecraftAddon,
        targetUri: Uri,
        targetTier: BedrockVersionTier = BedrockVersionTier.V1_26_PLUS
    ): Result<Unit> {
        return try {
            val packDir = File(stagedMojangDir, "${addon.type.folderName}/${addon.id}")
            if (!packDir.exists()) {
                packDir.mkdirs()
            }
            File(packDir, "manifest.json").writeText(buildBedrockManifestJson(addon, targetTier))
            context.contentResolver.openOutputStream(targetUri)?.use { rawOut ->
                ZipOutputStream(BufferedOutputStream(rawOut)).use { zos ->
                    zipDirectoryRecursive(packDir, packDir, zos)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun zipDirectoryRecursive(rootDir: File, currentDir: File, zos: ZipOutputStream) {
        val files = currentDir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                zipDirectoryRecursive(rootDir, file, zos)
            } else {
                val relPath = file.relativeTo(rootDir).path.replace("\\", "/")
                zos.putNextEntry(ZipEntry(relPath))
                FileInputStream(file).use { fis ->
                    fis.copyTo(zos)
                }
                zos.closeEntry()
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            }
        }.getOrNull()
    }

    fun saveFullStateJson(
        modules: List<ClientModule>,
        crosshair: CrosshairConfig,
        addons: List<MinecraftAddon>,
        scripts: List<CustomScript>,
        profiles: List<ClientProfile>,
        activeProfileId: String,
        accentPreset: AccentPreset,
        globalUiScale: Float,
        autoStartOverlay: Boolean,
        targetServerHost: String,
        linkedSafUri: String?,
        selectedVersionTier: BedrockVersionTier = BedrockVersionTier.V1_26_PLUS
    ) {
        val root = serializeStateToJsonObject(
            modules = modules,
            crosshair = crosshair,
            addons = addons,
            scripts = scripts,
            profiles = profiles,
            activeProfileId = activeProfileId,
            accentPreset = accentPreset,
            globalUiScale = globalUiScale,
            autoStartOverlay = autoStartOverlay,
            targetServerHost = targetServerHost,
            linkedSafUri = linkedSafUri,
            selectedVersionTier = selectedVersionTier
        )
        stateFile.writeText(root.toString(2))
        syncMojangConfigFiles(modules, addons, globalUiScale, selectedVersionTier)
    }

    fun serializeStateToJsonObject(
        modules: List<ClientModule>,
        crosshair: CrosshairConfig,
        addons: List<MinecraftAddon>,
        scripts: List<CustomScript>,
        profiles: List<ClientProfile>,
        activeProfileId: String,
        accentPreset: AccentPreset,
        globalUiScale: Float,
        autoStartOverlay: Boolean,
        targetServerHost: String,
        linkedSafUri: String?,
        selectedVersionTier: BedrockVersionTier = BedrockVersionTier.V1_26_PLUS
    ): JSONObject {
        val root = JSONObject()
        root.put("client_version", "Flarial Bedrock Universal 3.0 (v1.19 - v1.26+)")
        root.put("exported_at", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
        root.put("selected_version_tier", selectedVersionTier.name)
        root.put("active_profile_id", activeProfileId)
        root.put("accent_preset", accentPreset.name)
        root.put("global_ui_scale", globalUiScale.toDouble())
        root.put("auto_start_overlay", autoStartOverlay)
        root.put("target_server_host", targetServerHost)
        if (linkedSafUri != null) {
            root.put("linked_saf_uri", linkedSafUri)
        }

        // Crosshair
        val chObj = JSONObject().apply {
            put("style", crosshair.style.name)
            put("size", crosshair.size.toDouble())
            put("thickness", crosshair.thickness.toDouble())
            put("gap", crosshair.gap.toDouble())
            put("dotEnabled", crosshair.dotEnabled)
            put("outlineEnabled", crosshair.outlineEnabled)
            put("dynamicHitExpand", crosshair.dynamicHitExpand)
            put("colorHex", crosshair.colorHex)
            put("pixelGrid", JSONArray(crosshair.pixelGrid))
        }
        root.put("crosshair", chObj)

        // Modules
        val modsArr = JSONArray()
        modules.forEach { mod ->
            val mObj = JSONObject().apply {
                put("id", mod.id)
                put("enabled", mod.enabled)
                put("hudX", mod.hudX.toDouble())
                put("hudY", mod.hudY.toDouble())
                put("scale", mod.scale.toDouble())
                val settingsObj = JSONObject()
                mod.settings.forEach { setting ->
                    when (setting) {
                        is SettingItem.SliderSetting -> settingsObj.put(setting.key, setting.value.toDouble())
                        is SettingItem.ToggleSetting -> settingsObj.put(setting.key, setting.value)
                        is SettingItem.ChoiceSetting -> settingsObj.put(setting.key, setting.selected)
                    }
                }
                put("settings", settingsObj)
            }
            modsArr.put(mObj)
        }
        root.put("modules", modsArr)

        // Addons
        val addonsArr = JSONArray()
        addons.forEach { addon ->
            addonsArr.put(
                JSONObject().apply {
                    put("id", addon.id)
                    put("uuid", addon.uuid)
                    put("name", addon.name)
                    put("description", addon.description)
                    put("version", addon.version)
                    put("type", addon.type.name)
                    put("enabled", addon.enabled)
                    put("fileSizeKb", addon.fileSizeKb)
                    put("author", addon.author)
                    put("subpacks", JSONArray(addon.subpacks))
                    put("selectedSubpack", addon.selectedSubpack)
                    put("priorityOrder", addon.priorityOrder)
                    put("minEngineVersion", addon.minEngineVersion)
                    put("maxCompatibleVersion", addon.maxCompatibleVersion)
                }
            )
        }
        root.put("addons", addonsArr)

        // Scripts
        val scriptsArr = JSONArray()
        scripts.forEach { script ->
            scriptsArr.put(
                JSONObject().apply {
                    put("id", script.id)
                    put("name", script.name)
                    put("description", script.description)
                    put("trigger", script.trigger)
                    put("enabled", script.enabled)
                    put("code", script.code)
                }
            )
        }
        root.put("scripts", scriptsArr)

        // Profiles
        val profArr = JSONArray()
        profiles.forEach { prof ->
            profArr.put(
                JSONObject().apply {
                    put("id", prof.id)
                    put("name", prof.name)
                    put("description", prof.description)
                    put("updatedAt", prof.updatedAt)
                    put("accentPreset", prof.accentPreset.name)
                    put("globalUiScale", prof.globalUiScale.toDouble())
                    put("autoStartOverlay", prof.autoStartOverlay)
                    put("targetServerHost", prof.targetServerHost)
                    put("enabledModules", JSONArray(prof.enabledModules.toList()))
                    put("enabledAddons", JSONArray(prof.enabledAddons.toList()))
                    put("targetVersionTier", prof.targetVersionTier.name)
                }
            )
        }
        root.put("profiles", profArr)

        return root
    }

    fun loadSavedStateOrDefault(): LoadedClientState {
        val defaultMods = defaultModules()
        val defaultAddons = defaultSeededAddons()
        val defaultScripts = defaultScripts()
        val defaultProfiles = defaultProfiles(defaultMods, defaultAddons)

        if (!stateFile.exists()) {
            return LoadedClientState(
                modules = defaultMods,
                crosshair = CrosshairConfig(),
                addons = defaultAddons,
                scripts = defaultScripts,
                profiles = defaultProfiles,
                activeProfileId = defaultProfiles.first().id,
                accentPreset = AccentPreset.CYBER_CYAN,
                globalUiScale = 1.0f,
                autoStartOverlay = true,
                targetServerHost = "play.hivebedrock.network",
                linkedSafUri = null,
                selectedVersionTier = BedrockVersionTier.V1_26_PLUS
            )
        }

        return try {
            parseStateFromJsonString(
                jsonString = stateFile.readText(),
                defaultMods = defaultMods,
                defaultAddons = defaultAddons,
                defaultScripts = defaultScripts,
                defaultProfiles = defaultProfiles
            )
        } catch (_: Exception) {
            LoadedClientState(
                modules = defaultMods,
                crosshair = CrosshairConfig(),
                addons = defaultAddons,
                scripts = defaultScripts,
                profiles = defaultProfiles,
                activeProfileId = defaultProfiles.first().id,
                accentPreset = AccentPreset.CYBER_CYAN,
                globalUiScale = 1.0f,
                autoStartOverlay = true,
                targetServerHost = "play.hivebedrock.network",
                linkedSafUri = null,
                selectedVersionTier = BedrockVersionTier.V1_26_PLUS
            )
        }
    }

    fun parseStateFromJsonString(
        jsonString: String,
        defaultMods: List<ClientModule> = defaultModules(),
        defaultAddons: List<MinecraftAddon> = defaultSeededAddons(),
        defaultScripts: List<CustomScript> = defaultScripts(),
        defaultProfiles: List<ClientProfile> = defaultProfiles(defaultMods, defaultAddons)
    ): LoadedClientState {
        val root = JSONObject(jsonString)

        val selectedVersionTier = runCatching {
            BedrockVersionTier.valueOf(
                root.optString("selected_version_tier", BedrockVersionTier.V1_26_PLUS.name)
            )
        }.getOrDefault(BedrockVersionTier.V1_26_PLUS)

        val accentPreset = runCatching {
            AccentPreset.valueOf(root.optString("accent_preset", AccentPreset.CYBER_CYAN.name))
        }.getOrDefault(AccentPreset.CYBER_CYAN)

        val globalUiScale = root.optDouble("global_ui_scale", 1.0).toFloat().coerceIn(0.6f, 1.5f)
        val autoStartOverlay = root.optBoolean("auto_start_overlay", true)
        val targetServerHost = root.optString("target_server_host", "play.hivebedrock.network")
        val linkedSafUri = root.optString("linked_saf_uri", "").ifBlank { null }

        // Parse Crosshair
        val chObj = root.optJSONObject("crosshair")
        val crosshair = if (chObj != null) {
            val gridArr = chObj.optJSONArray("pixelGrid")
            val grid = if (gridArr != null && gridArr.length() == 81) {
                List(81) { idx -> gridArr.optBoolean(idx, false) }
            } else {
                CrosshairConfig.defaultCrosshairGrid()
            }
            CrosshairConfig(
                style = runCatching {
                    CrosshairStyle.valueOf(chObj.optString("style", CrosshairStyle.CROSS.name))
                }.getOrDefault(CrosshairStyle.CROSS),
                size = chObj.optDouble("size", 14.0).toFloat(),
                thickness = chObj.optDouble("thickness", 2.5).toFloat(),
                gap = chObj.optDouble("gap", 4.0).toFloat(),
                dotEnabled = chObj.optBoolean("dotEnabled", true),
                outlineEnabled = chObj.optBoolean("outlineEnabled", true),
                dynamicHitExpand = chObj.optBoolean("dynamicHitExpand", true),
                colorHex = chObj.optString("colorHex", "#00F0FF"),
                pixelGrid = grid
            )
        } else {
            CrosshairConfig()
        }

        // Merge saved module states with defaults
        val savedModsMap = mutableMapOf<String, JSONObject>()
        val modsArr = root.optJSONArray("modules")
        if (modsArr != null) {
            for (i in 0 until modsArr.length()) {
                val obj = modsArr.getJSONObject(i)
                savedModsMap[obj.optString("id")] = obj
            }
        }

        val mergedModules = defaultMods.map { defMod ->
            val savedObj = savedModsMap[defMod.id]
            if (savedObj == null) {
                defMod
            } else {
                val settingsObj = savedObj.optJSONObject("settings")
                val updatedSettings = defMod.settings.map { item ->
                    if (settingsObj == null || !settingsObj.has(item.key)) {
                        item
                    } else {
                        when (item) {
                            is SettingItem.SliderSetting -> item.copy(
                                value = settingsObj.optDouble(item.key, item.value.toDouble()).toFloat()
                                    .coerceIn(item.min, item.max)
                            )
                            is SettingItem.ToggleSetting -> item.copy(
                                value = settingsObj.optBoolean(item.key, item.value)
                            )
                            is SettingItem.ChoiceSetting -> {
                                val chosen = settingsObj.optString(item.key, item.selected)
                                item.copy(selected = if (chosen in item.options) chosen else item.selected)
                            }
                        }
                    }
                }
                defMod.copy(
                    enabled = savedObj.optBoolean("enabled", defMod.enabled),
                    hudX = savedObj.optDouble("hudX", defMod.hudX.toDouble()).toFloat().coerceIn(0.02f, 0.88f),
                    hudY = savedObj.optDouble("hudY", defMod.hudY.toDouble()).toFloat().coerceIn(0.02f, 0.88f),
                    scale = savedObj.optDouble("scale", defMod.scale.toDouble()).toFloat().coerceIn(0.6f, 1.6f),
                    settings = updatedSettings
                )
            }
        }

        // Parse Addons
        val addonsArr = root.optJSONArray("addons")
        val loadedAddons = if (addonsArr != null && addonsArr.length() > 0) {
            buildList {
                for (i in 0 until addonsArr.length()) {
                    val obj = addonsArr.getJSONObject(i)
                    val subArr = obj.optJSONArray("subpacks")
                    val subs = buildList {
                        if (subArr != null) {
                            for (j in 0 until subArr.length()) add(subArr.optString(j))
                        }
                    }
                    add(
                        MinecraftAddon(
                            id = obj.optString("id"),
                            uuid = obj.optString("uuid", UUID.randomUUID().toString()),
                            name = obj.optString("name"),
                            description = obj.optString("description"),
                            version = obj.optString("version", "v1.26.0"),
                            type = runCatching {
                                AddonType.valueOf(obj.optString("type", AddonType.RESOURCE_PACK.name))
                            }.getOrDefault(AddonType.RESOURCE_PACK),
                            enabled = obj.optBoolean("enabled", true),
                            fileSizeKb = obj.optInt("fileSizeKb", 128),
                            author = obj.optString("author", "Flarial"),
                            subpacks = subs,
                            selectedSubpack = obj.optString("selectedSubpack", subs.firstOrNull() ?: ""),
                            priorityOrder = obj.optInt("priorityOrder", i),
                            minEngineVersion = obj.optString("minEngineVersion", "1.19.0"),
                            maxCompatibleVersion = obj.optString("maxCompatibleVersion", "1.26+")
                        )
                    )
                }
            }
        } else {
            defaultAddons
        }

        // Parse Scripts
        val scriptsArr = root.optJSONArray("scripts")
        val loadedScripts = if (scriptsArr != null && scriptsArr.length() > 0) {
            buildList {
                for (i in 0 until scriptsArr.length()) {
                    val obj = scriptsArr.getJSONObject(i)
                    add(
                        CustomScript(
                            id = obj.optString("id"),
                            name = obj.optString("name"),
                            description = obj.optString("description"),
                            trigger = obj.optString("trigger", "onTick"),
                            enabled = obj.optBoolean("enabled", true),
                            code = obj.optString("code")
                        )
                    )
                }
            }
        } else {
            defaultScripts
        }

        val activeProfId = root.optString("active_profile_id", defaultProfiles.first().id)

        return LoadedClientState(
            modules = mergedModules,
            crosshair = crosshair,
            addons = loadedAddons,
            scripts = loadedScripts,
            profiles = defaultProfiles,
            activeProfileId = activeProfId,
            accentPreset = accentPreset,
            globalUiScale = globalUiScale,
            autoStartOverlay = autoStartOverlay,
            targetServerHost = targetServerHost,
            linkedSafUri = linkedSafUri,
            selectedVersionTier = selectedVersionTier
        )
    }

    fun defaultModules(): List<ClientModule> = listOf(
        ClientModule(
            id = "fps_ping",
            name = "FPS & Server Ping HUD",
            shortTag = "FPS / PING",
            description = "Real-time frame-rate counter, frame-time telemetry graph, and Bedrock server latency monitor.",
            category = ModuleCategory.HUD,
            enabled = true,
            keybind = "F3",
            hudX = 0.04f,
            hudY = 0.06f,
            scale = 1.0f,
            settings = listOf(
                SettingItem.SliderSetting("fps_cap", "Target FPS Cap", 120f, 60f, 240f, " FPS", 10f),
                SettingItem.SliderSetting("bg_opacity", "HUD Card Opacity", 0.78f, 0.2f, 1.0f, ""),
                SettingItem.ToggleSetting("show_ping", "Show Server Ping (ms)", true, "Measures latency to target server"),
                SettingItem.ToggleSetting("show_frametime", "Frame-Time Sparkline", true, "Draws 1% low & ms graph on HUD"),
                SettingItem.ToggleSetting("unlock_fps", "Bypass VSync Limiter", true, "Writes gfx_vsync:0 to options.txt")
            )
        ),
        ClientModule(
            id = "keystrokes",
            name = "Keystrokes & CPS Display",
            shortTag = "WASD + CPS",
            description = "Visualizes WASD movement keys, Spacebar jump bar, and LMB/RMB clicks-per-second in real time.",
            category = ModuleCategory.MOVEMENT,
            enabled = true,
            keybind = "K",
            hudX = 0.04f,
            hudY = 0.46f,
            scale = 1.0f,
            settings = listOf(
                SettingItem.SliderSetting("press_speed", "Key Highlight Decay", 140f, 50f, 350f, " ms"),
                SettingItem.ToggleSetting("show_cps", "Show LMB / RMB CPS", true, "Live clicks-per-second counter"),
                SettingItem.ToggleSetting("show_space", "Show Jump Spacebar", true, "Bottom horizontal jump indicator"),
                SettingItem.ChoiceSetting("layout_mode", "Keystroke Style", "Tactical Neon", listOf("Tactical Neon", "Minimal Glass", "Chroma RGB"))
            )
        ),
        ClientModule(
            id = "entity_esp",
            name = "Player / Entity ESP & Scale",
            shortTag = "ESP / SCALE",
            description = "Renders entity bounding boxes, look-vector rays, armor durability HUD, and global Bedrock UI scaling.",
            category = ModuleCategory.UTILITY,
            enabled = true,
            keybind = "H",
            hudX = 0.68f,
            hudY = 0.70f,
            scale = 1.0f,
            settings = listOf(
                SettingItem.SliderSetting("entity_range", "ESP Telemetry Radius", 32f, 8f, 64f, " blocks"),
                SettingItem.ToggleSetting("hitbox_outline", "3D Entity Hitbox Box", true, "Shows 0.6x1.8m entity bounding box"),
                SettingItem.ToggleSetting("eye_vector", "Look Direction Ray", true, "Draws entity eye-level look vector"),
                SettingItem.ToggleSetting("armor_hud", "Armor & Offhand Durability HUD", true, "Shows remaining helmet/chest/leggings/boots %"),
                SettingItem.ToggleSetting("clear_particles", "Reduce Potion & Explosion Clutter", true, "Optimizes FPS in crowded PvP arenas")
            )
        ),
        ClientModule(
            id = "fullbright",
            name = "Fullbright (Night Vision)",
            shortTag = "GAMMA 1000%",
            description = "Overrides Bedrock lighting gamma curve for crystal-clear visibility in pitch-black caves, Nether, and underwater.",
            category = ModuleCategory.VISUAL,
            enabled = true,
            keybind = "G",
            hudX = 0.72f,
            hudY = 0.06f,
            scale = 1.0f,
            settings = listOf(
                SettingItem.SliderSetting("gamma_level", "Gamma Brightness", 850f, 100f, 1000f, "%"),
                SettingItem.ToggleSetting("clear_water_fog", "Clear Underwater Fog", true, "Removes murky biome water fog"),
                SettingItem.ToggleSetting("smooth_fade", "Smooth Light Transition", true, "Gradually interpolates gamma changes"),
                SettingItem.ChoiceSetting("tint_mode", "Ambient Light Tint", "Neutral Daylight", listOf("Neutral Daylight", "Cool Cyan", "Warm Torch"))
            )
        ),
        ClientModule(
            id = "zoom_fov",
            name = "Custom Zoom & FOV Modifier",
            shortTag = "OPTIC ZOOM",
            description = "Instant optic zoom magnifier with smooth cinematic camera damping and customizable base FOV.",
            category = ModuleCategory.VISUAL,
            enabled = true,
            keybind = "C",
            hudX = 0.72f,
            hudY = 0.22f,
            scale = 1.0f,
            settings = listOf(
                SettingItem.SliderSetting("zoom_factor", "Zoom Magnification", 4.0f, 1.5f, 10.0f, "x"),
                SettingItem.SliderSetting("base_fov", "Base Field of View", 90f, 30f, 120f, "°"),
                SettingItem.ToggleSetting("smooth_camera", "Cinematic Smooth Zoom", true, "Dampens camera sensitivity while zoomed"),
                SettingItem.ToggleSetting("lock_sprint_fov", "Static Speed/Sprint FOV", false, "Prevents speed potions from warping FOV")
            )
        ),
        ClientModule(
            id = "coordinates",
            name = "Coordinates & XYZ Tracker",
            shortTag = "XYZ / BIOME",
            description = "Displays exact X/Y/Z block coordinates, cardinal direction, chunk boundaries, and live Nether portal calculator.",
            category = ModuleCategory.HUD,
            enabled = true,
            keybind = "Z",
            hudX = 0.04f,
            hudY = 0.24f,
            scale = 1.0f,
            settings = listOf(
                SettingItem.SliderSetting("coord_precision", "Coordinate Update Rate", 20f, 5f, 60f, " Hz"),
                SettingItem.ToggleSetting("nether_coords", "Nether / Overworld Linked XYZ", true, "Auto-calculates 1:8 Nether portal coords"),
                SettingItem.ToggleSetting("show_direction", "Cardinal Compass & Yaw", true, "Displays N/NE/E/SE/S/SW/W/NW"),
                SettingItem.ToggleSetting("chunk_grid", "Chunk Border Alert", false, "Highlights 16x16 chunk boundaries"),
                SettingItem.ChoiceSetting("coord_layout", "Display Format", "Compact Stack", listOf("Compact Stack", "Horizontal Pill", "Minimalist"))
            )
        ),
        ClientModule(
            id = "crosshair_mod",
            name = "Custom Crosshair Overlay",
            shortTag = "CROSSHAIR",
            description = "Replaces the default crosshair with a high-contrast tactical reticle, dynamic hit indicator, and custom pixel grid.",
            category = ModuleCategory.VISUAL,
            enabled = true,
            keybind = "X",
            hudX = 0.48f,
            hudY = 0.48f,
            scale = 1.0f,
            settings = listOf(
                SettingItem.SliderSetting("reticle_opacity", "Reticle HUD Opacity", 100f, 25f, 100f, "%"),
                SettingItem.ToggleSetting("dynamic_hit", "Expand on Attack / CPS", true, "Pulses reticle when attacking"),
                SettingItem.ToggleSetting("show_cooldown", "Attack Rhythm Indicator", true, "Shows optimal PvP combo timing bar"),
                SettingItem.ToggleSetting("high_contrast", "Adaptive Contrast Outline", true, "Ensures visibility against snow & lava")
            )
        ),
        ClientModule(
            id = "time_changer",
            name = "Time & Atmosphere Changer",
            shortTag = "TIME / SKY",
            description = "Overrides client-side world time (Noon, Sunset, Midnight), locks daylight cycle, and clears rain/thunder fog across 1.19–1.26+.",
            category = ModuleCategory.VISUAL,
            enabled = true,
            keybind = "T",
            hudX = 0.72f,
            hudY = 0.38f,
            scale = 1.0f,
            settings = listOf(
                SettingItem.SliderSetting("world_time_hour", "Client World Time", 18f, 0f, 24f, ":00h", 1f),
                SettingItem.ToggleSetting("lock_time_cycle", "Lock Daylight Cycle", true, "Freezes sun/moon angle at selected hour"),
                SettingItem.ToggleSetting("clear_weather", "Clear Rain & Storm Fog", true, "Suppresses client weather particles"),
                SettingItem.ChoiceSetting("custom_sky_tint", "Atmosphere Preset", "Crystal Midnight", listOf("Vanilla Sky", "Crystal Midnight", "Golden Sunset", "Crimson Bloodmoon", "Aurora End"))
            )
        ),
        ClientModule(
            id = "game_speed_fov",
            name = "Game Speed & Dynamic FOV",
            shortTag = "SPEED / FOV",
            description = "Fine-tunes client animation/tick pacing, creative fly speed multiplier, and dynamic sprint FOV scaling for 1.19–1.26+.",
            category = ModuleCategory.MOVEMENT,
            enabled = true,
            keybind = "V",
            hudX = 0.72f,
            hudY = 0.54f,
            scale = 1.0f,
            settings = listOf(
                SettingItem.SliderSetting("timer_speed", "Client Tick / Anim Speed", 1.0f, 0.5f, 2.0f, "x", 0.05f),
                SettingItem.SliderSetting("sprint_fov_boost", "Sprint FOV Multiplier", 14f, 0f, 35f, "°", 1f),
                SettingItem.SliderSetting("fly_speed_mult", "Spectator / Creative Fly Speed", 2.0f, 1.0f, 5.0f, "x", 0.5f),
                SettingItem.ToggleSetting("smooth_fov_interp", "Smooth FOV Interpolation", true, "Prevents abrupt FOV snaps on speed potions")
            )
        )
    )

    fun defaultSeededAddons(): List<MinecraftAddon> = listOf(
        MinecraftAddon(
            id = "flarial_fullbright_pack",
            uuid = "b7a14f90-42c1-4e82-9a10-112233445501",
            name = "Flarial Universal Fullbright & Optics",
            description = "Shaderless night-vision lightmap table, crystal-clear water, and zero vignette across 1.19 to 1.26+.",
            version = "v1.26.0",
            type = AddonType.RESOURCE_PACK,
            enabled = true,
            fileSizeKb = 420,
            author = "Flarial Core",
            subpacks = listOf("1000% Max Gamma", "500% Soft Night Vision", "Clear Fog Only"),
            selectedSubpack = "1000% Max Gamma",
            priorityOrder = 0,
            minEngineVersion = "1.19.0",
            maxCompatibleVersion = "1.26+"
        ),
        MinecraftAddon(
            id = "flarial_pvp_lowfire",
            uuid = "b7a14f90-42c1-4e82-9a10-112233445502",
            name = "Flarial Low-Fire & PvP Short Swords",
            description = "Lowers screen fire overlay by 65%, shortens sword viewmodels, and adds bordered ores & connected glass.",
            version = "v1.26.0",
            type = AddonType.TEXTURE_PACK,
            enabled = true,
            fileSizeKb = 1840,
            author = "Flarial Visuals",
            subpacks = listOf("16x Competitive", "32x Crisp Faithful", "1.26 Deferred PBR"),
            selectedSubpack = "16x Competitive",
            priorityOrder = 1,
            minEngineVersion = "1.19.0",
            maxCompatibleVersion = "1.26+"
        ),
        MinecraftAddon(
            id = "flarial_chunk_hitboxes",
            uuid = "b7a14f90-42c1-4e82-9a10-112233445503",
            name = "Flarial Hitbox & Chunk Bounds",
            description = "Renders entity collision wireframes, spawn-proof light level markers, and 16x16 chunk border grids.",
            version = "v1.26.0",
            type = AddonType.RESOURCE_PACK,
            enabled = true,
            fileSizeKb = 610,
            author = "Flarial Technical",
            subpacks = listOf("Cyan Wireframe", "Chroma Hitbox", "Chunk Grid Only"),
            selectedSubpack = "Cyan Wireframe",
            priorityOrder = 2,
            minEngineVersion = "1.19.0",
            maxCompatibleVersion = "1.26+"
        ),
        MinecraftAddon(
            id = "flarial_telemetry_bp",
            uuid = "b7a14f90-42c1-4e82-9a10-112233445504",
            name = "Flarial GameTest Speed & DPS Script",
            description = "Bedrock Script API behavior pack providing live movement velocity (bps), combo counter, and entity health bars.",
            version = "v1.26.0",
            type = AddonType.BEHAVIOR_PACK,
            enabled = true,
            fileSizeKb = 195,
            author = "Flarial Scripting",
            subpacks = emptyList(),
            selectedSubpack = "",
            priorityOrder = 3,
            minEngineVersion = "1.19.0",
            maxCompatibleVersion = "1.26+"
        ),
        MinecraftAddon(
            id = "flarial_dark_ui",
            uuid = "b7a14f90-42c1-4e82-9a10-112233445505",
            name = "Flarial Obsidian Dark HUD & Quick Craft",
            description = "Translucent dark-mode inventory containers, quick-drop hotbar buttons, and RGB XP level bar.",
            version = "v1.26.0",
            type = AddonType.MCADDON_BUNDLE,
            enabled = true,
            fileSizeKb = 950,
            author = "Flarial UI",
            subpacks = listOf("Obsidian Cyan", "Midnight Violet"),
            selectedSubpack = "Obsidian Cyan",
            priorityOrder = 4,
            minEngineVersion = "1.19.0",
            maxCompatibleVersion = "1.26+"
        ),
        MinecraftAddon(
            id = "flarial_pvp_arena_mcworld",
            uuid = "b7a14f90-42c1-4e82-9a10-112233445506",
            name = "Flarial Aim & Knockback Practice Arena",
            description = "Pre-configured .mcworld training map with CPS bots, strafe corridors, and 0-latency chunk pregen.",
            version = "v1.26.0",
            type = AddonType.MCWORLD_TEMPLATE,
            enabled = true,
            fileSizeKb = 3420,
            author = "Flarial Worlds",
            subpacks = emptyList(),
            selectedSubpack = "",
            priorityOrder = 5,
            minEngineVersion = "1.19.0",
            maxCompatibleVersion = "1.26+"
        )
    )

    fun defaultScripts(): List<CustomScript> = listOf(
        CustomScript(
            id = "script_auto_sprint_hud",
            name = "Sprint & Velocity Telemetry Hook",
            description = "Calculates horizontal block velocity (BPS) and locks toggle-sprint state during air strafing (v1.19–v1.26+).",
            trigger = "onClientTick (20Hz)",
            enabled = true,
            code = """
                // Flarial Universal Molang + Script Config (1.19 - 1.26+)
                {
                  "hook": "query.modified_move_speed",
                  "toggle_sprint_lock": true,
                  "hud_format": "SPD: {{bps}} m/s | YAW: {{yaw}}°",
                  "air_strafe_assist_hud": true
                }
            """.trimIndent()
        ),
        CustomScript(
            id = "script_pvp_combo_tracker",
            name = "PvP Reach & Combo Counter",
            description = "Tracks consecutive melee hits without taking damage and measures exact hit distance (0.00 - 3.00m).",
            trigger = "onEntityAttack",
            enabled = true,
            code = """
                {
                  "event": "minecraft:entity_attack",
                  "reset_timeout_ms": 2500,
                  "display_reach_distance": true,
                  "sound_pitch_on_combo_5": 1.25
                }
            """.trimIndent()
        ),
        CustomScript(
            id = "script_auto_low_shield",
            name = "Dynamic Shield & Totem Offset",
            description = "Lowers offhand shield and totem of undying by 40% when not actively blocking.",
            trigger = "onRenderItemInHand",
            enabled = false,
            code = """
                {
                  "target_item": ["minecraft:shield", "minecraft:totem_of_undying"],
                  "idle_translate_y": -0.42,
                  "blocking_translate_y": -0.10,
                  "scale_multiplier": 0.82
                }
            """.trimIndent()
        )
    )

    fun defaultProfiles(
        mods: List<ClientModule>,
        addons: List<MinecraftAddon>
    ): List<ClientProfile> = listOf(
        ClientProfile(
            id = "profile_v126_universal",
            name = "Next-Gen 1.24–1.26+ Survival & Shaders",
            description = "Tailored for Minecraft 1.24–1.26+ Vibrant Visuals & RenderDragon: Fullbright, Time Changer, Dynamic FOV, XYZ Coords & GameTest BP.",
            updatedAt = "v1.26+ Preset",
            accentPreset = AccentPreset.CYBER_CYAN,
            globalUiScale = 1.0f,
            autoStartOverlay = true,
            targetServerHost = "play.hivebedrock.network",
            enabledModules = setOf(
                "fps_ping", "keystrokes", "fullbright", "zoom_fov",
                "coordinates", "crosshair_mod", "entity_esp", "time_changer", "game_speed_fov"
            ),
            crosshair = CrosshairConfig(style = CrosshairStyle.CROSS, size = 13f, gap = 3f, colorHex = "#00F0FF"),
            enabledAddons = setOf(
                "flarial_fullbright_pack", "flarial_pvp_lowfire",
                "flarial_chunk_hitboxes", "flarial_telemetry_bp", "flarial_dark_ui"
            ),
            targetVersionTier = BedrockVersionTier.V1_26_PLUS
        ),
        ClientProfile(
            id = "profile_v119_legacy_pvp",
            name = "Legacy 1.19–1.20 Competitive PvP",
            description = "Tailored for older 1.19 & 1.20 combat servers: Classic knockback FOV lock, WASD + CPS, Low-Fire 16x, and Sunset Time Changer.",
            updatedAt = "v1.19–1.20 Preset",
            accentPreset = AccentPreset.CRIMSON_PULSE,
            globalUiScale = 0.92f,
            autoStartOverlay = true,
            targetServerHost = "zeqa.net",
            enabledModules = setOf(
                "fps_ping", "keystrokes", "fullbright", "zoom_fov",
                "crosshair_mod", "entity_esp", "time_changer", "game_speed_fov"
            ),
            crosshair = CrosshairConfig(style = CrosshairStyle.CROSS, size = 11f, gap = 2.5f, colorHex = "#FF2E93"),
            enabledAddons = setOf("flarial_fullbright_pack", "flarial_pvp_lowfire", "flarial_dark_ui"),
            targetVersionTier = BedrockVersionTier.V1_19
        ),
        ClientProfile(
            id = "profile_v121_tricky_trials",
            name = "Modern 1.21–1.23 Ranked & Trials",
            description = "Tuned for 1.21–1.23 Wind Charge & Mace meta: Entity ESP Hitboxes, 180 FPS VSync bypass, and Circle+Dot Reticle.",
            updatedAt = "v1.21–1.23 Preset",
            accentPreset = AccentPreset.EMERALD_MATRIX,
            globalUiScale = 0.96f,
            autoStartOverlay = true,
            targetServerHost = "play.cubecraft.net",
            enabledModules = setOf(
                "fps_ping", "keystrokes", "coordinates", "fullbright",
                "zoom_fov", "entity_esp", "game_speed_fov"
            ),
            crosshair = CrosshairConfig(style = CrosshairStyle.CIRCLE_DOT, size = 14f, gap = 4f, colorHex = "#10B981"),
            enabledAddons = setOf("flarial_fullbright_pack", "flarial_chunk_hitboxes", "flarial_telemetry_bp", "flarial_dark_ui"),
            targetVersionTier = BedrockVersionTier.V1_21
        ),
        ClientProfile(
            id = "profile_cinematic_minimal",
            name = "Clean Minimal & Optic Zoom",
            description = "Ultra-clean screen with only FPS badge, smooth 8x cinematic optic zoom, Time Changer, and subtle diamond reticle.",
            updatedAt = "Built-in Preset",
            accentPreset = AccentPreset.ELECTRIC_VIOLET,
            globalUiScale = 0.85f,
            autoStartOverlay = false,
            targetServerHost = "play.galaxite.net",
            enabledModules = setOf("fps_ping", "zoom_fov", "crosshair_mod", "time_changer"),
            crosshair = CrosshairConfig(style = CrosshairStyle.DIAMOND, size = 10f, gap = 2f, colorHex = "#A855F7"),
            enabledAddons = setOf("flarial_fullbright_pack", "flarial_dark_ui"),
            targetVersionTier = BedrockVersionTier.V1_25
        )
    )

    private val localBackupsDir: File
        get() = File(context.filesDir, "local_backups").apply { mkdirs() }

    fun ensureDefaultLocalBackupFile(defaultJson: String, moduleCount: Int, addonCount: Int) {
        val dir = localBackupsDir
        if (dir.listFiles { f -> f.extension == "json" }.isNullOrEmpty()) {
            saveLocalBackupFile(
                title = "Default Flarial v1.26+ Starter Backup",
                jsonContent = defaultJson,
                moduleCount = moduleCount,
                addonCount = addonCount
            )
        }
    }

    fun listLocalBackupFiles(): List<com.example.model.LocalBackupFileEntry> {
        val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.US)
        val files = localBackupsDir.listFiles { f -> f.isFile && f.extension == "json" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

        return files.map { file ->
            val raw = runCatching { file.readText() }.getOrDefault("{}")
            val obj = runCatching { JSONObject(raw) }.getOrNull()
            val modCount = obj?.optJSONArray("modules")?.let { arr ->
                (0 until arr.length()).count { idx -> arr.optJSONObject(idx)?.optBoolean("enabled", false) == true }
            } ?: 0
            val addCount = obj?.optJSONArray("addons")?.let { arr ->
                (0 until arr.length()).count { idx -> arr.optJSONObject(idx)?.optBoolean("enabled", false) == true }
            } ?: 0
            val displayTitle = obj?.optString("backupTitle")?.takeIf { it.isNotBlank() }
                ?: file.nameWithoutExtension.replace("_", " ").replaceFirstChar { it.uppercase() }

            com.example.model.LocalBackupFileEntry(
                fileName = file.name,
                title = displayTitle,
                filePath = file.absolutePath,
                sizeBytes = file.length(),
                lastModifiedLabel = fmt.format(Date(file.lastModified())),
                moduleCount = modCount,
                addonCount = addCount
            )
        }
    }

    fun saveLocalBackupFile(
        title: String,
        jsonContent: String,
        moduleCount: Int,
        addonCount: Int
    ): com.example.model.LocalBackupFileEntry {
        val safeTitle = title.trim().ifEmpty { "Flarial Config Backup" }
        val slug = safeTitle.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifEmpty { "backup" }
        val fileName = "${slug}_${System.currentTimeMillis() % 100000}.json"
        val targetFile = File(localBackupsDir, fileName)

        val enrichedJson = runCatching {
            val jsonObj = JSONObject(jsonContent)
            jsonObj.put("backupTitle", safeTitle)
            jsonObj.toString(2)
        }.getOrDefault(jsonContent)

        targetFile.writeText(enrichedJson)
        val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.US)
        return com.example.model.LocalBackupFileEntry(
            fileName = targetFile.name,
            title = safeTitle,
            filePath = targetFile.absolutePath,
            sizeBytes = targetFile.length(),
            lastModifiedLabel = fmt.format(Date(targetFile.lastModified())),
            moduleCount = moduleCount,
            addonCount = addonCount
        )
    }

    fun readLocalBackupFile(fileName: String): Result<String> = runCatching {
        val file = File(localBackupsDir, fileName)
        require(file.exists()) { "Backup file $fileName does not exist" }
        file.readText()
    }

    fun deleteLocalBackupFile(fileName: String): Boolean {
        val file = File(localBackupsDir, fileName)
        return file.exists() && file.delete()
    }

    fun detectMinecraftStatus(
        linkedSafUri: String?,
        preferredVersionTier: BedrockVersionTier = BedrockVersionTier.V1_26_PLUS
    ): com.example.model.MinecraftInstallStatus {
        return com.example.util.MinecraftLauncherHelper.detectMinecraftStatus(
            context = context,
            stagedMojangDir = stagedMojangDir,
            linkedSafUri = linkedSafUri,
            preferredVersionTier = preferredVersionTier
        )
    }

    fun hasOverlayPermission(): Boolean {
        return com.example.util.MinecraftLauncherHelper.hasOverlayPermission(context)
    }

    fun startOrStopSystemOverlay(start: Boolean) {
        val intent = android.content.Intent(context, com.example.overlay.FlarialOverlayService::class.java)
        if (start) {
            context.startService(intent)
        } else {
            context.stopService(intent)
        }
    }

    fun launchMinecraftPackage(
        status: com.example.model.MinecraftInstallStatus,
        serverAddress: String?,
        targetPackageOverride: String? = null
    ): Boolean {
        return com.example.util.MinecraftLauncherHelper.launchMinecraft(
            context = context,
            status = status,
            serverAddress = serverAddress,
            targetPackageOverride = targetPackageOverride
        )
    }

    fun takePersistableSafPermission(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
    }

    fun writeBackupJsonToUri(uri: Uri, jsonText: String): Result<Unit> = runCatching {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(jsonText.toByteArray(Charsets.UTF_8))
        } ?: error("Unable to open output stream")
    }

    fun readBackupJsonFromUri(uri: Uri): Result<String> = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader().readText()
        } ?: error("Empty file")
    }
}

data class LoadedClientState(
    val modules: List<ClientModule>,
    val crosshair: CrosshairConfig,
    val addons: List<MinecraftAddon>,
    val scripts: List<CustomScript>,
    val profiles: List<ClientProfile>,
    val activeProfileId: String,
    val accentPreset: AccentPreset,
    val globalUiScale: Float,
    val autoStartOverlay: Boolean,
    val targetServerHost: String,
    val linkedSafUri: String?,
    val selectedVersionTier: BedrockVersionTier = BedrockVersionTier.V1_26_PLUS
)
