package com.vocabulary.app.ui.list

import com.vocabulary.app.data.model.WordView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vocabulary.app.data.model.ListOptions
import com.vocabulary.app.data.model.ReadFilter
import com.vocabulary.app.data.model.WordOrder
import com.vocabulary.app.data.model.WordType
import com.vocabulary.app.ui.components.AppIcons
import com.vocabulary.app.ui.components.SegmentedControl
import com.vocabulary.app.ui.components.label
import com.vocabulary.app.ui.components.levelLabel
import com.vocabulary.app.ui.theme.Spacing

/** What the list is showing, without the word count: "Unread · A–Z · Hard · B C M". */
fun ListOptions.summaryParts(filters: WordFilters = WordFilters()): List<String> = buildList {
    when (read) {
        ReadFilter.READ -> add("Read")
        ReadFilter.UNREAD -> add("Unread")
        ReadFilter.ALL -> Unit
    }
    if (favouriteOnly) add("Favourites")
    type?.let { add(it.label()) }
    level?.let { add(levelLabel(it)) }
    add(if (order == WordOrder.A_TO_Z) "A\u2013Z" else "Random")
    if (letters.isNotEmpty()) add(letters.sorted().joinToString(" "))
}

/** "Unread · A–Z · 20 words · B C M": what the list is showing right now, in one line. */
fun ListOptions.summary(resultCount: Int, filters: WordFilters = WordFilters()): String {
    val parts = summaryParts(filters).toMutableList()
    // The word count sits right after the order, before any letters.
    val countIndex = parts.indexOfFirst { it == "A\u2013Z" || it == "Random" } + 1
    parts.add(countIndex, if (resultCount == 1) "1 word" else "$resultCount words")
    return parts.joinToString(" \u00B7 ")
}

/** One-line summary of the list settings. Tapping it opens the settings panel. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListSummaryRow(
    options: ListOptions,
    resultCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filters: WordFilters = WordFilters()
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 40.dp)
                .padding(horizontal = Spacing.Standard, vertical = Spacing.Compact),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = options.summary(resultCount, filters),
                style = MaterialTheme.typography.bodyMedium,
                color = if (options.isActive || filters.isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = AppIcons.Settings,
                contentDescription = "List settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Settings panel that slides in from the right. Every change applies at once: the big number at
 * the top and the button at the bottom always show how many words the list will have. Place it
 * as the last child of a Box that fills the screen so it draws over the content.
 */
@Composable
fun ListOptionsSidebar(
    visible: Boolean,
    options: ListOptions,
    filters: WordFilters,
    resultCount: Int,
    availableLetters: Set<Char>,
    onToggleLetter: (Char) -> Unit,
    onClearLetters: () -> Unit,
    stripEnabled: Boolean,
    onStripChange: (Boolean) -> Unit,
    onOptionsChange: (ListOptions) -> Unit,
    onTypeSelected: (WordType?) -> Unit,
    onLevelSelected: (Int?) -> Unit,
    onShuffleAgain: () -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
    showStripSwitch: Boolean = true,
    showAppearance: Boolean = false,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = visible, onBack = onClose)

    val screenWidth = LocalConfiguration.current.screenWidthDp
    val panelWidth = minOf(screenWidth * 0.88f, 380f).dp
    val panelShape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(visible = visible, enter = fadeIn(tween(220)), exit = fadeOut(tween(180))) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClose
                    )
            )
        }

        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = slideInHorizontally(tween(260)) { it },
            exit = slideOutHorizontally(tween(200)) { it }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(panelWidth)
                    // Swallow taps so they do not fall through to the scrim.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = panelShape,
                color = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shadowElevation = 16.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    PanelHeader(
                        canReset = options.isActive || filters.isActive,
                        onReset = onReset,
                        onClose = onClose
                    )
                    ResultLine(options = options, filters = filters, resultCount = resultCount)

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = Spacing.Content, vertical = Spacing.Compact),
                        verticalArrangement = Arrangement.spacedBy(Spacing.Section)
                    ) {
                        SettingsGroup("Filter by") {
                            DropdownSelector(
                                title = "Type",
                                selected = filters.type,
                                options = listOf<Pair<WordType?, String>>(null to "All types") +
                                    WordType.entries.map { it to it.label() },
                                onSelected = onTypeSelected
                            )
                            DropdownSelector(
                                title = "Level",
                                selected = filters.level,
                                options = listOf<Pair<Int?, String>>(null to "All levels") +
                                    listOf(1, 2, 3).map { it to levelLabel(it) },
                                onSelected = onLevelSelected
                            )
                            LetterPicker(
                                selected = options.letters,
                                available = availableLetters,
                                onToggle = onToggleLetter,
                                onClear = onClearLetters
                            )
                        }

                        SettingsGroup("Show") {
                            SegmentedControl(
                                options = ReadFilter.entries.toList(),
                                selected = options.read,
                                label = {
                                    when (it) {
                                        ReadFilter.ALL -> "All"
                                        ReadFilter.READ -> "Read"
                                        ReadFilter.UNREAD -> "Unread"
                                    }
                                },
                                onSelect = { onOptionsChange(options.copy(read = it)) }
                            )
                            FavouriteRow(
                                checked = options.favouriteOnly,
                                onCheckedChange = { onOptionsChange(options.copy(favouriteOnly = it)) }
                            )
                        }

                        SettingsGroup("Order") {
                            SegmentedControl(
                                options = WordOrder.entries.toList(),
                                selected = options.order,
                                label = { if (it == WordOrder.A_TO_Z) "A\u2013Z" else "Random" },
                                onSelect = { order ->
                                    onOptionsChange(
                                        if (order == WordOrder.RANDOM) {
                                            options.copy(order = order, shuffleSeed = System.nanoTime())
                                        } else {
                                            options.copy(order = order)
                                        }
                                    )
                                }
                            )
                            if (options.order == WordOrder.RANDOM) {
                                TextButton(onClick = onShuffleAgain) {
                                    Icon(AppIcons.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Shuffle again")
                                }
                            }
                        }

                        SettingsGroup("Number of words") {
                            CountPicker(
                                count = options.count,
                                onCountChange = { onOptionsChange(options.copy(count = it)) }
                            )
                        }

                        if (showAppearance) {
                            SettingsGroup("Appearance") {
                                FieldLabel("How a word opens")
                                WordView.entries.forEach { view ->
                                    AppearanceRow(
                                        title = when (view) {
                                            WordView.DETAILS -> "Word Details"
                                            WordView.POPUP -> "Popup Modal"
                                            WordView.SHEET -> "Bottom Popup"
                                        },
                                        subtitle = when (view) {
                                            WordView.DETAILS -> "Opens the full word page"
                                            WordView.POPUP -> "A card in the middle of the screen"
                                            WordView.SHEET -> "A sheet from the bottom, swipe between words"
                                        },
                                        selected = options.wordView == view,
                                        onClick = { onOptionsChange(options.copy(wordView = view)) }
                                    )
                                }
                            }
                        }

                        if (showStripSwitch) {
                            SettingsGroup("Letter strip") {
                                StripRow(
                                    checked = stripEnabled,
                                    onCheckedChange = onStripChange
                                )
                            }
                        }

                    }


                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Button(
                        onClick = onClose,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.Content)
                            .heightIn(min = 54.dp)
                    ) {
                        Text(
                            text = if (resultCount == 1) "Show 1 word" else "Show $resultCount words",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelHeader(canReset: Boolean, onReset: () -> Unit, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.Content, end = Spacing.Micro, top = Spacing.Compact),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "List settings",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onReset, enabled = canReset) { Text("Reset all") }
        IconButton(onClick = onClose) {
            Icon(AppIcons.Close, contentDescription = "Close settings")
        }
    }
}

/** The live number of words under the title, with what the list is currently showing. */
@Composable
private fun ResultLine(options: ListOptions, filters: WordFilters, resultCount: Int) {
    val animatedCount by animateIntAsState(resultCount, tween(320), label = "resultCount")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.Content)
            .padding(bottom = Spacing.Compact)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "$animatedCount",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (resultCount == 1) "word" else "words",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Text(
            text = options.summaryParts(filters).joinToString(" \u00B7 "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** A labelled group: small spaced capitals above a soft card. */
@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Spacing.Micro, bottom = Spacing.Compact)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                .padding(Spacing.Standard),
            verticalArrangement = Arrangement.spacedBy(Spacing.Standard)
        ) { content() }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** A rounded pill option. Selected pills are filled, the others have a hairline outline. */
@Composable
private fun Pill(text: String, selected: Boolean, onClick: () -> Unit) {
    val background by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(180),
        label = "pillBackground"
    )
    val content by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(180),
        label = "pillContent"
    )
    Box(
        modifier = Modifier
            .heightIn(min = 36.dp)
            .clip(CircleShape)
            .background(background)
            .border(
                width = 1.dp,
                color = if (selected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1)
    }
}

@Composable
private fun FavouriteRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AppIcons.StarFilled,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = "Favourites only",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Spacing.Standard)
        )
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** Multi-select A-Z letters: keeps only the words that start with any chosen letter. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LetterPicker(
    selected: Set<Char>,
    available: Set<Char>,
    onToggle: (Char) -> Unit,
    onClear: () -> Unit
) {
    val letters = ('A'..'Z').toList() + if ('#' in available || '#' in selected) listOf('#') else emptyList()
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Compact)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Starting letters",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (selected.isNotEmpty()) {
                TextButton(onClick = onClear) { Text("Clear") }
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            letters.forEach { letter ->
                val isSelected = letter in selected
                LetterPill(
                    letter = letter,
                    selected = isSelected,
                    // A chosen letter can always be dropped, even if nothing matches it now.
                    enabled = isSelected || letter in available,
                    onClick = { onToggle(letter) }
                )
            }
        }
    }
}

@Composable
private fun LetterPill(letter: Char, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (selected) scheme.primary else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (selected) Color.Transparent else scheme.outlineVariant,
                shape = CircleShape
            )
            .toggleable(value = selected, enabled = enabled, role = Role.Checkbox, onValueChange = { onClick() }),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = when {
                selected -> scheme.onPrimary
                !enabled -> scheme.onSurfaceVariant.copy(alpha = 0.38f)
                else -> scheme.onSurface
            }
        )
    }
}

@Composable
private fun AppearanceRow(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.RadioButton(selected = selected, onClick = null)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = Spacing.Standard)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StripRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = Spacing.Standard)
        ) {
            Text(text = "A\u2013Z strip", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "Side index to jump to a letter",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CountPicker(count: Int?, onCountChange: (Int?) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.Compact),
        verticalArrangement = Arrangement.spacedBy(Spacing.Compact)
    ) {
        Pill(text = "All", selected = count == null, onClick = { onCountChange(null) })
        listOf(10, 20, 50, 100).forEach { n ->
            Pill(text = "$n", selected = count == n, onClick = { onCountChange(n) })
        }
    }
    var text by remember(count) { mutableStateOf(count?.toString() ?: "") }
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            val digits = raw.filter { it.isDigit() }.take(5)
            text = digits
            onCountChange(digits.toIntOrNull()?.takeIf { it > 0 })
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Custom number (empty = all)") },
        singleLine = true,
        shape = MaterialTheme.shapes.small,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

/**
 * A labelled dropdown field: the small title on top, the chosen option below. The first option
 * (key null) is the "no filter" choice; when another option is chosen the value turns accent.
 */
@Composable
private fun <T> DropdownSelector(
    title: String,
    selected: T,
    options: List<Pair<T, String>>,
    onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selected }?.second ?: options.first().second
    val isFiltering = selected != options.first().first

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isFiltering) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = AppIcons.ChevronDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    trailingIcon = {
                        if (value == selected) {
                            Icon(
                                imageVector = AppIcons.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    onClick = {
                        onSelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}
