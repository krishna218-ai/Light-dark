package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.model.CrosshairConfig
import com.example.model.CrosshairStyle
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HudAndCrosshairScreen(
    state: FlarialUiState,
    onDragHudWidget: (String, Float, Float) -> Unit,
    onResetHudLayout: () -> Unit,
    onSimulateKey: (String) -> Unit,
    onSimulateClick: (Boolean) -> Unit,
    onToggleZoomPreview: () -> Unit,
    onUpdateCrosshairStyle: (CrosshairStyle) -> Unit,
    onUpdateCrosshairParams: (Float?, Float?, Float?, Boolean?, Boolean?, Boolean?, String?) -> Unit,
    onTogglePixelCell: (Int) -> Unit,
    onResetOrClearGrid: (Boolean) -> Unit
) {
    val fullbrightMod = state.modules.find { it.id == "fullbright" }
    val zoomMod = state.modules.find { it.id == "zoom_fov" }
    val espMod = state.modules.find { it.id == "entity_esp" }
    val fpsMod = state.modules.find { it.id == "fps_ping" }
    val keystrokesMod = state.modules.find { it.id == "keystrokes" }
    val coordsMod = state.modules.find { it.id == "coordinates" }
    val crosshairMod = state.modules.find { it.id == "crosshair_mod" }

    val isFullbrightOn = fullbrightMod?.enabled == true
    val gammaPct = fullbrightMod?.sliderValue("gamma_level", 850f) ?: 100f
    val zoomFactor = if (state.isZoomActive) (zoomMod?.sliderValue("zoom_factor", 4f) ?: 4f) else 1f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("hud_crosshair_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Live Draggable HUD Viewport & Bedrock World Simulator
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(22.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "LIVE BEDROCK HUD & OPTICS SANDBOX",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Drag any HUD widget directly on the viewport to reposition.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        OutlinedButton(
                            onClick = onResetHudLayout,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("reset_hud_positions_button")
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset Layout", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Interactive 16:9 Viewport Container
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(275.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                            .testTag("live_hud_viewport")
                    ) {
                        val density = LocalDensity.current
                        val viewportWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
                        val viewportHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

                        // Custom 3D Voxel World + Entity ESP + Crosshair Canvas
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val horizonY = h * 0.48f

                            // Sky brightness responds dynamically to Fullbright toggle & Gamma slider
                            val brightnessFactor = if (isFullbrightOn) {
                                (gammaPct / 1000f).coerceIn(0.35f, 1.0f)
                            } else {
                                0.16f
                            }

                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(
                                            red = 0.05f + 0.18f * brightnessFactor,
                                            green = 0.08f + 0.30f * brightnessFactor,
                                            blue = 0.16f + 0.45f * brightnessFactor
                                        ),
                                        Color(
                                            red = 0.08f + 0.24f * brightnessFactor,
                                            green = 0.12f + 0.38f * brightnessFactor,
                                            blue = 0.20f + 0.42f * brightnessFactor
                                        )
                                    ),
                                    startY = 0f,
                                    endY = horizonY
                                )
                            )

                            // Voxel Terrain Ground
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(
                                            red = 0.06f + 0.15f * brightnessFactor,
                                            green = 0.16f + 0.38f * brightnessFactor,
                                            blue = 0.12f + 0.22f * brightnessFactor
                                        ),
                                        Color(0xFF090D16)
                                    ),
                                    startY = horizonY,
                                    endY = h
                                ),
                                topLeft = Offset(0f, horizonY),
                                size = Size(w, h - horizonY)
                            )

                            // Perspective Voxel Chunk Grid Lines (scales with Optic Zoom!)
                            val cx = w / 2f
                            val cy = h / 2f
                            val gridCols = 8
                            for (i in -gridCols..gridCols) {
                                val topX = cx + (i * (w / 14f)) * zoomFactor
                                val botX = cx + (i * (w / 3.5f)) * zoomFactor
                                drawLine(
                                    color = NeonCyan.copy(alpha = 0.16f),
                                    start = Offset(topX, horizonY),
                                    end = Offset(botX, h),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }

                            // Entity ESP Hitbox Simulation if Entity ESP is enabled (or previewed)
                            val targetSize = 36.dp.toPx() * zoomFactor.coerceIn(1f, 2.4f)
                            val targetLeft = cx + 48.dp.toPx() - targetSize / 2f
                            val targetTop = horizonY - targetSize * 0.65f

                            // Draw Voxel Player Target Figure
                            drawRect(
                                color = Color(0xFF38BDF8).copy(alpha = 0.65f),
                                topLeft = Offset(targetLeft + targetSize * 0.25f, targetTop),
                                size = Size(targetSize * 0.5f, targetSize * 1.3f)
                            )

                            if (espMod?.enabled == true) {
                                // 3D Red/Cyan Hitbox Box + Eye Vector Ray
                                drawRect(
                                    color = NeonRose,
                                    topLeft = Offset(targetLeft, targetTop - 4.dp.toPx()),
                                    size = Size(targetSize, targetSize * 1.45f),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                if (espMod.toggleValue("eye_vector", true)) {
                                    drawLine(
                                        color = NeonCyan,
                                        start = Offset(targetLeft + targetSize / 2f, targetTop + targetSize * 0.25f),
                                        end = Offset(targetLeft - targetSize * 0.8f, targetTop + targetSize * 0.4f),
                                        strokeWidth = 2.dp.toPx()
                                    )
                                }
                            }

                            // Center Custom Crosshair Reticle
                            if (crosshairMod?.enabled != false) {
                                drawCustomCrosshair(
                                    config = state.crosshair,
                                    center = Offset(cx, cy),
                                    isAttacking = state.lmbCps > 6
                                )
                            }
                        }

                        // 1. Draggable FPS & Ping HUD Widget
                        if (fpsMod?.enabled == true) {
                            DraggableHudBox(
                                xNorm = fpsMod.hudX,
                                yNorm = fpsMod.hudY,
                                viewportWidthPx = viewportWidthPx,
                                viewportHeightPx = viewportHeightPx,
                                onDragDelta = { dx, dy -> onDragHudWidget(fpsMod.id, dx, dy) }
                            ) {
                                Text(
                                    text = "${state.liveFps} FPS  |  ${state.livePingMs} ms",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Frame: ${state.liveFrameTimeMs}ms • ${state.targetServerHost.substringBefore(".")}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        // 2. Draggable Coordinates XYZ + Nether HUD Widget
                        if (coordsMod?.enabled == true) {
                            DraggableHudBox(
                                xNorm = coordsMod.hudX,
                                yNorm = coordsMod.hudY,
                                viewportWidthPx = viewportWidthPx,
                                viewportHeightPx = viewportHeightPx,
                                onDragDelta = { dx, dy -> onDragHudWidget(coordsMod.id, dx, dy) }
                            ) {
                                Text(
                                    text = "XYZ: ${state.playerX}, ${state.playerY.toInt()}, ${state.playerZ}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = NeonEmerald
                                )
                                if (coordsMod.toggleValue("nether_coords", true)) {
                                    val nx = (state.playerX / 8f * 10f).roundToInt() / 10f
                                    val nz = (state.playerZ / 8f * 10f).roundToInt() / 10f
                                    Text(
                                        text = "Nether: $nx, ${state.playerY.toInt()}, $nz  [SE]",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        // 3. Draggable Keystrokes (WASD + CPS) HUD Widget
                        if (keystrokesMod?.enabled == true) {
                            DraggableHudBox(
                                xNorm = keystrokesMod.hudX,
                                yNorm = keystrokesMod.hudY,
                                viewportWidthPx = viewportWidthPx,
                                viewportHeightPx = viewportHeightPx,
                                onDragDelta = { dx, dy -> onDragHudWidget(keystrokesMod.id, dx, dy) }
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    KeystrokeBox("W", "W" in state.activeKeys)
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        KeystrokeBox("A", "A" in state.activeKeys)
                                        KeystrokeBox("S", "S" in state.activeKeys)
                                        KeystrokeBox("D", "D" in state.activeKeys)
                                    }
                                    if (keystrokesMod.toggleValue("show_cps", true)) {
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "LMB: ${state.lmbCps} CPS | RMB: ${state.rmbCps}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        // 4. Draggable Fullbright & Optic Zoom Badge
                        if (isFullbrightOn || state.isZoomActive) {
                            DraggableHudBox(
                                xNorm = fullbrightMod?.hudX ?: 0.70f,
                                yNorm = fullbrightMod?.hudY ?: 0.06f,
                                viewportWidthPx = viewportWidthPx,
                                viewportHeightPx = viewportHeightPx,
                                onDragDelta = { dx, dy ->
                                    fullbrightMod?.let { onDragHudWidget(it.id, dx, dy) }
                                }
                            ) {
                                Text(
                                    text = buildString {
                                        if (isFullbrightOn) append("☀ GAMMA ${gammaPct.toInt()}% ")
                                        if (state.isZoomActive) append("🔍 ZOOM ${zoomFactor}x")
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NeonAmber
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Interactive Test Bar for Keystrokes, CPS, and Optic Zoom
                    Text(
                        text = "INTERACTIVE HUD TEST CONTROLS (TAP KEYS & CPS)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("W", "A", "S", "D").forEach { key ->
                            val active = key in state.activeKeys
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (active) MaterialTheme.colorScheme.primary else ObsidianSurface,
                                modifier = Modifier
                                    .clickable { onSimulateKey(key) }
                                    .testTag("sim_key_$key")
                            ) {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (active) ObsidianBg else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NeonRose.copy(alpha = 0.2f),
                            modifier = Modifier
                                .border(1.dp, NeonRose, RoundedCornerShape(8.dp))
                                .clickable { onSimulateClick(true) }
                                .testTag("sim_lmb_cps_button")
                        ) {
                            Text(
                                text = "⚔ LMB Click (${state.lmbCps} CPS)",
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonRose,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (state.isZoomActive) NeonCyan else ObsidianSurface,
                            modifier = Modifier
                                .border(1.dp, NeonCyan, RoundedCornerShape(8.dp))
                                .clickable { onToggleZoomPreview() }
                                .testTag("sim_optic_zoom_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ZoomIn,
                                    contentDescription = null,
                                    tint = if (state.isZoomActive) ObsidianBg else NeonCyan,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (state.isZoomActive) "Zoom Active (${zoomFactor.toInt()}x)" else "Test Zoom",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (state.isZoomActive) ObsidianBg else NeonCyan
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Custom Crosshair Designer Studio
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(22.dp))
                    .testTag("crosshair_designer_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusStrong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "CUSTOM CROSSHAIR DESIGNER STUDIO",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Select a procedural reticle shape or paint a custom 9x9 pixel crosshair.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Reticle Shape Selector
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CrosshairStyle.entries.forEach { style ->
                            val selected = state.crosshair.style == style
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else ObsidianSurface,
                                modifier = Modifier
                                    .border(
                                        1.dp,
                                        if (selected) MaterialTheme.colorScheme.primary else ObsidianBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onUpdateCrosshairStyle(style) }
                                    .testTag("crosshair_style_${style.name}")
                            ) {
                                Text(
                                    text = style.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) MaterialTheme.colorScheme.primary else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Neon Reticle Color Swatches
                    Text(
                        text = "RETICLE NEON COLOR",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val colorSwatches = listOf(
                        "#00F0FF" to NeonCyan,
                        "#FF2E93" to NeonRose,
                        "#10B981" to NeonEmerald,
                        "#A855F7" to NeonViolet,
                        "#F59E0B" to NeonAmber,
                        "#FFFFFF" to Color.White
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        colorSwatches.forEach { (hex, col) ->
                            val isSelected = state.crosshair.colorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) Color.White else ObsidianBorder,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        onUpdateCrosshairParams(null, null, null, null, null, null, hex)
                                    }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sliders for Size, Thickness, Gap
                    CrosshairSliderRow(
                        label = "Reticle Size",
                        value = state.crosshair.size,
                        range = 6f..28f,
                        unit = "px",
                        onValueChange = { onUpdateCrosshairParams(it, null, null, null, null, null, null) }
                    )
                    CrosshairSliderRow(
                        label = "Line Thickness",
                        value = state.crosshair.thickness,
                        range = 1f..6f,
                        unit = "px",
                        onValueChange = { onUpdateCrosshairParams(null, it, null, null, null, null, null) }
                    )
                    CrosshairSliderRow(
                        label = "Center Gap",
                        value = state.crosshair.gap,
                        range = 0f..12f,
                        unit = "px",
                        onValueChange = { onUpdateCrosshairParams(null, null, it, null, null, null, null) }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Center Dot", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = state.crosshair.dotEnabled,
                            onCheckedChange = { onUpdateCrosshairParams(null, null, null, it, null, null, null) }
                        )
                        Text("Dark Outline", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = state.crosshair.outlineEnabled,
                            onCheckedChange = { onUpdateCrosshairParams(null, null, null, null, it, null, null) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = ObsidianBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    // 9x9 Pixel Matrix Editor
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GridOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "9x9 PIXEL MATRIX EDITOR (TAP CELLS)",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onResetOrClearGrid(true) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Clear", style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = { onResetOrClearGrid(false) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = ObsidianBg
                                )
                            ) {
                                Text("Default Cross", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val activeColor = parseHexColorSafe(state.crosshair.colorHex)
                        for (row in 0 until 9) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (col in 0 until 9) {
                                    val idx = row * 9 + col
                                    val filled = state.crosshair.pixelGrid.getOrElse(idx) { false }
                                    val isCenter = row == 4 && col == 4
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (filled) activeColor else ObsidianSurface
                                            )
                                            .border(
                                                width = if (isCenter) 1.5.dp else 1.dp,
                                                color = if (isCenter) NeonRose else ObsidianBorder,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .clickable { onTogglePixelCell(idx) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DraggableHudBox(
    xNorm: Float,
    yNorm: Float,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    onDragDelta: (Float, Float) -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (xNorm * viewportWidthPx).roundToInt(),
                    y = (yNorm * viewportHeightPx).roundToInt()
                )
            }
            .pointerInput(viewportWidthPx, viewportHeightPx) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDragDelta(
                        dragAmount.x / viewportWidthPx,
                        dragAmount.y / viewportHeightPx
                    )
                }
            }
            .clip(RoundedCornerShape(8.dp))
            .background(ObsidianBg.copy(alpha = 0.80f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun KeystrokeBox(label: String, isPressed: Boolean) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (isPressed) MaterialTheme.colorScheme.primary else ObsidianSurface.copy(alpha = 0.85f)
            )
            .border(
                1.dp,
                if (isPressed) MaterialTheme.colorScheme.primary else ObsidianBorder,
                RoundedCornerShape(4.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isPressed) ObsidianBg else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CrosshairSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = String.format(Locale.US, "%.1f %s", value, unit),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}

private fun DrawScope.drawCustomCrosshair(
    config: CrosshairConfig,
    center: Offset,
    isAttacking: Boolean
) {
    val mainColor = parseHexColorSafe(config.colorHex)
    val dynamicGap = if (config.dynamicHitExpand && isAttacking) config.gap + 5f else config.gap
    val sizePx = config.size.dp.toPx()
    val thickPx = config.thickness.dp.toPx()
    val gapPx = dynamicGap.dp.toPx()

    if (config.style == CrosshairStyle.PIXEL_CUSTOM) {
        val cellPx = (config.size / 4f).coerceAtLeast(2.5f).dp.toPx()
        val startX = center.x - 4.5f * cellPx
        val startY = center.y - 4.5f * cellPx
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                if (config.pixelGrid.getOrElse(r * 9 + c) { false }) {
                    drawRect(
                        color = mainColor,
                        topLeft = Offset(startX + c * cellPx, startY + r * cellPx),
                        size = Size(cellPx, cellPx)
                    )
                }
            }
        }
        return
    }

    when (config.style) {
        CrosshairStyle.CROSS -> {
            if (config.outlineEnabled) {
                val outStroke = thickPx + 2.dp.toPx()
                drawLine(Color.Black, Offset(center.x - sizePx - gapPx, center.y), Offset(center.x - gapPx, center.y), outStroke)
                drawLine(Color.Black, Offset(center.x + gapPx, center.y), Offset(center.x + sizePx + gapPx, center.y), outStroke)
                drawLine(Color.Black, Offset(center.x, center.y - sizePx - gapPx), Offset(center.x, center.y - gapPx), outStroke)
                drawLine(Color.Black, Offset(center.x, center.y + gapPx), Offset(center.x, center.y + sizePx + gapPx), outStroke)
            }
            drawLine(mainColor, Offset(center.x - sizePx - gapPx, center.y), Offset(center.x - gapPx, center.y), thickPx)
            drawLine(mainColor, Offset(center.x + gapPx, center.y), Offset(center.x + sizePx + gapPx, center.y), thickPx)
            drawLine(mainColor, Offset(center.x, center.y - sizePx - gapPx), Offset(center.x, center.y - gapPx), thickPx)
            drawLine(mainColor, Offset(center.x, center.y + gapPx), Offset(center.x, center.y + sizePx + gapPx), thickPx)
        }

        CrosshairStyle.CIRCLE_DOT -> {
            if (config.outlineEnabled) {
                drawCircle(Color.Black, radius = sizePx, center = center, style = Stroke(width = thickPx + 2.dp.toPx()))
            }
            drawCircle(mainColor, radius = sizePx, center = center, style = Stroke(width = thickPx))
        }

        CrosshairStyle.CHEVRON -> {
            val path = Path().apply {
                moveTo(center.x - sizePx, center.y + sizePx * 0.6f)
                lineTo(center.x, center.y - gapPx)
                lineTo(center.x + sizePx, center.y + sizePx * 0.6f)
            }
            if (config.outlineEnabled) {
                drawPath(path, Color.Black, style = Stroke(width = thickPx + 2.dp.toPx()))
            }
            drawPath(path, mainColor, style = Stroke(width = thickPx))
        }

        CrosshairStyle.DIAMOND -> {
            val path = Path().apply {
                moveTo(center.x, center.y - sizePx)
                lineTo(center.x + sizePx, center.y)
                lineTo(center.x, center.y + sizePx)
                lineTo(center.x - sizePx, center.y)
                close()
            }
            if (config.outlineEnabled) {
                drawPath(path, Color.Black, style = Stroke(width = thickPx + 2.dp.toPx()))
            }
            drawPath(path, mainColor, style = Stroke(width = thickPx))
        }

        CrosshairStyle.PIXEL_CUSTOM -> Unit
    }

    if (config.dotEnabled) {
        drawCircle(mainColor, radius = (thickPx * 0.8f).coerceAtLeast(2f), center = center)
    }
}

private fun parseHexColorSafe(hex: String): Color {
    return runCatching {
        Color(android.graphics.Color.parseColor(hex))
    }.getOrDefault(NeonCyan)
}
