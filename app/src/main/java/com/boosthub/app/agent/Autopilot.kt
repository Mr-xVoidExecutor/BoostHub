package com.boosthub.app.agent

import android.content.Context
import com.boosthub.app.data.Ai
import com.boosthub.app.data.MetaApi
import com.boosthub.app.data.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object Autopilot {

    fun dataJson(camps: List<MetaApi.Campaign>, ins: Map<String, MetaApi.Insight>): String {
        val a = JSONArray()
        camps.forEach { c ->
            val i = ins[c.id]
            a.put(
                JSONObject()
                    .put("campaign_id", c.id)
                    .put("name", c.name)
                    .put("status", c.status)
                    .put("daily_budget_minor_units", c.dailyBudget)
                    .put("spend", i?.spend ?: 0.0)
                    .put("impressions", i?.impressions ?: 0L)
                    .put("clicks", i?.clicks ?: 0L)
                    .put("ctr", i?.ctr ?: 0.0)
                    .put("cpc", i?.cpc ?: 0.0)
                    .put("results", i?.results ?: 0.0)
            )
        }
        return a.toString()
    }

    suspend fun runOnce(ctx: Context, manual: Boolean = false): String = withContext(Dispatchers.IO) {
        val s = Store(ctx)
        if (!manual && !s.autopilot) return@withContext "Autopilot is off"
        if (!s.aiReady || !s.fbReady) {
            s.log("Server not connected. Open Settings and connect your server.")
            return@withContext "Server not connected"
        }
        val sb = StringBuilder()
        if (s.autoReply && s.pageId.isNotBlank()) {
            try { sb.appendLine(handleMessages(ctx, s)) } catch (e: Exception) { s.log("Messages error: ${e.message}"); sb.appendLine("Messages error") }
        }
        if (s.autoComment && s.pageId.isNotBlank()) {
            try { sb.appendLine(handleComments(ctx, s)) } catch (e: Exception) { s.log("Comments error: ${e.message}"); sb.appendLine("Comments error") }
        }
        if (s.adsOpt && s.adAccount.isNotBlank()) {
            try { sb.appendLine(handleAds(ctx, s)) } catch (e: Exception) { s.log("Ads error: ${e.message}"); sb.appendLine("Ads error") }
        }
        sb.toString().trim().ifBlank { "Nothing to do" }
    }

    private fun handleMessages(ctx: Context, s: Store): String {
        val convs = MetaApi.conversations(s)
        var replied = 0
        val now = System.currentTimeMillis()
        for (c in convs) {
            if (replied >= s.maxReplies) break
            if (c.lastFromPage || c.psid.isBlank() || s.isDone(c.lastMsgId)) continue
            if (now - c.lastTime > 23L * 3600 * 1000) { s.markDone(c.lastMsgId); continue } // Messenger 24-hour rule
            val reply = Ai.reply(s, c.transcript)
            if (reply.isBlank() || reply.contains("HANDOFF")) {
                s.log("Needs your attention: ${c.name} - \"${c.lastText.take(60)}\"")
                Notifier.send(ctx, "Needs your attention", "${c.name}: ${c.lastText.take(100)}")
                s.markDone(c.lastMsgId)
                continue
            }
            MetaApi.sendMessage(s, c.psid, reply)
            s.markDone(c.lastMsgId)
            s.log("Replied to ${c.name}: ${reply.take(80)}")
            replied++
        }
        if (replied > 0) Notifier.send(ctx, "BoostHub", "$replied message(s) answered")
        return "Messages: $replied replied"
    }

    private fun handleComments(ctx: Context, s: Store): String {
        val posts = MetaApi.pagePosts(s).take(5)
        var n = 0
        val now = System.currentTimeMillis()
        for (p in posts) {
            for (c in MetaApi.comments(s, p.id)) {
                if (n >= s.maxReplies) break
                if (c.fromId == s.pageId || s.isDone(c.id)) continue
                s.markDone(c.id)
                if (now - c.time > 48L * 3600 * 1000) continue
                val raw = Ai.comment(s, c.text)
                val a = raw.indexOf('{')
                val b = raw.lastIndexOf('}')
                if (a < 0 || b < a) continue
                val o = try { JSONObject(raw.substring(a, b + 1)) } catch (e: Exception) { continue }
                val txt = o.optString("text")
                when (o.optString("action")) {
                    "reply" -> if (txt.isNotBlank()) {
                        MetaApi.replyComment(s, c.id, txt)
                        s.log("Replied to a comment: ${txt.take(70)}")
                        n++
                    }
                    "hide" -> {
                        if (s.autoApply) {
                            MetaApi.hideComment(s, c.id)
                            s.log("Hid a comment: ${c.text.take(50)}")
                            n++
                        } else s.log("Suggestion (hide): ${c.text.take(50)}")
                    }
                }
            }
        }
        if (n > 0) Notifier.send(ctx, "BoostHub", "$n comment action(s) taken")
        return "Comments: $n action(s)"
    }

    private fun handleAds(ctx: Context, s: Store): String {
        val camps = MetaApi.campaigns(s)
        if (camps.isEmpty()) return "Ads: no campaigns"
        val ins = MetaApi.insights(s)
        val text = Ai.decisions(s, dataJson(camps, ins), s.maxActions, s.maxChangePct)
        return applyDecisions(ctx, s, camps, text)
    }

    private fun applyDecisions(ctx: Context, s: Store, camps: List<MetaApi.Campaign>, text: String): String {
        val a = text.indexOf('[')
        val b = text.lastIndexOf(']')
        if (a < 0 || b < a) return "Ads: no actions returned"
        val arr = JSONArray(text.substring(a, b + 1))
        val byId = camps.associateBy { it.id }
        val cap = s.dailyCap * 100L
        var total = camps.filter { it.status == "ACTIVE" }.sumOf { it.dailyBudget }
        var done = 0
        for (k in 0 until arr.length()) {
            if (done >= s.maxActions) break
            val o = arr.getJSONObject(k)
            val c = byId[o.optString("campaign_id")] ?: continue
            val reason = o.optString("reason")
            when (o.optString("type")) {
                "pause" -> {
                    if (c.status != "ACTIVE") continue
                    if (s.autoApply) {
                        MetaApi.setStatus(s, c.id, "PAUSED"); total -= c.dailyBudget
                        s.log("Paused: ${c.name} - $reason")
                        Notifier.send(ctx, "Campaign paused", "${c.name}: $reason")
                    } else s.log("Suggestion (pause): ${c.name} - $reason")
                    done++
                }
                "resume" -> {
                    if (c.status != "PAUSED") continue
                    if (cap <= 0 || total + c.dailyBudget > cap) { s.log("Not resumed (budget limit): ${c.name}"); continue }
                    if (s.autoApply) {
                        MetaApi.setStatus(s, c.id, "ACTIVE"); total += c.dailyBudget
                        s.log("Resumed: ${c.name} - $reason")
                        Notifier.send(ctx, "Campaign resumed", "${c.name}: $reason")
                    } else s.log("Suggestion (resume): ${c.name} - $reason")
                    done++
                }
                "set_budget" -> {
                    val nb = o.optLong("new_daily_budget")
                    if (c.dailyBudget <= 0 || nb <= 0) continue
                    val lo = c.dailyBudget * (100 - s.maxChangePct) / 100
                    val hi = c.dailyBudget * (100 + s.maxChangePct) / 100
                    if (nb < lo || nb > hi) { s.log("Budget change cancelled (outside limit): ${c.name}"); continue }
                    if (nb > c.dailyBudget && (cap <= 0 || total - c.dailyBudget + nb > cap)) {
                        s.log("Budget increase cancelled (daily cap): ${c.name}"); continue
                    }
                    if (s.autoApply) {
                        MetaApi.setBudget(s, c.id, nb); total += nb - c.dailyBudget
                        s.log("Budget ${c.dailyBudget / 100} -> ${nb / 100}: ${c.name} - $reason")
                        Notifier.send(ctx, "Budget changed", "${c.name}: ${c.dailyBudget / 100} -> ${nb / 100}")
                    } else s.log("Suggestion (budget ${nb / 100}): ${c.name} - $reason")
                    done++
                }
            }
        }
        return "Ads: $done action(s)"
    }
}
