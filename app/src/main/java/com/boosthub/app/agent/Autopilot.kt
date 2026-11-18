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
        if (!manual && !s.autopilot) return@withContext "অটোপাইলট বন্ধ"
        if (s.aiKey.isBlank() || s.fbToken.isBlank()) {
            s.log("⚠️ সেটিংসে ফেসবুক টোকেন ও AI কী দিন")
            return@withContext "কী দেওয়া হয়নি"
        }
        val sb = StringBuilder()
        if (s.autoReply && s.pageId.isNotBlank()) {
            try { sb.appendLine(handleMessages(ctx, s)) } catch (e: Exception) { s.log("❌ মেসেজ এরর: ${e.message}"); sb.appendLine("মেসেজ এরর") }
        }
        if (s.autoComment && s.pageId.isNotBlank()) {
            try { sb.appendLine(handleComments(ctx, s)) } catch (e: Exception) { s.log("❌ কমেন্ট এরর: ${e.message}"); sb.appendLine("কমেন্ট এরর") }
        }
        if (s.adsOpt && s.adAccount.isNotBlank()) {
            try { sb.appendLine(handleAds(ctx, s)) } catch (e: Exception) { s.log("❌ অ্যাডস এরর: ${e.message}"); sb.appendLine("অ্যাডস এরর") }
        }
        sb.toString().trim().ifBlank { "কিছু করার ছিল না" }
    }

    private fun handleMessages(ctx: Context, s: Store): String {
        val convs = MetaApi.conversations(s)
        var replied = 0
        val now = System.currentTimeMillis()
        for (c in convs) {
            if (replied >= s.maxReplies) break
            if (c.lastFromPage || c.psid.isBlank() || s.isDone(c.lastMsgId)) continue
            if (now - c.lastTime > 23L * 3600 * 1000) { s.markDone(c.lastMsgId); continue } // মেসেঞ্জারের ২৪ ঘণ্টার নিয়ম
            val reply = Ai.reply(s, c.transcript)
            if (reply.isBlank() || reply.contains("HANDOFF")) {
                s.log("🙋 আপনার নজর দরকার: ${c.name} — \"${c.lastText.take(60)}\"")
                Notifier.send(ctx, "🙋 আপনার নজর দরকার", "${c.name}: ${c.lastText.take(100)}")
                s.markDone(c.lastMsgId)
                continue
            }
            MetaApi.sendMessage(s, c.psid, reply)
            s.markDone(c.lastMsgId)
            s.log("💬 ${c.name} কে উত্তর: ${reply.take(80)}")
            replied++
        }
        if (replied > 0) Notifier.send(ctx, "BoostHub", "$replied টি মেসেজের উত্তর দেওয়া হয়েছে")
        return "মেসেজ: $replied টি উত্তর দেওয়া হয়েছে"
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
                        s.log("💬 কমেন্টে উত্তর: ${txt.take(70)}")
                        n++
                    }
                    "hide" -> {
                        if (s.autoApply) {
                            MetaApi.hideComment(s, c.id)
                            s.log("🙈 কমেন্ট হাইড: ${c.text.take(50)}")
                            n++
                        } else s.log("💡 সাজেশন (হাইড): ${c.text.take(50)}")
                    }
                }
            }
        }
        if (n > 0) Notifier.send(ctx, "BoostHub", "কমেন্টে $n টি অ্যাকশন নেওয়া হয়েছে")
        return "কমেন্ট: $n টি অ্যাকশন"
    }

    private fun handleAds(ctx: Context, s: Store): String {
        val camps = MetaApi.campaigns(s)
        if (camps.isEmpty()) return "অ্যাডস: কোনো ক্যাম্পেইন নেই"
        val ins = MetaApi.insights(s)
        val text = Ai.decisions(s, dataJson(camps, ins), s.maxActions, s.maxChangePct)
        return applyDecisions(ctx, s, camps, text)
    }

    private fun applyDecisions(ctx: Context, s: Store, camps: List<MetaApi.Campaign>, text: String): String {
        val a = text.indexOf('[')
        val b = text.lastIndexOf(']')
        if (a < 0 || b < a) return "অ্যাডস: কোনো অ্যাকশন আসেনি"
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
                        s.log("⏸ বন্ধ করা হয়েছে: ${c.name} — $reason")
                        Notifier.send(ctx, "⏸ ক্যাম্পেইন বন্ধ", "${c.name}: $reason")
                    } else s.log("💡 সাজেশন (বন্ধ): ${c.name} — $reason")
                    done++
                }
                "resume" -> {
                    if (c.status != "PAUSED") continue
                    if (cap <= 0 || total + c.dailyBudget > cap) { s.log("🚫 চালু করা হয়নি (বাজেট সীমা): ${c.name}"); continue }
                    if (s.autoApply) {
                        MetaApi.setStatus(s, c.id, "ACTIVE"); total += c.dailyBudget
                        s.log("▶️ চালু করা হয়েছে: ${c.name} — $reason")
                        Notifier.send(ctx, "▶️ ক্যাম্পেইন চালু", "${c.name}: $reason")
                    } else s.log("💡 সাজেশন (চালু): ${c.name} — $reason")
                    done++
                }
                "set_budget" -> {
                    val nb = o.optLong("new_daily_budget")
                    if (c.dailyBudget <= 0 || nb <= 0) continue
                    val lo = c.dailyBudget * (100 - s.maxChangePct) / 100
                    val hi = c.dailyBudget * (100 + s.maxChangePct) / 100
                    if (nb < lo || nb > hi) { s.log("🚫 বাজেট বদল বাতিল (সীমার বাইরে): ${c.name}"); continue }
                    if (nb > c.dailyBudget && (cap <= 0 || total - c.dailyBudget + nb > cap)) {
                        s.log("🚫 বাজেট বাড়ানো বাতিল (দৈনিক সীমা): ${c.name}"); continue
                    }
                    if (s.autoApply) {
                        MetaApi.setBudget(s, c.id, nb); total += nb - c.dailyBudget
                        s.log("💰 বাজেট ${c.dailyBudget / 100} → ${nb / 100}: ${c.name} — $reason")
                        Notifier.send(ctx, "💰 বাজেট বদল", "${c.name}: ${c.dailyBudget / 100} → ${nb / 100}")
                    } else s.log("💡 সাজেশন (বাজেট ${nb / 100}): ${c.name} — $reason")
                    done++
                }
            }
        }
        return "অ্যাডস: $done টি অ্যাকশন"
    }
}
