package com.vocabulary.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.vocabulary.app.data.model.ThemeMode
import com.vocabulary.app.data.prefs.AppPreferences
import com.vocabulary.app.data.seed.SeedStatus
import com.vocabulary.app.data.seed.WordSeedImporter
import com.vocabulary.app.ui.SeedGate
import com.vocabulary.app.ui.VocabularyRoot
import com.vocabulary.app.ui.theme.VocabularyTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var preferences: AppPreferences
    @Inject lateinit var seedImporter: WordSeedImporter

    /** Word to open, set when the daily word notification is tapped. */
    private var openWordId by mutableStateOf<Long?>(null)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) openWordId = wordIdFrom(intent)
        requestNotificationPermissionOnce()
        setContent {
            // Null until DataStore answers. The themed window background (cream or espresso) covers that
            // moment, so nothing blocks the main thread and nothing flashes.
            val themeFlow: Flow<ThemeMode?> = preferences.themeMode
            val themeMode by themeFlow.collectAsStateWithLifecycle(initialValue = null)
            val seedStatus by seedImporter.status.collectAsStateWithLifecycle()
            val mode = themeMode
            if (mode != null) {
                val darkTheme = when (mode) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                }
                VocabularyTheme(darkTheme = darkTheme) {
                    if (seedStatus == SeedStatus.READY) {
                        VocabularyRoot(
                            openWordId = openWordId,
                            onWordOpened = {
                                intent.removeExtra(EXTRA_WORD_ID)
                                openWordId = null
                            }
                        )
                    } else {
                        SeedGate(
                            status = seedStatus,
                            onRetry = { lifecycleScope.launch { seedImporter.importIfNeeded() } }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openWordId = wordIdFrom(intent)
    }

    private fun wordIdFrom(intent: Intent?): Long? =
        if (intent?.hasExtra(EXTRA_WORD_ID) == true) intent.getLongExtra(EXTRA_WORD_ID, 0L) else null

    /** Android 13+ needs a runtime permission for notifications. Asked once, on first launch. */
    private fun requestNotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        lifecycleScope.launch {
            if (preferences.notificationPermissionAsked.first()) return@launch
            val granted = ContextCompat.checkSelfPermission(
                this@MainActivity, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) return@launch
            preferences.setNotificationPermissionAsked()
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        const val EXTRA_WORD_ID = "extra_word_id"
    }
}
