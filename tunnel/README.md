# Публічний доступ до FunaChat

Один публічний HTTPS-origin віддає і Local API, і оновлення:

```
https://<адреса тунелю>/v1/...              -> Local API застосунку (127.0.0.1:8787)
https://<адреса тунелю>/updates/latest.json -> маніфест оновлень
https://<адреса тунелю>/updates/FunaChat.apk -> APK
```

Застосунок при цьому далі слухає лише петлю (`127.0.0.1`), у LAN нічого не
відкривається. Між тунелем і застосунком стоїть `proxy.js` на `127.0.0.1:8790`.

## Швидкий старт (без акаунтів і карток)

```bash
cd ~/downloads/funachat/tunnel
./start.sh          # Cloudflare quick tunnel, інакше zrok
```

Скрипт:

1. піднімає `proxy.js`, якщо той ще не працює;
2. піднімає `cloudflared tunnel --url http://127.0.0.1:8790`;
3. пише адресу у `tunnel/url.txt` і в `Download/FunaFiles/tunnel.txt`.

Далі в застосунку: **Налаштування → API → Публічний доступ → «З файлу»**.
Адреса підхопиться сама, кнопка «Перевірити» покаже стан.

Адреса quick-тунелю живе, поки живий процес, і змінюється після перезапуску —
тому й потрібна кнопка «З файлу», а не ручне копіювання.

## Канали

| Канал | Акаунт | Картка | Стабільна адреса | Стан |
|---|---|---|---|---|
| Cloudflare quick tunnel | не потрібен | ні | ні (змінюється) | працює |
| Cloudflare named tunnel + `funachat.net.eu.org` | потрібен | ні | так | після делегування домену |
| zrok Free | потрібен (myzrok.io) | ні | так, резервована назва | після `zrok enable` |

### Перевірка каналу

```bash
U=$(cat url.txt)            # Cloudflare
# U=$(cat url-zrok.txt)     # zrok
curl -s "$U/updates/latest.json"
curl -s -H "Authorization: Bearer fk-…" "$U/health"
```

`401` без ключа — це правильно: edge дійшов до застосунку. `502` означає,
що FunaChat не запущений або його Local API вимкнено. `403` із кодом
`public_access_disabled` — у застосунку вимкнено «Дозволити доступ із
публічної мережі»; тунель при цьому може працювати й далі (оновлення
доступні), але API ззовні закритий.

### zrok (резерв)

```bash
# одноразово: токен з https://myzrok.io (email або GitHub, картка не потрібна)
cd ~/downloads/funachat/tunnel
./start-zrok.sh enable <enable-токен>

# далі
./start-zrok.sh
```

`zrok2` — glibc-бінарник, у Termux він працює через proot (Debian уже
встановлений): скрипт сам викликає `proot-distro login debian -- ~/zrok/zrok2`.
Ліміт безкоштовного тарифу — 5 ГБ/добу, публічний HTTPS, антифішингова
сторінка для браузерів (API-клієнти її не бачать).

### Cloudflare named tunnel (основний, після EU.org)

```bash
cloudflared tunnel login                 # один раз, у браузері
cloudflared tunnel create funachat
cloudflared tunnel route dns funachat api.funachat.net.eu.org
cloudflared tunnel route dns funachat dl.funachat.net.eu.org
```

`~/.cloudflared/config.yml`:

```yaml
tunnel: funachat
credentials-file: /data/data/com.termux/files/home/.cloudflared/<id>.json
ingress:
  - hostname: api.funachat.net.eu.org
    service: http://127.0.0.1:8790
  - hostname: dl.funachat.net.eu.org
    service: http://127.0.0.1:8790
  - service: http_status:404
```

Обидва імені ведуть на один edge: `/updates/*` — статика, решта — API.
Після цього публічна адреса в застосунку — `https://api.funachat.net.eu.org`.

## Оновлення

```bash
./publish.sh /sdcard/Download/FunaChat.apk 2.42.20 95 "коротко, що змінилось"
```

Копіює APK у `dist/`, рахує md5 і переписує `dist/release.json`. Edge віддає
`/updates/latest.json` уже з абсолютним посиланням на поточний хост.

Застосунок перевіряє оновлення тихо під час запуску (якщо адреса задана) і
вручну на екрані «Історія оновлень». Порівняння — за `code`, запасний
варіант — за рядком версії.

## Безпека

- ключ `fk-…` перевіряється на кожному запиті, включно з `/health`;
- ліміт 120 запитів/хв на IP, за тунелем IP береться з `CF-Connecting-IP`
  або `X-Forwarded-For` (тільки для з'єднань із петлі);
- назовні відкриті лише `/v1/*`, `/health` і `/updates/*`;
- `funa-bridge`, файлові інструменти й ADB у публічному API не виставлені:
  сервер виконує лише `time`, `calc`, `convert`, `random`, `base64`, `hash`,
  `skill`, `subagent`;
- не вмикайте «Доступ із локальної мережі», якщо працює тунель.

## Автозапуск

Якщо стоїть Termux:Boot, покладіть у `~/.termux/boot/`:

```bash
#!/data/data/com.termux/files/usr/bin/sh
sleep 20
/data/data/com.termux/files/home/downloads/funachat/tunnel/start.sh >> /data/data/com.termux/files/home/downloads/funachat/tunnel/boot.log 2>&1
```

і зробіть файл виконуваним (`chmod +x`).

## GitHub Releases (типовий канал оновлень)

Репозиторій: <https://github.com/Sqwid-member/Funachat>

Кожен реліз містить два assets:

- `FunaChat.apk` — підписаний APK;
- `latest.json` — той самий маніфест, що віддає edge.

Стабільні посилання (не змінюються між версіями):

```
https://github.com/Sqwid-member/Funachat/releases/latest/download/latest.json
https://github.com/Sqwid-member/Funachat/releases/latest/download/FunaChat.apk
```

Застосунок бере цей маніфест, якщо публічна адреса тунелю не задана. Якщо
задана — пріоритет має вона (можна тримати власний канал).

Публікація (потрібен токен з доступом до репозиторію):

```bash
./tunnel/release-gh.sh 2.42.28 103 "коротко, що змінилось"
```

Скрипт бере токен із `~/.config/funachat/gh_token` (права 600, у репозиторій
не потрапляє), створює тег `v<code>`, заливає APK і `latest.json`.

## Аудит злитих ключів

Секретний endpoint `POST /v1/audit/key` показує власника ключа, дозволяє
його відкликати й повідомити власника. Деталі форматів — у
[`../API.md`](../API.md).

Швидко:

```bash
cd ~/downloads/funachat

# секрет пристрою: Налаштування → API → Аудит ключів → копіювати
export FUNA_AUDIT_SECRET='fa-…'
./tunnel/audit-key.sh https://example.trycloudflare.com fk-… info

# ключ розробника (працює на будь-якому пристрої)
./tunnel/audit-key.sh https://example.trycloudflare.com fk-… revoke
./tunnel/audit-key.sh https://example.trycloudflare.com fk-… notify "знайшов у публічному репо"

# локально, без тунелю
./tunnel/audit-key.sh http://127.0.0.1:8787 fk-… info
```

Скрипт бере секрет із `FUNA_AUDIT_SECRET` або
`~/.config/funachat/audit_secret`, інакше підписує запит приватним ключем
`~/.config/funachat/dev_audit_key.pem`. Для `revoke` питає підтвердження
(`FUNA_YES=1` — без питання).

### Ключ розробника

```bash
./tunnel/dev-keygen.sh          # створить ~/.config/funachat/dev_audit_key.pem
```

Скрипт друкує публічний ключ. Щоб ротувати ключ, замініть константу
`DEV_AUDIT_PUB` у `src/com/funa/chat/ApiServer.java` на надруковане значення
й перезберіть APK. Приватний ключ у репозиторій не потрапляє (`.gitignore`).

Якщо глобальний аудит не потрібен — приберіть `DEV_AUDIT_PUB` і гілку
`devSignature`; лишиться тільки секрет пристрою.


## Діагностична телеметрія

Застосунок (Довідка → «Діагностичні дані») раз на добу кладе знеособлений
JSON у `/sdcard/Download/FunaFiles/telemetry`. Токен бота в APK не входить:
його читає цей скрипт із `~/.config/funachat/telegram_bot`.

```bash
# chat_id діагностики (приватний чат або канал)
echo '-100…' > ~/.config/funachat/telegram_diag_chat
chmod 600 ~/.config/funachat/telegram_diag_chat

# перевірка доступу
./tunnel/telemetry.sh --check

# одна відправка або постійний релей
./tunnel/telemetry.sh
./tunnel/telemetry.sh --loop 60
```

Файл після успішної відправки переїжджає в `telemetry/sent`; при помилці
лишається в черзі. Для автозапуску додайте `./telemetry.sh --loop 60` у
`~/.termux/boot/`.

Звіт містить версію, модель і Android, локаль, стан дозволів, лічильники
запитів/помилок та ознаку збою. Немає токенів, API-ключів, пошти, тексту
чатів, шляхів і IP.

### Варіант без Termux: Cloudflare Worker

`../telemetry-worker/` — невеликий Worker, який приймає звіти й пересилає їх
у Telegram. Токен бота зберігається в секретах Cloudflare, не в APK. URL
Worker вписується в **Довідка → Діагностичні дані → HTTPS-приймач**; якщо
поле порожнє, працює локальна черга й цей скрипт.
