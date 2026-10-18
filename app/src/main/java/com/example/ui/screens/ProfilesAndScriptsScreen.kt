package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.auth.GoogleSignInButton
import com.example.data.CloudConfigBackup
import com.example.model.AccentPreset
import com.example.model.BedrockVersionTier
import com.example.model.ClientProfile
import com.example.model.CustomScript
import com.example.model.LocalBackupFileEntry
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.CloudSyncUiState
import com.example.viewmodel.FlarialUiState
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfilesAndScriptsScreen(
    state: FlarialUiState,
    cloudSyncState: CloudSyncUiState,
    userEmail: String?,
    onSignOutClick: () -> Unit,
    onSyncToCloud: (String, String) -> Unit,
    onRestoreCloudBackup: (CloudConfigBackup) -> Unit,
    onDeleteCloudBackup: (CloudConfigBackup) -> Unit,
    onSaveQuickLocalBackup: (String) -> Unit,
    onRestoreLocalBackupFile: (LocalBackupFileEntry) -> Unit,
    onDeleteLocalBackupFile: (LocalBackupFileEntry) -> Unit,
    onApplyProfile: (ClientProfile) -> Unit,
    onSaveNewProfile: (String, String, BedrockVersionTier) -> Unit,
    onToggleScript: (String) -> Unit,
    onUpdateScriptCode: (String, String) -> Unit,
    onUpdatePreferences: (AccentPreset?, Float?, Boolean?, String?) -> Unit,
    onExportBackupClick: () -> Unit,
    onRestoreBackupClick: () -> Unit,
    onGenerateRawJson: () -> String,
    onApplyRawJson: (String) -> Unit,
    onAuthSuccessMessage: (String) -> Unit = {}
) {
    var showCreateProfileDialog by remember { mutableStateOf(false) }
    var showCloudUploadDialog by remember { mutableStateOf(false) }
    var showLocalSnapshotDialog by remember { mutableStateOf(false) }
    var showRawJsonDialog by remember { mutableStateOf(false) }
    var editingScript by remember { mutableStateOf<CustomScript?>(null) }
    var authErrorBanner by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profiles_scripts_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. CLOUD SYNC & LOCAL BACKUP SECTION HEADER + CARDS
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                    .testTag("cloud_sync_local_backup_section")
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
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "CLOUD SYNC & LOCAL BACKUP",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Direct Access Local .JSON Export/Restore + Optional Google Cloud Sync",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Account Status / Direct Access Mode Card
                    if (userEmail != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ObsidianSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = NeonEmerald,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = userEmail,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Firestore Enterprise Cloud Sync Active",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeonEmerald
                                        )
                                    }
                                }
                                OutlinedButton(
                                    onClick = onSignOutClick,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("google_sign_out_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = "Sign Out",
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Skip / Sign Out", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    } else {
                        // Direct Access Mode Active (Account Skipped)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ObsidianSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, NeonEmerald.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                                .testTag("direct_access_mode_banner")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = NeonEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "DIRECT ACCESS MODE ACTIVE (ACCOUNT GATE SKIPPED)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = NeonEmerald,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "All launcher features, Pocket Edition zero-lag boosts, and Local .JSON Backups work immediately without an account. Sign in with Google below only if you want cross-device Firestore Cloud Sync.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                if (authErrorBanner != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = authErrorBanner ?: "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonRose
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                GoogleSignInButton(
                                    onAuthSuccess = {
                                        authErrorBanner = null
                                        onAuthSuccessMessage("Google Account connected for Firestore Cloud Sync!")
                                    },
                                    onAuthError = { err ->
                                        authErrorBanner = err
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // SUBSECTION A: LOCAL STORAGE EXPORT & RESTORE (.JSON FILES)
                    Text(
                        text = "LOCAL STORAGE .JSON EXPORT & RESTORE (DIRECT ACCESS)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onExportBackupClick,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_json_backup_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = ObsidianBg
                            )
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export .JSON")
                        }

                        OutlinedButton(
                            onClick = onRestoreBackupClick,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("restore_json_backup_button")
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore .JSON")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showLocalSnapshotDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_local_snapshot_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Local Snapshot")
                        }

                        OutlinedButton(
                            onClick = { showRawJsonDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("inspect_raw_json_button")
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Raw JSON Editor")
                        }
                    }

                    // Saved Local Backup Files List
                    if (state.localBackups.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.localBackups.forEach { entry ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = ObsidianSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                                        .testTag("local_backup_item_${entry.fileName}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = NeonCyan,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = entry.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${entry.fileName} • ${(entry.sizeBytes / 1024f).coerceAtLeast(0.1f).let { String.format(Locale.US, "%.1f KB", it) }} • ${entry.moduleCount} Mods",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            OutlinedButton(
                                                onClick = { onRestoreLocalBackupFile(entry) },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.testTag("restore_local_file_${entry.fileName}")
                                            ) {
                                                Text("Restore", style = MaterialTheme.typography.labelSmall)
                                            }
                                            IconButton(
                                                onClick = { onDeleteLocalBackupFile(entry) },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .testTag("delete_local_file_${entry.fileName}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete local backup",
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = ObsidianBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    // SUBSECTION B: FIRESTORE CLOUD SYNC SNAPSHOTS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CLOUD CONFIGURATION SNAPSHOTS",
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonViolet,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (userEmail != null) {
                                    "Real-time cloud backups synced to your Google account"
                                } else {
                                    "Optional: Sign in with Google above to enable cloud backups"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        if (userEmail != null) {
                            Button(
                                onClick = { showCloudUploadDialog = true },
                                enabled = !state.isCloudSyncing,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonViolet,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("sync_to_cloud_button")
                            ) {
                                if (state.isCloudSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Syncing...", style = MaterialTheme.typography.labelMedium)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Upload to Cloud", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }

                    if (userEmail != null) {
                        Spacer(modifier = Modifier.height(10.dp))

                        when (cloudSyncState) {
                            is CloudSyncUiState.GuestDirectMode,
                            is CloudSyncUiState.Loading -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Loading cloud backups from Firestore...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            is CloudSyncUiState.Error -> {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = NeonRose.copy(alpha = 0.14f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, NeonRose.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                ) {
                                    Text(
                                        text = cloudSyncState.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NeonRose,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }

                            is CloudSyncUiState.Success -> {
                                if (cloudSyncState.backups.isEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = ObsidianSurface,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudDone,
                                                contentDescription = null,
                                                tint = TextSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "No cloud snapshots saved yet. Tap 'Upload to Cloud' to back up your current modules & HUD layout.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        cloudSyncState.backups.forEach { backup ->
                                            val displayTs = formatCloudTimestamp(backup.updatedAt ?: backup.createdAt)
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = ObsidianSurface,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, NeonViolet.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                                                    .testTag("cloud_backup_card_${backup.id}")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = backup.title,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = backup.description,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = TextSecondary
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = "${backup.moduleCount} Mods • ${backup.addonCount} Packs • $displayTs",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = NeonCyan
                                                        )
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Button(
                                                            onClick = { onRestoreCloudBackup(backup) },
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = MaterialTheme.colorScheme.primary,
                                                                contentColor = ObsidianBg
                                                            ),
                                                            modifier = Modifier.testTag("restore_cloud_backup_${backup.id}")
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.CloudDownload,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Restore", style = MaterialTheme.typography.labelSmall)
                                                        }
                                                        IconButton(
                                                            onClick = { onDeleteCloudBackup(backup) },
                                                            modifier = Modifier
                                                                .size(36.dp)
                                                                .testTag("delete_cloud_backup_${backup.id}")
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Delete,
                                                                contentDescription = "Delete cloud backup",
                                                                tint = TextSecondary,
                                                                modifier = Modifier.size(18.dp)
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
            }
        }

        // 2. Client Profiles & Multi-Version Presets Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MULTI-VERSION PROFILES & HUD PRESETS (1.19 - 1.26+)",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "Tailored configurations for PvP, Survival, and Pocket Edition Zero-Lag",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Button(
                    onClick = { showCreateProfileDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = ObsidianBg
                    ),
                    modifier = Modifier.testTag("save_new_profile_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Preset", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.profiles.forEach { profile ->
                    val isActive = profile.id == state.activeProfileId
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ObsidianCard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isActive) 1.5.dp else 1.dp,
                                color = if (isActive) MaterialTheme.colorScheme.primary else ObsidianBorder,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onApplyProfile(profile) }
                            .testTag("profile_card_${profile.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = profile.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = NeonViolet.copy(alpha = 0.18f)
                                    ) {
                                        Text(
                                            text = profile.targetVersionTier.shortLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeonViolet,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                    if (isActive) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = NeonEmerald.copy(alpha = 0.18f)
                                        ) {
                                            Text(
                                                text = "ACTIVE",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = NeonEmerald,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = profile.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${profile.enabledModules.size} Modules • ${profile.enabledAddons.size} Packs • ${profile.accentPreset.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            if (isActive) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Active Profile",
                                    tint = NeonEmerald
                                )
                            } else {
                                OutlinedButton(onClick = { onApplyProfile(profile) }) {
                                    Text("Load")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Advanced Molang & Bedrock Script Hooks
        item {
            Text(
                text = "ADVANCED BEDROCK SCRIPT & MOLANG HOOKS",
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.scripts.forEach { script ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ObsidianCard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (script.enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else ObsidianBorder,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .testTag("script_card_${script.id}")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = script.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Trigger: ${script.trigger}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Switch(
                                    checked = script.enabled,
                                    onCheckedChange = { onToggleScript(script.id) }
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = script.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ObsidianBg)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = script.code,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeonCyan
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = { editingScript = script },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Edit Script JSON", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Client Neon Theme & Global Preferences Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CLIENT NEON ACCENT & ENGINE SETTINGS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AccentPreset.entries.forEach { preset ->
                            val isSelected = state.accentPreset == preset
                            val pColor = runCatching {
                                Color(android.graphics.Color.parseColor(preset.primaryHex))
                            }.getOrDefault(NeonCyan)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) pColor.copy(alpha = 0.2f) else ObsidianSurface,
                                modifier = Modifier
                                    .border(
                                        1.dp,
                                        if (isSelected) pColor else ObsidianBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onUpdatePreferences(preset, null, null, null) }
                                    .testTag("accent_preset_${preset.name}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(pColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = preset.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) pColor else TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Global Bedrock UI Scale", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${(state.globalUiScale * 100).roundToInt()}%",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = state.globalUiScale,
                        onValueChange = { onUpdatePreferences(null, it, null, null) },
                        valueRange = 0.65f..1.40f
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Start Floating Overlay on Launch", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "Starts the draggable ClickGUI window when launching Minecraft",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = state.autoStartOverlay,
                            onCheckedChange = { onUpdatePreferences(null, null, it, null) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = state.targetServerHost,
                        onValueChange = { onUpdatePreferences(null, null, null, it) },
                        label = { Text("Ping Telemetry Target Bedrock Server") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // Upload to Cloud Dialog
    if (showCloudUploadDialog) {
        var cloudTitleInput by remember { mutableStateOf("") }
        var cloudDescInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCloudUploadDialog = false },
            containerColor = ObsidianSurface,
            title = { Text("Sync Config to Firestore Cloud") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = cloudTitleInput,
                        onValueChange = { cloudTitleInput = it },
                        label = { Text("Cloud Snapshot Name") },
                        placeholder = { Text("e.g., Hive Ranked 180FPS Setup") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cloud_backup_title_input")
                    )
                    OutlinedTextField(
                        value = cloudDescInput,
                        onValueChange = { cloudDescInput = it },
                        label = { Text("Notes / Description") },
                        placeholder = { Text("Optional summary of active mods & packs") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cloud_backup_desc_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSyncToCloud(cloudTitleInput, cloudDescInput)
                        showCloudUploadDialog = false
                    },
                    modifier = Modifier.testTag("confirm_cloud_upload_button")
                ) {
                    Text("Upload to Cloud")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloudUploadDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Save Local Snapshot Dialog
    if (showLocalSnapshotDialog) {
        var localTitleInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showLocalSnapshotDialog = false },
            containerColor = ObsidianSurface,
            title = { Text("Save Local .JSON Snapshot") },
            text = {
                OutlinedTextField(
                    value = localTitleInput,
                    onValueChange = { localTitleInput = it },
                    label = { Text("Snapshot File Label") },
                    placeholder = { Text("e.g., Zeqa Duels Reticle & Keystrokes") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("local_snapshot_title_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveQuickLocalBackup(localTitleInput)
                        showLocalSnapshotDialog = false
                    },
                    modifier = Modifier.testTag("confirm_local_snapshot_button")
                ) {
                    Text("Save .JSON File")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocalSnapshotDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Create New Profile Dialog with Target Minecraft Version Tier selector
    if (showCreateProfileDialog) {
        var nameInput by remember { mutableStateOf("") }
        var descInput by remember { mutableStateOf("") }
        var selectedTier by remember { mutableStateOf(state.selectedVersionTier) }
        AlertDialog(
            onDismissRequest = { showCreateProfileDialog = false },
            containerColor = ObsidianSurface,
            title = { Text("Save Multi-Version Profile Preset") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Profile Name") },
                        placeholder = { Text("e.g., Pocket Edition 120FPS PvP") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text("Description") },
                        placeholder = { Text("Custom HUD layout & active packs") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Target Minecraft Version Tier:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BedrockVersionTier.entries.forEach { tier ->
                            val isChosen = selectedTier == tier
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isChosen) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else ObsidianCard,
                                modifier = Modifier
                                    .border(
                                        1.dp,
                                        if (isChosen) MaterialTheme.colorScheme.primary else ObsidianBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedTier = tier }
                            ) {
                                Text(
                                    text = tier.shortLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isChosen) MaterialTheme.colorScheme.primary else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSaveNewProfile(nameInput, descInput, selectedTier)
                    showCreateProfileDialog = false
                }) {
                    Text("Save JSON Preset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateProfileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Script Code Dialog
    editingScript?.let { script ->
        var codeText by remember(script) { mutableStateOf(script.code) }
        AlertDialog(
            onDismissRequest = { editingScript = null },
            containerColor = ObsidianSurface,
            title = { Text("Edit ${script.name}") },
            text = {
                OutlinedTextField(
                    value = codeText,
                    onValueChange = { codeText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    textStyle = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateScriptCode(script.id, codeText)
                    editingScript = null
                }) {
                    Text("Save Hook")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingScript = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Live Raw JSON Config Editor Dialog
    if (showRawJsonDialog) {
        var rawJsonText by remember { mutableStateOf(onGenerateRawJson()) }
        AlertDialog(
            onDismissRequest = { showRawJsonDialog = false },
            containerColor = ObsidianSurface,
            title = { Text("flarial_client_state.json") },
            text = {
                OutlinedTextField(
                    value = rawJsonText,
                    onValueChange = { rawJsonText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    textStyle = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                Button(onClick = {
                    onApplyRawJson(rawJsonText)
                    showRawJsonDialog = false
                }) {
                    Text("Apply & Save JSON")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRawJsonDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

private fun formatCloudTimestamp(timestamp: Timestamp?): String {
    val safeTs = timestamp ?: Timestamp.now()
    val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.US)
    return fmt.format(safeTs.toDate())
}
