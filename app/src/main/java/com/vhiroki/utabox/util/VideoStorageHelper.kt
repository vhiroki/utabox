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


    @Suppress("DEPRECATION")
    private fun getVolumePath(volume: StorageVolume): String? {
        return volume.directory?.absolutePath
    }
}
