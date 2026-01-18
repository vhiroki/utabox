package com.vhiroki.utabox.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.DocumentsContract
import androidx.core.content.getSystemService
import androidx.documentfile.provider.DocumentFile
import java.io.File

/**
 * Helper class to locate karaoke video files.
 * Priority:
 * 1. Test folder: /sdcard/Download/karaoke/
 * 2. USB flash drives (auto-detect removable storage)
 * 3. User-selected folder via document picker
 */
class VideoStorageHelper(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "video_storage_prefs"
        private const val KEY_PERSISTED_URI = "persisted_folder_uri"
        private const val TEST_FOLDER = "Download/karaoke"
        private const val VIDEO_FOLDER_NAME = "videoke"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Get the video URI for a given music ID.
     * Returns null if video file is not found.
     */
    fun getVideoUri(musicId: String): Uri? {
        val filename = "$musicId.mp4"

        // 1. Check test folder first (development)
        getTestFolderUri(filename)?.let { return it }

        // 2. Check USB/removable storage
        getUsbStorageUri(filename)?.let { return it }

        // 3. Check user-persisted folder
        getPersistedFolderUri(filename)?.let { return it }

        return null
    }

    /**
     * Check if any video source is available
     */
    fun hasVideoSource(): Boolean {
        return hasPersistedFolder() || isTestFolderAvailable() || isUsbStorageAvailable()
    }

    /**
     * Get description of current video source for UI
     */
    fun getVideoSourceDescription(): String {
        return when {
            hasPersistedFolder() -> {
                val uri = getPersistedUri()
                val path = uri?.path?.substringAfter("primary:")?.replace("/", "/") ?: uri.toString()
                "Folder: $path"
            }
            isTestFolderAvailable() -> "Test folder: ${getTestFolder()?.absolutePath}"
            isUsbStorageAvailable() -> "USB: ${getUsbVideoFolder()?.absolutePath ?: "Removable storage"}"
            else -> "No video source configured"
        }
    }

    /**
     * Save user-selected folder URI with persistent permission
     */
    fun savePersistedFolderUri(uri: Uri) {
        // Take persistable permission
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, takeFlags)

        prefs.edit().putString(KEY_PERSISTED_URI, uri.toString()).apply()
    }

    /**
     * Clear persisted folder selection
     */
    fun clearPersistedFolder() {
        getPersistedUri()?.let { uri ->
            try {
                context.contentResolver.releasePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) { }
        }
        prefs.edit().remove(KEY_PERSISTED_URI).apply()
    }

    fun hasPersistedFolder(): Boolean {
        return getPersistedUri() != null
    }

    /**
     * Get the video source directory as a File (for test folder or USB).
     * Returns null if only SAF/persisted folder is available.
     */
    fun getVideoSourceDirectory(): File? {
        getTestFolder()?.let { if (isTestFolderAvailable()) return it }
        getUsbVideoFolder()?.let { return it }
        return null
    }

    /**
     * Get the persisted folder URI (for SAF access).
     * Returns null if no folder is persisted.
     */
    fun getPersistedTreeUri(): Uri? {
        return getPersistedUri()
    }

    // ===== Private methods =====

    private fun getTestFolder(): File? {
        val folder = File(Environment.getExternalStorageDirectory(), TEST_FOLDER)
        android.util.Log.d("VideoStorageHelper", "Test folder path: ${folder.absolutePath}, exists: ${folder.exists()}, isDir: ${folder.isDirectory}")
        return if (folder.exists() && folder.isDirectory) folder else null
    }

    private fun isTestFolderAvailable(): Boolean {
        val folder = getTestFolder() ?: return false
        val files = folder.listFiles()
        android.util.Log.d("VideoStorageHelper", "Test folder files: ${files?.map { it.name }}")
        // If listFiles returns null, try checking if a known file exists
        if (files == null) {
            // Try direct file check as fallback
            val testFile = File(folder, "02017.mp4")
            android.util.Log.d("VideoStorageHelper", "Fallback check for 02017.mp4: ${testFile.exists()}")
            return testFile.exists()
        }
        return files.any { it.extension == "mp4" }
    }

    private fun getTestFolderUri(filename: String): Uri? {
        val folder = getTestFolder() ?: return null
        val file = File(folder, filename)
        android.util.Log.d("VideoStorageHelper", "Looking for file: ${file.absolutePath}, exists: ${file.exists()}")
        return if (file.exists()) Uri.fromFile(file) else null
    }

    private fun getUsbVideoFolder(): File? {
        val storageManager = context.getSystemService<StorageManager>() ?: return null

        for (volume in storageManager.storageVolumes) {
            if (volume.isRemovable && volume.state == Environment.MEDIA_MOUNTED) {
                val path = getVolumePath(volume) ?: continue

                // Check for videoke folder
                val videokeFolder = File(path, VIDEO_FOLDER_NAME)
                if (videokeFolder.exists() && videokeFolder.isDirectory) {
                    return videokeFolder
                }

                // Also check root of USB for mp4 files
                val rootFolder = File(path)
                if (rootFolder.listFiles()?.any { it.extension == "mp4" } == true) {
                    return rootFolder
                }
            }
        }
        return null
    }

    private fun isUsbStorageAvailable(): Boolean {
        return getUsbVideoFolder() != null
    }

    private fun getUsbStorageUri(filename: String): Uri? {
        val folder = getUsbVideoFolder() ?: return null
        val file = File(folder, filename)
        return if (file.exists()) Uri.fromFile(file) else null
    }

    private fun getPersistedUri(): Uri? {
        val uriString = prefs.getString(KEY_PERSISTED_URI, null) ?: return null
        val uri = Uri.parse(uriString)

        // Verify we still have permission
        val hasPermission = context.contentResolver.persistedUriPermissions
            .any { it.uri == uri && it.isReadPermission }

        return if (hasPermission) uri else null
    }

    private fun getPersistedFolderUri(filename: String): Uri? {
        val folderUri = getPersistedUri() ?: return null
        val documentFile = DocumentFile.fromTreeUri(context, folderUri) ?: return null

        // Search in root and videoke subfolder
        documentFile.findFile(filename)?.let { return it.uri }
        documentFile.findFile(VIDEO_FOLDER_NAME)?.findFile(filename)?.let { return it.uri }

        return null
    }

    @Suppress("DEPRECATION")
    private fun getVolumePath(volume: StorageVolume): String? {
        return volume.directory?.absolutePath
    }
}
