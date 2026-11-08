package dali.hamza.shared.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dali.hamza.shared.ui.icons.automirrored.outlined.ArrowBack
import dali.hamza.shared.ui.icons.automirrored.outlined.ArrowForward
import dali.hamza.shared.ui.icons.automirrored.outlined.ArrowRightAlt
import dali.hamza.shared.ui.icons.automirrored.outlined.Chat
import dali.hamza.shared.ui.icons.automirrored.outlined.Label
import dali.hamza.shared.ui.icons.automirrored.outlined.Logout
import dali.hamza.shared.ui.icons.outlined.CurrencyExchange
import dali.hamza.shared.ui.icons.outlined.Help
import dali.hamza.shared.ui.icons.outlined.HelpOutline
import dali.hamza.shared.ui.icons.outlined.Home
import dali.hamza.shared.ui.icons.outlined.QueryStats
import dali.hamza.shared.ui.icons.outlined.Send
import dali.hamza.shared.ui.icons.outlined.TrendingUp
import dali.hamza.shared.ui.icons.outlined.WorkspacePremium
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/** Artwork for every vendored icon is also checked against pinned sources by the vendor script. */
@Suppress("DEPRECATION")
class IconsTest {
    private val mirrored: List<() -> ImageVector> = listOf(
        { Icons.AutoMirrored.Outlined.ArrowBack },
        { Icons.AutoMirrored.Outlined.ArrowForward },
        { Icons.AutoMirrored.Outlined.ArrowRightAlt },
        { Icons.AutoMirrored.Outlined.Chat },
        { Icons.AutoMirrored.Outlined.Label },
        { Icons.AutoMirrored.Outlined.Logout },
    )

    private val outlined: List<() -> ImageVector> = listOf(
        { Icons.Outlined.CurrencyExchange },
        { Icons.Outlined.Home },
        { Icons.Outlined.QueryStats },
        { Icons.Outlined.WorkspacePremium },
        // Preserve existing non-mirrored artwork, even for upstream-deprecated variants.
        { Icons.Outlined.Help },
        { Icons.Outlined.HelpOutline },
        { Icons.Outlined.Send },
        { Icons.Outlined.TrendingUp },
    )

    @Test
    fun materialDimensionsArePreserved() {
        for (icon in mirrored + outlined) {
            val vector = icon()
            assertEquals(24.dp, vector.defaultWidth, vector.name)
            assertEquals(24.dp, vector.defaultHeight, vector.name)
            assertEquals(24f, vector.viewportWidth, vector.name)
            assertEquals(24f, vector.viewportHeight, vector.name)
        }
    }

    @Test
    fun rtlMirroringMatchesTheOriginalVariants() {
        for (icon in mirrored) {
            assertEquals(true, icon().autoMirror, icon().name)
        }
        for (icon in outlined) {
            assertEquals(false, icon().autoMirror, icon().name)
        }
    }

    @Test
    fun vectorsAreReusedRatherThanRebuiltOnEveryAccess() {
        for (icon in mirrored + outlined) {
            assertSame(icon(), icon(), icon().name)
        }
    }
}
