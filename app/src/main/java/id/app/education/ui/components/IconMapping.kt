package id.app.education.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AddCard
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalLibrary
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Maps the design handoff's Material Symbols Rounded icon-name strings (e.g. `"auto_stories"`)
 * to the equivalent `Icons.Rounded.*` from `material-icons-extended`, so screens can be built
 * directly against the mockup's icon names instead of guessing an ImageVector each time.
 */
fun iconFor(name: String): ImageVector = when (name) {
    "auto_stories" -> Icons.Rounded.AutoStories
    "assignment" -> Icons.Rounded.Assignment
    "how_to_reg" -> Icons.Rounded.HowToReg
    "edit_note" -> Icons.Rounded.EditNote
    "event_note" -> Icons.Rounded.EventNote
    "quiz" -> Icons.Rounded.Quiz
    "workspace_premium" -> Icons.Rounded.WorkspacePremium
    "business_center" -> Icons.Rounded.BusinessCenter
    "menu_book" -> Icons.Rounded.MenuBook
    "code" -> Icons.Rounded.Code
    "image" -> Icons.Rounded.Image
    "refresh" -> Icons.Rounded.Refresh
    "manage_accounts" -> Icons.Rounded.ManageAccounts
    "alternate_email" -> Icons.Rounded.AlternateEmail
    "lock_reset" -> Icons.Rounded.LockReset
    "badge" -> Icons.Rounded.Badge
    "devices" -> Icons.Rounded.Devices
    "gavel" -> Icons.Rounded.Gavel
    "info" -> Icons.Rounded.Info
    "logout" -> Icons.Rounded.Logout
    "warning" -> Icons.Rounded.Warning
    "photo_camera" -> Icons.Rounded.PhotoCamera
    "receipt_long" -> Icons.Rounded.ReceiptLong
    "campaign" -> Icons.Rounded.Campaign
    "account_balance_wallet" -> Icons.Rounded.AccountBalanceWallet
    "add_card" -> Icons.Rounded.AddCard
    "swap_horiz" -> Icons.Rounded.SwapHoriz
    "qr_code_scanner" -> Icons.Rounded.QrCodeScanner
    "history" -> Icons.Rounded.History
    "bolt" -> Icons.Rounded.Bolt
    "volunteer_activism" -> Icons.Rounded.VolunteerActivism
    "shopping_bag" -> Icons.Rounded.ShoppingBag
    "local_library" -> Icons.Rounded.LocalLibrary
    "more_horiz" -> Icons.Rounded.MoreHoriz
    "help" -> Icons.Rounded.HelpOutline
    else -> Icons.Rounded.Info
}
