package id.diskola.app.ui.screens.presensi

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.GpsOff
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.LocationSearching
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.WrongLocation
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.diskola.app.dataclass.ResponData.PresensiCheckData
import id.diskola.app.dataclass.ResponData.PresensiSchedule
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppCard
import id.diskola.app.ui.components.AvatarImage
import id.diskola.app.ui.components.BannerError
import id.diskola.app.ui.components.ButtonVariant
import id.diskola.app.ui.components.CardVariant
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.ProvideShimmer
import id.diskola.app.ui.components.SkeletonBlock
import id.diskola.app.ui.components.SkeletonSizes
import id.diskola.app.ui.components.rememberReduceMotion
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.HeroOverlap
import id.diskola.app.ui.theme.Motion
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.ui.theme.extendedColors
import id.diskola.app.utils.PresensiRules
import id.diskola.app.utils.PresensiRules.CardTone
import id.diskola.app.utils.PresensiRules.MasukCard
import id.diskola.app.utils.PresensiRules.MasukStatus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val HERO_DATE = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale("id"))
private val HERO_CLOCK = DateTimeFormatter.ofPattern("HH:mm")
private val HERO_SECONDS = DateTimeFormatter.ofPattern(":ss")

/** The map is the first thing the eye lands on, so it keeps the height it always had. */
private val MAP_HEIGHT = 240.dp

/** The icon box of the status card, which its text (and the skeleton standing in for it) is centred against. */
private val STATUS_ICON_BOX = 40.dp

/** Everything [MasukContent] draws, gathered so the stateless screen can be previewed with any combination. */
internal data class MasukViewState(
    val userName: String,
    val userAvatar: String,
    val lastLabel: String,
    val check: PresensiCheckData?,
    val status: MasukStatus,
    val restricted: Boolean,
    val radius: Float,
    /** Meters to the school, null until there is a fix. */
    val distance: Float?,
    val locationOff: Boolean,
    val submitting: Boolean,
    val errorMessage: String,
)

/**
 * "Masuk Sekolah" without any side effect: hero with the live clock, the map pulled up over it, the status card, the schedule and
 * the sticky button. [map] is a slot because `GoogleMap` cannot be rendered by a preview.
 */
@Composable
internal fun MasukContent(
    state: MasukViewState,
    clock: State<LocalDateTime>,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    onRetry: () -> Unit,
    onEnableLocation: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
    map: @Composable () -> Unit,
) {
    val isCheckIn = state.check?.isCheckIn == true
    val proximity = PresensiRules.proximity(state.restricted, state.radius, state.distance)
    // The map sits inside a scrolling page: left alone, the page takes every vertical drag and the map can only be panned sideways.
    var mapTouched by remember { mutableStateOf(false) }
    DetailScaffold(
        title = "Masuk Sekolah",
        onBack = onBack,
        useCloseIcon = true,
        modifier = modifier,
        bottomBar = {
            MasukBottomBar(status = state.status, isCheckIn = isCheckIn, submitting = state.submitting, onSubmit = onSubmit, onRetry = onRetry)
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState(), enabled = !mapTouched)) {
            MasukHero(
                name = state.userName,
                avatar = state.userAvatar,
                lastLabel = state.lastLabel,
                clock = clock,
                verb = state.check?.let { if (isCheckIn) "Masuk" else "Pulang" },
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = ScreenHorizontalPadding, end = ScreenHorizontalPadding, bottom = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                if (state.errorMessage.isNotBlank()) BannerError(message = state.errorMessage, onDismiss = onDismissError)
                Box(modifier = Modifier.reportTouch { mapTouched = it }) { map() }
                MasukStatusCard(
                    card = PresensiRules.masukCard(state.status, state.locationOff, proximity),
                    status = state.status,
                    onEnableLocation = onEnableLocation,
                )
                state.check?.schedule?.let { MasukScheduleCard(it) }
            }
        }
    }
}

// ---- Hero ----

/**
 * The brand-gradient header of Home and Pembayaran, holding who is checking in and the clock. It reports a height shorter by
 * [HeroOverlap] while still painting the full gradient, so the content that follows starts that far up and sits on the gradient (no
 * negative offset, so the map keeps its touches and the page has no dead strip at the bottom).
 */
@Composable
private fun MasukHero(
    name: String,
    avatar: String,
    lastLabel: String,
    clock: State<LocalDateTime>,
    verb: String?,
    modifier: Modifier = Modifier,
) {
    val extended = MaterialTheme.extendedColors
    val onHero = Color.White.copy(alpha = 0.9f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .overlapBelow(HeroOverlap)
            .background(
                Brush.verticalGradient(
                    0f to extended.brandGradientDark,
                    0.6f to extended.brandGradientMid,
                    1f to extended.brandGradientLight,
                ),
                RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
            )
            .padding(start = ScreenHorizontalPadding, end = ScreenHorizontalPadding, top = Spacing.md, bottom = HeroOverlap + Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            AvatarImage(imageUrl = avatar, name = name, size = 52.dp, initialStyle = MaterialTheme.typography.titleMedium)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (lastLabel.isNotBlank()) Text(lastLabel, style = MaterialTheme.typography.labelMedium, color = onHero, maxLines = 2)
            }
        }
        // Only this block reads the ticking clock, so a new second redraws the hero and nothing below it.
        val now = clock.value
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(HERO_DATE.format(now), style = MaterialTheme.typography.bodyMedium, color = onHero, modifier = Modifier.weight(1f))
            if (verb != null) HeroPill("Presensi $verb")
        }
        Row {
            Text(
                HERO_CLOCK.format(now),
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 44.sp),
                color = Color.White,
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                HERO_SECONDS.format(now),
                style = MaterialTheme.typography.titleLarge,
                color = onHero,
                modifier = Modifier.alignByBaseline(),
            )
        }
    }
}

@Composable
private fun HeroPill(text: String) {
    Row(
        modifier = Modifier
            // 12%: at 18% white text on it falls to 4.3:1 (needs 4.5:1 at this size).
            .background(Color.White.copy(alpha = 0.12f), DiskolaExtraShapes.chipBadge)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(6.dp).background(Color.White, CircleShape))
        Spacer(modifier = Modifier.size(Spacing.xs))
        Text(text, style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}

/** Tells [onChange] whether a finger is down inside this node. Watches only (Initial pass, nothing consumed): the map still gets every event. */
internal fun Modifier.reportTouch(onChange: (Boolean) -> Unit): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            onChange(event.changes.any { it.pressed })
        }
    }
}

/** Measures like normal but reports [overlap] less height: what follows is placed over the bottom of this node's own drawing. */
private fun Modifier.overlapBelow(overlap: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val cut = overlap.roundToPx().coerceIn(0, placeable.height)
    layout(placeable.width, placeable.height - cut) { placeable.place(0, 0) }
}

// ---- Map frame ----

/**
 * The map card: rounded and bordered, the floating "my location" button, and [overlay] (the distance pill). Shared by the real map
 * and the preview placeholder, so what a preview shows is what the screen shows. [onMyLocation] null while there is no fix.
 */
@Composable
internal fun MapFrame(
    onMyLocation: (() -> Unit)?,
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {},
    map: @Composable BoxScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
            .height(MAP_HEIGHT)
            .clip(DiskolaExtraShapes.card)
            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card),
    ) {
        map()
        overlay()
        IconButton(
            onClick = { onMyLocation?.invoke() },
            enabled = onMyLocation != null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Spacing.sm)
                .shadow(3.dp, CircleShape)
                .background(scheme.surfaceContainerLowest, CircleShape),
        ) {
            Icon(
                Icons.Rounded.MyLocation,
                contentDescription = "Lokasi saya",
                tint = if (onMyLocation != null) scheme.primary else scheme.onSurface.copy(alpha = 0.38f),
            )
        }
    }
}

/** The shadow the map card casts on the hero; given by the screen, so the Dinas Luar map stays flat. */
internal fun Modifier.mapCardShadow(): Modifier = shadow(3.dp, DiskolaExtraShapes.card, clip = false)

/** "45 m dari sekolah", floating over the top-left of the map. */
@Composable
internal fun BoxScope.DistancePill(meters: Float) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(Spacing.sm)
            .shadow(2.dp, DiskolaExtraShapes.searchPill)
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.searchPill)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Place, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.size(Spacing.xs))
        Text("${PresensiRules.formatDistance(meters)} dari sekolah", style = MaterialTheme.typography.labelLarge, color = scheme.onSurface)
    }
}

// ---- Status card ----

private data class ToneColors(val container: Color, val content: Color)

@Composable
private fun toneColors(tone: CardTone): ToneColors {
    val scheme = MaterialTheme.colorScheme
    val extended = MaterialTheme.extendedColors
    return when (tone) {
        CardTone.Neutral -> ToneColors(scheme.surfaceContainerLow, scheme.onSurface)
        CardTone.Info -> ToneColors(extended.infoContainer, extended.onInfoContainer)
        CardTone.Success -> ToneColors(extended.successContainer, extended.onSuccessContainer)
        CardTone.Warning -> ToneColors(extended.warningContainer, extended.onWarningContainer)
        CardTone.Error -> ToneColors(scheme.errorContainer, scheme.onErrorContainer)
    }
}

private fun cardIcon(status: MasukStatus, card: MasukCard): ImageVector = when (status) {
    MasukStatus.Loading -> Icons.Rounded.Info
    MasukStatus.CheckFailed -> Icons.Rounded.CloudOff
    is MasukStatus.ServerMessage -> Icons.Rounded.Info
    MasukStatus.ScheduleNotSet -> Icons.Rounded.EventBusy
    MasukStatus.WaitingLocation -> if (card.offersEnableLocation) Icons.Rounded.LocationOff else Icons.Rounded.LocationSearching
    MasukStatus.Mock -> Icons.Rounded.GpsOff
    MasukStatus.OutsideRadius -> Icons.Rounded.WrongLocation
    is MasukStatus.Ready -> if (card.tone == CardTone.Success) Icons.Rounded.CheckCircle else Icons.Rounded.Public
}

/** The one place the screen says where the user stands. [card] null = the check is still loading, shown as a skeleton of the same shape. */
@Composable
internal fun MasukStatusCard(
    card: MasukCard?,
    status: MasukStatus,
    onEnableLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (card == null) {
        MasukStatusSkeleton(modifier)
        return
    }
    val colors = toneColors(card.tone)
    val duration = if (rememberReduceMotion()) 0 else Motion.BASE
    val container by animateColorAsState(colors.container, tween(duration), label = "masukCardContainer")
    val content by animateColorAsState(colors.content, tween(duration), label = "masukCardContent")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(container, DiskolaExtraShapes.card)
            .padding(Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier.size(STATUS_ICON_BOX).background(content.copy(alpha = 0.12f), DiskolaExtraShapes.iconBox),
            contentAlignment = Alignment.Center,
        ) {
            Icon(cardIcon(status, card), contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
        }
        // At least as tall as the icon box and centred in it: one line of text (a server message like "Belum waktunya pulang") lines up with
        // the icon instead of hanging from its top edge; longer text, or text plus the button, simply grows from the top.
        Column(
            modifier = Modifier.weight(1f).heightIn(min = STATUS_ICON_BOX),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterVertically),
        ) {
            // Only the words are a live region: a change of state is read out, the button stays its own stop.
            Column(modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
                Text(card.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = content)
                card.detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = content.copy(alpha = 0.85f)) }
            }
            if (card.offersEnableLocation) {
                AppButton(text = "Aktifkan lokasi", onClick = onEnableLocation, variant = ButtonVariant.Tonal)
            }
        }
    }
}

@Composable
private fun MasukStatusSkeleton(modifier: Modifier = Modifier) {
    ProvideShimmer {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow, DiskolaExtraShapes.card)
                .padding(Spacing.lg)
                .semantics(mergeDescendants = true) { contentDescription = "Memuat data presensi" },
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkeletonBlock(modifier = Modifier.size(STATUS_ICON_BOX), shape = DiskolaExtraShapes.iconBox)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SkeletonBlock(modifier = Modifier.fillMaxWidth(0.7f).height(SkeletonSizes.TitleBar))
                SkeletonBlock(modifier = Modifier.fillMaxWidth(0.45f).height(SkeletonSizes.SubBar))
            }
        }
    }
}

// ---- Schedule ----

/** "Jadwal absen dari {sumber}" with the start and end as two tiles. The values are shown as the server sent them. */
@Composable
internal fun MasukScheduleCard(schedule: PresensiSchedule, modifier: Modifier = Modifier) {
    if (schedule.start_at.isBlank() && schedule.end_at.isBlank()) return
    val scheme = MaterialTheme.colorScheme
    val source = PresensiRules.scheduleSourceLabel(schedule.source)
    AppCard(modifier = modifier, variant = CardVariant.Filled, contentPadding = PaddingValues(Spacing.lg)) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(
                if (source.isBlank()) "Jadwal absen" else "Jadwal absen dari $source",
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                ScheduleTile("Mulai", schedule.start_at, Icons.AutoMirrored.Rounded.Login, Modifier.weight(1f))
                ScheduleTile("Pulang", schedule.end_at, Icons.AutoMirrored.Rounded.Logout, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ScheduleTile(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.iconBox)
            .padding(Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(22.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            Text(
                value.ifBlank { "-" },
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ---- Bottom bar ----

/** The server-driven button (its label comes from `check`), or "Coba lagi" when `check` failed; nothing in any other state. */
@Composable
internal fun MasukBottomBar(
    status: MasukStatus,
    isCheckIn: Boolean,
    submitting: Boolean,
    onSubmit: () -> Unit,
    onRetry: () -> Unit,
) {
    val ready = status as? MasukStatus.Ready
    if (ready == null && status !is MasukStatus.CheckFailed) return
    val scheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.background(scheme.surfaceContainerLowest)) {
        HorizontalDivider(color = scheme.outlineVariant)
        Box(modifier = Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md)) {
            if (ready != null) {
                AppButton(
                    text = ready.buttonLabel,
                    onClick = onSubmit,
                    enabled = !submitting,
                    loading = submitting,
                    leadingIcon = {
                        Icon(if (isCheckIn) Icons.AutoMirrored.Rounded.Login else Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                AppButton(text = "Coba lagi", onClick = onRetry, variant = ButtonVariant.Outlined, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
