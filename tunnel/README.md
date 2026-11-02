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
./tunnel/release-gh.sh 2.42.20 95 "коротко, що змінилось"
```

Скрипт бере токен із `~/.config/funachat/gh_token` (права 600, у репозиторій
не потрапляє), створює тег `v<code>`, заливає APK і `latest.json`.
