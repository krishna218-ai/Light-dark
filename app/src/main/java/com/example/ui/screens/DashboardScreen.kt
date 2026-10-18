package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.BedrockVersionTier
import com.example.model.FeaturedBedrockServer
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.FlarialUiState
import com.example.viewmodel.MainNavTab

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    state: FlarialUiState,
    onLaunchMinecraft: (Boolean, String?) -> Unit,
    onSelectInstance: (String) -> Unit,
    onSelectVersionTier: (BedrockVersionTier) -> Unit = {},
    onOptimizeZeroLag: () -> Unit = {},
    onUpdatePlayerSettings: (String?, String?, Int?, String?) -> Unit,
    onAddCustomServer: (String, String, Int) -> Unit,
    onOpenPlayStore: () -> Unit,
    onRefreshDetection: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onToggleSystemOverlay: () -> Unit,
    onOpenClickGui: () -> Unit,
    onToggleModule: (String) -> Unit,
    onNavigateTab: (MainNavTab) -> Unit
) {
    var showEditPlayerDialog by remember { mutableStateOf(false) }
    var showAddServerDialog by remember { mutableStateOf(false) }

    val activeProfile = remember(state.profiles, state.activeProfileId) {
        state.profiles.find { it.id == state.activeProfileId } ?: state.profiles.firstOrNull()
    }
    val selectedInstance = remember(state.instances, state.selectedInstanceId) {
        state.instances.find { it.id == state.selectedInstanceId } ?: state.instances.first()
    }
    val enabledModsCount = remember(state.modules) { state.modules.count { it.enabled } }
    val enabledAddonsCount = remember(state.addons) { state.addons.count { it.enabled } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Direct Access Player Bar + Instant Zero-Lag Boost Pill
        item(key = "player_bar") {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(20.dp))
                    .testTag("launcher_player_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Isometric Voxel Skin Head Avatar Canvas
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(ObsidianSurface)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(36.dp)) {
                                val w = size.width
                                val h = size.height
                                drawRect(
                                    color = Color(0xFF1E293B),
                                    topLeft = Offset(w * 0.1f, h * 0.1f),
                                    size = Size(w * 0.8f, h * 0.8f)
                                )
                                drawRect(
                                    color = NeonViolet,
                                    topLeft = Offset(w * 0.06f, h * 0.06f),
                                    size = Size(w * 0.88f, h * 0.28f)
                                )
                                drawRect(
                                    color = NeonCyan,
                                    topLeft = Offset(w * 0.20f, h * 0.46f),
                                    size = Size(w * 0.22f, h * 0.14f)
                                )
                                drawRect(
                                    color = NeonCyan,
                                    topLeft = Offset(w * 0.58f, h * 0.46f),
                                    size = Size(w * 0.22f, h * 0.14f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = state.gamertag,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = NeonEmerald.copy(alpha = 0.18f)
                                ) {
                                    Text(
                                        text = "DIRECT ACCESS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonEmerald,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Pocket Edition & Bedrock (${state.selectedVersionTier.shortLabel}) • Account Login Skipped",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showEditPlayerDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("edit_player_profile_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tag", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // 2. Flagship Flarial Pocket Edition & Multi-Version Launcher Dock
        item(key = "hero_launcher_dock") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f),
                                ObsidianBorder
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .background(ObsidianSurface)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(330.dp)
                ) {
                    val w = size.width
                    val h = size.height

                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF0B132B),
                                Color(0xFF1C1142),
                                ObsidianSurface
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(w, h)
                        )
                    )

                    val stepX = w / 8f
                    for (i in 1..7) {
                        drawLine(
                            color = NeonCyan.copy(alpha = 0.07f),
                            start = Offset(i * stepX, 0f),
                            end = Offset(i * stepX, h),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val cubeCx = w * 0.84f
                    val cubeCy = h * 0.30f
                    val r = h * 0.20f
                    val hex = Path().apply {
                        moveTo(cubeCx, cubeCy - r)
                        lineTo(cubeCx + r * 0.86f, cubeCy - r * 0.5f)
                        lineTo(cubeCx + r * 0.86f, cubeCy + r * 0.5f)
                        lineTo(cubeCx, cubeCy + r)
                        lineTo(cubeCx - r * 0.86f, cubeCy + r * 0.5f)
                        lineTo(cubeCx - r * 0.86f, cubeCy - r * 0.5f)
                        close()
                    }
                    drawPath(
                        path = hex,
                        brush = Brush.verticalGradient(
                            listOf(NeonCyan.copy(alpha = 0.25f), NeonViolet.copy(alpha = 0.15f))
                        )
                    )
                    drawPath(
                        path = hex,
                        color = NeonCyan.copy(alpha = 0.45f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Top status row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            modifier = Modifier.border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                RoundedCornerShape(50)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "MCPE & BEDROCK • ${selectedInstance.badgeText}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = ObsidianBg.copy(alpha = 0.85f)
                        ) {
                            Text(
                                text = "${state.liveFps} FPS • ${state.livePingMs}ms • ZERO LAG",
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonEmerald,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = selectedInstance.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = selectedInstance.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Horizontal Instance / Pocket Edition Selector Pills
                    Text(
                        text = "SELECT POCKET EDITION / BEDROCK LAUNCH TARGET",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        state.instances.forEach { inst ->
                            val isSelected = inst.id == state.selectedInstanceId
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                                } else {
                                    ObsidianBg.copy(alpha = 0.75f)
                                },
                                modifier = Modifier
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else ObsidianBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onSelectInstance(inst.id) }
                                    .testTag("instance_chip_${inst.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = inst.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = inst.versionTag,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Animated Pre-Launch Injection Bar when launching
                    AnimatedVisibility(visible = state.isLaunchingSequence) {
                        Column(modifier = Modifier.padding(bottom = 12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = state.launchStageLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NeonCyan
                                )
                                Text(
                                    text = "${(state.launchProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NeonEmerald
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { state.launchProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = NeonCyan,
                                trackColor = ObsidianBg
                            )
                        }
                    }

                    // Prominent Launch Dock Row + 1-Tap Lag Fixer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onLaunchMinecraft(true, null) },
                            enabled = !state.isLaunchingSequence,
                            modifier = Modifier
                                .weight(1.45f)
                                .height(56.dp)
                                .testTag("launch_minecraft_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = ObsidianBg
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Launch Minecraft Pocket Edition",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = if (state.isLaunchingSequence) "PATCHING..." else "RUN POCKET / BEDROCK",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${state.selectedVersionTier.shortLabel} • $enabledModsCount Mods • $enabledAddonsCount Packs",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ObsidianBg.copy(alpha = 0.82f)
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onOptimizeZeroLag,
                            modifier = Modifier
                                .weight(0.95f)
                                .height(56.dp)
                                .testTag("fix_lag_boost_dashboard_button"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RocketLaunch,
                                contentDescription = "Fix Lag & Boost FPS",
                                tint = NeonEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = "FIX LAG",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonEmerald
                                )
                                Text(
                                    text = "180 FPS Boost",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Universal Multi-Version Compatibility & Soon Update Patcher Card (1.19 through 1.26+ & 1.27 Soon)
        item(key = "multi_version_patcher_card") {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                    .testTag("multi_version_compatibility_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "UNIVERSAL MULTI-VERSION & SOON UPDATE ENGINE",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Supports MCPE & Bedrock 1.19, 1.20, 1.21, 1.22, 1.23, 1.24, 1.25, 1.26+ & Upcoming Releases",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Live Status Badges ("Minecraft v1.26 Detected", "Injected Successfully", "Compatible Mode Active", "Soon Update Ready")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusPillBadge(
                            text = "Minecraft ${state.selectedVersionTier.shortLabel} Detected",
                            color = NeonEmerald
                        )
                        StatusPillBadge(
                            text = "Injected Successfully",
                            color = NeonCyan
                        )
                        StatusPillBadge(
                            text = "Compatible Mode Active",
                            color = NeonViolet
                        )
                        StatusPillBadge(
                            text = "Soon Update (v1.27) Pre-Patched",
                            color = NeonAmber
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Horizontal Version Selector Strip (1.19 -> 1.26+)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BedrockVersionTier.entries.forEach { tier ->
                            val selected = state.selectedVersionTier == tier
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else ObsidianSurface,
                                modifier = Modifier
                                    .border(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else ObsidianBorder,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onSelectVersionTier(tier) }
                                    .testTag("version_tier_chip_${tier.name}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = tier.shortLabel,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "API ${tier.scriptApiVersion}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ObsidianSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Active Schema: ${state.selectedVersionTier.fullTitle}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Path: com.mojang/{resource_packs, behavior_packs, minecraftWorlds} • ${state.threadBoostMode}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Update,
                                contentDescription = null,
                                tint = NeonEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Direct Server Quick-Launch Browser (1-Tap Join via minecraft://)
        item(key = "direct_server_browser") {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(20.dp))
                    .testTag("direct_server_browser_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "1-TAP POCKET & BEDROCK SERVER LAUNCHER",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Auto-connects via minecraft:// external server deep-link.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { showAddServerDialog = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("add_custom_server_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add IP", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.featuredServers.forEach { server ->
                            ServerQuickLaunchRow(
                                server = server,
                                onJoinServer = { onLaunchMinecraft(true, server.host) }
                            )
                        }
                    }
                }
            }
        }

        // 5. Engine Performance, Render Distance & Multi-Package Diagnostics Card
        item(key = "minecraft_detection_diagnostics") {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(20.dp))
                    .testTag("minecraft_detection_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NeonEmerald.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (state.installStatus.isInstalled) Icons.Default.CheckCircle else Icons.Default.Gamepad,
                                    contentDescription = null,
                                    tint = NeonEmerald
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (state.installStatus.isInstalled) {
                                        "Pocket / Bedrock APK Detected (${state.selectedVersionTier.shortLabel})"
                                    } else {
                                        "Pocket Edition Compatible Mode & 3D Simulator Active"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Target: ${state.installStatus.packageName} • ${state.installStatus.versionName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        IconButton(
                            onClick = onRefreshDetection,
                            modifier = Modifier.testTag("refresh_detection_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Re-scan Minecraft Packages",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Render Distance Chunks Slider (written to options.txt)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pocket Edition Chunk Distance (options.txt)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(
                            text = "${state.renderDistanceChunks} Chunks",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = state.renderDistanceChunks.toFloat(),
                        onValueChange = { onUpdatePlayerSettings(null, null, it.toInt(), null) },
                        valueRange = 5f..28f
                    )

                    // Readiness Badges Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ObsidianSurface,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (state.hasOverlayPermission) onToggleSystemOverlay()
                                    else onRequestOverlayPermission()
                                }
                                .testTag("overlay_status_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                state.isSystemOverlayRunning -> NeonEmerald
                                                state.hasOverlayPermission -> NeonCyan
                                                else -> NeonRose
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = when {
                                            state.isSystemOverlayRunning -> "Overlay Running"
                                            state.hasOverlayPermission -> "Start Overlay"
                                            else -> "Grant Overlay"
                                        },
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = if (state.hasOverlayPermission) "SYSTEM_ALERT_WINDOW OK" else "Tap to enable in Settings",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ObsidianSurface,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onOpenClickGui() }
                                .testTag("open_clickgui_dashboard_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Open ClickGUI",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "9 Modules & Scale",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    if (!state.installStatus.isInstalled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pocket Edition Simulator ready — or install official Minecraft APK from Play Store.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = onOpenPlayStore,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Play Store", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        // 6. Telemetry Metrics Overview Row
        item(key = "telemetry_metrics_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TelemetryMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "ACTIVE MODS",
                    value = "$enabledModsCount / ${state.modules.size}",
                    subtitle = "ClickGUI Ready",
                    accentColor = NeonCyan,
                    icon = Icons.Default.Speed,
                    onClick = { onNavigateTab(MainNavTab.MODULES) }
                )
                TelemetryMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "UNIVERSAL PACKS",
                    value = "$enabledAddonsCount Active",
                    subtitle = "${state.selectedVersionTier.shortLabel} Patched",
                    accentColor = NeonViolet,
                    icon = Icons.Default.Extension,
                    onClick = { onNavigateTab(MainNavTab.ADDONS) }
                )
                TelemetryMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "PROFILES & SYNC",
                    value = activeProfile?.targetVersionTier?.shortLabel ?: "v1.26+",
                    subtitle = activeProfile?.name ?: "Direct Mode",
                    accentColor = NeonRose,
                    icon = Icons.Default.Code,
                    onClick = { onNavigateTab(MainNavTab.PROFILES) }
                )
            }
        }

        // 7. Instant Utility Module Matrix (All 9 Modules including Time Changer & Game Speed/FOV)
        item(key = "instant_utility_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INSTANT UTILITY TOGGLES (9 MODULES)",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextSecondary
                )
                Text(
                    text = "Configure Sliders →",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { onNavigateTab(MainNavTab.MODULES) }
                        .padding(4.dp)
                )
            }
        }

        item(key = "instant_utility_matrix") {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = 2
            ) {
                state.modules.forEach { mod ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (mod.enabled) ObsidianCard else ObsidianSurface,
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = 1.dp,
                                color = if (mod.enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f) else ObsidianBorder,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onToggleModule(mod.id) }
                            .testTag("dashboard_quick_mod_${mod.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (mod.enabled) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                    } else {
                                        ObsidianBg
                                    }
                                ) {
                                    Text(
                                        text = mod.shortTag,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (mod.enabled) MaterialTheme.colorScheme.primary else TextSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Switch(
                                    checked = mod.enabled,
                                    onCheckedChange = { onToggleModule(mod.id) }
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = mod.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Keybind [${mod.keybind}] • Scale ${(mod.scale * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // 8. Live Launcher Console & Hook Terminal
        item(key = "launcher_console_terminal") {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
                    .testTag("launcher_console_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "FLARIAL MULTI-VERSION STATUS LOGS",
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonCyan
                            )
                        }
                        Text(
                            text = state.selectedVersionTier.shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonEmerald
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = ObsidianBorder)
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ObsidianBg)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        state.launcherLogs.takeLast(7).forEach { log ->
                            Text(
                                text = "[${log.timestamp}] [${log.tag}] ${log.message}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (log.isHighlight) NeonCyan else TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }

    // Player Gamertag & Cosmetic Customization Dialog
    if (showEditPlayerDialog) {
        var tagInput by remember(state.gamertag) { mutableStateOf(state.gamertag) }
        var chosenCosmetic by remember(state.selectedCosmetic) { mutableStateOf(state.selectedCosmetic) }
        val cosmetics = listOf(
            "Cyber Cyan Wings",
            "Void Violet Cape",
            "Crimson Dragon Aura",
            "Emerald Matrix Crown"
        )
        AlertDialog(
            onDismissRequest = { showEditPlayerDialog = false },
            containerColor = ObsidianSurface,
            title = { Text("Customize Launcher Identity") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        label = { Text("Pocket / Bedrock Gamertag") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "ACTIVE CLIENT COSMETIC",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    cosmetics.forEach { item ->
                        val selected = item == chosenCosmetic
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else ObsidianCard,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else ObsidianBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { chosenCosmetic = item }
                        ) {
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdatePlayerSettings(tagInput.trim().ifEmpty { "FlarialPlayer" }, chosenCosmetic, null, null)
                    showEditPlayerDialog = false
                }) {
                    Text("Save Identity")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPlayerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Custom Bedrock Server Dialog
    if (showAddServerDialog) {
        var srvName by remember { mutableStateOf("") }
        var srvHost by remember { mutableStateOf("") }
        var srvPort by remember { mutableStateOf("19132") }
        AlertDialog(
            onDismissRequest = { showAddServerDialog = false },
            containerColor = ObsidianSurface,
            title = { Text("Add Direct-Launch Bedrock Server") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = srvName,
                        onValueChange = { srvName = it },
                        label = { Text("Server Name") },
                        placeholder = { Text("e.g., Practice PvP") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = srvHost,
                        onValueChange = { srvHost = it },
                        label = { Text("Server IP / Domain") },
                        placeholder = { Text("e.g., play.hyperlands.net") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = srvPort,
                        onValueChange = { srvPort = it },
                        label = { Text("Port (Default 19132)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAddCustomServer(
                        srvName,
                        srvHost,
                        srvPort.toIntOrNull() ?: 19132
                    )
                    showAddServerDialog = false
                }) {
                    Text("Add Server")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddServerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatusPillBadge(
    text: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f),
        modifier = Modifier.border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(50))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ServerQuickLaunchRow(
    server: FeaturedBedrockServer,
    onJoinServer: () -> Unit
) {
    val accent = remember(server.accentHex) {
        runCatching {
            Color(android.graphics.Color.parseColor(server.accentHex))
        }.getOrDefault(NeonCyan)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ObsidianSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
            .testTag("server_row_${server.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = server.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${server.pingMs}ms • ${server.onlinePlayers} online",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonEmerald
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${server.host}:${server.port} • ${server.gameModes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = onJoinServer,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent.copy(alpha = 0.22f),
                    contentColor = accent
                ),
                modifier = Modifier
                    .border(1.dp, accent, RoundedCornerShape(50))
                    .testTag("join_server_${server.id}")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("JOIN", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun TelemetryMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ObsidianCard,
        modifier = modifier
            .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor
            )
        }
    }
}
