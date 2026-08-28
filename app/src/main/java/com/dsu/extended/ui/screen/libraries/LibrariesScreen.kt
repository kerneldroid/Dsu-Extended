package com.dsu.extended.ui.screen.libraries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.util.withContext
import java.util.Locale
import com.dsu.extended.R
import com.dsu.extended.ui.components.AppScaffold
import com.dsu.extended.ui.components.PreferenceItem
import com.dsu.extended.ui.screen.Destinations
import com.dsu.extended.util.AppLogger
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon

private data class LibraryEntry(
    val name: String,
    val licenses: String,
    val website: String? = null,
)

private val fallbackLibraries = listOf(
    LibraryEntry("AndroidX Compose", "Apache-2.0", "https://developer.android.com/jetpack/compose"),
    LibraryEntry("Material 3", "Apache-2.0", "https://m3.material.io"),
    LibraryEntry("Navigation Compose", "Apache-2.0", "https://developer.android.com/jetpack/compose/navigation"),
    LibraryEntry("DataStore", "Apache-2.0", "https://developer.android.com/topic/libraries/architecture/datastore"),
    LibraryEntry("Dagger Hilt", "Apache-2.0", "https://dagger.dev/hilt"),
    LibraryEntry("Kotlin Serialization", "Apache-2.0", "https://github.com/Kotlin/kotlinx.serialization"),
    LibraryEntry("libsu", "Apache-2.0", "https://github.com/topjohnwu/libsu"),
    LibraryEntry("Shizuku", "Apache-2.0", "https://github.com/RikkaApps/Shizuku"),
    LibraryEntry("Dhizuku API", "Apache-2.0", "https://github.com/iamr0s/Dhizuku-API"),
    LibraryEntry("AboutLibraries", "Apache-2.0", "https://github.com/mikepenz/AboutLibraries"),
    LibraryEntry("HiddenApiBypass", "Apache-2.0", "https://github.com/LSPosed/AndroidHiddenApiBypass"),
    LibraryEntry("XZ for Java", "Public Domain", "https://tukaani.org/xz/java.html"),
    LibraryEntry("Apache Commons Compress", "Apache-2.0", "https://commons.apache.org/proper/commons-compress/"),
)

private fun normalizeLibraryEntry(entry: LibraryEntry): LibraryEntry {
    val normalizedName = entry.name.trim()
    val normalizedWebsite = entry.website?.trim()
    return entry.copy(name = normalizedName, website = normalizedWebsite)
}

private fun mergeLibraries(
    generatedLibraries: List<LibraryEntry>,
    fallbackLibraries: List<LibraryEntry>,
): List<LibraryEntry> {
    val mergedByName = linkedMapOf<String, LibraryEntry>()
    (fallbackLibraries + generatedLibraries)
        .map(::normalizeLibraryEntry)
        .forEach { item ->
            val key = item.name.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "")
            val existing = mergedByName[key]
            if (existing == null) {
                mergedByName[key] = item
            } else {
                mergedByName[key] = existing.copy(
                    licenses = existing.licenses.ifBlank { item.licenses },
                    website = existing.website?.takeIf { it.isNotBlank() } ?: item.website,
                )
            }
        }
    return mergedByName.values.toList()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrariesScreen(
    navigate: (String) -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val libraries = remember(context) {
        val generatedLibraries =
            runCatching {
                Libs.Builder()
                    .withContext(context)
                    .build()
                    .libraries
                    .map { library ->
                        LibraryEntry(
                            name = library.name,
                            licenses = library.licenses.joinToString(", ") { it.name },
                            website = library.website,
                        )
                    }
                    .filter { it.name.isNotBlank() }
            }.onFailure {
                AppLogger.e("LibrariesScreen", "Failed to parse AboutLibraries metadata", it)
            }.getOrDefault(emptyList())

        mergeLibraries(generatedLibraries, fallbackLibraries)
    }

    val appBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(appBarState)

    AppScaffold(
        title = { Text(text = stringResource(id = R.string.libraries_title), style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            IconButton(onClick = { navigate(Destinations.Up) }) {
                Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
            }
        },
        scrollBehavior = scrollBehavior,
    ) {
        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(libraries.size) { index ->
                val library = libraries[index]
                PreferenceItem(
                    shapes = ListItemDefaults.segmentedShapes(index = index, count = libraries.size),
                    title = library.name,
                        description = library.licenses,
                        icon = Icons.Rounded.Description,
                        onClick = {
                            val url = library.website
                            if (!url.isNullOrBlank()) {
                                runCatching { uriHandler.openUri(url) }
                                    .onFailure {
                                        AppLogger.w(
                                            "LibrariesScreen",
                                            "Failed to open library url",
                                            "url" to url,
                                            "error" to (it.message ?: "unknown"),
                                        )
                                    }
                            }
                        },
                )
            }
            item { Spacer(modifier = Modifier.padding(2.dp)) }
        }
    }
}
