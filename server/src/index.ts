import express, { Request, Response } from "express";
import cors from "cors";

const app = express();

app.use(cors());
app.use(express.json({ limit: "1mb" }));

app.post("/api/ai/chat", async (req: Request, res: Response) => {
  try {
    const { message, system, model } = req.body;

    if (!message || typeof message !== "string") {
      return res.status(400).json({ error: "Message is required" });
    }

    const anthropicKey = process.env.ANTHROPIC_API_KEY;

    if (!anthropicKey) {
      return res.status(500).json({ error: "Server API key is not configured" });
    }

    const response = await fetch("https://api.anthropic.com/v1/messages", {
      method: "POST",
      headers: {
        "x-api-key": anthropicKey,
        "anthropic-version": "2023-06-01",
        "content-type": "application/json"
      },
      body: JSON.stringify({
        model: model ?? "claude-sonnet-4-20250514",
        max_tokens: 1024,
        system: system ?? "You are a helpful AI assistant.",
        messages: [{ role: "user", content: message }]
      })
    });

    const result = await response.json();

    if (!response.ok) {
      return res.status(response.status).json({
        error: "AI provider request failed",
        details: result
      });
    }

    const reply =
      result.content
        ?.filter((item: any) => item.type === "text")
        .map((item: any) => item.text)
        .join("") ?? "";

    return res.json({ reply });
  } catch (error: any) {
    console.error(error);
    return res.status(500).json({ error: "Internal server error" });
  }
});

app.get("/api/health", (_req, _reqRes) => {
  _reqRes.json({ status: "ok" });
});

const port = Number(process.env.PORT ?? 3000);
app.listen(port, () => {
  console.log(`Server running on port ${port}`);
});
