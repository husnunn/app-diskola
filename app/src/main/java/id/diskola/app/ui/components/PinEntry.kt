package id.diskola.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.utils.PaymentRules

/**
 * Six dots and the numeric keypad: digits only, at most [PaymentRules.PIN_LENGTH]. Used wherever a wallet PIN is typed; it replaces
 * legacy's six boxes + system keyboard so the PIN never passes through a third-party keyboard.
 */
@Composable
fun PinEntry(
    pin: String,
    onPinChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    errorText: String? = null,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        PinDots(length = pin.length, total = PaymentRules.PIN_LENGTH)
        if (errorText != null) {
            Text(errorText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        } else {
            // keeps the keypad from jumping when the label appears
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(Spacing.lg))
        }
        NumericKeypad(
            onDigit = { onPinChange(PaymentRules.appendPinDigit(pin, it)) },
            onBackspace = { onPinChange(PaymentRules.deletePinDigit(pin)) },
        )
    }
}
