package com.boosthub.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** All settings and logs are kept in the phone's private storage. API keys are NOT stored here. */
class Store(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("boosthub", Context.MODE_PRIVATE)

    private fun s(k: String, d: String = ""): String = sp.getString(k, d) ?: d
    private fun b(k: String, d: Boolean): Boolean = sp.getBoolean(k, d)
    private fun i(k: String, d: Int): Int = sp.getInt(k, d)

    // ---- Server connection (entered by the user) ----
    var serverUrl: String
        get() = s("serverUrl")
        set(v) { sp.edit().putString("serverUrl", v.trim().trimEnd('/')).apply() }
    var accessCode: String
        get() = s("accessCode")
        set(v) { sp.edit().putString("accessCode", v.trim()).apply() }

    // ---- Cached from the server (not entered by the user) ----
    var adAccount: String
        get() = s("adAccount")
        set(v) { sp.edit().putString("adAccount", v.trim()).apply() }
    var pageId: String
        get() = s("pageId")
        set(v) { sp.edit().putString("pageId", v.trim()).apply() }
    var fbReady: Boolean
        get() = b("fbReady", false)
        set(v) { sp.edit().putBoolean("fbReady", v).apply() }
    var aiReady: Boolean
        get() = b("aiReady", false)
        set(v) { sp.edit().putBoolean("aiReady", v).apply() }
    var aiModel: String
        get() = s("aiModel")
        set(v) { sp.edit().putString("aiModel", v.trim()).apply() }

    var bizInfo: String
        get() = s("bizInfo")
        set(v) { sp.edit().putString("bizInfo", v).apply() }

    var autopilot: Boolean
        get() = b("autopilot", false)
        set(v) { sp.edit().putBoolean("autopilot", v).apply() }
    var autoReply: Boolean
        get() = b("autoReply", true)
        set(v) { sp.edit().putBoolean("autoReply", v).apply() }
    var adsOpt: Boolean
        get() = b("adsOpt", true)
        set(v) { sp.edit().putBoolean("adsOpt", v).apply() }
    var autoApply: Boolean
        get() = b("autoApply", false)
        set(v) { sp.edit().putBoolean("autoApply", v).apply() }
    var autoComment: Boolean
        get() = b("autoComment", false)
        set(v) { sp.edit().putBoolean("autoComment", v).apply() }

    /** Max total daily budget across all campaigns (main currency). 0 = budget increases not allowed */
    var dailyCap: Int
        get() = i("dailyCap", 0)
        set(v) { sp.edit().putInt("dailyCap", v).apply() }
    var maxChangePct: Int
        get() = i("maxChangePct", 20)
        set(v) { sp.edit().putInt("maxChangePct", v).apply() }
    var maxActions: Int
        get() = i("maxActions", 3)
        set(v) { sp.edit().putInt("maxActions", v).apply() }
    var maxReplies: Int
        get() = i("maxReplies", 5)
        set(v) { sp.edit().putInt("maxReplies", v).apply() }

    fun isDone(id: String): Boolean = s("done").split(",").contains(id)
    fun markDone(id: String) {
        val l = s("done").split(",").filter { it.isNotBlank() }.toMutableList()
        l.add(id)
        sp.edit().putString("done", l.takeLast(300).joinToString(",")).apply()
    }

    fun log(msg: String) {
        val old = JSONArray(s("log", "[]"))
        val n = JSONArray()
        n.put(JSONObject().put("t", System.currentTimeMillis()).put("m", msg))
        for (k in 0 until minOf(old.length(), 79)) n.put(old.get(k))
        sp.edit().putString("log", n.toString()).apply()
    }

    fun logs(): List<Pair<Long, String>> {
        val a = JSONArray(s("log", "[]"))
        return (0 until a.length()).map { a.getJSONObject(it).let { o -> o.getLong("t") to o.getString("m") } }
    }
}
