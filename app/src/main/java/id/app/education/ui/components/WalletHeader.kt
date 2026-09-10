package id.app.education.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.app.education.ui.theme.HeroOverlap
import id.app.education.ui.theme.Spacing
import id.app.education.ui.theme.extendedColors

/** Klaspay balance hero — brand gradient, used at the top of the Pembayaran main screen. */
@Composable
fun WalletBalanceHeader(
    saldoLabel: String,
    walletId: String,
    onCopyWalletId: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extended = MaterialTheme.extendedColors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to extended.brandGradientDark,
                    0.6f to extended.brandGradientMid,
                    1f to extended.brandGradientLight,
                ),
            )
            .padding(horizontal = Spacing.xl)
            // The extra bottom room is the strip the screen's first card is pulled up onto, so
            // the card overlaps empty gradient instead of covering the balance.
            .padding(top = Spacing.xl, bottom = Spacing.xl + HeroOverlap),
    ) {
        Text(
            "SALDO KLASPAY",
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.8.sp),
            color = Color.White.copy(alpha = 0.82f),
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(Spacing.sm))
        Text(
            saldoLabel,
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 30.sp, fontWeight = FontWeight.Black),
            color = Color.White,
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(Spacing.sm))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = onCopyWalletId),
        ) {
            Text("ID Wallet · $walletId", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.82f))
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(Spacing.xs))
            Icon(Icons.Rounded.ContentCopy, contentDescription = "Salin", tint = Color.White.copy(alpha = 0.82f), modifier = Modifier.size(14.dp))
        }
    }
}
