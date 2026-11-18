import type { VercelRequest, VercelResponse } from "@vercel/node";
import { authorized } from "./_lib/auth";

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (!authorized(req, res)) return;
  if (req.method !== "POST") {
    res.status(405).json({ error: "Method not allowed" });
    return;
  }

  const key = process.env.GEMINI_API_KEY;
  if (!key) {
    res.status(500).json({ error: "GEMINI_API_KEY is not set on the server" });
    return;
  }
  const model = process.env.GEMINI_MODEL || "gemini-2.5-flash";

  const { system, messages, maxTokens } = (req.body ?? {}) as {
    system?: string;
    messages?: { role: string; content: string }[];
    maxTokens?: number;
  };

  if (!Array.isArray(messages) || messages.length === 0) {
    res.status(400).json({ error: "messages is required" });
    return;
  }

  const contents = messages.map((m) => ({
    role: m.role === "assistant" ? "model" : "user",
    parts: [{ text: String(m.content ?? "") }]
  }));

  const body: Record<string, unknown> = {
    contents,
    // extra room because newer Gemini models may use tokens for thinking
    generationConfig: { maxOutputTokens: Math.max(Number(maxTokens) * 4 || 0, 2048) }
  };
  if (system) body.systemInstruction = { parts: [{ text: String(system) }] };

  const r = await fetch(
    `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json", "x-goog-api-key": key },
      body: JSON.stringify(body)
    }
  );

  const j: any = await r.json().catch(() => ({}));
  if (!r.ok) {
    res.status(r.status).json({ error: j?.error?.message ?? "Gemini request failed" });
    return;
  }

  const parts: any[] = j?.candidates?.[0]?.content?.parts ?? [];
  const reply = parts.map((p) => p?.text ?? "").join("").trim();
  if (!reply) {
    res.status(502).json({ error: "Gemini returned an empty reply" });
    return;
  }
  res.status(200).json({ reply });
}
