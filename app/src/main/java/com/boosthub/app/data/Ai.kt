package com.boosthub.app.data

import org.json.JSONArray
import org.json.JSONObject

/** AI requests go to your Vercel server, which calls Gemini with the key stored there. */
object Ai {
    fun call(s: Store, system: String, msgs: List<Pair<String, String>>, maxTokens: Int = 1024): String {
        val arr = JSONArray()
        msgs.forEach { arr.put(JSONObject().put("role", it.first).put("content", it.second)) }
        val body = JSONObject()
            .put("system", system)
            .put("messages", arr)
            .put("maxTokens", maxTokens)
        return JSONObject(Backend.post(s, "ai", body)).optString("reply").trim()
    }

    fun reply(s: Store, transcript: String): String {
        val system = """You are the customer support assistant for a business's Facebook Page.
Business info and rules from the owner:
${s.bizInfo.ifBlank { "(none provided)" }}

Rules:
- Reply in the same language as the customer (Bengali or English).
- Keep it short, friendly and helpful.
- Never invent prices, stock, delivery times, policies or promises that are not in the business info.
- If the customer asks whether you are a bot, say you are an automated assistant.
- If you cannot answer confidently, the customer is angry, or they mention refund, complaint, legal issue, or need a human: output exactly HANDOFF
- Output ONLY the reply text (or HANDOFF)."""
        return call(s, system, listOf("user" to "Conversation so far:\n$transcript\n\nWrite the next Page reply."), 500)
    }

    fun analyze(s: Store, dataJson: String): String {
        val system = "You are an expert Facebook ads media buyer. Answer in English, short and practical."
        return call(
            s, system,
            listOf("user" to "Last 7 days campaign data (money in minor units for budgets, spend in main currency):\n$dataJson\n\nGive a short analysis: what is working, what is wasting money, and 3 concrete next actions."),
            900
        )
    }

    fun decisions(s: Store, dataJson: String, maxActions: Int, pct: Int): String {
        val system = """You are an expert, conservative Facebook ads media buyer managing campaigns automatically.
Return ONLY a JSON array (no other text) of at most $maxActions actions. Each action:
{"type":"pause"|"resume"|"set_budget","campaign_id":"...","new_daily_budget":<integer minor units, only for set_budget>,"reason":"<short English reason>"}
Rules: be conservative. Do not pause campaigns with fewer than 1000 impressions. Budget changes must stay within ${pct}% of the current daily budget. Prefer pausing clear money-wasters (spend with zero results) and gently scaling clear winners. Return [] if nothing needs to change."""
        return call(s, system, listOf("user" to "Campaign data (last 7 days):\n$dataJson"), 800)
    }

    fun comment(s: Store, text: String): String {
        val system = """You moderate and reply to comments on a business's Facebook post.
Business info from the owner:
${s.bizInfo.ifBlank { "(none provided)" }}

Return ONLY a JSON object: {"action":"reply"|"hide"|"ignore","text":"<reply>"}
- hide = spam, scam links, abuse, hate.
- reply = genuine questions or positive comments. Reply in the commenter's language, max 2 short sentences. If price/order details are needed and not in the business info, invite them to message the Page inbox. Never invent prices or policies.
- ignore = anything else.
"text" is only needed when action is reply."""
        return call(s, system, listOf("user" to "Comment: $text"), 300)
    }

    fun writePost(s: Store, idea: String): String {
        val system = """You write engaging Facebook post captions for a business. Write in the same language as the idea.
Business info:
${s.bizInfo.ifBlank { "(none provided)" }}
Output ONLY the final post text: a hook, short body, a few emojis and a clear call to action. Under 120 words. Do not invent prices or offers that are not given."""
        return call(s, system, listOf("user" to idea), 600)
    }

    fun adCopy(s: Store, idea: String): String {
        val system = """You are an expert Facebook ads copywriter. Write in the same language as the idea.
Business info:
${s.bizInfo.ifBlank { "(none provided)" }}
Write 3 different ad copy variations. For each: Headline (max 40 characters) and Primary text (max 125 characters). Numbered list. Do not invent prices or claims that are not given."""
        return call(s, system, listOf("user" to idea), 700)
    }
}
