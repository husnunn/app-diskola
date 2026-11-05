package id.diskola.app.ui.screens.poin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import id.diskola.app.dataclass.ResponData.SearchPoinStudentItem
import id.diskola.app.ui.components.AppTextField
import id.diskola.app.ui.theme.DiskolaTheme
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.viewmodel.PoinSearchState

// The teacher's "Poin Siswa" search under the field in each of its states, at the default and a large font scale (the app is light-only).
@Composable
private fun SearchPreview(query: String, state: PoinSearchState) {
    DiskolaTheme {
        Column(Modifier.padding(horizontal = ScreenHorizontalPadding)) {
            AppTextField(value = query, onValueChange = {}, placeholder = "Ketikkan Nama atau NISN/NIS/NIK")
            SearchResults(state = state, query = query, onPick = {}, onRetry = {})
        }
    }
}

@Preview(name = "Cari, kosong", showBackground = true, widthDp = 360, heightDp = 640)
@Preview(name = "Cari, kosong, font besar", showBackground = true, widthDp = 360, heightDp = 900, fontScale = 1.5f)
@Composable
private fun IdlePreview() = SearchPreview("", PoinSearchState.Idle)

@Preview(name = "Cari, baru 2 huruf", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun ShortQueryPreview() = SearchPreview("bu", PoinSearchState.Idle)

@Preview(name = "Cari, tidak ditemukan", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun NotFoundPreview() = SearchPreview("zzz", PoinSearchState.Result(emptyList()))

@Preview(name = "Cari, akun tanpa akses", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun ForbiddenPreview() = SearchPreview("budi", PoinSearchState.Error("Akun Anda tidak memiliki akses ke fitur ini.", forbidden = true))

@Preview(name = "Cari, gagal", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun FailedPreview() = SearchPreview("budi", PoinSearchState.Error("Terjadi gangguan pada koneksi internet Anda. Silakan coba lagi."))

@Preview(name = "Cari, ada hasil", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun ResultPreview() = SearchPreview(
    "budi",
    PoinSearchState.Result(listOf(SearchPoinStudentItem(id = 1, name = "BUDI SANTOSO", student_class = "10 - DEVELOPER MOBILE", nisn = "0012345678"))),
)
