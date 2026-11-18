import type { VercelRequest, VercelResponse } from "@vercel/node";
import { timingSafeEqual } from "crypto";

export function authorized(req: VercelRequest, res: VercelResponse): boolean {
  const expected = process.env.APP_ACCESS_CODE;
  if (!expected) {
    res.status(500).json({ error: "APP_ACCESS_CODE is not set on the server" });
    return false;
  }
  const given = String(req.headers["x-access-code"] ?? "");
  const a = Buffer.from(given);
  const b = Buffer.from(expected);
  if (a.length !== b.length || !timingSafeEqual(a, b)) {
    res.status(401).json({ error: "Invalid access code" });
    return false;
  }
  return true;
}
