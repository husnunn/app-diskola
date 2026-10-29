package id.diskola.app.ui.screens.materi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Description
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.diskola.app.dataclass.ResponData.MateriTable
import id.diskola.app.ui.screens.tugas.TextCard
import id.diskola.app.ui.screens.tugas.TugasFileCard
import id.diskola.app.ui.theme.DiskolaTheme
import id.diskola.app.ui.theme.Spacing

// The header and sections of "Detail Materi" with the awkward content they must survive: a long title, subject and teacher name,
// and a long file name, at the default and a large font scale. The app is light-only, so there is no dark variant.

private val longMateri = MateriTable(
    id = 1,
    name = "Pengantar Basis Data Relasional: Konsep Entitas, Atribut, dan Normalisasi Bentuk Pertama Hingga Ketiga",
    description = "Materi ini membahas dasar-dasar basis data relasional. Pelajari cara mengenali entitas dan atribut dari sebuah kasus nyata, " +
        "menggambar diagram hubungan antar-entitas, lalu menormalkan tabel sampai bentuk normal ketiga.\n\nBaca file materi sebelum pertemuan berikutnya.",
    file_name = "Modul Basis Data Relasional - Bab 3 Normalisasi Lengkap dengan Latihan Soal.pdf",
    file_path = "materi/modul-basis-data.pdf",
    file_size = "2457600",
    created_at_label = "Senin, 26 Oktober 2026",
    teacher_name = "Dedi Kurniawan Pratama, S.Kom., M.Pd.",
    subject_name = "Dasar-Dasar Program Keahlian Pengembangan Perangkat Lunak dan Gim",
    class_name = "XI PPLG 2",
)

@Composable
private fun PreviewColumn(content: @Composable () -> Unit) {
    DiskolaTheme {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(Spacing.xl)) { content() }
    }
}

@Preview(name = "Detail Materi, long content", showBackground = true, widthDp = 360, heightDp = 780)
@Preview(name = "Detail Materi, large font", showBackground = true, widthDp = 360, heightDp = 1400, fontScale = 1.5f)
@Composable
private fun MateriDetailPreview() {
    PreviewColumn {
        MateriHeaderCard(longMateri)
        MateriSection("Deskripsi", Icons.Rounded.Description) { TextCard(longMateri.description) }
        MateriSection("File Materi", Icons.Rounded.AttachFile) {
            TugasFileCard(
                fileName = longMateri.file_name,
                fileSize = "2.34 Mb",
                loading = false,
                readLabel = "Lihat",
                downloadLabel = "Unduh",
                onOpen = {},
                onDownload = {},
            )
        }
    }
}

@Preview(name = "Header, nothing but a title", showBackground = true, widthDp = 360)
@Composable
private fun MateriHeaderBarePreview() {
    PreviewColumn { MateriHeaderCard(MateriTable(id = 2, name = "Latihan Soal Minggu Ke-3")) }
}
