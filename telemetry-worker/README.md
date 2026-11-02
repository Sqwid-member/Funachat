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
5. Скопіюйте URL виду `https://funachat-telemetry.<account>.workers.dev`.
6. Вставте його в застосунку: **Довідка → Діагностичні дані → HTTPS-приймач**.

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
обмежує частоту (з IP і з інсталяції) і надсилає в Telegram лише
відібрані поля. Сирі дані не логуються.

Публічний URL може використати сторонній для фейкових звітів — це
обмежується лімітами й валідацією. Токен бота при цьому не компрометується.
