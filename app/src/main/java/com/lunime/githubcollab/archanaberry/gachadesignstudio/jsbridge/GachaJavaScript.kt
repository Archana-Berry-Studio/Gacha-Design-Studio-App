package com.lunime.githubcollab.archanaberry.gachadesignstudio.jsbridge

import android.content.Context
import android.webkit.JavascriptInterface
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * JS bridge providing file operations that support both single-file and multi-file modes.
 * Usage examples (from JS in WebView):
 *
 * // Single file copy:
 * window.Gacha.copy("/path/src.txt", "/path/dest.txt");
 *
 * // Multiple file copy:
 * window.Gacha.copyMultiple(["/path/a.txt", "/path/b.txt"], "/dest/dir");
 *
 * // Single file move:
 * window.Gacha.move("/path/src.txt", "/path/dest.txt");
 *
 * // Multiple file move:
 * window.Gacha.moveMultiple(["/path/a.txt", "/path/b.txt"], "/dest/dir");
 *
 * // Write single file:
 * window.Gacha.write("/path/file.txt", "Hello world");
 *
 * // Write multiple files (same content):
 * window.Gacha.writeMultiple(["/f1.txt", "/f2.txt"], "Hello");
 *
 * // Read single file:
 * window.Gacha.read("/path/file.txt"); // returns string or null
 *
 * // Read multiple files:
 * window.Gacha.readMultiple(["/a.txt", "/b.txt"]); // returns JSON array of contents or null entries
 *
 * // Zip files / directory:
 * window.Gacha.zip(["/a.txt", "/b.txt"], "/out/archive.zip");
 * window.Gacha.zipDir("/my/dir", "/out/dir.zip", true);
 *
 * // Extract zip:
 * window.Gacha.unzip("/in/archive.zip", "/out/folder");
 */
class GachaJavascript(private val context: Context) {

    // ===== COPY =====
    @JavascriptInterface
    fun copy(sourcePath: String, destPath: String): Boolean {
        // Delegate to copyMultiple for uniform handling
        return copyMultiple(arrayOf(sourcePath), destPath)
    }

    @JavascriptInterface
    fun copyMultiple(sourcePaths: Array<String>, destDir: String): Boolean {
        return try {
            val destDirectory = File(destDir)
            if (!destDirectory.exists()) destDirectory.mkdirs()
            sourcePaths.forEach { path ->
                val src = File(path)
                if (src.exists()) {
                    val dest = if (destDirectory.isDirectory) File(destDirectory, src.name) else File(destDir)
                    // Copy file or directory recursively
                    if (src.isDirectory) {
                        copyDirectory(src, dest)
                    } else {
                        src.copyTo(dest, overwrite = true)
                    }
                }
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    private fun copyDirectory(source: File, destination: File) {
        if (!destination.exists()) destination.mkdirs()
        source.listFiles()?.forEach { file ->
            val destFile = File(destination, file.name)
            if (file.isDirectory) {
                copyDirectory(file, destFile)
            } else {
                file.copyTo(destFile, overwrite = true)
            }
        }
    }

    // ===== MOVE =====
    @JavascriptInterface
    fun move(sourcePath: String, destPath: String): Boolean {
        return moveMultiple(arrayOf(sourcePath), destPath)
    }

    @JavascriptInterface
    fun moveMultiple(sourcePaths: Array<String>, destDir: String): Boolean {
        return try {
            val destDirectory = File(destDir)
            if (!destDirectory.exists()) destDirectory.mkdirs()
            sourcePaths.forEach { path ->
                val src = File(path)
                if (src.exists()) {
                    val dest = if (destDirectory.isDirectory) File(destDirectory, src.name) else File(destDir)
                    // Move by copy then delete
                    if (src.isDirectory) {
                        copyDirectory(src, dest)
                        src.deleteRecursively()
                    } else {
                        src.copyTo(dest, overwrite = true)
                        src.delete()
                    }
                }
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    // ===== WRITE =====
    @JavascriptInterface
    fun write(filePath: String, content: String): Boolean {
        return writeMultiple(arrayOf(filePath), content)
    }

    @JavascriptInterface
    fun writeMultiple(filePaths: Array<String>, content: String): Boolean {
        return try {
            filePaths.forEach { path ->
                val file = File(path)
                file.parentFile?.mkdirs()
                file.writeText(content)
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    // ===== READ =====
    // ===== READ SINGLE =====
    @JavascriptInterface
    fun read(filePath: String): String? {
        return try {
            val file = File(filePath)
            if (file.exists() && file.isFile) file.readText() else null
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    // ===== READ MULTIPLE =====
    /**
     * @param filePaths array of full file paths
     * @return JSON string: each line '<filename>:<content>'
     */
    @JavascriptInterface
    fun readMultiple(filePaths: Array<String>): String {
        val builder = StringBuilder()
        filePaths.forEachIndexed { index, path ->
            try {
                val file = File(path)
                if (file.exists() && file.isFile) {
                    val name = file.name
                    val content = file.readText()
                    builder.append("$name:\n$content")
                } else {
                    builder.append("$path: (not found)")
                }
            } catch (e: IOException) {
                builder.append("$path: (error)")
            }
            if (index < filePaths.size - 1) builder.append("\n\n")
        }
        return builder.toString()
    }
    
    // ===== ZIP =====
    @JavascriptInterface
    fun zip(sources: Array<String>, zipPath: String): Boolean {
        return try {
            val zipFile = File(zipPath)
            zipFile.parentFile?.mkdirs()
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                sources.forEach { srcPath ->
                    val file = File(srcPath)
                    if (file.exists()) {
                        if (file.isDirectory) {
                            zipDirectoryRec(file, file.name, zos)
                        } else {
                            zos.putNextEntry(ZipEntry(file.name))
                            file.inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }
                }
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    private fun zipDirectoryRec(folder: File, baseName: String, zos: ZipOutputStream) {
        folder.listFiles()?.forEach { file ->
            val entryName = "$baseName/${file.name}"
            if (file.isDirectory) {
                zipDirectoryRec(file, entryName, zos)
            } else {
                zos.putNextEntry(ZipEntry(entryName))
                file.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }

    @JavascriptInterface
    fun zipDir(dirPath: String, zipPath: String, includeRoot: Boolean): Boolean {
        val folder = File(dirPath)
        if (!folder.exists() || !folder.isDirectory) return false
        return zip(arrayOf(dirPath), zipPath)
    }

    // ===== UNZIP =====
    @JavascriptInterface
    fun unzip(zipPath: String, outputDir: String): Boolean {
        return try {
            val destDir = File(outputDir)
            if (!destDir.exists()) destDir.mkdirs()
            ZipInputStream(File(zipPath).inputStream()).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val outFile = File(destDir, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().use { zis.copyTo(it) }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }
}
