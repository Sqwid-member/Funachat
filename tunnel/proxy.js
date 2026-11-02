#!/usr/bin/env node
/*
 * FunaChat public edge.
 *
 * Один публічний origin для всього:
 *   /updates/*  -> статика з dist/ (APK + latest.json)
 *   решта       -> 127.0.0.1:8787 (Local API застосунку)
 *
 * Так тунелю (cloudflared/zrok) потрібен лише один порт, а застосунок
 * лишається на петлі й не слухає LAN.
 */
const http = require('http');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const ROOT = path.resolve(__dirname, '..');
const DIST = path.join(ROOT, 'dist');
const APP_PORT = Number(process.env.APP_PORT || 8787);
const PORT = Number(process.env.PORT || 8790);

const TYPES = {
  '.json': 'application/json; charset=utf-8',
  '.apk': 'application/vnd.android.package-archive',
  '.txt': 'text/plain; charset=utf-8',
  '.md': 'text/markdown; charset=utf-8'
};

function hostOf(req) {
  const h = (req.headers['x-forwarded-host'] || req.headers.host || '').split(',')[0].trim();
  if (!h) return null;
  const proto = (req.headers['x-forwarded-proto'] || 'https').split(',')[0].trim() || 'https';
  return proto + '://' + h;
}

function release() {
  const f = path.join(DIST, 'release.json');
  let r;
  try { r = JSON.parse(fs.readFileSync(f, 'utf8')); } catch (e) { return null; }
  const file = String(r.file || 'FunaChat.apk').replace(/[^A-Za-z0-9._-]/g, '');
  const apk = path.join(DIST, file);
  const out = {
    version: String(r.version || ''),
    code: Number(r.code || 0),
    file,
    notes: String(r.notes || ''),
    date: String(r.date || '')
  };
  try {
    const st = fs.statSync(apk);
    out.size = st.size;
    if (r.md5 && r.md5.length === 32) out.md5 = r.md5;
    else out.md5 = crypto.createHash('md5').update(fs.readFileSync(apk)).digest('hex');
  } catch (e) { out.size = 0; }
  return out;
}

function json(res, code, obj) {
  const b = Buffer.from(JSON.stringify(obj));
  res.writeHead(code, {
    'content-type': 'application/json; charset=utf-8',
    'content-length': b.length,
    'cache-control': 'no-store',
    'access-control-allow-origin': '*'
  });
  res.end(b);
}

function serveFile(req, res, rel) {
  const name = path.basename(rel);
  if (name.startsWith('.') || name !== rel) {
    return json(res, 404, { error: 'not found' });
  }
  const f = path.join(DIST, name);
  let st;
  try { st = fs.statSync(f); } catch (e) { return json(res, 404, { error: 'not found' }); }
  const type = TYPES[path.extname(name).toLowerCase()] || 'application/octet-stream';
  const head = {
    'content-type': type,
    'accept-ranges': 'bytes',
    'access-control-allow-origin': '*',
    'cache-control': name.endsWith('.apk') ? 'public, max-age=31536000, immutable' : 'no-store'
  };
  let start = 0, end = st.size - 1;
  const range = req.headers.range;
  if (range) {
    const m = /^bytes=(\d*)-(\d*)$/.exec(range.trim());
    if (m) {
      if (m[1]) start = Number(m[1]);
      if (m[2]) end = Math.min(Number(m[2]), st.size - 1);
      if (!m[1] && m[2]) { start = Math.max(0, st.size - Number(m[2])); end = st.size - 1; }
      if (start > end || start >= st.size) {
        res.writeHead(416, { 'content-range': 'bytes */' + st.size });
        return res.end();
      }
      head['content-range'] = 'bytes ' + start + '-' + end + '/' + st.size;
      head['content-length'] = end - start + 1;
      res.writeHead(206, head);
    }
  }
  if (!res.getHeader('content-length') && !head['content-length']) head['content-length'] = st.size;
  if (!res.headersSent) res.writeHead(200, head);
  if (req.method === 'HEAD') return res.end();
  fs.createReadStream(f, { start, end }).on('error', () => res.destroy()).pipe(res);
}

const server = http.createServer((req, res) => {
  const url = (req.url || '/').split('?')[0];

  if (req.method === 'OPTIONS') {
    res.writeHead(204, {
      'access-control-allow-origin': '*',
      'access-control-allow-headers': '*',
      'access-control-allow-methods': 'GET,POST,OPTIONS'
    });
    return res.end();
  }

  if (url === '/updates/health') {
    const r = release();
    return json(res, 200, { ok: true, dist: !!r, version: r ? r.version : null, code: r ? r.code : null });
  }

  if (url === '/updates/latest.json' || url === '/latest.json') {
    const r = release();
    if (!r) return json(res, 404, { error: 'no release published' });
    const base = hostOf(req);
    return json(res, 200, {
      version: r.version,
      code: r.code,
      url: (base ? base : '') + '/updates/' + r.file,
      size: r.size,
      md5: r.md5,
      date: r.date,
      notes: r.notes
    });
  }

  if (url.startsWith('/updates/')) {
    return serveFile(req, res, url.slice('/updates/'.length));
  }

  const p = http.request({
    host: '127.0.0.1',
    port: APP_PORT,
    path: req.url,
    method: req.method,
    headers: Object.assign({}, req.headers, { 'x-forwarded-proto': 'https' })
  }, (up) => {
    res.writeHead(up.statusCode || 502, up.headers);
    up.pipe(res);
  });
  // стрім (SSE) не має таймауту на відповідь, лише на підключення
  p.setTimeout(0);
  p.on('error', () => {
    if (!res.headersSent) json(res, 502, { error: 'FunaChat API is not running' });
    else res.end();
  });
  req.pipe(p);
  res.on('close', () => p.destroy());
});

server.requestTimeout = 0;
server.headersTimeout = 30000;
server.keepAliveTimeout = 65000;
server.listen(PORT, '127.0.0.1', () => {
  console.log('funa-edge on http://127.0.0.1:' + PORT + ' -> app :' + APP_PORT + ', dist ' + DIST);
});
