import type { VercelRequest, VercelResponse } from "@vercel/node";
import { authorized } from "./_lib/auth";

const GRAPH = "https://graph.facebook.com/v21.0";
let pageTokenCache: string | null = null;

async function getPageToken(userToken: string, pageId: string): Promise<string> {
  if (pageTokenCache) return pageTokenCache;
  try {
    const r = await fetch(
      `${GRAPH}/${pageId}?fields=access_token&access_token=${encodeURIComponent(userToken)}`
    );
    const j: any = await r.json();
    if (j && j.access_token) {
      pageTokenCache = String(j.access_token);
      return pageTokenCache;
    }
  } catch {
    // fall back to the user token
  }
  return userToken;
}

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (!authorized(req, res)) return;
  if (req.method !== "POST") {
    res.status(405).json({ error: "Method not allowed" });
    return;
  }

  const userToken = process.env.FB_ACCESS_TOKEN;
  const pageId = process.env.FB_PAGE_ID;
  if (!userToken) {
    res.status(500).json({ error: "FB_ACCESS_TOKEN is not set on the server" });
    return;
  }

  const { method, path, params, as } = (req.body ?? {}) as {
    method?: string;
    path?: string;
    params?: Record<string, unknown>;
    as?: string;
  };

  if (method !== "GET" && method !== "POST") {
    res.status(400).json({ error: "method must be GET or POST" });
    return;
  }
  if (typeof path !== "string" || !/^[A-Za-z0-9_.\-\/]+$/.test(path) || path.includes("..")) {
    res.status(400).json({ error: "Invalid path" });
    return;
  }

  const token = as === "page" && pageId ? await getPageToken(userToken, pageId) : userToken;

  const p = new URLSearchParams();
  for (const [k, v] of Object.entries(params ?? {})) {
    if (k === "access_token") continue;
    p.set(k, String(v));
  }
  p.set("access_token", token);

  const url = `${GRAPH}/${path}`;
  const r =
    method === "GET"
      ? await fetch(`${url}?${p.toString()}`)
      : await fetch(url, {
          method: "POST",
          headers: { "Content-Type": "application/x-www-form-urlencoded" },
          body: p.toString()
        });

  const text = await r.text();
  res.status(r.status).setHeader("Content-Type", "application/json").send(text);
}
