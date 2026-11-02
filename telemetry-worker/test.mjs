import assert from 'node:assert/strict';
import worker from './index.js';

let lastMessage = null;
let lastDoc = null;
let docText = null;
let telegramOk = true;
let chatLookup = null;
let nextMessageId = 100;
let lastSentId = 0;
let lastDocId = 0;
const calls = [];

function kv() {
  const m = new Map();
  return {
    async put(k, v) { m.set(k, v); },
    async get(k, type) {
      const v = m.get(k);
      if (v == null) return null;
      return type === 'json' ? JSON.parse(v) : v;
    },
    async list({ prefix = '', limit = 1000 } = {}) {
      return {
        keys: [...m.keys()].filter(k => k.startsWith(prefix)).slice(0, limit).map(name => ({ name }))
      };
    }
  };
}

globalThis.fetch = async (url, opt) => {
  calls.push({ url, body: opt.body });
  const isDoc = url.includes('/sendDocument');
  const isEdit = url.includes('/editMessageText');
  const isReaction = url.includes('/setMessageReaction');
  let chatId = '';
  if (isDoc) {
    lastDoc = { url, body: opt.body };
    chatId = opt.body.get('chat_id');
    docText = await opt.body.get('document').text();
  } else if (isEdit || isReaction) {
    const body = JSON.parse(opt.body);
    chatId = String(body.chat_id);
  } else {
    const body = JSON.parse(opt.body);
    lastMessage = { url, body };
    chatId = String(body.chat_id);
  }
  if (chatLookup && chatLookup[chatId] === false) {
    return {
      ok: false,
      status: 400,
      json: async () => ({ ok: false, description: 'Bad Request: chat not found' })
    };
  }
  const result = (isEdit || isReaction)
    ? true
    : { message_id: ++nextMessageId, chat: { id: Number(chatId) } };
  if (isDoc) lastDocId = result.message_id;
  else if (!isEdit && !isReaction) lastSentId = result.message_id;
  return {
    ok: telegramOk,
    status: telegramOk ? 200 : 500,
    json: async () => ({ ok: telegramOk, description: telegramOk ? '' : 'fail', result: telegramOk ? result : null })
  };
};

async function secretFor(token) {
  const bytes = new TextEncoder().encode('funachat-webhook:' + token);
  const hash = await crypto.subtle.digest('SHA-256', bytes);
  return Array.from(new Uint8Array(hash)).map(b => b.toString(16).padStart(2, '0')).join('');
}

const TOKEN = '123:secret';
const env = { TELEGRAM_BOT_TOKEN: TOKEN, TELEGRAM_CHAT_ID: '-1001', STATE: kv() };

const payload = (id = 'abcdef1234567890', err = 1) => JSON.stringify({
  schema: 1, id, ts: 1793634000000, source: 'manual',
  app: { version: '2.42.29', code: 104 },
  device: { manufacturer: 'Google', model: 'Pixel', release: '15', sdk: 35, locale: 'uk-UA', tz: 'Europe/Kyiv' },
  state: { crash: false, storage: true, termux: true, accounts: 1, logLines: 10 },
  api: { keys: 2, public: false },
  usage: { q: 5, done: 4, err, tool: 2, edit: 0 },
  logs: '11-02 18:40:01.123 I/app: start\n11-02 18:40:02.456 E/net: timeout',
  crashText: 'crash 2.42.29 java.lang.RuntimeException: boom'
});

const req = (body, ip = '1.2.3.4', path = '/v1/telemetry') => new Request('https://w.test' + path, {
  method: 'POST', headers: { 'content-type': 'application/json', 'cf-connecting-ip': ip }, body
});

const hook = (update, secret) => new Request('https://w.test/v1/telegram', {
  method: 'POST',
  headers: {
    'content-type': 'application/json',
    'x-telegram-bot-api-secret-token': secret
  },
  body: JSON.stringify(update)
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

assert.ok(lastMessage.url.includes('/sendMessage'));
assert.equal(lastMessage.body.chat_id, '-1001');
assert.ok(lastMessage.body.text.includes('FunaChat 2.42.29'));
assert.ok(lastMessage.body.text.includes('#error'), lastMessage.body.text);
assert.ok(lastMessage.body.text.includes('errors 1'));
assert.ok(lastMessage.body.text.includes('log lines 2'), lastMessage.body.text);
assert.ok(lastMessage.body.text.includes('I/app: start'), 'log tail missing');
assert.ok(!lastMessage.body.text.includes('123:secret'));
assert.ok(lastMessage.body.text.includes('2026-11-02'), lastMessage.body.text);
assert.ok(lastMessage.body.text.length <= 4096, 'telegram message too long');
assert.ok(!lastMessage.body.text.includes('workers.dev'), 'endpoint must not leak');

assert.ok(lastDoc.url.includes('/sendDocument'));
assert.ok(lastDoc.body.get('caption').includes('FunaChat 2.42.29'));
const full = JSON.parse(docText);
assert.equal(full.logs, '11-02 18:40:01.123 I/app: start\n11-02 18:40:02.456 E/net: timeout');
assert.ok(docText.includes('java.lang.RuntimeException'), 'crash text missing from document');

const reportId = lastSentId;
const saved = await env.STATE.get('r:-1001:' + reportId, 'json');
assert.ok(saved && saved.e === 1 && saved.f === false, 'report not stored: ' + JSON.stringify(saved));
assert.ok(saved.dm > 0, 'document message id not stored');

// Реакція ❤ від власника -> #fixed, ✅ і запис у сховищі.
const secret = await secretFor(TOKEN);
r = await worker.fetch(hook({
  update_id: 1,
  message_reaction: {
    chat: { id: -1001 },
    message_id: reportId,
    user: { id: 8080203197 },
    new_reaction: [{ type: 'emoji', emoji: '❤' }]
  }
}, secret), env);
assert.equal(r.status, 200);
const fixed = await env.STATE.get('r:-1001:' + reportId, 'json');
assert.equal(fixed.f, true);
assert.ok(calls.some(c => c.url.includes('/editMessageText') &&
  JSON.parse(c.body).text.includes('#fixed')), 'message not edited');
assert.ok(calls.some(c => c.url.includes('/setMessageReaction') &&
  JSON.parse(c.body).reaction[0].emoji === '✅'), 'reaction not set');

// /review після фіксу — порожньо.
calls.length = 0;
r = await worker.fetch(hook({
  update_id: 2,
  message: {
    message_id: 50,
    chat: { id: 8080203197, type: 'private' },
    from: { id: 8080203197 },
    text: '/review'
  }
}, secret), env);
assert.equal(r.status, 200);
const review = calls.filter(c => c.url.includes('/sendMessage')).pop();
assert.ok(review && JSON.parse(review.body).text.includes('немає'), 'review should be empty');

// Нове помилкове звіт -> /review дає посилання.
r = await worker.fetch(req(payload('second000000001', 3)), env);
assert.equal(r.status, 200);
const secondId = lastSentId;
calls.length = 0;
r = await worker.fetch(hook({
  update_id: 3,
  message: {
    message_id: 51,
    chat: { id: 8080203197, type: 'private' },
    from: { id: 8080203197 },
    text: '/review'
  }
}, secret), env);
assert.equal(r.status, 200);
const review2 = calls.filter(c => c.url.includes('/sendMessage')).pop();
const reviewText = JSON.parse(review2.body).text;
assert.ok(reviewText.includes('https://t.me/c/1/' + secondId), reviewText);
assert.ok(reviewText.includes('errors 3'), reviewText);

// Чуже сердечко нічого не фіксує.
r = await worker.fetch(hook({
  update_id: 4,
  message_reaction: {
    chat: { id: -1001 },
    message_id: secondId,
    user: { id: 111 },
    new_reaction: [{ type: 'emoji', emoji: '❤' }]
  }
}, secret), env);
assert.equal(r.status, 200);
assert.equal((await env.STATE.get('r:-1001:' + secondId, 'json')).f, false);

// Без правильного секрету webhook закритий.
r = await worker.fetch(hook({ update_id: 5 }, 'wrong'), env);
assert.equal(r.status, 403);

// У каналах Telegram надсилає згруповані reaction_count без автора:
// ❤ на документі теж має закрити звіт.
r = await worker.fetch(req(payload('countreport00001', 2)), env);
assert.equal(r.status, 200);
const countId = lastSentId;
const countDocId = lastDocId;
calls.length = 0;
r = await worker.fetch(hook({
  update_id: 6,
  message_reaction_count: {
    chat: { id: -1001 },
    message_id: countDocId,
    reactions: [{ type: { type: 'emoji', emoji: '❤' }, total_count: 1 }]
  }
}, secret), env);
assert.equal(r.status, 200);
const counted = await env.STATE.get('r:-1001:' + countId, 'json');
assert.equal(counted.f, true, 'reaction_count should mark fixed');
assert.equal(counted.h, 1);
assert.ok(calls.some(c => c.url.includes('/editMessageText') &&
  JSON.parse(c.body).text.includes('#fixed')), 'summary not edited after doc reaction');

// Дуже довгий лог: у повідомленні лише хвіст, документ містить усе.
const longLog = Array.from({ length: 180 }, (_, i) => 'line ' + i + ' ' + 'x'.repeat(40)).join('\n');
const bigPayload = JSON.parse(payload('longlogs00000001'));
bigPayload.logs = longLog;
r = await worker.fetch(req(JSON.stringify(bigPayload)), env);
assert.equal(r.status, 200);
assert.ok(lastMessage.body.text.length <= 4096, 'trimmed message too long: ' + lastMessage.body.text.length);
assert.ok(lastMessage.body.text.includes('line 179'), 'tail of logs expected');
assert.ok(lastMessage.body.text.includes('log lines 180'), lastMessage.body.text);
assert.equal(JSON.parse(docText).logs, longLog);

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
const envBare = { TELEGRAM_BOT_TOKEN: TOKEN, TELEGRAM_CHAT_ID: '4311112103' };
r = await worker.fetch(req(payload('barchannel000001')), envBare);
assert.equal(r.status, 200);
assert.equal(lastMessage.body.chat_id, '-1004311112103');
assert.equal(lastDoc.body.get('chat_id'), '-1004311112103');
chatLookup = null;

console.log('WORKER TESTS PASS');
