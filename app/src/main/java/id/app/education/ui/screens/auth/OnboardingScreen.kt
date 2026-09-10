package id.app.education.ui.screens.auth

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.app.education.R
import id.app.education.ui.components.AppButton
import id.app.education.ui.components.ButtonVariant
import id.app.education.ui.theme.ScreenHorizontalPadding
import id.app.education.ui.theme.Spacing
import id.app.education.ui.theme.contentContainer
import kotlinx.coroutines.launch

private data class OnboardingPage(val tag: String, val icon: ImageVector, val title: String, val description: String)

private val pages = listOf(
    OnboardingPage(
        "Pembelajaran", Icons.Rounded.AutoStories,
        "Belajar tanpa batas ruang kelas",
        "Materi, tugas, dan jurnal kelas dalam satu tempat — semua yang kamu butuhkan untuk mengikuti pelajaran.",
    ),
    OnboardingPage(
        "Kewirausahaan", Icons.Rounded.Storefront,
        "Toko sekolah di saku kamu",
        "Kelola produk, pesanan, dan pemasukan unit usaha sekolah langsung dari ponsel.",
    ),
    OnboardingPage(
        "Dompet digital", Icons.Rounded.AccountBalanceWallet,
        "Bayar sekolah tanpa antre",
        "SPP, tagihan, dan top up saldo Klaspay cukup dari satu dompet digital.",
    ),
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit, modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.contentContainer().fillMaxSize().padding(horizontal = ScreenHorizontalPadding)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm, bottom = Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painter = painterResource(R.drawable.logo_mark), contentDescription = null, modifier = Modifier.size(30.dp))
            Spacer(modifier = Modifier.size(Spacing.sm))
            Text("DISKOLA", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp, fontWeight = FontWeight.Black))
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            OnboardingPageContent(pages[page])
        }

        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.lg)) {
            pages.indices.forEach { index ->
                val selected = pagerState.currentPage == index
                val width by animateDpAsState(targetValue = if (selected) 22.dp else 6.dp, label = "indicatorWidth")
                val alpha by animateFloatAsState(targetValue = if (selected) 1f else 0.28f, label = "indicatorAlpha")
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(width)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha), RoundedCornerShape(3.dp)),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.lg)) {
            if (pagerState.currentPage > 0) {
                AppButton(
                    text = "kembali",
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                    variant = ButtonVariant.Text,
                )
            }
            AppButton(
                text = if (pagerState.currentPage == pages.lastIndex) "selesai" else "berikutnya",
                onClick = {
                    if (pagerState.currentPage == pages.lastIndex) {
                        onFinish()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 252.dp, height = 196.dp)
                .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(page.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(52.dp))
                Spacer(modifier = Modifier.size(Spacing.sm))
                Text("Ilustrasi · ${page.tag}", style = MaterialTheme.typography.labelLarge)
            }
        }
        Spacer(modifier = Modifier.size(Spacing.xxl))
        Text(page.title, style = MaterialTheme.typography.displaySmall.copy(fontSize = 25.sp), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.size(Spacing.sm))
        Text(
            page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
