package com.vocabulary.app.ui.quiz

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vocabulary.app.ui.components.AppIcons
import com.vocabulary.app.ui.theme.Spacing

/** Boxed control: label, a minus button, a typeable number and a plus button. */
@Composable
internal fun StepperBox(
    label: String,
    caption: String?,
    value: Int,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.Standard, vertical = Spacing.Compact),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Micro)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                if (caption != null) {
                    Text(
                        caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(
                onClick = { onValueChange((value - 1).coerceIn(min, max)) },
                enabled = value > min,
                modifier = Modifier.size(Spacing.Target)
            ) { Icon(AppIcons.Remove, contentDescription = "Decrease $label") }
            NumberField(value, min, max, onValueChange, description = label)
            IconButton(
                onClick = { onValueChange((value + 1).coerceIn(min, max)) },
                enabled = value < max,
                modifier = Modifier.size(Spacing.Target)
            ) { Icon(AppIcons.Add, contentDescription = "Increase $label") }
        }
    }
}

/** Typing is free; the value is clamped to [min]..[max] when the field loses focus. */
@Composable
private fun NumberField(
    value: Int,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit,
    description: String
) {
    var text by remember { mutableStateOf(value.toString()) }
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Keep the text in sync when the value changes from the +/- buttons or clamping.
    LaunchedEffect(value) {
        if (text.toIntOrNull() != value) text = value.toString()
    }

    fun commit() {
        val result = text.toIntOrNull()?.coerceIn(min, max) ?: value
        text = result.toString()
        if (result != value) onValueChange(result)
    }

    BasicTextField(
        value = text,
        onValueChange = { input ->
            val digits = input.filter { it.isDigit() }.take(3)
            text = digits
            digits.toIntOrNull()?.let { if (it >= min) onValueChange(it.coerceAtMost(max)) }
        },
        modifier = Modifier
            .size(width = 72.dp, height = Spacing.Target)
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
            .onFocusChanged {
                if (focused && !it.isFocused) commit()
                focused = it.isFocused
            }
            .semantics { contentDescription = "$description, $value" },
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        ),
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        decorationBox = { inner -> Box(contentAlignment = Alignment.Center) { inner() } }
    )
}
