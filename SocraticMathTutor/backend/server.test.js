'use strict';
const test = require('node:test');
const assert = require('node:assert');
const { createServer } = require('./server');

function start(env, fetchImpl) {
  const server = createServer({ env, fetch: fetchImpl });
  return new Promise((resolve) => server.listen(0, () => resolve(server)));
}
async function post(server, body, headers = {}) {
  const r = await fetch(`http://127.0.0.1:${server.address().port}/v1/tutor`, {
    method: 'POST', headers: { 'content-type': 'application/json', ...headers },
    body: typeof body === 'string' ? body : JSON.stringify(body),
  });
  return { status: r.status, json: await r.json() };
}
const ok = { system: 's', history: [{ role: 'user', text: 'hi' }] };
const geminiReply = (text) => async () => ({ ok: true, status: 200, json: async () => ({ candidates: [{ content: { parts: [{ text }] } }] }) });

test('forwards a valid request to Gemini and returns text', async () => {
  let seen;
  const s = await start({ GEMINI_API_KEY: 'k' }, async (url, init) => { seen = { url, init }; return geminiReply('{"message":"hi"}')(); });
  const r = await post(s, ok);
  assert.strictEqual(r.status, 200);
  assert.strictEqual(r.json.text, '{"message":"hi"}');
  assert.strictEqual(seen.init.headers['x-goog-api-key'], 'k');
  assert.ok(!JSON.stringify(r.json).includes('"k"'));
  s.close();
});

test('rejects invalid input', async () => {
  const s = await start({ GEMINI_API_KEY: 'k' }, geminiReply('{}'));
  assert.strictEqual((await post(s, { system: 's', history: [] })).status, 400);
  assert.strictEqual((await post(s, { system: 's', history: [{ role: 'assistant', text: 'x' }] })).status, 400);
  assert.strictEqual((await post(s, 'not json')).status, 400);
  s.close();
});

test('requires app token when configured', async () => {
  const s = await start({ GEMINI_API_KEY: 'k', APP_TOKEN: 't' }, geminiReply('{"a":1}'));
  assert.strictEqual((await post(s, ok)).status, 401);
  assert.strictEqual((await post(s, ok, { 'x-app-token': 't' })).status, 200);
  s.close();
});

test('rate limits per client', async () => {
  const s = await start({ GEMINI_API_KEY: 'k', RATE_LIMIT_PER_MIN: '2' }, geminiReply('{"a":1}'));
  assert.strictEqual((await post(s, ok)).status, 200);
  assert.strictEqual((await post(s, ok)).status, 200);
  assert.strictEqual((await post(s, ok)).status, 429);
  s.close();
});

test('rejects non-JSON AI output and empty output', async () => {
  let s = await start({ GEMINI_API_KEY: 'k' }, geminiReply('just words'));
  const r = await post(s, ok);
  assert.strictEqual(r.status, 502); assert.strictEqual(r.json.error, 'invalid_ai_response');
  s.close();
  s = await start({ GEMINI_API_KEY: 'k' }, geminiReply('  '));
  assert.strictEqual((await post(s, ok)).json.error, 'empty_ai_response');
  s.close();
});

test('fails clearly without a key, never leaks upstream errors', async () => {
  let s = await start({}, geminiReply('{}'));
  assert.strictEqual((await post(s, ok)).status, 500);
  s.close();
  s = await start({ GEMINI_API_KEY: 'k' }, async () => ({ ok: false, status: 500, json: async () => ({ error: { message: 'secret detail' } }) }));
  const r = await post(s, ok);
  assert.strictEqual(r.status, 502);
  assert.ok(!JSON.stringify(r.json).includes('secret'));
  s.close();
});

test('claude provider path', async () => {
  const s = await start({ AI_PROVIDER: 'claude', ANTHROPIC_API_KEY: 'k' }, async () => ({ ok: true, status: 200, json: async () => ({ content: [{ type: 'text', text: '{"message":"x"}' }] }) }));
  assert.strictEqual((await post(s, ok)).json.text, '{"message":"x"}');
  s.close();
});
