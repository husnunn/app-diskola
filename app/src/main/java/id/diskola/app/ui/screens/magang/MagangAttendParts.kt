package id.diskola.app.ui.screens.magang

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import id.diskola.app.ui.components.AppCard
import id.diskola.app.ui.components.BannerError
import id.diskola.app.ui.components.CardVariant
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.screens.presensi.MasukBottomBar
import id.diskola.app.ui.screens.presensi.MasukHero
import id.diskola.app.ui.screens.presensi.MasukStatusCard
import id.diskola.app.ui.screens.presensi.ScheduleTile
import id.diskola.app.ui.screens.presensi.reportTouch
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.utils.MagangRules
import id.diskola.app.utils.PresensiRules
import id.diskola.app.viewmodel.MagangAttendView
import java.time.LocalDateTime

/**
 * "Presensi Masuk" of Magang without any side effect: the hero with the live clock (the company stands where the school's name does on
 * the school screen), the map pulled up over it, the status card, the company and schedule, and the sticky "Konfirmasi". [map] is a slot
 * because `GoogleMap` cannot be rendered by a preview.
 */
@Composable
internal fun MagangAttendContent(
    view: MagangAttendView,
    userName: String,
    userAvatar: String,
    clock: State<LocalDateTime>,
    locationOff: Boolean,
    submitting: Boolean,
    errorMessage: String,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    onEnableLocation: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
    map: @Composable () -> Unit,
) {
    val proximity = PresensiRules.proximity(view.enforced, view.radius, view.distance)
    // The map sits inside a scrolling page: left alone, the page takes every vertical drag and the map can only be panned sideways.
    var mapTouched by remember { mutableStateOf(false) }
    DetailScaffold(
        title = "Presensi Magang",
        onBack = onBack,
        useCloseIcon = true,
        modifier = modifier,
        bottomBar = {
            MasukBottomBar(status = view.status, isCheckIn = true, submitting = submitting, onSubmit = onConfirm, onRetry = {})
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState(), enabled = !mapTouched)) {
            MasukHero(name = userName, avatar = userAvatar, lastLabel = view.companyName, clock = clock, verb = "Masuk")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = ScreenHorizontalPadding, end = ScreenHorizontalPadding, bottom = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                if (errorMessage.isNotBlank()) BannerError(message = errorMessage, onDismiss = onDismissError)
                Box(modifier = Modifier.reportTouch { mapTouched = it }) { map() }
                MasukStatusCard(
                    card = MagangRules.attendCard(view.status, locationOff, proximity),
                    status = view.status,
                    onEnableLocation = onEnableLocation,
                )
                CompanyCard(view)
            }
        }
    }
}

/** Where and when: the company's name and address, the day and the hours. */
@Composable
private fun CompanyCard(view: MagangAttendView, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    AppCard(modifier = modifier, variant = CardVariant.Filled, contentPadding = PaddingValues(Spacing.lg)) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Column {
                Text(view.companyName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black), color = scheme.onSurface)
                if (view.address.isNotBlank()) {
                    Text(view.address, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                ScheduleTile("Tanggal", view.dateLabel, Icons.Rounded.CalendarMonth, Modifier.weight(1f))
                ScheduleTile("Pukul", view.timeLabel, Icons.Rounded.Schedule, Modifier.weight(1f))
            }
        }
    }
}
