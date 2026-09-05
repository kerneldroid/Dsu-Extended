package com.dsu.extended

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.core.view.WindowCompat
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dsu.extended.ui.components.LocalContentMaxWidth
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener
import com.rosan.dhizuku.api.DhizukuUserServiceArgs
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider
import com.dsu.extended.model.Session
import com.dsu.extended.preferences.AppPrefs
import com.dsu.extended.service.Connection
import com.dsu.extended.service.PrivilegedProvider
import com.dsu.extended.service.PrivilegedRootService
import com.dsu.extended.service.PrivilegedService
import com.dsu.extended.service.PrivilegedSystemService
import com.dsu.extended.ui.screen.Navigation
import com.dsu.extended.ui.theme.AppFontPreset
import com.dsu.extended.ui.theme.ColorPaletteStyle
import com.dsu.extended.ui.theme.ColorSpecVersion
import com.dsu.extended.ui.theme.DsuExtendedTheme
import com.dsu.extended.ui.theme.ThemeMode
import com.dsu.extended.util.AppLogger
import com.dsu.extended.util.DataStoreUtils
import com.dsu.extended.util.OperationMode
import com.dsu.extended.util.OperationModeUtils
import com.dsu.extended.util.PreferredPrivilegedMode

@AndroidEntryPoint
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class MainActivity : ComponentActivity(), Shizuku.OnRequestPermissionResultListener {

    @Inject
    lateinit var session: Session

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    private val tag = this.javaClass.simpleName

    private var shouldCheckShizuku = false
    private var shouldCheckDhizuku = false
    private var activeServiceMode: OperationMode? = null
    private var checkAllPendingCallback: (() -> Unit)? = null
    private var checkAllAwaitingShizukuPermission = false
    private var checkAllAwaitingDhizukuPermission = false

    private suspend fun resolvePreferredPrivilegedMode(): PreferredPrivilegedMode {
        val preferenceValue =
            DataStoreUtils.readStringPref(
                dataStore,
                AppPrefs.OPERATION_MODE_OVERRIDE,
                PreferredPrivilegedMode.ALL.value,
            )
        return PreferredPrivilegedMode.fromPreference(preferenceValue)
    }

    private suspend fun setupSessionOperationMode() {
        withContext(Dispatchers.IO) {
            val preferredMode = resolvePreferredPrivilegedMode()
        if (preferredMode == PreferredPrivilegedMode.SHIZUKU || preferredMode == PreferredPrivilegedMode.ALL) {
            shouldCheckShizuku = true
        }
        if (preferredMode == PreferredPrivilegedMode.DHIZUKU || preferredMode == PreferredPrivilegedMode.ALL) {
            shouldCheckDhizuku = true
        }
        val operationMode =
            OperationModeUtils.getOperationMode(
                context = application,
                checkShizuku = shouldCheckShizuku,
                checkDhizuku = shouldCheckDhizuku,
                preferredPrivilegedMode = preferredMode,
            )
        session.setOperationMode(operationMode)
            AppLogger.i(
                tag,
                "Operation mode resolved",
                "mode" to operationMode,
                "checkShizuku" to shouldCheckShizuku,
                "checkDhizuku" to shouldCheckDhizuku,
                "preferredMode" to preferredMode,
            )
        }
    }

    private fun finishCheckAllIfReady() {
        if (checkAllAwaitingShizukuPermission || checkAllAwaitingDhizukuPermission) {
            return
        }
        val callback = checkAllPendingCallback ?: return
        checkAllPendingCallback = null
        lifecycleScope.launch {
            refreshOperationModeAndService()
            runOnUiThread {
                callback()
            }
        }
    }

    fun requestPermissionsForCheckAll(onComplete: () -> Unit = {}) {
        lifecycleScope.launch {
            val hasRoot = withContext(Dispatchers.IO) {
                runCatching { Shell.getShell().isRoot }.getOrDefault(false)
            }
            val shizukuNeedsPermission =
                Shizuku.pingBinder() && !OperationModeUtils.isShizukuPermissionGranted(this@MainActivity)
            val dhizukuNeedsPermission =
                runCatching { Dhizuku.init(this@MainActivity) && !Dhizuku.isPermissionGranted() }
                    .getOrDefault(false)
            AppLogger.d(
                tag,
                "Check ALL requested",
                "root" to hasRoot,
                "shizukuNeedsPermission" to shizukuNeedsPermission,
                "dhizukuNeedsPermission" to dhizukuNeedsPermission,
            )

            if (!shizukuNeedsPermission && !dhizukuNeedsPermission) {
                refreshOperationModeAndService()
                onComplete()
                return@launch
            }

            checkAllPendingCallback = onComplete
            checkAllAwaitingShizukuPermission = shizukuNeedsPermission
            checkAllAwaitingDhizukuPermission = dhizukuNeedsPermission

            if (shizukuNeedsPermission) {
                runCatching { Shizuku.removeRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER) }
                Shizuku.addRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER)
                askShizukuPermission()
            }

            if (dhizukuNeedsPermission) {
                askDhizukuPermission()
            }

            finishCheckAllIfReady()
        }
    }

    private suspend fun refreshOperationModeAndService() {
        val previousMode = session.getOperationMode()
        setupSessionOperationMode()
        val currentMode = session.getOperationMode()
        rebindServiceIfNeeded(previousMode, currentMode)
    }

    val userServiceArgs =
        Shizuku.UserServiceArgs(
            ComponentName(BuildConfig.APPLICATION_ID, PrivilegedService::class.java.name),
        )
            .daemon(false)
            .processNameSuffix("service")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)

    val dhizukuUserServiceArgs =
        DhizukuUserServiceArgs(
            ComponentName(BuildConfig.APPLICATION_ID, PrivilegedService::class.java.name),
        )

    private val SHIZUKU_REQUEST_CODE = 1000
    private val REQUEST_PERMISSION_RESULT_LISTENER = this::onRequestPermissionResult

    // Runtime permission request (notification permission).
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            if (checkAllAwaitingShizukuPermission) {
                checkAllAwaitingShizukuPermission = false
                finishCheckAllIfReady()
            }
        }

    private fun addShizukuListeners() {
        Shizuku.addBinderReceivedListenerSticky(BINDER_RECEIVED_LISTENER)
        Shizuku.addRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER)
    }

    private fun removeShizukuListeners() {
        Shizuku.removeRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER)
        Shizuku.removeBinderReceivedListener(BINDER_RECEIVED_LISTENER)
    }

    private val BINDER_RECEIVED_LISTENER = Shizuku.OnBinderReceivedListener {
        if (OperationModeUtils.isShizukuPermissionGranted(this)) {
            bindShizuku()
        } else {
            AppLogger.d(tag, "Shizuku binder received without permission; waiting for explicit grant")
        }
    }

    private fun askShizukuPermission() {
        Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
    }

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        AppLogger.d(
            tag,
            "Shizuku permission callback",
            "requestCode" to requestCode,
            "grantResult" to grantResult,
        )
        if (grantResult == PackageManager.PERMISSION_GRANTED && requestCode == SHIZUKU_REQUEST_CODE) {
            bindShizuku()
        }
        Shizuku.removeRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER)
        if (requestCode == SHIZUKU_REQUEST_CODE && checkAllAwaitingShizukuPermission) {
            checkAllAwaitingShizukuPermission = false
            finishCheckAllIfReady()
        }
    }

    fun bindShizuku() {
        AppLogger.i(tag, "Binding Shizuku user service")
        Shizuku.bindUserService(userServiceArgs, PrivilegedProvider.connection)
        shouldCheckShizuku = true
        lifecycleScope.launch {
            setupSessionOperationMode()
            activeServiceMode = session.getOperationMode()
        }
    }

    private fun askDhizukuPermission() {
        if (!Dhizuku.init(this)) {
            AppLogger.w(tag, "Dhizuku is not initialized, cannot request permission")
            checkAllAwaitingDhizukuPermission = false
            finishCheckAllIfReady()
            return
        }
        runCatching {
            Dhizuku.requestPermission(
                object : DhizukuRequestPermissionListener() {
                    override fun onRequestPermission(grantResult: Int) {
                        AppLogger.d(tag, "Dhizuku permission callback", "grantResult" to grantResult)
                        if (grantResult == PackageManager.PERMISSION_GRANTED) {
                            runOnUiThread {
                                bindDhizuku()
                            }
                        }
                        checkAllAwaitingDhizukuPermission = false
                        finishCheckAllIfReady()
                    }
                },
            )
        }.onFailure {
            AppLogger.w(tag, "Failed to request Dhizuku permission", "error" to it.message)
            checkAllAwaitingDhizukuPermission = false
            finishCheckAllIfReady()
        }
    }

    fun bindDhizuku() {
        if (!Dhizuku.init(this)) {
            AppLogger.w(tag, "Dhizuku not active or unavailable")
            return
        }
        if (!Dhizuku.isPermissionGranted()) {
            AppLogger.i(tag, "Dhizuku permission missing, requesting")
            askDhizukuPermission()
            return
        }
        AppLogger.i(tag, "Binding Dhizuku user service")
        val bound = Dhizuku.bindUserService(dhizukuUserServiceArgs, PrivilegedProvider.connection)
        if (!bound) {
            AppLogger.w(tag, "Failed to bind Dhizuku user service")
            return
        }
        shouldCheckDhizuku = true
        lifecycleScope.launch {
            setupSessionOperationMode()
            activeServiceMode = session.getOperationMode()
        }
    }

    companion object {
        init {
            Shell.enableLegacyStderrRedirection = true
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setTimeout(10),
            )
        }
    }

    private suspend fun setupService() {
        val isRoot = withContext(Dispatchers.IO) { session.isRoot() }
        if (isRoot) {
            AppLogger.i(tag, "Binding root service")
            val privRootService = Intent(this, PrivilegedRootService::class.java)
            RootService.bind(privRootService, PrivilegedProvider.connection)
            activeServiceMode = session.getOperationMode()
            return
        }

        if (session.getOperationMode() == OperationMode.SYSTEM) {
            AppLogger.i(tag, "Binding system privileged service")
            val service = Intent(this, PrivilegedSystemService::class.java)
            bindService(service, PrivilegedProvider.connection, Context.BIND_AUTO_CREATE)
            activeServiceMode = session.getOperationMode()
            return
        }

        if (session.getOperationMode() == OperationMode.DHIZUKU) {
            AppLogger.i(tag, "Trying Dhizuku privileged service")
            bindDhizuku()
            activeServiceMode = session.getOperationMode()
            return
        }

        if (resolvePreferredPrivilegedMode() == PreferredPrivilegedMode.DHIZUKU) {
            if (Dhizuku.init(this) && !Dhizuku.isPermissionGranted()) {
                AppLogger.i(tag, "Dhizuku available but permission not granted")
                askDhizukuPermission()
                activeServiceMode = session.getOperationMode()
                return
            }
        }

        // Only add Shizuku listeners if we are not already in a superior mode
        AppLogger.i(tag, "Checking for Shizuku binder")
        if (OperationModeUtils.isShizukuPermissionGranted(this)) {
            bindShizuku()
        } else {
            addShizukuListeners()
        }
        activeServiceMode = session.getOperationMode()
    }

    private fun unbindForMode(mode: OperationMode?) {
        when (mode) {
            OperationMode.ROOT, OperationMode.SYSTEM_AND_ROOT -> {
                runCatching { RootService.unbind(PrivilegedProvider.connection) }
                    .onFailure { AppLogger.w(tag, "Failed to unbind root service", "error" to it.message) }
            }

            OperationMode.SYSTEM -> {
                runCatching { applicationContext.unbindService(PrivilegedProvider.connection) }
                    .onFailure { AppLogger.w(tag, "Failed to unbind system service", "error" to it.message) }
            }

            OperationMode.SHIZUKU -> {
                runCatching {
                    removeShizukuListeners()
                    Shizuku.unbindUserService(userServiceArgs, PrivilegedProvider.connection, true)
                }.onFailure { AppLogger.w(tag, "Failed to unbind shizuku service", "error" to it.message) }
            }

            OperationMode.DHIZUKU -> {
                runCatching {
                    removeShizukuListeners()
                    Dhizuku.stopUserService(dhizukuUserServiceArgs)
                    Dhizuku.unbindUserService(PrivilegedProvider.connection)
                }.onFailure { AppLogger.w(tag, "Failed to unbind dhizuku service", "error" to it.message) }
            }

            OperationMode.ADB -> {
                runCatching { removeShizukuListeners() }
                    .onFailure { AppLogger.w(tag, "Failed to remove shizuku listeners", "error" to it.message) }
            }

            else -> {}
        }
    }

    private suspend fun rebindServiceIfNeeded(previousMode: OperationMode, currentMode: OperationMode) {
        if (previousMode == currentMode) {
            return
        }
        AppLogger.i(
            tag,
            "Operation mode changed, rebinding service",
            "previousMode" to previousMode,
            "currentMode" to currentMode,
        )
        unbindForMode(previousMode)
        PrivilegedProvider.connection = Connection()
        setupService()
    }

    private fun observeModePreferenceChanges() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                dataStore.data
                    .map { preferences ->
                        preferences[stringPreferencesKey(AppPrefs.OPERATION_MODE_OVERRIDE)] ?: PreferredPrivilegedMode.ALL.value
                    }
                    .distinctUntilChanged()
                    .collect { prefValue ->
                        val preferredMode = PreferredPrivilegedMode.fromPreference(prefValue)
                        if (preferredMode == PreferredPrivilegedMode.SHIZUKU || preferredMode == PreferredPrivilegedMode.ALL) {
                            shouldCheckShizuku = true
                        }
                        if (preferredMode == PreferredPrivilegedMode.DHIZUKU || preferredMode == PreferredPrivilegedMode.ALL) {
                            shouldCheckDhizuku = true
                        }
                        val previousMode = session.getOperationMode()
                        setupSessionOperationMode()
                        val currentMode = session.getOperationMode()
                        rebindServiceIfNeeded(previousMode, currentMode)
                    }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return
        }
        permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.printWatermarkOnce()
        Shell.getShell {}
        WindowCompat.setDecorFitsSystemWindows(window, false)
        AppLogger.i(tag, "MainActivity created", "savedState" to (savedInstanceState != null))

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val contentMaxWidth = when (windowSizeClass.widthSizeClass) {
                WindowWidthSizeClass.Medium -> 640.dp
                WindowWidthSizeClass.Expanded -> 840.dp
                else -> Dp.Unspecified
            }
            val preferences by dataStore.data.collectAsState(initial = emptyPreferences())
            val themeModePreference = preferences[stringPreferencesKey(AppPrefs.THEME_MODE)] ?: ThemeMode.SYSTEM.value
            val themeMode = ThemeMode.fromPreference(themeModePreference)
            val fontPresetPreference =
                preferences[stringPreferencesKey(AppPrefs.APP_FONT_PRESET)] ?: AppFontPreset.GOOGLE_SANS_FLEX.value
            val appFontPreset = AppFontPreset.fromPreference(fontPresetPreference)
            val colorPalettePreference =
                preferences[stringPreferencesKey(AppPrefs.MATERIAL_COLOR_STYLE)] ?: ColorPaletteStyle.TONAL_SPOT.value
            val colorPaletteStyle = ColorPaletteStyle.fromPreference(colorPalettePreference)
            val useDynamicColor = preferences[booleanPreferencesKey(AppPrefs.USE_DYNAMIC_COLOR)] ?: false
            val colorSpecPreference =
                preferences[stringPreferencesKey(AppPrefs.MATERIAL_COLOR_SPEC)] ?: ColorSpecVersion.SPEC_2021.value
            val colorSpecVersion = ColorSpecVersion.fromPreference(colorSpecPreference)
            DsuExtendedTheme(
                themeMode = themeMode,
                dynamicColor = useDynamicColor,
                colorPaletteStyle = colorPaletteStyle,
                colorSpecVersion = colorSpecVersion,
                appFontPreset = appFontPreset,
            ) {
                CompositionLocalProvider(LocalContentMaxWidth provides contentMaxWidth) {
                    Navigation()
                }
            }
        }

        if (savedInstanceState == null) {
            lifecycleScope.launch {
                setupSessionOperationMode()
                setupService()
            }
        }
        requestNotificationPermissionIfNeeded()
        observeModePreferenceChanges()
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
    }

    override fun onDestroy() {
        super.onDestroy()
        AppLogger.i(tag, "MainActivity destroyed", "changingConfig" to isChangingConfigurations)
        // Binder ownership is process-scoped on purpose: unbinding here would
        // drop RootService/Shizuku mid-install when the Activity dies while
        // DsuInstallService is still flashing. Unbind happens only on
        // privileged-mode switch (rebindServiceIfNeeded) or process death.
    }
}
