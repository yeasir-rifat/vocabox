package com.vocabulary.app.ui.admin

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vocabulary.app.ui.components.AppIcons
import com.vocabulary.app.ui.components.SettingsDivider
import com.vocabulary.app.ui.components.SettingsGroup
import com.vocabulary.app.ui.components.SettingsLabel
import com.vocabulary.app.ui.components.SettingsRow
import com.vocabulary.app.ui.theme.AppColors
import com.vocabulary.app.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.onFilePicked(uri, displayName(context, uri))
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) viewModel.export(uri) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Admin") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.Content),
            verticalArrangement = Arrangement.spacedBy(Spacing.Standard)
        ) {
            SettingsLabel("Signed in as admin")
            SettingsGroup {
                SettingsRow(
                    icon = AppIcons.Shield,
                    title = state.email.ifBlank { "Admin" },
                    subtitle = "Changes you make here reach every phone on the next sync"
                )
            }

            SettingsLabel("Word list")
            SettingsGroup {
                SettingsRow(
                    icon = AppIcons.Upload,
                    title = "Import words.json",
                    subtitle = "Adds new words and updates the same id. Words missing from the file are kept",
                    enabled = !state.busy,
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
                )
                SettingsDivider()
                SettingsRow(
                    icon = AppIcons.Download,
                    title = "Export words.json",
                    subtitle = "Save the full current list as a backup",
                    enabled = !state.busy,
                    onClick = { exportLauncher.launch("words.json") }
                )
            }

            if (state.busy) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(Spacing.Standard))
                    Text(state.busyText, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    state.preview?.let { preview ->
        val plan = preview.plan
        val blocked = plan.errors.isNotEmpty()
        AlertDialog(
            onDismissRequest = viewModel::dismissPreview,
            title = { Text(if (blocked) "Problems in the file" else "Merge into database?") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Compact)
                ) {
                    if (state.fileName.isNotBlank()) {
                        Text(state.fileName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (blocked) {
                        Text("Nothing was changed. Fix these and import again:")
                        plan.errors.take(MAX_ERRORS_SHOWN).forEach {
                            Text("• $it", style = MaterialTheme.typography.bodyMedium, color = AppColors.danger)
                        }
                        if (plan.errors.size > MAX_ERRORS_SHOWN) {
                            Text("...and ${plan.errors.size - MAX_ERRORS_SHOWN} more.")
                        }
                    } else {
                        CountLine("New words", plan.added)
                        CountLine("Updated", plan.updated)
                        CountLine("Unchanged", plan.unchanged)
                        CountLine("Total after merge", plan.merged.size, bold = true)
                        Text(
                            "Words that are not in this file stay as they are. The list is uploaded to Firebase and saved on this phone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                if (!blocked) TextButton(onClick = viewModel::confirmImport) { Text("Merge") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissPreview) { Text(if (blocked) "Close" else "Cancel") }
            }
        )
    }
}

@Composable
private fun CountLine(label: String, count: Int, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = if (bold) FontWeight.SemiBold else null)
        Text("$count", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

private const val MAX_ERRORS_SHOWN = 8

private fun displayName(context: android.content.Context, uri: Uri): String =
    try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: uri.lastPathSegment.orEmpty()
    } catch (e: Exception) {
        uri.lastPathSegment.orEmpty()
    }
