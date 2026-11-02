/**
 * FunaChat telemetry receiver (Cloudflare Worker).
 *
 * Приймає знеособлений JSON від застосунку й пересилає короткий звіт у
 * Telegram. Токен бота живе лише в секретах Worker — в APK його немає.
 *
 * Env:
 *   TELEGRAM_BOT_TOKEN  — обов'язково
 *   TELEGRAM_CHAT_ID    — обов'язково
 *   TELEGRAM_SILENT     — "0", щоб надсилати зі звуком (типово тихо)
 *   MAX_PER_HOUR        — ліміт з одного IP (типово 30)
 */

const MAX_BODY = 16 * 1024;
const HITS = new Map();

function json(obj, status = 200) {
  return new Response(JSON.stringify(obj), {
    status,
    headers: { 'content-type': 'application/json; charset=utf-8' }
  });
}

function str(v, max = 120) {
  if (v === null || v === undefined) return '';
  let s = String(v);
  if (s.length > max) s = s.slice(0, max);
  return s;
}

function num(v) {
  const n = Number(v);
  if (!isFinite(n) || n < 0) return 0;
  return Math.min(Math.floor(n), 1000000000);
}

function esc(v) {
  return str(v, 4000)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}

function rateLimited(key, limit) {
  const now = Date.now();
  const hour = 60 * 60 * 1000;
  const arr = (HITS.get(key) || []).filter(t => now - t < hour);
  arr.push(now);
  HITS.set(key, arr);
  if (HITS.size > 5000) {
    for (const [k, v] of HITS) {
      if (!v.length || now - v[v.length - 1] > hour) HITS.delete(k);
    }
  }
  return arr.length > limit;
}

function valid(d) {
  if (!d || typeof d !== 'object' || Array.isArray(d)) return false;
  if (d.schema !== 1) return false;
  if (typeof d.id !== 'string' || d.id.length < 8 || d.id.length > 64) return false;
  if (!d.app || typeof d.app !== 'object') return false;
  if (typeof d.app.version !== 'string' || !d.app.version) return false;
  if (typeof d.ts !== 'number' || !isFinite(d.ts)) return false;
  return true;
}

function message(d) {
  const app = d.app || {};
  const dev = d.device || {};
  const st = d.state || {};
  const use = d.usage || {};
  const when = new Date(num(d.ts) || Date.now()).toISOString().replace('T', ' ').slice(0, 16);
  const lines = [
    `<b>FunaChat ${esc(app.version)}</b> · ${esc(d.source || 'auto')}` +
      (st.crash ? ' · <b>CRASH</b>' : ''),
    `${esc(dev.manufacturer)} ${esc(dev.model)} · Android ${esc(dev.release)} (${num(dev.sdk)})`,
    `${esc(dev.locale)} · ${esc(dev.tz)} · storage: ${st.storage ? 'yes' : 'no'} · termux: ${st.termux ? 'yes' : 'no'}`,
    `requests ${num(use.q)} · done ${num(use.done)} · errors ${num(use.err)} · tools ${num(use.tool)} · edits ${num(use.edit)}`,
    `accounts ${num(st.accounts)} · keys ${num((d.api || {}).keys)} · public ${((d.api || {}).public) ? 'on' : 'off'} · logs ${num(st.logLines)}`,
    `<code>${esc(d.id).slice(0, 12)}…</code> · ${esc(when)} UTC`
  ];
  return lines.join('\n');
}

/**
 * Канали й супергрупи мають id виду -100<base>. У Telegram-посиланнях і
 * налаштуваннях часто показують лише <base>, тому якщо перша спроба
 * отримала «chat not found», пробуємо той самий id з префіксом -100.
 */
function chatCandidates(raw) {
  const id = String(raw || '').trim();
  const out = [id];
  if (/^[0-9]{6,}$/.test(id)) out.push('-100' + id);
  return out;
}

async function telegramOnce(token, chatId, text, silent) {
  const url = 'https://api.telegram.org/bot' + token + '/sendMessage';
  const body = JSON.stringify({
    chat_id: chatId,
    text,
    parse_mode: 'HTML',
    disable_web_page_preview: true,
    disable_notification: silent
  });
  const r = await fetch(url, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body
  });
  let desc = '';
  if (!r.ok) {
    try { desc = (await r.json()).description || ''; } catch (e) {}
  }
  return { ok: r.ok, desc };
}

async function sendTelegram(env, text) {
  const silent = env.TELEGRAM_SILENT !== '0';
  let last = { ok: false, desc: '' };
  for (const chatId of chatCandidates(env.TELEGRAM_CHAT_ID)) {
    last = await telegramOnce(env.TELEGRAM_BOT_TOKEN, chatId, text, silent);
    if (last.ok) return true;
    if (!/chat not found/i.test(last.desc)) break;
  }
  return false;
}

export default {
  async fetch(req, env) {
    const url = new URL(req.url);
    if (req.method === 'GET' && url.pathname === '/health') {
      return json({ ok: true, app: 'funachat-telemetry' });
    }
    if (req.method !== 'POST' || url.pathname !== '/v1/telemetry') {
      return json({ ok: false, error: 'not_found' }, 404);
    }
    if (!env.TELEGRAM_BOT_TOKEN || !env.TELEGRAM_CHAT_ID) {
      return json({ ok: false, error: 'not_configured' }, 503);
    }
    const ip = req.headers.get('cf-connecting-ip') || 'unknown';
    const limit = Math.max(1, parseInt(env.MAX_PER_HOUR || '30', 10) || 30);
    if (rateLimited('ip:' + ip, limit)) {
      return json({ ok: false, error: 'rate_limited' }, 429);
    }
    const text = await req.text();
    if (text.length > MAX_BODY) return json({ ok: false, error: 'too_large' }, 413);
    let d;
    try { d = JSON.parse(text); } catch (e) { return json({ ok: false, error: 'bad_json' }, 400); }
    if (!valid(d)) return json({ ok: false, error: 'bad_schema' }, 400);
    if (rateLimited('id:' + d.id, Math.max(1, Math.floor(limit / 3)))) {
      return json({ ok: false, error: 'rate_limited' }, 429);
    }
    try {
      const sent = await sendTelegram(env, message(d));
      if (!sent) return json({ ok: false, error: 'telegram_failed' }, 502);
    } catch (e) {
      return json({ ok: false, error: 'telegram_failed' }, 502);
    }
    return json({ ok: true });
  }
};
