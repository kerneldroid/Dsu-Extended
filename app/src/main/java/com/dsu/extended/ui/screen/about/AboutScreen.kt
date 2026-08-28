package com.dsu.extended.ui.screen.about

import android.widget.Toast
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest
import com.dsu.extended.R
import com.dsu.extended.ui.components.AppScaffold
import com.dsu.extended.ui.cards.updater.UpdaterCard
import com.dsu.extended.ui.components.PreferenceItem
import com.dsu.extended.ui.components.Title
import com.dsu.extended.ui.screen.Destinations
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ListItemShapes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GitHubAvatar(login: String) {
    val cookieShape = MaterialShapes.Cookie9Sided.toShape()
    var loaded by remember(login) { mutableStateOf(false) }
    var failed by remember(login) { mutableStateOf(false) }
    val shimmerAngle by rememberInfiniteTransition(label = "avatarShimmer").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(1600, easing = LinearEasing)),
        label = "avatarShimmerAngle",
    )
    val outlineColor = MaterialTheme.colorScheme.primary
    val outlineColorAlt = MaterialTheme.colorScheme.tertiary
    Box(modifier = Modifier.size(40.dp)) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data("https://github.com/$login.png")
                .crossfade(240)
                .build(),
            contentDescription = login,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(cookieShape),
            onState = { state ->
                loaded = state is AsyncImagePainter.State.Success
                failed = state is AsyncImagePainter.State.Error
            },
        )
        if (!loaded) {
            Spacer(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        val outline = cookieShape.createOutline(size, layoutDirection, this)
                        if (!failed) {
                            rotate(shimmerAngle) {
                                drawOutline(
                                    outline = outline,
                                    brush = Brush.sweepGradient(
                                        listOf(outlineColor, outlineColorAlt, outlineColor.copy(alpha = 0.15f), outlineColor),
                                    ),
                                    style = Stroke(width = 2.dp.toPx()),
                                )
                            }
                        } else {
                            drawOutline(
                                outline = outline,
                                color = outlineColor.copy(alpha = 0.4f),
                                style = Stroke(width = 2.dp.toPx()),
                            )
                        }
                    },
            )
        }
    }
}

private data class Collaborator(
    val login: String,
    val title: String,
    val description: String,
    val url: String?,
)

private const val GITHUB_MARK_PATH = "M8 0C3.58 0 0 3.58 0 8C0 11.54 2.29 14.53 5.47 15.59C5.87 15.66 6.02 15.42 6.02 15.21C6.02 15.02 6.01 14.39 6.01 13.72C4 14.09 3.48 13.23 3.32 12.78C3.23 12.55 2.84 11.84 2.5 11.65C2.22 11.5 1.82 11.13 2.49 11.12C3.12 11.11 3.57 11.7 3.72 11.94C4.44 13.15 5.59 12.81 6.05 12.6C6.12 12.08 6.33 11.73 6.56 11.53C4.78 11.33 2.92 10.64 2.92 7.58C2.92 6.71 3.23 5.99 3.74 5.43C3.66 5.23 3.38 4.41 3.82 3.31C3.82 3.31 4.49 3.1 6.02 4.13C6.66 3.95 7.34 3.86 8.02 3.86C8.7 3.86 9.38 3.95 10.02 4.13C11.55 3.09 12.22 3.31 12.22 3.31C12.66 4.41 12.38 5.23 12.3 5.43C12.81 5.99 13.12 6.7 13.12 7.58C13.12 10.65 11.25 11.33 9.47 11.53C9.76 11.78 10.01 12.26 10.01 13.01C10.01 14.08 10 14.94 10 15.21C10 15.42 10.15 15.67 10.55 15.59C13.71 14.53 16 11.53 16 8C16 3.58 12.42 0 8 0Z"

private val GithubMarkIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "GithubMark",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 16f,
        viewportHeight = 16f,
    ).apply {
        addPath(
            pathData = PathParser().parsePathString(GITHUB_MARK_PATH).toNodes(),
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.EvenOdd,
        )
    }.build()
}

object AboutLinks {
    const val CONTRIBUTORS_URL = "https://github.com/kerneldroid/Dsu-Extended/graphs/contributors"
    const val REPOSITORY_URL = "https://github.com/kerneldroid/Dsu-Extended"
    const val WSTXDA_GITHUB = "https://github.com/WSTxda"
    const val VEGABOBO_GITHUB = "https://github.com/VegaBobo"
    const val KERNELDROID_GITHUB = "https://github.com/kerneldroid"
    const val SENODROID_GITHUB = "https://github.com/senodroid"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navigate: (String) -> Unit,
    aboutViewModel: AboutViewModel = hiltViewModel(),
) {
    val uiState by aboutViewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        aboutViewModel.resetDeveloperOptionsCounter()
        uiState.toastDisplay.collectLatest {
            when (it) {
                DevOptToastDisplay.ENABLED_DEV_OPT ->
                    Toast.makeText(
                        context,
                        context.getString(R.string.developer_options_enabled),
                        Toast.LENGTH_LONG,
                    ).show()

                DevOptToastDisplay.DISABLED_DEV_OPT ->
                    Toast.makeText(
                        context,
                        context.getString(R.string.developer_options_disabled),
                        Toast.LENGTH_LONG,
                    ).show()

                else -> {}
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    AppScaffold(
        title = { Text(text = stringResource(id = R.string.about), style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            IconButton(onClick = { navigate(Destinations.Up) }) {
                Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
            }
        },
        scrollBehavior = scrollBehavior,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(start = 12.dp, end = 12.dp, top = 10.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
        UpdaterCard(
            uiState = uiState.updaterCardState,
            isUpdaterAvailable = uiState.isUpdaterAvailable,
            onClickImage = { aboutViewModel.onClickImage() },
            onClickCheckUpdates = { aboutViewModel.onClickCheckUpdates() },
            onClickDownloadUpdate = { aboutViewModel.onClickDownloadUpdate() },
            onClickViewChangelog = { uriHandler.openUri(aboutViewModel.response.changelogUrl) },
        )
        Title(
            stringResource(id = R.string.application),
            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
        )

        val applicationItems = buildList<@Composable (ListItemShapes) -> Unit> {
            add { shapes ->
                PreferenceItem(
                    shapes = shapes,
                    title = stringResource(id = R.string.github_repo),
                    description = stringResource(id = R.string.github_repo_description),
                    icon = GithubMarkIcon,
                    onClick = { uriHandler.openUri(AboutLinks.REPOSITORY_URL) },
                )
            }
            add { shapes ->
                PreferenceItem(
                    shapes = shapes,
                    title = stringResource(id = R.string.libraries_title),
                    description = stringResource(id = R.string.libraries_description),
                    icon = Icons.Rounded.MenuBook,
                    onClick = { navigate(Destinations.Libraries) },
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            applicationItems.forEachIndexed { index, item ->
                item(ListItemDefaults.segmentedShapes(index = index, count = applicationItems.size))
            }
        }

        Title(
            stringResource(id = R.string.collaborators),
            modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
        )

        val translators = stringResource(id = R.string.translators_list)
        val collaboratorItems = listOf(
            Collaborator("kerneldroid", "kerneldroid", "Main Developer", AboutLinks.KERNELDROID_GITHUB),
            Collaborator("senodroid", "senodroid", "Original Fork Author", AboutLinks.SENODROID_GITHUB),
            Collaborator("yangFenTuoZi", "yangFenTuoZi", "DSU-Sideloader-Plus author", "https://github.com/yangFenTuoZi"),
            Collaborator("VegaBobo", "VegaBobo", stringResource(id = R.string.role_developer), AboutLinks.VEGABOBO_GITHUB),
            Collaborator("WSTxda", "WSTxda", stringResource(id = R.string.role_design_icon), AboutLinks.WSTXDA_GITHUB),
        ) + buildList {
            if (translators.isNotEmpty() && translators != "translators_list") {
                add(Collaborator("", stringResource(id = R.string.translators_title), translators, null))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            collaboratorItems.forEachIndexed { index, collaborator ->
                val onClick = collaborator.url?.let { url -> { uriHandler.openUri(url) } }
                SegmentedListItem(
                    onClick = { onClick?.invoke() },
                    enabled = onClick != null,
                    shapes = ListItemDefaults.segmentedShapes(index = index, count = collaboratorItems.size),
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    leadingContent = {
                        if (collaborator.login.isNotEmpty()) {
                            GitHubAvatar(collaborator.login)
                        } else {
                            Icon(
                                imageVector = if (collaborator.title == stringResource(id = R.string.contributors_title)) {
                                    Icons.Rounded.People
                                } else {
                                    Icons.Rounded.Info
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    content = {
                        Column {
                            Text(
                                text = collaborator.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = collaborator.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    trailingContent = if (onClick != null) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.OpenInNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }
}
}
