package com.stellarflux.stepik.api

import com.intellij.util.io.HttpRequests
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Base64

class StepikAuth(private val clientId: String, private val clientSecret: String) {

    private var cachedToken: String? = null
    private var expiresAt: Long = 0

    @Synchronized
    fun getToken(): String {
        val now = System.currentTimeMillis()
        cachedToken?.let { if (now < expiresAt) return it }
        return refreshToken()
    }

    @Synchronized
    fun invalidateToken() {
        cachedToken = null
        expiresAt = 0
    }

    private fun refreshToken(): String {
        val credentials = Base64.getEncoder().encodeToString("$clientId:$clientSecret".toByteArray())
        val body = "grant_type=client_credentials"

        val response = HttpRequests.post(TOKEN_URL, "application/x-www-form-urlencoded")
            .tuner { it.setRequestProperty("Authorization", "Basic $credentials") }
            .connect { request ->
                request.write(body)
                request.readString()
            }

        val json = Json.parseToJsonElement(response).jsonObject
        val token = json["access_token"]?.jsonPrimitive?.content
            ?: throw RuntimeException("Failed to obtain access token from Stepik")
        val expiresIn = json["expires_in"]?.jsonPrimitive?.content?.toLongOrNull() ?: 3600L

        cachedToken = token
        expiresAt = System.currentTimeMillis() + (expiresIn - 60) * 1000
        return token
    }

    companion object {
        private const val TOKEN_URL = "https://stepik.org/oauth2/token/"
    }
}
