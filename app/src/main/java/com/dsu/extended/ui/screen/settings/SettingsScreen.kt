package com.dsu.extended.ui.screen.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.dsu.extended.R
import com.dsu.extended.ui.components.AppScaffold
import com.dsu.extended.ui.components.PreferenceItem
import com.dsu.extended.ui.screen.Destinations
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.Icon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Settings(
    navigate: (String) -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    AppScaffold(
        title = { Text(text = stringResource(id = R.string.settings), style = MaterialTheme.typography.titleLarge) },
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
                .padding(horizontal = 12.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
        val installationItems = buildList<@Composable () -> Unit> {
            add {
                PreferenceItem(
                    title = stringResource(id = R.string.builtin_installer),
                    description = stringResource(id = R.string.builtin_installer_description),
                    showToggle = true,
                    isChecked = uiState.useBuiltInInstaller,
                    onClick = { settingsViewModel.setUseBuiltInInstaller(!it) },
                )
            }
            add {
                PreferenceItem(
                    title = stringResource(id = R.string.unmount_sd_title),
                    description = stringResource(id = R.string.unmount_sd_description),
                    showToggle = true,
                    isChecked = uiState.unmountSdCard,
                    onClick = { settingsViewModel.setUnmountSdCard(!it) },
                )
            }
            add {
                PreferenceItem(
                    title = stringResource(id = R.string.storage_check_title),
                    description = stringResource(id = R.string.storage_check_description),
                    showToggle = true,
                    isChecked = uiState.disableStorageCheck,
                    onClick = { settingsViewModel.setDisableStorageCheck(!it) },
                )
            }
        }

        val developerItems = buildList<@Composable () -> Unit> {
            if (uiState.isDeveloperOptionsEnabled) {
                add {
                    PreferenceItem(
                        title = stringResource(id = R.string.full_logcat_logging_title),
                        description = stringResource(id = R.string.full_logcat_logging_description),
                        showToggle = true,
                        isChecked = uiState.fullLogcatLogging,
                        onClick = { settingsViewModel.setFullLogcatLogging(!it) },
                    )
                }
                add {
                    PreferenceItem(
                        title = stringResource(id = R.string.keep_screen_on),
                        showToggle = true,
                        isChecked = uiState.keepScreenOn,
                        onClick = { settingsViewModel.setKeepScreenOn(!it) },
                    )
                }
            }
        }

        SettingsContentExpressive(
            uiState = uiState,
            settingsViewModel = settingsViewModel,
            installationItems = installationItems,
            developerItems = developerItems,
            checkAllStatusRow = {
                PreferenceItem(
                    title = stringResource(id = R.string.check_all_title),
                    description = settingsViewModel.checkAllStatusSummary(),
                    icon = Icons.Rounded.Info,
                    onClick = { settingsViewModel.runCheckAll() },
                )
            },
            aboutItem = {
                PreferenceItem(
                    title = stringResource(id = R.string.about),
                    description = stringResource(id = R.string.about_description),
                    icon = Icons.Rounded.Info,
                    onClick = { navigate(Destinations.About) },
                )
            },
            onOpenDialog = { settingsViewModel.openDialog(it) },
        )
    }

    // Dialogs
    AnimatedContent(
        targetState = uiState.dialogSheetState,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "settingsDialogs",
    ) { state ->
        when (state) {
            DialogSheetState.OPERATION_MODE_SELECTOR ->
                OperationModeSelectorExpressiveMenu(
                    selectedMode = uiState.preferredPrivilegedMode,
                    onDismiss = { settingsViewModel.dismissDialog() },
                    onSelectMode = { settingsViewModel.setPreferredPrivilegedMode(it) },
                )

            DialogSheetState.THEME_MODE_SELECTOR ->
                ThemeModeSelectorExpressiveMenu(
                    selectedMode = uiState.themeMode,
                    onDismiss = { settingsViewModel.dismissDialog() },
                    onSelectMode = { settingsViewModel.setThemeMode(it) },
                )

            DialogSheetState.FONT_SELECTOR ->
                FontPresetSelectorExpressiveMenu(
                    selectedPreset = uiState.appFontPreset,
                    onDismiss = { settingsViewModel.dismissDialog() },
                    onSelectPreset = { settingsViewModel.setAppFontPreset(it) },
                )

            DialogSheetState.COLOR_STYLE_SELECTOR ->
                ColorStyleSelectorExpressiveMenu(
                    selectedStyle = uiState.colorPaletteStyle,
                    onDismiss = { settingsViewModel.dismissDialog() },
                    onSelectStyle = { settingsViewModel.setColorPaletteStyle(it) },
                )

            else -> {}
        }
        }
    }
}
