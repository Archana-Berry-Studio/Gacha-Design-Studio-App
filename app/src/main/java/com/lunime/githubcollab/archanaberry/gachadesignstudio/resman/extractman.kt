
/* -------------------------- Extractman.kt -------------------------- */
package com.lunime.githubcollab.archanaberry.gachadesignstudio.resman

import com.lunime.githubcollab.archanaberry.gachadesignstudio.*
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class Extractman(private val localization: GachaStudioLocalization) {

    init {
        CONFIG_FILE_PATH = "$baseDir$APP_FOLDER/data/data.xml"
        TEMP_DIR_PATH = "$baseDir$APP_FOLDER/.temp"
        ZIP_FILE_PATH = "$TEMP_DIR_PATH/DL.zip"
        EXTRACTED_DIR_PATH = "$TEMP_DIR_PATH/"
        SOURCE_DIR_PATH = "$EXTRACTED_DIR_PATH/Gacha-Design-Studio-DL/"
        MANIFEST_RESOURCE = "$baseDir$APP_FOLDER/manifest.json"
        DEST_DIR_PATH = "$baseDir$APP_FOLDER/"
    }

    fun manualExtract(): Boolean {
        return unzip(ZIP_FILE_PATH!!, EXTRACTED_DIR_PATH!!)
                && moveExtractedFiles(SOURCE_DIR_PATH!!, DEST_DIR_PATH!!)
                && deleteTempDirectory()
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
            GachaStudioLogger.log("Ekstraksi manual gagal: ${e.message}", isError = true)
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
            GachaStudioLogger.log("Pemindahan manual gagal: ${e.message}", isError = true)
            false
        }
    }

    private fun deleteTempDirectory(): Boolean {
        return try {
            File(TEMP_DIR_PATH).deleteRecursively()
            true
        } catch (e: Exception) {
            GachaStudioLogger.log("Gagal hapus temp manual: ${e.message}", isError = true)
            false
        }
    }
}