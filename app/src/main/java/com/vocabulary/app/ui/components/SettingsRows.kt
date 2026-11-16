package com.vocabulary.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vocabulary.app.ui.theme.AppColors
import com.vocabulary.app.ui.theme.Spacing

/** Small caption above a [SettingsGroup]. */
@Composable
fun SettingsLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = Spacing.Micro)
    )
}

/** One rounded card that holds several [SettingsRow]s, split by [SettingsDivider]s. */
@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    AppCard(modifier = modifier.fillMaxWidth(), content = content)
}

/** Hairline between two rows, indented so it starts under the row text. */
@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = Spacing.Content + SETTINGS_ICON_SIZE + SETTINGS_ICON_GAP),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

private val SETTINGS_ICON_SIZE = 22.dp
private val SETTINGS_ICON_GAP = 14.dp

/**
 * One line of a settings list: icon, title, optional detail line and something on the right.
 *
 * - Pass [onClick] to make the row tappable. It then ends with a chevron unless [trailing] is given.
 * - For a switch, pass the Switch as [trailing] (with onCheckedChange = null) and toggle in [onClick],
 *   so the whole row is the tap target.
 * - [destructive] draws the row in the danger color, for actions that delete something.
 */
@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    destructive: Boolean = false,
    trailing: (@Composable () -> Unit)? = null
) {
    val contentColor = if (destructive) AppColors.danger else MaterialTheme.colorScheme.onSurface
    val tap = if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.4f)
            .then(tap)
            .heightIn(min = 56.dp)
            .padding(horizontal = Spacing.Content, vertical = Spacing.Compact),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(SETTINGS_ICON_SIZE)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = SETTINGS_ICON_GAP),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = contentColor)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        when {
            trailing != null -> trailing()
            onClick != null -> Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Current value of a setting, for the right side of a [SettingsRow]. */
@Composable
fun SettingsValue(text: String, modifier: Modifier = Modifier, showChevron: Boolean = true) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        if (showChevron) {
            Spacer(Modifier.width(Spacing.Micro))
            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
