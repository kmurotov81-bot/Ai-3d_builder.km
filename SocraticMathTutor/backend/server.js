// Minimal secure proxy: Android app -> this server -> Gemini/Claude. No npm dependencies (Node 18+).
// The AI key lives ONLY in this server's environment. Run: node server.js
'use strict';
const http = require('http');

const LIMITS = { bodyBytes: 8 * 1024 * 1024, system: 30000, turns: 40, text: 12000, image: 4 * 1024 * 1024, maxTokens: 8000 };

function createServer(opts = {}) {
  const env = opts.env || process.env;
  const doFetch = opts.fetch || fetch;
  const hits = new Map(); // ip -> [timestamps]
  const perMinute = Number(env.RATE_LIMIT_PER_MIN || 30);

  function rateLimited(ip) {
    const now = Date.now();
    const arr = (hits.get(ip) || []).filter((t) => now - t < 60000);
    arr.push(now);
    hits.set(ip, arr);
    return arr.length > perMinute;
  }

  function validate(b) {
    if (!b || typeof b !== 'object') return 'body must be an object';
    if (typeof b.system !== 'string' || !b.system || b.system.length > LIMITS.system) return 'bad system';
    if (!Array.isArray(b.history) || b.history.length === 0 || b.history.length > LIMITS.turns) return 'bad history';
    for (const t of b.history) {
      if (!t || (t.role !== 'user' && t.role !== 'assistant')) return 'bad role';
      if (typeof t.text !== 'string' || !t.text || t.text.length > LIMITS.text) return 'bad text';
      if (t.imageB64 !== undefined && (typeof t.imageB64 !== 'string' || t.imageB64.length > LIMITS.image)) return 'bad image';
    }
    if (b.history[b.history.length - 1].role !== 'user') return 'last turn must be user';
    if (b.maxTokens !== undefined && (!Number.isInteger(b.maxTokens) || b.maxTokens < 1 || b.maxTokens > LIMITS.maxTokens)) return 'bad maxTokens';
    return null;
  }

  async function callGemini(b) {
    const model = env.GEMINI_MODEL || 'gemini-2.5-flash';
    const flash = model.includes('flash');
    const gen = { maxOutputTokens: (b.maxTokens || 2048) + (flash ? 1024 : 4096), responseMimeType: 'application/json' };
    if (flash) gen.thinkingConfig = { thinkingBudget: 1024 };
    const r = await doFetch(`https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent`, {
      method: 'POST',
      headers: { 'content-type': 'application/json', 'x-goog-api-key': env.GEMINI_API_KEY },
      body: JSON.stringify({
        systemInstruction: { parts: [{ text: b.system }] },
        contents: b.history.map((t) => ({
          role: t.role === 'assistant' ? 'model' : 'user',
          parts: [...(t.imageB64 ? [{ inline_data: { mime_type: 'image/jpeg', data: t.imageB64 } }] : []), { text: t.text }],
        })),
        generationConfig: gen,
      }),
    });
    const j = await r.json().catch(() => ({}));
    if (!r.ok) return { status: r.status, error: 'upstream_error' };
    const parts = (((j.candidates || [])[0] || {}).content || {}).parts || [];
    return { text: parts.map((p) => p.text || '').join('') };
  }

  async function callClaude(b) {
    const r = await doFetch('https://api.anthropic.com/v1/messages', {
      method: 'POST',
      headers: { 'content-type': 'application/json', 'x-api-key': env.ANTHROPIC_API_KEY, 'anthropic-version': '2023-06-01' },
      body: JSON.stringify({
        model: env.CLAUDE_MODEL || 'claude-sonnet-5-5',
        max_tokens: b.maxTokens || 2048,
        system: b.system,
        messages: b.history.map((t) => ({
          role: t.role,
          content: [
            ...(t.imageB64 ? [{ type: 'image', source: { type: 'base64', media_type: 'image/jpeg', data: t.imageB64 } }] : []),
            { type: 'text', text: t.text },
          ],
        })),
      }),
    });
    const j = await r.json().catch(() => ({}));
    if (!r.ok) return { status: r.status, error: 'upstream_error' };
    return { text: (j.content || []).filter((c) => c.type === 'text').map((c) => c.text).join('') };
  }

  // The tutor app needs a JSON object back; reject anything else so the client can retry.
  function looksLikeJson(text) {
    const s = text.indexOf('{'), e = text.lastIndexOf('}');
    if (s < 0 || e <= s) return false;
    try { JSON.parse(text.slice(s, e + 1)); return true; } catch { return false; }
  }

  return http.createServer((req, res) => {
    const send = (status, obj) => { res.writeHead(status, { 'content-type': 'application/json' }); res.end(JSON.stringify(obj)); };
    if (req.method === 'GET' && req.url === '/health') return send(200, { ok: true });
    if (req.method !== 'POST' || req.url !== '/v1/tutor') return send(404, { error: 'not_found' });
    if (env.APP_TOKEN && req.headers['x-app-token'] !== env.APP_TOKEN) return send(401, { error: 'unauthorized' });
    const ip = (req.headers['x-forwarded-for'] || req.socket.remoteAddress || '').toString().split(',')[0].trim();
    if (rateLimited(ip)) return send(429, { error: 'rate_limited' });

    let size = 0; const chunks = [];
    req.on('data', (c) => {
      size += c.length;
      if (size > LIMITS.bodyBytes) { send(413, { error: 'too_large' }); req.destroy(); return; }
      chunks.push(c);
    });
    req.on('end', async () => {
      if (res.writableEnded) return;
      let body;
      try { body = JSON.parse(Buffer.concat(chunks).toString('utf8')); } catch { return send(400, { error: 'bad_json' }); }
      const problem = validate(body);
      if (problem) return send(400, { error: 'invalid_request', detail: problem });
      const provider = (env.AI_PROVIDER || 'gemini').toLowerCase();
      const key = provider === 'claude' ? env.ANTHROPIC_API_KEY : env.GEMINI_API_KEY;
      if (!key) return send(500, { error: 'server_not_configured' });
      try {
        const out = await (provider === 'claude' ? callClaude(body) : callGemini(body));
        if (out.error) return send(out.status === 429 ? 429 : 502, { error: out.error });
        if (!out.text || !out.text.trim()) return send(502, { error: 'empty_ai_response' });
        // Test-generation replies use {"questions":..}; tutor replies use {"message":..}. Both are JSON objects.
        if (!looksLikeJson(out.text)) return send(502, { error: 'invalid_ai_response' });
        return send(200, { text: out.text });
      } catch (e) {
        return send(502, { error: 'upstream_unreachable' });
      }
    });
  });
}

if (require.main === module) {
  const port = Number(process.env.PORT || 8080);
  createServer().listen(port, () => console.log(`tutor backend listening on :${port}`));
}
module.exports = { createServer, LIMITS };
