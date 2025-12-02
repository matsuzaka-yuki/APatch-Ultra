package me.bmax.apatch.ui.screen

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.RemoveFromQueue
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Translate

import androidx.compose.ui.res.painterResource
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.BuildConfig
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.SwitchItem
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.ui.theme.BackgroundManager
import me.bmax.apatch.ui.theme.refreshTheme
import me.bmax.apatch.util.APatchKeyHelper
import me.bmax.apatch.util.PermissionRequestHandler
import me.bmax.apatch.util.PermissionUtils
import me.bmax.apatch.util.getBugreportFile
import me.bmax.apatch.util.isForceUsingOverlayFS
import me.bmax.apatch.util.isGlobalNamespaceEnabled
import me.bmax.apatch.util.isLiteModeEnabled
import me.bmax.apatch.util.outputStream
import me.bmax.apatch.util.overlayFsAvailable
import me.bmax.apatch.util.rootShellForResult
import me.bmax.apatch.util.setForceUsingOverlayFS
import me.bmax.apatch.util.setGlobalNamespaceEnabled
import me.bmax.apatch.util.setLiteMode
import me.bmax.apatch.util.ui.APDialogBlurBehindUtils
import me.bmax.apatch.util.ui.LocalSnackbarHost
import me.bmax.apatch.util.ui.NavigationBarsSpacer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Destination<RootGraph>
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingScreen() {
    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    val kPatchReady = state != APApplication.State.UNKNOWN_STATE
    val aPatchReady =
        (state == APApplication.State.ANDROIDPATCH_INSTALLING || state == APApplication.State.ANDROIDPATCH_INSTALLED || state == APApplication.State.ANDROIDPATCH_NEED_UPDATE)
    var isGlobalNamespaceEnabled by rememberSaveable {
        mutableStateOf(false)
    }
    var isLiteModeEnabled by rememberSaveable {
        mutableStateOf(false)
    }
    var forceUsingOverlayFS by rememberSaveable {
        mutableStateOf(false)
    }
    var bSkipStoreSuperKey by rememberSaveable {
        mutableStateOf(APatchKeyHelper.shouldSkipStoreSuperKey())
    }
    val isOverlayFSAvailable by rememberSaveable {
        mutableStateOf(overlayFsAvailable())
    }
    if (kPatchReady && aPatchReady) {
        isGlobalNamespaceEnabled = isGlobalNamespaceEnabled()
        isLiteModeEnabled = isLiteModeEnabled()
        forceUsingOverlayFS = isForceUsingOverlayFS()
    }

    val snackBarHost = LocalSnackbarHost.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
            )
        },
        containerColor = if (BackgroundConfig.isCustomBackgroundEnabled) {
            Color.Transparent
        } else {
            MaterialTheme.colorScheme.background
        },
        snackbarHost = { SnackbarHost(snackBarHost) }
    ) { paddingValues ->

        val loadingDialog = rememberLoadingDialog()
        val clearKeyDialog = rememberConfirmDialog(
            onConfirm = {
                APatchKeyHelper.clearConfigKey()
                APApplication.superKey = ""
            }
        )

        val showLanguageDialog = rememberSaveable { mutableStateOf(false) }
        LanguageDialog(showLanguageDialog)

        val showResetSuPathDialog = remember { mutableStateOf(false) }
        if (showResetSuPathDialog.value) {
            ResetSUPathDialog(showResetSuPathDialog)
        }

        val showThemeChooseDialog = remember { mutableStateOf(false) }
        if (showThemeChooseDialog.value) {
            ThemeChooseDialog(showThemeChooseDialog)
        }

        val showIconChooseDialog = remember { mutableStateOf(false) }
        if (showIconChooseDialog.value) {
            IconChooseDialog(showIconChooseDialog)
        }

        val showAppTitleDialog = remember { mutableStateOf(false) }
        if (showAppTitleDialog.value) {
            AppTitleChooseDialog(showAppTitleDialog)
        }

        var showLogBottomSheet by remember { mutableStateOf(false) }

        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val logSavedMessage = stringResource(R.string.log_saved)
        val exportBugreportLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/gzip")
        ) { uri: Uri? ->
            if (uri != null) {
                scope.launch(Dispatchers.IO) {
                    loadingDialog.show()
                    uri.outputStream().use { output ->
                        getBugreportFile(context).inputStream().use {
                            it.copyTo(output)
                        }
                    }
                    loadingDialog.hide()
                    snackBarHost.showSnackbar(message = logSavedMessage)
                }
            }
        }

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {

            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val prefs = APApplication.sharedPreferences

            // clear key
            if (kPatchReady) {
                val clearKeyDialogTitle = stringResource(id = R.string.clear_super_key)
                val clearKeyDialogContent =
                    stringResource(id = R.string.settings_clear_super_key_dialog)
                ListItem(
                    leadingContent = {
                        Icon(
                            Icons.Filled.Key, stringResource(id = R.string.super_key)
                        )
                    },
                    headlineContent = { Text(stringResource(id = R.string.clear_super_key)) },
                    modifier = Modifier.clickable {
                        clearKeyDialog.showConfirm(
                            title = clearKeyDialogTitle,
                            content = clearKeyDialogContent,
                            markdown = false,
                        )

                    })
            }

            // store key local?
            SwitchItem(
                icon = Icons.Filled.Key,
                title = stringResource(id = R.string.settings_donot_store_superkey),
                summary = stringResource(id = R.string.settings_donot_store_superkey_summary),
                checked = bSkipStoreSuperKey,
                onCheckedChange = {
                    bSkipStoreSuperKey = it
                    APatchKeyHelper.setShouldSkipStoreSuperKey(bSkipStoreSuperKey)
                })

            // Global mount
            if (kPatchReady && aPatchReady) {
                SwitchItem(
                    icon = Icons.Filled.Engineering,
                    title = stringResource(id = R.string.settings_global_namespace_mode),
                    summary = stringResource(id = R.string.settings_global_namespace_mode_summary),
                    checked = isGlobalNamespaceEnabled,
                    onCheckedChange = {
                        setGlobalNamespaceEnabled(
                            if (isGlobalNamespaceEnabled) {
                                "0"
                            } else {
                                "1"
                            }
                        )
                        isGlobalNamespaceEnabled = it
                    })
            }

            // Lite Mode
            if (kPatchReady && aPatchReady) {
                SwitchItem(
                    icon = Icons.Filled.RemoveFromQueue,
                    title = stringResource(id = R.string.settings_lite_mode),
                    summary = stringResource(id = R.string.settings_lite_mode_mode_summary),
                    checked = isLiteModeEnabled,
                    onCheckedChange = {
                        setLiteMode(it)
                        isLiteModeEnabled = it
                    })
            }

            // Force OverlayFS
            if (kPatchReady && aPatchReady && isOverlayFSAvailable) {
                SwitchItem(
                    icon = Icons.Filled.FilePresent,
                    title = stringResource(id = R.string.settings_force_overlayfs_mode),
                    summary = stringResource(id = R.string.settings_force_overlayfs_mode_summary),
                    checked = forceUsingOverlayFS,
                    onCheckedChange = {
                        setForceUsingOverlayFS(it)
                        forceUsingOverlayFS = it
                    })
            }

            // WebView Debug
            if (aPatchReady) {
                var enableWebDebugging by rememberSaveable {
                    mutableStateOf(
                        prefs.getBoolean("enable_web_debugging", false)
                    )
                }
                SwitchItem(
                    icon = Icons.Filled.DeveloperMode,
                    title = stringResource(id = R.string.enable_web_debugging),
                    summary = stringResource(id = R.string.enable_web_debugging_summary),
                    checked = enableWebDebugging
                ) {
                    APApplication.sharedPreferences.edit {
                        putBoolean("enable_web_debugging", it)
                    }
                    enableWebDebugging = it
                }

                // Install Confirm
                var installConfirm by rememberSaveable {
                    mutableStateOf(
                        prefs.getBoolean("apm_install_confirm_enabled", true)
                    )
                }
                SwitchItem(
                    icon = Icons.Filled.Save, // Reusing Save icon or maybe Help/Info
                    title = stringResource(id = R.string.settings_apm_install_confirm),
                    summary = stringResource(id = R.string.settings_apm_install_confirm_summary),
                    checked = installConfirm
                ) {
                    prefs.edit { putBoolean("apm_install_confirm_enabled", it) }
                    installConfirm = it
                }
            }

            // Hide Learn APatch card
            var hideApatchCard by rememberSaveable {
                mutableStateOf(
                    prefs.getBoolean("hide_apatch_card", true) // 默认隐藏
                )
            }
            SwitchItem(
                icon = Icons.Filled.Info,
                title = stringResource(id = R.string.settings_hide_apatch_card),
                summary = stringResource(id = R.string.settings_hide_apatch_card_summary),
                checked = hideApatchCard
            ) {
                APApplication.sharedPreferences.edit {
                    putBoolean("hide_apatch_card", it)
                }
                hideApatchCard = it
            }



            // Night Mode Follow System
            var nightFollowSystem by rememberSaveable {
                mutableStateOf(
                    prefs.getBoolean("night_mode_follow_sys", true)
                )
            }
            SwitchItem(
                icon = Icons.Filled.InvertColors,
                title = stringResource(id = R.string.settings_night_mode_follow_sys),
                summary = stringResource(id = R.string.settings_night_mode_follow_sys_summary),
                checked = nightFollowSystem
            ) {
                prefs.edit { putBoolean("night_mode_follow_sys", it) }
                nightFollowSystem = it
                refreshTheme.value = true
            }

            // Custom Night Theme Switch
            if (!nightFollowSystem) {
                var nightThemeEnabled by rememberSaveable {
                    mutableStateOf(
                        prefs.getBoolean("night_mode_enabled", false)
                    )
                }
                SwitchItem(
                    icon = Icons.Filled.DarkMode,
                    title = stringResource(id = R.string.settings_night_theme_enabled),
                    checked = nightThemeEnabled
                ) {
                    prefs.edit { putBoolean("night_mode_enabled", it) }
                    nightThemeEnabled = it
                    refreshTheme.value = true
                }
            }

            // System dynamic color theme
            val isDynamicColorSupport = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            if (isDynamicColorSupport) {
                var useSystemDynamicColor by rememberSaveable {
                    mutableStateOf(
                        prefs.getBoolean("use_system_color_theme", true)
                    )
                }
                SwitchItem(
                    icon = Icons.Filled.ColorLens,
                    title = stringResource(id = R.string.settings_use_system_color_theme),
                    summary = stringResource(id = R.string.settings_use_system_color_theme_summary),
                    checked = useSystemDynamicColor
                ) {
                    prefs.edit { putBoolean("use_system_color_theme", it) }
                    useSystemDynamicColor = it
                    refreshTheme.value = true
                }

                if (!useSystemDynamicColor) {
                    ListItem(headlineContent = {
                        Text(text = stringResource(id = R.string.settings_custom_color_theme))
                    }, modifier = Modifier.clickable {
                        showThemeChooseDialog.value = true
                    }, supportingContent = {
                        val colorMode = prefs.getString("custom_color", "blue")
                        Text(
                            text = stringResource(colorNameToString(colorMode.toString())),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }, leadingContent = { Icon(Icons.Filled.FormatColorFill, null) })

                }
            } else {
                ListItem(headlineContent = {
                    Text(text = stringResource(id = R.string.settings_custom_color_theme))
                }, modifier = Modifier.clickable {
                    showThemeChooseDialog.value = true
                }, supportingContent = {
                    val colorMode = prefs.getString("custom_color", "blue")
                    Text(
                        text = stringResource(colorNameToString(colorMode.toString())),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }, leadingContent = { Icon(Icons.Filled.FormatColorFill, null) })
            }

            // App Title Settings
            ListItem(headlineContent = {
                Text(text = stringResource(id = R.string.settings_app_title))
            }, modifier = Modifier.clickable {
                showAppTitleDialog.value = true
            }, supportingContent = {
                val currentTitle = prefs.getString("app_title", "folkpatch")
                Text(
                    text = stringResource(appTitleNameToString(currentTitle.toString())),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }, leadingContent = { Icon(Icons.Filled.Label, null) })

            ListItem(headlineContent = {
                Text(text = stringResource(id = R.string.settings_launcher_icon))
            }, modifier = Modifier.clickable {
                showIconChooseDialog.value = true
            }, supportingContent = {
                val currentIcon = prefs.getString("launcher_icon_variant", "default")
                Text(
                    text = stringResource(iconNameToString(currentIcon.toString())),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }, leadingContent = { Icon(painterResource(id = R.drawable.settings), null) })

            // Custom Background Settings
            var isCustomBackgroundEnabled by rememberSaveable {
                mutableStateOf(
                    BackgroundConfig.isCustomBackgroundEnabled
                )
            }
            var customBackgroundUri by rememberSaveable {
                mutableStateOf(
                    BackgroundConfig.customBackgroundUri ?: ""
                )
            }
            
            // Custom background toggle
            SwitchItem(
                icon = Icons.Filled.FormatColorFill,
                title = stringResource(id = R.string.settings_custom_background),
                summary = if (isCustomBackgroundEnabled) {
                    if (customBackgroundUri.isNotEmpty()) {
                        stringResource(id = R.string.settings_custom_background_enabled)
                    } else {
                        stringResource(id = R.string.settings_select_background_image)
                    }
                } else {
                    stringResource(id = R.string.settings_custom_background_summary)
                },
                checked = isCustomBackgroundEnabled
            ) {
                isCustomBackgroundEnabled = it
                BackgroundConfig.setCustomBackgroundEnabledState(it)
                BackgroundConfig.save(context)
                refreshTheme.value = true
            }
            
            // Select background image (only show when custom background is enabled)
            if (isCustomBackgroundEnabled) {
                // Opacity Slider
                var opacity by remember { mutableStateOf(BackgroundConfig.customBackgroundOpacity) }
                ListItem(
                    headlineContent = { Text(stringResource(id = R.string.settings_custom_background_opacity)) },
                    supportingContent = {
                        androidx.compose.material3.Slider(
                            value = opacity,
                            onValueChange = {
                                opacity = it
                                BackgroundConfig.setCustomBackgroundOpacityValue(it)
                                refreshTheme.value = true
                            },
                            onValueChangeFinished = {
                                BackgroundConfig.save(context)
                            },
                            valueRange = 0f..1f
                        )
                    }
                )

                // Dim Slider
                var dim by remember { mutableStateOf(BackgroundConfig.customBackgroundDim) }
                ListItem(
                    headlineContent = { Text(stringResource(id = R.string.settings_custom_background_dim)) },
                    supportingContent = {
                        androidx.compose.material3.Slider(
                            value = dim,
                            onValueChange = {
                                dim = it
                                BackgroundConfig.setCustomBackgroundDimValue(it)
                                refreshTheme.value = true
                            },
                            onValueChangeFinished = {
                                BackgroundConfig.save(context)
                            },
                            valueRange = 0f..1f
                        )
                    }
                )

                val pickImageLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    uri?.let {
                        scope.launch {
                            loadingDialog.show()
                            val success = BackgroundManager.saveAndApplyCustomBackground(context, it)
                            loadingDialog.hide()
                            if (success) {
                                // 使用保存后的URI，而不是原始URI
                                customBackgroundUri = BackgroundConfig.customBackgroundUri ?: ""
                                snackBarHost.showSnackbar(
                                    message = context.getString(R.string.settings_custom_background_saved)
                                )
                                refreshTheme.value = true
                            } else {
                                snackBarHost.showSnackbar(
                                    message = context.getString(R.string.settings_custom_background_error)
                                )
                            }
                        }
                    }
                }
                
                // 权限请求处理器
                val permissionHandler = remember { 
                    // 这里需要获取当前Activity，但在Compose中我们无法直接获取
                    // 所以我们需要使用一个不同的方法
                    null 
                }
                
                ListItem(
                    headlineContent = { 
                        Text(text = stringResource(id = R.string.settings_select_background_image))
                    },
                    supportingContent = {
                        if (customBackgroundUri.isNotEmpty()) {
                            Text(
                                text = stringResource(id = R.string.settings_background_selected),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    },
                    leadingContent = { Icon(painterResource(id = R.drawable.ic_custom_background), null) },
                    modifier = Modifier.clickable {
                        // 检查权限
                        if (PermissionUtils.hasExternalStoragePermission(context) && 
                            PermissionUtils.hasWriteExternalStoragePermission(context)) {
                            // 有权限，直接启动图片选择器
                            pickImageLauncher.launch("image/*")
                        } else {
                            // 无权限，显示提示信息
                            Toast.makeText(
                                context, 
                                "请先授予存储权限才能选择背景图片", 
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
                
                // Clear background button (only show when there's a background set)
                if (customBackgroundUri.isNotEmpty()) {
                    val clearBackgroundDialog = rememberConfirmDialog(
                        onConfirm = {
                            scope.launch {
                                loadingDialog.show()
                                BackgroundManager.clearCustomBackground(context)
                                customBackgroundUri = ""
                                loadingDialog.hide()
                                snackBarHost.showSnackbar(
                                    message = context.getString(R.string.settings_background_image_cleared)
                                )
                                refreshTheme.value = true
                            }
                        }
                    )
                    
                    val clearTitle = stringResource(id = R.string.settings_clear_background)
                    val clearConfirm = stringResource(id = R.string.settings_clear_background_confirm)
                    ListItem(
                        headlineContent = { 
                            Text(text = clearTitle)
                        },
                        leadingContent = { Icon(painterResource(id = R.drawable.ic_clear_background), null) },
                        modifier = Modifier.clickable {
                            clearBackgroundDialog.showConfirm(
                                title = clearTitle,
                                content = clearConfirm,
                                markdown = false
                            )
                        }
                    )
                }
            }

            // su path
            if (kPatchReady) {
                ListItem(
                    leadingContent = {
                        Icon(
                            Icons.Filled.Commit, stringResource(id = R.string.setting_reset_su_path)
                        )
                    },
                    supportingContent = {},
                    headlineContent = { Text(stringResource(id = R.string.setting_reset_su_path)) },
                    modifier = Modifier.clickable {
                        showResetSuPathDialog.value = true
                    })
            }

            // DPI Settings
            val showDpiDialog = remember { mutableStateOf(false) }
            ListItem(
                headlineContent = { Text(stringResource(id = R.string.settings_app_dpi)) },
                modifier = Modifier.clickable {
                    showDpiDialog.value = true
                },
                supportingContent = {
                    val currentDpi = me.bmax.apatch.util.DPIUtils.currentDpi
                    val dpiText = if (currentDpi == -1) {
                        stringResource(id = R.string.system_default)
                    } else {
                        "$currentDpi DPI"
                    }
                    Text(
                        text = dpiText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                },
                leadingContent = { Icon(Icons.Filled.AspectRatio, null) }
            )
            if (showDpiDialog.value) {
                DpiChooseDialog(showDpiDialog)
            }

            // language
            ListItem(headlineContent = {
                Text(text = stringResource(id = R.string.settings_app_language))
            }, modifier = Modifier.clickable {
                showLanguageDialog.value = true
            }, supportingContent = {
                Text(text = AppCompatDelegate.getApplicationLocales()[0]?.displayLanguage?.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(
                        Locale.getDefault()
                    ) else it.toString()
                } ?: stringResource(id = R.string.system_default),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline)
            }, leadingContent = { Icon(Icons.Filled.Translate, null) })

            // log
            ListItem(
                leadingContent = {
                    Icon(
                        Icons.Filled.BugReport, stringResource(id = R.string.send_log)
                    )
                },
                headlineContent = { Text(stringResource(id = R.string.send_log)) },
                modifier = Modifier.clickable {
                    showLogBottomSheet = true
                })
            if (showLogBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showLogBottomSheet = false },
                    contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
                    content = {
                        Row(
                            modifier = Modifier
                                .padding(10.dp)
                                .align(Alignment.CenterHorizontally)

                        ) {
                            Box {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .clickable {
                                            scope.launch {
                                                val formatter =
                                                    DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm")
                                                val current = LocalDateTime.now().format(formatter)
                                                exportBugreportLauncher.launch("APatch_bugreport_${current}.tar.gz")
                                                showLogBottomSheet = false
                                            }
                                        }
                                ) {
                                    Icon(
                                        Icons.Filled.Save,
                                        contentDescription = null,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                    Text(
                                        text = stringResource(id = R.string.save_log),
                                        modifier = Modifier.padding(top = 16.dp),
                                        textAlign = TextAlign.Center.also {
                                            LineHeightStyle(
                                                alignment = LineHeightStyle.Alignment.Center,
                                                trim = LineHeightStyle.Trim.None
                                            )
                                        }

                                    )
                                }

                            }
                            Box {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .clickable {
                                            scope.launch {
                                                val bugreport = loadingDialog.withLoading {
                                                    withContext(Dispatchers.IO) {
                                                        getBugreportFile(context)
                                                    }
                                                }

                                                val uri: Uri = FileProvider.getUriForFile(
                                                    context,
                                                    "${BuildConfig.APPLICATION_ID}.fileprovider",
                                                    bugreport
                                                )

                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    putExtra(Intent.EXTRA_STREAM, uri)
                                                    setDataAndType(uri, "application/gzip")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }

                                                context.startActivity(
                                                    Intent.createChooser(
                                                        shareIntent,
                                                        context.getString(R.string.send_log)
                                                    )
                                                )
                                                showLogBottomSheet = false
                                            }
                                        }) {
                                    Icon(
                                        Icons.Filled.Share,
                                        contentDescription = null,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                    Text(
                                        text = stringResource(id = R.string.send_log),
                                        modifier = Modifier.padding(top = 16.dp),
                                        textAlign = TextAlign.Center.also {
                                            LineHeightStyle(
                                                alignment = LineHeightStyle.Alignment.Center,
                                                trim = LineHeightStyle.Trim.None
                                            )
                                        }

                                    )
                                }

                            }
                        }
                        NavigationBarsSpacer()
                    })
            }


        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeChooseDialog(showDialog: MutableState<Boolean>) {
    val prefs = APApplication.sharedPreferences

    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false }, properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(310.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            LazyColumn {
                items(colorsList()) {
                    ListItem(
                        headlineContent = { Text(text = stringResource(it.nameId)) },
                        modifier = Modifier.clickable {
                            showDialog.value = false
                            prefs.edit { putString("custom_color", it.name) }
                            refreshTheme.value = true
                        })
                }

            }

            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }

}

private data class APColor(
    val name: String, @param:StringRes val nameId: Int
)

private fun colorsList(): List<APColor> {
    return listOf(
        APColor("amber", R.string.amber_theme),
        APColor("blue_grey", R.string.blue_grey_theme),
        APColor("blue", R.string.blue_theme),
        APColor("brown", R.string.brown_theme),
        APColor("cyan", R.string.cyan_theme),
        APColor("deep_orange", R.string.deep_orange_theme),
        APColor("deep_purple", R.string.deep_purple_theme),
        APColor("green", R.string.green_theme),
        APColor("indigo", R.string.indigo_theme),
        APColor("light_blue", R.string.light_blue_theme),
        APColor("light_green", R.string.light_green_theme),
        APColor("lime", R.string.lime_theme),
        APColor("orange", R.string.orange_theme),
        APColor("pink", R.string.pink_theme),
        APColor("purple", R.string.purple_theme),
        APColor("red", R.string.red_theme),
        APColor("sakura", R.string.sakura_theme),
        APColor("teal", R.string.teal_theme),
        APColor("yellow", R.string.yellow_theme),
    )
}

@Composable
private fun colorNameToString(colorName: String): Int {
    return colorsList().find { it.name == colorName }?.nameId ?: R.string.blue_theme
}

val suPathChecked: (path: String) -> Boolean = {
    it.startsWith("/") && it.trim().length > 1
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetSUPathDialog(showDialog: MutableState<Boolean>) {
    val context = LocalContext.current
    var suPath by remember { mutableStateOf(Natives.suPath()) }
    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false }, properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(310.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(PaddingValues(all = 24.dp))) {
                Box(
                    Modifier
                        .padding(PaddingValues(bottom = 16.dp))
                        .align(Alignment.Start)
                ) {
                    Text(
                        text = stringResource(id = R.string.setting_reset_su_path),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                Box(
                    Modifier
                        .weight(weight = 1f, fill = false)
                        .padding(PaddingValues(bottom = 12.dp))
                        .align(Alignment.Start)
                ) {
                    OutlinedTextField(
                        value = suPath,
                        onValueChange = {
                            suPath = it
                        },
                        label = { Text(stringResource(id = R.string.setting_reset_su_new_path)) },
                        visualTransformation = VisualTransformation.None,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showDialog.value = false }) {

                        Text(stringResource(id = android.R.string.cancel))
                    }

                    Button(enabled = suPathChecked(suPath), onClick = {
                        showDialog.value = false
                        val success = Natives.resetSuPath(suPath)
                        Toast.makeText(
                            context,
                            if (success) R.string.success else R.string.failure,
                            Toast.LENGTH_SHORT
                        ).show()
                        rootShellForResult("echo $suPath > ${APApplication.SU_PATH_FILE}")
                    }) {
                        Text(stringResource(id = android.R.string.ok))
                    }
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTitleChooseDialog(showDialog: MutableState<Boolean>) {
    val prefs = APApplication.sharedPreferences

    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false }, properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(310.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            LazyColumn {
                items(appTitleList()) { title ->
                    ListItem(
                        headlineContent = { Text(text = stringResource(title.nameId)) },
                        modifier = Modifier.clickable {
                            showDialog.value = false
                            prefs.edit { putString("app_title", title.name) }
                        })
                }
            }

            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}

private data class AppTitle(
    val name: String, @param:StringRes val nameId: Int
)

private fun appTitleList(): List<AppTitle> {
    return listOf(
        AppTitle("folkpatch", R.string.app_title_folkpatch),
        AppTitle("fpatch", R.string.app_title_fpatch),
        AppTitle("apatch_folk", R.string.app_title_apatch_folk),
        AppTitle("apatchx", R.string.app_title_apatchx),
        AppTitle("apatch", R.string.app_title_apatch),
        AppTitle("kernelpatch", R.string.app_title_kernelpatch),
    )
}

@Composable
private fun appTitleNameToString(titleName: String): Int {
    return appTitleList().find { it.name == titleName }?.nameId ?: R.string.app_title_folkpatch
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageDialog(showLanguageDialog: MutableState<Boolean>) {

    val languages = stringArrayResource(id = R.array.languages)
    val languagesValues = stringArrayResource(id = R.array.languages_values)

    if (showLanguageDialog.value) {
        BasicAlertDialog(
            onDismissRequest = { showLanguageDialog.value = false }
        ) {
            Surface(
                modifier = Modifier
                    .width(150.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(28.dp),
                tonalElevation = AlertDialogDefaults.TonalElevation,
                color = AlertDialogDefaults.containerColor,
            ) {
                LazyColumn {
                    itemsIndexed(languages) { index, item ->
                        ListItem(
                            headlineContent = { Text(item) },
                            modifier = Modifier.clickable {
                                showLanguageDialog.value = false
                                if (index == 0) {
                                    AppCompatDelegate.setApplicationLocales(
                                        LocaleListCompat.getEmptyLocaleList()
                                    )
                                } else {
                                    AppCompatDelegate.setApplicationLocales(
                                        LocaleListCompat.forLanguageTags(
                                            languagesValues[index]
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DpiChooseDialog(showDialog: MutableState<Boolean>) {
    val context = LocalContext.current
    val activity = context as? Activity

    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false }, properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(310.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            LazyColumn {
                items(me.bmax.apatch.util.DPIUtils.presets) { preset ->
                    ListItem(
                        headlineContent = { Text(text = preset.name) },
                        modifier = Modifier.clickable {
                            showDialog.value = false
                            me.bmax.apatch.util.DPIUtils.setDpi(context, preset.value)
                            // Restart activity to apply changes
                            activity?.recreate()
                        })
                }
            }

            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconChooseDialog(showDialog: MutableState<Boolean>) {
    val prefs = APApplication.sharedPreferences
    val context = LocalContext.current

    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false }, properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(310.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            LazyColumn {
                items(iconPresetList()) { preset ->
                    ListItem(
                        headlineContent = { Text(text = stringResource(preset.nameId)) },
                        modifier = Modifier.clickable {
                            showDialog.value = false
                            prefs.edit { putString("launcher_icon_variant", preset.name) }
                            me.bmax.apatch.util.LauncherIconUtils.applySaved(context, preset.name)
                        })
                }
            }

            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}

private data class IconPreset(
    val name: String, @param:StringRes val nameId: Int
)

private fun iconPresetList(): List<IconPreset> {
    return listOf(
        IconPreset("default", R.string.launcher_icon_default),
        IconPreset("classic", R.string.launcher_icon_classic),
        IconPreset("apatch", R.string.launcher_icon_apatch),
        IconPreset("kernelsu", R.string.launcher_icon_kernelsu),
        IconPreset("kernelsunext", R.string.launcher_icon_kernelsu_next),
        IconPreset("kitsune", R.string.launcher_icon_kitsune),
        IconPreset("magisk", R.string.launcher_icon_magisk),
    )
}

@Composable
private fun iconNameToString(iconName: String): Int {
    return iconPresetList().find { it.name == iconName }?.nameId ?: R.string.launcher_icon_default
}
