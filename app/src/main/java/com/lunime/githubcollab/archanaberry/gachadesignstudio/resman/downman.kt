package com.lunime.githubcollab.archanaberry.gachadesignstudio.resman

import com.lunime.githubcollab.archanaberry.gachadesignstudio.*

import androidx.appcompat.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.app.Activity
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.os.AsyncTask
import com.lunime.githubcollab.archanaberry.gachadesignstudio.R
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class Downman(private val context: Context, private val localization: GachaStudioLocalization, private val onSuccess: () -> Unit = {}) {

    init {
        CONFIG_FILE_PATH = "$baseDir$APP_FOLDER/data/data.xml"
        TEMP_DIR_PATH = "$baseDir$APP_FOLDER/.temp"
        ZIP_FILE_PATH = "$TEMP_DIR_PATH/DL.zip"
        EXTRACTED_DIR_PATH = "$TEMP_DIR_PATH/"
        SOURCE_DIR_PATH = "$EXTRACTED_DIR_PATH/Gacha-Design-Studio-DL/"
        MANIFEST_RESOURCE = "$baseDir$APP_FOLDER/manifest.json"
        DEST_DIR_PATH = "$baseDir$APP_FOLDER/"
    }

    private var progressDialog: AlertDialog? = null
    private var progressBar: ProgressBar? = null

    fun downloadResources() {
        progressDialog = AlertDialog.Builder(context).create()
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_progress, null)
        progressBar = dialogView.findViewById(R.id.progressBar)
        progressDialog?.apply {
            setView(dialogView)
            setTitle(localization.getString("additional_resource_download"))
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            show()
        }

        GachaStudioLogger.log(localization.getString("executing_download_task"))
        DownloadResourcesTask().execute()
    }

    private inner class DownloadResourcesTask : AsyncTask<Void, Int, Boolean>() {

        override fun doInBackground(vararg params: Void): Boolean {
            return try {
                val url = URL(RESOURCE_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                val fileLength = connection.contentLength
                val tempDir = File(TEMP_DIR_PATH).apply { mkdirs() }
                val outputFile = File(ZIP_FILE_PATH)

                if (outputFile.exists()) outputFile.delete()

                BufferedInputStream(connection.inputStream).use { input ->
                    FileOutputStream(outputFile).use { output ->
                        val buffer = ByteArray(1024)
                        var total: Long = 0
                        var count: Int
                        while (input.read(buffer).also { count = it } != -1) {
                            total += count
                            publishProgress((total * 100 / fileLength).toInt())
                            output.write(buffer, 0, count)
                        }
                    }
                }

                if (!unzip(ZIP_FILE_PATH!!, EXTRACTED_DIR_PATH!!)) return false
                if (!moveExtractedFiles(SOURCE_DIR_PATH!!, DEST_DIR_PATH!!)) return false
                if (!deleteTempDirectory()) return false

                true
            } catch (e: IOException) {
                GachaStudioLogger.log(localization.getString("process_failed"), isError = true)
                GachaStudioLogger.log(localization.getString("failure_detail", e.message ?: "Unknown error"), isError = true)
                false
            }
        }

        override fun onProgressUpdate(vararg values: Int?) {
            values[0]?.let {
                progressBar?.progress = it
                GachaStudioLogger.log(localization.getString("update_progress", it))
            }
        }

        override fun onPostExecute(success: Boolean) {
            progressDialog?.dismiss()
            val builder = AlertDialog.Builder(context).apply {
                setCancelable(false)
                setPositiveButton(localization.getString("oke0")) { dialog, _ ->
                    dialog.dismiss()
                    if (success) onSuccess() else (context as? Activity)?.finish()
                }
            }

            if (success) {
                builder.setTitle(localization.getString("download_successful"))
                    .setMessage(localization.getString("resources_extracted_successfully"))
                GachaStudioLogger.log(localization.getString("resource_extraction_success"))
            } else {
                builder.setTitle(localization.getString("download_failed"))
                    .setMessage(localization.getString("download_failed_message"))
                GachaStudioLogger.log(localization.getString("download_failed"), isError = true)
            }

            builder.show()
        }
    }

    private fun openGachaDesignStudio(context: Context) {
        val intent = Intent(context, GachaStudio::class.java)
        context.startActivity(intent)
        if (context is Activity) {
            context.finish()
        }
    }

    private fun unzip(zipFilePath: String, extractedDirPath: String): Boolean {
        return try {
            val destDir = File(extractedDirPath).apply { mkdirs() }
            ZipInputStream(FileInputStream(zipFilePath)).use { zipIn ->
                var entry: ZipEntry?
                while (zipIn.nextEntry.also { entry = it } != null) {
                    val file = File(destDir, entry!!.name)
                    if (entry!!.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        FileOutputStream(file).use { output ->
                            val buffer = ByteArray(1024)
                            var len: Int
                            while (zipIn.read(buffer).also { len = it } != -1) {
                                output.write(buffer, 0, len)
                            }
                        }
                    }
                    zipIn.closeEntry()
                }
            }
            true
        } catch (e: Exception) {
            GachaStudioLogger.log(localization.getString("extraction_failed", e.message ?: "Unknown error"), isError = true)
            false
        }
    }

    private fun moveExtractedFiles(sourcePath: String, destPath: String): Boolean {
        return try {
            val sourceDir = File(sourcePath)
            val destDir = File(destPath).apply { mkdirs() }
            sourceDir.listFiles()?.forEach { file ->
                file.renameTo(File(destDir, file.name))
            }
            true
        } catch (e: Exception) {
            GachaStudioLogger.log(localization.getString("move_failed", e.message ?: "Unknown error"), isError = true)
            false
        }
    }

    private fun deleteTempDirectory(): Boolean {
        return try {
            File(TEMP_DIR_PATH!!).deleteRecursively()
            true
        } catch (e: Exception) {
            GachaStudioLogger.log(localization.getString("delete_temp_failed", e.message ?: "Unknown error"), isError = true)
            false
        }
    }
}