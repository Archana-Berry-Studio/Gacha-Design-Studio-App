package com.lunime.githubcollab.archanaberry.gachadesignstudio.resman

import com.lunime.githubcollab.archanaberry.gachadesignstudio.*
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class UpdaterMan(private val context: Context, private val localization: GachaStudioLocalization) {

    init {
        CONFIG_FILE_PATH = "$baseDir$APP_FOLDER/data/data.xml"
        TEMP_DIR_PATH = "$baseDir$APP_FOLDER/.temp"
        ZIP_FILE_PATH = "$TEMP_DIR_PATH/DL.zip"
        EXTRACTED_DIR_PATH = "$TEMP_DIR_PATH/"
        SOURCE_DIR_PATH = "$EXTRACTED_DIR_PATH/Gacha-Design-Studio-DL/"
        MANIFEST_RESOURCE = "$baseDir$APP_FOLDER/manifest.json"
        DEST_DIR_PATH = "$baseDir$APP_FOLDER/"
    }

    fun checkForUpdates(): Boolean {
        val localManifestFile = File(MANIFEST_RESOURCE)
        if (localManifestFile.exists()) {
            val localManifest = JSONObject(localManifestFile.readText())
            val remoteManifest = getRemoteManifest()

            remoteManifest?.let {
                val localAppVer = localManifest.optString("appver", "0.0_stable")
                val localVersion = localManifest.optString("version", "0.0_stable")
                val remoteAppVer = it.optString("appver", "0.0_stable")
                val remoteVersion = it.optString("version", "0.0_stable")

                return when {
                    compareVersions(remoteAppVer, localAppVer) > 0 -> {
                        showUpdateDialog()
                        true
                    }
                    compareVersions(remoteAppVer, localAppVer) < 0 -> {
                        showDowngradeDialog()
                        false
                    }
                    compareVersions(remoteVersion, localVersion) > 0 -> {
                        showResourceUpdateDialog()
                        true
                    }
                    else -> false
                }
            }
        }
        return false
    }

    private fun getRemoteManifest(): JSONObject? {
        return try {
            val url = URL(MANIFEST_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()
            
            val inputStream = connection.inputStream
            val content = inputStream.readBytes().toString(Charsets.UTF_8)
            inputStream.close()
            
            JSONObject(content)
        } catch (e: Exception) {
            GachaStudioLogger.log(localization.getString("fetch_manifest_error", e.message), isError = true)
            null
        }
    }

    private fun compareVersions(version1: String, version2: String): Int {
        val order = mapOf("alpha" to 1, "beta" to 2, "stable" to 3, "unstable" to 4)

        return try {
            val parts1 = version1.split("_")
            val parts2 = version2.split("_")

            val num1 = parts1.getOrNull(0)?.toDoubleOrNull() ?: 0.0
            val num2 = parts2.getOrNull(0)?.toDoubleOrNull() ?: 0.0
            val type1 = order[parts1.getOrNull(1) ?: "stable"] ?: 3
            val type2 = order[parts2.getOrNull(1) ?: "stable"] ?: 3

            when {
                num1 > num2 -> 1
                num1 < num2 -> -1
                else -> type1.compareTo(type2)
            }
        } catch (e: Exception) {
            GachaStudioLogger.log(localization.getString("compare_version_error", e.message), isError = true)
            0
        }
    }

    private fun showUpdateDialog() {
        AlertDialog.Builder(context).apply {
            setTitle("Gacha Design Studio")
            setMessage(localization.getString("update_required"))
            setPositiveButton(localization.getString("update_now")) { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URL))
                context.startActivity(intent)
            }
            setNegativeButton(localization.getString("update_later"), null)
            show()
        }
    }

    private fun showDowngradeDialog() {
        AlertDialog.Builder(context).apply {
            setTitle("Gacha Design Studio")
            setMessage(localization.getString("downgrade_required"))
            setPositiveButton(localization.getString("downgrade_now")) { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URL))
                context.startActivity(intent)
            }
            setNegativeButton(localization.getString("downgrade_later"), null)
            show()
        }
    }

    private fun showResourceUpdateDialog() {
        AlertDialog.Builder(context).apply {
            setTitle("Gacha Design Studio")
            setMessage(localization.getString("resource_update_available"))
            setPositiveButton(localization.getString("download_now")) { _, _ ->
                Downman(context, localization).downloadResources()
            }
            setNegativeButton(localization.getString("download_later"), null)
            show()
        }
    }
}