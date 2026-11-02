# FunaChat telemetry Worker

Приймач знеособлених діагностичних звітів. Токен бота зберігається тільки
в секретах Cloudflare — в APK його немає.

## Деплой через dashboard (без картки)

1. <https://dash.cloudflare.com> → **Workers & Pages** → **Create** →
   **Worker**.
2. Назва `funachat-telemetry`, **Deploy**.
3. **Edit code** → вставте вміст `index.js` → **Deploy**.
4. **Settings → Variables and Secrets**:
   - `TELEGRAM_BOT_TOKEN` — токен від BotFather (Secret);
   - `TELEGRAM_CHAT_ID` — chat_id діагностики (Secret);
   - опційно `TELEGRAM_SILENT=0` і `MAX_PER_HOUR=30`.
5. Скопіюйте URL виду `https://funabot.<account>.workers.dev`.

### Реакції та /review (потрібен KV)

1. Cloudflare → **Storage & Databases → KV** → **Create namespace**,
   назва `funabot-state`.
2. Worker `funabot` → **Settings → Bindings → Add → KV namespace**:
   - Variable name: `STATE`
   - KV namespace: `funabot-state`
3. **Важливо:** binding, доданий лише в dashboard, злітає після деплою з
   GitHub. Скопіюйте **Namespace ID** (32 hex) і впишіть у `wrangler.toml`:

```toml
[[kv_namespaces]]
binding = "STATE"
id = "ваш_namespace_id"
```

Після цього деплой з GitHub зберігає binding. Перевірка:
`GET /health` → `"state":true`.

Після цього:
- кожен звіт отримує тег `#error` або `#ok` (і `#crash`, якщо був збій);
- ❤ від власника (`8080203197`) на повідомленні зі звітом позначає його
  `#fixed`, ставить ✅ і прибирає зі списку відкритих;
- команда `/review` (у приватному чаті з ботом або в каналі діагностики)
  надсилає посилання на звіти з помилками, які ще не позначені ❤.

Webhook уже налаштовується окремо на `/v1/telegram`; секрет обчислюється
з `TELEGRAM_BOT_TOKEN`, додаткових змінних не потрібно.

Перевірка:

```bash
curl -s https://funachat-telemetry.<account>.workers.dev/health
# {"ok":true,"app":"funachat-telemetry"}
```

## Деплой через wrangler

```bash
cd telemetry-worker
npx wrangler login
npx wrangler secret put TELEGRAM_BOT_TOKEN
npx wrangler secret put TELEGRAM_CHAT_ID
npx wrangler deploy
```

## Що приймається

`POST /v1/telemetry`, JSON до 16 КБ, `schema: 1`. Worker перевіряє поля,
обмежує частоту (з IP і з інсталяції) і надсилає в Telegram короткий
підсумок із хвостом логу та повний JSON документом. Сирі дані не логуються.

Публічний URL може використати сторонній для фейкових звітів — це
обмежується лімітами й валідацією. Токен бота при цьому не компрометується.
