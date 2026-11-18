package com.boosthub.app.data

import org.json.JSONObject
import java.io.IOException

/** All Facebook and AI requests go through your own Vercel server. No API keys live in the app. */
object Backend {
    private fun base(s: Store): String {
        val u = s.serverUrl.trim().trimEnd('/')
        if (u.isBlank()) throw IOException("Server is not connected. Open Settings and connect your server.")
        return u
    }

    private fun headers(s: Store) = mapOf(
        "Content-Type" to "application/json",
        "x-access-code" to s.accessCode
    )

    fun post(s: Store, route: String, body: JSONObject): String =
        Http.request("POST", "${base(s)}/api/$route", headers(s), body.toString())

    fun get(s: Store, route: String): String =
        Http.request("GET", "${base(s)}/api/$route", headers(s), null)

    /** Reads the server status and caches it on the phone. Throws if the server cannot be reached. */
    fun sync(s: Store) {
        val j = JSONObject(get(s, "config"))
        s.fbReady = j.optBoolean("facebook")
        s.aiReady = j.optBoolean("ai")
        s.adAccount = j.optString("adAccountId")
        s.pageId = j.optString("pageId")
        s.aiModel = j.optString("model")
    }
}
