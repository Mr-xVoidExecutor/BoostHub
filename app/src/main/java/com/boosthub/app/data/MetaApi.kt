package com.boosthub.app.data

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

/** Facebook Graph API calls. They go through your Vercel server, which holds the access token. */
object MetaApi {
    private const val USER = "user"
    private const val PAGE = "page"

    private fun call(s: Store, method: String, path: String, params: Map<String, String>, role: String): String {
        val body = JSONObject()
            .put("method", method)
            .put("path", path)
            .put("params", JSONObject(params))
            .put("as", role)
        return Backend.post(s, "meta", body)
    }

    private fun get(s: Store, path: String, params: Map<String, String>, role: String): String =
        call(s, "GET", path, params, role)

    private fun post(s: Store, path: String, params: Map<String, String>, role: String): String =
        call(s, "POST", path, params, role)

    private fun acct(s: Store) = if (s.adAccount.startsWith("act_")) s.adAccount else "act_${s.adAccount}"

    // ---------- Ads ----------
    data class Campaign(val id: String, val name: String, val status: String, val dailyBudget: Long)
    data class Insight(val spend: Double, val impressions: Long, val clicks: Long, val ctr: Double, val cpc: Double, val results: Double)

    fun campaigns(s: Store): List<Campaign> {
        val j = JSONObject(get(s, "${acct(s)}/campaigns", mapOf("fields" to "id,name,effective_status,daily_budget", "limit" to "50"), USER))
        val a = j.optJSONArray("data") ?: return emptyList()
        val out = mutableListOf<Campaign>()
        for (k in 0 until a.length()) {
            val o = a.getJSONObject(k)
            val st = o.optString("effective_status")
            if (st == "DELETED" || st == "ARCHIVED") continue
            out.add(Campaign(o.optString("id"), o.optString("name"), st, o.optString("daily_budget", "0").toLongOrNull() ?: 0L))
        }
        return out
    }

    fun insights(s: Store): Map<String, Insight> {
        val j = JSONObject(
            get(
                s,
                "${acct(s)}/insights",
                mapOf("level" to "campaign", "fields" to "campaign_id,spend,impressions,clicks,ctr,cpc,actions", "date_preset" to "last_7d", "limit" to "100"),
                USER
            )
        )
        val a = j.optJSONArray("data") ?: return emptyMap()
        val m = mutableMapOf<String, Insight>()
        for (k in 0 until a.length()) {
            val o = a.getJSONObject(k)
            var results = 0.0
            val acts = o.optJSONArray("actions")
            if (acts != null) {
                for (x in 0 until acts.length()) {
                    val ao = acts.getJSONObject(x)
                    val t = ao.optString("action_type")
                    if (t.contains("purchase") || t.contains("lead") || t.contains("messaging_conversation_started")) {
                        results += ao.optString("value", "0").toDoubleOrNull() ?: 0.0
                    }
                }
            }
            m[o.optString("campaign_id")] = Insight(
                o.optString("spend", "0").toDoubleOrNull() ?: 0.0,
                o.optString("impressions", "0").toLongOrNull() ?: 0L,
                o.optString("clicks", "0").toLongOrNull() ?: 0L,
                o.optString("ctr", "0").toDoubleOrNull() ?: 0.0,
                o.optString("cpc", "0").toDoubleOrNull() ?: 0.0,
                results
            )
        }
        return m
    }

    fun setStatus(s: Store, id: String, status: String) {
        post(s, id, mapOf("status" to status), USER)
    }

    fun setBudget(s: Store, id: String, minorUnits: Long) {
        post(s, id, mapOf("daily_budget" to minorUnits.toString()), USER)
    }

    // ---------- Page messages ----------
    data class Conv(
        val id: String,
        val psid: String,
        val name: String,
        val lastMsgId: String,
        val lastText: String,
        val lastTime: Long,
        val lastFromPage: Boolean,
        val transcript: String
    )

    private fun parseTime(x: String): Long =
        try { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).parse(x)?.time ?: 0L } catch (e: Exception) { 0L }

    fun conversations(s: Store): List<Conv> {
        val j = JSONObject(
            get(
                s,
                "${s.pageId}/conversations",
                mapOf("platform" to "messenger", "fields" to "id,updated_time,messages.limit(6){id,message,from,created_time}", "limit" to "15"),
                PAGE
            )
        )
        val a = j.optJSONArray("data") ?: return emptyList()
        val out = mutableListOf<Conv>()
        for (k in 0 until a.length()) {
            val o = a.getJSONObject(k)
            val msgs = o.optJSONObject("messages")?.optJSONArray("data") ?: continue
            if (msgs.length() == 0) continue
            val first = msgs.getJSONObject(0)
            val lastFromPage = first.optJSONObject("from")?.optString("id") == s.pageId
            var psid = ""
            var name = "Customer"
            val sb = StringBuilder()
            for (x in msgs.length() - 1 downTo 0) {
                val m = msgs.getJSONObject(x)
                val from = m.optJSONObject("from")
                val isPage = from?.optString("id") == s.pageId
                if (!isPage && from != null) { psid = from.optString("id"); name = from.optString("name", "Customer") }
                sb.appendLine((if (isPage) "Page: " else "Customer: ") + m.optString("message"))
            }
            out.add(
                Conv(
                    o.optString("id"), psid, name, first.optString("id"),
                    first.optString("message"), parseTime(first.optString("created_time")),
                    lastFromPage, sb.toString().trim()
                )
            )
        }
        return out
    }

    fun sendMessage(s: Store, psid: String, text: String) {
        post(
            s,
            "${s.pageId}/messages",
            mapOf(
                "recipient" to JSONObject().put("id", psid).toString(),
                "message" to JSONObject().put("text", text).toString(),
                "messaging_type" to "RESPONSE"
            ),
            PAGE
        )
    }

    // ---------- Billing ----------
    data class Billing(
        val name: String,
        val id: String,
        val currency: String,
        val balance: Long,
        val amountSpent: Long,
        val spendCap: Long,
        val status: Int,
        val funding: String,
        val bizName: String,
        val bizAddress: String,
        val taxId: String,
        val prepay: Long,
        val threshold: Long,
        val daily: List<Pair<String, Double>>
    )

    fun billing(s: Store): Billing {
        val j = JSONObject(
            get(
                s,
                acct(s),
                mapOf("fields" to "name,account_id,currency,balance,amount_spent,spend_cap,account_status,funding_source_details"),
                USER
            )
        )
        var bizName = ""; var bizAddr = ""; var taxId = ""
        try {
            val b = JSONObject(
                get(
                    s,
                    acct(s),
                    mapOf("fields" to "business_name,business_street,business_city,business_zip,business_country_code,tax_id"),
                    USER
                )
            )
            bizName = b.optString("business_name")
            bizAddr = listOf(b.optString("business_street"), b.optString("business_city"), b.optString("business_zip"), b.optString("business_country_code"))
                .filter { it.isNotBlank() }.joinToString(", ")
            taxId = b.optString("tax_id")
        } catch (e: Exception) { }
        var prepay = 0L
        try {
            val p = JSONObject(get(s, acct(s), mapOf("fields" to "total_prepay_balance"), USER))
            prepay = p.optJSONObject("total_prepay_balance")?.optString("amount_in_hundredths")?.toLongOrNull() ?: 0L
        } catch (e: Exception) { }
        var threshold = 0L
        try {
            val d = JSONObject(get(s, "${acct(s)}/adspaymentcycle", mapOf("fields" to "threshold_amount"), USER)).optJSONArray("data")
            if (d != null && d.length() > 0) threshold = d.getJSONObject(0).optString("threshold_amount").toLongOrNull() ?: 0L
        } catch (e: Exception) { }
        val daily = mutableListOf<Pair<String, Double>>()
        try {
            val d = JSONObject(
                get(s, acct(s), mapOf("fields" to "spend", "time_increment" to "1", "date_preset" to "last_7d", "level" to "account"), USER)
            ).optJSONArray("data")
            if (d != null) for (k in 0 until d.length()) {
                val o = d.getJSONObject(k)
                daily.add(o.optString("date_start") to (o.optString("spend", "0").toDoubleOrNull() ?: 0.0))
            }
        } catch (e: Exception) { }
        return Billing(
            j.optString("name"), j.optString("account_id"), j.optString("currency"),
            j.optString("balance", "0").toLongOrNull() ?: 0L,
            j.optString("amount_spent", "0").toLongOrNull() ?: 0L,
            j.optString("spend_cap", "0").toLongOrNull() ?: 0L,
            j.optInt("account_status"),
            j.optJSONObject("funding_source_details")?.optString("display_string") ?: "",
            bizName, bizAddr, taxId, prepay, threshold, daily
        )
    }

    /** Account spending limit (minor units). 0 = remove the limit */
    fun setSpendCap(s: Store, minorUnits: Long) {
        post(s, acct(s), mapOf("spend_cap" to minorUnits.toString()), USER)
    }

    fun billingUrl(s: Store): String {
        val id = s.adAccount.removePrefix("act_")
        return "https://business.facebook.com/billing_hub/payment_settings?asset_id=$id"
    }

    // ---------- Dashboard ----------
    data class Summary(val spend: Double, val impressions: Long, val clicks: Long, val results: Double, val daily: List<Pair<String, Double>>)

    fun summary(s: Store): Summary {
        val d = JSONObject(
            get(
                s,
                acct(s),
                mapOf("fields" to "spend,impressions,clicks,actions", "time_increment" to "1", "date_preset" to "last_7d", "level" to "account"),
                USER
            )
        ).optJSONArray("data")
        var sp = 0.0; var im = 0L; var cl = 0L; var rs = 0.0
        val daily = mutableListOf<Pair<String, Double>>()
        if (d != null) for (k in 0 until d.length()) {
            val o = d.getJSONObject(k)
            val x = o.optString("spend", "0").toDoubleOrNull() ?: 0.0
            sp += x
            im += o.optString("impressions", "0").toLongOrNull() ?: 0L
            cl += o.optString("clicks", "0").toLongOrNull() ?: 0L
            val acts = o.optJSONArray("actions")
            if (acts != null) for (a in 0 until acts.length()) {
                val ao = acts.getJSONObject(a)
                val tp = ao.optString("action_type")
                if (tp.contains("purchase") || tp.contains("lead") || tp.contains("messaging_conversation_started")) {
                    rs += ao.optString("value", "0").toDoubleOrNull() ?: 0.0
                }
            }
            daily.add(o.optString("date_start") to x)
        }
        return Summary(sp, im, cl, rs, daily)
    }

    // ---------- Posts, comments ----------
    data class Post(val id: String, val message: String, val time: Long)
    data class Comment(val id: String, val postId: String, val text: String, val fromId: String, val time: Long)

    fun pagePosts(s: Store): List<Post> {
        val j = JSONObject(get(s, "${s.pageId}/posts", mapOf("fields" to "id,message,created_time", "limit" to "15"), PAGE))
        val a = j.optJSONArray("data") ?: return emptyList()
        return (0 until a.length()).map {
            val o = a.getJSONObject(it)
            Post(o.optString("id"), o.optString("message"), parseTime(o.optString("created_time")))
        }
    }

    /** scheduleUnix = null publishes now, otherwise schedules (between 10 minutes and 75 days ahead) */
    fun publishPost(s: Store, message: String, scheduleUnix: Long?): String {
        val p = mutableMapOf("message" to message)
        if (scheduleUnix != null) {
            p["published"] = "false"
            p["scheduled_publish_time"] = scheduleUnix.toString()
        }
        return JSONObject(post(s, "${s.pageId}/feed", p, PAGE)).optString("id")
    }

    fun comments(s: Store, postId: String): List<Comment> {
        val j = JSONObject(
            get(
                s,
                "$postId/comments",
                mapOf("fields" to "id,message,from,created_time", "order" to "reverse_chronological", "limit" to "10"),
                PAGE
            )
        )
        val a = j.optJSONArray("data") ?: return emptyList()
        return (0 until a.length()).map {
            val o = a.getJSONObject(it)
            Comment(
                o.optString("id"), postId, o.optString("message"),
                o.optJSONObject("from")?.optString("id") ?: "", parseTime(o.optString("created_time"))
            )
        }
    }

    fun replyComment(s: Store, commentId: String, text: String) {
        post(s, "$commentId/comments", mapOf("message" to text), PAGE)
    }

    fun hideComment(s: Store, commentId: String) {
        post(s, commentId, mapOf("is_hidden" to "true"), PAGE)
    }

    // ---------- Boost a post ----------
    /** Creates campaign, ad set, creative and ad, then activates it. Returns the campaign ID on success */
    fun boostPost(s: Store, postId: String, dailyMinor: Long, days: Int, countries: String, ageMin: Int, ageMax: Int): String {
        val a = acct(s)
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
        val start = System.currentTimeMillis() + 10 * 60 * 1000L
        val end = start + days * 24L * 3600 * 1000L

        val camp = JSONObject(
            post(
                s,
                "$a/campaigns",
                mapOf(
                    "name" to "Boost ${postId.takeLast(8)}",
                    "objective" to "OUTCOME_ENGAGEMENT",
                    "status" to "PAUSED",
                    "special_ad_categories" to "[]",
                    "daily_budget" to dailyMinor.toString(),
                    "bid_strategy" to "LOWEST_COST_WITHOUT_CAP"
                ),
                USER
            )
        ).getString("id")

        val codes = countries.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() }
        val targeting = JSONObject()
            .put("geo_locations", JSONObject().put("countries", org.json.JSONArray(codes)))
            .put("age_min", ageMin)
            .put("age_max", ageMax)

        val adset = JSONObject(
            post(
                s,
                "$a/adsets",
                mapOf(
                    "name" to "Boost Adset",
                    "campaign_id" to camp,
                    "billing_event" to "IMPRESSIONS",
                    "optimization_goal" to "POST_ENGAGEMENT",
                    "destination_type" to "ON_POST",
                    "targeting" to targeting.toString(),
                    "promoted_object" to JSONObject().put("page_id", s.pageId).toString(),
                    "start_time" to fmt.format(java.util.Date(start)),
                    "end_time" to fmt.format(java.util.Date(end)),
                    "status" to "ACTIVE"
                ),
                USER
            )
        ).getString("id")

        val creative = JSONObject(
            post(s, "$a/adcreatives", mapOf("name" to "Boost Creative", "object_story_id" to postId), USER)
        ).getString("id")

        post(
            s,
            "$a/ads",
            mapOf(
                "name" to "Boost Ad",
                "adset_id" to adset,
                "creative" to JSONObject().put("creative_id", creative).toString(),
                "status" to "ACTIVE"
            ),
            USER
        )
        post(s, camp, mapOf("status" to "ACTIVE"), USER)
        return camp
    }
}
