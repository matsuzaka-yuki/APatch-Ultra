package me.bmax.apatch.util

import android.content.Context
import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File

object ModuleBackupUtils {

    private const val MODULE_DIR = "/data/adb/modules"

    suspend fun backupModules(context: Context, snackBarHost: SnackbarHostState, uri: Uri) {
        withContext(Dispatchers.IO) {
            try {
                // Use the busybox bundled with APatch
                val busyboxPath = "/data/adb/ap/bin/busybox"
                val tempFile = File(context.cacheDir, "backup_tmp.tar.gz")
                val tempPath = tempFile.absolutePath

                if (tempFile.exists()) tempFile.delete()

                // Construct command to tar the modules directory to temp file
                // And chmod it so the app can read it
                val command = "cd \"$MODULE_DIR\" && $busyboxPath tar -czf \"$tempPath\" ./* && chmod 666 \"$tempPath\""

                val result = getRootShell().newJob().add(command).exec()

                if (result.isSuccess) {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        tempFile.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                    tempFile.delete()
                    withContext(Dispatchers.Main) {
                        snackBarHost.showSnackbar(context.getString(R.string.apm_backup_success))
                    }
                } else {
                    val error = result.err.joinToString("\n")
                    withContext(Dispatchers.Main) {
                        snackBarHost.showSnackbar(context.getString(R.string.apm_backup_failed_msg, error))
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    snackBarHost.showSnackbar(context.getString(R.string.apm_backup_failed_msg, e.message))
                }
            }
        }
    }

    suspend fun restoreModules(context: Context, snackBarHost: SnackbarHostState, uri: Uri) {
        withContext(Dispatchers.IO) {
            try {
                val busyboxPath = "/data/adb/ap/bin/busybox"
                val tempFile = File(context.cacheDir, "restore_tmp.tar.gz")
                val tempPath = tempFile.absolutePath

                if (tempFile.exists()) tempFile.delete()

                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                // Make sure root can read it
                tempFile.setReadable(true, false)

                val command = "cd \"$MODULE_DIR\" && $busyboxPath tar -xzf \"$tempPath\""
                val result = getRootShell().newJob().add(command).exec()

                tempFile.delete()

                if (result.isSuccess) {
                    // Refresh module list
                    // APatchCli.refresh() // Wait, this refreshes shell, not module list. 
                    // Module list is refreshed by viewModel.fetchModuleList() in UI
                    
                    withContext(Dispatchers.Main) {
                        snackBarHost.showSnackbar(context.getString(R.string.apm_restore_success))
                    }
                } else {
                    val error = result.err.joinToString("\n")
                    withContext(Dispatchers.Main) {
                        snackBarHost.showSnackbar(context.getString(R.string.apm_restore_failed_msg, error))
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    snackBarHost.showSnackbar(context.getString(R.string.apm_restore_failed_msg, e.message))
                }
            }
        }
    }
}
