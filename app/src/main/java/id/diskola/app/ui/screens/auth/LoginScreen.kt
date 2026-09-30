package id.diskola.app.ui.screens.auth

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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.diskola.app.R
import id.diskola.app.dataclass.ResponData.CheckAccountUserData
import id.diskola.app.ui.components.AmbientGradientBackground
import id.diskola.app.ui.components.AppBottomSheet
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppCard
import id.diskola.app.ui.components.AppLoadingDialog
import id.diskola.app.ui.components.AppTextField
import id.diskola.app.ui.components.ButtonVariant
import id.diskola.app.ui.components.CardVariant
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.ui.theme.contentContainer
import id.diskola.app.viewmodel.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onAccountVerified: (CheckAccountUserData) -> Unit,
    onOpenTerms: () -> Unit,
    onGoogleSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
    termsAccepted: Boolean,
    onTermsAcceptedChange: (Boolean) -> Unit,
) {
    var nisn by remember { mutableStateOf("") }
    var schoolSheetVisible by remember { mutableStateOf(false) }

    val selectedSchool by viewModel.selectedSchool.collectAsStateWithLifecycle()
    val schools by viewModel.schools.collectAsStateWithLifecycle()
    val schoolListLoading by viewModel.schoolListLoading.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val checkAccountResult by viewModel.checkAccountResult.collectAsStateWithLifecycle()

    LaunchedEffect(checkAccountResult) {
        checkAccountResult?.let(onAccountVerified)
    }

    // Doc `02-auth-login-sesi.md` §4.1: just non-blank, not a length gate (gap `02a` L1).
    val canSubmit = nisn.isNotBlank() && selectedSchool != null && termsAccepted

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
            }

            Text("LOGIN AKUN", style = MaterialTheme.typography.displaySmall.copy(fontSize = 24.sp, fontWeight = FontWeight.Black))

            AppCard(variant = CardVariant.Outlined) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    AppTextField(
                        value = nisn,
                        onValueChange = { input -> nisn = input.filter { it.isDigit() } },
                        label = "NISN / NIS / NIK",
                        placeholder = "Ketikkan NISN/NIS/NIK",
                        keyboardType = KeyboardType.Number,
                    )

                    AppTextField(
                        value = selectedSchool?.name.orEmpty(),
                        onValueChange = {},
                        label = "Sekolah",
                        placeholder = "Pilih Sekolah",
                        readOnly = true,
                        onClick = {
                            schoolSheetVisible = true
                            viewModel.openSchoolPicker()
                        },
                        trailing = { Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    )

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = termsAccepted, onCheckedChange = onTermsAcceptedChange)
                        val termsText = remember {
                            androidx.compose.ui.text.buildAnnotatedString {
                                append("Dengan ini saya membaca, memahami dan menyetujui hal-hal yang tercantum pada ")
                                pushStringAnnotation(tag = "terms", annotation = "terms")
                                withStyle(androidx.compose.ui.text.SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)) {
                                    append("Syarat dan Ketentuan dan Kebijakan Privasi")
                                }
                                pop()
                                append(" yang berlaku")
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
                        text = "masuk",
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
        }
    }

    if (schoolSheetVisible) {
        SchoolPickerSheet(
            schools = schools,
            selected = selectedSchool,
            loading = schoolListLoading,
            onQueryChange = { viewModel.onSchoolQueryChange(it) },
            onLoadMore = { viewModel.loadMoreSchools() },
            onRefresh = { viewModel.refreshSchools() },
            onSelect = {
                viewModel.setSelectedSchool(it)
                schoolSheetVisible = false
            },
            onDismiss = { schoolSheetVisible = false },
        )
    }

    if (loading && checkAccountResult == null) {
        AppLoadingDialog(message = "Sedang mencari data pengguna...")
    }

    if (errorMessage.isNotBlank()) {
        AppBottomSheet(title = "Login Gagal", onDismiss = { viewModel.clearError() }) {
            Column(modifier = Modifier.padding(horizontal = Spacing.xl, vertical = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                Text(errorMessage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                AppButton(text = "Coba Lagi", onClick = { viewModel.clearError() }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
