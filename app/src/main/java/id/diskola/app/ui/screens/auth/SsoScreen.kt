package id.diskola.app.ui.screens.auth

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import id.diskola.app.ui.components.AmbientGradientBackground
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppCard
import id.diskola.app.ui.components.AppLoadingDialog
import id.diskola.app.ui.components.AppTextField
import id.diskola.app.ui.components.ButtonVariant
import id.diskola.app.ui.components.CardVariant
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.ui.theme.contentContainer
import id.diskola.app.utils.session.AuthError
import id.diskola.app.viewmodel.SsoViewModel

/** Result of the Google Credential-Manager sign-in step, handed in from [LoginScreen]. */
data class GoogleAccountInfo(
    val idToken: String,
    val displayName: String,
    val email: String,
    val profilePictureUri: String = "",
) : java.io.Serializable

/**
 * `LoginSso` (doc `02-auth-login-sesi.md` §5.3). Whether "iya, itu saya"/"tidak, bukan saya" or a
 * school picker + "daftar" is shown follows the account's own `school` from `check-email`, exactly
 * like the legacy layout's `school.name.isEmpty()` binding — the only deliberate change (decision
 * Q2) is that "daftar" now requires a school to actually be picked first.
 */
@Composable
fun SsoScreen(
    account: GoogleAccountInfo,
    onBack: () -> Unit,
    /** `registering` = which button was pressed ("daftar" vs "iya, itu saya"); `klaspayActive` =
     * `login-sso/school`'s result, so the caller can route to Main vs Klaspay activation. */
    onConfirmed: (registering: Boolean, klaspayActive: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SsoViewModel = hiltViewModel(),
) {
    val checkEmailResult by viewModel.checkEmailResult.collectAsStateWithLifecycle()
    val selectedSchool by viewModel.selectedSchool.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val ssoSchoolResult by viewModel.ssoSchoolResult.collectAsStateWithLifecycle()
    val isGuestGate by viewModel.isGuestGate.collectAsStateWithLifecycle()

    var schoolSheetVisible by remember { mutableStateOf(false) }
    var registering by remember { mutableStateOf(false) }
    var guestGateConfirmed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (checkEmailResult == null) viewModel.checkEmail(account)
    }

    LaunchedEffect(ssoSchoolResult) {
        ssoSchoolResult?.let { klaspayActive -> onConfirmed(registering, klaspayActive) }
    }

    // Doc §5.2, decision Q1: this gate must clear before the rest of the screen is usable.
    if (isGuestGate == true && !guestGateConfirmed) {
        GuestConfirmDialog(onDismiss = onBack, onConfirm = { guestGateConfirmed = true })
        return
    }

    val ssoData = checkEmailResult?.data
    val hasSchool = ssoData?.school?.name?.isNotBlank() == true
    val roleLabel = when {
        ssoData?.is_student == true -> "Siswa"
        ssoData?.is_teacher == true -> "Guru"
        else -> "Tamu"
    }
    val className = ssoData?.current_class?.class_room?.name?.takeIf { it.isNotBlank() }
        ?: ssoData?.current_class?.name?.takeIf { it.isNotBlank() }

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
                    Text("masuk sebagai".uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.size(Spacing.sm))
                    val image = ssoData?.image?.takeIf { it.isNotBlank() } ?: account.profilePictureUri
                    if (image.isNotBlank()) {
                        AsyncImage(
                            model = image,
                            contentDescription = null,
                            modifier = Modifier.size(84.dp).background(MaterialTheme.colorScheme.surfaceContainerLow, CircleShape).border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(38.dp))
                        }
                    }
                    Spacer(modifier = Modifier.size(Spacing.sm))
                    Text((ssoData?.username?.takeIf { it.isNotBlank() } ?: account.displayName).ifBlank { "-" }, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    Text(ssoData?.email?.takeIf { it.isNotBlank() } ?: account.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                AppCard(variant = CardVariant.Outlined) {
                    Column {
                        DetailRow("Peran", roleLabel)
                        if (!className.isNullOrBlank()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.xs))
                            DetailRow("Kelas", className)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.xs))
                        if (hasSchool) {
                            DetailRow("Sekolah", ssoData?.school?.name.orEmpty())
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = Spacing.xs)) {
                                Text("Sekolah", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                            }
                            AppTextField(
                                value = selectedSchool?.name.orEmpty(),
                                onValueChange = {},
                                placeholder = "Pilih Sekolah",
                                readOnly = true,
                                onClick = { schoolSheetVisible = true },
                                trailing = { Icon(Icons.Rounded.ExpandMore, contentDescription = null) },
                            )
                        }
                    }
                }

                if (hasSchool) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.fillMaxWidth()) {
                        AppButton(
                            text = "iya, itu saya",
                            onClick = { registering = false; viewModel.confirmSchool() },
                            modifier = Modifier.weight(1f),
                        )
                        AppButton(text = "tidak, bukan saya", onClick = onBack, variant = ButtonVariant.Text, modifier = Modifier.weight(1f))
                    }
                } else {
                    AppButton(
                        text = "daftar",
                        onClick = { registering = true; viewModel.confirmSchool() },
                        // Decision Q2 — deliberate deviation from the legacy app: "daftar" no
                        // longer submits with an empty `school_id`.
                        enabled = selectedSchool != null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (schoolSheetVisible) {
        SsoSchoolPicker(
            selected = selectedSchool,
            onSelect = { viewModel.setSelectedSchool(it); schoolSheetVisible = false },
            onDismiss = { schoolSheetVisible = false },
        )
    }

    if (loading) {
        AppLoadingDialog(message = if (checkEmailResult == null) "Sedang mencari data pengguna..." else "Proses login akun")
    }

    when (val error = authError) {
        is AuthError.DeviceConflict -> DeviceConflictDialog(message = error.message, onDismiss = { viewModel.clearAuthError() })
        is AuthError.InvalidPassword -> LoginFailedSheet(message = error.message, onDismiss = { viewModel.clearAuthError() })
        is AuthError.Message -> LoginFailedSheet(message = error.message, onDismiss = { viewModel.clearAuthError() })
        null -> Unit
    }
}

/** A minimal school picker for SSO registration — reuses the same local-only cache as
 * [SchoolPickerSheet], but doesn't need its search/paging affordances wired to a shared
 * `LoginViewModel` instance, so it drives `SchoolRepository` through its own tiny state holder. */
@Composable
private fun SsoSchoolPicker(
    selected: id.diskola.app.dataclass.ResponData.SchoolItem?,
    onSelect: (id.diskola.app.dataclass.ResponData.SchoolItem) -> Unit,
    onDismiss: () -> Unit,
    viewModel: id.diskola.app.viewmodel.LoginViewModel = hiltViewModel(),
) {
    val schools by viewModel.schools.collectAsStateWithLifecycle()
    val loading by viewModel.schoolListLoading.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.openSchoolPicker() }
    SchoolPickerSheet(
        schools = schools,
        selected = selected,
        loading = loading,
        onQueryChange = { viewModel.onSchoolQueryChange(it) },
        onLoadMore = { viewModel.loadMoreSchools() },
        onRefresh = { viewModel.refreshSchools() },
        onSelect = onSelect,
        onDismiss = onDismiss,
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
    }
}
