package id.diskola.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.diskola.app.ui.components.CounterBadge
import id.diskola.app.ui.components.iconFor
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.viewmodel.HomeProfile

/** Doc §1.8: routing keyed by role/feature identity, not grid position — the legacy app's
 * position-based routing silently breaks for a user who is somehow both `is_student` and
 * `is_teacher` (doc §1.8's ❓, not reproduced here since `MenuKey` never relies on position). */
enum class MenuKey { MATERI, TUGAS, PRESENSI, JURNAL, AGENDA_MINGGUAN, ASESMEN, POIN, MAGANG }

data class MenuTileData(val key: MenuKey, val icon: String, val label: String, val implemented: Boolean, val locked: Boolean)

/** Doc §1.5's exact per-role item list (identical to legacy `PembelajaranPage.kt:212-260`); every tile locks without a class. */
fun menuTilesFor(profile: HomeProfile): List<MenuTileData> {
    val locked = !profile.isHavingClass
    fun tile(key: MenuKey, icon: String, label: String, implemented: Boolean) =
        MenuTileData(key, icon, label, implemented, locked)
    return when {
        profile.isStudent -> listOf(
            tile(MenuKey.MATERI, "auto_stories", "Materi", true),
            tile(MenuKey.TUGAS, "assignment", "Tugas", true),
            tile(MenuKey.PRESENSI, "how_to_reg", "Presensi", true),
            tile(MenuKey.JURNAL, "edit_note", "Jurnal", true),
            tile(MenuKey.ASESMEN, "quiz", "Asesmen", true),
            tile(MenuKey.POIN, "workspace_premium", "Poin", true),
            tile(MenuKey.MAGANG, "business_center", "Magang", false),
        )
        profile.isTeacher -> listOf(
            tile(MenuKey.MATERI, "auto_stories", "Materi", true),
            tile(MenuKey.TUGAS, "assignment", "Tugas", true),
            tile(MenuKey.PRESENSI, "how_to_reg", "Presensi", true),
            tile(MenuKey.JURNAL, "edit_note", "Jurnal", true),
            tile(MenuKey.AGENDA_MINGGUAN, "event_note", "Agenda Mingguan", true),
            tile(MenuKey.POIN, "workspace_premium", "Poin", true),
        )
        else -> listOf(
            tile(MenuKey.MATERI, "auto_stories", "Materi", true),
            tile(MenuKey.TUGAS, "assignment", "Tugas", true),
            tile(MenuKey.PRESENSI, "how_to_reg", "Presensi", true),
            tile(MenuKey.JURNAL, "edit_note", "Jurnal", true),
            tile(MenuKey.POIN, "workspace_premium", "Poin", true),
        )
    }
}

/**
 * One menu tile. At least 88 dp high and growing with the label at large font scales; a long label wraps to two lines and is
 * cut with an ellipsis after that. Lock icon top-left, count badge top-right (legacy `pembelajaran_menu_item.xml`).
 */
@Composable
fun MenuGridTile(tile: MenuTileData, badge: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .heightIn(min = 88.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(Spacing.sm),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(iconFor(tile.icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                tile.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!tile.locked && badge > 0) {
            CounterBadge(count = badge, max = 9, modifier = Modifier.align(Alignment.TopEnd))
        }
        if (tile.locked) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(20.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = "Terkunci", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
            }
        }
    }
}
