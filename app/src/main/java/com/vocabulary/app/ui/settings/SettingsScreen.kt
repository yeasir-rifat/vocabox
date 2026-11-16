package com.vocabulary.app.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vocabulary.app.data.model.ThemeMode
import com.vocabulary.app.ui.components.AppIcons
import com.vocabulary.app.ui.components.SettingsDivider
import com.vocabulary.app.ui.components.SettingsGroup
import com.vocabulary.app.ui.components.SettingsLabel
import com.vocabulary.app.ui.components.SettingsRow
import com.vocabulary.app.ui.components.SettingsValue
import com.vocabulary.app.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.util.Calendar

private val GOAL_OPTIONS = listOf(5, 10, 15, 20, 30, 50)

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showThemeDialog by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    var showGoalDialog by rememberSaveable { mutableStateOf(false) }

    // Re-checked every time the screen resumes, so returning from system settings updates it.
    var notificationsBlocked by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        notificationsBlocked = !NotificationManagerCompat.from(context).areNotificationsEnabled()
        onPauseOrDispose { }
    }

    // Android 13+: ask for the notification permission at the moment the reminder is switched on.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        notificationsBlocked = !NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.Content),
            verticalArrangement = Arrangement.spacedBy(Spacing.Standard)
        ) {
            Text(text = "Settings", style = MaterialTheme.typography.headlineMedium)

            // The profile card (ui/components/ProfileCard.kt) returns here once account login exists.

            SettingsLabel("Preferences")
            SettingsGroup {
                SettingsRow(
                    icon = AppIcons.Moon,
                    title = "Appearance",
                    onClick = { showThemeDialog = true },
                    trailing = { SettingsValue(state.themeMode.label()) }
                )
                SettingsDivider()
                SettingsRow(
                    icon = AppIcons.Bell,
                    title = "Word of the day",
                    subtitle = "A notification with a new word",
                    onClick = {
                        val turnOn = !state.reminder.enabled
                        viewModel.setReminderEnabled(turnOn)
                        if (turnOn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !NotificationManagerCompat.from(context).areNotificationsEnabled()
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    trailing = {
                        Switch(checked = state.reminder.enabled, onCheckedChange = null)
                    }
                )
                SettingsDivider()
                SettingsRow(
                    icon = AppIcons.Clock,
                    title = "Reminder time",
                    enabled = state.reminder.enabled,
                    onClick = { showTimePicker = true },
                    trailing = {
                        SettingsValue(formatTime(context, state.reminder.hour, state.reminder.minute))
                    }
                )
                if (state.reminder.enabled && notificationsBlocked) {
                    SettingsDivider()
                    SettingsRow(
                        icon = AppIcons.AlertCircle,
                        title = "Notifications are turned off",
                        subtitle = "Tap to open the app's notification settings",
                        destructive = true,
                        onClick = { openNotificationSettings(context) }
                    )
                }
                SettingsDivider()
                SettingsRow(
                    icon = AppIcons.Target,
                    title = "Daily goal",
                    subtitle = "Cards per day for your streak",
                    onClick = { showGoalDialog = true },
                    trailing = { SettingsValue("${state.dailyGoal}") }
                )
            }

            SettingsLabel("More", modifier = Modifier.padding(top = Spacing.Compact))
            SettingsGroup {
                SettingsRow(
                    icon = AppIcons.Info,
                    title = "About application",
                    trailing = { SettingsValue(appVersion(context), showChevron = false) }
                )
                SettingsDivider()
                SettingsRow(
                    icon = AppIcons.Trash,
                    title = "Reset progress",
                    destructive = true,
                    onClick = { showResetDialog = true }
                )
            }
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Appearance") },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .selectable(
                                    selected = state.themeMode == mode,
                                    role = Role.RadioButton,
                                    onClick = {
                                        viewModel.setThemeMode(mode)
                                        showThemeDialog = false
                                    }
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = state.themeMode == mode, onClick = null)
                            Spacer(Modifier.width(Spacing.Standard))
                            Text(mode.label(), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showThemeDialog = false }) { Text("Cancel") } }
        )
    }

    if (showGoalDialog) {
        val options = (GOAL_OPTIONS + state.dailyGoal).distinct().sorted()
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("Daily goal") },
            text = {
                Column {
                    options.forEach { goal ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .selectable(
                                    selected = state.dailyGoal == goal,
                                    role = Role.RadioButton,
                                    onClick = {
                                        viewModel.setDailyGoal(goal)
                                        showGoalDialog = false
                                    }
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = state.dailyGoal == goal, onClick = null)
                            Spacer(Modifier.width(Spacing.Standard))
                            Text("$goal cards", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showGoalDialog = false }) { Text("Cancel") } }
        )
    }

    if (showTimePicker) {
        ReminderTimeDialog(
            initialHour = state.reminder.hour,
            initialMinute = state.reminder.minute,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                viewModel.setReminderTime(hour, minute)
                showTimePicker = false
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset progress?") },
            text = {
                Text(
                    "Clears review progress, mistakes, quiz history, streaks and read marks. " +
                        "Words and favourites are kept. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        viewModel.resetProgress {
                            scope.launch { snackbarHostState.showSnackbar("Progress reset") }
                        }
                    }
                ) {
                    Text("Reset", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val context = LocalContext.current
    val timeState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = DateFormat.is24HourFormat(context)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state = timeState) },
        confirmButton = {
            TextButton(onClick = { onConfirm(timeState.hour, timeState.minute) }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatTime(context: Context, hour: Int, minute: Int): String {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    return DateFormat.getTimeFormat(context).format(cal.time)
}

private fun openNotificationSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", context.packageName, null))
    }
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun appVersion(context: Context): String =
    try {
        "v" + context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (e: Exception) {
        ""
    }
