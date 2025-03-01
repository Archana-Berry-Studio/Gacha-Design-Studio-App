package com.lunime.githubcollab.archanaberry.gachadesignstudio

import com.lunime.githubcollab.archanaberry.gachadesignstudio.gachadynamicstorage.GachaDynamicStorage

import android.widget.RemoteViews
import android.app.AlertDialog
import android.widget.ProgressBar
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

// Constants
const val DOWNLOAD_REQUEST_CODE = 2
const val APP_FOLDER = "/Lunime/Gacha Design Studio"

// File paths and other variables
var CONFIG_FILE_PATH: String? = null
var TEMP_DIR_PATH: String? = null
var ZIP_FILE_PATH: String? = null
var EXTRACTED_DIR_PATH: String? = null
var SOURCE_DIR_PATH: String? = null
var DEST_DIR_PATH: String? = null
var MANIFEST_RESOURCE: String? = null

// UI components
var progressDialog: AlertDialog? = null
var progressBar: ProgressBar? = null
var remoteViews: RemoteViews? = null
var builder: NotificationCompat.Builder? = null
var notificationManager: NotificationManagerCompat? = null
var agreementAccepted: Boolean = false

// URLs
const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.lunime.githubcollab.archanaberry.gachadesignstudio"
const val MANIFEST_URL = "https://raw.githubusercontent.com/archanaberry/Gacha-Design-Studio/DL/manifest.json"
const val RESOURCE_URL = "https://github.com/archanaberry/Gacha-Design-Studio/archive/refs/heads/DL.zip"

// WebView and back press logic
var backPressedTime: Long = 0
const val PRESS_BACK_INTERVAL = 2000L // 2 seconds

const val REQUEST_CODE_FILE_PICK = 100
const val PERMISSION_REQUEST_CODE = 1

// Akses baseDir secara dinamis menggunakan GachaDynamicStorage
val baseDir = GachaDynamicStorage.detectDynamicDirectory()