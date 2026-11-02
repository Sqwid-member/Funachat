# FunaChat telemetry front

Нейтральна адреса для телеметрії замість `<worker>.<account>.workers.dev`.

```
https://funa-telemetry.pages.dev/v1/telemetry  ─┐
https://funa-telemetry.pages.dev/v1/telegram   ─┼─► https://<worker>.workers.dev
https://funa-telemetry.pages.dev/health        ─┘
```

В APK і в налаштуваннях Telegram потрапляє лише `pages.dev`; імʼя
акаунта Cloudflare лишається в секреті `UPSTREAM` на боці Pages.
Токен бота, як і раніше, є тільки в секретах воркера.

Webhook Telegram можна перевести на нейтральну адресу:

```bash
curl -s "https://api.telegram.org/bot<token>/setWebhook" \
  -d "url=https://funa-telemetry.pages.dev/v1/telegram" \
  -d "secret_token=<sha256(funachat-webhook:token)>" \
  -d "allowed_updates=[\"message\",\"message_reaction\",\"message_reaction_count\"]"
```

## Деплой

```bash
cd telemetry-worker
npx wrangler pages deploy ../telemetry-proxy --project-name funa-telemetry

# один раз: адреса справжнього воркера
npx wrangler pages secret put UPSTREAM --project-name funa-telemetry
# вставити https://<worker>.workers.dev
```

Перевірка:

```bash
curl -s -o /dev/null -w '%{http_code}\n' \
  -X POST https://funa-telemetry.pages.dev/v1/telemetry \
  -H 'content-type: application/json' -d '{}'
# 400 — проксі живий, воркер відхилив порожній знімок
```

## Збірка APK з новою адресою

```bash
FUNA_TELEMETRY_URL=https://funa-telemetry.pages.dev/v1/telemetry \
  ./build.sh /sdcard/Download/FunaChat.apk
```

`build.sh` копіює `src` у `build/src` і переписує XOR-масив лише в копії.
Робоче дерево не змінюється. Щоб змінити адресу назавжди:

```bash
python3 tools/set-endpoint.py src/com/funa/chat/Telemetry.java \
  https://funa-telemetry.pages.dev/v1/telemetry
```

## Чому не zrok для телеметрії

zrok тунелює на пристрій, де запущений агент. Телеметрія з усіх
встановлень перестане доходити, щойно телефон вимкнеться або тунель
впаде. Для API-тунелю це нормально, для фонової телеметрії — ні.
Pages/воркер працює постійно й не потребує картки.

Якщо все ж пускати через zrok, `tunnel/proxy.js` уміє пересилати
`POST /v1/telemetry` на `TELEMETRY_UPSTREAM` (див. `tunnel/README.md`).
