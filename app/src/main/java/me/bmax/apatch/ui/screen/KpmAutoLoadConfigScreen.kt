package me.bmax.apatch.ui.screen

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.KpmAutoLoadConfig
import me.bmax.apatch.ui.component.KpmAutoLoadManager
import me.bmax.apatch.util.ui.APDialogBlurBehindUtils
import me.bmax.apatch.util.ui.showToast

@Destination<RootGraph>
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KpmAutoLoadConfigScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    var isEnabled by remember { mutableStateOf(KpmAutoLoadManager.isEnabled.value) }
    var jsonString by remember { mutableStateOf(KpmAutoLoadManager.getConfigJson()) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var isValidJson by remember { mutableStateOf(true) }
    var isVisualMode by remember { mutableStateOf(false) }
    var kpmPathsList by remember { mutableStateOf(KpmAutoLoadManager.kpmPaths.value.toList()) }
    
    // 获取URI的真实路径
    fun getPathFromUri(context: Context, uri: android.net.Uri): String {
        // 简化处理，在实际应用中应该更完善地处理不同类型的URI
        return when (uri.scheme) {
            "content" -> {
                // 对于content URI，尝试从文件名获取一个合理的路径
                val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "unknown.kpm"
                "/storage/emulated/0/Download/$fileName"
            }
            "file" -> uri.path ?: ""
            else -> uri.toString()
        }
    }
    
    // 根据路径列表更新JSON字符串
    fun updateJsonString(paths: List<String>, enabled: Boolean, onUpdate: (String) -> Unit) {
        val config = KpmAutoLoadConfig(enabled, paths)
        onUpdate(KpmAutoLoadManager.getConfigJson(config))
    }
    
    // 文件选择器
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val path = getPathFromUri(context, it)
            if (path.endsWith(".kpm", ignoreCase = true) && path !in kpmPathsList) {
                kpmPathsList = kpmPathsList + path
                // 更新JSON字符串
                updateJsonString(kpmPathsList, isEnabled) { newJson ->
                    jsonString = newJson
                }
            }
        }
    }
    
    LaunchedEffect(Unit) {
        val config = KpmAutoLoadManager.loadConfig(context)
        isEnabled = config.enabled
        jsonString = KpmAutoLoadManager.getConfigJson()
        kpmPathsList = config.kpmPaths
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.kpm_autoload_title)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(android.R.string.cancel)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 功能启用开关
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.kpm_autoload_enabled),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.kpm_autoload_enabled_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { 
                            isEnabled = it
                            updateJsonString(kpmPathsList, it) { newJson ->
                                jsonString = newJson
                            }
                        }
                    )
                }
            }

            // 可视化模式或JSON模式
            if (isVisualMode) {
                // 可视化模式
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.kpm_autoload_kpm_list_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Button(
                                onClick = {
                                    filePickerLauncher.launch("application/octet-stream")
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(stringResource(R.string.kpm_autoload_add_kpm))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (kpmPathsList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.kpm_autoload_no_kpm_added),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f)
                            ) {
                                items(kpmPathsList) { path ->
                                    ListItem(
                                        headlineContent = { 
                                            Text(
                                                text = path.substringAfterLast("/"),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        },
                                        supportingContent = { 
                                            Text(
                                                text = path,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        trailingContent = {
                                            IconButton(
                                                onClick = {
                                                    kpmPathsList = kpmPathsList - path
                                                    updateJsonString(kpmPathsList, isEnabled) { newJson ->
                                                        jsonString = newJson
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = stringResource(R.string.kpm_autoload_remove_kpm),
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // JSON配置编辑框
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.kpm_autoload_json_config),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        OutlinedTextField(
                            value = jsonString,
                            onValueChange = { 
                                jsonString = it
                                isValidJson = KpmAutoLoadManager.parseConfigFromJson(it) != null
                                // 如果JSON有效，更新路径列表
                                if (isValidJson) {
                                    KpmAutoLoadManager.parseConfigFromJson(it)?.let { config ->
                                        kpmPathsList = config.kpmPaths
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            label = { Text(stringResource(R.string.kpm_autoload_json_label)) },
                            placeholder = { Text(stringResource(R.string.kpm_autoload_json_placeholder)) },
                            isError = !isValidJson,
                            supportingText = {
                                if (!isValidJson) {
                                    Text(stringResource(R.string.kpm_autoload_json_error))
                                } else {
                                    Text(stringResource(R.string.kpm_autoload_json_helper))
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 可视化模式/JSON模式切换按钮
                Button(
                    onClick = {
                        isVisualMode = !isVisualMode
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isVisualMode) stringResource(R.string.kpm_autoload_json_mode) else stringResource(R.string.kpm_autoload_visual_mode))
                }
                
                // 保存按钮
                Button(
                    onClick = {
                        showSaveDialog = true
                    },
                    modifier = Modifier.weight(1f),
                    enabled = if (isVisualMode) kpmPathsList.isNotEmpty() else isValidJson
                ) {
                    Text(stringResource(R.string.kpm_autoload_save))
                }
            }
        }
    }

    // 保存确认对话框
    if (showSaveDialog) {
        BasicAlertDialog(
            onDismissRequest = { showSaveDialog = false }
        ) {
            Surface(
                modifier = Modifier
                    .width(320.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                tonalElevation = AlertDialogDefaults.TonalElevation,
                color = AlertDialogDefaults.containerColor,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.kpm_autoload_save_confirm),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showSaveDialog = false }) {
                            Text(stringResource(android.R.string.cancel))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            val config = if (isVisualMode) {
                                // 使用可视化模式的数据
                                KpmAutoLoadConfig(enabled = isEnabled, kpmPaths = kpmPathsList)
                            } else {
                                // 使用JSON模式的数据
                                KpmAutoLoadConfig(enabled = isEnabled, kpmPaths = 
                                    KpmAutoLoadManager.parseConfigFromJson(jsonString)?.kpmPaths ?: emptyList()
                                )
                            }
                            
                            val success = KpmAutoLoadManager.saveConfig(context, config)
                            if (success) {
                                showToast(context, context.getString(R.string.kpm_autoload_save_success))
                                navigator.navigateUp()
                            } else {
                                showToast(context, context.getString(R.string.kpm_autoload_save_failed))
                            }
                            showSaveDialog = false
                        }) {
                            Text(stringResource(android.R.string.ok))
                        }
                    }
                }
            }
        }
    }
}