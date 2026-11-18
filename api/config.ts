import type { VercelRequest, VercelResponse } from "@vercel/node";
import { authorized } from "./_lib/auth";

export default function handler(req: VercelRequest, res: VercelResponse) {
  if (!authorized(req, res)) return;
  res.status(200).json({
    facebook: Boolean(process.env.FB_ACCESS_TOKEN),
    adAccountId: process.env.FB_AD_ACCOUNT_ID ?? "",
    pageId: process.env.FB_PAGE_ID ?? "",
    ai: Boolean(process.env.GEMINI_API_KEY),
    model: process.env.GEMINI_MODEL || "gemini-2.5-flash"
  });
}
