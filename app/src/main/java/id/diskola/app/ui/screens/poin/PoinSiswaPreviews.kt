package id.diskola.app.ui.screens.poin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import id.diskola.app.repository.PoinCall
import id.diskola.app.repository.PoinEvent
import id.diskola.app.ui.theme.DiskolaTheme
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing

// The student's event cards and calling row with a date, a long creator and message, and a date the server sent in an unexpected shape,
// at the default and a large font scale (the app is light-only).
@Preview(name = "Kartu kejadian", showBackground = true, widthDp = 360, heightDp = 780)
@Preview(name = "Kartu kejadian, font besar", showBackground = true, widthDp = 360, heightDp = 1200, fontScale = 1.5f)
@Composable
private fun PoinEventCardsPreview() {
    DiskolaTheme {
        Column(Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            PoinEventCard(
                event = PoinEvent("a", "Terlambat masuk sekolah", 5, "", "Terlambat 15 menit", "2026-11-05", "07:15:00", "Budi Santoso, S.Pd., M.Pd."),
                onOpenImage = {},
            )
            PoinEventCard(
                event = PoinEvent("b", "Juara 1 Lomba Karya Ilmiah Remaja Tingkat Provinsi", 20, "foto.jpg", "Mewakili sekolah dan membawa pulang piala bergilir untuk pertama kalinya", "2026-10-30", "", "Guru DEV"),
                onOpenImage = {},
            )
            PoinEventCard(
                event = PoinEvent("c", "Prestasi", 2, "", "", "kemarin", "10:07", ""),
                onOpenImage = {},
            )
            CallRow(PoinCall(1, "Panggilan 1", "2026-11-06 10:10:00", "Mohon hadir ke ruang BK bersama orang tua"))
        }
    }
}
