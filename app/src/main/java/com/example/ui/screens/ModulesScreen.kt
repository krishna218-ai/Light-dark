package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.model.ClientModule
import com.example.model.ModuleCategory
import com.example.model.SettingItem
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
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ModulesScreen(
    state: FlarialUiState,
    onSelectCategory: (ModuleCategory?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleModule: (String) -> Unit,
    onToggleExpandModule: (String) -> Unit,
    onSliderChange: (String, String, Float) -> Unit,
    onScaleChange: (String, Float) -> Unit,
    onToggleSettingChange: (String, String, Boolean) -> Unit,
    onChoiceSettingChange: (String, String, String) -> Unit,
    onSetAllModulesEnabled: (Boolean) -> Unit,
    onResetHudPositions: () -> Unit,
    onOptimizeEngine: () -> Unit = {}
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val trimmedQuery = state.moduleSearchQuery.trim()

    val filteredModules = androidx.compose.runtime.remember(
        state.modules,
        state.selectedCategory,
        trimmedQuery
    ) {
        state.modules.filter { mod ->
            val matchesCategory = state.selectedCategory == null || mod.category == state.selectedCategory
            val matchesSearch = trimmedQuery.isEmpty() ||
                mod.name.contains(trimmedQuery, ignoreCase = true) ||
                mod.shortTag.contains(trimmedQuery, ignoreCase = true) ||
                mod.description.contains(trimmedQuery, ignoreCase = true) ||
                mod.keybind.equals(trimmedQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    val activeCount = androidx.compose.runtime.remember(state.modules) {
        state.modules.count { it.enabled }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("modules_screen_container")
    ) {
        // Persistent Sticky Search & Filter Header at Top of 'Modules' Tab
        Surface(
            color = ObsidianSurface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("persistent_modules_search_header")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // 1. Persistent Search Input Bar right at the top
                OutlinedTextField(
                    value = state.moduleSearchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("module_search_input"),
                    placeholder = {
                        Text(
                            text = "Search utility mods by name (FPS, Keystrokes, ESP, Time, Speed...)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Modules",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "${filteredModules.size}/${state.modules.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                            if (state.moduleSearchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSearchQueryChange("") },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("clear_module_search_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search query",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { keyboardController?.hide() }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ObsidianCard,
                        unfocusedContainerColor = ObsidianCard,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = ObsidianBorder
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Quick-Filter Name Pills + Category Filter Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = state.selectedCategory == null && state.moduleSearchQuery.isEmpty(),
                        onClick = {
                            onSelectCategory(null)
                            onSearchQueryChange("")
                        },
                        label = { Text("All (${state.modules.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    // Quick Name Filter Shortcuts for all utility mods
                    listOf("FPS", "Keystrokes", "ESP", "Fullbright", "Zoom", "XYZ", "Time", "Speed").forEach { quickName ->
                        val isQuickSelected = state.moduleSearchQuery.equals(quickName, ignoreCase = true)
                        FilterChip(
                            selected = isQuickSelected,
                            onClick = {
                                onSearchQueryChange(if (isQuickSelected) "" else quickName)
                            },
                            label = { Text(quickName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.24f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("quick_search_chip_${quickName.lowercase()}")
                        )
                    }

                    ModuleCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = state.selectedCategory == cat,
                            onClick = {
                                onSelectCategory(if (state.selectedCategory == cat) null else cat)
                            },
                            label = { Text(cat.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3. Compact Status & Batch Action Row with 1-Tap Lag Fixer & Optimizer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$activeCount Active • ${state.selectedVersionTier.shortLabel} Ready",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onOptimizeEngine,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = ObsidianBg
                            ),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("optimize_engine_modules_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Fix Lag & Optimize",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { onSetAllModulesEnabled(activeCount < state.modules.size) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("toggle_all_modules_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (activeCount < state.modules.size) "Enable All" else "Disable All",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        OutlinedButton(
                            onClick = onResetHudPositions,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("reset_modules_hud_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Reset HUD Positions",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = ObsidianBorder)

        // List-Based UI of Utility Mods (or Empty State if search has 0 matches)
        if (filteredModules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
                    .testTag("modules_empty_search_state"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No utility mods match \"${state.moduleSearchQuery}\"",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Try searching for FPS, Keystrokes, ESP, Fullbright, Zoom, Time, or Speed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            onSearchQueryChange("")
                            onSelectCategory(null)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = ObsidianBg
                        ),
                        modifier = Modifier.testTag("reset_search_filter_button")
                    ) {
                        Text("Show All Utility Mods")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("modules_lazy_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(filteredModules, key = { it.id }) { mod ->
                    ModuleListRowCard(
                        module = mod,
                        isAdvancedExpanded = state.expandedModuleId == mod.id,
                        onToggleEnabled = { onToggleModule(mod.id) },
                        onToggleAdvancedExpanded = { onToggleExpandModule(mod.id) },
                        onSliderChange = { key, value -> onSliderChange(mod.id, key, value) },
                        onScaleChange = { scaleVal -> onScaleChange(mod.id, scaleVal) },
                        onToggleSettingChange = { key, value -> onToggleSettingChange(mod.id, key, value) },
                        onChoiceSettingChange = { key, opt -> onChoiceSettingChange(mod.id, key, opt) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModuleListRowCard(
    module: ClientModule,
    isAdvancedExpanded: Boolean,
    onToggleEnabled: () -> Unit,
    onToggleAdvancedExpanded: () -> Unit,
    onSliderChange: (String, Float) -> Unit,
    onScaleChange: (Float) -> Unit,
    onToggleSettingChange: (String, Boolean) -> Unit,
    onChoiceSettingChange: (String, String) -> Unit
) {
    val accentColor = moduleAccentColor(module.id)
    val sliderSettings = module.settings.filterIsInstance<SettingItem.SliderSetting>()
    val toggleSettings = module.settings.filterIsInstance<SettingItem.ToggleSetting>()
    val choiceSettings = module.settings.filterIsInstance<SettingItem.ChoiceSetting>()

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ObsidianCard,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (module.enabled) 1.5.dp else 1.dp,
                color = if (module.enabled) accentColor.copy(alpha = 0.75f) else ObsidianBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("module_card_${module.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 1. Top List Row: Module Icon, Title, Category/Keybind Badges, and Primary Toggle Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (module.enabled) accentColor.copy(alpha = 0.18f)
                                else ObsidianSurface
                            )
                            .border(
                                width = 1.dp,
                                color = if (module.enabled) accentColor else ObsidianBorder,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = moduleIcon(module.id),
                            contentDescription = module.name,
                            tint = if (module.enabled) accentColor else TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = module.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (module.enabled) accentColor.copy(alpha = 0.18f) else ObsidianBg
                            ) {
                                Text(
                                    text = if (module.enabled) "● ${module.shortTag}" else "○ ${module.shortTag}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (module.enabled) accentColor else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ObsidianSurface
                            ) {
                                Text(
                                    text = "KEY [${module.keybind}]",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Primary Module Toggle Switch
                Switch(
                    checked = module.enabled,
                    onCheckedChange = { onToggleEnabled() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ObsidianBg,
                        checkedTrackColor = accentColor,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = ObsidianSurface
                    ),
                    modifier = Modifier.testTag("module_switch_${module.id}")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = module.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Always-Visible Inline Slider Configuration Panel for Every Module in the List
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ObsidianSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ObsidianBorder.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    sliderSettings.forEach { slider ->
                        val formattedValue = if (slider.max <= 10f) {
                            String.format(Locale.US, "%.2f%s", slider.value, slider.unit)
                        } else {
                            "${slider.value.roundToInt()}${slider.unit}"
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = slider.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = accentColor.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = formattedValue,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = accentColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Slider(
                                value = slider.value,
                                onValueChange = { onSliderChange(slider.key, it) },
                                valueRange = slider.min..slider.max,
                                colors = SliderDefaults.colors(
                                    thumbColor = accentColor,
                                    activeTrackColor = accentColor,
                                    inactiveTrackColor = ObsidianCard
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("module_slider_${module.id}_${slider.key}")
                            )
                        }
                    }

                    // Inline HUD Widget Scale Slider
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "HUD Widget Scale",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "${(module.scale * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = accentColor
                            )
                        }
                        Slider(
                            value = module.scale,
                            onValueChange = onScaleChange,
                            valueRange = 0.60f..1.60f,
                            colors = SliderDefaults.colors(
                                thumbColor = accentColor,
                                activeTrackColor = accentColor.copy(alpha = 0.85f),
                                inactiveTrackColor = ObsidianCard
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("module_scale_slider_${module.id}")
                        )
                    }
                }
            }

            // 3. Quick Secondary Sub-Toggles Strip + Expandable Options Drawer
            if (toggleSettings.isNotEmpty() || choiceSettings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onToggleAdvancedExpanded() }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SUB-OPTIONS & TOGGLES (${toggleSettings.count { it.value }}/${toggleSettings.size} ON)",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isAdvancedExpanded) "Hide" else "Customize",
                            style = MaterialTheme.typography.labelMedium,
                            color = accentColor
                        )
                        Icon(
                            imageVector = if (isAdvancedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand options",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = isAdvancedExpanded) {
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HorizontalDivider(color = ObsidianBorder)

                        toggleSettings.forEach { toggle ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = toggle.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (toggle.subtitle.isNotBlank()) {
                                        Text(
                                            text = toggle.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                Switch(
                                    checked = toggle.value,
                                    onCheckedChange = { onToggleSettingChange(toggle.key, it) },
                                    modifier = Modifier.testTag("sub_toggle_${module.id}_${toggle.key}")
                                )
                            }
                        }

                        choiceSettings.forEach { choice ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    text = choice.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    choice.options.forEach { option ->
                                        val isSelected = choice.selected == option
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    if (isSelected) accentColor.copy(alpha = 0.2f)
                                                    else ObsidianSurface
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) accentColor else ObsidianBorder,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .clickable { onChoiceSettingChange(choice.key, option) }
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = option,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isSelected) accentColor else TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun moduleIcon(moduleId: String): ImageVector = when (moduleId) {
    "fps_ping" -> Icons.Default.Speed
    "keystrokes" -> Icons.Default.Keyboard
    "entity_esp" -> Icons.Default.Radar
    "fullbright" -> Icons.Default.LightMode
    "zoom_fov" -> Icons.Default.ZoomIn
    "coordinates" -> Icons.Default.Explore
    "crosshair_mod" -> Icons.Default.CenterFocusStrong
    "time_changer" -> Icons.Default.AccessTime
    "game_speed_fov" -> Icons.Default.RocketLaunch
    else -> Icons.Default.CheckCircle
}

private fun moduleAccentColor(moduleId: String): Color = when (moduleId) {
    "fps_ping" -> NeonCyan
    "keystrokes" -> NeonViolet
    "entity_esp" -> NeonRose
    "fullbright" -> NeonAmber
    "zoom_fov" -> NeonCyan
    "coordinates" -> NeonEmerald
    "crosshair_mod" -> NeonViolet
    "time_changer" -> NeonAmber
    "game_speed_fov" -> NeonEmerald
    else -> NeonCyan
}
