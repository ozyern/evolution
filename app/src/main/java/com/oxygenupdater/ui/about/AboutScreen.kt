package com.oxygenupdater.ui.about

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.RoundedRectangle
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.R
import com.oxygenupdater.extensions.openLink
import com.oxygenupdater.icons.Discord
import com.oxygenupdater.icons.Download
import com.oxygenupdater.icons.Forum
import com.oxygenupdater.icons.GitHub
import com.oxygenupdater.icons.Help
import com.oxygenupdater.icons.Link
import com.oxygenupdater.icons.Logos
import com.oxygenupdater.icons.Mail
import com.oxygenupdater.icons.Patreon
import com.oxygenupdater.icons.Symbols
import com.oxygenupdater.ui.common.ConditionalNavBarPadding
import com.oxygenupdater.ui.common.GroupedCardShape
import com.oxygenupdater.ui.common.GroupedFooter
import com.oxygenupdater.ui.common.GroupedHeader
import com.oxygenupdater.ui.common.GroupedRow
import com.oxygenupdater.ui.common.GroupedSection
import com.oxygenupdater.ui.common.RichText
import com.oxygenupdater.ui.common.groupedBackground
import com.oxygenupdater.ui.common.groupedCardColor
import com.oxygenupdater.ui.main.ChildRoute
import com.oxygenupdater.ui.main.CnRomsRoute
import com.oxygenupdater.ui.main.FaqRoute
import com.oxygenupdater.ui.main.GuideRoute
import com.oxygenupdater.ui.main.NavType
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewThemes
import com.oxygenupdater.ui.theme.PreviewWindowSize

/**
 * Styled like an iOS app's "About" page in Settings: the app icon & version up top, then grouped
 * cards of links, and the longer text in cards of its own.
 */
@Composable
fun AboutScreen(
    navType: NavType,
    @Suppress("UNUSED_PARAMETER") windowWidthSize: WindowWidthSizeClass,
    navigateTo: (ChildRoute) -> Unit,
    openEmail: () -> Unit,
) = Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
        .fillMaxHeight()
        .background(groupedBackground)
        .verticalScroll(rememberScrollState())
        .testTag(AboutScreenTestTag)
) {
    // Keep cards at a readable width on tablets/foldables
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(max = 640.dp)
    ) {
        AppHeader()
        Links(navigateTo = navigateTo, openEmail = openEmail)

        TextCard(Modifier.padding(top = 20.dp)) {
            RichText(
                text = stringResource(R.string.about_description),
                modifier = Modifier.testTag(AboutScreen_DescriptionTestTag)
            )
            RichText(
                text = stringResource(R.string.about_support),
                modifier = Modifier
                    .padding(top = 12.dp)
                    .testTag(AboutScreen_SupportTestTag)
            )
        }

        GroupedHeader(
            text = stringResource(R.string.about_background_story_header),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .testTag(AboutScreen_BackgroundStoryHeaderTestTag)
        )
        TextCard {
            RichText(
                text = stringResource(R.string.about_background_story),
                modifier = Modifier.testTag(AboutScreen_BackgroundStoryTestTag)
            )
        }

        GroupedFooter(
            text = stringResource(R.string.about_third_party_app_notice),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .testTag(AboutScreen_ThirdPartyNoticeTestTag)
        )

        ConditionalNavBarPadding(navType)
    }
}

@Composable
private fun AppHeader() = Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
) {
    Image(
        painter = painterResource(R.drawable.app_logo),
        contentDescription = null,
        modifier = Modifier
            .size(88.dp)
            .clip(RoundedRectangle(20.dp))
    )
    Text(
        text = stringResource(R.string.app_name),
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 12.dp)
    )
    Text(
        text = "v${BuildConfig.VERSION_NAME}",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 15.sp,
    )
}

@Composable
private fun Links(
    navigateTo: (ChildRoute) -> Unit,
    openEmail: () -> Unit,
) = with(LocalContext.current) {
    GroupedSection(
        header = stringResource(R.string.about_help_header),
        modifier = Modifier.testTag(AboutScreen_LinksTestTag)
    ) {
        GroupedRow(
            title = stringResource(R.string.install_guide),
            icon = Symbols.Help,
            iconTint = IconBlue,
            chevron = true,
            onClick = { navigateTo(GuideRoute(false)) },
        )
        GroupedRow(
            title = stringResource(R.string.faq_menu_item),
            icon = Symbols.Forum,
            iconTint = IconOrange,
            chevron = true,
            onClick = { navigateTo(FaqRoute) },
        )
    }

    GroupedSection(header = stringResource(R.string.about_firmware_header)) {
        GroupedRow(
            title = stringResource(R.string.cn_roms_title),
            subtitle = stringResource(R.string.cn_roms_row_subtitle),
            icon = Symbols.Download,
            iconTint = IconRed,
            chevron = true,
            onClick = { navigateTo(CnRomsRoute) },
        )
    }

    GroupedSection(header = stringResource(R.string.about_community_header)) {
        GroupedRow(
            title = stringResource(R.string.about_discord_button_text),
            icon = Logos.Discord,
            iconTint = IconIndigo,
            chevron = true,
            onClick = { openLink(LinkType.Discord.value) },
        )
        GroupedRow(
            title = stringResource(R.string.about_email_button_text),
            icon = Symbols.Mail,
            iconTint = IconBlue,
            chevron = true,
            onClick = openEmail,
        )
        GroupedRow(
            title = stringResource(R.string.about_github_button_text),
            icon = Logos.GitHub,
            iconTint = IconGray,
            chevron = true,
            onClick = { openLink(LinkType.GitHub.value) },
        )
        GroupedRow(
            title = stringResource(R.string.about_website_button_text),
            icon = Symbols.Link,
            iconTint = IconTeal,
            chevron = true,
            onClick = { openLink(LinkType.Website.value) },
        )
        GroupedRow(
            title = stringResource(R.string.about_patreon_button_text),
            icon = Logos.Patreon,
            iconTint = IconPink,
            chevron = true,
            onClick = { openLink(LinkType.Patreon.value) },
        )
    }
}

/** A plain rounded card for long-form text */
@Composable
private fun TextCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) = Column(
    modifier
        .padding(horizontal = 16.dp)
        .fillMaxWidth()
        .clip(GroupedCardShape)
        .background(groupedCardColor)
        .padding(16.dp)
) { content() }

@JvmInline
private value class LinkType private constructor(val value: String) {

    override fun toString() = value

    companion object {
        val Discord = LinkType("https://discord.gg/5TXdhKJ")
        val GitHub = LinkType("https://github.com/oxygen-updater/oxygen-updater")
        val Patreon = LinkType("https://patreon.com/oxygenupdater")
        val Website = LinkType("https://oxygenupdater.com/")
    }
}

// Apple system colours, for settings-style icons
private val IconBlue = Color(0xFF007AFF)
private val IconOrange = Color(0xFFFF9500)
private val IconRed = Color(0xFFFF3B30)
private val IconIndigo = Color(0xFF5856D6)
private val IconTeal = Color(0xFF30B0C7)
private val IconPink = Color(0xFFFF2D55)
private val IconGray = Color(0xFF8E8E93)

private const val TAG = "AboutScreen"

@VisibleForTesting
const val AboutScreenTestTag = TAG

@VisibleForTesting
const val AboutScreen_LinksTestTag = TAG + "_Links"

@VisibleForTesting
const val AboutScreen_DescriptionTestTag = TAG + "_Description"

@VisibleForTesting
const val AboutScreen_SupportTestTag = TAG + "_Support"

@VisibleForTesting
const val AboutScreen_BackgroundStoryHeaderTestTag = TAG + "_BackgroundStoryHeader"

@VisibleForTesting
const val AboutScreen_BackgroundStoryTestTag = TAG + "_BackgroundStory"

@VisibleForTesting
const val AboutScreen_ThirdPartyNoticeTestTag = TAG + "_ThirdPartyNotice"

@PreviewThemes
@Composable
fun PreviewAboutScreen() = PreviewAppTheme {
    val windowWidthSize = PreviewWindowSize.widthSizeClass
    AboutScreen(
        navType = NavType.from(windowWidthSize),
        windowWidthSize = windowWidthSize,
        navigateTo = {},
        openEmail = {},
    )
}
