package id.diskola.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import id.diskola.app.ui.theme.HeroOverlap
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.ui.theme.extendedColors

/** What the Klaspay hero shows in place of the balance. */
sealed interface WalletHeaderState {
    /** The wallet is not activated yet: an "Aktivasi Wallet" button instead of a balance. */
    data object Inactive : WalletHeaderState

    /** `payment/wallet` has not answered yet. */
    data object Loading : WalletHeaderState

    /** `payment/wallet` failed and nothing is known: offer a retry. */
    data object Failed : WalletHeaderState

    data class Ready(val balanceLabel: String) : WalletHeaderState
}

/** Klaspay balance hero — brand gradient, used at the top of the Pembayaran main screen. */
@Composable
fun WalletBalanceHeader(
    state: WalletHeaderState,
    onInfo: () -> Unit,
    onActivate: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extended = MaterialTheme.extendedColors
    val onHero = Color.White.copy(alpha = 0.82f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to extended.brandGradientDark,
                    0.6f to extended.brandGradientMid,
                    1f to extended.brandGradientLight,
                ),
            )
            .padding(start = Spacing.xl, end = Spacing.sm)
            // The extra bottom room is the strip the screen's first card is pulled up onto, so
            // the card overlaps empty gradient instead of covering the balance.
            .padding(top = Spacing.xl, bottom = Spacing.xl + HeroOverlap),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                "SALDO KLASPAY",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.8.sp),
                color = onHero,
            )
            when (state) {
                is WalletHeaderState.Ready -> Text(
                    state.balanceLabel,
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 30.sp, fontWeight = FontWeight.Black),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                WalletHeaderState.Loading -> Text("Memuat saldo…", style = MaterialTheme.typography.bodyLarge, color = onHero)
                WalletHeaderState.Failed -> {
                    Text("Saldo belum dapat dimuat", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                    TextButton(onClick = onRetry, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                        Text("Coba lagi")
                    }
                }
                WalletHeaderState.Inactive -> {
                    Text("Wallet belum diaktifkan", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                    Spacer(modifier = Modifier.size(Spacing.xs))
                    AppButton(text = "Aktivasi Wallet", onClick = onActivate, variant = ButtonVariant.Tonal)
                }
            }
        }
        // 48dp touch target
        IconButton(onClick = onInfo) {
            Icon(Icons.Rounded.Info, contentDescription = "Info wallet", tint = onHero)
        }
    }
}
