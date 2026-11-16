package com.vocabulary.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vocabulary.app.ui.theme.Spacing

/**
 * Bottom bar with two actions: a filled main one on the right and an outlined one on the left.
 * Same look as the "Quiz this group" bar on the group page, so lists end the same way everywhere.
 */
@Composable
fun ActionBar(
    primaryLabel: String,
    primaryIcon: ImageVector?,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    secondaryIcon: ImageVector?,
    onSecondary: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.Content),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Standard)
            ) {
                OutlinedButton(
                    onClick = onSecondary,
                    enabled = enabled,
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(
                        2.dp,
                        if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                ) { ActionLabel(secondaryLabel, secondaryIcon) }
                Button(
                    onClick = onPrimary,
                    enabled = enabled,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                ) { ActionLabel(primaryLabel, primaryIcon) }
            }
        }
    }
}

@Composable
private fun ActionLabel(text: String, icon: ImageVector?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.Compact))
        }
        Text(text = text, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}
