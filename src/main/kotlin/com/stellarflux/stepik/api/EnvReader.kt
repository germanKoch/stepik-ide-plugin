package com.stellarflux.stepik.api

import java.io.File

object EnvReader {

    fun read(projectBasePath: String): Map<String, String> {
        val envFile = File(projectBasePath, ".env")
        if (!envFile.exists()) return emptyMap()
        return envFile.readLines()
            .filter { it.isNotBlank() && !it.trimStart().startsWith('#') }
            .mapNotNull { line ->
                val idx = line.indexOf('=')
                if (idx > 0) {
                    val key = line.substring(0, idx).trim()
                    val value = line.substring(idx + 1).trim()
                    key to value
                } else null
            }
            .toMap()
    }

    fun readCredentials(projectBasePath: String): Pair<String, String>? {
        val env = read(projectBasePath)
        val clientId = env["STEPIK_CLIENT_ID"] ?: return null
        val clientSecret = env["STEPIK_CLIENT_SECRET"] ?: return null
        if (clientId.isBlank() || clientSecret.isBlank()) return null
        return clientId to clientSecret
    }
}
