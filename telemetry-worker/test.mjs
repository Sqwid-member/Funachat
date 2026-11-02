import assert from 'node:assert/strict';
import worker from './index.js';

let lastTelegram = null;
let telegramOk = true;
globalThis.fetch = async (url, opt) => {
  lastTelegram = { url, body: JSON.parse(opt.body) };
  return { ok: telegramOk, status: telegramOk ? 200 : 500 };
};

const env = { TELEGRAM_BOT_TOKEN: '123:secret', TELEGRAM_CHAT_ID: '-1001' };
const payload = (id = 'abcdef1234567890') => JSON.stringify({
  schema: 1, id, ts: 1793634000000, source: 'manual',
  app: { version: '2.42.25', code: 100 },
  device: { manufacturer: 'Google', model: 'Pixel', release: '15', sdk: 35, locale: 'uk-UA', tz: 'Europe/Kyiv' },
  state: { crash: false, storage: true, termux: true, accounts: 1, logLines: 10 },
  api: { keys: 2, public: false },
  usage: { q: 5, done: 4, err: 1, tool: 2, edit: 0 }
});

const req = (body, ip = '1.2.3.4', path = '/v1/telemetry') => new Request('https://w.test' + path, {
  method: 'POST', headers: { 'content-type': 'application/json', 'cf-connecting-ip': ip }, body
});

let r = await worker.fetch(new Request('https://w.test/health'), env);
assert.equal(r.status, 200);
assert.equal((await r.json()).ok, true);

r = await worker.fetch(req('{oops'), env);
assert.equal(r.status, 400);

r = await worker.fetch(req(payload('short')), env);
assert.equal(r.status, 400);

r = await worker.fetch(req(payload()), env);
assert.equal(r.status, 200);
assert.equal((await r.json()).ok, true);
assert.ok(lastTelegram.url.includes('/sendMessage'));
assert.equal(lastTelegram.body.chat_id, '-1001');
assert.ok(lastTelegram.body.text.includes('FunaChat 2.42.25'));
assert.ok(lastTelegram.body.text.includes('errors 1'));
assert.ok(!lastTelegram.body.text.includes('123:secret'));

telegramOk = false;
r = await worker.fetch(req(payload('otherinstall01'), '9.9.9.9'), env);
assert.equal(r.status, 502);

r = await worker.fetch(req('x'.repeat(17000), '8.8.8.8'), env);
assert.equal(r.status, 413);

telegramOk = true;
let limited = false;
for (let i = 0; i < 35; i++) {
  r = await worker.fetch(req(payload('id' + i + 'abcdefgh'), '7.7.7.7'), env);
  if (r.status === 429) { limited = true; break; }
}
assert.ok(limited, 'rate limit should trigger');

console.log('WORKER TESTS PASS');
