package com.dsu.extended.ui.screen.adb

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.dsu.extended.R
import com.dsu.extended.ui.cards.CopyableTextCard
import com.dsu.extended.ui.screen.Destinations
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import com.dsu.extended.ui.components.AppScaffold
import androidx.compose.material3.Icon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdbScreen(
    navigate: (String) -> Unit,
    adbViewModel: AdbViewModel = hiltViewModel(),
) {
    val scriptPath = adbViewModel.obtainScriptPath()

    val startInstallationCommand = "sh \"$scriptPath\""
    val startInstallationCommandAdb = "adb shell $startInstallationCommand"
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    AppScaffold(
        title = { Text(text = stringResource(id = R.string.installation), style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            IconButton(onClick = { navigate(Destinations.Up) }) {
                Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
            }
        },
        actions = {
            IconButton(onClick = { navigate(Destinations.Preferences) }) {
                Icon(imageVector = Icons.Rounded.Settings, contentDescription = null)
            }
        },
        scrollBehavior = scrollBehavior,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(start = 18.dp, end = 18.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(text = stringResource(id = R.string.adb_how_to_adb_shell))
            CopyableTextCard(text = startInstallationCommandAdb)
            Text(text = stringResource(id = R.string.adb_how_to_shell))
            CopyableTextCard(text = startInstallationCommand)
            Text(text = stringResource(id = R.string.adb_how_to_done))
        }
    }
}
