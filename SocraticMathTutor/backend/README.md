# Tutor backend

Android app -> **this server** -> Gemini (or Claude). The AI key exists only here.

```
GEMINI_API_KEY=... APP_TOKEN=some-long-random-string node server.js
```

Environment: `GEMINI_API_KEY` or `ANTHROPIC_API_KEY`, `AI_PROVIDER` (`gemini` default / `claude`),
`GEMINI_MODEL`, `CLAUDE_MODEL`, `APP_TOKEN` (optional shared token), `RATE_LIMIT_PER_MIN` (default 30), `PORT`.

It validates the request (roles, sizes, image limits), rate-limits per IP, rejects AI output that is not a JSON
object (the app then retries), and never forwards upstream error text to clients. Test: `node --test`.
Deploy it anywhere that runs Node 18+ behind HTTPS, then build the app with `BACKEND_URL=https://your-host` (+ `APP_TOKEN`).
