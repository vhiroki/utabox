package com.vhiroki.utabox.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import androidx.core.content.getSystemService
import java.io.File

/**
 * Helper class to locate karaoke video files from USB flash drives.
 * Auto-detects removable storage with a "videoke" folder containing CSV and video files.
 */
class VideoStorageHelper(private val context: Context) {

    companion object {
        private const val VIDEO_FOLDER_NAME = "videoke"
    }

    /**
     * Get the video URI for a given music ID.
     * Returns null if video file is not found.
     */
    fun getVideoUri(musicId: String): Uri? {
        val filename = "$musicId.mp4"
        return getUsbStorageUri(filename)
    }

    /**
     * Check if any video source is available
     */
    fun hasVideoSource(): Boolean {
        return isUsbStorageAvailable()
    }

    /**
     * Get description of current video source for UI
     */
    fun getVideoSourceDescription(): String {
        return if (isUsbStorageAvailable()) {
            "USB: ${getUsbVideoFolder()?.absolutePath ?: "Removable storage"}"
        } else {
            "No USB drive with 'videoke' folder detected"
        }
    }

    /**
     * Get the video source directory as a File (for USB).
     * Returns null if no USB storage is available.
     */
    fun getVideoSourceDirectory(): File? {
        return getUsbVideoFolder()
    }

    /**
     * Get the persisted folder URI (for SAF access).
     * Returns null - folder picker is not supported.
     */
    fun getPersistedTreeUri(): Uri? {
        return null
    }

    // ===== Private methods =====

    private fun getUsbVideoFolder(): File? {
        val storageManager = context.getSystemService<StorageManager>() ?: run {
            android.util.Log.e("VideoStorageHelper", "StorageManager is null")
            return null
        }

        android.util.Log.d("VideoStorageHelper", "Found ${storageManager.storageVolumes.size} storage volumes")

        for (volume in storageManager.storageVolumes) {
            val path = getVolumePath(volume)
            android.util.Log.d("VideoStorageHelper", "Volume: ${volume.getDescription(context)}, isRemovable=${volume.isRemovable}, state=${volume.state}, path=$path")

            if (volume.isRemovable && volume.state == Environment.MEDIA_MOUNTED) {
                if (path == null) {
                    android.util.Log.w("VideoStorageHelper", "Removable volume has null path")
                    continue
                }

                // Check for videoke folder
                val videokeFolder = File(path, VIDEO_FOLDER_NAME)
                android.util.Log.d("VideoStorageHelper", "Checking videoke folder: ${videokeFolder.absolutePath}, exists=${videokeFolder.exists()}, isDirectory=${videokeFolder.isDirectory}")

                if (videokeFolder.exists() && videokeFolder.isDirectory) {
                    val files = videokeFolder.listFiles()
                    android.util.Log.d("VideoStorageHelper", "videoke folder contents: ${files?.map { it.name } ?: "null (no permission?)"}")
                    return videokeFolder
                }

                // Also check root of USB for mp4 files
                val rootFolder = File(path)
                val rootFiles = rootFolder.listFiles()
                android.util.Log.d("VideoStorageHelper", "Root folder contents: ${rootFiles?.map { it.name } ?: "null (no permission?)"}")

                if (rootFiles?.any { it.extension == "mp4" } == true) {
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


    @Suppress("DEPRECATION")
    private fun getVolumePath(volume: StorageVolume): String? {
        return volume.directory?.absolutePath
    }
}
