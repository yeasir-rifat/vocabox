package com.vocabulary.app.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vocabulary.app.ui.components.AppCard
import com.vocabulary.app.ui.components.AppIcons
import com.vocabulary.app.ui.theme.Spacing

/** The Words tab: three big cards, one for each way to study the words. */
@Composable
fun WordsHubScreen(
    onOpenList: () -> Unit,
    onOpenFlashcards: () -> Unit,
    onOpenGroups: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WordsHubViewModel = hiltViewModel()
) {
    val total by viewModel.totalWords.collectAsStateWithLifecycle()
    val read by viewModel.readWords.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.Content, vertical = Spacing.Content),
        verticalArrangement = Arrangement.spacedBy(Spacing.Standard)
    ) {
        Text(text = "Words", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = if (total == 1) "1 entry" else "$total entries",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HubCard(
            icon = AppIcons.List,
            title = "Word List",
            subtitle = "$read of $total read",
            onClick = onOpenList
        )
        HubCard(
            icon = AppIcons.Cards,
            title = "Flashcards",
            subtitle = "Flip a card, swipe to the next",
            onClick = onOpenFlashcards
        )
        HubCard(
            icon = AppIcons.Grid,
            title = "Groupwise",
            subtitle = "Study in small sets: Group 1, 2, 3...",
            onClick = onOpenGroups
        )
    }
}

@Composable
private fun HubCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.Card),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Spacing.Content),
                verticalArrangement = Arrangement.spacedBy(Spacing.Micro)
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
