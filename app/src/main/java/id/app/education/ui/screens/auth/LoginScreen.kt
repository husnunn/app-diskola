package id.app.education.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.app.education.R
import id.app.education.dataclass.ResponData.CheckAccountUserData
import id.app.education.ui.components.AmbientGradientBackground
import id.app.education.ui.components.AppButton
import id.app.education.ui.components.AppCard
import id.app.education.ui.components.AppTextField
import id.app.education.ui.components.BadgeTone
import id.app.education.ui.components.BannerError
import id.app.education.ui.components.ButtonVariant
import id.app.education.ui.components.CardVariant
import id.app.education.ui.components.StatusBadge
import id.app.education.ui.theme.ScreenHorizontalPadding
import id.app.education.ui.theme.Spacing
import id.app.education.ui.theme.contentContainer
import id.app.education.viewmodel.AuthViewModel
import androidx.compose.ui.text.withStyle

@Composable
fun LoginScreen(
    onAccountVerified: (CheckAccountUserData) -> Unit,
    onOpenTerms: () -> Unit,
    onGoogleSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
    termsAccepted: Boolean,
    onTermsAcceptedChange: (Boolean) -> Unit,
) {
    var nisn by remember { mutableStateOf("") }
    var schoolSheetVisible by remember { mutableStateOf(false) }

    val selectedSchool by viewModel.selectedSchool.collectAsStateWithLifecycle()
    val schools by viewModel.schools.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val checkAccountResult by viewModel.checkAccountResult.collectAsStateWithLifecycle()

    LaunchedEffect(checkAccountResult) {
        checkAccountResult?.let(onAccountVerified)
    }

    val canSubmit = nisn.length >= 4 && selectedSchool != null && termsAccepted

    AmbientGradientBackground(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .contentContainer()
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenHorizontalPadding)
                .padding(top = Spacing.md, bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Image(painter = painterResource(R.drawable.logo_mark), contentDescription = null, modifier = Modifier.size(56.dp))
                Spacer(modifier = Modifier.size(Spacing.sm))
                Text("DISKOLA", style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 6.sp, fontWeight = FontWeight.Black))
                Text(
                    "SISTEM INFORMASI AKADEMIK TERPADU",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.6.sp, fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.size(Spacing.sm))
                StatusBadge(label = "Aktif", tone = BadgeTone.Success)
            }

            Column {
                Text("Selamat Datang", style = MaterialTheme.typography.displaySmall.copy(fontSize = 26.sp))
                Text(
                    "Masuk untuk melanjutkan ke portal sekolahmu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (errorMessage.isNotBlank()) {
                BannerError(message = errorMessage, onDismiss = { viewModel.clearError() })
            }

            AppCard(variant = CardVariant.Outlined) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    Text("Login Akun", style = MaterialTheme.typography.titleMedium)

                    AppTextField(
                        value = nisn,
                        onValueChange = { input -> nisn = input.filter { it.isDigit() } },
                        label = "NISN / NIS / NIK",
                        placeholder = "Contoh: 0051234567",
                        keyboardType = KeyboardType.Number,
                    )

                    AppTextField(
                        value = selectedSchool?.name.orEmpty(),
                        onValueChange = {},
                        label = "Sekolah",
                        placeholder = "Pilih sekolah",
                        readOnly = true,
                        onClick = { schoolSheetVisible = true },
                        trailing = { Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    )

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = termsAccepted, onCheckedChange = onTermsAcceptedChange)
                        val termsText = remember {
                            androidx.compose.ui.text.buildAnnotatedString {
                                append("Saya menyetujui ")
                                pushStringAnnotation(tag = "terms", annotation = "terms")
                                withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append("Syarat & Ketentuan")
                                }
                                pop()
                                append(" penggunaan Diskola.")
                            }
                        }
                        val onSurfaceColor = MaterialTheme.colorScheme.onSurface
                        androidx.compose.foundation.text.ClickableText(
                            text = termsText,
                            style = MaterialTheme.typography.bodySmall.copy(color = onSurfaceColor),
                            modifier = Modifier.weight(1f),
                            onClick = { offset ->
                                termsText.getStringAnnotations("terms", offset, offset).firstOrNull()?.let { onOpenTerms() }
                            },
                        )
                    }

                    AppButton(
                        text = "Masuk",
                        onClick = { viewModel.checkAccount(nisn) },
                        enabled = canSubmit,
                        loading = loading,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        androidx.compose.material3.HorizontalDivider(modifier = Modifier.weight(1f))
                        Text("atau", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = Spacing.sm))
                        androidx.compose.material3.HorizontalDivider(modifier = Modifier.weight(1f))
                    }

                    AppButton(
                        text = "Masuk dengan Akun Google",
                        onClick = onGoogleSignIn,
                        variant = ButtonVariant.Outlined,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            AppCard(variant = CardVariant.Filled) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        "NISN/NIK belum terdaftar? Hubungi operator sekolah Anda untuk pendaftaran akun.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    AppButton(text = "Bantuan", onClick = {}, variant = ButtonVariant.Outlined, modifier = Modifier.fillMaxWidth(0.4f))
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
            onSelect = {
                viewModel.setSelectedSchool(it)
                schoolSheetVisible = false
            },
            onDismiss = { schoolSheetVisible = false },
        )
    }
}
