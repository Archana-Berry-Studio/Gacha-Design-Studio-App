package com.lunime.githubcollab.archanaberry.gachadesignstudio.resman

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.text.format.Formatter
import android.webkit.URLUtil
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import com.lunime.githubcollab.archanaberry.gachadesignstudio.GachaStudio
import com.lunime.githubcollab.archanaberry.gachadesignstudio.GachaStudioLocalization
import com.lunime.githubcollab.archanaberry.gachadesignstudio.GachaStudioLogger
import com.lunime.githubcollab.archanaberry.gachadesignstudio.R
import com.lunime.githubcollab.archanaberry.gachadesignstudio.*
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import kotlin.concurrent.thread

class Downman(
    private val context: Context,
    private val localization: GachaStudioLocalization,
    private val onSuccess: () -> Unit = {}
) {
    init {
        CONFIG_FILE_PATH    = "$baseDir$APP_FOLDER/data/data.xml"
        TEMP_DIR_PATH       = "$baseDir$APP_FOLDER/.temp"
        ZIP_FILE_PATH       = "$TEMP_DIR_PATH/DL.zip"
        EXTRACTED_DIR_PATH  = "$TEMP_DIR_PATH/"
        SOURCE_DIR_PATH     = "$EXTRACTED_DIR_PATH/Gacha-Design-Studio-DL/"
        MANIFEST_RESOURCE   = "$baseDir$APP_FOLDER/manifest.json"
        DEST_DIR_PATH       = "$baseDir$APP_FOLDER/"
    }

    private var progressDialog: AlertDialog? = null
    private var progressBar: ProgressBar?  = null
    private var textFileName: TextView?    = null
    private var textProgress: TextView?    = null
    private var textFileSize: TextView?    = null

    fun downloadResources() {
        progressDialog = AlertDialog.Builder(context).create()
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_progress, null)
        progressBar   = view.findViewById(R.id.progressBar)
        textFileName  = view.findViewById(R.id.textFileName)
        textProgress  = view.findViewById(R.id.textProgress)
        textFileSize  = view.findViewById(R.id.textFileSize)
        textFileName?.text = ""
        progressDialog?.apply {
            setView(view)
            setTitle(localization.getString("downloading"))
            setCancelable(false)
            show()
        }

        thread {
            try {
                var fileLength = -1L
                runCatching {
                    val head = URL(RESOURCE_URL).openConnection() as HttpURLConnection
                    head.requestMethod = "HEAD"
                    head.connectTimeout = 10_000
                    head.readTimeout = 10_000
                    head.connect()
                    fileLength = head.getHeaderFieldLong("Content-Length", -1)
                    head.disconnect()
                }

                if (fileLength <= 0 && RESOURCE_URL.contains("raw.githubusercontent.com")) {
                    runCatching {
                        val baseApi = RESOURCE_URL.replace(
                            "https://raw.githubusercontent.com/",
                            "https://api.github.com/repos/"
                        )
                        val pattern = Regex("/([^/]+/[^/]+/)(.+)")
                        val apiUrl = pattern.replaceFirst(baseApi, "$1contents/$2")
                        val apiConn = URL(apiUrl).openConnection() as HttpURLConnection
                        apiConn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                        apiConn.connectTimeout = 10_000
                        apiConn.readTimeout = 10_000
                        apiConn.connect()
                        val json = BufferedReader(InputStreamReader(apiConn.inputStream)).use { it.readText() }
                        fileLength = JSONObject(json).optLong("size", -1)
                        apiConn.disconnect()
                    }
                }

                val conn = URL(RESOURCE_URL).openConnection() as HttpURLConnection
                conn.connectTimeout = 15_000
                conn.readTimeout = 15_000
                conn.connect()

                val guessedName = URLUtil.guessFileName(
                    RESOURCE_URL,
                    conn.getHeaderField("Content-Disposition") ?: "",
                    null
                )
                (context as Activity).runOnUiThread {
                    if (fileLength > 0) {
                        progressBar?.isIndeterminate = false
                        progressBar?.max = 100
                    } else {
                        progressBar?.isIndeterminate = true
                    }
                    textFileName?.text = guessedName
                }

                File(TEMP_DIR_PATH).apply { mkdirs() }
                val zipFile = File(ZIP_FILE_PATH).apply { if (exists()) delete() }

                BufferedInputStream(conn.inputStream).use { input ->
                    FileOutputStream(zipFile).use { out ->
                        val buffer = ByteArray(4096)
                        var total = 0L
                        var count: Int
                        while (input.read(buffer).also { count = it } != -1) {
                            total += count
                            (context as Activity).runOnUiThread {
                                if (fileLength > 0) {
                                    val pct = (total * 100 / fileLength).toInt()
                                    progressBar?.progress = pct
                                    textProgress?.text = "$pct%"
                                    textFileSize?.text = "${Formatter.formatFileSize(context, total)} / ${Formatter.formatFileSize(context, fileLength)}"
                                } else {
                                    textProgress?.text = ""
                                    textFileSize?.text = Formatter.formatFileSize(context, total)
                                }
                            }
                            out.write(buffer, 0, count)
                        }
                        out.flush()
                    }
                }
                conn.disconnect()

                val zip = ZipFile(ZIP_FILE_PATH)
                val entries = zip.entries().toList().filter { !it.isDirectory }
                val totalEntries = entries.size.takeIf { it > 0 } ?: 1
                zip.close()
                (context as Activity).runOnUiThread {
                    progressDialog?.setTitle(localization.getString("assembling_resources"))
                    progressBar?.isIndeterminate = false
                    progressBar?.max = totalEntries
                    textProgress?.text = "0/$totalEntries"
                }
                var done = 0
                ZipInputStream(FileInputStream(ZIP_FILE_PATH)).use { zin ->
                    var currentEntry: ZipEntry? = zin.nextEntry
                    while (currentEntry != null) {
                        val entryName = currentEntry.name
                        val target = File(EXTRACTED_DIR_PATH, entryName)
                        if (currentEntry.isDirectory) target.mkdirs() else {
                            target.parentFile?.mkdirs()
                            FileOutputStream(target).use { fos -> zin.copyTo(fos) }
                        }
                        zin.closeEntry()
                        done++
                        (context as Activity).runOnUiThread {
                            textFileName?.text = entryName
                            progressBar?.progress = done
                            textProgress?.text = "$done/$totalEntries (EXTRACT)"
                        }
                        currentEntry = zin.nextEntry
                    }
                }

                val filesToMove = File(SOURCE_DIR_PATH!!).walk().filter { it.isFile }.toList()
                val moveCount = filesToMove.size.takeIf { it > 0 } ?: 1
                done = 0
                (context as Activity).runOnUiThread {
                    progressBar?.max = moveCount
                    textProgress?.text = "0/$moveCount (MOVE)"
                }
                filesToMove.forEach { f ->
                    val rel = f.relativeTo(File(SOURCE_DIR_PATH!!)).path
                    val dst = File(DEST_DIR_PATH!!, rel)
                    dst.parentFile?.mkdirs()
                    f.renameTo(dst)
                    done++
                    (context as Activity).runOnUiThread {
                        textFileName?.text = rel
                        progressBar?.progress = done
                        textProgress?.text = "$done/$moveCount (MOVE)"
                    }
                }
                deleteTemp()

                (context as Activity).runOnUiThread {
                    progressDialog?.dismiss()
                    Intent(context, GachaStudio::class.java).also {
                        it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        context.startActivity(it)
                    }
                    onSuccess()
                }

            } catch (e: Exception) {
                GachaStudioLogger.log("Error: ${e.message}", isError = true)
                (context as Activity).runOnUiThread {
                    progressDialog?.dismiss()
                    AlertDialog.Builder(context)
                        .setCancelable(false)
                        .setTitle(localization.getString("download_failed"))
                        .setMessage(localization.getString("download_failed_message"))
                        .setPositiveButton(localization.getString("oke0")) { d, _ ->
                            d.dismiss()
                            (context as? Activity)?.finish()
                        }
                        .show()
                }
            }
        }
    }

    private fun deleteTemp() {
        File(TEMP_DIR_PATH).deleteRecursively()
    }
}
