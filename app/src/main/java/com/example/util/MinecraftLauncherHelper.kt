package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.example.model.BedrockVersionTier
import com.example.model.DetectedBedrockPackage
import com.example.model.MinecraftInstallStatus
import java.io.File

object MinecraftLauncherHelper {

    val KNOWN_MINECRAFT_PACKAGES = listOf(
        Triple("com.mojang.minecraftpe", "Minecraft Bedrock Official", BedrockVersionTier.V1_26_PLUS),
        Triple("com.mojang.minecraftpe.beta", "Minecraft Preview / Beta", BedrockVersionTier.V1_26_PLUS),
        Triple("com.mojang.minecraftpe.preview", "Minecraft Preview Channel", BedrockVersionTier.V1_26_PLUS),
        Triple("com.mojang.minecraftedu", "Minecraft Education Edition", BedrockVersionTier.V1_21),
        Triple("com.mojang.minecraftpe.clone", "Custom / Cloned Bedrock APK", BedrockVersionTier.V1_20),
        Triple("net.flarial.minecraftpe", "Flarial Patched Bedrock Client", BedrockVersionTier.V1_26_PLUS)
    )

    fun detectMinecraftStatus(
        context: Context,
        stagedMojangDir: File,
        linkedSafUri: String?,
        preferredVersionTier: BedrockVersionTier = BedrockVersionTier.V1_26_PLUS
    ): MinecraftInstallStatus {
        val pm = context.packageManager
        val scannedVariants = mutableListOf<DetectedBedrockPackage>()
        var primaryInstalledPkg: String? = null
        var primaryLabel: String? = null
        var primaryVersionName: String? = null
        var primaryVersionCode: Long = 0L
        var primaryTier: BedrockVersionTier = preferredVersionTier

        for ((pkg, label, defaultTier) in KNOWN_MINECRAFT_PACKAGES) {
            try {
                val info: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(pkg, 0)
                }
                val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    info.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    info.versionCode.toLong()
                }
                val vName = info.versionName ?: defaultTier.shortLabel.removePrefix("v") + ".0"
                val resolvedTier = BedrockVersionTier.fromVersionString(vName)
                scannedVariants.add(
                    DetectedBedrockPackage(
                        packageName = pkg,
                        label = label,
                        isInstalled = true,
                        versionName = vName,
                        versionTier = resolvedTier
                    )
                )
                if (primaryInstalledPkg == null) {
                    primaryInstalledPkg = pkg
                    primaryLabel = label
                    primaryVersionName = vName
                    primaryVersionCode = vCode
                    primaryTier = resolvedTier
                }
            } catch (_: PackageManager.NameNotFoundException) {
                scannedVariants.add(
                    DetectedBedrockPackage(
                        packageName = pkg,
                        label = label,
                        isInstalled = false,
                        versionName = "Compatible (${defaultTier.shortLabel})",
                        versionTier = defaultTier
                    )
                )
            }
        }

        val schemeSupported = canResolveMinecraftScheme(pm)

        if (primaryInstalledPkg != null) {
            return MinecraftInstallStatus(
                isInstalled = true,
                packageName = primaryInstalledPkg,
                editionName = primaryLabel ?: "Minecraft Bedrock Edition",
                versionName = primaryVersionName ?: "1.26.0",
                versionCode = primaryVersionCode,
                deepLinkSupported = schemeSupported,
                stagedMojangPath = stagedMojangDir.absolutePath,
                linkedSafUri = linkedSafUri,
                detectedTier = primaryTier,
                compatibleModeActive = true,
                packageVariants = scannedVariants
            )
        }

        return MinecraftInstallStatus(
            isInstalled = schemeSupported,
            packageName = "com.mojang.minecraftpe",
            editionName = if (schemeSupported) {
                "Minecraft Bedrock (${preferredVersionTier.shortLabel} Deep-Link)"
            } else {
                "Minecraft Bedrock (${preferredVersionTier.shortLabel} Compatible Mode)"
            },
            versionName = if (schemeSupported) {
                "${preferredVersionTier.engineVersionDotString} (URI Ready)"
            } else {
                "${preferredVersionTier.shortLabel} Compatible Mode"
            },
            versionCode = 126000L,
            deepLinkSupported = schemeSupported,
            stagedMojangPath = stagedMojangDir.absolutePath,
            linkedSafUri = linkedSafUri,
            detectedTier = preferredVersionTier,
            compatibleModeActive = true,
            packageVariants = scannedVariants
        )
    }

    private fun canResolveMinecraftScheme(pm: PackageManager): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("minecraft://"))
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        return resolved.isNotEmpty()
    }

    fun launchMinecraft(
        context: Context,
        status: MinecraftInstallStatus,
        serverAddress: String? = null,
        targetPackageOverride: String? = null
    ): Boolean {
        val pm = context.packageManager
        val pkgToLaunch = targetPackageOverride?.takeIf { it.isNotBlank() && it != "internal.sandbox" }
            ?: status.packageName

        // 1. Try direct package launch intent across target package or any installed variant
        val candidatePackages = listOf(pkgToLaunch) +
            status.packageVariants.filter { it.isInstalled }.map { it.packageName }

        for (pkg in candidatePackages.distinct()) {
            val launchIntent = pm.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                return try {
                    context.startActivity(launchIntent)
                    true
                } catch (_: Exception) {
                    false
                }
            }
        }

        // 2. Try minecraft:// URI scheme (with optional server connection parameter)
        val uriString = if (!serverAddress.isNullOrBlank()) {
            "minecraft://?addExternalServer=FlarialServer|${serverAddress.trim()}:19132"
        } else {
            "minecraft://"
        }
        val schemeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(schemeIntent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openPlayStoreForMinecraft(context: Context) {
        val marketIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=com.mojang.minecraftpe")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(marketIntent)
        } catch (_: Exception) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=com.mojang.minecraftpe")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(webIntent)
            } catch (_: Exception) {
                // Ignore if no browser
            }
        }
    }

    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun buildOverlayPermissionIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
    }
}
