# FunaChat

Android-клієнт для DeepSeek V4.1 Flash: чат, інструменти, субагенти, пам'ять,
локальний OpenAI-сумісний API і публічний доступ через тунель.

- Версія: **2.42.28** (code 103)
- Мінімальний Android: 8.0 (API 26)
- Мова інтерфейсу: українська, англійська, російська
- APK: [releases](https://github.com/Sqwid-member/Funachat/releases/latest)

## Що вміє

- Чат із Markdown, таблицями, кодом і KaTeX — усе рендериться офлайн.
- Черга повідомлень, версії відповідей, стиснення контексту до 1M токенів.
- Пам'ять (картки знань), папки, пошук, кілька акаунтів DeepSeek.
- Локальні інструменти: файли, PDF/DOCX, `funa-bridge` для Termux, субагенти.
- Локальний API: `http://127.0.0.1:8787/v1` (OpenAI-сумісний, SSE, tools).
- Кілька API-ключів із терміном дії; ключ `fk-` + 48 hex; публічний
  доступ можна вимкнути одним перемикачем.
- Аудит злитих ключів: `POST /v1/audit/key` показує власника, дозволяє
  відкликати ключ і повідомити власника (секрет пристрою або підпис
  розробника).
- Діагностична телеметрія: знеособлений знімок раз на добу, увімкнений за
  замовчуванням. Або напряму на Cloudflare Worker (токен бота — лише в
  секретах Worker), або через Termux-релей у Telegram.
- Блокування застосунку, шифрування токенів через Android Keystore.
- Публічний доступ: Cloudflare Tunnel або zrok, один origin для API й оновлень.
- Перевірка оновлень через `latest.json` у цьому репозиторії.

## Збірка

```bash
./build.sh /sdcard/Download/FunaChat.apk
```

Потрібні: `aapt2`, `javac` (JDK 17), `d8` (R8), `zipalign`, `apksigner`,
Android SDK platform 35. Ключ підпису — поза репозиторієм.

## Оновлення

`latest.json` у корені `main` — стабільний маніфест (raw.githubusercontent.com
і jsDelivr віддають `Access-Control-Allow-Origin: *`, тому перевірка працює із
WebView). Застосунок пробує джерела по черзі: заданий канал → тунель → raw →
jsDelivr, тож закешований raw не ховає нову версію:

```json
{"version":"2.42.28","code":103,"url":"…/FunaChat.apk","md5":"…","size":642389,"notes":"…"}
```

Публікація релізу:

```bash
./tunnel/publish.sh /sdcard/Download/FunaChat.apk 2.42.28 103 "що змінилось"
./tunnel/release-gh.sh 2.42.28 103 "що змінилось"
```

Скрипт бере токен із `~/.config/funachat/gh_token` (права `600`, у репозиторій
не потрапляє), комітить маніфест і створює тег `v<code>` з APK та `latest.json`.

## Публічний доступ

```bash
cd tunnel && ./start.sh     # Cloudflare quick tunnel, резерв — zrok
```

Один origin: `/v1/*` — API (ключ `fk-…`), `/updates/*` — APK і маніфест.
Застосунок слухає лише `127.0.0.1`; назовні не виставлені файлові
інструменти, `funa-bridge` і ADB. Деталі — [`tunnel/README.md`](tunnel/README.md).

## Документація

- [`CHANGELOG.md`](CHANGELOG.md) — журнал версій.
- [`API.md`](API.md) — локальний API, ліміти, приклади.
- [`tunnel/README.md`](tunnel/README.md) — тунелі, релізи, безпека.
