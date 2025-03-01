package com.lunime.githubcollab.archanaberry.gachadesignstudio.jsbridge

import android.content.Context
import android.webkit.JavascriptInterface
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class GachaJavascript(private val context: Context) {
    
    @JavascriptInterface
    fun copyFile(sourcePath: String, destPath: String): Boolean {
        return try {
            val source = File(sourcePath)
            val dest = File(destPath)
            source.copyTo(dest, overwrite = true)
            true
        } catch (e: IOException) {
            false
        }
    }
    
    @JavascriptInterface
    fun moveFile(sourcePath: String, destPath: String): Boolean {
        return try {
            val source = File(sourcePath)
            val dest = File(destPath)
            source.copyTo(dest, overwrite = true)
            source.delete()
            true
        } catch (e: IOException) {
            false
        }
    }
    
    @JavascriptInterface
    fun writeFile(filePath: String, content: String): Boolean {
        return try {
            val file = File(filePath)
            file.writeText(content)
            true
        } catch (e: IOException) {
            false
        }
    }
    
    @JavascriptInterface
    fun readFile(filePath: String): String? {
        return try {
            val file = File(filePath)
            if (file.exists()) file.readText() else null
        } catch (e: IOException) {
            null
        }
    }
    
    @JavascriptInterface
    fun zipFiles(zipPath: String, files: Array<String>): Boolean {
        return try {
            val zipFile = File(zipPath)
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                files.forEach { filePath ->
                    val file = File(filePath)
                    if (file.exists()) {
                        zos.putNextEntry(ZipEntry(file.name))
                        file.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
            true
        } catch (e: IOException) {
            false
        }
    }
    
    @JavascriptInterface
    fun extractZip(zipPath: String, outputDir: String): Boolean {
        return try {
            val destDir = File(outputDir)
            if (!destDir.exists()) destDir.mkdirs()
            
            ZipInputStream(File(zipPath).inputStream()).use { zis ->
                var entry: ZipEntry?
                while (zis.nextEntry.also { entry = it } != null) {
                    val file = File(destDir, entry!!.name)
                    file.outputStream().use { zis.copyTo(it) }
                    zis.closeEntry()
                }
            }
            true
        } catch (e: IOException) {
            false
        }
    }
}