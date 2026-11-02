/**
 * FunaChat neutral front (Cloudflare Pages, advanced mode).
 *
 * Навіщо: адреса воркера містить імʼя власника акаунта
 * (<worker>.<account>.workers.dev). Цей проксі дає нейтральний
 * https://<project>.pages.dev/... і не світить його в APK та
 * налаштуваннях Telegram.
 *
 * Дозволені лише три маршрути:
 *   POST /v1/telemetry  — звіти застосунку
 *   POST /v1/telegram   — webhook Telegram (секрет перевіряє воркер)
 *   GET  /health        — перевірка стану
 *
 * Деплой:
 *   wrangler pages deploy telemetry-proxy --project-name funa-telemetry
 *   wrangler pages secret put UPSTREAM --project-name funa-telemetry
 *   # UPSTREAM = https://<worker>.workers.dev
 *
 * Токен бота й далі живе лише в секретах воркера.
 */
const ALLOWED = new Set(['/v1/telemetry', '/v1/telegram', '/health']);
const MAX_BODY = 64 * 1024;

export default {
  async fetch(req, env) {
    const url = new URL(req.url);
    if (!ALLOWED.has(url.pathname)) {
      return new Response('not found', { status: 404 });
    }
    const upstream = (env.UPSTREAM || '').replace(/\/+$/, '');
    if (!upstream) {
      return new Response('upstream is not configured', { status: 500 });
    }
    const body = req.method === 'GET' || req.method === 'HEAD'
      ? undefined
      : await req.arrayBuffer();
    if (body && body.byteLength > MAX_BODY) {
      return new Response('too large', { status: 413 });
    }
    const h = new Headers(req.headers);
    h.delete('host');
    h.delete('content-length');
    h.delete('connection');
    if (body) h.set('content-length', String(body.byteLength));
    const r = await fetch(upstream + url.pathname + url.search, {
      method: req.method,
      headers: h,
      body
    });
    const out = new Headers();
    for (const k of ['content-type', 'cache-control']) {
      const v = r.headers.get(k);
      if (v) out.set(k, v);
    }
    if (!out.has('cache-control')) out.set('cache-control', 'no-store');
    return new Response(r.body, { status: r.status, headers: out });
  }
};
