package id.diskola.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import id.diskola.app.ui.theme.HeroOverlap
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.ui.theme.extendedColors
import id.diskola.app.utils.HubRules
import id.diskola.app.utils.resolveAssetUrl
import id.diskola.app.viewmodel.HomeProfile

private const val GREETING = "Assalamualaikum,"

/**
 * The hub's green header (legacy `pembelajaran_page.xml` header + user card): school logo and name, refresh and bell (with the
 * unread badge), then avatar, name and the class/e-mail line. Every text is one line with an ellipsis, so a long school or user
 * name never pushes the layout. The bottom padding reserves the [HeroOverlap] the content card slides up into, so larger font
 * scales grow the header instead of letting the card cover the texts.
 */
@Composable
fun HomeHeader(
    profile: HomeProfile,
    bellCount: Int,
    loading: Boolean,
    onRefresh: () -> Unit,
    onBell: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extended = MaterialTheme.extendedColors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 196.dp)
            .background(
                Brush.verticalGradient(
                    0f to extended.brandGradientDark,
                    0.6f to extended.brandGradientMid,
                    1f to extended.brandGradientLight,
                ),
                RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
            )
            .padding(start = Spacing.xl, end = Spacing.xl, top = Spacing.md, bottom = HeroOverlap + Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            SchoolLogo(profile)
            Text(
                profile.schoolName.ifBlank { "Sekolah" },
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = Spacing.sm),
            )
            IconButton(onClick = onRefresh, enabled = !loading) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = Color.White)
                }
            }
            Box {
                IconButton(onClick = onBell) { Icon(Icons.Rounded.Notifications, contentDescription = "Notifikasi", tint = Color.White) }
                HubRules.bellBadge(bellCount)?.let { text -> BellBadge(text, Modifier.align(Alignment.TopEnd).padding(top = Spacing.xs, end = Spacing.xs)) }
            }
        }
        UserRow(profile)
    }
}

@Composable
private fun SchoolLogo(profile: HomeProfile) {
    val extended = MaterialTheme.extendedColors
    Box(
        modifier = Modifier.size(38.dp).clip(CircleShape).background(Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            profile.schoolName.trim().take(1).uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = extended.brandGradientDark,
        )
        if (profile.schoolLogo.isNotBlank()) {
            AsyncImage(
                model = resolveAssetUrl(profile.schoolLogo),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun UserRow(profile: HomeProfile) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f), CircleShape)
                .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(profile.userName.trim().take(1).uppercase(), style = MaterialTheme.typography.titleSmall, color = Color.White)
            // avatar, else the school logo (legacy), else the initial above
            val image = profile.userAvatar.ifBlank { profile.schoolLogo }
            if (image.isNotBlank()) {
                AsyncImage(
                    model = resolveAssetUrl(image),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Column(modifier = Modifier.weight(1f).padding(start = Spacing.md)) {
            Text(GREETING, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.82f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                profile.userName.ifBlank { "Pengguna" },
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                profile.classLine,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.82f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Unread count on the bell: red pill, "99+" from 99. */
@Composable
private fun BellBadge(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = 18.dp)
            .background(MaterialTheme.colorScheme.error, CircleShape)
            .padding(horizontal = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1)
    }
}
