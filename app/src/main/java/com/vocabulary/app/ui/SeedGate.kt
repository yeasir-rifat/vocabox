package com.vocabulary.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.vocabulary.app.data.seed.SeedStatus
import com.vocabulary.app.ui.theme.Spacing

/** Full-screen state shown while the word list is imported, or when the import failed. */
@Composable
fun SeedGate(status: SeedStatus, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        if (status == SeedStatus.FAILED) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.Content),
                modifier = Modifier.padding(Spacing.Section)
            ) {
                Text(
                    "The word list could not be loaded.",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Button(shape = MaterialTheme.shapes.small, onClick = onRetry) { Text("Try again") }
            }
        } else {
            CircularProgressIndicator()
        }
    }
}
