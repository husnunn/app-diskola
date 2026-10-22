package id.diskola.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import id.diskola.app.ui.theme.Spacing

/**
 * Text for content we do not control (server messages, event names): at most [collapsedLines] lines with an ellipsis; tapping it
 * shows everything, and a small "Selengkapnya" appears only when something is actually cut.
 */
@Composable
fun ExpandableText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    collapsedLines: Int = 3,
) {
    var expanded by rememberSaveable(text) { mutableStateOf(false) }
    var overflowing by remember(text) { mutableStateOf(false) }
    Column(modifier = modifier.clickable(enabled = overflowing || expanded) { expanded = !expanded }) {
        Text(
            text,
            style = style,
            color = color,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded) overflowing = it.hasVisualOverflow },
        )
        if (overflowing || expanded) {
            Text(
                if (expanded) "Ringkas" else "Selengkapnya",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }
    }
}
