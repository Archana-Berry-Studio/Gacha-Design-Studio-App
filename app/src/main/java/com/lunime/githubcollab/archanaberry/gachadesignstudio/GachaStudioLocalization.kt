package com.lunime.githubcollab.archanaberry.gachadesignstudio

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

class GachaStudioLocalization private constructor(private val localizationMap: Map<String, String>) {

    companion object {
        private const val LANGUAGE_FOLDER = "localization"
        private const val DEFAULT_LANGUAGE = "en"
        private const val LANGUAGE_PREFIX = "GachaStudio."

        private const val DELIMITER = "\u2B80" // Simbol '⮀'
        private const val NEWLINE_PLACEHOLDER = "\\nl\\" // Placeholder untuk newline

        /**
         * Factory method untuk membuat instance GachaStudioLocalization
         */
        fun load(context: Context): GachaStudioLocalization {
            val languageCode = getSystemLanguageCode()
            val filesToTry = listOf(
                "$LANGUAGE_PREFIX$languageCode",
                "$LANGUAGE_PREFIX$DEFAULT_LANGUAGE"
            )

            val localizationMap = loadLanguageFiles(context, filesToTry)
            return GachaStudioLocalization(localizationMap)
        }

        private fun getSystemLanguageCode(): String {
            return Locale.getDefault().language.lowercase()
        }

        private fun loadLanguageFiles(context: Context, filesToTry: List<String>): Map<String, String> {
            val map = mutableMapOf<String, String>()
            val assetManager = context.assets

            for (fileName in filesToTry) {
                try {
                    val fullPath = "$LANGUAGE_FOLDER/$fileName"
                    assetManager.open(fullPath).use { inputStream ->
                        val reader = BufferedReader(InputStreamReader(inputStream))
                        reader.forEachLine { line ->
                            val (key, value) = parseKeyValue(line)
                            if (key.isNotBlank()) {
                                map[key] = value
                            }
                        }
                    }
                    return map
                } catch (e: Exception) {
                    println("Tidak dapat memuat file bahasa: $fileName")
                }
            }
            return map
        }

        private fun parseKeyValue(line: String): Pair<String, String> {
            val startIndexKey = line.indexOf(DELIMITER)
            val endIndexKey = line.indexOf(DELIMITER, startIndexKey + 1)
            val separatorIndex = line.indexOf("=")

            if (startIndexKey >= 0 && endIndexKey > startIndexKey && separatorIndex > endIndexKey) {
                val key = line.substring(startIndexKey + 1, endIndexKey)

                // Ambil nilai di sebelah kanan '=' dan hapus delimiter tanpa trim keseluruhan string
                val rawValue = line.substring(separatorIndex + 1).replace(DELIMITER, "")
                val value = processNewlines(rawValue)
                return Pair(key, value)
            }
            return Pair("", "")
        }

        private fun processNewlines(value: String): String {
            return value.replace(NEWLINE_PLACEHOLDER, "\n")
        }
    }

    fun getString(key: String, vararg args: Any?): String {
        val template = localizationMap[key] ?: "[$key]"
        return if (args.isEmpty()) {
            template
        } else {
            if (template.contains("%")) {
                try {
                    val nonNullableArgs = args.map { it?.toString() ?: "" }.toTypedArray()
                    String.format(template, *nonNullableArgs)
                } catch (e: Exception) {
                    "[$key: Error formatting string]"
                }
            } else {
                template + args.joinToString("")
            }
        }
    }
}