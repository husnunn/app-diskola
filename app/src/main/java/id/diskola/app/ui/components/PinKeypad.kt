package id.diskola.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.diskola.app.ui.theme.contentContainer

private val KeypadMaxWidth = 360.dp

/** 6-dot PIN progress indicator. */
@Composable
fun PinDots(length: Int, total: Int = 6, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(total) { index ->
            val filled = index < length
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(
                        if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                        CircleShape,
                    ),
            )
        }
    }
}

/** 3x3 + 0/backspace numeric keypad for PIN entry. */
@Composable
fun NumericKeypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
    )
    Column(
        // Keys are weight-based, so without a cap a tablet stretches each one to ~310dp wide and
        // the keypad grows taller than the screen. A thumb-reachable keypad is the point.
        modifier = modifier.contentContainer(maxWidth = KeypadMaxWidth),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { digit ->
                    KeypadKey(modifier = Modifier.weight(1f), onClick = { onDigit(digit) }) {
                        Text(digit.toString(), style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f))
            KeypadKey(modifier = Modifier.weight(1f), onClick = { onDigit('0') }) {
                Text("0", style = MaterialTheme.typography.headlineSmall)
            }
            KeypadKey(modifier = Modifier.weight(1f), onClick = onBackspace) {
                Icon(Icons.Rounded.Backspace, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun KeypadKey(modifier: Modifier = Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .aspectRatio(1.6f)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
