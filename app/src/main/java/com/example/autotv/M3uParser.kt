package com.example.autotv

import java.net.HttpURLConnection
import java.net.URL

data class Channel(val name: String, val url: String)

object M3uParser {
    fun load(source: String): List<Channel> {
        val text = if (source.startsWith("http")) {
            val c = URL(source).openConnection() as HttpURLConnection
            c.connectTimeout = 10000; c.readTimeout = 20000
            c.inputStream.bufferedReader().use { it.readText() }
        } else return emptyList()
        val out = ArrayList<Channel>()
        var name: String? = null
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.startsWith("#EXTINF")) {
                name = line.substringAfterLast(",", "Channel").trim()
            } else if (line.isNotEmpty() && !line.startsWith("#")) {
                out.add(Channel(name ?: line, line)); name = null
            }
        }
        return out
    }
}
