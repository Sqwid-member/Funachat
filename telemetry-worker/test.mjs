import assert from 'node:assert/strict';
import worker from './index.js';

let lastTelegram = null;
let telegramOk = true;
let chatLookup = null;
globalThis.fetch = async (url, opt) => {
  lastTelegram = { url, body: JSON.parse(opt.body) };
  if (chatLookup && chatLookup[lastTelegram.body.chat_id] === false) {
    return {
      ok: false,
      status: 400,
      json: async () => ({ ok: false, description: 'Bad Request: chat not found' })
    };
  }
  return { ok: telegramOk, status: telegramOk ? 200 : 500, json: async () => ({ ok: telegramOk }) };
};

const env = { TELEGRAM_BOT_TOKEN: '123:secret', TELEGRAM_CHAT_ID: '-1001' };
const payload = (id = 'abcdef1234567890') => JSON.stringify({
  schema: 1, id, ts: 1793634000000, source: 'manual',
  app: { version: '2.42.25', code: 100 },
  device: { manufacturer: 'Google', model: 'Pixel', release: '15', sdk: 35, locale: 'uk-UA', tz: 'Europe/Kyiv' },
  state: { crash: false, storage: true, termux: true, accounts: 1, logLines: 10 },
  api: { keys: 2, public: false },
  usage: { q: 5, done: 4, err: 1, tool: 2, edit: 0 },
  logs: '11-02 18:40:01.123 I/app: start\n11-02 18:40:02.456 E/net: timeout',
  crashText: 'crash 2.42.27 java.lang.RuntimeException: boom'
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
assert.ok(lastTelegram.body.text.includes('2026-11-02'), lastTelegram.body.text);
assert.ok(lastTelegram.body.text.includes('I/app: start'), 'log line missing');
assert.ok(lastTelegram.body.text.includes('java.lang.RuntimeException'), 'crash text missing');
assert.ok(lastTelegram.body.text.length <= 4096, 'telegram message too long');
assert.ok(!lastTelegram.body.text.includes('workers.dev'), 'endpoint must not leak');

// Дуже довгий лог не має роздувати повідомлення понад ліміт Telegram.
const longLog = Array.from({ length: 180 }, (_, i) => 'line ' + i + ' ' + 'x'.repeat(40)).join('\n');
const bigPayload = JSON.parse(payload('longlogs00000001'));
bigPayload.logs = longLog;
r = await worker.fetch(req(JSON.stringify(bigPayload)), env);
assert.equal(r.status, 200);
assert.ok(lastTelegram.body.text.length <= 4096, 'trimmed message too long: ' + lastTelegram.body.text.length);
assert.ok(lastTelegram.body.text.includes('line 179'), 'tail of logs expected');

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

// Канал без префікса -100: Worker має спробувати -100<id>.
chatLookup = { '4311112103': false };
const envBare = { TELEGRAM_BOT_TOKEN: '123:secret', TELEGRAM_CHAT_ID: '4311112103' };
r = await worker.fetch(req(payload('barchannel000001')), envBare);
assert.equal(r.status, 200);
assert.equal(lastTelegram.body.chat_id, '-1004311112103');
chatLookup = null;

console.log('WORKER TESTS PASS');
