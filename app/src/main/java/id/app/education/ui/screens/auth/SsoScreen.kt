package id.app.education.ui.screens.auth

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import id.app.education.dataclass.ResponData.SchoolItem
import id.app.education.ui.components.AmbientGradientBackground
import id.app.education.ui.components.AppButton
import id.app.education.ui.components.AppCard
import id.app.education.ui.components.AppTextField
import id.app.education.ui.components.ButtonVariant
import id.app.education.ui.components.CardVariant
import id.app.education.ui.theme.ScreenHorizontalPadding
import id.app.education.ui.theme.Spacing
import id.app.education.ui.theme.contentContainer
import id.app.education.viewmodel.AuthViewModel

/** Result of the Google Credential-Manager sign-in step, handed in from [LoginScreen]. */
data class GoogleAccountInfo(
    val idToken: String,
    val displayName: String,
    val email: String,
) : java.io.Serializable

@Composable
fun SsoScreen(
    account: GoogleAccountInfo,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    // NOTE: the backend `POST mobile/app/authentication/login-sso` contract (request/response
    // shape) is not present in this codebase and could not be verified against the real API, so
    // it is intentionally not called yet — wire it into AuthApiService/AuthViewModel once confirmed.
    var linked by remember { mutableStateOf(false) }
    var schoolSheetVisible by remember { mutableStateOf(false) }
    var selectedSchool by remember { mutableStateOf<SchoolItem?>(null) }
    val schools by viewModel.schools.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()

    AmbientGradientBackground(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null) }
            }
            Column(
                modifier = Modifier
                    .contentContainer()
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.xl),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("MASUK SEBAGAI", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.size(Spacing.sm))
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(38.dp))
                    }
                    Spacer(modifier = Modifier.size(Spacing.sm))
                    Text(account.displayName.ifBlank { "-" }, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    Text(account.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                AppCard(variant = CardVariant.Outlined) {
                    Column {
                        DetailRow("Peran", if (linked) "Siswa" else "-")
                        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.xs))
                        DetailRow("NISN", if (linked) "-" else "-")
                        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.xs))
                        DetailRow("Kelas", if (linked) "-" else "-")
                        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.xs))
                        if (linked) {
                            DetailRow("Sekolah", "-")
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = Spacing.xs)) {
                                Text("Sekolah", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                            }
                            AppTextField(
                                value = selectedSchool?.name.orEmpty(),
                                onValueChange = {},
                                placeholder = "Pilih sekolah",
                                readOnly = true,
                                onClick = { schoolSheetVisible = true },
                                trailing = { Icon(Icons.Rounded.ExpandMore, contentDescription = null) },
                            )
                        }
                    }
                }

                if (linked) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.fillMaxWidth()) {
                        AppButton(text = "Iya, itu saya", onClick = onContinue, modifier = Modifier.weight(1f))
                        AppButton(text = "Tidak, bukan saya", onClick = onBack, variant = ButtonVariant.Text, modifier = Modifier.weight(1f))
                    }
                } else {
                    AppButton(
                        text = "Daftar",
                        onClick = onContinue,
                        enabled = selectedSchool != null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (schoolSheetVisible) {
        SchoolPickerSheet(
            schools = schools,
            selected = selectedSchool,
            loading = loading,
            onQueryChange = { viewModel.searchSchools(it) },
            onSelect = { selectedSchool = it; schoolSheetVisible = false },
            onDismiss = { schoolSheetVisible = false },
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
    }
}
