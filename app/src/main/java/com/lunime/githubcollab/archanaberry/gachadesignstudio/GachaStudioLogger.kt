package com.lunime.githubcollab.archanaberry.gachadesignstudio

import android.os.Process
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory

object GachaStudioLogger {

    private val logBuffer = mutableListOf<String>()
    private var logFile: File? = null
    private var isLoggingEnabled: Boolean = true // Default aktif

    // Direktori Dinamis
    private val baseDir: String = detectDynamicDirectory()
    private val APP_FOLDER = "/Lunime/Gacha Design Studio"
    
    private val CONFIG_FILE_PATH = "$baseDir$APP_FOLDER/data/data.xml"
    private val LOG_DIR_PATH = "$baseDir$APP_FOLDER/data/logging"

    private fun detectDynamicDirectory(): String {
        val userId = Process.myUserHandle().hashCode()
        return if (userId == 0) {
            "/storage/emulated/0"
        } else {
            "/storage/emulated/$userId"
        }
    }

    private fun getLogFile(): File {
        val directory = File(LOG_DIR_PATH)
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val dateFormat = SimpleDateFormat("ddMMMyyyy", Locale.getDefault())
        val date = dateFormat.format(Date())
        var logFile = File(directory, "log0-$date.txt")
        var logIndex = 0

        while (logFile.exists()) {
            logIndex++
            logFile = File(directory, "log$logIndex-$date.txt")
        }

        return logFile
    }

    // Fungsi untuk membaca status logging dari data.xml
    private fun updateLoggingStatus() {
        val configFile = File(CONFIG_FILE_PATH)

        if (configFile.exists()) {
            try {
                val dbFactory = DocumentBuilderFactory.newInstance()
                val dBuilder = dbFactory.newDocumentBuilder()
                val fis = FileInputStream(configFile)
                val doc: Document = dBuilder.parse(fis)
                doc.documentElement.normalize()

                val loggingNodes = doc.getElementsByTagName("logging")
                if (loggingNodes.length > 0) {
                    val loggingNode = loggingNodes.item(0) as Element
                    isLoggingEnabled = loggingNode.getAttribute("enabled") == "true"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun log(message: String, isError: Boolean = false) {
        updateLoggingStatus() // Selalu baca ulang status logging sebelum menulis log

        if (!isLoggingEnabled) return

        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timeStamp = timeFormat.format(Date())
        val dateFormat = SimpleDateFormat("ddMMMyyyy", Locale.getDefault())
        val date = dateFormat.format(Date())

        val logType = if (isError) "Lunime Corrupted" else "Archana Berry Analyzer"
        val logMessage = "$timeStamp/$date - $logType: $message"

        println(logMessage)

        try {
            if (logFile == null) {
                logFile = getLogFile()
            }
            logFile?.appendText("$logMessage\n")
        } catch (e: Exception) {
            println("Gagal menulis log ke file: ${e.message}")
        }
    }
}