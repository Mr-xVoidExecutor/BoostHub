package com.boosthub.app.data

import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale

object MetaApi {
    private const val V = "https://graph.facebook.com/v21.0"

    private fun enc(x: String) = URLEncoder.encode(x, "UTF-8")

    private fun get(path: String, params: Map<String, String>, token: String): String {
        val q = (params + ("access_token" to token)).entries.joinToString("&") { "${enc(it.key)}=${enc(it.value)}" }
        return Http.request("GET", "$V/$path?$q", emptyMap(), null)
    }

    private fun post(path: String, params: Map<String, String>, token: String): String {
        val body = (params + ("access_token" to token)).entries.joinToString("&") { "${enc(it.key)}=${enc(it.value)}" }
        return Http.request("POST", "$V/$path", mapOf("Content-Type" to "application/x-www-form-urlencoded"), body)
    }

    private fun acct(s: Store) = if (s.adAccount.startsWith("act_")) s.adAccount else "act_${s.adAccount}"

    // ---------- Ads ----------
    data class Campaign(val id: String, val name: String, val status: String, val dailyBudget: Long)
    data class Insight(val spend: Double, val impressions: Long, val clicks: Long, val ctr: Double, val cpc: Double, val results: Double)

    fun campaigns(s: Store): List<Campaign> {
        val j = JSONObject(get("${acct(s)}/campaigns", mapOf("fields" to "id,name,effective_status,daily_budget", "limit" to "50"), s.fbToken))
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
                "${acct(s)}/insights",
                mapOf("level" to "campaign", "fields" to "campaign_id,spend,impressions,clicks,ctr,cpc,actions", "date_preset" to "last_7d", "limit" to "100"),
                s.fbToken
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
        post(id, mapOf("status" to status), s.fbToken)
    }

    fun setBudget(s: Store, id: String, minorUnits: Long) {
        post(id, mapOf("daily_budget" to minorUnits.toString()), s.fbToken)
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

    fun pageToken(s: Store): String {
        if (s.pageToken.isNotBlank()) return s.pageToken
        return try {
            val j = JSONObject(get(s.pageId, mapOf("fields" to "access_token"), s.fbToken))
            val t = j.optString("access_token")
            if (t.isNotBlank()) { s.pageToken = t; t } else s.fbToken
        } catch (e: Exception) {
            s.fbToken
        }
    }

    private fun parseTime(x: String): Long =
        try { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).parse(x)?.time ?: 0L } catch (e: Exception) { 0L }

    fun conversations(s: Store): List<Conv> {
        val t = pageToken(s)
        val j = JSONObject(
            get(
                "${s.pageId}/conversations",
                mapOf("platform" to "messenger", "fields" to "id,updated_time,messages.limit(6){id,message,from,created_time}", "limit" to "15"),
                t
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
            "${s.pageId}/messages",
            mapOf(
                "recipient" to JSONObject().put("id", psid).toString(),
                "message" to JSONObject().put("text", text).toString(),
                "messaging_type" to "RESPONSE"
            ),
            pageToken(s)
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
                acct(s),
                mapOf("fields" to "name,account_id,currency,balance,amount_spent,spend_cap,account_status,funding_source_details"),
                s.fbToken
            )
        )
        var bizName = ""; var bizAddr = ""; var taxId = ""
        try {
            val b = JSONObject(
                get(
                    acct(s),
                    mapOf("fields" to "business_name,business_street,business_city,business_zip,business_country_code,tax_id"),
                    s.fbToken
                )
            )
            bizName = b.optString("business_name")
            bizAddr = listOf(b.optString("business_street"), b.optString("business_city"), b.optString("business_zip"), b.optString("business_country_code"))
                .filter { it.isNotBlank() }.joinToString(", ")
            taxId = b.optString("tax_id")
        } catch (e: Exception) { }
        var prepay = 0L
        try {
            val p = JSONObject(get(acct(s), mapOf("fields" to "total_prepay_balance"), s.fbToken))
            prepay = p.optJSONObject("total_prepay_balance")?.optString("amount_in_hundredths")?.toLongOrNull() ?: 0L
        } catch (e: Exception) { }
        var threshold = 0L
        try {
            val d = JSONObject(get("${acct(s)}/adspaymentcycle", mapOf("fields" to "threshold_amount"), s.fbToken)).optJSONArray("data")
            if (d != null && d.length() > 0) threshold = d.getJSONObject(0).optString("threshold_amount").toLongOrNull() ?: 0L
        } catch (e: Exception) { }
        val daily = mutableListOf<Pair<String, Double>>()
        try {
            val d = JSONObject(
                get(acct(s), mapOf("fields" to "spend", "time_increment" to "1", "date_preset" to "last_7d", "level" to "account"), s.fbToken)
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

    /** অ্যাকাউন্ট স্পেন্ডিং লিমিট (মাইনর ইউনিটে). 0 = সীমা তুলে দেওয়া */
    fun setSpendCap(s: Store, minorUnits: Long) {
        post(acct(s), mapOf("spend_cap" to minorUnits.toString()), s.fbToken)
    }

    fun billingUrl(s: Store): String {
        val id = s.adAccount.removePrefix("act_")
        return "https://business.facebook.com/billing_hub/payment_settings?asset_id=$id"
    }

    fun statusText(code: Int): String = when (code) {
        1 -> "সক্রিয়"
        2 -> "বন্ধ (Disabled)"
        3 -> "বকেয়া আছে (Unsettled)"
        7 -> "রিভিউয়ে আছে"
        9 -> "গ্রেস পিরিয়ড"
        100 -> "বন্ধ হওয়ার অপেক্ষায়"
        101 -> "বন্ধ"
        else -> "কোড $code"
    }

    // ---------- Dashboard ----------
    data class Summary(val spend: Double, val impressions: Long, val clicks: Long, val results: Double, val daily: List<Pair<String, Double>>)

    fun summary(s: Store): Summary {
        val d = JSONObject(
            get(
                acct(s),
                mapOf("fields" to "spend,impressions,clicks,actions", "time_increment" to "1", "date_preset" to "last_7d", "level" to "account"),
                s.fbToken
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
        val j = JSONObject(get("${s.pageId}/posts", mapOf("fields" to "id,message,created_time", "limit" to "15"), pageToken(s)))
        val a = j.optJSONArray("data") ?: return emptyList()
        return (0 until a.length()).map {
            val o = a.getJSONObject(it)
            Post(o.optString("id"), o.optString("message"), parseTime(o.optString("created_time")))
        }
    }

    /** scheduleUnix = null হলে এখনই পাবলিশ, নইলে শিডিউল (১০ মিনিট থেকে ৭৫ দিনের মধ্যে) */
    fun publishPost(s: Store, message: String, scheduleUnix: Long?): String {
        val p = mutableMapOf("message" to message)
        if (scheduleUnix != null) {
            p["published"] = "false"
            p["scheduled_publish_time"] = scheduleUnix.toString()
        }
        return JSONObject(post("${s.pageId}/feed", p, pageToken(s))).optString("id")
    }

    fun comments(s: Store, postId: String): List<Comment> {
        val j = JSONObject(
            get(
                "$postId/comments",
                mapOf("fields" to "id,message,from,created_time", "order" to "reverse_chronological", "limit" to "10"),
                pageToken(s)
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
        post("$commentId/comments", mapOf("message" to text), pageToken(s))
    }

    fun hideComment(s: Store, commentId: String) {
        post(commentId, mapOf("is_hidden" to "true"), pageToken(s))
    }

    // ---------- Boost a post ----------
    /** ক্যাম্পেইন → অ্যাডসেট → ক্রিয়েটিভ → অ্যাড বানিয়ে শেষে চালু করে। সফল হলে ক্যাম্পেইন আইডি দেয় */
    fun boostPost(s: Store, postId: String, dailyMinor: Long, days: Int, countries: String, ageMin: Int, ageMax: Int): String {
        val a = acct(s)
        val t = s.fbToken
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
        val start = System.currentTimeMillis() + 10 * 60 * 1000L
        val end = start + days * 24L * 3600 * 1000L

        val camp = JSONObject(
            post(
                "$a/campaigns",
                mapOf(
                    "name" to "Boost ${postId.takeLast(8)}",
                    "objective" to "OUTCOME_ENGAGEMENT",
                    "status" to "PAUSED",
                    "special_ad_categories" to "[]",
                    "daily_budget" to dailyMinor.toString(),
                    "bid_strategy" to "LOWEST_COST_WITHOUT_CAP"
                ),
                t
            )
        ).getString("id")

        val codes = countries.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() }
        val targeting = JSONObject()
            .put("geo_locations", JSONObject().put("countries", org.json.JSONArray(codes)))
            .put("age_min", ageMin)
            .put("age_max", ageMax)

        val adset = JSONObject(
            post(
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
                t
            )
        ).getString("id")

        val creative = JSONObject(
            post("$a/adcreatives", mapOf("name" to "Boost Creative", "object_story_id" to postId), t)
        ).getString("id")

        post(
            "$a/ads",
            mapOf(
                "name" to "Boost Ad",
                "adset_id" to adset,
                "creative" to JSONObject().put("creative_id", creative).toString(),
                "status" to "ACTIVE"
            ),
            t
        )
        post(camp, mapOf("status" to "ACTIVE"), t)
        return camp
    }
}

