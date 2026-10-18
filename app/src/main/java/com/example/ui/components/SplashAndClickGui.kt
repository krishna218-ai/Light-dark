package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ClientModule
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun FlarialSplashScreen(
    onSkipSplash: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1A1443),
                        ObsidianSurface,
                        ObsidianBg
                    )
                )
            )
            .clickable { onSkipSplash() }
            .testTag("splash_screen_container"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(32.dp)
                .widthIn(max = 420.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(150.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val baseR = (size.minDimension / 2.6f) * pulseScale

                    // Outer tactical radar ring
                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.22f),
                        radius = baseR * 1.25f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = NeonViolet.copy(alpha = 0.38f),
                        radius = baseR * 1.05f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Isometric Hexagon Voxel Core
                    val hexPath = Path()
                    for (i in 0..5) {
                        val angleRad = Math.toRadians((60.0 * i - 30.0))
                        val px = cx + (baseR * 0.85f * cos(angleRad)).toFloat()
                        val py = cy + (baseR * 0.85f * sin(angleRad)).toFloat()
                        if (i == 0) hexPath.moveTo(px, py) else hexPath.lineTo(px, py)
                    }
                    hexPath.close()

                    drawPath(
                        path = hexPath,
                        brush = Brush.linearGradient(
                            colors = listOf(NeonCyan.copy(alpha = 0.35f), NeonViolet.copy(alpha = 0.35f))
                        )
                    )
                    drawPath(
                        path = hexPath,
                        color = NeonCyan,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Center stylized Flarial reticle lines
                    val chLen = baseR * 0.35f
                    drawLine(
                        color = NeonCyan,
                        start = Offset(cx - chLen, cy),
                        end = Offset(cx + chLen, cy),
                        strokeWidth = 3.dp.toPx()
                    )
                    drawLine(
                        color = NeonCyan,
                        start = Offset(cx, cy - chLen),
                        end = Offset(cx, cy + chLen),
                        strokeWidth = 3.dp.toPx()
                    )
                    drawCircle(
                        color = NeonRose,
                        radius = 4.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "FLARIAL CLIENT",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "MINECRAFT BEDROCK UTILITY & HUD ENGINE",
                style = MaterialTheme.typography.labelMedium,
                color = NeonCyan
            )

            Spacer(modifier = Modifier.height(28.dp))

            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = NeonCyan,
                trackColor = ObsidianCard
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Mounting com.mojang Pack Bridge & ClickGUI Overlay...",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FloatingClickGuiModal(
    visible: Boolean,
    modules: List<ClientModule>,
    liveFps: Int,
    livePingMs: Int,
    globalUiScale: Float,
    hasOverlayPermission: Boolean,
    isSystemOverlayRunning: Boolean,
    onDismiss: () -> Unit,
    onToggleModule: (String) -> Unit,
    onUpdateUiScale: (Float) -> Unit,
    onToggleSystemOverlay: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onLaunchMinecraft: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .widthIn(max = 580.dp)
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .testTag("floating_clickgui_modal"),
                shape = RoundedCornerShape(22.dp),
                color = ObsidianSurface.copy(alpha = 0.97f),
                tonalElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "ClickGUI",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "FLARIAL CLICKGUI OVERLAY",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$liveFps FPS  •  ${livePingMs}ms Ping  •  ${modules.count { it.enabled }}/${modules.size} Mods Active",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_clickgui_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close ClickGUI"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = ObsidianBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    // System Floating Overlay Control Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ObsidianCard,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSystemOverlayRunning) NeonEmerald
                                                else if (hasOverlayPermission) NeonCyan
                                                else NeonRose
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isSystemOverlayRunning) {
                                            "In-Game Floating Window: ACTIVE"
                                        } else if (hasOverlayPermission) {
                                            "System Floating Window: Ready"
                                        } else {
                                            "System Overlay Permission Required"
                                        },
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Draws a draggable ⚡ FLARIAL ClickGUI pill over Minecraft Bedrock Edition.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            if (hasOverlayPermission) {
                                Button(
                                    onClick = onToggleSystemOverlay,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSystemOverlayRunning) NeonRose else MaterialTheme.colorScheme.primary,
                                        contentColor = ObsidianBg
                                    ),
                                    modifier = Modifier.testTag("toggle_system_overlay_button")
                                ) {
                                    Text(if (isSystemOverlayRunning) "Stop Overlay" else "Start Overlay")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = onRequestOverlayPermission,
                                    modifier = Modifier.testTag("grant_overlay_permission_modal_btn")
                                ) {
                                    Text("Grant Access")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "QUICK MODULE MATRIX (TAP TO TOGGLE)",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        maxItemsInEachRow = 2
                    ) {
                        modules.forEach { mod ->
                            val isEnabled = mod.enabled
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isEnabled) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                } else {
                                    ObsidianCard
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        width = 1.dp,
                                        color = if (isEnabled) MaterialTheme.colorScheme.primary else ObsidianBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onToggleModule(mod.id) }
                                    .testTag("clickgui_toggle_${mod.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mod.shortTag,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Key: [${mod.keybind}]",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                    Switch(
                                        checked = isEnabled,
                                        onCheckedChange = { onToggleModule(mod.id) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Global HUD Scale Slider inside ClickGUI
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Global HUD & GUI Scale",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Text(
                            text = "${(globalUiScale * 100).roundToInt()}%",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = globalUiScale,
                        onValueChange = onUpdateUiScale,
                        valueRange = 0.65f..1.40f
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Back to Client")
                        }
                        Button(
                            onClick = {
                                onDismiss()
                                onLaunchMinecraft()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("clickgui_launch_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = ObsidianBg
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Launch Game")
                        }
                    }
                }
            }
        }
    }
}
