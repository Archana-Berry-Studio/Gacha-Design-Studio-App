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

    fun unzip(zipFilePath: String, destDirectory: String) {
        try {
            val destDir = File(destDirectory)
            if (!destDir.exists()) {
                destDir.mkdirs()
                GachaStudioLogger.log(localization.getString("create_folder", destDir))
            }

            ZipInputStream(FileInputStream(zipFilePath)).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                while (entry != null) {
                    val filePath = "$destDirectory${File.separator}${entry.name}"
                    if (!entry.isDirectory) {
                        extractFile(zipIn, filePath)
                        GachaStudioLogger.log(localization.getString("extracting_file", filePath))
                    } else {
                        File(filePath).mkdirs()
                        GachaStudioLogger.log(localization.getString("create_folder", filePath))
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
            GachaStudioLogger.log(localization.getString("close_extraction"))
        } catch (e: IOException) {
            GachaStudioLogger.log(localization.getString("extraction_error", e.message), isError = true)
            e.printStackTrace()
        }
    }

    private fun extractFile(zipIn: ZipInputStream, filePath: String) {
        try {
            File(filePath).parentFile?.mkdirs()
            BufferedOutputStream(FileOutputStream(filePath)).use { bos ->
                val buffer = ByteArray(4096)
                var len: Int
                while (zipIn.read(buffer).also { len = it } != -1) {
                    bos.write(buffer, 0, len)
                }
            }
            GachaStudioLogger.log(localization.getString("file_extracted", filePath))
        } catch (e: IOException) {
            GachaStudioLogger.log(localization.getString("bos_error", e.message), isError = true)
            e.printStackTrace()
        }
    }

    fun moveExtractedFiles(srcDirPath: String, destDirPath: String) {
        try {
            val srcDir = File(srcDirPath)
            val destDir = File(destDirPath)

            if (!destDir.exists()) {
                destDir.mkdirs()
                GachaStudioLogger.log(localization.getString("move_extracted_files", srcDir, destDir))
            }

            srcDir.walkTopDown().forEach { file ->
                val relativePath = file.relativeTo(srcDir)
                val destFile = File(destDir, relativePath.path)
                if (file.isDirectory) {
                    destFile.mkdirs()
                    GachaStudioLogger.log(localization.getString("copy_files", relativePath, destFile))
                } else {
                    file.copyTo(destFile, overwrite = true)
                    GachaStudioLogger.log(localization.getString("overwrite_warning"), isError = true)
                }
            }

            if (srcDir.exists()) {
                srcDir.deleteRecursively()
                GachaStudioLogger.log(localization.getString("delete_folder", srcDir), isError = true)
            }
        } catch (e: IOException) {
            GachaStudioLogger.log(localization.getString("generic_error", e.message), isError = true)
            e.printStackTrace()
        }
    }

    fun deleteTempDirectory(tempDirPath: String) {
        try {
            val tempDir = File(tempDirPath)
            tempDir.deleteRecursively()
            GachaStudioLogger.log(localization.getString("delete_temp_to_free_memory", tempDir), isError = true)
        } catch (e: IOException) {
            GachaStudioLogger.log(localization.getString("temp_delete_error", e.message), isError = true)
            e.printStackTrace()
        }
    }
}