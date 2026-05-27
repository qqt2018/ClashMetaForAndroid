package com.github.kr328.clash.util

import com.github.kr328.clash.design.model.SitenavItem
import java.io.File

object SitenavParser {
    val DEFAULT_ITEMS = listOf(
        SitenavItem("Google", "https://www.google.com"),
        SitenavItem("OpenAI", "https://www.openai.com"),
        SitenavItem("Gemini", "https://gemini.google.com"),
        SitenavItem("Anthropic", "https://www.anthropic.com"),
        SitenavItem("Reddit", "https://www.reddit.com"),
        SitenavItem("redd.it", "https://www.redd.it"),
        SitenavItem("Vimeo", "https://vimeo.com"),
        SitenavItem("Imgur", "https://imgur.com"),
        SitenavItem("phncdn", "https://www.phncdn.com")
    )

    fun parse(file: File): List<SitenavItem> {
        if (!file.exists()) return DEFAULT_ITEMS
        return try {
            val text = file.readText()
            val parsed = parseYamlSitenav(text)
            if (parsed.isEmpty()) DEFAULT_ITEMS else parsed
        } catch (e: Exception) {
            DEFAULT_ITEMS
        }
    }

    private fun parseYamlSitenav(yamlText: String): List<SitenavItem> {
        val lines = yamlText.lines()
        var sitenavLineIndex = -1
        var baseIndent = -1
        for (i in lines.indices) {
            val line = lines[i]
            val trimmed = line.trimStart()
            if (trimmed.startsWith("sitenav:")) {
                sitenavLineIndex = i
                baseIndent = line.length - trimmed.length
                break
            }
        }
        if (sitenavLineIndex == -1) return emptyList()

        val items = mutableListOf<SitenavItem>()
        var currentName: String? = null
        var currentUrl: String? = null
        var currentOpenMode: String? = null
        var currentSpan: Int? = null

        fun commitCurrent() {
            val name = currentName
            val url = currentUrl
            val openMode = currentOpenMode
            val span = currentSpan ?: 4
            if (name != null && url != null) {
                items.add(SitenavItem(name, url, openMode, span))
            }
            currentName = null
            currentUrl = null
            currentOpenMode = null
            currentSpan = null
        }

        for (i in (sitenavLineIndex + 1) until lines.size) {
            val line = lines[i]
            if (line.trim().isEmpty()) continue
            val trimmed = line.trimStart()
            val indent = line.length - trimmed.length
            if (indent <= baseIndent) {
                break
            }

            if (trimmed.startsWith("-")) {
                commitCurrent()
                val rest = trimmed.substring(1).trimStart()
                parseKeyValuePair(rest)?.let { (key, value) ->
                    if (key == "name") currentName = value
                    if (key == "url") currentUrl = value
                    if (key == "open_mode") currentOpenMode = value
                    if (key == "span") currentSpan = value.toIntOrNull()
                }
            } else {
                parseKeyValuePair(trimmed)?.let { (key, value) ->
                    if (key == "name") currentName = value
                    if (key == "url") currentUrl = value
                    if (key == "open_mode") currentOpenMode = value
                    if (key == "span") currentSpan = value.toIntOrNull()
                }
            }
        }
        commitCurrent()
        return items

    }

    private fun parseKeyValuePair(line: String): Pair<String, String>? {
        val colonIdx = line.indexOf(':')
        if (colonIdx == -1) return null
        val key = line.substring(0, colonIdx).trim()
        var value = line.substring(colonIdx + 1).trim()
        if ((value.startsWith("'") && value.endsWith("'")) || (value.startsWith("\"") && value.endsWith("\""))) {
            value = value.substring(1, value.length - 1)
        }
        return Pair(key, value)
    }
}
