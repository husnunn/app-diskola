package id.diskola.app.ui.screens.akm

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import id.diskola.app.utils.AkmRules
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import id.diskola.app.dataclass.akm.AkmMedia
import id.diskola.app.dataclass.akm.AkmPairSlot
import id.diskola.app.dataclass.akm.AkmPlayerAnswer
import id.diskola.app.dataclass.akm.QuestionType
import id.diskola.app.ui.components.AkmWatermarkOverlay
import id.diskola.app.ui.components.AnswerOption
import id.diskola.app.ui.components.AppBottomSheet
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.AppTextField
import id.diskola.app.ui.components.ButtonVariant
import id.diskola.app.ui.components.EmptyState
import id.diskola.app.ui.components.LoadingState
import id.diskola.app.ui.components.ExamModeHeader
import id.diskola.app.ui.components.MathHtmlText
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.ui.theme.extendedColors
import id.diskola.app.utils.AkmPenaltyRules
import id.diskola.app.utils.AkmQuestionRules
import id.diskola.app.utils.NextButtonMode
import id.diskola.app.viewmodel.AkmQuestionViewModel
import kotlinx.coroutines.delay

private val OPTION_LETTERS = listOf("A", "B", "C", "D", "E", "F")

/** Coil's default String->Uri mapping doesn't reliably resolve a bare absolute filesystem path
 * (no `file://` scheme) — `AkmDownloadWorker` stores local cache paths that way, so wrap them in a
 * [java.io.File] (a model type Coil always handles) instead of passing the raw string through. */
private fun imageModel(pathOrLocalFile: String): Any =
    if (pathOrLocalFile.startsWith("/")) java.io.File(pathOrLocalFile) else pathOrLocalFile

/** The question player for the instruction in the route: every tap is written to Room at once (essay text after a short pause). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AkmQuestionScreen(
    examId: Int,
    onBack: () -> Unit,
    onTimeUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AkmQuestionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val index by viewModel.index.collectAsStateWithLifecycle()
    val answers by viewModel.answers.collectAsStateWithLifecycle()
    val timeUp by viewModel.timeUp.collectAsStateWithLifecycle()
    val watermark by viewModel.watermark.collectAsStateWithLifecycle()
    var gridVisible by rememberSaveable { mutableStateOf(false) }

    val questions = state.questions
    val question = questions.getOrNull(index)
    val answeredCount = questions.count { answers.containsKey(it.id) }

    // An essay still waiting for its 200 ms pause is written when the app goes to the background or this screen is left.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_PAUSE) viewModel.flush() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    // While the session runs the hardware back key does nothing (legacy `shouldDisableBackButton`); the arrow in the header returns to the lobby.
    val session = LocalExamSession.current
    val sessionState by session.state.collectAsStateWithLifecycle()
    val sessionRunning = sessionState.active && sessionState.examId == examId && !state.locked
    EndSessionWhenHandedIn(examId = examId, handedIn = state.locked)
    val penalty = sessionState.penalty?.takeIf { sessionRunning }
    BackHandler {
        if (!sessionRunning) {
            viewModel.flush()
            onBack()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        ExamModeHeader(
            title = when {
                penalty != null -> AkmPenaltyRules.QUESTION_TITLE
                questions.isEmpty() -> "Soal"
                else -> "Soal ${index + 1} dari ${questions.size}"
            },
            subtitle = "",
            penaltyLabel = penalty?.let { AkmPenaltyRules.pillLabel(it.remainingMillis) },
            onBack = {
                viewModel.flush()
                onBack()
            },
            trailing = {
                if (questions.isNotEmpty() && penalty == null) {
                    Row(
                        modifier = Modifier
                            .clickable { gridVisible = true }
                            .padding(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.GridView,
                            contentDescription = "Pilih soal",
                            tint = MaterialTheme.extendedColors.onExamSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            },
        )

        if (question == null) {
            if (!state.loaded) {
                LoadingState(modifier = Modifier.weight(1f))
            } else {
                EmptyState(
                    title = "Soal tidak ditemukan",
                    description = "Instruksi ini belum memiliki soal, atau belum diunduh ke perangkat.",
                    icon = Icons.Rounded.ReportProblem,
                    modifier = Modifier.weight(1f),
                )
            }
            return
        }

        if (penalty != null) {
            // Legacy hides the question and "Berikutnya" while the penalty runs; the dialog above says why.
            Spacer(modifier = Modifier.weight(1f))
        } else {
            val answer = answers[question.id]
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                AkmChip(question.type.label)
                if (state.locked) {
                    InfoBanner(icon = Icons.Rounded.ReportProblem, text = "Jawaban sudah dikumpulkan dan tidak dapat diubah lagi.")
                }

                // Legacy layout order confirmed from `akm_question_multiple_item.xml`: question image
                // (hidden entirely when blank, not a placeholder), then question text, then media.
                if (question.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = imageModel(question.imageUrl),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow, DiskolaExtraShapes.card),
                    )
                }

                MathHtmlText(
                    question.text,
                    dataValueFormulas = true,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                if (question.media.isNotEmpty()) {
                    QuestionMediaList(media = question.media)
                }

                when (question.type) {
                    QuestionType.PilihanGanda -> {
                        val selected = (answer as? AkmPlayerAnswer.Choice)?.answerId
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            question.options.forEachIndexed { optionIndex, option ->
                                AnswerOption(
                                    label = option.text,
                                    selected = selected == option.id,
                                    multiSelect = false,
                                    letter = OPTION_LETTERS.getOrNull(optionIndex),
                                    onClick = { viewModel.selectChoice(question, option.id) },
                                )
                            }
                        }
                    }

                    // Both options share the single statement row's id, so the pick is "Benar" (first) or "Salah" (second).
                    QuestionType.BenarSalah -> {
                        val picked = (answer as? AkmPlayerAnswer.TrueFalse)?.isTrue
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            question.options.forEachIndexed { optionIndex, option ->
                                val isBenar = optionIndex == 0
                                AnswerOption(
                                    label = option.text,
                                    selected = picked == isBenar,
                                    multiSelect = false,
                                    onClick = { viewModel.selectTrueFalse(question, isBenar) },
                                )
                            }
                        }
                    }

                    QuestionType.MultiJawaban -> {
                        val selected = (answer as? AkmPlayerAnswer.MultiChoice)?.answerIds.orEmpty()
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            question.options.forEach { option ->
                                AnswerOption(
                                    label = option.text,
                                    selected = option.id in selected,
                                    multiSelect = true,
                                    onClick = { viewModel.toggleMultiChoice(question, option.id) },
                                )
                            }
                        }
                    }

                    QuestionType.EsaiSingkat -> {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            AppTextField(
                                value = (answer as? AkmPlayerAnswer.Essay)?.text.orEmpty(),
                                onValueChange = { viewModel.setEssay(question, it) },
                                // 8 lines at least; "Selesai" instead of Enter (an answer never holds a line break); the caret line stays in view.
                                minLines = AkmRules.ESSAY_MIN_LINES,
                                imeAction = if (AkmRules.usesDoneAction(question.rawAnswerType)) ImeAction.Done else ImeAction.Default,
                                followCursor = true,
                                label = "Jawaban",
                                placeholder = when (question.rawAnswerType) {
                                    "SHORT_ESSAY_WORD" -> "Satu kata, tanpa spasi"
                                    "SHORT_ESSAY_NUM" -> "Jawaban berupa angka"
                                    else -> "Ketikkan jawabanmu . . ."
                                },
                                keyboardType = if (question.rawAnswerType == "SHORT_ESSAY_NUM") KeyboardType.Number else KeyboardType.Text,
                            )
                            if (question.rawAnswerType == "SHORT_ESSAY_WORD") {
                                Text(
                                    "Spasi otomatis dihapus — jawaban ini tidak boleh mengandung spasi.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    QuestionType.TabelPernyataan -> {
                        val rows = (answer as? AkmPlayerAnswer.TableAnswer)?.rows.orEmpty()
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            question.options.forEach { row ->
                                StatementRow(
                                    imageUrl = row.imageUrl,
                                    text = row.text,
                                    value = rows[row.id],
                                    onSelect = { isTrue -> viewModel.selectTableRow(question, row.id, isTrue) },
                                )
                            }
                        }
                    }

                    QuestionType.Menjodohkan -> {
                        val slots = viewModel.pairSlotsFor(question)
                        MenjodohkanPairs(
                            slots = slots,
                            isImage = question.pairIsImage,
                            scrollState = scrollState,
                            onSwap = { a, b -> viewModel.swapPair(question, a, b) },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                AppButton(
                    text = "Sebelumnya",
                    onClick = { if (session.checkpoint("snap_soal")) viewModel.previousQuestion() },
                    variant = ButtonVariant.Outlined,
                    enabled = index > 0,
                    modifier = Modifier.weight(1f),
                )
                // The right-hand button is always in the layout, so its place never changes; hidden, it is only invisible and dead.
                val nextMode = AkmQuestionRules.nextButtonMode(
                    position = index,
                    questionCount = questions.size,
                    penaltyActive = penalty != null,
                    isExplanation = false,
                    allowFinish = AkmRules.allAnswered(questions.map { it.id }, answers.keys),
                    // Whether the pin was lost is only known to a checkpoint (never run while composing); a lost pin shows the recovery dialog.
                    hasBreach = sessionState.recovery != null,
                )
                AppButton(
                    text = if (nextMode == NextButtonMode.FINISH) "Selesai" else "Berikutnya",
                    onClick = {
                        if (nextMode == NextButtonMode.FINISH) {
                            // Same as the back arrow: back to the lobby, where "Kumpulkan" is (this does not hand in anything).
                            if (session.checkpoint("menu_kumpulkan")) {
                                viewModel.flush()
                                onBack()
                            }
                        } else if (session.checkpoint("next_soal")) {
                            viewModel.nextQuestion()
                        }
                    },
                    enabled = nextMode != NextButtonMode.HIDDEN,
                    modifier = Modifier
                        .weight(1f)
                        .then(if (nextMode == NextButtonMode.HIDDEN) Modifier.alpha(0f).clearAndSetSemantics { } else Modifier),
                )
            }
        }

        if (gridVisible) {
            AppBottomSheet(title = "Pilih Soal", onDismiss = { gridVisible = false }) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Text(
                        "$answeredCount dari ${questions.size} soal terjawab",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    // The grid's natural height grows with the number of questions; cap it so the sheet (and its legend)
                    // never outgrows the screen — the grid itself scrolls beyond the cap.
                    val gridMaxHeight = (LocalConfiguration.current.screenHeightDp * 0.5f).dp
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                        modifier = Modifier.height(minOf(((questions.size + 4) / 5 * 60).dp, gridMaxHeight)),
                    ) {
                        itemsIndexed(questions) { questionIndex, item ->
                            QuestionGridCell(
                                number = questionIndex + 1,
                                current = questionIndex == index,
                                answered = answers.containsKey(item.id),
                                onClick = {
                                    if (session.checkpoint("pilih_soal")) viewModel.goToQuestion(questionIndex)
                                    gridVisible = false
                                },
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        GridLegend("Soal aktif", MaterialTheme.colorScheme.primary)
                        GridLegend("Sudah dijawab", MaterialTheme.colorScheme.secondaryContainer)
                        GridLegend("Belum dijawab", MaterialTheme.colorScheme.surfaceContainerLowest)
                    }
                }
            }
        }
    }

    // The "Dibatalkan" mark as a diagonal watermark: a plain overlay, so touches reach the question underneath (doc 06 §4.4).
    if (watermark.enabled) AkmWatermarkOverlay(watermark.label)
    }

    if (timeUp) {
        AppDialog(
            onDismiss = {},
            dismissible = false,
            title = "Perhatian",
            body = "Waktu ujian telah berakhir, jawaban akan dikumpulkan",
            primaryButtonText = "OK",
            onPrimaryClick = onTimeUp,
        )
    }
}

@Composable
private fun QuestionGridCell(number: Int, current: Boolean, answered: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val background = when {
        current -> scheme.primary
        answered -> scheme.secondaryContainer
        else -> scheme.surfaceContainerLowest
    }
    val foreground = when {
        current -> scheme.onPrimary
        answered -> scheme.onSecondaryContainer
        else -> scheme.onSurface
    }
    Box(
        modifier = Modifier
            .heightIn(min = 52.dp)
            .background(background, DiskolaExtraShapes.card)
            .border(1.dp, if (current) scheme.primary else scheme.outlineVariant, DiskolaExtraShapes.card)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            number.toString(),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
            color = foreground,
        )
    }
}

/**
 * Legacy `AkmQuestionMediaBinder` inflates one row per item (audio → `MediaPlayer`, video →
 * `VideoView`/YouTube WebView) with an in-app player. That player machinery is out of scope here —
 * this instead opens each media URL externally via [Intent.ACTION_VIEW], which is an honest
 * affordance rather than a fake unbuilt player.
 */
@Composable
private fun QuestionMediaList(media: List<AkmMedia>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        media.forEach { item ->
            val isAudio = item.type.equals("audio", ignoreCase = true)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url))) }
                    }
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, DiskolaExtraShapes.card)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, DiskolaExtraShapes.card)
                    .padding(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Icon(
                    if (isAudio) Icons.Rounded.Audiotrack else Icons.Rounded.Videocam,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (isAudio) "Audio soal" else "Video soal",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = "Buka di aplikasi lain",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * One Tabel Pernyataan row: a statement + independent Benar/Salah toggle. [value] is `null` until
 * the student picks either side — deliberately no correctness (hijau/merah) styling here, matching
 * `akm_answer_statement_table.xml` (docs §3.4: legacy never colors this row by `isTrue` while the
 * student is still working the exam).
 */
@Composable
private fun StatementRow(imageUrl: String, text: String, value: Boolean?, onSelect: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageModel(imageUrl),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(scheme.surfaceContainerLow, DiskolaExtraShapes.card),
            )
        }
        MathHtmlText(text, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            StatementToggle(label = "Benar", selected = value == true, onClick = { onSelect(true) }, modifier = Modifier.weight(1f))
            StatementToggle(label = "Salah", selected = value == false, onClick = { onSelect(false) }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatementToggle(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .background(if (selected) scheme.primary else scheme.surfaceContainerLow, DiskolaExtraShapes.card)
            .border(1.5.dp, if (selected) scheme.primary else scheme.outlineVariant, DiskolaExtraShapes.card)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
        )
    }
}

/** Distance from the top/bottom of the screen that triggers auto-scroll while dragging a
 * Menjodohkan card — mirrors the edge-triggered auto-scroll in `QuestionPairVh.kt:66-83`. */
private val DRAG_AUTOSCROLL_EDGE = 64.dp
private const val DRAG_AUTOSCROLL_SPEED = 16f

/**
 * Renders one Menjodohkan question's pairs and owns the drag-and-drop state — a real long-press
 * drag ported from `QuestionPairVh.kt:60-143`/`akm_answer_pair_item.xml:78-113`: only the right
 * ("drag_area"-equivalent) card is draggable, the left anchor never moves, and dropping onto
 * another row swaps their right-side content via [onSwap]. `detectDragGesturesAfterLongPress`
 * (not a plain drag) means an ordinary swipe still scrolls the page normally — only a sustained
 * long-press starts capturing the gesture, same as the legacy `setOnLongClickListener` + `startDrag`.
 */
@Composable
private fun MenjodohkanPairs(
    slots: List<AkmPairSlot>,
    isImage: Boolean,
    scrollState: ScrollState,
    onSwap: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val view = LocalView.current
    val edgePx = with(density) { DRAG_AUTOSCROLL_EDGE.toPx() }

    var containerTop by remember { mutableFloatStateOf(0f) }
    val rowBounds = remember { mutableStateMapOf<Int, ClosedFloatingPointRange<Float>>() }
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var targetIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var autoScrollDirection by remember { mutableIntStateOf(0) }

    LaunchedEffect(autoScrollDirection, draggedIndex) {
        if (draggedIndex == -1 || autoScrollDirection == 0) return@LaunchedEffect
        while (true) {
            scrollState.scrollBy(autoScrollDirection * DRAG_AUTOSCROLL_SPEED)
            delay(16)
        }
    }

    fun resetDrag() {
        draggedIndex = -1
        targetIndex = -1
        dragOffsetY = 0f
        autoScrollDirection = 0
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { containerTop = it.positionInRoot().y },
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        slots.forEachIndexed { position, slot ->
            val isDragging = draggedIndex == position
            PairRow(
                slot = slot,
                isImage = isImage,
                isDragging = isDragging,
                dragOffsetY = if (isDragging) dragOffsetY else 0f,
                rowModifier = Modifier.onGloballyPositioned {
                    val top = it.positionInParent().y
                    rowBounds[position] = top..(top + it.size.height)
                },
                dragHandleModifier = Modifier.pointerInput(slots.size) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            draggedIndex = position
                            targetIndex = position
                            dragOffsetY = 0f
                        },
                        onDragEnd = {
                            if (targetIndex != -1 && targetIndex != draggedIndex) onSwap(draggedIndex, targetIndex)
                            resetDrag()
                        },
                        onDragCancel = { resetDrag() },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetY += dragAmount.y
                            val origin = rowBounds[position]
                            if (origin != null) {
                                val currentCenter = (origin.start + origin.endInclusive) / 2f + dragOffsetY
                                targetIndex = rowBounds.entries.firstOrNull { currentCenter in it.value }?.key ?: targetIndex
                                val absoluteY = containerTop + origin.start + dragOffsetY
                                autoScrollDirection = when {
                                    absoluteY < edgePx -> -1
                                    absoluteY > view.height - edgePx -> 1
                                    else -> 0
                                }
                            }
                        },
                    )
                },
            )
        }
    }
}

/**
 * One Menjodohkan row: a fixed left anchor ([AkmPairSlot.firstText]/[AkmPairSlot.firstImageUrl])
 * and a draggable right side ([AkmPairSlot.currentSecondText]/[AkmPairSlot.currentSecondImageUrl]).
 * [rowModifier] tracks this row's bounds for drop-target detection; [dragHandleModifier] carries
 * the actual long-press-drag gesture detector — both applied by [MenjodohkanPairs].
 */
@Composable
private fun PairRow(
    slot: AkmPairSlot,
    isImage: Boolean,
    isDragging: Boolean,
    dragOffsetY: Float,
    rowModifier: Modifier,
    dragHandleModifier: Modifier,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth().then(rowModifier),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp)
                .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
                .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
                .padding(Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(slot.label, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black), color = scheme.onSurfaceVariant)
            if (isImage && slot.firstImageUrl.isNotBlank()) {
                AsyncImage(
                    model = imageModel(slot.firstImageUrl),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.weight(1f).aspectRatio(1f).background(scheme.surfaceContainerLow, DiskolaExtraShapes.card),
                )
            } else {
                MathHtmlText(slot.firstText, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface, modifier = Modifier.weight(1f))
            }
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp)
                .zIndex(if (isDragging) 1f else 0f)
                .graphicsLayer { translationY = dragOffsetY }
                .then(if (isDragging) Modifier.shadow(6.dp, DiskolaExtraShapes.card) else Modifier)
                .background(if (isDragging) scheme.surfaceContainerLow else scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
                .border(1.5.dp, if (isDragging) scheme.primary else scheme.outlineVariant, DiskolaExtraShapes.card)
                .then(dragHandleModifier)
                .padding(Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isImage && slot.currentSecondImageUrl.isNotBlank()) {
                AsyncImage(
                    model = imageModel(slot.currentSecondImageUrl),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.weight(1f).aspectRatio(1f).background(scheme.surfaceContainerLow, DiskolaExtraShapes.card),
                )
            } else {
                MathHtmlText(slot.currentSecondText, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GridLegend(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(color, RoundedCornerShape(4.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp)),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Spacing.xs),
        )
    }
}
