import express from 'express';
import cors from 'cors';
import { GoogleGenAI } from '@google/genai';

const app = express();
app.use(cors());
app.use(express.json());

// Gemini API ক্লাইন্ট ইনিশিয়ালাইজেশন (Vercel Environment Variable থেকে GEMINI_API_KEY নেবে)
const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });

app.post('/api/ai/chat', async (req, res) => {
  try {
    const { message, system } = req.body;

    const response = await ai.models.generateContent({
      model: 'gemini-1.5-flash',
      contents: message,
      config: {
        systemInstruction: system || 'You are a helpful assistant.',
      },
    });

    res.json({ reply: response.text });
  } catch (error: any) {
    console.error(error);
    res.status(500).json({ error: error.message || 'Something went wrong' });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
