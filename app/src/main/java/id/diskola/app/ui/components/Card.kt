package id.diskola.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card as M3Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.Spacing

enum class CardVariant { Elevated, Outlined, Filled }

/**
 * The design-system card — elevated/outlined/filled, per the DiskolaKit gallery. `Outlined` is
 * used for form cards sitting on top of the Auth ambient background; `Filled` for secondary
 * info/help cards.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    variant: CardVariant = CardVariant.Elevated,
    contentPadding: PaddingValues = PaddingValues(Spacing.xl),
    content: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    // Cards span their column unless a caller narrows them: left to wrap content, two cards with
    // the same role end up different widths purely from how much text each happens to hold.
    val cardModifier = Modifier.fillMaxWidth().then(modifier)
    when (variant) {
        CardVariant.Elevated -> M3Card(
            modifier = cardModifier,
            shape = DiskolaExtraShapes.card,
            colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLowest),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            androidx.compose.foundation.layout.Box(modifier = Modifier.padding(contentPadding)) { content() }
        }

        CardVariant.Outlined -> M3Card(
            modifier = cardModifier,
            shape = DiskolaExtraShapes.card,
            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
            border = BorderStroke(1.dp, scheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            androidx.compose.foundation.layout.Box(modifier = Modifier.padding(contentPadding)) { content() }
        }

        CardVariant.Filled -> M3Card(
            modifier = cardModifier,
            shape = DiskolaExtraShapes.card,
            colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            androidx.compose.foundation.layout.Box(modifier = Modifier.padding(contentPadding)) { content() }
        }
    }
}
