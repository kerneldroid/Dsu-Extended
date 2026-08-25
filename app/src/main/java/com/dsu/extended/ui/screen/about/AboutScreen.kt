package com.dsu.extended.ui.screen.about

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.dsu.extended.ui.components.DynamicListItem
import com.dsu.extended.ui.cards.updater.UpdaterCard
import com.dsu.extended.ui.components.PreferenceItem
import com.dsu.extended.ui.components.Title
import com.dsu.extended.ui.screen.Destinations
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon

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

        val applicationItems = buildList<@Composable () -> Unit> {
            add {
                PreferenceItem(
                    title = stringResource(id = R.string.github_repo),
                    description = stringResource(id = R.string.github_repo_description),
                    icon = Icons.Rounded.Description,
                    onClick = { uriHandler.openUri(AboutLinks.REPOSITORY_URL) },
                )
            }
            add {
                PreferenceItem(
                    title = stringResource(id = R.string.libraries_title),
                    description = stringResource(id = R.string.libraries_description),
                    icon = Icons.Rounded.Settings,
                    onClick = { navigate(Destinations.Libraries) },
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            applicationItems.forEachIndexed { index, item ->
                DynamicListItem(listLength = applicationItems.lastIndex, currentValue = index) {
                    item()
                }
            }
        }

        Title(
            stringResource(id = R.string.collaborators),
            modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
        )

        val collaboratorItems = buildList<@Composable () -> Unit> {
            add {
                PreferenceItem(
                    title = "kerneldroid",
                    description = "Main Developer",
                    icon = Icons.Rounded.NewReleases,
                    onClick = { uriHandler.openUri(AboutLinks.KERNELDROID_GITHUB) },
                )
            }
            add {
                PreferenceItem(
                    title = "senodroid",
                    description = "Original Fork Author",
                    icon = Icons.Rounded.Description,
                    onClick = { uriHandler.openUri(AboutLinks.SENODROID_GITHUB) },
                )
            }
            add {
                PreferenceItem(
                    title = "VegaBobo",
                    description = stringResource(id = R.string.role_developer),
                    icon = Icons.Rounded.NewReleases,
                    onClick = { uriHandler.openUri(AboutLinks.VEGABOBO_GITHUB) },
                )
            }
            add {
                PreferenceItem(
                    title = "WSTxda",
                    description = stringResource(id = R.string.role_design_icon),
                    icon = Icons.Rounded.Description,
                    onClick = { uriHandler.openUri(AboutLinks.WSTXDA_GITHUB) },
                )
            }
            val translators = stringResource(id = R.string.translators_list)
            if (translators.isNotEmpty() && translators != "translators_list") {
                add {
                    PreferenceItem(
                        title = stringResource(id = R.string.translators_title),
                        description = stringResource(id = R.string.translators_list),
                        icon = Icons.Rounded.Settings,
                    )
                }
            }
            add {
                PreferenceItem(
                    title = stringResource(id = R.string.contributors_title),
                    description = stringResource(id = R.string.contributors_text),
                    icon = Icons.Rounded.Description,
                    onClick = { uriHandler.openUri(AboutLinks.CONTRIBUTORS_URL) },
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            collaboratorItems.forEachIndexed { index, item ->
                DynamicListItem(listLength = collaboratorItems.lastIndex, currentValue = index) {
                    item()
                }
            }
        }
        }
    }
}
