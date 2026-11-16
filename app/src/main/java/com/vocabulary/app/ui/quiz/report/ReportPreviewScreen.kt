package com.vocabulary.app.ui.quiz.report

import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxHeight
import com.vocabulary.app.ui.components.AppIcons
import com.vocabulary.app.ui.components.ExamHeaderBar
import com.vocabulary.app.ui.components.ExamIconButton
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vocabulary.app.ui.quiz.QuizSummary
import com.vocabulary.app.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportPreviewScreen(
    summary: QuizSummary,
    options: ReportOptions,
    onOptionsChange: (ReportOptions) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val layouter = remember { PaperLayout(context.applicationContext) }
    var showSheet by rememberSaveable { mutableStateOf(false) }

    // Re-layout after a short pause so typing in a text field does not rebuild every key.
    val pages by produceState<List<PaperPage>>(emptyList(), summary, options) {
        delay(150)
        value = withContext(Dispatchers.Default) { layouter.build(summary, options) }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val doc = PdfDocument()
                    layouter.build(summary, options).forEachIndexed { i, page ->
                        val info = PdfDocument.PageInfo.Builder(PAGE_W.toInt(), PAGE_H.toInt(), i + 1).create()
                        val p = doc.startPage(info)
                        page.draw(p.canvas)
                        doc.finishPage(p)
                    }
                    context.contentResolver.openOutputStream(uri)?.use { doc.writeTo(it) }
                        ?: error("No output stream")
                    doc.close()
                }.isSuccess
            }
            Toast.makeText(
                context,
                if (ok) "PDF saved" else "Could not save the PDF",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Toolbar: "Preview" and the page count on the left; back, settings and download on the right.
        ExamHeaderBar {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Preview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = if (pages.size == 1) "1 page" else "${pages.size} pages",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ExamIconButton(
                icon = AppIcons.ArrowBack,
                contentDescription = "Back to results",
                onClick = onBack
            )
            ExamIconButton(
                icon = AppIcons.Tune,
                contentDescription = "Settings",
                onClick = { showSheet = true }
            )
            ExamIconButton(
                icon = AppIcons.Download,
                contentDescription = "Download PDF",
                filled = true,
                enabled = pages.isNotEmpty(),
                onClick = {
                    val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                    saveLauncher.launch("Vocabox_Quiz_$stamp.pdf")
                }
            )
        }

        // The sheets sit on the plain page background with a clear gap between them.
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            itemsIndexed(pages) { _, page -> PagePreview(page) }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            CustomizeSheet(options, summary, onOptionsChange)
        }
    }
}

@Composable
private fun PagePreview(page: PaperPage) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(2.dp), ambientColor = Color(0x1A111827), spotColor = Color(0x1A111827))
            .aspectRatio(PAGE_W / PAGE_H)
            .background(Color.White)
            .drawBehind {
                val s = size.width / PAGE_W
                drawIntoCanvas { c ->
                    val canvas = c.nativeCanvas
                    canvas.save()
                    canvas.scale(s, s)
                    page.draw(canvas)
                    canvas.restore()
                }
            }
    )
}

// ---- Customize sheet --------------------------------------------------------------------

@Composable
private fun CustomizeSheet(
    options: ReportOptions,
    summary: QuizSummary,
    onChange: (ReportOptions) -> Unit
) {
    var title by rememberSaveable { mutableStateOf(options.title) }
    var group by rememberSaveable { mutableStateOf(options.groupText) }
    var penalty by rememberSaveable { mutableStateOf(formatMarks(options.negativeMark.toDouble())) }
    val statsOnly = options.content == ReportContent.STATS_ONLY
    val paper = !statsOnly
    val answersShown = paper && options.paperType != PaperType.QUESTIONS

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.Content)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.titleLarge)

        SheetLabel("Content")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = statsOnly,
                onClick = { onChange(options.copy(content = ReportContent.STATS_ONLY)) },
                label = { Text("Result statistics only") }
            )
            FilterChip(
                selected = paper,
                onClick = { onChange(options.copy(content = ReportContent.PAPER)) },
                label = { Text("MCQ paper") }
            )
        }

        if (paper) {
            SheetLabel("Paper")
            RadioRow("Questions only", options.paperType == PaperType.QUESTIONS) {
                onChange(options.copy(paperType = PaperType.QUESTIONS))
            }
            RadioRow("Questions + answers", options.paperType == PaperType.WITH_ANSWERS) {
                onChange(options.copy(paperType = PaperType.WITH_ANSWERS))
            }
            RadioRow("Questions + answers + detailed explanation", options.paperType == PaperType.WITH_EXPLANATION) {
                onChange(options.copy(paperType = PaperType.WITH_EXPLANATION))
            }
            SwitchRow("Show my answers", options.showMyAnswers, enabled = answersShown) {
                onChange(options.copy(showMyAnswers = it))
            }

            SheetLabel("Questions")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = options.scope == PaperScope.ALL,
                    onClick = { onChange(options.copy(scope = PaperScope.ALL)) },
                    label = { Text("All (${summary.total})") }
                )
                FilterChip(
                    selected = options.scope == PaperScope.WRONG_ONLY,
                    onClick = { onChange(options.copy(scope = PaperScope.WRONG_ONLY)) },
                    label = { Text("Wrong only (${summary.wrong.size})") }
                )
            }

            SheetLabel("Columns")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = options.columns == 2,
                    onClick = { onChange(options.copy(columns = 2)) },
                    label = { Text("Two columns") }
                )
                FilterChip(
                    selected = options.columns == 1,
                    onClick = { onChange(options.copy(columns = 1)) },
                    label = { Text("One column") }
                )
            }

            SheetLabel("Text size")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReportTextSize.entries.forEach { size ->
                    FilterChip(
                        selected = options.textSize == size,
                        onClick = { onChange(options.copy(textSize = size)) },
                        label = { Text(size.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
        }

        HorizontalDivider()
        SheetLabel("Heading")
        OutlinedTextField(
            value = title,
            onValueChange = { title = it; onChange(options.copy(title = it)) },
            label = { Text("Exam / category name") },
            placeholder = { Text(defaultReportTitle(summary)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = group,
            onValueChange = { group = it; onChange(options.copy(groupText = it)) },
            label = { Text("Group no. / other note") },
            placeholder = { Text(summary.groupLabel ?: "e.g. Group 5") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        SwitchRow("Logo", options.showLogo) { onChange(options.copy(showLogo = it)) }
        SwitchRow("Date", options.showDate) { onChange(options.copy(showDate = it)) }
        SwitchRow("Statistics table", options.showStats || statsOnly, enabled = paper) {
            onChange(options.copy(showStats = it))
        }

        HorizontalDivider()
        SheetLabel("Marking")
        OutlinedTextField(
            value = penalty,
            onValueChange = { v ->
                if (v.matches(Regex("""\d{0,2}(\.\d{0,2})?"""))) {
                    penalty = v
                    onChange(options.copy(negativeMark = (v.toFloatOrNull() ?: 0f).coerceAtMost(10f)))
                }
            },
            label = { Text("Negative mark per wrong answer") },
            supportingText = { Text("1 mark per correct answer. Unanswered questions are not penalised.") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(end = 12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 10.dp))
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}
