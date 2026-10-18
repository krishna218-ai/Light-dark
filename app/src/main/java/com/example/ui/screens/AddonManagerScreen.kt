package com.example.ui.screens

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AddonType
import com.example.model.BedrockVersionTier
import com.example.model.MinecraftAddon
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.FlarialUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddonManagerScreen(
    state: FlarialUiState,
    onSelectFilter: (AddonType?) -> Unit,
    onToggleAddon: (String) -> Unit,
    onSelectSubpack: (String, String) -> Unit,
    onMoveAddonPriority: (String, Boolean) -> Unit,
    onImportAddonClick: () -> Unit,
    onLinkMojangFolderClick: () -> Unit,
    onExportAddonClick: (MinecraftAddon) -> Unit,
    onInspectManifest: (MinecraftAddon) -> String,
    onSelectVersionTier: (BedrockVersionTier) -> Unit = {},
    onPatchAllAddons: () -> Unit = {}
) {
    var inspectedAddon by remember { mutableStateOf<MinecraftAddon?>(null) }

    val filteredAddons = remember(state.addons, state.selectedAddonFilter) {
        state.addons.filter { addon ->
            state.selectedAddonFilter == null || addon.type == state.selectedAddonFilter
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("addons_screen_container")
    ) {
        // Top Universal Multi-Version Injector & Scoped Storage Toolbar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianSurface)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onImportAddonClick,
                    modifier = Modifier
                        .weight(1.2f)
                        .height(48.dp)
                        .testTag("import_addon_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = ObsidianBg
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Inject .mcpack / .mcworld / .zip",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = onLinkMojangFolderClick,
                    modifier = Modifier
                        .weight(0.95f)
                        .height(48.dp)
                        .testTag("link_mojang_folder_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.linkedSafUri != null) "SAF Linked" else "Link com.mojang",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Universal 1.19 - 1.26+ & Soon Update Schema Patcher Bar
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ObsidianCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeonViolet.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = NeonEmerald,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "UNIVERSAL INJECTOR (${state.selectedVersionTier.shortLabel} + SOON UPDATE)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = state.lastInjectionStatusLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }

                        Button(
                            onClick = onPatchAllAddons,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonViolet,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.testTag("addon_patch_all_schemas_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Patch All (${state.selectedVersionTier.shortLabel})",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Version Tier Pills for Manifest Schema Adaptation
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BedrockVersionTier.entries.forEach { tier ->
                            val isSelected = state.selectedVersionTier == tier
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else ObsidianSurface,
                                modifier = Modifier
                                    .border(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else ObsidianBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSelectVersionTier(tier) }
                                    .testTag("addon_tier_chip_${tier.name.lowercase()}")
                            ) {
                                Text(
                                    text = tier.shortLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scoped Storage Fallback & Directory Path Banner
            val isFallbackMode = state.linkedSafUri == null
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ObsidianCard,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isFallbackMode) {
                                "COMPATIBLE STAGED FALLBACK ACTIVE (ANDROID 12-15+)"
                            } else {
                                "DIRECT SAF COM.MOJANG BRIDGE ACTIVE"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isFallbackMode) NeonAmber else NeonEmerald
                        )
                        Text(
                            text = state.linkedSafUri
                                ?: ".../${state.selectedVersionTier.mojangSubpath}/{resource_packs, behavior_packs, minecraftWorlds}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = "${state.addons.count { it.enabled }}/${state.addons.size} Active",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips (.mcpack, .mcaddon, .mcworld, .zip)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.selectedAddonFilter == null,
                    onClick = { onSelectFilter(null) },
                    label = { Text("All Formats (${state.addons.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                AddonType.entries.forEach { type ->
                    FilterChip(
                        selected = state.selectedAddonFilter == type,
                        onClick = { onSelectFilter(type) },
                        label = { Text(type.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        HorizontalDivider(color = ObsidianBorder)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(filteredAddons, key = { _, item -> item.id }) { index, addon ->
                AddonItemCard(
                    addon = addon,
                    isFirst = index == 0,
                    isLast = index == filteredAddons.lastIndex,
                    onToggle = { onToggleAddon(addon.id) },
                    onSelectSubpack = { sub -> onSelectSubpack(addon.id, sub) },
                    onMoveUp = { onMoveAddonPriority(addon.id, true) },
                    onMoveDown = { onMoveAddonPriority(addon.id, false) },
                    onInspectManifest = { inspectedAddon = addon },
                    onExportMcpack = { onExportAddonClick(addon) }
                )
            }
        }
    }

    // Manifest.json Inspector Modal
    inspectedAddon?.let { addon ->
        val manifestJson = remember(addon, state.selectedVersionTier) { onInspectManifest(addon) }
        AlertDialog(
            onDismissRequest = { inspectedAddon = null },
            containerColor = ObsidianSurface,
            title = {
                Column {
                    Text(
                        text = "${addon.name} • manifest.json",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Schema Format v${state.selectedVersionTier.schemaFormatVersion} • Engine ${addon.minEngineVersion}–${addon.maxCompatibleVersion}",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonEmerald
                    )
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ObsidianBg)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = manifestJson,
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonCyan
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val target = addon
                    inspectedAddon = null
                    onExportAddonClick(target)
                }) {
                    Text("Export ${addon.type.fileExtension}")
                }
            },
            dismissButton = {
                TextButton(onClick = { inspectedAddon = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddonItemCard(
    addon: MinecraftAddon,
    isFirst: Boolean,
    isLast: Boolean,
    onToggle: () -> Unit,
    onSelectSubpack: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onInspectManifest: () -> Unit,
    onExportMcpack: () -> Unit
) {
    val badgeColor = runCatching {
        Color(android.graphics.Color.parseColor(addon.type.badgeColorHex))
    }.getOrDefault(NeonCyan)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (addon.enabled) 1.5.dp else 1.dp,
                color = if (addon.enabled) badgeColor.copy(alpha = 0.65f) else ObsidianBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("addon_card_${addon.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = badgeColor.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = addon.type.label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NeonEmerald.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "MC ${addon.minEngineVersion}–${addon.maxCompatibleVersion}",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonEmerald,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = "${addon.version} • ${addon.fileSizeKb} KB",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = addon.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = addon.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Switch(
                    checked = addon.enabled,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.testTag("addon_switch_${addon.id}")
                )
            }

            // Subpack Resolution / Mode Selector if available
            if (addon.subpacks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "SUBPACK / TIER CONFIGURATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    addon.subpacks.forEach { sub ->
                        val isSelected = addon.selectedSubpack == sub
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else ObsidianSurface,
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else ObsidianBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelectSubpack(sub) }
                        ) {
                            Text(
                                text = sub,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else TextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = ObsidianBorder)
            Spacer(modifier = Modifier.height(8.dp))

            // Footer Actions: Priority Order, Inspect Manifest, Export .mcpack/.mcworld
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = !isFirst,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Raise Load Priority",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = !isLast,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Lower Load Priority",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "com.mojang/${addon.type.folderName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onInspectManifest,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("inspect_manifest_${addon.id}")
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Manifest", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = onExportMcpack,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("export_mcpack_${addon.id}")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(addon.type.fileExtension, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
