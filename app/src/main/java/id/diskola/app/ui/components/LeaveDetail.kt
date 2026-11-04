package id.diskola.app.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.DiskolaTheme
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.ui.theme.extendedColors
import id.diskola.app.utils.AppDialogTone

/** The approval state of a request, where the module has one (school izin does, Magang izin does not). */
@Immutable
data class LeaveStatusUi(val label: String, val tone: BadgeTone)

/** One labelled line of the detail card. */
@Immutable
data class LeaveFact(val label: String, val value: String)

/** The proof file: [url] is absolute, [isImage] decides between showing it in place and a button that opens it elsewhere. */
@Immutable
data class LeaveProofUi(val url: String, val fileName: String, val isImage: Boolean)

/** Everything a leave-request detail screen shows, whichever module it came from. */
@Immutable
data class LeaveDetailUi(
    /** `sakit` or `izin`: picks the icon and its colour. */
    val type: String,
    val typeLabel: String,
    val dateLabel: String,
    val status: LeaveStatusUi? = null,
    val facts: List<LeaveFact> = emptyList(),
    /** Shown in an error-toned card; null when the request was not rejected. */
    val rejectionNote: String? = null,
    val proof: LeaveProofUi? = null,
    /** A closing line such as "Direview 05 Nov 2026". */
    val footnote: String? = null,
)

/** Sick leave is the red document, izin the green one (the same pair in every list and detail of leave requests). */
@Composable
fun LeaveTypeIcon(type: String, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    val scheme = MaterialTheme.colorScheme
    val extended = MaterialTheme.extendedColors
    val sick = type.equals("sakit", ignoreCase = true)
    Box(
        modifier = modifier
            .size(size)
            .background(if (sick) scheme.errorContainer else extended.successContainer, DiskolaExtraShapes.iconBox),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (sick) Icons.Rounded.MedicalServices else Icons.Rounded.Description,
            contentDescription = null,
            tint = if (sick) scheme.onErrorContainer else extended.onSuccessContainer,
            modifier = Modifier.size(size / 2),
        )
    }
}

/**
 * "Detail Pengajuan", the same for every kind of izin: a summary card (type icon, type, date, status), one card with the labelled facts, the
 * rejection reason when there is one, the proof file and a closing line. Scrolls; draw it inside the scaffold's padding.
 * A picture proof is drawn in place; any other proof (a PDF) is a button that calls [onOpenFile], which brings it into the in-app viewer.
 */
@Composable
fun LeaveDetailContent(
    ui: LeaveDetailUi,
    onOpenFile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var preview by remember { mutableStateOf<String?>(null) }
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        AppCard(variant = CardVariant.Elevated) {
            // Read as one sentence ("Izin, 04 Nov 2026, Menunggu") instead of three stops.
            Row(
                modifier = Modifier.semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                LeaveTypeIcon(ui.type, size = 56.dp)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        ui.typeLabel,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = scheme.onSurface,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Text(ui.dateLabel, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                    }
                    ui.status?.let { StatusBadge(label = it.label, tone = it.tone, modifier = Modifier.padding(top = Spacing.xs)) }
                }
            }
        }

        if (ui.facts.isNotEmpty()) {
            FormCard {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    ui.facts.forEachIndexed { index, fact ->
                        if (index > 0) HorizontalDivider(color = scheme.outlineVariant)
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            Text(fact.label, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                            Text(fact.value, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
                        }
                    }
                }
            }
        }

        ui.rejectionNote?.let { reason ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(scheme.errorContainer, DiskolaExtraShapes.card)
                    .padding(Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = scheme.onErrorContainer)
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        "Alasan Penolakan",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onErrorContainer,
                    )
                    Text(reason, style = MaterialTheme.typography.bodyMedium, color = scheme.onErrorContainer)
                }
            }
        }

        ui.proof?.let { proof ->
            FormCard {
                FieldLabel("File Bukti", required = false)
                if (proof.isImage) {
                    // The picture is shown in place; tap for the full-screen preview.
                    ProofImage(model = proof.url, onClick = { preview = proof.url })
                    Text(proof.fileName, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                } else {
                    // A PDF cannot be drawn in place: it opens in the app's own viewer.
                    AppButton(
                        text = proof.fileName,
                        onClick = onOpenFile,
                        variant = ButtonVariant.Outlined,
                        leadingIcon = { Icon(fileIcon(proof.fileName), contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        ui.footnote?.let { note ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.EventAvailable, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Text(note, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            }
        }
    }
    preview?.let { ImagePreviewDialog(model = it, onDismiss = { preview = null }) }
}

/**
 * What bringing a proof file into the app shows: a blocking "membuka file" while it downloads, and a centred error dialog when it could not
 * be opened (the app does not fall back to another app).
 */
@Composable
fun ProofFileStatus(opening: Boolean, error: String?, onDismissError: () -> Unit) {
    if (opening) AppLoadingDialog(message = "membuka file")
    error?.let {
        AppDialog(
            onDismiss = onDismissError,
            title = "Tidak Bisa Membuka File",
            body = it,
            primaryButtonText = "Tutup",
            onPrimaryClick = onDismissError,
            tone = AppDialogTone.ERROR,
        )
    }
}

// ---- Previews ----
// A rejected school izin with a long note and a picture, a pending one with a PDF, and a Magang one with a company and no status. The
// pictures load from the network, so a preview shows their spinner; the frame and the text are what is judged here.

private val rejected = LeaveDetailUi(
    type = "sakit",
    typeLabel = "Sakit",
    dateLabel = "04 Nov 2026",
    status = LeaveStatusUi("Ditolak", BadgeTone.Error),
    facts = listOf(
        LeaveFact(
            "Keterangan",
            "Demam tinggi sejak semalam dan dokter menyarankan istirahat penuh selama dua hari, surat keterangan dokter terlampir pada pengajuan ini.",
        ),
    ),
    rejectionNote = "Surat keterangan dokter tidak terbaca. Mohon unggah ulang foto yang lebih jelas.",
    proof = LeaveProofUi("https://dev.assets.diskola.id/x/surat.png", "Y9qK977esnWfbRvgp5Fhjf0xA92XpCEaL1Pcyn18.png", isImage = true),
    footnote = "Direview 05 Nov 2026",
)

private val pendingPdf = LeaveDetailUi(
    type = "izin",
    typeLabel = "Izin",
    dateLabel = "04 Nov 2026",
    status = LeaveStatusUi("Menunggu", BadgeTone.Warning),
    facts = listOf(LeaveFact("Keterangan", "Acara keluarga di luar kota.")),
    proof = LeaveProofUi("https://dev.assets.diskola.id/x/surat.pdf", "surat-izin-orang-tua.pdf", isImage = false),
)

private val magang = LeaveDetailUi(
    type = "sakit",
    typeLabel = "Sakit",
    dateLabel = "04 Jun 2026",
    facts = listOf(
        LeaveFact("Perusahaan", "PT. Teknologi Pendidikan Nusantara Berkembang Bersama"),
        LeaveFact("Keterangan", "Flu dan demam ringan."),
    ),
    proof = LeaveProofUi("https://dev.assets.diskola.id/x/bukti.jpg", "bukti.jpg", isImage = true),
)

@Composable
private fun DetailPreview(ui: LeaveDetailUi) {
    DiskolaTheme(darkTheme = isSystemInDarkTheme()) {
        Surface(modifier = Modifier.fillMaxSize()) { LeaveDetailContent(ui, onOpenFile = {}) }
    }
}

@Preview(name = "Izin detail, rejected", showBackground = true, widthDp = 360, heightDp = 800)
@Preview(name = "Izin detail, rejected, dark", showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Izin detail, rejected, large font", showBackground = true, widthDp = 360, heightDp = 1200, fontScale = 1.5f)
@Composable
private fun RejectedPreview() = DetailPreview(rejected)

@Preview(name = "Izin detail, pending with PDF", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun PendingPdfPreview() = DetailPreview(pendingPdf)

@Preview(name = "Izin detail, Magang", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun MagangPreview() = DetailPreview(magang)
