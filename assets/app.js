/* FunaChat UI · M3 dark · i18n · memory */
(function () {
'use strict';

const $ = (id) => document.getElementById(id);
const el = (tag, cls, html) => { const n = document.createElement(tag); if (cls) n.className = cls; if (html != null) n.innerHTML = html; return n; };
const esc = (s) => String(s == null ? '' : s).replace(/[&<>"']/g, (c) => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const ic = (id, cls) => '<svg class="ic' + (cls ? ' ' + cls : '') + '"><use href="#' + id + '"/></svg>';

/* ---------------- i18n ---------------- */

const I18N = {
  uk: {
    localApi: 'Локальний API', apiAuto: 'Запускати із застосунком', apiLan: 'Доступ із локальної мережі',
    apiDel: 'Видаляти сесію після запиту', apiKey: 'API-ключ', apiRegen: 'Новий ключ',
    apiStart: 'Запустити', apiStop: 'Зупинити', apiOn: 'працює', apiOff: 'зупинено',
    apiHint: 'OpenAI-сумісний API: /v1/models, /v1/tools, /v1/chat/completions (стрім, tools, авто-тули, субагенти), /v1/completions',
    pubTitle: 'Публічний доступ (тунель)', pubFile: 'З файлу', pubCheck: 'Перевірити',
    pubClient: 'Копіювати конфіг клієнта',
    pubHint: 'Адресу ставить скрипт тунелю (tunnel/start.sh) — тут вона лише для читання. Вимкнений доступ ріже запити з тунелю (403), навіть якщо він запущений.',
    apiKeys: 'API-ключі', keyAdd: 'Створити ключ', keyNew: 'Новий API-ключ',
    keyHint: 'Кожен ключ можна відкликати окремо; термін дії перевіряється на кожному запиті.',
    keyNamePh: 'Назва (напр. «ноутбук»)', keyTtl: 'Термін дії',
    ttlNever: 'Безстроково', ttl1d: '1 день', ttl7d: '7 днів', ttl30d: '30 днів',
    ttl90d: '90 днів', ttl365d: '1 рік',
    keyCreate: 'Створити', keyDone: 'Готово',
    keyOnce: 'Скопіюйте ключ зараз — більше він не показується.',
    keyNoKeys: 'Ключів немає — API недоступний, поки не створите ключ.',
    keyNever: 'безстроковий', keyExpired: 'прострочений', keyExpires: 'до',
    keyDeleteAsk: 'Відкликати цей ключ? Клієнти з ним одразу втратять доступ.',
    pubAllow: 'Дозволити доступ із публічної мережі', pubForget: 'Забути адресу',
    auditTitle: 'Аудит ключів', auditRotate: 'Створити новий секрет',
    auditHint: 'Секрет аудиту дозволяє перевірити злитий ключ цього пристрою: показати власника, відкликати ключ і повідомити власника. Нікому не передавайте його.',
    auditContactPh: 'Контакт власника (email або @telegram)',
    auditHookPh: 'Webhook для повідомлень (https://…)',
    auditLogTitle: 'Журнал аудиту', auditLogHint: 'Тут видно перевірки, відкликання й повідомлення, зроблені через /v1/audit/key.',
    auditEmpty: 'Подій ще немає.',
    auditActInfo: 'перевірка', auditActRevoke: 'відкликання', auditActNotify: 'повідомлення',
    auditRotateAsk: 'Старий секрет перестане працювати. Створити новий?',
    teleTitle: 'Діагностичні дані', teleToggle: 'Надсилати діагностику',
    teleHint: 'Раз на добу: версія, модель пристрою, дозволи, лічильники та останні рядки логу. Токени, ключі, пошта й текст чатів вирізаються. Звіт іде на приватний приймач.',
    teleOff: 'Вимкнено — звіти не створюються.',
    teleReady: 'Увімкнено — звіт раз на добу.',
    teleQueue: 'у черзі', teleLastAt: 'останній',
    telePreview: 'Показати звіт', teleSend: 'Надіслати зараз',
    teleQueued: 'Діагностику додано в чергу.',
    teleError: 'Не вдалося зберегти діагностику.',
    teleNoRelay: 'Якщо Termux-релей не запущено, файли чекатимуть у Download/FunaFiles/telemetry.',
    teleSent: 'Діагностику надіслано.',
    pubNone: 'не налаштовано', pubChecking: 'Перевіряю…', pubOk: 'працює', pubBad: 'немає відповіді',
    pubCopied: 'Конфіг скопійовано',
    updCheck: 'Перевірити оновлення', updChecking: 'Перевіряю…',
    updNone: 'Канал оновлень не налаштований — вкажіть публічну адресу',
    updLatest: 'У вас остання версія', updAvail: 'Доступна версія', updFail: 'Не вдалося перевірити',
    updDownload: 'Завантажити', updBadJson: 'Некоректний маніфест оновлень',
    guard: 'Захист акаунта', guardOn: 'Безпечний темп запитів', guardGap: 'Мін. пауза, с',
    guardPerMin: 'Запитів на хвилину', guardPerDay: 'Запитів на добу',
    guardReset: 'Зняти паузу', guardToday: 'Сьогодні', guardPause: 'пауза',
    guardHint: 'Обмежує темп, щоб акаунт не виглядав як бот. Пауза ставиться сама після 401/403/429.',
    chat: 'Чат', foldAll: 'Згортати блоки автоматично',
    foldAllHint: 'Мислення, інструменти й субагенти завжди згорнуті — розгорнути можна дотиком.',
    aiStats: 'Статистика ШІ', statReq: 'Запитів', statDone: 'Відповідей', statStop: 'Зупинено',
    statTime: 'Загальний час', statAvgTime: 'Середній час', statTokIn: 'Токенів контексту',
    statTokOut: 'Токенів відповіді', statAvgTok: 'Середньо токенів', statEdit: 'Правок',
    statRegen: 'Регенерацій', statTool: 'Інструментів', statErr: 'Помилок', statSpeed: 'Швидкість',
    statLast: 'Останній запит', statReset: 'Скинути', statResetAsk: 'Скинути статистику ШІ?',
    statHint: 'Оцінка за символами (~1 токен на 3.2). Зберігається для активного акаунта.',
    aiWork: 'робота', versionPrev: 'Старіша версія', versionNext: 'Новіша версія',
    versionArchived: 'Ця версія до стиснення контексту — перемикання недоступне. Почни новий чат.',
    context: 'Контекст', ctxUsed: 'Використано', ctxMessages: 'Повідомлень',
    ctxThinking: 'Мислення, символів', ctxChars: 'Символів усього',
    ctxHint: 'Оцінка: ~1 токен на 3.2 символи. Коли контекст наближається до ліміту, він стискається автоматично перед надсиланням.',
    ctxLimit: 'Ліміт контексту',
    ctxLimitHint: 'Скільки токенів тримати в одній розмові до автоматичного стиснення. 1M — максимум DeepSeek V4.1 Flash.',
    close: 'Закрити',
    help: 'Допомога й логи', openHelp: 'Відкрити діагностику',
    helpHint: 'Перевірка термінала (Termux/ReTerminal), дозволів, токена й API. Тут можна надіслати лог.',
    helpIntro: 'Перевірка показує, що заважає застосунку працювати. Червоне — критично, жовте — бажано виправити.',
    helpEmpty: 'Немає даних діагностики',
    logs: 'Логи', logEmail: 'Email', logShare: 'Поділитися', logPreview: 'Переглянути звіт',
    logClear: 'Очистити', logClearAsk: 'Очистити лог і позначку про збій?',
    logsHint: 'Звіт містить версію, пристрій, діагностику й останні 300 рядків логу. Токен у лог не пишеться.',
 reportTitle: 'Звіт', reportLoading: 'Збираю звіт…',
    crashToast: 'Минулого разу застосунок впав. Відкрий «Допомога й логи» і надішли звіт.',
    
    introTitle: 'Ласкаво просимо у FunaChat',
    introSub: 'Приватний клієнт DeepSeek для Android: чат, файли, інструменти й локальний API — усе працює на телефоні.',
    introS1t: 'Увійди в DeepSeek',
    introS1b: 'Натисни «Увійти через сайт DeepSeek». Пароль і капчу обробляє сам сайт, застосунок отримує лише токен сесії.',
    introS2t: 'Дозволь доступ до файлів',
    introS2b: 'Налаштування → «Файли й дані» → «Керування всіма файлами». Без цього доступні лише Download і FunaFiles.',
    introS3t: 'Підключи міст термінала',
    introS3b: 'Постав Termux або ReTerminal з F-Droid, запусти funa-bridge і залиш сесію відкритою — так працюють команди й файли поза Download.',
    introS4t: 'Додай памʼять',
    introS4b: 'Меню → «Памʼять»: факти про тебе та проєкти, які модель має памʼятати в кожному чаті.',
    introS5t: 'Захисти застосунок',
    introS5b: 'Налаштування → «Акаунт і безпека» → «Блокування»: пароль запитується при кожному відкритті FunaChat.',
    introStart: 'Почати',
    introSkip: 'Пропустити',
    introAgain: 'Показати вступ ще раз',
    refreshSite: 'Оновити через сайт',
    verifyAccount: 'Пройти перевірку',
    accChallenge: 'потрібна перевірка',
    accChallengeHint: 'Пройди перевірку на сайті',
    authTitle: 'Акаунт DeepSeek', 
    authSub: 'Ідентичність у FunaChat — це акаунт DeepSeek. Токен зберігається зашифрованим на телефоні, пароль захищає сам застосунок.',
    authPass: 'Пароль', 
    authLogin: 'Увійти', 
    authLater: 'Пізніше',
    authProfile: 'Акаунт DeepSeek', authLock: 'Запитувати пароль при запуску',
    authNoProfile: 'DeepSeek не підключено',
    
    
    
    
    authLoggedOut: 'Вихід виконано',
    authLoginOk: 'Вітаю!',
    authSkip: 'Пропустити',
    authUnlock: 'Розблокувати', 
    dsLoginTitle: 'Вхід у DeepSeek', authUnlockTitle: 'Розблокувати FunaChat',
    dsSub: 'Один раз увійди на сайті DeepSeek — далі застосунок працює сам. Пароль не зберігається.',
    authUnlockSub: 'Введи локальний пароль FunaChat — він перевіряється на телефоні, без мережі.',
    authChecking: 'Перевіряю пароль…',
    authNeedPass: 'Введи пароль', authUnlocked: 'Розблоковано',
    authSwitch: 'Увійти в інший акаунт', 
    authTokenSaved: 'Токен збережено',
    authSite: 'Увійти через сайт DeepSeek',
    authKey: 'Ключ сесії',
    authKeyHint: 'Встав токен (Bearer) або рядок cookie від chat.deepseek.com. Застосунок сам перевірить, що підходить.',
    authKeyPh: 'eyJ… або ds_session_id=…',
    authKeySave: 'Імпортувати',
    authSiteHint: 'Відкриється справжня сторінка DeepSeek. Це разовий вхід: далі застосунок працює сам, пароль не зберігається.',
    
    docsTitle: 'Документація', openDocs: 'Документація', quickStart: 'Швидкі дії',
    qaLogin: 'Увійти', qaChat: 'Новий чат', qaMemory: "Пам'ять", qaApi: 'API', qaDocs: 'Документація',
    memFail: 'Не вдалося зберегти картку', cardSaved: 'Картку збережено',
    authNoPass: 'Спочатку постав пароль блокування — він зберігається лише на телефоні',
    lanWarn: 'LAN відкриває API для всієї мережі — тримай ключ приватним.',
    sessions: 'Сесії', newChat: 'Новий чат', memory: 'Пам\'ять', language: 'Мова',
    settings: 'Налаштування', settingsIndex: 'Зміст налаштувань', account: 'Акаунт DeepSeek', 
    settingsHint: 'Обери категорію — усередині лише те, що стосується однієї теми.',
    catAccount: 'Акаунт і безпека', catAccountSub: 'Вхід, акаунти, блокування, захист',
    catChat: 'Чат і ШІ', catChatSub: 'Системний промт, модель, мислення, пошук',
    catApi: 'Мережа та API', catApiSub: 'Локальний OpenAI-сумісний сервер і ключ',
    catData: 'Файли й дані', catDataSub: 'Доступ до /sdcard і збережені сторінки',
    catLook: 'Мова й вигляд', catLookSub: 'Мова інтерфейсу та згортання блоків',
    catStats: 'Статистика ШІ', catStatsSub: 'Запити, час відповіді, токени',
    catHelp: 'Довідка й застосунок', catHelpSub: 'Діагностика, документація, версія',
    import: 'Імпорт session.json', check: 'Перевірити',
    basePrompt: 'Системний промт', reset: 'Скинути',
    model: 'Модель', thinkingDefault: 'Мислення за замовчуванням', searchDefault: 'Пошук у мережі',
    files: 'Доступ до файлів', grant: 'Дати доступ до /sdcard',
    filesHint: 'Потрібно для читання файлів і моста термінала (Termux/ReTerminal). Робоча тека: /sdcard/Download/FunaFiles',
    about: 'Про застосунок',
    aboutText: 'FunaChat {v} · власний клієнт DeepSeek: файли, документи, зображення, пам\'ять, локальний API',
    updates: 'Історія оновлень', updatesTitle: 'Історія оновлень',
    updatesSub: 'Що змінилося в кожній версії FunaChat', updatesOpen: 'Відкрити історію оновлень',
    updatesNow: 'Поточна версія', updatesEarly: 'Ранні версії', updatesHint: 'Новіші оновлення — зверху. Поточна версія підсвічена.',
    save: 'Зберегти', welcomeTitle: 'FunaChat', welcomeSub: 'DeepSeek з інструментами, файлами й пам\'яттю',
    thinking: 'Мислення', search: 'Пошук', placeholder: 'Напиши повідомлення…',
    memoryHint: 'Картки додаються до промту в кожній новій сесії й належать активному акаунту DeepSeek.',
    memoryEmpty: 'Поки немає карток', newCard: 'Нова картка', editCard: 'Картка',
    cardTitlePh: 'Заголовок', cardBodyPh: 'Текст, факти, правила…', cardTagsPh: 'Теги через кому',
    delete: 'Видалити', rename: 'Перейменувати', cancel: 'Скасувати', edit: 'Змінити',
    enable: 'Увімкнути', disable: 'Вимкнути',
    saved: 'Збережено', untitled: 'Без назви', noSessions: 'Поки немає сесій',
    searchSessions: 'Пошук сесій', searchCards: 'Пошук карток', pin: 'Закріпити', unpin: 'Відкріпити',
    exportJson: 'Експорт', importJson: 'Імпорт', exported: 'Експортовано', imported: 'Імпортовано',
    pinned: 'Закріплено', nothing: 'Нічого не знайдено',
    copy: 'Копіювати', messageActions: 'Повідомлення', editing: 'Редагування повідомлення',
    accounts: 'Акаунти DeepSeek', addAccount: 'Додати акаунт', 
    accountsAll: 'Усі акаунти', accActive: 'активний', accExpired: 'токен не діє',
    accExpiredHint: 'Потрібно увійти знову', accEmpty: 'Поки немає жодного акаунта',
    accHint: 'Токени зберігаються зашифрованими (Keystore) і нікуди не надсилаються. Перемикання акаунта змінює, від чийого імені йдуть запити.',
    accDeleteAsk: 'Видалити акаунт із цього телефона?', accSwitched: 'Акаунт перемкнено',
    accOk: 'Токен діє', checking: 'Перевіряю…',
    lockTitle: 'Блокування паролем', lockHint: 'Це окремий пароль FunaChat. На телефоні зберігається лише PBKDF2-хеш, пароль нікуди не надсилається. Він захищає застосунок, а не акаунт DeepSeek.',
    lockEnable: 'Увімкнути блокування', lockShort: 'Пароль — мінімум 6 символів',
    lockForget: 'Забув пароль — увійти в DeepSeek', lockForgetHint: 'Увійди в DeepSeek заново — локальне блокування скинеться, і можна буде поставити новий пароль.',
    lockOn: 'Блокування увімкнено', lockOff: 'Блокування вимкнено',
    lockReset: 'Локальне блокування скинуто — постав новий пароль у Налаштуваннях.',
    authNoProfileHint: 'Увійди в DeepSeek, щоб почати',
    active: 'активний', 
    folders: 'Папки', allChats: 'Усі', unfiled: 'Без папки', newFolder: 'Нова папка',
    folderName: 'Назва папки', moveToFolder: 'Перемістити в папку', folder: 'Папка',
    noFolder: 'Без папки', folderDelete: 'Видалити папку?', interactive: 'Інтерактивна сторінка', buildingPage: 'Сторінка збирається…', showCode: 'Показати код', hideCode: 'Згорнути код', showFull: 'Показати повністю', showLess: 'Згорнути',
    folderNamePh: 'Назва папки', renameFolder: 'Перейменувати папку', createFolder: 'Нова папка',
    noFind: 'Немає збігів', continue: 'Продовжити', regen: 'Перегенерувати',
    regenHint: 'Перегенерувати відповідь', exportChat: 'Експорт у Markdown', chatExported: 'Чат збережено',
    pages: 'Збережені сторінки', openPages: 'Відкрити список', pagesHint: 'HTML-сторінки, які створив ШІ',
    pagesEmpty: 'Ще немає збережених сторінок', openPage: 'Відкрити', deletePage: 'Видалити сторінку?',
    delete: 'Видалити', done: 'готово', you: 'Я',
    attach: 'Прикріпити файл', fileUploading: 'надсилаю…', fileWait: 'Файл ще обробляється',
    fileError: 'Не вдалося', files: 'Файли', fileLimit: 'Не більше 40 файлів на повідомлення',
    expand: 'Розгорнути', download: 'Завантажити', savedTo: 'Збережено',
    viewFile: 'Відкрити', openWith: 'Відкрити за допомогою', shareFile: 'Поширити', saveFile: 'Зберегти копію',
    loadingFile: 'Завантаження…', fileMissing: 'Файл не знайдено', pdfMore: 'Показати ще сторінки',
    pdfLimit: 'Показано перші {n} сторінок',
    compactCtx: 'Стиснути контекст', compacting: 'Стискаю контекст…', compacted: 'Контекст стиснуто',
    compactAuto: 'Контекст майже заповнений — стискаю…', compactError: 'Не вдалося стиснути контекст',
    queueTitle: 'Черга', queueHint: 'піде після відповіді', queueAdded: 'Додано в чергу',
    queueEdit: 'Повідомлення в черзі', queueUp: 'Вище', queueDown: 'Нижче',
    queueRemove: 'Прибрати з черги', queuePaused: 'Черга на паузі',
    queueResume: 'Запустити чергу', queueRestored: 'Не надіслано — повернуто в чергу',
    queueCtxFull: 'Контекст заповнений: стисни його, потім запусти чергу',
    queueLeave: 'У черзі ще {n} повідомлень. Перейти й очистити чергу?',
    olderHidden: 'Старіших повідомлень приховано: {n}',
    pagesCount: 'стор.',
    noPreview: 'Попередній перегляд недоступний',
    copied: 'Скопійовано', needToken: 'Встав токен у налаштуваннях',
    accessOk: 'Доступ до /sdcard надано', accessNo: 'Доступ до /sdcard не надано',
    delSession: 'Видалити цю сесію?', delCard: 'Видалити картку?', renamePrompt: 'Нова назва сесії',
    stopped: 'зупинено', answering: 'відповідаю…', stopFirst: 'Спочатку зупини відповідь',
    subagent: 'Субагент', subOpen: 'Відкрити сесію', subReadonly: 'лише перегляд',
    subRunning: 'працює…', subDone: 'готово', subStopped: 'зупинено', subFailed: 'помилка',
    subEmpty: 'Сесія субагента порожня', subFinishedToast: 'Субагент завершив роботу',
    subStop: 'Зупинити', 
    error: 'Помилка', cards: 'карток',
    q1: 'Файли', q1s: 'Огляд робочої теки', q1p: 'Прочитай /sdcard/Download/FunaFiles і скажи, що там є.',
    q2: 'Термінал', q2s: 'Команди на пристрої', q2p: 'Покажи, де виконуються команди (Termux чи ReTerminal), версію sh і вільне місце на диску.',
    q3: 'План', q3s: 'Покрокове мислення', q3p: 'Допоможи спланувати невеликий Android-застосунок українською.'
  },
  en: {
    localApi: 'Local API', apiAuto: 'Start with the app', apiLan: 'Allow local network access',
    apiDel: 'Delete the session after each request', apiKey: 'API key', apiRegen: 'New key',
    apiStart: 'Start', apiStop: 'Stop', apiOn: 'running', apiOff: 'stopped',
    apiHint: 'OpenAI-compatible API: /v1/models, /v1/tools, /v1/chat/completions (stream, tools, auto tools, subagents), /v1/completions',
    pubTitle: 'Public access (tunnel)', pubFile: 'From file', pubCheck: 'Check',
    pubClient: 'Copy client config',
    pubHint: 'The tunnel script (tunnel/start.sh) sets this address — it is read-only here. Turning public access off rejects tunnel requests (403) even while the tunnel runs.',
    apiKeys: 'API keys', keyAdd: 'Create key', keyNew: 'New API key',
    keyHint: 'Each key can be revoked separately; expiry is checked on every request.',
    keyNamePh: 'Name (e.g. “laptop”)', keyTtl: 'Expires in',
    ttlNever: 'Never', ttl1d: '1 day', ttl7d: '7 days', ttl30d: '30 days',
    ttl90d: '90 days', ttl365d: '1 year',
    keyCreate: 'Create', keyDone: 'Done',
    keyOnce: 'Copy the key now — it will not be shown again.',
    keyNoKeys: 'No keys — the API is unavailable until you create one.',
    keyNever: 'no expiry', keyExpired: 'expired', keyExpires: 'until',
    keyDeleteAsk: 'Revoke this key? Clients using it lose access immediately.',
    pubAllow: 'Allow access from the public network', pubForget: 'Forget address',
    auditTitle: 'Key audit', auditRotate: 'Generate a new secret',
    auditHint: 'The audit secret lets you inspect a leaked key from this device: show the owner, revoke the key and notify the owner. Never share it.',
    auditContactPh: 'Owner contact (email or @telegram)',
    auditHookPh: 'Notification webhook (https://…)',
    auditLogTitle: 'Audit log', auditLogHint: 'Checks, revocations and notifications made through /v1/audit/key appear here.',
    auditEmpty: 'No events yet.',
    auditActInfo: 'check', auditActRevoke: 'revocation', auditActNotify: 'notification',
    auditRotateAsk: 'The old secret will stop working. Generate a new one?',
    teleTitle: 'Diagnostic data', teleToggle: 'Send diagnostics',
    teleHint: 'Once a day: version, device model, permissions, counters and the latest log lines. Tokens, keys, email and chat text are stripped. The report goes to a private receiver.',
    teleOff: 'Off — no reports are created.',
    teleReady: 'On — one report a day.',
    teleQueue: 'queued', teleLastAt: 'last',
    telePreview: 'Show report', teleSend: 'Send now',
    teleQueued: 'Diagnostics queued.',
    teleError: 'Could not save diagnostics.',
    teleNoRelay: 'If the Termux relay is not running, files wait in Download/FunaFiles/telemetry.',
    teleSent: 'Diagnostics sent.',
    pubNone: 'not set', pubChecking: 'Checking…', pubOk: 'reachable', pubBad: 'no response',
    pubCopied: 'Config copied',
    updCheck: 'Check for updates', updChecking: 'Checking…',
    updNone: 'Update channel is not set — add the public URL',
    updLatest: 'You are on the latest version', updAvail: 'Available version', updFail: 'Check failed',
    updDownload: 'Download', updBadJson: 'Invalid update manifest',
    guard: 'Account protection', guardOn: 'Safe request pacing', guardGap: 'Min gap, s',
    guardPerMin: 'Requests per minute', guardPerDay: 'Requests per day',
    guardReset: 'Clear pause', guardToday: 'Today', guardPause: 'pause',
    guardHint: 'Limits the pace so the account does not look like a bot. Pauses automatically on 401/403/429.',
    chat: 'Chat', foldAll: 'Auto-collapse blocks',
    foldAllHint: 'Thinking, tools and subagents stay collapsed — tap to expand.',
    aiStats: 'AI usage', statReq: 'Requests', statDone: 'Answers', statStop: 'Stopped',
    statTime: 'Total time', statAvgTime: 'Average time', statTokIn: 'Context tokens',
    statTokOut: 'Answer tokens', statAvgTok: 'Avg tokens', statEdit: 'Edits',
    statRegen: 'Regenerations', statTool: 'Tool calls', statErr: 'Errors', statSpeed: 'Throughput',
    statLast: 'Last request', statReset: 'Reset', statResetAsk: 'Reset AI statistics?',
    statHint: 'Estimated from characters (~1 token per 3.2). Stored for the active account.',
    aiWork: 'worked', versionPrev: 'Older version', versionNext: 'Newer version',
    versionArchived: 'This version is before context compaction — switching is unavailable. Start a new chat.',
    context: 'Context', ctxUsed: 'Used', ctxMessages: 'Messages',
    ctxThinking: 'Thinking chars', ctxChars: 'Total characters',
    ctxHint: 'Estimate: ~1 token per 3.2 characters. When the context approaches the limit it is compacted automatically before sending.',
    ctxLimit: 'Context limit',
    ctxLimitHint: 'How many tokens to keep in one conversation before automatic compaction. 1M is the DeepSeek V4.1 Flash maximum.',
    close: 'Close',
    help: 'Help & logs', openHelp: 'Open diagnostics',
    helpHint: 'Checks the terminal (Termux/ReTerminal), permissions, token and API. You can send the log here.',
    helpIntro: 'The check shows what blocks the app. Red is critical, amber is worth fixing.',
    helpEmpty: 'No diagnostics yet',
    logs: 'Logs', logEmail: 'Email', logShare: 'Share', logPreview: 'Preview report',
    logClear: 'Clear', logClearAsk: 'Clear the log and the crash marker?',
    logsHint: 'The report includes version, device, diagnostics and the last 300 log lines. The token is never logged.',
 reportTitle: 'Report', reportLoading: 'Building report…',
    crashToast: 'The app crashed last time. Open “Help & logs” and send the report.',
    
    introTitle: 'Welcome to FunaChat',
    introSub: 'A private DeepSeek client for Android: chat, files, tools and a local API — everything runs on the phone.',
    introS1t: 'Sign in to DeepSeek',
    introS1b: 'Tap “Sign in via the DeepSeek site”. The site handles the password and captcha; the app only receives the session token.',
    introS2t: 'Allow file access',
    introS2b: 'Settings → “Files & data” → “All files access”. Without it only Download and FunaFiles are available.',
    introS3t: 'Connect the terminal bridge',
    introS3b: 'Install Termux or ReTerminal from F-Droid, run funa-bridge and keep the session open — that is how commands and files outside Download work.',
    introS4t: 'Add memory',
    introS4b: 'Menu → “Memory”: facts about you and your projects that the model should remember in every chat.',
    introS5t: 'Protect the app',
    introS5b: 'Settings → “Account & security” → “App lock”: a password is asked every time FunaChat opens.',
    introStart: 'Get started',
    introSkip: 'Skip',
    introAgain: 'Show the intro again',
    refreshSite: 'Refresh via the site',
    verifyAccount: 'Complete the check',
    accChallenge: 'check required',
    accChallengeHint: 'Complete the check on the site',
    authTitle: 'DeepSeek account', 
    authSub: 'Your FunaChat identity is the DeepSeek account. The token is encrypted on the phone; the password locks the app itself.',
    authPass: 'Password', 
    authLogin: 'Sign in', 
    authLater: 'Later',
    authProfile: 'DeepSeek account', authLock: 'Ask for the password on launch',
    authNoProfile: 'DeepSeek is not connected',
    
    
    
    
    authLoggedOut: 'Signed out',
    authLoginOk: 'Welcome!',
    authSkip: 'Skip',
    authUnlock: 'Unlock', 
    dsLoginTitle: 'Sign in to DeepSeek', authUnlockTitle: 'Unlock FunaChat',
    dsSub: 'Sign in to the DeepSeek site once — after that the app works on its own. The password is not stored.',
    authUnlockSub: 'Enter the local FunaChat password — it is checked on the phone, offline.',
    authChecking: 'Checking the password…',
    authNeedPass: 'Enter the password', authUnlocked: 'Unlocked',
    authSwitch: 'Sign in to another account', 
    authTokenSaved: 'Token saved',
    authSite: 'Sign in via the DeepSeek site',
    authKey: 'Session key',
    authKeyHint: 'Paste a Bearer token or a cookie string from chat.deepseek.com. The app checks which one works.',
    authKeyPh: 'eyJ… or ds_session_id=…',
    authKeySave: 'Import',
    authSiteHint: 'The real DeepSeek page opens. This is a one-time sign-in: after that the app works on its own, the password is not stored.',
    
    docsTitle: 'Documentation', openDocs: 'Documentation', quickStart: 'Quick actions',
    qaLogin: 'Sign in', qaChat: 'New chat', qaMemory: 'Memory', qaApi: 'API', qaDocs: 'Docs',
    memFail: 'Could not save the card', cardSaved: 'Card saved',
    authNoPass: 'Set a lock password first — it is stored only on the phone',
    lanWarn: 'LAN exposes the API to the whole network — keep the key private.',
    sessions: 'Sessions', newChat: 'New chat', memory: 'Memory', language: 'Language',
    settings: 'Settings', settingsIndex: 'Settings sections', account: 'DeepSeek account', 
    settingsHint: 'Pick a category — each page holds only related settings.',
    catAccount: 'Account & security', catAccountSub: 'Sign-in, accounts, app lock, protection',
    catChat: 'Chat & AI', catChatSub: 'System prompt, model, thinking, search',
    catApi: 'Network & API', catApiSub: 'Local OpenAI-compatible server and key',
    catData: 'Files & data', catDataSub: 'Storage access and saved pages',
    catLook: 'Language & look', catLookSub: 'Interface language and block folding',
    catStats: 'AI usage', catStatsSub: 'Requests, response time, tokens',
    catHelp: 'Help & app', catHelpSub: 'Diagnostics, docs, version',
    import: 'Import session.json', check: 'Verify',
    basePrompt: 'System prompt', reset: 'Reset',
    model: 'Model', thinkingDefault: 'Thinking by default', searchDefault: 'Web search',
    files: 'Storage access', grant: 'Allow /sdcard',
    filesHint: 'Needed to read files and for the terminal bridge (Termux/ReTerminal). Workspace: /sdcard/Download/FunaFiles',
    about: 'About',
    aboutText: 'FunaChat {v} · a custom DeepSeek client with files, documents, images, memory and a local API',
    updates: 'Update log', updatesTitle: 'Update log',
    updatesSub: 'What changed in every FunaChat version', updatesOpen: 'Open the update log',
    updatesNow: 'Current version', updatesEarly: 'Early versions', updatesHint: 'Newer releases come first. Your current version is highlighted.',
    save: 'Save', welcomeTitle: 'FunaChat', welcomeSub: 'DeepSeek with tools, files and memory',
    thinking: 'Thinking', search: 'Search', placeholder: 'Message FunaChat…',
    memoryHint: 'Cards are added to the prompt in every new session and belong to the active DeepSeek account.',
    memoryEmpty: 'No cards yet', newCard: 'New card', editCard: 'Card',
    cardTitlePh: 'Title', cardBodyPh: 'Text, facts, rules…', cardTagsPh: 'Comma separated tags',
    delete: 'Delete', rename: 'Rename', cancel: 'Cancel', edit: 'Edit',
    enable: 'Enable', disable: 'Disable',
    saved: 'Saved', untitled: 'Untitled', noSessions: 'No sessions yet',
    searchSessions: 'Search sessions', searchCards: 'Search cards', pin: 'Pin', unpin: 'Unpin',
    exportJson: 'Export', importJson: 'Import', exported: 'Exported', imported: 'Imported',
    pinned: 'Pinned', nothing: 'Nothing found',
    copy: 'Copy', messageActions: 'Message', editing: 'Editing message',
    accounts: 'DeepSeek accounts', addAccount: 'Add account', 
    accountsAll: 'All accounts', accActive: 'active', accExpired: 'token expired',
    accExpiredHint: 'Sign in again', accEmpty: 'No accounts yet',
    accHint: 'Tokens are encrypted with the Android Keystore and never leave the phone. Switching an account changes whose identity requests use.',
    accDeleteAsk: 'Remove this account from the phone?', accSwitched: 'Account switched',
    accOk: 'The token works', checking: 'Checking…',
    lockTitle: 'Password lock', lockHint: 'This is a separate FunaChat password. Only a PBKDF2 hash is stored on the phone; the password is never sent anywhere. It protects the app, not the DeepSeek account.',
    lockEnable: 'Enable the lock', lockShort: 'Password must be at least 6 characters',
    lockForget: 'Forgot the password — sign in to DeepSeek', lockForgetHint: 'Sign in to DeepSeek again — the local lock is reset and you can set a new password.',
    lockOn: 'Password lock enabled', lockOff: 'Password lock disabled',
    lockReset: 'The local lock was reset — set a new password in Settings.',
    authNoProfileHint: 'Sign in to DeepSeek to start',
    active: 'active', 
    folders: 'Folders', allChats: 'All', unfiled: 'Unfiled', newFolder: 'New folder',
    folderName: 'Folder name', moveToFolder: 'Move to folder', folder: 'Folder',
    noFolder: 'No folder', folderDelete: 'Delete this folder?', interactive: 'Interactive page', buildingPage: 'Building the page…', showCode: 'Show code', hideCode: 'Hide code', showFull: 'Show full answer', showLess: 'Collapse',
    folderNamePh: 'Folder name', renameFolder: 'Rename folder', createFolder: 'New folder',
    noFind: 'No matches', continue: 'Continue', regen: 'Regenerate',
    regenHint: 'Regenerate answer', exportChat: 'Export as Markdown', chatExported: 'Chat saved',
    pages: 'Saved pages', openPages: 'Open list', pagesHint: 'HTML pages created by the AI',
    pagesEmpty: 'No saved pages yet', openPage: 'Open', deletePage: 'Delete this page?',
    delete: 'Delete', done: 'done', you: 'You',
    attach: 'Attach file', fileUploading: 'uploading…', fileWait: 'File is still processing',
    fileError: 'Failed', files: 'Files', fileLimit: 'At most 40 files per message',
    expand: 'Expand', download: 'Download', savedTo: 'Saved to',
    viewFile: 'Open', openWith: 'Open with', shareFile: 'Share', saveFile: 'Save a copy',
    loadingFile: 'Loading…', fileMissing: 'File not found', pdfMore: 'Load more pages',
    pdfLimit: 'Showing the first {n} pages',
    compactCtx: 'Compact context', compacting: 'Compacting context…', compacted: 'Context compacted',
    compactAuto: 'Context is almost full — compacting…', compactError: 'Could not compact the context',
    queueTitle: 'Queue', queueHint: 'sends after the answer', queueAdded: 'Added to the queue',
    queueEdit: 'Queued message', queueUp: 'Move up', queueDown: 'Move down',
    queueRemove: 'Remove from queue', queuePaused: 'Queue paused',
    queueResume: 'Resume queue', queueRestored: 'Not sent — returned to the queue',
    queueCtxFull: 'Context is full: compact it, then resume the queue',
    queueLeave: 'There are {n} messages in the queue. Leave and clear it?',
    olderHidden: 'Older messages hidden: {n}',
    pagesCount: 'pp.',
    noPreview: 'No preview available',
    copied: 'Copied', needToken: 'Paste your token in settings',
    accessOk: 'Storage access granted', accessNo: 'Storage access not granted',
    delSession: 'Delete this session?', delCard: 'Delete this card?', renamePrompt: 'New session title',
    stopped: 'stopped', answering: 'answering…', stopFirst: 'Stop the current answer first',
    subagent: 'Subagent', subOpen: 'Open session', subReadonly: 'read-only',
    subRunning: 'running…', subDone: 'done', subStopped: 'stopped', subFailed: 'error',
    subEmpty: 'The subagent session is empty', subFinishedToast: 'Subagent finished',
    subStop: 'Stop', 
    error: 'Error', cards: 'cards',
    q1: 'Files', q1s: 'Browse the workspace', q1p: 'Read /sdcard/Download/FunaFiles and tell me what is there.',
    q2: 'Terminal', q2s: 'Run commands on device', q2p: 'Show where commands run (Termux or ReTerminal), the sh version and free disk space.',
    q3: 'Plan', q3s: 'Step by step thinking', q3p: 'Help me plan a small Android app in English.'
  },
  ru: {
    localApi: 'Локальный API', apiAuto: 'Запускать с приложением', apiLan: 'Доступ из локальной сети',
    apiDel: 'Удалять сессию после запроса', apiKey: 'API-ключ', apiRegen: 'Новый ключ',
    apiStart: 'Запустить', apiStop: 'Остановить', apiOn: 'работает', apiOff: 'остановлен',
    apiHint: 'OpenAI-совместимый API: /v1/models, /v1/tools, /v1/chat/completions (стрим, tools, авто-инструменты, субагенты), /v1/completions',
    pubTitle: 'Публичный доступ (туннель)', pubFile: 'Из файла', pubCheck: 'Проверить',
    pubClient: 'Копировать конфиг клиента',
    pubHint: 'Адрес задаёт скрипт туннеля (tunnel/start.sh) — здесь он только для чтения. Выключенный доступ отклоняет запросы из туннеля (403), даже если он запущен.',
    apiKeys: 'API-ключи', keyAdd: 'Создать ключ', keyNew: 'Новый API-ключ',
    keyHint: 'Каждый ключ можно отозвать отдельно; срок действия проверяется на каждом запросе.',
    keyNamePh: 'Название (напр. «ноутбук»)', keyTtl: 'Срок действия',
    ttlNever: 'Бессрочно', ttl1d: '1 день', ttl7d: '7 дней', ttl30d: '30 дней',
    ttl90d: '90 дней', ttl365d: '1 год',
    keyCreate: 'Создать', keyDone: 'Готово',
    keyOnce: 'Скопируйте ключ сейчас — больше он не показывается.',
    keyNoKeys: 'Ключей нет — API недоступен, пока не создадите ключ.',
    keyNever: 'бессрочный', keyExpired: 'просрочен', keyExpires: 'до',
    keyDeleteAsk: 'Отозвать этот ключ? Клиенты с ним сразу потеряют доступ.',
    pubAllow: 'Разрешить доступ из публичной сети', pubForget: 'Забыть адрес',
    auditTitle: 'Аудит ключей', auditRotate: 'Создать новый секрет',
    auditHint: 'Секрет аудита позволяет проверить утёкший ключ этого устройства: показать владельца, отозвать ключ и уведомить владельца. Никому не передавайте его.',
    auditContactPh: 'Контакт владельца (email или @telegram)',
    auditHookPh: 'Webhook для уведомлений (https://…)',
    auditLogTitle: 'Журнал аудита', auditLogHint: 'Здесь видны проверки, отзывы и уведомления через /v1/audit/key.',
    auditEmpty: 'Событий пока нет.',
    auditActInfo: 'проверка', auditActRevoke: 'отзыв', auditActNotify: 'уведомление',
    auditRotateAsk: 'Старый секрет перестанет работать. Создать новый?',
    teleTitle: 'Диагностические данные', teleToggle: 'Отправлять диагностику',
    teleHint: 'Раз в сутки: версия, модель устройства, разрешения, счётчики и последние строки лога. Токены, ключи, почта и текст чатов вырезаются. Отчёт идёт на приватный приёмник.',
    teleOff: 'Выключено — отчёты не создаются.',
    teleReady: 'Включено — отчёт раз в сутки.',
    teleQueue: 'в очереди', teleLastAt: 'последний',
    telePreview: 'Показать отчёт', teleSend: 'Отправить сейчас',
    teleQueued: 'Диагностика добавлена в очередь.',
    teleError: 'Не удалось сохранить диагностику.',
    teleNoRelay: 'Если Termux-релей не запущен, файлы ждут в Download/FunaFiles/telemetry.',
    teleSent: 'Диагностика отправлена.',
    pubNone: 'не настроено', pubChecking: 'Проверяю…', pubOk: 'работает', pubBad: 'нет ответа',
    pubCopied: 'Конфиг скопирован',
    updCheck: 'Проверить обновления', updChecking: 'Проверяю…',
    updNone: 'Канал обновлений не настроен — укажите публичный адрес',
    updLatest: 'У вас последняя версия', updAvail: 'Доступна версия', updFail: 'Не удалось проверить',
    updDownload: 'Скачать', updBadJson: 'Некорректный манифест обновлений',
    guard: 'Защита аккаунта', guardOn: 'Безопасный темп запросов', guardGap: 'Мин. пауза, с',
    guardPerMin: 'Запросов в минуту', guardPerDay: 'Запросов в сутки',
    guardReset: 'Снять паузу', guardToday: 'Сегодня', guardPause: 'пауза',
    guardHint: 'Ограничивает темп, чтобы аккаунт не выглядел как бот. Пауза ставится сама после 401/403/429.',
    chat: 'Чат', foldAll: 'Сворачивать блоки автоматически',
    foldAllHint: 'Мышление, инструменты и субагенты всегда свёрнуты — раскрыть можно касанием.',
    aiStats: 'Статистика ИИ', statReq: 'Запросов', statDone: 'Ответов', statStop: 'Остановлено',
    statTime: 'Общее время', statAvgTime: 'Среднее время', statTokIn: 'Токенов контекста',
    statTokOut: 'Токенов ответа', statAvgTok: 'В среднем токенов', statEdit: 'Правок',
    statRegen: 'Регенераций', statTool: 'Инструментов', statErr: 'Ошибок', statSpeed: 'Скорость',
    statLast: 'Последний запрос', statReset: 'Сбросить', statResetAsk: 'Сбросить статистику ИИ?',
    statHint: 'Оценка по символам (~1 токен на 3.2). Хранится для активного аккаунта.',
    aiWork: 'работа', versionPrev: 'Старая версия', versionNext: 'Новая версия',
    versionArchived: 'Эта версия до сжатия контекста — переключение недоступно. Начни новый чат.',
    context: 'Контекст', ctxUsed: 'Использовано', ctxMessages: 'Сообщений',
    ctxThinking: 'Мышление, символов', ctxChars: 'Символов всего',
    ctxHint: 'Оценка: ~1 токен на 3.2 символа. Когда контекст приближается к лимиту, он сжимается автоматически перед отправкой.',
    ctxLimit: 'Лимит контекста',
    ctxLimitHint: 'Сколько токенов держать в одном разговоре до автоматического сжатия. 1M — максимум DeepSeek V4.1 Flash.',
    close: 'Закрыть',
    help: 'Помощь и логи', openHelp: 'Открыть диагностику',
    helpHint: 'Проверка терминала (Termux/ReTerminal), разрешений, токена и API. Здесь можно отправить лог.',
    helpIntro: 'Проверка показывает, что мешает приложению. Красное — критично, жёлтое — желательно исправить.',
    helpEmpty: 'Нет данных диагностики',
    logs: 'Логи', logEmail: 'Email', logShare: 'Поделиться', logPreview: 'Посмотреть отчёт',
    logClear: 'Очистить', logClearAsk: 'Очистить лог и отметку о сбое?',
    logsHint: 'Отчёт содержит версию, устройство, диагностику и последние 300 строк лога. Токен в лог не пишется.',
 reportTitle: 'Отчёт', reportLoading: 'Собираю отчёт…',
    crashToast: 'В прошлый раз приложение упало. Открой «Помощь и логи» и отправь отчёт.',
    
    introTitle: 'Добро пожаловать в FunaChat',
    introSub: 'Приватный клиент DeepSeek для Android: чат, файлы, инструменты и локальный API — всё работает на телефоне.',
    introS1t: 'Войди в DeepSeek',
    introS1b: 'Нажми «Войти через сайт DeepSeek». Пароль и капчу обрабатывает сам сайт, приложение получает только токен сессии.',
    introS2t: 'Разреши доступ к файлам',
    introS2b: 'Настройки → «Файлы и данные» → «Управление всеми файлами». Без этого доступны только Download и FunaFiles.',
    introS3t: 'Подключи мост терминала',
    introS3b: 'Установи Termux или ReTerminal из F-Droid, запусти funa-bridge и оставь сессию открытой — так работают команды и файлы вне Download.',
    introS4t: 'Добавь память',
    introS4b: 'Меню → «Память»: факты о тебе и проектах, которые модель должна помнить в каждом чате.',
    introS5t: 'Защити приложение',
    introS5b: 'Настройки → «Аккаунт и безопасность» → «Блокировка»: пароль спрашивается при каждом открытии FunaChat.',
    introStart: 'Начать',
    introSkip: 'Пропустить',
    introAgain: 'Показать вступление ещё раз',
    refreshSite: 'Обновить через сайт',
    verifyAccount: 'Пройти проверку',
    accChallenge: 'нужна проверка',
    accChallengeHint: 'Пройди проверку на сайте',
    authTitle: 'Аккаунт DeepSeek', 
    authSub: 'Твоя личность в FunaChat — это аккаунт DeepSeek. Токен хранится зашифрованным на телефоне, пароль защищает само приложение.',
    authPass: 'Пароль', 
    authLogin: 'Войти', 
    authLater: 'Позже',
    authProfile: 'Аккаунт DeepSeek', authLock: 'Спрашивать пароль при запуске',
    authNoProfile: 'DeepSeek не подключён',
    
    
    
    
    authLoggedOut: 'Выход выполнен',
    authLoginOk: 'Добро пожаловать!',
    authSkip: 'Пропустить',
    authUnlock: 'Разблокировать', 
    dsLoginTitle: 'Вход в DeepSeek', authUnlockTitle: 'Разблокировать FunaChat',
    dsSub: 'Один раз войди на сайте DeepSeek — дальше приложение работает само. Пароль не сохраняется.',
    authUnlockSub: 'Введи локальный пароль FunaChat — он проверяется на телефоне, без сети.',
    authChecking: 'Проверяю пароль…',
    authNeedPass: 'Введи пароль', authUnlocked: 'Разблокировано',
    authSwitch: 'Войти в другой аккаунт', 
    authTokenSaved: 'Токен сохранён',
    authSite: 'Войти через сайт DeepSeek',
    authKey: 'Ключ сессии',
    authKeyHint: 'Вставь токен (Bearer) или строку cookie от chat.deepseek.com. Приложение само проверит, что подходит.',
    authKeyPh: 'eyJ… или ds_session_id=…',
    authKeySave: 'Импортировать',
    authSiteHint: 'Откроется настоящая страница DeepSeek. Это разовый вход: дальше приложение работает само, пароль не сохраняется.',
    
    docsTitle: 'Документация', openDocs: 'Документация', quickStart: 'Быстрые действия',
    qaLogin: 'Войти', qaChat: 'Новый чат', qaMemory: 'Память', qaApi: 'API', qaDocs: 'Документация',
    memFail: 'Не удалось сохранить карточку', cardSaved: 'Карточка сохранена',
    authNoPass: 'Сначала задай пароль блокировки — он хранится только на телефоне',
    lanWarn: 'LAN открывает API для всей сети — держи ключ приватным.',
    sessions: 'Сессии', newChat: 'Новый чат', memory: 'Память', language: 'Язык',
    settings: 'Настройки', settingsIndex: 'Разделы настроек', account: 'Аккаунт DeepSeek', 
    settingsHint: 'Выбери категорию — внутри только то, что относится к одной теме.',
    catAccount: 'Аккаунт и безопасность', catAccountSub: 'Вход, аккаунты, блокировка, защита',
    catChat: 'Чат и ИИ', catChatSub: 'Системный промт, модель, мышление, поиск',
    catApi: 'Сеть и API', catApiSub: 'Локальный OpenAI-совместимый сервер и ключ',
    catData: 'Файлы и данные', catDataSub: 'Доступ к /sdcard и сохранённые страницы',
    catLook: 'Язык и вид', catLookSub: 'Язык интерфейса и сворачивание блоков',
    catStats: 'Статистика ИИ', catStatsSub: 'Запросы, время ответа, токены',
    catHelp: 'Справка и приложение', catHelpSub: 'Диагностика, документация, версия',
    import: 'Импорт session.json', check: 'Проверить',
    basePrompt: 'Системный промт', reset: 'Сбросить',
    model: 'Модель', thinkingDefault: 'Мышление по умолчанию', searchDefault: 'Поиск в сети',
    files: 'Доступ к файлам', grant: 'Дать доступ к /sdcard',
    filesHint: 'Нужно для чтения файлов и моста терминала (Termux/ReTerminal). Рабочая папка: /sdcard/Download/FunaFiles',
    about: 'О приложении',
    aboutText: 'FunaChat {v} · свой клиент DeepSeek: файлы, документы, изображения, память, локальный API',
    updates: 'История обновлений', updatesTitle: 'История обновлений',
    updatesSub: 'Что изменилось в каждой версии FunaChat', updatesOpen: 'Открыть историю обновлений',
    updatesNow: 'Текущая версия', updatesEarly: 'Ранние версии', updatesHint: 'Новые обновления — сверху. Текущая версия подсвечена.',
    save: 'Сохранить', welcomeTitle: 'FunaChat', welcomeSub: 'DeepSeek с инструментами, файлами и памятью',
    thinking: 'Мышление', search: 'Поиск', placeholder: 'Напиши сообщение…',
    memoryHint: 'Карточки добавляются в промт в каждой новой сессии и принадлежат активному аккаунту DeepSeek.',
    memoryEmpty: 'Пока нет карточек', newCard: 'Новая карточка', editCard: 'Карточка',
    cardTitlePh: 'Заголовок', cardBodyPh: 'Текст, факты, правила…', cardTagsPh: 'Теги через запятую',
    delete: 'Удалить', rename: 'Переименовать', cancel: 'Отмена', edit: 'Изменить',
    enable: 'Включить', disable: 'Выключить',
    saved: 'Сохранено', untitled: 'Без названия', noSessions: 'Пока нет сессий',
    searchSessions: 'Поиск сессий', searchCards: 'Поиск карточек', pin: 'Закрепить', unpin: 'Открепить',
    exportJson: 'Экспорт', importJson: 'Импорт', exported: 'Экспортировано', imported: 'Импортировано',
    pinned: 'Закреплено', nothing: 'Ничего не найдено',
    copy: 'Копировать', messageActions: 'Сообщение', editing: 'Редактирование сообщения',
    accounts: 'Аккаунты DeepSeek', addAccount: 'Добавить аккаунт', 
    accountsAll: 'Все аккаунты', accActive: 'активный', accExpired: 'токен не действует',
    accExpiredHint: 'Нужно войти снова', accEmpty: 'Пока нет ни одного аккаунта',
    accHint: 'Токены хранятся зашифрованными (Keystore) и никуда не отправляются. Переключение аккаунта меняет, от чьего имени идут запросы.',
    accDeleteAsk: 'Удалить аккаунт с этого телефона?', accSwitched: 'Аккаунт переключён',
    accOk: 'Токен действует', checking: 'Проверяю…',
    lockTitle: 'Блокировка паролем', lockHint: 'Это отдельный пароль FunaChat. На телефоне хранится только PBKDF2-хеш, пароль никуда не отправляется. Он защищает приложение, а не аккаунт DeepSeek.',
    lockEnable: 'Включить блокировку', lockShort: 'Пароль — минимум 6 символов',
    lockForget: 'Забыл пароль — войти в DeepSeek', lockForgetHint: 'Войди в DeepSeek заново — локальная блокировка сбросится, и можно будет задать новый пароль.',
    lockOn: 'Блокировка включена', lockOff: 'Блокировка выключена',
    lockReset: 'Локальная блокировка сброшена — задай новый пароль в Настройках.',
    authNoProfileHint: 'Войди в DeepSeek, чтобы начать',
    active: 'активный', 
    folders: 'Папки', allChats: 'Все', unfiled: 'Без папки', newFolder: 'Новая папка',
    folderName: 'Название папки', moveToFolder: 'Переместить в папку', folder: 'Папка',
    noFolder: 'Без папки', folderDelete: 'Удалить папку?', interactive: 'Интерактивная страница', buildingPage: 'Страница собирается…', showCode: 'Показать код', hideCode: 'Свернуть код', showFull: 'Показать полностью', showLess: 'Свернуть',
    folderNamePh: 'Название папки', renameFolder: 'Переименовать папку', createFolder: 'Новая папка',
    noFind: 'Нет совпадений', continue: 'Продолжить', regen: 'Перегенерировать',
    regenHint: 'Перегенерировать ответ', exportChat: 'Экспорт в Markdown', chatExported: 'Чат сохранён',
    pages: 'Сохранённые страницы', openPages: 'Открыть список', pagesHint: 'HTML-страницы, созданные ИИ',
    pagesEmpty: 'Пока нет сохранённых страниц', openPage: 'Открыть', deletePage: 'Удалить страницу?',
    delete: 'Удалить', done: 'готово', you: 'Я',
    attach: 'Прикрепить файл', fileUploading: 'отправляю…', fileWait: 'Файл ещё обрабатывается',
    fileError: 'Не удалось', files: 'Файлы', fileLimit: 'Не больше 40 файлов на сообщение',
    expand: 'Развернуть', download: 'Скачать', savedTo: 'Сохранено',
    viewFile: 'Открыть', openWith: 'Открыть с помощью', shareFile: 'Поделиться', saveFile: 'Сохранить копию',
    loadingFile: 'Загрузка…', fileMissing: 'Файл не найден', pdfMore: 'Показать ещё страницы',
    pdfLimit: 'Показаны первые {n} страниц',
    compactCtx: 'Сжать контекст', compacting: 'Сжимаю контекст…', compacted: 'Контекст сжат',
    compactAuto: 'Контекст почти заполнен — сжимаю…', compactError: 'Не удалось сжать контекст',
    queueTitle: 'Очередь', queueHint: 'уйдёт после ответа', queueAdded: 'Добавлено в очередь',
    queueEdit: 'Сообщение в очереди', queueUp: 'Выше', queueDown: 'Ниже',
    queueRemove: 'Убрать из очереди', queuePaused: 'Очередь на паузе',
    queueResume: 'Запустить очередь', queueRestored: 'Не отправлено — возвращено в очередь',
    queueCtxFull: 'Контекст заполнен: сожми его, затем запусти очередь',
    queueLeave: 'В очереди ещё {n} сообщений. Перейти и очистить очередь?',
    olderHidden: 'Старых сообщений скрыто: {n}',
    pagesCount: 'стр.',
    noPreview: 'Предпросмотр недоступен',
    copied: 'Скопировано', needToken: 'Вставь токен в настройках',
    accessOk: 'Доступ к /sdcard предоставлен', accessNo: 'Доступ к /sdcard не предоставлен',
    delSession: 'Удалить эту сессию?', delCard: 'Удалить карточку?', renamePrompt: 'Новое название сессии',
    stopped: 'остановлено', answering: 'отвечаю…', stopFirst: 'Сначала останови ответ',
    subagent: 'Субагент', subOpen: 'Открыть сессию', subReadonly: 'только чтение',
    subRunning: 'работает…', subDone: 'готово', subStopped: 'остановлен', subFailed: 'ошибка',
    subEmpty: 'Сессия субагента пуста', subFinishedToast: 'Субагент завершил работу',
    subStop: 'Остановить', 
    error: 'Ошибка', cards: 'карточек',
    q1: 'Файлы', q1s: 'Обзор рабочей папки', q1p: 'Прочитай /sdcard/Download/FunaFiles и скажи, что там есть.',
    q2: 'Терминал', q2s: 'Команды на устройстве', q2p: 'Покажи, где выполняются команды (Termux или ReTerminal), версию sh и свободное место на диске.',
    q3: 'План', q3s: 'Пошаговое мышление', q3p: 'Помоги спланировать небольшое Android-приложение по-русски.'
  }
};

let LANG = 'uk';
const t = (k) => (I18N[LANG] && I18N[LANG][k]) || I18N.uk[k] || k;

/* ---------------- state ---------------- */

const S = {
  signedIn: false, activeId: '', base: '', baseAuto: true, thinking: true, search: false, model: 'default',
  ctxLimit: 1000000,
  storage: false, session: null, parent: 0, first: true, msgs: [], sessions: [], animList: false,
  busy: false, cur: null, title: '', pendingUser: null, sheetId: null, lang: 'uk',
  mem: [], memEdit: null, version: '2.4', memCount: 0, q: '', mq: '', sheetPin: false,
  accounts: [], active: 0, folders: [], fmap: {}, folder: 'all',
  apiRunning: false, apiPort: 8787, apiKey: '', apiAuto: true, apiLan: false, apiDel: true,
  apiPublic: true, keys: [], auditMask: '', auditContact: '', auditHook: '', auditLog: [],
  telemetryOn: true, telemetryLast: 0, telemetryPending: 0,
  guardOn: true, guardGap: 3, guardPerMin: 12, guardPerDay: 400, guardCount: 0, guardCool: 0, guardNote: '',
  guardCoolAt: 0,
  foldAll: false,
  auth: {}, authMode: 'ds', authForce: false, fromUnlock: false,
  editing: null, msgTarget: null, artName: '', nameId: null, mode: 'send', contTarget: null,
  find: '', findHits: [], findIdx: -1, pages: [], att: [],
  compact: '', compactBusy: false, pendingSend: null, pendingResume: false, ctxFrom: 0,
  queue: [], queueHold: false, queueEdit: null, queueSeq: 0,
  view: null, viewSeq: 0, fileTarget: null, subs: {}, subView: null,
  regenBackup: null, editBackup: null, pendingMsg: null,
  editTarget: null, regenPending: null, reqBaseOut: 0, reqLive: false,
  follow: true
};

/* Ключ головної сторінки: артефакти в пісочниці не мають доступу до нього. */
const BRIDGE_KEY = (() => {
  try {
    const a = new Uint8Array(24);
    crypto.getRandomValues(a);
    return Array.from(a, b => b.toString(16).padStart(2, '0')).join('');
  } catch (e) {
    return Date.now().toString(36) + Math.random().toString(36).slice(2) + Math.random().toString(36).slice(2);
  }
})();

function api(o) {
  if (!o || typeof o !== 'object') return;
  o._k = BRIDGE_KEY;
  try { FunaBridge.post(JSON.stringify(o)); } catch (e) { toast('bridge offline'); }
}

window.addEventListener('error', (ev) => {
  try {
    api({ cmd: 'log', level: 'error',
      msg: (ev.message || 'error') + ' @ ' + (ev.filename || 'app.js') + ':' + (ev.lineno || 0) });
  } catch (e) {}
});
window.addEventListener('unhandledrejection', (ev) => {
  try {
    const r = ev.reason;
    api({ cmd: 'log', level: 'error',
      msg: 'promise: ' + (r && r.message ? r.message : String(r)) });
  } catch (e) {}
});

function vibe(ms) { try { if (navigator.vibrate) navigator.vibrate(ms); } catch (e) {} }

function toast(msg) {
  const n = $('toast');
  n.textContent = msg;
  n.classList.add('on');
  clearTimeout(n._h);
  n._h = setTimeout(() => n.classList.remove('on'), 2400);
}

/* ---------------- markdown ---------------- */

let MATHQ = [];

function mathHtml(tex, display) {
  try {
    if (window.katex && window.katex.renderToString) {
      return window.katex.renderToString(tex, {
        displayMode: !!display, throwOnError: false, strict: 'ignore', trust: false
      });
    }
  } catch (e) {}
  return '<code class="mathraw">' + esc(tex) + '</code>';
}

function mathPlace(tex, display) {
  MATHQ.push({ tex: String(tex || '').trim(), display: !!display });
  return '\u0003' + (MATHQ.length - 1) + '\u0003';
}

function extractMath(src) {
  src = src.replace(/\$\$([\s\S]+?)\$\$/g, (m, tex) => mathPlace(tex, true));
  src = src.replace(/\\\[([\s\S]+?)\\\]/g, (m, tex) => mathPlace(tex, true));
  src = src.replace(/\\\(([\s\S]+?)\\\)/g, (m, tex) => mathPlace(tex, false));
  src = src.replace(/(?<!\\)\$([^$\n]+?)\$/g, (m, tex) => mathPlace(tex, false));
  return src;
}

/* Пісочниця артефакту не може відкрити посилання сама: без
   allow-same-origin вона не має доступу до моста. Тому клік по http(s)
   пересилаємо батьківській сторінці, а та відкриває браузер. */
const ART_LINK_HOOK = '<script>(function(){document.addEventListener("click",function(e){' +
  'var a=e.target&&e.target.closest?e.target.closest("a[href]"):null;if(!a)return;' +
  'var h=a.getAttribute("href")||"";if(!/^https?:/i.test(h))return;' +
  'e.preventDefault();try{parent.postMessage({funa:"open",url:h},"*");}catch(_){}} ,true);})();' +
  '<' + '/script>';

/* Вставляємо гачок після <!doctype>, інакше скрипт перед ним
   переводить документ у quirks mode і ламає верстку артефакту. */
function withLinkHook(code) {
  const src = String(code == null ? '' : code);
  const m = /^\s*<!doctype[^>]*>/i.exec(src);
  if (m) return src.slice(0, m[0].length) + ART_LINK_HOOK + src.slice(m[0].length);
  return ART_LINK_HOOK + src;
}

/* Сохранённая страница не должна уносить с собой наш служебный скрипт. */
function stripLinkHook(code) {
  return String(code == null ? '' : code).split(ART_LINK_HOOK).join('');
}

function artifactBlock(code) {
  return '<div class="artifact">' +
    '<div class="arthead">' + ic('i-page') + '<span>' + esc(t('interactive')) + '</span>' +
    '<span class="grow"></span>' +
    '<button class="artbtn" data-art-full title="' + esc(t('expand')) + '">' + ic('i-expand') + '</button>' +
    '<button class="artbtn" data-art-dl title="' + esc(t('download')) + '">' + ic('i-download') + '</button>' +
    '</div><div class="artframe"><iframe sandbox="allow-scripts allow-forms allow-modals allow-popups allow-pointer-lock" ' +
    'srcdoc="' + esc(withLinkHook(code)) + '"></iframe></div></div>';
}

/* Поки HTML-сторінка ще тече, не показуємо її код: інакше чат
   перетворюється на простирадло тегів. Після a_end буде справжня картка. */
function artifactPending() {
  return '<div class="artifact pending"><div class="arthead">' + ic('i-page') +
    '<span>' + esc(t('buildingPage')) + '</span><span class="grow"></span>' +
    '<span class="typing"><i></i><i></i><i></i></span></div></div>';
}

/** Чи це повноцінний HTML-документ, а не шматок розмітки чи Markdown. */
function looksLikeHtml(code) {
  const s = String(code == null ? '' : code);
  return looksLikeHtmlStart(s) && /<\/(?:html|svg)\s*>/i.test(s);
}

/** Під час стріму кінця ще немає: достатньо початку документа. */
function looksLikeHtmlStart(code) {
  const s = String(code == null ? '' : code);
  return /^\s*(?:<!--[\s\S]*?-->\s*)*(?:<!doctype\s+html|<html[\s>]|<svg[\s>])/i.test(s);
}

function fmtCodeSize(n) {
  n = Math.max(0, n || 0);
  return n >= 1024 ? (n < 10240 ? (n / 1024).toFixed(1) : Math.round(n / 1024)) + ' KB' : n + ' B';
}

/** Підсвітка дорога на мегабайтах: великий код показуємо як є. */
function codeHtml(code) {
  return String(code).length > 50000 ? esc(code) : hl(code);
}

function userText(txt) {
  MATHQ = [];
  let src = esc(String(txt == null ? '' : txt));
  src = extractMath(src);
  return src.replace(/\u0003(\d+)\u0003/g, (_, i) => {
    const q = MATHQ[+i] || { tex: '', display: false };
    return mathHtml(q.tex, q.display);
  });
}

function inlineMd(s) {
  const codes = [];
  s = s.replace(/`([^`\n]+)`/g, (_, c) => { codes.push(c); return '\u0001' + (codes.length - 1) + '\u0001'; });
  s = s.replace(/!\[([^\]]*)\]\(([^)\s]+)\)/g, (m, alt, url) => imgHtml(url, alt));
  s = s.replace(/\[([^\]]+)\]\(([^)\s]+)\)/g, (m, label, url) => {
    if (/^https?:/i.test(url)) return '<a href="' + url + '" target="_blank" rel="noreferrer">' + label + '</a>';
    if (/^(\/\/:file:|file:\/\/)/i.test(url)) return '<a class="flink" data-fpath="' + esc(normPath(url)) + '">' + label + '</a>';
    return m;
  });
  s = s.replace(/(^|[\s(])((?:\/\/:file:|file:\/\/)[^\s<)]+)/g, (m, pre, url) => {
    const path = normPath(url);
    return pre + '<a class="flink" data-fpath="' + esc(path) + '">' + esc(fileBase(path)) + '</a>';
  });
  s = s.replace(/(^|[\s(])(https?:\/\/[^\s<)]+)/g, '$1<a href="$2" target="_blank" rel="noreferrer">$2</a>');
  s = s.replace(/\*\*([^*]+)\*\*/g, '<b>$1</b>');
  s = s.replace(/(^|[^*])\*([^*\n]+)\*/g, '$1<i>$2</i>');
  s = s.replace(/~~([^~]+)~~/g, '<s>$1</s>');
  s = s.replace(/==([^=]+)==/g, '<mark>$1</mark>');
  s = s.replace(/\u0001(\d+)\u0001/g, (_, i) => '<code class="inl">' + codes[+i] + '</code>');
  s = s.replace(/\u0003(\d+)\u0003/g, (_, i) => {
    const q = MATHQ[+i] || { tex: '', display: false };
    return mathHtml(q.tex, q.display);
  });
  return s;
}

function splitRow(line) {
  return line.trim().replace(/^\|/, '').replace(/\|$/, '').split('|').map(x => x.trim());
}
function isTableSep(line) {
  return line.indexOf('-') >= 0 && /^\s*\|?[\s:|-]+\|[\s:|-]*$/.test(line);
}

function md(src, streaming) {
  if (!src) return '';
  MATHQ = [];
  const blocks = [];
  const attaches = [];
  const htmls = [];
  // HTML-документ у ```html може містити всередині власні ```-приклади.
  // Тому таку огорожу закриваємо по </html>, а не по першому ```.
  src = String(src).replace(
    /(^|\n)`{3,}(?:html|htm|funa-html)[ \t]*\r?\n([\s\S]*?<\/(?:html|svg)\s*>)([\s\S]*?)(?:\r?\n`{3,}[ \t]*(?=\n|$)|$)/gi,
    (m, pre, code, tail) => {
      htmls.push(code);
      return pre + '\u0004' + (htmls.length - 1) + '\u0004' + '\n' + (tail || '');
    });
  src = String(src).replace(/```([^\n`]*)\n?([\s\S]*?)(?:\n```[ \t]*(?=\n|$)|$)/g, (_, lang, code) => {
    blocks.push({ lang: (lang || '').trim(), code: code.replace(/\n$/, '') });
    return '\u0000' + (blocks.length - 1) + '\u0000';
  });
  // Модель іноді віддає HTML без ```-огорожі. Такий документ теж має
  // стати карткою, а не текстом із тегами.
  src = src.replace(/<!doctype\s+html[\s\S]*?<\/html\s*>/gi, (m) => {
    if (m.length < 400) return m;
    htmls.push(m);
    return '\n\n\u0004' + (htmls.length - 1) + '\u0004\n\n';
  });
  src = src.replace(/<html[\s>][\s\S]*?<\/html\s*>/gi, (m) => {
    if (m.length < 400) return m;
    htmls.push(m);
    return '\n\n\u0004' + (htmls.length - 1) + '\u0004\n\n';
  });
  // Під час стріму закритого </html> ще немає. Якщо документ почався
  // з doctype чи <html>, ховаємо його теги за карткою очікування.
  if (streaming) {
    const open = src.match(/(^|\n)[ \t]*(?:<!doctype\s+html[^>]*>|<html[\s>][^>]*>)/i);
    if (open && !/<\/(?:html|svg)\s*>/i.test(src.slice(open.index))) {
      src = src.slice(0, open.index) + '\n\n\u0004' + htmls.length + '\u0004\n\n';
      htmls.push('');
    }
  }
  src = extractMath(src);
  const lines = esc(src).split('\n');
  const out = [];
  let para = [], list = null, quote = [];

  const flushPara = () => { if (para.length) { out.push('<p>' + inlineMd(para.join('\n')).replace(/\n/g, '<br>') + '</p>'); para = []; } };
  const flushList = () => {
    if (!list) return;
    out.push('<' + list.tag + '>' + list.items.map(x => {
      const tk = x.match(/^\[([ xX])\]\s+([\s\S]*)$/);
      const on = tk && tk[1].toLowerCase() === 'x';
      const body = tk ? tk[2] : x;
      return '<li>' + (tk ? '<span class="tick' + (on ? ' on' : '') + '">' + (on ? '✓' : '○') + '</span> ' : '') + inlineMd(body) + '</li>';
    }).join('') + '</' + list.tag + '>');
    list = null;
  };
  const flushQuote = () => { if (quote.length) { out.push('<blockquote>' + inlineMd(quote.join('\n')).replace(/\n/g, '<br>') + '</blockquote>'); quote = []; } };
  const flushAll = () => { flushPara(); flushList(); flushQuote(); };

  for (let li = 0; li < lines.length; li++) {
    const raw = lines[li];
    const line = raw.replace(/\s+$/, '');
    let m;

    if (/^\u0000\d+\u0000$/.test(line.trim())) {
      flushAll();
      const b = blocks[+line.trim().replace(/\u0000/g, '')] || { lang: '', code: '' };
      if (/^attach$/i.test(b.lang)) {
        for (const ln of b.code.split('\n')) {
          const p = ln.trim().replace(/^[-*•]\s*/, '').replace(/^[`"']|[`"']$/g, '').trim();
          if (p && !/^attach$/i.test(p)) attaches.push(p);
        }
        continue;
      }
      const langHtml = /^(html|htm|funa-html)$/i.test(b.lang);
      // Явно позначений html-блок лишаємо карткою, навіть якщо це фрагмент
      // без <html>; без позначки — лише повний документ.
      const isArtifact = (langHtml &&
          (streaming ? looksLikeHtmlStart(b.code) : looksLikeHtml(b.code)) ||
          (langHtml && /<(!doctype|html|body|style|script|svg)/i.test(b.code))) ||
        (b.lang === '' && (streaming ? looksLikeHtmlStart(b.code) : looksLikeHtml(b.code)));
      if (isArtifact) {
        out.push(streaming ? artifactPending() : artifactBlock(b.code));
      } else if (b.code.length > 4000) {
        // Довгий код не розгортаємо самі: у чаті він займає кілька екранів.
        out.push('<div class="code folded"><div class="codehead"><span>' +
          esc((b.lang || 'text') + ' · ' + fmtCodeSize(b.code.length)) + '</span>' +
          '<span class="grow"></span>' +
          '<button data-copy="1">' + esc(t('copy')) + '</button>' +
          '<button data-fold="1">' + esc(t('showCode')) + '</button></div>' +
          '<pre hidden><code>' + codeHtml(b.code) + '</code></pre></div>');
      } else {
        out.push('<div class="code"><div class="codehead"><span>' + esc(b.lang || 'text') + '</span>' +
          '<button data-copy="1">' + esc(t('copy')) + '</button></div><pre><code>' + codeHtml(b.code) + '</code></pre></div>');
      }
      continue;
    }

    if (/^\u0004\d+\u0004$/.test(line.trim())) {
      flushAll();
      const code = htmls[+line.trim().replace(/\u0004/g, '')] || '';
      if (streaming) out.push(artifactPending());
      else if (code) out.push(artifactBlock(code));
      continue;
    }

    if (/^\u0003\d+\u0003$/.test(line.trim())) {
      flushAll();
      const q = MATHQ[+line.trim().replace(/\u0003/g, '')] || { tex: '', display: true };
      out.push('<div class="mathblock">' + mathHtml(q.tex, true) + '</div>');
      continue;
    }

    if (!line.trim()) { flushAll(); continue; }

    if (line.includes('|') && li + 1 < lines.length && isTableSep(lines[li + 1])) {
      flushAll();
      const head = splitRow(line);
      const rows = [];
      li += 2;
      while (li < lines.length && lines[li].includes('|') && lines[li].trim()) {
        rows.push(splitRow(lines[li]));
        li++;
      }
      li--;
      out.push('<div class="tablewrap"><table><thead><tr>' +
        head.map(c => '<th>' + inlineMd(c) + '</th>').join('') + '</tr></thead><tbody>' +
        rows.map(r => '<tr>' + r.map(c => '<td>' + inlineMd(c) + '</td>').join('') + '</tr>').join('') +
        '</tbody></table></div>');
      continue;
    }

    if ((m = line.match(/^(#{1,6})\s+(.*)$/))) {
      flushAll(); const n = Math.min(6, m[1].length);
      out.push('<h' + n + '>' + inlineMd(m[2]) + '</h' + n + '>'); continue;
    }
    if (/^(-{3,}|\*{3,}|_{3,})$/.test(line.trim())) { flushAll(); out.push('<hr>'); continue; }
    if ((m = line.match(/^\s*[-*+]\s+(.*)$/))) {
      flushPara(); flushQuote();
      if (!list || list.tag !== 'ul') { flushList(); list = { tag: 'ul', items: [] }; }
      list.items.push(m[1]); continue;
    }
    if ((m = line.match(/^\s*\d+[.)]\s+(.*)$/))) {
      flushPara(); flushQuote();
      if (!list || list.tag !== 'ol') { flushList(); list = { tag: 'ol', items: [] }; }
      list.items.push(m[1]); continue;
    }
    if ((m = line.match(/^>\s?(.*)$/))) { flushPara(); flushList(); quote.push(m[1]); continue; }
    flushList(); flushQuote();
    para.push(line);
  }
  flushAll();
  if (attaches.length) out.push(attachCards(attaches));
  return out.join('');
}

/* ---------------- syntax highlight ---------------- */

const HL = /(&quot;[^&\n]*?&quot;|&#39;[^&\n]*?&#39;|`[^`\n]*`)|(\/\/[^\n]*|#[^\n]*|--[^\n]*)|\b(\d+(?:\.\d+)?)\b|\b(?:fun|val|var|if|else|when|for|while|return|class|object|interface|import|package|public|private|protected|static|final|void|new|try|catch|finally|throw|throws|const|let|function|async|await|def|elif|from|as|with|lambda|True|False|None|nil|end|do|then|fi|esac|echo|sudo|pip|git|rm|cp|mv|mkdir|chmod|export|source|select|where|insert|update|delete|create|table)\b/g;

function hl(code) {
  return esc(code).replace(HL, (m, str, com, num) => {
    if (str) return '<span class="hl-s">' + str + '</span>';
    if (com) return '<span class="hl-c">' + com + '</span>';
    if (num) return '<span class="hl-n">' + num + '</span>';
    return '<span class="hl-k">' + m + '</span>';
  });
}

/* ---------------- messages ---------------- */

const TOOL_ICON = { read: 'i-folder', list: 'i-folder', ls: 'i-folder', write: 'i-edit', append: 'i-edit',
  edit: 'i-edit', sh: 'i-term', bash: 'i-term', termux: 'i-term', doc: 'i-page', document: 'i-page' };
const toolIcon = (name) => TOOL_ICON[String(name || '').toLowerCase()] || 'i-term';

function toolHint(m) {
  const txt = ((m.output || '') + ' ' + (m.args || '') + ' ' + (m.name || '')).toLowerCase();
  const failed = m.status === 'error' || /не відповідає|немає доступу|not responding|permission denied|eacces|timed out|timeout/.test(txt);
  if (!failed) return null;
  if (/termux|funaqueue|міст|bridge/.test(txt)) return { t: 't1t', b: 't1b' };
  if (/eacces|permission|доступ|access|sdcard|storage/.test(txt)) return { t: 't2t', b: 't2b' };
  if (/401|token|токен/.test(txt)) return { t: 't3t', b: 't3b' };
  return null;
}

function toolMsg(m) {
  const st = m.status === 'run' ? t('answering') : (m.status === 'error' ? t('error') : t('done'));
  const hasOut = !!(m.output && m.output.length);
  const hint = toolHint(m);
  if (m.open === undefined) m.open = !S.foldAll && (m.status === 'run' || !hasOut || (m.output || '').length < 700);
  const node = el('div', 'msg');
  node._m = m;
  node.innerHTML =
    '<div class="av">' + ic(toolIcon(m.name)) + '</div>' +
    '<div class="body"><div class="name">' + esc(m.name || 'tool') + '</div>' +
    '<div class="toolcard' + (m.open ? ' open' : '') + '">' +
      '<button class="toolhead ' + esc(m.status || '') + '"><span class="st"></span>' +
      '<b>' + esc(m.name || '') + '</b><span class="grow"></span>' +
      '<span class="tstat">' + st + '</span>' + ic('i-chevron', 'chev') + '</button>' +
      '<div class="toolwrap"><div class="toolinner">' +
        (m.args ? '<div class="toolargs">' + esc(m.args) + '</div>' : '') +
        (hasOut ? '<div class="toolbody"><pre>' + esc(m.output) + '</pre></div>' : '') +
      '</div></div>' +
    '</div>' +
    (hint ? '<div class="toolhint"><b>' + esc(t(hint.t)) + '</b><p>' + esc(t(hint.b)) + '</p>' +
      '<button data-help="1">' + esc(t('openHelp')) + '</button></div>' : '') +
    '</div>';
  return node;
}

/* ---------------- subagent cards ---------------- */

function fmtMs(ms) {
  ms = Math.max(0, ms || 0);
  if (ms < 1000) return ms + ' ms';
  const s = ms / 1000;
  if (s < 60) return s.toFixed(1) + ' s';
  if (s < 3600) return Math.floor(s / 60) + 'm ' + Math.round(s % 60) + 's';
  const h = Math.floor(s / 3600);
  return h + 'h ' + Math.round((s % 3600) / 60) + 'm';
}

/* ---------------- дата повідомлень, версії, статистика ---------------- */

function pad2(n) { return (n < 10 ? '0' : '') + n; }

/** Компактний час: сьогодні — лише HH:MM, інакше дата + час. */
function fmtClock(ts) {
  ts = +ts || 0;
  if (!ts) return '';
  const d = new Date(ts), now = new Date();
  const hm = pad2(d.getHours()) + ':' + pad2(d.getMinutes());
  if (d.toDateString() === now.toDateString()) return hm;
  const dm = pad2(d.getDate()) + '.' + pad2(d.getMonth() + 1);
  if (d.getFullYear() !== now.getFullYear()) return dm + '.' + d.getFullYear() + ' ' + hm;
  return dm + ' ' + hm;
}

function fmtFull(ts) {
  try {
    return new Date(ts).toLocaleString(
      LANG === 'en' ? 'en-GB' : LANG === 'ru' ? 'ru-RU' : 'uk-UA',
      { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit' });
  } catch (e) { return fmtClock(ts); }
}

function verCount(m) { return (m && m.vers && m.vers.length) || 1; }

/** Перемикач ‹ N/M › для редагованого повідомлення чи регенерації. */
function verNav(m) {
  const n = verCount(m);
  if (n < 2 || S.busy || S.compactBusy) return '';
  const pos = Math.max(1, Math.min(n, (m.vi || 0) + 1));
  return '<span class="vernav" role="group" aria-label="versions">' +
    '<button type="button" class="vprev" data-vprev="1" aria-label="' + esc(t('versionPrev')) +
      '" title="' + esc(t('versionPrev')) + '"' + (pos <= 1 ? ' disabled' : '') + '>' +
      ic('i-chevron') + '</button>' +
    '<span class="vnum">' + pos + '/' + n + '</span>' +
    '<button type="button" class="vnext" data-vnext="1" aria-label="' + esc(t('versionNext')) +
      '" title="' + esc(t('versionNext')) + '"' + (pos >= n ? ' disabled' : '') + '>' +
      ic('i-chevron') + '</button></span>';
}

function msgMeta(m) {
  const bits = [];
  if (m.ts) bits.push('<span class="mtime" title="' + esc(fmtFull(m.ts)) + '">' + esc(fmtClock(m.ts)) + '</span>');
  if (m.role === 'assistant' && m.ms) {
    bits.push('<span class="mdur" title="' + esc(fmtMs(m.ms)) + '">' + esc(t('aiWork')) + ' ' + esc(fmtMs(m.ms)) + '</span>');
  }
  const v = verNav(m);
  if (!bits.length && !v) return '';
  return '<div class="mmeta">' + bits.join('<span class="mdot">·</span>') +
    (v ? '<span class="grow"></span>' + v : '') + '</div>';
}

function paintMeta(m) {
  if (!m || !m._foot) return;
  m._foot.innerHTML = msgMeta(m);
}

/* Снапшот версії: усе, що треба відновити, плюс хвіст переписки під нею. */
function verSnapshot(m, tail) {
  return {
    role: m.role, text: m.text || '', think: m.think || '',
    id: m.id || 0, status: m.status || 'FINISHED',
    ts: m.ts || 0, ms: m.ms || 0,
    files: (m.files || []).map(f => Object.assign({}, f)),
    tail: tail || []
  };
}

/** Синхронізує live-повідомлення у його поточний снапшот версії. */
function verSync(m, withTail) {
  if (!m || !m.vers || !m.vers.length) return;
  const v = m.vers[m.vi];
  if (!v) return;
  v.text = m.text || '';
  v.think = m.think || '';
  v.id = m.id || 0;
  v.status = m.status || 'FINISHED';
  v.ts = m.ts || 0;
  v.ms = m.ms || 0;
  v.files = (m.files || []).map(f => Object.assign({}, f));
  v.role = m.role;
  if (withTail) v.tail = captureTail(m);
}

function captureTail(m) {
  const i = S.msgs.indexOf(m);
  if (i < 0) return [];
  return S.msgs.slice(i + 1);
}

function resetUiMsg(x) {
  if (!x) return x;
  x._el = null; x._md = null; x._foot = null;
  x._raf = 0; x._tmr = 0;
  x.streaming = false;
  return x;
}

/**
 * Повідомлення до останнього стиснення належать старій серверній сесії:
 * їхні id не можна передавати як parent у новій сесії. Перемикання таких
 * версій лише плутало б наступну відправку, тому не даємо цього робити.
 */
function archivedMsg(m) {
  const i = S.msgs.indexOf(m);
  return i >= 0 && !!S.compact && (S.ctxFrom || 0) > 0 && i < S.ctxFrom;
}

/**
 * Перемикає редаговане повідомлення / регенерацію на старішу чи новішу
 * версію. Хвіст, що йшов під поточною версією, зберігається у снапшоті,
 * а хвіст цільової версії повертається в чат — як в OpenCode.
 */
function switchVersion(m, dir) {
  if (!m || S.busy || S.compactBusy || !m.vers || m.vers.length < 2) return;
  const i = S.msgs.indexOf(m);
  if (i < 0) return;
  if (archivedMsg(m)) { toast(t('versionArchived')); return; }
  const target = (m.vi || 0) + dir;
  if (target < 0 || target >= m.vers.length) return;
  verSync(m, true);
  const v = m.vers[target];
  if (!v) return;
  m.text = v.text || '';
  m.think = v.think || '';
  m.id = v.id || 0;
  m.status = v.status || 'FINISHED';
  m.files = (v.files || []).map(f => Object.assign({}, f));
  m.ts = v.ts || 0;
  m.ms = v.ms || 0;
  m.streaming = false;
  m.thinkOpen = false;
  m.vi = target;
  resetUiMsg(m);
  rememberTs(m);
  const tsmap = msgTsLoad();
  const tail = [];
  for (const x of (v.tail || [])) {
    if (!x || x.role === 'divider') continue;
    const it = (x.role === 'tool' || x.role === 'sub') ? x : histMsg(x, tsmap);
    if (it) {
      tail.push(resetUiMsg(it));
      if (it.id) msgTsPut(tsmap, it.id, it.ts, it.ms);
      if (it.vers) for (const vv of it.vers) msgTsPut(tsmap, vv.id, vv.ts, vv.ms);
    }
  }
  msgTsSave(tsmap);
  S.msgs = S.msgs.slice(0, i + 1).concat(tail);
  S.parent = lastMsgId();
  // Нативна сторона теж має знати батьківське повідомлення нової гілки,
  // інакше наступна відправка піде в стару гілку.
  api({ cmd: 'branch', parent: S.parent });
  S.pendingUser = null;
  S.cur = null;
  renderAll();
  updateCtxChip(true);
  try {
    const el = m._el;
    if (el && el.scrollIntoView) el.scrollIntoView({ block: 'center' });
  } catch (e) {}
  if (S.find) runFind(S.find);
  vibe(8);
}

/* Статистика ШІ зберігається окремо для кожного акаунта DeepSeek. */
const STATS_DEF = { q: 0, done: 0, stop: 0, ms: 0, tin: 0, tout: 0,
  edit: 0, regen: 0, tool: 0, err: 0, last: 0 };

function statsKey() { return 'funa.stats.' + (S.activeId || 'anon'); }

function statsGet() {
  let v = null;
  try { v = JSON.parse(localStorage.getItem(statsKey()) || '{}'); } catch (e) {}
  return Object.assign({}, STATS_DEF, v || {});
}

function statsSet(st) {
  try { localStorage.setItem(statsKey(), JSON.stringify(st)); } catch (e) {}
}

function statsAdd(patch) {
  if (!patch) return;
  const st = statsGet();
  for (const k in patch) st[k] = (+st[k] || 0) + (+patch[k] || 0);
  if (patch.q) st.last = Date.now();
  statsSet(st);
  renderStats();
}

function statsReset() {
  try { localStorage.removeItem(statsKey()); } catch (e) {}
  renderStats();
}

/* Час повідомлень. Сервер віддає insert_at не для кожної репліки (а старі
   сесії — часто взагалі без нього), тому кешуємо ts/ms локально за id
   повідомлення. Ключ — акаунт: id унікальні в його межах. */
const MSG_TS_MAX = 2000;
function msgTsKey() { return 'funa.ts.' + (S.activeId || 'anon'); }
function msgTsLoad() {
  try {
    const v = JSON.parse(localStorage.getItem(msgTsKey()) || '{}');
    return v && typeof v === 'object' ? v : {};
  } catch (e) { return {}; }
}
function msgTsSave(map) {
  const keys = Object.keys(map);
  if (keys.length > MSG_TS_MAX) {
    keys.sort((a, b) => (+a || 0) - (+b || 0));
    for (let i = 0; i < keys.length - MSG_TS_MAX; i++) delete map[keys[i]];
  }
  try { localStorage.setItem(msgTsKey(), JSON.stringify(map)); } catch (e) {}
}
function msgTsPut(map, id, ts, ms) {
  if (!map || !id || (!ts && !ms)) return;
  const old = map[id] || {};
  map[id] = { ts: ts || old.ts || 0, ms: ms || old.ms || 0 };
}
function rememberTs(m) {
  if (!m || !m.id) return;
  const map = msgTsLoad();
  msgTsPut(map, m.id, m.ts, m.ms);
  msgTsSave(map);
}

function renderStats() {
  const box = $('statGrid');
  if (!box) return;
  const st = statsGet();
  const total = (st.done || 0) + (st.stop || 0);
  const rows = [
    [t('statReq'), fmtNum(st.q)],
    [t('statDone'), fmtNum(st.done)],
    [t('statStop'), fmtNum(st.stop)],
    [t('statTime'), fmtMs(st.ms)],
    [t('statAvgTime'), total ? fmtMs(st.ms / total) : '—'],
    [t('statTokIn'), fmtTok(st.tin)],
    [t('statTokOut'), fmtTok(st.tout)],
    [t('statAvgTok'), st.done ? fmtTok(st.tout / st.done) : '—'],
    [t('statEdit'), fmtNum(st.edit)],
    [t('statRegen'), fmtNum(st.regen)],
    [t('statTool'), fmtNum(st.tool)],
    [t('statErr'), fmtNum(st.err)],
    [t('statSpeed'), st.ms > 0 ? (st.tout / (st.ms / 1000)).toFixed(1) + ' tok/s' : '—']
  ];
  box.innerHTML = rows.map(r => '<div class="statrow"><span>' + esc(r[0]) +
    '</span><b>' + esc(r[1]) + '</b></div>').join('');
  const last = $('statLast');
  if (last) last.textContent = st.last ? t('statLast') + ': ' + fmtClock(st.last) : '';
}

/** Оцінка токенів однієї відповіді (для статистики). */
function estMsgTokens(m) {
  if (!m) return 0;
  return Math.ceil(((m.text || '').length + (m.think || '').length) / 3.2);
}

/** Сумарний обсяг усіх відповідей: різниця до/після запиту дає вихід. */
function totalOutTokens() {
  let n = 0;
  for (const m of S.msgs) if (m.role === 'assistant') n += estMsgTokens(m);
  return n;
}

function subState(st) {
  if (st === 'run') return { cls: 'run', label: t('subRunning') };
  if (st === 'error') return { cls: 'error', label: t('subFailed') };
  if (st === 'stopped') return { cls: 'error', label: t('subStopped') };
  return { cls: 'done', label: t('subDone') };
}

function subMsg(m) {
  const s = S.subs[m.id] || { id: m.id, status: 'run', title: m.id, task: '', live: '', t0: Date.now() };
  const st = subState(s.status);
  const ms = s.status === 'run' ? Date.now() - (s.t0 || Date.now()) : (s.ms || 0);
  const preview = s.status === 'run' ? (s.live || '') : (s.result || s.error || '');
  const node = el('div', 'msg');
  node._m = m;
  node.innerHTML =
    '<div class="av">' + ic('i-plan') + '</div>' +
    '<div class="body"><div class="name">' + esc(t('subagent')) + ' · ' + esc(s.id || m.id) + '</div>' +
      '<div class="toolcard subcard ' + st.cls + '">' +
        '<div class="toolhead ' + st.cls + '"><span class="st"></span>' +
          '<b>' + esc(s.title || m.id) + '</b><span class="grow"></span>' +
          '<span class="tstat">' + esc(st.label) + '</span>' + ic('i-open') + '</div>' +
        (!S.foldAll && s.task ? '<div class="subtask">' + esc(s.task) + '</div>' : '') +
        (!S.foldAll && preview ? '<div class="subprev">' + esc(preview) + '</div>' : '') +
        '<div class="subfoot">' + ic('i-open') + '<span>' + esc(t('subOpen')) + '</span>' +
          '<span class="grow"></span><span>' + esc(ms ? fmtMs(ms) : '') + '</span></div>' +
      '</div></div>';
  return node;
}

/** Прибирає картки субагентів, які належать іншій сесії. */
function pruneSubs() {
  if (!S.session) return;
  let changed = false;
  S.msgs = S.msgs.filter((m) => {
    if (m.role !== 'sub') return true;
    const sb = S.subs[m.id];
    if (sb && sb.sid && sb.sid !== S.session) {
      if (m._el && m._el.parentNode) m._el.parentNode.removeChild(m._el);
      delete S.subs[m.id];
      changed = true;
      return false;
    }
    return true;
  });
  if (changed) { updateWelcome(); updateCtxChip(); }
}

function repaintSub(id) {
  for (const m of S.msgs) {
    if (m.role === 'sub' && m.id === id && m._el) {
      const n = msgNode(m);
      m._el.replaceWith(n);
      m._el = n;
      maybeScroll();
      return;
    }
  }
}

function openSub(id) {
  S.subView = S.subs[id] || { id: id, title: id, status: 'run', ms: 0, messages: [] };
  $('subTitle').textContent = S.subView.title || id;
  $('subMeta').textContent = t('subReadonly');
  $('subBody').innerHTML = '';
  $('subSheet').classList.add('on');
  api({ cmd: 'sub_view', id: id });
}

function closeSub() {
  $('subSheet').classList.remove('on');
  S.subView = null;
}

function renderSub(o) {
  S.subView = o;
  const st = subState(o.status);
  $('subTitle').textContent = o.title || o.id;
  $('subMeta').textContent = t('subReadonly') + ' · ' + st.label + (o.ms ? ' · ' + fmtMs(o.ms) : '');
  $('subStop').style.display = o.status === 'run' ? '' : 'none';
  const b = $('subBody');
  b.innerHTML = '';
  const msgs = o.messages || [];
  if (!msgs.length) {
    b.innerHTML = '<div class="subempty">' + esc(t('subEmpty')) + '</div>';
  }
  for (const m of msgs) {
    if (m.role === 'user') {
      b.insertAdjacentHTML('beforeend', '<div class="submsg user"><div class="subrole">' + esc(t('you')) +
        '</div><div class="md">' + md(m.text || '', false) + '</div></div>');
    } else if (m.role === 'assistant') {
      const n = el('div', 'submsg');
      n.innerHTML = '<div class="subrole">' + esc(t('subagent')) + '</div>' +
        '<div class="md">' + md(m.text || '', false) + '</div>' +
        (m.think ? '<div class="subthink">' + esc(m.think) + '</div>' : '');
      b.appendChild(n);
    } else if (m.role === 'tool') {
      const n = el('div', 'subtool');
      const ok = m.status === 'done' ? 'done' : (m.status === 'error' ? t('subFailed') : t('subRunning'));
      n.innerHTML = '<div class="h">' + ic(toolIcon(m.name)) + '<b>' + esc(m.name || 'tool') + '</b>' +
        '<span class="grow"></span><span>' + esc(ok) + '</span></div>' +
        (m.args ? '<pre>' + esc(m.args) + '</pre>' : '') +
        (m.output ? '<pre>' + esc(m.output) + '</pre>' : '');
      b.appendChild(n);
    }
  }
  if (o.status === 'run') {
    const n = el('div', 'submsg');
    n.innerHTML = '<div class="subrole">' + esc(t('subRunning')) +
      '</div><div class="sublive" id="subLiveText">' + esc(o.live || '') + '</div>';
    b.appendChild(n);
  } else if (o.status !== 'done' && o.error) {
    const n = el('div', 'submsg');
    n.innerHTML = '<div class="subrole">' + esc(o.status === 'stopped' ? t('subStopped') : t('subFailed')) +
      '</div><div class="subthink">' + esc(o.error) + '</div>';
    b.appendChild(n);
  }
  b.scrollTop = b.scrollHeight;
}

function msgNode(m) {
  if (m.role === 'tool') return toolMsg(m);
  if (m.role === 'sub') return subMsg(m);
  if (m.role === 'divider') {
    const node = el('div', 'ctxdivider');
    node.innerHTML = '<span>' + ic('i-cards') + '</span><b>' + esc(m.text || t('compacted')) + '</b>';
    return node;
  }
  if (m.role === 'user') {
    const node = el('div', 'msg user');
    node._m = m;
    node.innerHTML = '<div class="uwrap"><div class="bubble">' + userText(m.text) + fileTiles(m.files) +
      '</div><div class="mfoot"></div></div>';
    m._foot = node.querySelector('.mfoot');
    paintMeta(m);
    return node;
  }
  const node = el('div', 'msg');
  node._m = m;
  node.innerHTML = '<div class="av">' + ic('i-spark') + '</div><div class="body">' +
    '<div class="name">DeepSeek<span class="sp"></span>' +
    '<button class="mact" data-mcopy="1" title="' + esc(t('copy')) + '">' + ic('i-copy') + '</button>' +
    '<button class="mact" data-mregen="1" title="' + esc(t('regenHint')) + '">' + ic('i-regen') + '</button>' +
    '<button class="mact" data-mmenu="1" title="' + esc(t('messageActions')) + '">' + ic('i-more') + '</button></div>' +
    '<div class="md"></div><div class="mfoot"></div></div>';
  m._md = node.querySelector('.md');
  m._foot = node.querySelector('.mfoot');
  paintAssistant(m);
  paintMeta(m);
  return node;
}

/* Пока ответ течёт, гигантский текст не парсим целиком каждые 400 мс:
   показываем начало и конец, полный рендер — после a_end. */
const STREAM_PREVIEW = 180000;
function streamPreview(s) {
  if (!s || s.length <= STREAM_PREVIEW) return s;
  const half = STREAM_PREVIEW >> 1;
  return s.slice(0, half) + '\n\n… (відповідь триває) …\n\n' + s.slice(-half);
}

function paintAssistant(m) {
  if (!m._md) return;
  let head = '';
  if (m.think) {
    if (m.thinkOpen === undefined) m.thinkOpen = !S.foldAll && !!m.streaming;
    const len = m.think.length;
    const shown = m.streaming ? streamPreview(m.think) : m.think;
    head += '<div class="think' + (m.thinkOpen ? ' open' : '') + '">' +
      '<button class="thinkhead" data-think>' + ic('i-spark') + '<span>' + esc(t('thinking')) + '</span>' +
      '<span class="sp"></span><span class="cnt">' + (len > 999 ? (len / 1000).toFixed(1) + 'k' : len) + '</span>' +
      ic('i-chevron', 'chev') + '</button>' +
      '<div class="thinkwrap"><div class="thinkinner"><div class="txt">' + esc(shown) + '</div></div></div></div>';
  }
  let body = md(m.streaming ? streamPreview(m.text) : m.text, m.streaming);
  if (m.streaming && !m.text && !m.think) body = '<div class="typing"><i></i><i></i><i></i></div>';
  if (m.streaming && m.text) body += '<span class="caret"></span>';
  if (m.status === 'STOPPED') body += '<div style="font-size:11.5px;color:var(--err);margin-top:8px">' + esc(t('stopped')) + '</div>';
  if (!m.streaming && m.text && m.status !== 'FINISHED' && m.id) {
    body += '<button class="contbtn" data-mcont="1">' + ic('i-regen') + '<span>' + esc(t('continue')) + '</span></button>';
  }
  // Дуже довга відповідь (простирадло тестів, таблиць, логу) не має
  // розсувати чат на десятки екранів: показуємо початок і кнопку.
  if (!m.streaming && body.length > 24000) {
    const open = !!m.expanded;
    body = '<div class="longmsg' + (open ? '' : ' folded') + '">' + body + '</div>' +
      '<button class="longbtn" data-longmsg="1">' +
      esc(t(open ? 'showLess' : 'showFull')) + '</button>';
  }
  m._md.innerHTML = head + body;
  hydrateNode(m._md);
  updateCtxChip();
}

/* Стеля тексту одного сообщения в UI: даже если апстрим выдаёт
   бесконечный поток, WebView не должен уйти в OOM. */
const UI_TEXT_CAP = 2000000;
function addDelta(m, key, v) {
  if (!m || !v) return;
  const cur = m[key] || '';
  if (cur.length >= UI_TEXT_CAP) return;
  const room = UI_TEXT_CAP - cur.length;
  m[key] = cur + (v.length <= room ? v : v.slice(0, room));
  schedulePaint(m);
}

function schedulePaint(m) {
  if (!m) return;
  if (m._raf || m._tmr) return;
  // Markdown длинного ответа пересобирается целиком: на каждом кадре это
  // O(n²) по тексту. Во время стрима достаточно ~10 обновлений в секунду,
  // а на очень длинном тексте — ещё реже, иначе WebView греется.
  const now = Date.now();
  const len = (m.text || '').length;
  const gap = len > 60000 ? 400 : len > 20000 ? 200 : 90;
  if (m.streaming && m._painted && now - m._painted < gap) {
    m._tmr = setTimeout(() => { m._tmr = 0; schedulePaint(m); }, gap - (now - m._painted));
    return;
  }
  m._raf = requestAnimationFrame(() => {
    m._raf = 0;
    m._painted = Date.now();
    const sc = $('scroll');
    const keep = S.follow ? null : sc.scrollTop;
    paintAssistant(m);
    if (keep !== null && Math.abs(sc.scrollTop - keep) > 1) sc.scrollTop = keep;
    maybeScroll();
  });
}

/* Автопрокрутка вниз працює лише поки користувач сам не пішов вище.
   Інакше кожен новий шматок відповіді смикав чат униз. */
function maybeScroll(force) {
  const sc = $('scroll');
  if (force) S.follow = true;
  if (!S.follow) return;
  sc.scrollTop = sc.scrollHeight;
}

function renderAll() {
  const box = $('msgs');
  box.innerHTML = '';
  for (const m of S.msgs) { const n = msgNode(m); m._el = n; box.appendChild(n); }
  updateWelcome();
  updateCtxChip(true);
  maybeScroll(true);
}

function appendMsg(m) {
  S.msgs.push(m);
  const n = msgNode(m);
  m._el = n;
  $('msgs').appendChild(n);
  updateWelcome();
  updateCtxChip();
  maybeScroll();
}

function updateWelcome() {
  const has = S.msgs.filter(m => m.role !== 'tool').length > 0;
  $('welcome').classList.toggle('on', !has);
}

/* ---------------- dynamic text ---------------- */

function renderQuick() {
  const box = $('quick');
  const items = [
    ['i-folder', t('q1'), t('q1s'), t('q1p')],
    ['i-term', t('q2'), t('q2s'), t('q2p')],
    ['i-plan', t('q3'), t('q3s'), t('q3p')]
  ];
  box.innerHTML = '';
  for (const [icon, title, sub, prompt] of items) {
    const b = el('button', 'qcard');
    b.innerHTML = '<span class="qi">' + ic(icon) + '</span><div><b>' + esc(title) + '</b><br><span>' + esc(sub) + '</span></div>';
    b.onclick = () => {
      const inp = $('input');
      inp.value = prompt;
      inp.dispatchEvent(new Event('input'));
      submit();
    };
    box.appendChild(b);
  }
}

function applyI18n() {
  document.documentElement.lang = LANG;
  document.querySelectorAll('[data-i18n]').forEach(n => {
    // {v} — актуальная версия из init, чтобы «О приложении» не отставало
    n.textContent = t(n.dataset.i18n).replace('{v}', S.version || '');
  });
  document.querySelectorAll('[data-i18n-ph]').forEach(n => { n.placeholder = t(n.dataset.i18nPh); });
  document.querySelectorAll('#langSeg button').forEach(b => b.classList.toggle('on', b.dataset.lang === LANG));
  $('dfootLang').textContent = ({ uk: 'Українська', en: 'English', ru: 'Русский' })[LANG] || LANG;
  $('memChipLabel').textContent = String(S.mem.filter(c => c.enabled).length);
  renderQuick();
  renderSessions();
  renderMem();
  renderAll();
  setBusy(S.busy);
  syncSettings();
  renderSetCats();
  syncSetHeader();
  renderProfile();
  if ($('help') && $('help').classList.contains('on')) api({ cmd: 'diag' });
  if ($('updates') && $('updates').classList.contains('on')) renderUpdates();
  if ($('docs') && $('docs').classList.contains('on')) renderDocs();
  if ($('auth') && $('auth').classList.contains('on')) {
    const unlock = S.authMode === 'unlock';
    $('authHead').textContent = t(unlock ? 'authUnlockTitle' : 'dsLoginTitle');
    $('authSub').textContent = t(unlock ? 'authUnlockSub' : 'dsSub');
  }
}

/* ---------------- settings sync ---------------- */

function syncSettings() {
  renderAccounts();
  $('fBase').value = S.base || '';
  $('fThink').checked = !!S.thinking;
  $('fSearch').checked = !!S.search;
  $('fModel').value = S.model || 'default';
  if ($('fModel')._label) $('fModel')._label();
  if ($('fCtx')) {
    $('fCtx').value = String(ctxLimit());
    if ($('fCtx')._label) $('fCtx')._label();
  }
  if (S.foldAll !== undefined) $('fFoldAll').checked = !!S.foldAll;
  $('baseCount').textContent = (S.base || '').length;
  $('permStatus').textContent = S.storage ? t('accessOk') : t('accessNo');
  $('permStatus').className = 'status ' + (S.storage ? 'ok' : 'bad');
  $('dfootMem').textContent = S.mem.length ? S.mem.length + ' ' + t('cards') : '';
  syncApi();
  syncGuard();
  syncChips();
  renderStats();
  refreshSetCats();
}

/** Прев'ю на головному екрані оновлюємо лише коли він справді видимий. */
function refreshSetCats() {
  const s = $('settings');
  if (s && s.classList.contains('on') && !SET_CAT) renderSetCats();
}

function apiHost() { return S.apiLan ? '0.0.0.0' : '127.0.0.1'; }

function syncApi() {
  if (!$('fApiOn')) return;
  $('fApiOn').checked = !!S.apiAuto;
  $('fApiLan').checked = !!S.apiLan;
  $('fApiDel').checked = !!S.apiDel;
  if (document.activeElement !== $('fApiPort')) $('fApiPort').value = S.apiPort || 8787;
  $('fApiKey').value = maskKey(S.apiKey);
  $('fAuditSecret').value = S.auditMask || '';
  if (document.activeElement !== $('fAuditContact')) $('fAuditContact').value = S.auditContact || '';
  if (document.activeElement !== $('fAuditHook')) $('fAuditHook').value = S.auditHook || '';
  $('fPubOn').checked = S.apiPublic !== false;
  const st = $('apiStatus');
  st.textContent = S.apiRunning ? (t('apiOn') + ' · ' + apiHost() + ':' + S.apiPort) : t('apiOff');
  st.className = 'status ' + (S.apiRunning ? 'ok' : 'bad');
  $('apiToggle').querySelector('span').textContent = S.apiRunning ? t('apiStop') : t('apiStart');
  $('apiHint').textContent = t('apiHint') + ' · http://' + apiHost() + ':' + (S.apiPort || 8787) + '/v1'
    + (S.apiLan ? ' · ⚠ ' + t('lanWarn') : '');
  syncPub();
}

/* ---------------- API keys ---------------- */

function maskKey(k) {
  k = String(k || '');
  if (!k) return '';
  return k.length > 12 ? k.slice(0, 7) + '…' + k.slice(-4) : k;
}

function keyMeta(k) {
  if (!k.exp) return t('keyNever');
  const d = new Date(k.exp);
  const s = d.toLocaleDateString();
  return k.expired ? (t('keyExpired') + ' · ' + s)
    : (t('keyExpires') + ' ' + s);
}

function renderKeys() {
  const box = $('keyList');
  if (!box) return;
  const items = S.keys || [];
  box.innerHTML = '';
  if (!items.length) {
    box.innerHTML = '<div class="accempty">' + esc(t('keyNoKeys')) + '</div>';
    return;
  }
  for (const k of items) {
    const row = el('div', 'accrow' + (k.expired ? ' bad' : ''));
    const meta = el('div', 'accmeta');
    meta.innerHTML = '<b>' + esc(k.name || 'API') + '</b>' +
      '<small>' + esc(k.mask || '') + '</small>' +
      '<small>' + esc(keyMeta(k)) + '</small>';
    const cp = el('button', 'accact');
    cp.innerHTML = ic('i-copy');
    cp.setAttribute('aria-label', 'copy');
    cp.onclick = () => api({ cmd: 'key_copy', id: k.id });
    const rm = el('button', 'accact');
    rm.innerHTML = ic('i-trash');
    rm.setAttribute('aria-label', 'delete');
    rm.onclick = () => { if (confirm(t('keyDeleteAsk'))) api({ cmd: 'key_del', id: k.id }); };
    row.append(meta, cp, rm);
    box.appendChild(row);
  }
}

function openApiKeySheet() {
  $('akForm').hidden = false;
  $('akDone').hidden = true;
  $('akValue').value = '';
  $('akName').value = '';
  $('akTtl').value = '0';
  $('apiKeySheet').classList.add('on');
}

function closeApiKeySheet() {
  $('apiKeySheet').classList.remove('on');
  $('akValue').value = '';
}

/* ---------------- diagnostic telemetry ---------------- */

function renderTelemetry() {
  const cb = $('fTelemetry');
  if (!cb) return;
  cb.checked = S.telemetryOn !== false;
  const st = $('teleStatus');
  if (S.telemetryOn === false) {
    st.textContent = t('teleOff');
    st.className = 'status';
  } else {
    let txt = t('teleReady');
    if (S.telemetryPending) txt += ' · ' + t('teleQueue') + ': ' + S.telemetryPending;
    if (S.telemetryLast) txt += ' · ' + t('teleLastAt') + ': ' + new Date(S.telemetryLast).toLocaleString();
    st.textContent = txt;
    st.className = 'status ok';
  }
  const send = $('teleSend');
  if (send) send.disabled = S.telemetryOn === false;
}

/* ---------------- key audit ---------------- */

function auditActionLabel(a) {
  if (a === 'revoke') return t('auditActRevoke');
  if (a === 'notify') return t('auditActNotify');
  return t('auditActInfo');
}

function renderAuditLog() {
  const box = $('auditLogList');
  if (!box) return;
  const items = S.auditLog || [];
  box.innerHTML = '';
  if (!items.length) {
    box.innerHTML = '<div class="accempty">' + esc(t('auditEmpty')) + '</div>';
    return;
  }
  for (const ev of items) {
    const row = el('div', 'accrow');
    const meta = el('div', 'accmeta');
    const when = ev.ts ? new Date(ev.ts).toLocaleString() : '';
    meta.innerHTML = '<b>' + esc(auditActionLabel(ev.action)) + ' · ' + esc(ev.mask || '') + '</b>' +
      '<small>' + esc(when) + (ev.ip ? ' · ' + esc(ev.ip) : '') + '</small>' +
      (ev.note ? '<small>' + esc(ev.note) + '</small>' : '');
    row.appendChild(meta);
    box.appendChild(row);
  }
}

/* ---------------- public access (tunnel) ---------------- */

function pubBase() { return String(S.pubUrl || '').replace(/\/+$/, ''); }

function syncPub() {
  if (!$('fPubUrl')) return;
  if (document.activeElement !== $('fPubUrl')) $('fPubUrl').value = S.pubUrl || '';
  const st = $('pubStatus');
  if (!S.pubUrl) { st.textContent = t('pubNone'); st.className = 'status bad'; }
  else { st.textContent = pubBase(); st.className = 'status ok'; }
}

function fetchT(url, opt, ms) {
  const c = new AbortController();
  const id = setTimeout(() => c.abort(), ms || 8000);
  return fetch(url, Object.assign({ cache: 'no-store' }, opt || {}, { signal: c.signal }))
    .finally(() => clearTimeout(id));
}

async function checkPublic() {
  const st = $('pubStatus');
  const b = pubBase();
  if (!b) { syncPub(); return; }
  st.textContent = t('pubChecking'); st.className = 'status';
  try {
    const r = await fetchT(b + '/health', { headers: { Authorization: 'Bearer ' + (S.apiKey || '') } }, 8000);
    const j = await r.json().catch(() => ({}));
    if (r.ok && j.ok) {
      st.textContent = t('pubOk') + ' · v' + (j.version || '?') + ' · ' + (j.tools ? 'tools' : '');
      st.className = 'status ok';
    } else if (r.ok && j.locked) {
      st.textContent = t('pubOk') + ' · 🔒'; st.className = 'status bad';
    } else {
      st.textContent = t('pubBad') + (r.status ? ' · HTTP ' + r.status : '');
      st.className = 'status bad';
    }
  } catch (e) {
    st.textContent = t('pubBad'); st.className = 'status bad';
  }
}

function copyClientConfig() {
  const b = pubBase();
  if (!b) { toast(t('pubNone')); return; }
  const text = 'Base URL: ' + b + '/v1\nAPI key: ' + (S.apiKey || '') + '\nModel: deepseek-chat';
  api({ cmd: 'copy', text });
  toast(t('pubCopied'));
}

/* ---------------- update manifest ---------------- */

/* Чистий розбір маніфесту: без мережі, щоб це можна було тестувати. */
function parseManifest(j) {
  if (!j || typeof j !== 'object') return null;
  const code = Number(j.code);
  const version = String(j.version || '');
  const url = String(j.url || '');
  if (!isFinite(code) || code <= 0 || !version || !/^https?:\/\//i.test(url)) return null;
  return { code, version, url, notes: String(j.notes || ''), size: Number(j.size) || 0, md5: String(j.md5 || '') };
}

/* code — головний критерій; рядок версії — запасний, якщо code не передали. */
function isNewer(code, curCode, version, curVersion) {
  if (isFinite(code) && code > 0 && isFinite(curCode) && curCode > 0) return code > curCode;
  const a = String(version || '').split('.').map(n => parseInt(n, 10) || 0);
  const b = String(curVersion || '').split('.').map(n => parseInt(n, 10) || 0);
  for (let i = 0; i < Math.max(a.length, b.length); i++) {
    const x = a[i] || 0, y = b[i] || 0;
    if (x !== y) return x > y;
  }
  return false;
}

/* Публічний канал на GitHub Releases: працює навіть коли тунель вимкнено.
   raw.githubusercontent інколи довго віддає старий latest.json із кешу,
   тому другим джерелом іде jsDelivr — він оновлюється швидше. */
const UPD_FALLBACKS = [
  'https://raw.githubusercontent.com/Sqwid-member/Funachat/main/latest.json',
  'https://cdn.jsdelivr.net/gh/Sqwid-member/Funachat@main/latest.json'
];

function updManifestUrls() {
  const out = [];
  const add = (u) => { u = String(u || ''); if (u && out.indexOf(u) < 0) out.push(u); };
  if (S.updUrl) add(S.updUrl);
  const b = pubBase();
  if (b) add(b + '/updates/latest.json');
  for (const u of UPD_FALLBACKS) add(u);
  return out;
}

let UPD = null;

async function checkUpdates(silent) {  const st = $('updStatus');
  const btn = $('updGet');
  if (!st) return;
  const urls = updManifestUrls();
  if (!urls.length) {
    st.textContent = t('updNone'); st.className = 'status bad';
    if (btn) btn.hidden = true;
    UPD = null;
    return;
  }
  st.textContent = t('updChecking'); st.className = 'status';
  if (btn) btn.hidden = true;
  let man = null, badJson = false;
  // Йдемо по джерелах, доки якесь не віддасть валідний маніфест:
  // тунель може бути вимкнений, а raw — закешований.
  for (const m of urls) {
    try {
      const r = await fetchT(m, {}, 10000);
      if (!r.ok) continue;
      const parsed = parseManifest(await r.json());
      if (!parsed) { badJson = true; continue; }
      man = parsed;
      break;
    } catch (e) { /* наступне джерело */ }
  }
  if (!man) {
    UPD = null;
    st.textContent = badJson ? t('updBadJson') : t('updFail');
    st.className = 'status bad';
    if (!badJson && !silent) toast(t('updFail'));
    return;
  }
  if (isNewer(man.code, S.code, man.version, S.version)) {
    UPD = man;
    st.textContent = t('updAvail') + ' ' + man.version;
    st.className = 'status bad';
    if (btn) { btn.hidden = false; btn.querySelector('span').textContent = t('updDownload') + ' · ' + man.version; }
    if (silent) toast(t('updAvail') + ' ' + man.version);
  } else {
    UPD = null;
    st.textContent = t('updLatest') + ' · v' + (S.version || '');
    st.className = 'status ok';
  }
}

// тести в jsdom: чиста логіка маніфесту без мережі
window.__funaUpd = { parseManifest, isNewer, updManifestUrls };


function syncGuard() {
  if (!$('fGuardOn')) return;
  $('fGuardOn').checked = !!S.guardOn;
  if (document.activeElement !== $('fGuardGap')) $('fGuardGap').value = S.guardGap;
  if (document.activeElement !== $('fGuardMin')) $('fGuardMin').value = S.guardPerMin;
  if (document.activeElement !== $('fGuardDay')) $('fGuardDay').value = S.guardPerDay;
  const st = $('guardStatus');
  let txt = t('guardToday') + ': ' + (S.guardCount || 0) + ' / ' + S.guardPerDay;
  // guardCool — це «скільки лишалось» на момент події. Рахуємо від
  // абсолютного часу: інакше «пауза 30 min» висіла б у налаштуваннях і
  // після того, як пауза давно минула.
  const leftMs = Math.max(0, (S.guardCoolAt || 0) - Date.now());
  if (leftMs > 0) {
    const sec = Math.ceil(leftMs / 1000);
    const left = sec >= 90 ? Math.ceil(sec / 60) + ' min' : sec + ' s';
    txt += ' · ' + t('guardPause') + ' ' + left;
  }
  if (S.guardNote) txt += ' · ' + S.guardNote;
  st.textContent = txt;
  st.className = 'status ' + (leftMs > 0 ? 'bad' : 'ok');
}

function syncChips() {
  $('chipThink').classList.toggle('on', !!S.thinking);
  $('chipSearch').classList.toggle('on', !!S.search);
  $('chipMem').classList.toggle('on', S.mem.filter(c => c.enabled).length > 0);
  $('memChipLabel').textContent = String(S.mem.filter(c => c.enabled).length);
}

function syncSend() {
  const b = $('send');
  if (!b) return;
  const has = composerHasContent();
  // Під час відповіді кнопка не блокується: Enter/надсилання ставить
  // повідомлення в чергу. Окрема кнопка «Стоп» поруч зупиняє генерацію.
  b.disabled = !has;
  if (b._ic !== 'i-send') { b.innerHTML = ic('i-send'); b._ic = 'i-send'; }
  const st = $('stopBtn');
  if (st) st.classList.toggle('on', !!S.busy);
}

function setBusy(on) {
  S.busy = on;
  const box = $('msgs');
  if (box) box.classList.toggle('busy', on);
  // Вкладення можна прикріпити й поставити в чергу під час відповіді.
  const ab = $('attach');
  if (ab) { ab.disabled = false; ab.style.opacity = ''; }
  syncSend();
  if (!on && $('hint').textContent === t('answering')) $('hint').textContent = '';
  if (on) $('hint').textContent = t('answering');
}

/* ---------------- sessions ---------------- */

function relTime(ts) {
  if (!ts) return '';
  const d = new Date(ts * 1000), diff = (Date.now() - ts * 1000) / 1000;
  if (diff < 60) return '·';
  if (diff < 3600) return Math.floor(diff / 60) + 'm';
  if (diff < 86400) return Math.floor(diff / 3600) + 'h';
  if (diff < 86400 * 7) return Math.floor(diff / 86400) + 'd';
  return d.toLocaleDateString(LANG === 'en' ? 'en-GB' : LANG === 'ru' ? 'ru-RU' : 'uk-UA', { day: 'numeric', month: 'short' });
}

let sessionRAF = 0;

/** Кілька викликів поспіль (відкриття шухляди + відповіді нативу) зводимо
 *  в один кадр, інакше список перемальовується кілька разів і анімація
 *  появи запускається повторно. */
function renderSessions() {
  if (sessionRAF) return;
  sessionRAF = requestAnimationFrame(() => { sessionRAF = 0; drawSessions(); });
}

function drawSessions() {
  const box = $('sessionList');
  const q = (S.q || '').toLowerCase().trim();
  const fidOf = (sid) => S.fmap[sid] || null;
  let list = S.sessions.slice();
  if (S.folder === 'none') list = list.filter(s => !fidOf(s.id));
  else if (S.folder !== 'all') list = list.filter(s => String(fidOf(s.id)) === String(S.folder));
  list = list
    .sort((a, b) => ((b.pinned ? 1 : 0) - (a.pinned ? 1 : 0)) || ((b.updated_at || 0) - (a.updated_at || 0)))
    .filter(s => !q || String(s.title || '').toLowerCase().includes(q));
  // Папка теж входить у підпис: інакше після folder_move список у
  // відфільтрованій папці лишається старим (ті самі id/назви).
  const sig = S.folder + '#' + q + '#' + S.session + '#' +
    list.map(s => s.id + '|' + (s.title || '') + '|' + (s.pinned ? 1 : 0) + '|' +
      (s.updated_at || 0) + '|' + (fidOf(s.id) || '')).join(',');
  if (box.dataset.sig === sig && box.childElementCount) return;
  box.dataset.sig = sig;
  box.innerHTML = '';
  if (!list.length) {
    const msg = S.sessions.length ? t('nothing') : t('noSessions');
    box.appendChild(el('div', 'sub2', '<div style="padding:18px 12px;color:var(--dim);font-size:13px">' + esc(msg) + '</div>'));
    return;
  }
  list.forEach((s, i) => {
    const item = el('div', 'sitem' + (s.id === S.session ? ' cur' : ''));
    item.style.setProperty('--i', String(Math.min(i, 12)));
    item.innerHTML = '<div class="st"><div class="ttl2">' + (s.pinned ? '<span class="pin">' + ic('i-pin') + '</span>' : '') +
      esc(s.title || t('untitled')) + '</div>' +
      '<div class="sub2">' + relTime(s.updated_at) + (s.pinned ? ' · ' + esc(t('pinned')) : '') + '</div></div>' +
      '<button class="more">' + ic('i-more') + '</button>';
    item.querySelector('.st').onclick = () => {
      if (S.busy) { toast(t('stopFirst')); return; }
      if (!leaveQueueOk()) return;
      closeDrawer();
      flushDraft();
      clearMessages();
      api({ cmd: 'open', id: s.id });
    };
    item.querySelector('.more').onclick = (e) => {
      e.stopPropagation();
      S.sheetId = s.id;
      S.sheetPin = !!s.pinned;
      $('sheetPinLabel').textContent = s.pinned ? t('unpin') : t('pin');
      const fid = S.fmap[s.id];
      $('sheetFolderName').textContent = fid ? folderName(fid) : t('noFolder');
      $('sheetTitle').textContent = s.title || s.id;
      $('sheet').classList.add('on');
    };
    box.appendChild(item);
  });
  if (S.animList) {
    S.animList = false;
    box.classList.remove('switching');
    void box.offsetWidth;
    box.classList.add('switching');
  }
}

function folderName(id) {
  const f = S.folders.find(x => String(x.id) === String(id));
  return f ? (f.name || t('untitled')) : '';
}

function renderFolders() {
  const row = $('foldRow');
  if (!row) return;
  row.innerHTML = '';
  const items = [['all', t('allChats')], ['none', t('unfiled')]];
  for (const f of S.folders) items.push([String(f.id), f.name || t('untitled')]);
  for (const [id, label] of items) {
    const b = el('button', 'fchip' + (String(S.folder) === id ? ' on' : ''));
    b.textContent = label;
    b.onclick = () => {
      if (b._lp) { b._lp = false; return; }
      if (String(S.folder) === id) return;
      S.folder = id;
      vibe(5);
      renderFolders();
      renderSessions();
    };
    const fid = (id === 'all' || id === 'none') ? null : id;
    if (fid) {
      let tm = 0;
      const start = () => {
        clearTimeout(tm);
        tm = setTimeout(() => {
          b._lp = true;
          vibe(12);
          openNameSheet({ title: t('renameFolder'), value: folderName(fid), id: fid });
        }, 430);
      };
      const stop = () => clearTimeout(tm);
      b.addEventListener('pointerdown', start);
      b.addEventListener('pointerup', stop);
      b.addEventListener('pointerleave', stop);
      b.addEventListener('pointercancel', stop);
    }
    row.appendChild(b);
  }

  const add = el('button', 'fchip add');
  add.innerHTML = ic('i-add');
  add.title = t('createFolder');
  add.onclick = () => { vibe(6); openNameSheet({ title: t('createFolder'), value: '', id: null }); };
  row.appendChild(add);
}

function openNameSheet(o) {
  S.nameId = o && o.id ? o.id : null;
  $('nameSheetTitle').textContent = (o && o.title) || t('createFolder');
  const inp = $('nameInput');
  inp.value = (o && o.value) || '';
  document.querySelector('#nameSheet .namerow').classList.toggle('editing', !!S.nameId);
  $('nameSheet').classList.add('on');
  setTimeout(() => {
    try { inp.focus(); inp.setSelectionRange(inp.value.length, inp.value.length); } catch (e) {}
  }, 260);
}

function closeNameSheet() {
  $('nameSheet').classList.remove('on');
  S.nameId = null;
}

function saveNameSheet() {
  const inp = $('nameInput');
  const v = inp.value.trim();
  if (!v) { inp.focus(); return; }
  if (S.nameId) api({ cmd: 'folder_rename', id: S.nameId, name: v });
  else api({ cmd: 'folder_add', name: v });
  vibe(8);
  closeNameSheet();
}

function renderFolderPick() {
  const box = $('folderPick');
  if (!box) return;
  box.innerHTML = '';
  const add = (id, label) => {
    const b = el('button', 'sheetbtn');
    b.innerHTML = ic('i-folder') + '<span>' + esc(label) + '</span>';
    b.onclick = () => {
      $('folderSheet').classList.remove('on');
      api({ cmd: 'folder_move', sid: S.sheetId, fid: id });
    };
    box.appendChild(b);
  };
  add('', t('noFolder'));
  for (const f of S.folders) add(String(f.id), f.name || t('untitled'));
}

function artCode(root) {
  const f = root ? root.querySelector('iframe') : null;
  return f ? (f.getAttribute('srcdoc') || '') : '';
}

function openArtFull(root) {
  const code = artCode(root);
  if (!code) return;
  const m = (code.match(/<title[^>]*>([^<]{1,60})<\/title>/i) || [])[1] || '';
  S.artName = m.replace(/[^A-Za-z0-9._-]+/g, '-').replace(/^-+|-+$/g, '') || 'page';
  // Той самий link-hook, що й у вбудованій картці: інакше клік по http(s)
  // у повноекранному режимі навігував би iframe замість браузера.
  $('artFullFrame').srcdoc = withLinkHook(code);
  $('artFull').classList.add('on');
  vibe(8);
}

function downloadArt(root) {
  const code = artCode(root);
  if (!code) return;
  const m = (code.match(/<title[^>]*>([^<]{1,60})<\/title>/i) || [])[1] || '';
  const name = m.replace(/[^A-Za-z0-9._-]+/g, '-').replace(/^-+|-+$/g, '') || 'page';
  api({ cmd: 'save_html', name, content: stripLinkHook(code) });
  vibe(8);
}

function startEdit(m) {
  if (S.busy || !m || m.role !== 'user') return;
  if (!m.id) { toast(t('error')); return; }
  if (archivedMsg(m)) { toast(t('versionArchived')); return; }
  const index = S.msgs.indexOf(m);
  let parent = 0;
  for (let i = index - 1; i >= 0; i--) {
    if (S.msgs[i].role === 'assistant' && S.msgs[i].id) { parent = S.msgs[i].id; break; }
  }
  S.editing = { m, index, parent, draft: $('input').value };
  const inp = $('input');
  inp.value = m.text;
  inp.dispatchEvent(new Event('input'));
  $('editBar').classList.add('on');
  inp.focus();
  try { inp.scrollIntoView({ block: 'center' }); } catch (e) {}
}

function cancelEdit(restore) {
  if (!S.editing) return;
  const draft = S.editing.draft;
  S.editing = null;
  $('editBar').classList.remove('on');
  if (restore !== false) {
    $('input').value = draft || '';
    $('input').dispatchEvent(new Event('input'));
  }
}

function openMsgSheet(m) {
  S.msgTarget = m;
  const asst = m.role !== 'user';
  $('msgSheetTitle').textContent = asst ? 'DeepSeek' : t('messageActions');
  $('msgEdit').style.display = asst ? 'none' : 'flex';
  $('msgRegen').style.display = asst && !!m.id && !S.busy && !S.compactBusy ? 'flex' : 'none';
  $('msgCont').style.display = asst && !!m.id && !S.busy && !S.compactBusy ? 'flex' : 'none';
  $('msgSheet').classList.add('on');
}

// Чернетка привязана до акаунта: перемикання не показує текст іншого.
function draftKey() { return 'funa.draft.' + (S.activeId || '') + '.' + (S.session || 'new'); }
/** Ключі чернеток у порядку збереження (localStorage тримає порядок вставки). */
function draftKeys() {
  const out = [];
  try {
    for (let i = 0; i < localStorage.length; i++) {
      const x = localStorage.key(i);
      if (x && x.indexOf('funa.draft.') === 0) out.push(x);
    }
  } catch (e) {}
  return out;
}
/** Лишає не більше max чернеток, не чіпаючи щойно записану. */
function trimDrafts(keep, max) {
  const keys = draftKeys().filter(x => x !== keep);
  const extra = keys.length + 1 - max;
  for (let i = 0; i < extra && i < keys.length; i++) {
    try { localStorage.removeItem(keys[i]); } catch (e) {}
  }
}
function saveDraft() {
  try {
    const k = draftKey();
    let v = $('input').value;
    if (!v) { localStorage.removeItem(k); return; }
    // очень длинная вставка не должна превращать localStorage в свалку
    if (v.length > 200000) v = v.slice(0, 200000);
    try {
      localStorage.setItem(k, v);
    } catch (e) {
      // квота: выкидываем чужие черновики и пробуем ещё раз
      trimDrafts(k, 1);
      try { localStorage.setItem(k, v); } catch (e2) {}
      return;
    }
    // Черновики копятся по одному на каждый чат: держим последние 60.
    trimDrafts(k, 60);
  } catch (e) {}
}
/** Скидає відкладене збереження й пише чернетку негайно (перед зміною чату). */
function flushDraft() {
  try { clearTimeout($('input')._dh); } catch (e) {}
  saveDraft();
}
function loadDraft() {
  const inp = $('input');
  try { inp.value = localStorage.getItem(draftKey()) || ''; } catch (e) { inp.value = ''; }
  inp.dispatchEvent(new Event('input'));
}

function clearMessages() {
  S.msgs = [];
  S.cur = null;
  queueClear();
  $('msgs').innerHTML = '';
  updateWelcome();
  updateCtxChip();
}

/* ---------------- memory ---------------- */

function renderMem() {
  const box = $('memList');
  box.innerHTML = '';
  const q = (S.mq || '').toLowerCase().trim();
  const list = S.mem.filter(c => !q || ((c.title || '') + ' ' + (c.body || '') + ' ' + (c.tags || '')).toLowerCase().includes(q));
  $('memEmpty').classList.toggle('on', !S.mem.length);
  for (const c of list) {
    const d = el('div', 'mcard' + (c.enabled ? '' : ' off'));
    const tags = (c.tags || '').split(',').map(x => x.trim()).filter(Boolean)
      .map(x => '<span class="tg">' + esc(x) + '</span>').join('');
    d.innerHTML =
      '<div class="mt"><b>' + esc(c.title || t('untitled')) + '</b>' +
      '<button class="mtog' + (c.enabled ? ' on' : '') + '" data-mtog>' + ic('i-check') + '</button></div>' +
      (c.body ? '<div class="mb">' + esc(c.body) + '</div>' : '') +
      (tags ? '<div class="mtags">' + tags + '</div>' : '');
    d.onclick = () => openCard(c);
    d.querySelector('[data-mtog]').onclick = (e) => {
      e.stopPropagation();
      api({ cmd: 'mem_toggle', id: c.id, on: !c.enabled });
    };
    box.appendChild(d);
  }
  syncChips();
  $('dfootMem').textContent = S.mem.length ? S.mem.length + ' ' + t('cards') : '';
}

function openCard(card) {
  S.memEdit = card || null;
  $('cardSheetTitle').textContent = card ? t('editCard') : t('newCard');
  $('cTitle').value = card ? (card.title || '') : '';
  $('cBody').value = card ? (card.body || '') : '';
  $('cTags').value = card ? (card.tags || '') : '';
  $('cardDelete').style.display = card ? 'inline-flex' : 'none';
  $('cardSheet').classList.add('on');
  setTimeout(() => $('cTitle').focus(), 60);
}

function closeCard() {
  $('cardSheet').classList.remove('on');
  S.memEdit = null;
}

function saveCard() {
  const title = $('cTitle').value.trim();
  const body = $('cBody').value.trim();
  const tags = $('cTags').value.trim();
  if (!title && !body) { toast(t('memoryEmpty')); return; }
  api({ cmd: 'mem_save', id: S.memEdit ? S.memEdit.id : 0, title, body, tags });
  closeCard();
}

/* ---------------- panels ---------------- */

function openDrawer() { S.animList = true; const sb = $('sessionList'); if (sb) sb.dataset.sig = ''; $('drawer').classList.add('on'); $('scrim').classList.add('on'); api({ cmd: 'sessions' }); api({ cmd: 'folders' }); }
function closeDrawer() { $('drawer').classList.remove('on'); $('scrim').classList.remove('on'); }
/* Налаштування розбиті на категорії: головний екран — список категорій,
   кожна категорія — окрема сторінка з власним заголовком. Секції лежать
   у DOM постійно (їх читає syncSettings), показується лише активна. */
const SET_CATS = [
  { id: 'account', icon: 'i-user', name: 'catAccount', sub: 'catAccountSub' },
  { id: 'chat', icon: 'i-spark', name: 'catChat', sub: 'catChatSub' },
  { id: 'api', icon: 'i-globe', name: 'catApi', sub: 'catApiSub' },
  { id: 'data', icon: 'i-folder', name: 'catData', sub: 'catDataSub' },
  { id: 'look', icon: 'i-lang', name: 'catLook', sub: 'catLookSub' },
  { id: 'stats', icon: 'i-cards', name: 'catStats', sub: 'catStatsSub' },
  { id: 'help', icon: 'i-file', name: 'catHelp', sub: 'catHelpSub' }
];
let SET_CAT = '';

function langName() {
  return ({ uk: 'Українська', en: 'English', ru: 'Русский' })[LANG] || LANG;
}

/** Короткий стан категорії для головного екрана. */
function setCatMeta(id) {
  switch (id) {
    case 'account': {
      const a = activeAccount();
      if (a) return accountTitle(a);
      if (S.signedIn) {
        const st = S.auth || {};
        return st.name || st.email || st.phone || 'DeepSeek';
      }
      return t('accEmpty');
    }
    case 'chat': return (S.model === 'expert' ? 'DeepSeek V4.1 Flash · Expert' : 'DeepSeek V4.1 Flash');
    case 'api': return (S.apiRunning ? t('apiOn') : t('apiOff')) + ' · ' + (S.apiPort || 8787);
    case 'data': return (S.pages ? S.pages.length : 0) + ' · ' +
      (S.storage ? t('accessOk') : t('accessNo'));
    case 'look': return langName();
    case 'stats': {
      const st = statsGet();
      return fmtNum(st.q) + ' ' + t('statReq').toLowerCase();
    }
    case 'help': return 'v' + (S.version || '');
    default: return '';
  }
}

function renderSetCats() {
  const box = $('setCats');
  if (!box) return;
  box.innerHTML = '';
  SET_CATS.forEach(c => {
    const b = el('button', 'setcat');
    b.type = 'button';
    b.dataset.cat = c.id;
    b.innerHTML = '<span class="scico">' + ic(c.icon) + '</span>' +
      '<span class="sctxt"><b>' + esc(t(c.name)) + '</b>' +
      '<small>' + esc(t(c.sub)) + '</small></span>' +
      '<span class="scmeta">' + esc(setCatMeta(c.id)) + '</span>' +
      '<svg class="ic schev"><use href="#i-chevron"/></svg>';
    b.onclick = () => openSetCat(c.id);
    box.appendChild(b);
  });
}

function syncSetHeader() {
  const cat = SET_CATS.find(c => c.id === SET_CAT);
  const ttl = $('setTitle'), sub = $('setSub');
  if (!ttl || !sub) return;
  ttl.textContent = cat ? t(cat.name) : t('settings');
  sub.textContent = cat ? t(cat.sub) : t('settingsHint');
}

function openSettingsHome() {
  SET_CAT = '';
  const home = $('setHome'), pages = $('setPages'), foot = $('setFoot');
  if (home) home.classList.add('on');
  if (pages) pages.classList.remove('on');
  if (foot) foot.classList.remove('on');
  document.querySelectorAll('#setPages .setpage').forEach(p => p.classList.remove('on'));
  renderSetCats();
  syncSetHeader();
  if (home) home.scrollTop = 0;
}

function openSetCat(id) {
  const page = document.querySelector('#setPages .setpage[data-cat="' + id + '"]');
  if (!page) return;
  SET_CAT = id;
  $('setHome').classList.remove('on');
  $('setPages').classList.add('on');
  $('setFoot').classList.add('on');
  document.querySelectorAll('#setPages .setpage').forEach(p => p.classList.toggle('on', p === page));
  syncSetHeader();
  $('setPages').scrollTop = 0;
  if (id === 'stats') renderStats();
  else if (id === 'account') { renderProfile(); renderAccounts(); }
  else if (id === 'api') { syncApi(); if (!S.pubUrl) api({ cmd: 'pub_file', quiet: true }); }
}

function openSettings() {
  syncSettings();
  openSettingsHome();
  $('settings').classList.add('on');
  api({ cmd: 'whoami' });
  api({ cmd: 'api_state' });
}
function closeSettings() { $('settings').classList.remove('on'); }
/** Назад у налаштуваннях: зі сторінки категорії — на список, зі списку — геть. */
function settingsBackOrClose() {
  if (SET_CAT) { openSettingsHome(); return; }
  closeSettings();
}
function openMemory() { $('memory').classList.add('on'); api({ cmd: 'mem_list' }); }
function closeMemory() { $('memory').classList.remove('on'); }

/* ---------------- attachments ---------------- */

function isImgFile(f) { return !!(f && (f.isImage || /^image\//.test(f.mime || ''))); }

function fmtSize(n) {
  n = +n || 0;
  if (n >= 1048576) return (n / 1048576).toFixed(1) + ' MB';
  if (n >= 1024) return Math.round(n / 1024) + ' KB';
  return n + ' B';
}

function fileTiles(files) {
  if (!files || !files.length) return '';
  return '<div class="ufiles">' + files.map(f => {
    const src = isImgFile(f) && f.thumb ? '<img src="' + f.thumb + '" alt="">' : '';
    return '<div class="ufile' + (src ? ' img' : '') + '">' + src +
      '<div class="umeta">' + ic(src ? 'i-image' : 'i-file') +
      '<span>' + esc(f.name || 'file') + '</span>' +
      (src ? '' : '<em>' + fmtSize(f.size) + '</em>') + '</div></div>';
  }).join('') + '</div>';
}

function clearAtt() {
  S.att = [];
  renderAtt();
}

function renderAtt() {
  const bar = $('attachBar');
  if (!bar) return;
  bar.innerHTML = S.att.map((f, i) => {
    const st = f.status || 'uploading';
    const pct = Math.max(0, Math.min(100, f.progress || 0));
    const thumb = isImgFile(f) && f.thumb
      ? '<img src="' + f.thumb + '" alt="">'
      : ic(isImgFile(f) ? 'i-image' : 'i-file');
    const sub = st === 'error' ? esc(f.msg || t('fileError'))
      : st === 'ok' ? fmtSize(f.size) : (pct ? pct + '%' : t('fileUploading'));
    return '<div class="afile ' + (st === 'error' ? 'err' : '') + '">' + thumb +
      '<div class="ameta"><b>' + esc(f.name || 'file') + '</b><em>' + sub + '</em></div>' +
      '<button class="adel" data-adel="' + i + '" aria-label="remove">' + ic('i-close') + '</button></div>';
  }).join('');
  bar.classList.toggle('on', S.att.length > 0);
  syncSend();
}

/* ---------------- files: links, images, cards, viewer ---------------- */

const FROOT = '/sdcard/Download/FunaFiles';
const THUMBS = {}, THUMB_PEND = {}, INFO_PEND = {};
/* Прев'ю — data:URL по кілька десятків КБ. Без стелі довгий чат із
   картинками тримає в памʼяті десятки мегабайт; старі витісняємо. */
const THUMB_ORDER = [], THUMB_MAX = 120;
function thumbPut(path, data) {
  if (!path || !data) return;
  if (!(path in THUMBS)) THUMB_ORDER.push(path);
  THUMBS[path] = data;
  while (THUMB_ORDER.length > THUMB_MAX) {
    const k = THUMB_ORDER.shift();
    if (k !== path) delete THUMBS[k];
  }
}

/** Одноразове розкодування HTML-сутностей (текст у Markdown уже екранований). */
function htmlUnesc(s) {
  return String(s == null ? '' : s).replace(/&(amp|lt|gt|quot|#39);/g,
    (m, k) => ({ amp: '&', lt: '<', gt: '>', quot: '"', '#39': "'" }[k]));
}

function normPath(p) {
  let s = htmlUnesc(p).trim();
  if (s.startsWith('//:file:')) s = s.slice(8);
  else if (s.startsWith('file://')) s = s.slice(7);
  try { s = decodeURIComponent(s); } catch (e) {}
  s = s.replace(/\\/g, '/');
  s = s.replace(/^\/storage\/emulated\/0/, '/sdcard');
  if (!s) return s;
  if (s.charAt(0) !== '/') s = FROOT + '/' + s;
  else if (!/^\/sdcard(\/|$)/.test(s)) s = '/sdcard' + s;
  return s;
}

function fileBase(p) {
  const s = String(p == null ? '' : p).replace(/\/+$/, '');
  const i = s.lastIndexOf('/');
  return i >= 0 ? s.slice(i + 1) : s;
}

function fileIconId(kind, name) {
  const k = String(kind || '').toLowerCase();
  if (k === 'image' || /\.(png|jpe?g|gif|webp|bmp|heic|heif)$/i.test(name || '')) return 'i-image';
  if (k === 'pdf' || k === 'docx' || k === 'doc' || k === 'sheet' || k === 'slides' || k === 'text') return 'i-page';
  return 'i-file';
}

function imgHtml(url, alt) {
  const u = String(url || '');
  const a = alt == null ? '' : String(alt);
  if (/^(\/\/:file:|file:\/\/)/.test(u)) {
    const path = normPath(u);
    const cached = THUMBS[path];
    return '<span class="imglink" data-fpath="' + esc(path) + '">' +
      '<img class="aimg" data-fpath="' + esc(path) + '" alt="' + a + '"' +
      (cached ? ' src="' + cached + '"' : ' src=""') + '>' +
      (a ? '<em>' + a + '</em>' : '') + '</span>';
  }
  return '<span class="imglink"><img class="aimg" src="' + u + '" alt="' + a + '" loading="lazy">' +
    (a ? '<em>' + a + '</em>' : '') + '</span>';
}

function attachCards(paths) {
  if (!paths || !paths.length) return '';
  return '<div class="attachcards">' + paths.map(raw => {
    const path = normPath(raw);
    return '<button class="acard" data-fpath="' + esc(path) + '" data-fname="' + esc(fileBase(raw)) + '">' +
      '<span class="aicon">' + ic('i-file') + '</span>' +
      '<span class="ameta2"><b>' + esc(fileBase(normPath(raw))) + '</b><em></em></span>' +
      ic('i-chevron', 'achest') + '</button>';
  }).join('') + '</div>';
}

function paintCard(card, o) {
  if (!card) return;
  const name = o.name || card.getAttribute('data-fname') || fileBase(o.path);
  const b = card.querySelector('b'); if (b) b.textContent = name;
  const em = card.querySelector('em');
  if (em) em.textContent = [o.kind || '', o.size != null ? fmtSize(o.size) : ''].filter(Boolean).join(' · ');
  const icon = card.querySelector('.aicon');
  if (icon) {
    if (o.thumb) icon.innerHTML = '<img src="' + o.thumb + '" alt="">';
    else icon.innerHTML = ic(fileIconId(o.kind, name));
  }
}

function hydrateNode(node) {
  if (!node || !node.querySelectorAll) return;
  node.querySelectorAll('img.aimg[data-fpath]').forEach(img => {
    const path = img.getAttribute('data-fpath');
    if (!path) return;
    if (THUMBS[path]) { img.src = THUMBS[path]; return; }
    if (img.getAttribute('src')) return;
    if (THUMB_PEND[path]) return;
    THUMB_PEND[path] = true;
    api({ cmd: 'file_thumb', path });
  });
  node.querySelectorAll('.acard[data-fpath]').forEach(card => {
    const path = card.getAttribute('data-fpath');
    if (!path) return;
    if (card.getAttribute('data-ready')) { const o = card._info; if (o) paintCard(card, o); return; }
    if (INFO_PEND[path]) return;
    INFO_PEND[path] = true;
    api({ cmd: 'file_info', path });
  });
}

function updateCards(path, o) {
  document.querySelectorAll('.acard[data-fpath]').forEach(card => {
    if (card.getAttribute('data-fpath') !== path) return;
    card._info = o;
    card.setAttribute('data-ready', '1');
    paintCard(card, o);
  });
}

function openFileSheet(path, name) {
  path = normPath(path);
  S.fileTarget = { path, name: name || fileBase(path) };
  $('fileSheetTitle').textContent = S.fileTarget.name;
  $('fileSheetMeta').textContent = t('loadingFile');
  $('filePreview').innerHTML = '';
  $('fileSheet').classList.add('on');
  api({ cmd: 'file_info', path });
  vibe(6);
}

function closeFileSheet() { $('fileSheet').classList.remove('on'); }

function openViewer(path, name) {
  path = normPath(path);
  // seq: ответ на запрос прошлого файла не должен попасть в этот просмотрщик
  S.view = { path, name: name || fileBase(path), pages: [], total: 0, kind: '', remote: false,
    seq: ++S.viewSeq };
  $('viewerName').textContent = S.view.name;
  $('viewerBody').innerHTML = '<div class="vload">' + esc(t('loadingFile')) + '</div>';
  document.querySelectorAll('#viewer .vact').forEach(b => { b.style.display = ''; });
  $('viewer').classList.add('on');
  api({ cmd: 'view_file', path, seq: S.view.seq });
  vibe(6);
}

function countNote(v, shown) {
  if (v.total > shown) return '<div class="vtotal">' + esc(t('pdfLimit').replace('{n}', String(shown))) + '</div>';
  return '<div class="vtotal">' + v.total + ' ' + esc(t('pagesCount')) + '</div>';
}

function openRemoteImage(url, name) {
  // seq теж потрібен: інакше відповідь view_file, що ще летіла,
  // перемалювала б вікно поверх щойно відкритої картинки
  S.view = { remote: true, path: '', name: name || 'image', pages: [], total: 0,
    kind: 'image', seq: ++S.viewSeq };
  $('viewerName').textContent = S.view.name;
  $('viewerBody').innerHTML = '<img class="vimg" src="' + esc(url) + '" alt="">';
  document.querySelectorAll('#viewer .vact').forEach(b => { b.style.display = 'none'; });
  $('viewer').classList.add('on');
}

function closeViewer() {
  $('viewer').classList.remove('on');
  $('viewerBody').innerHTML = '';
  S.view = null;
}

/* 40 страниц по ~200 КБ data-url — это уже предел WebView: сотни
   картинок в DOM роняют рендерер. Дальше предлагаем «Открыть с…». */
const PDF_MAX_PAGES = 40;

function paintPdfPages() {
  const v = S.view;
  if (!v) return;
  const box = $('viewerBody');
  const shown = v.pages.length;
  const moreOk = v.total > shown && shown < PDF_MAX_PAGES;
  box.innerHTML = '<div class="vpages">' + v.pages.map((d, i) =>
    '<div class="vpage"><span>' + (i + 1) + '</span><img src="' + d + '" alt=""></div>').join('') + '</div>' +
    (moreOk
      ? '<button class="btn tonal vmore" id="vMore">' + esc(t('pdfMore')) +
        ' (' + shown + '/' + v.total + ')</button>'
      : countNote(v, shown));
  const more = $('vMore');
  if (more) more.onclick = () => {
    more.disabled = true;
    api({ cmd: 'pdf_more', path: v.path, offset: v.pages.length, count: 3, seq: v.seq || 0 });
  };
}

function renderViewer(o) {
  const v = S.view;
  if (!v) return;
  v.kind = o.kind || v.kind;
  if (o.path) v.path = o.path;
  if (o.name) { v.name = o.name; $('viewerName').textContent = o.name; }
  const box = $('viewerBody');
  if (o.kind === 'image' && o.data) {
    box.innerHTML = '<img class="vimg" src="' + o.data + '" alt="">';
  } else if (o.kind === 'pdf') {
    v.pages = (o.pages || []).slice();
    v.total = o.total || v.pages.length;
    paintPdfPages();
  } else if (o.kind === 'docx' || o.kind === 'text') {
    box.innerHTML = '<pre class="vtext">' + esc(o.text || '') + '</pre>';
  } else {
    box.innerHTML = '<div class="vload">' + esc(t('noPreview')) +
      '<br><span>' + esc(v.path) + '</span></div>';
  }
}

/* ---------------- context compaction ---------------- */

/* Ліміт контексту можна вибрати в «Чат і ШІ». Типове значення — 1M
   токенів: стільки тримає актуальна модель DeepSeek V4.1 Flash. */
const CTX_PRESETS = [60000, 128000, 256000, 512000, 1000000];
const CTX_DEFAULT = 1000000;

function ctxLimit() {
  const v = +S.ctxLimit;
  if (CTX_PRESETS.indexOf(v) >= 0) return v;
  return CTX_DEFAULT;
}

function estTokens() {
  let n = 0;
  const from = Math.max(0, S.ctxFrom || 0);
  for (let i = from; i < S.msgs.length; i++) {
    const m = S.msgs[i];
    if (m.role === 'user' || m.role === 'assistant') n += (m.text || '').length + (m.think || '').length;
  }
  return Math.ceil(n / 3.2);
}

function transcriptText() {
  const parts = [];
  // Після першого стиснення стара частина вже є в конспекті: не
  // переказуємо її сирий текст удруге, а додаємо конспект і нові репліки.
  const prev = (S.compact || '').trim();
  if (prev) parts.push('CONTEXT SUMMARY SO FAR:\n' + prev);
  const from = prev ? Math.max(0, S.ctxFrom || 0) : 0;
  for (let i = from; i < S.msgs.length; i++) {
    const m = S.msgs[i];
    if (m.role === 'user' && (m.text || '').trim()) parts.push('USER: ' + m.text.trim());
    else if (m.role === 'assistant' && (m.text || '').trim()) parts.push('ASSISTANT: ' + m.text.trim());
  }
  let s = parts.join('\n\n');
  // На 1M контексті конспект має бачити більше розмови, ніж на 60k.
  const cap = Math.max(140000, Math.min(400000, Math.round(ctxLimit() * 0.4)));
  if (s.length > cap) {
    const head = Math.min(60000, Math.round(cap * 0.15));
    s = s.slice(0, head) + '\n\n…\n\n' + s.slice(-(cap - head));
  }
  return s;
}

function addDivider(text) { appendMsg({ role: 'divider', text }); }

function setHint(msg) {
  const h = $('hint');
  if (!h) return;
  h.textContent = msg || '';
  h.classList.toggle('on', !!msg);
}

function startCompact(auto) {
  if (S.compactBusy || S.busy || !S.msgs.length) return;
  const tr = transcriptText();
  if (tr.length < 200) {
    S.pendingSend = null;
    if (auto) submit(true);
    return;
  }
  S.compactBusy = true;
  setHint(t(auto ? 'compactAuto' : 'compacting'));
  if (auto) toast(t('compactAuto'));
  vibe(10);
  api({ cmd: 'compact', transcript: tr, auto: !!auto, sid: S.session || '' });
}

/* ---------------- send & queue ---------------- */

function composerHasContent() {
  const v = $('input') ? $('input').value.trim() : '';
  return !!v || S.att.some(f => f.status === 'ok' && f.id);
}

function renderQueue() {
  const box = $('queueBar');
  if (!box) return;
  const n = S.queue.length;
  box.classList.toggle('on', n > 0);
  if (!n) { box.innerHTML = ''; syncSend(); return; }
  const head = '<div class="qhead">' + ic('i-cards') +
    '<b>' + esc(t('queueTitle')) + ' · ' + n + '</b>' +
    '<span class="qnote">' + esc(S.queueHold ? t('queuePaused') : t('queueHint')) + '</span>' +
    '<span class="grow"></span>' +
    (S.queueHold ? '<button class="qplay" data-qplay="1" title="' + esc(t('queueResume')) +
      '" aria-label="' + esc(t('queueResume')) + '">' + ic('i-send') + '</button>' : '') +
    '</div>';
  const rows = S.queue.map((it, i) => {
    const label = it.text ? esc(it.text) : (it.files && it.files.length ? '📎 ' + it.files.length : '');
    return '<div class="qrow" role="listitem" data-qi="' + i + '">' +
      '<span class="qix">' + (i + 1) + '</span>' +
      '<div class="qtext">' + label + '</div>' +
      '<span class="qacts">' +
        '<button type="button" data-qup="1" title="' + esc(t('queueUp')) + '"' +
          (i === 0 ? ' disabled' : '') + '>' + ic('i-up') + '</button>' +
        '<button type="button" data-qdown="1" title="' + esc(t('queueDown')) + '"' +
          (i === n - 1 ? ' disabled' : '') + '>' + ic('i-down') + '</button>' +
        '<button type="button" data-qedit="1" title="' + esc(t('edit')) + '">' + ic('i-edit') + '</button>' +
        '<button type="button" data-qdel="1" title="' + esc(t('queueRemove')) + '">' + ic('i-trash') + '</button>' +
      '</span></div>';
  }).join('');
  box.innerHTML = head + rows;
  syncSend();
}

/** Додає повідомлення в чергу; воно піде автоматично після поточної відповіді. */
function enqueueMsg(it) {
  S.queueSeq = (S.queueSeq || 0) + 1;
  S.queue.push({
    id: 'q' + S.queueSeq,
    text: it.text || '',
    files: it.files || [],
    thinking: it.thinking !== undefined ? !!it.thinking : !!S.thinking,
    search: it.search !== undefined ? !!it.search : !!S.search
  });
  renderQueue();
  toast(t('queueAdded'));
}

/** Не дає загубити чергу при переході в інший чат. */
function leaveQueueOk() {
  if (!S.queue.length) return true;
  return confirm(t('queueLeave').replace('{n}', S.queue.length));
}

function openQueueEdit(i) {
  const it = S.queue[i];
  if (!it) return;
  S.queueEdit = it;
  $('queueText').value = it.text || '';
  $('queueSheet').classList.add('on');
  setTimeout(() => { try { $('queueText').focus(); } catch (e) {} }, 30);
}

function queueClear() {  S.queue = [];
  S.queueHold = false;
  if ($('queueSheet').classList.contains('on')) $('queueSheet').classList.remove('on');
  S.queueEdit = null;
  renderQueue();
}

/** Відправляє наступний елемент черги, щойно модель завершила відповідь. */
function pumpQueue() {
  if (S.busy || S.compactBusy || S.queueHold || !S.queue.length) return;
  if (!S.signedIn) { S.queueHold = true; renderQueue(); return; }
  if (estTokens() > ctxLimit()) {
    S.queueHold = true;
    toast(t('queueCtxFull'));
    renderQueue();
    return;
  }
  const it = S.queue.shift();
  renderQueue();
  if (!it) return;
  deliver(it.text || '', it.files || [], {
    fromQueue: true, thinking: it.thinking, search: it.search
  });
}

/** Надсилає вже підготовлене повідомлення (з поля введення або з черги). */
function deliver(text, files, opts) {
  opts = opts || {};
  S.editTarget = null;
  // Черга не має чіпати те, що користувач уже набирає в полі введення.
  if (!opts.fromQueue) {
    const inp = $('input');
    inp.value = '';
    inp.style.height = 'auto';
    try { localStorage.removeItem(draftKey()); } catch (e) {}
    clearAtt();
  }
  vibe(8);
  S.follow = true;
  S.pendingUser = text;
  const bn = $('msgs').querySelector('.banner'); if (bn) bn.remove();
  const msg = { role: 'user', text, files, ts: Date.now() };
  appendMsg(msg);
  // Сообщение нарисовано до подтверждения сервером: если запрос упадёт
  // (401/429/сеть), его нужно убрать из чата, а текст и вложения вернуть
  // в поле ввода (или обратно в очередь) — иначе после перезагрузки оно
  // бы просто исчезло.
  S.pendingMsg = { msg, text, files, summary: S.compact || '', parent: S.parent,
    fromQueue: !!opts.fromQueue };
  S.parent = 0;
  setBusy(true);
  api({ cmd: 'send', text, files: files.map(f => f.id), summary: S.compact || '',
    thinking: opts.thinking !== undefined ? !!opts.thinking : S.thinking,
    search: opts.search !== undefined ? !!opts.search : S.search });
  S.compact = '';
}

function submit(skipCompact) {
  const inp = $('input');
  const text = inp.value.trim();
  const ready = S.att.filter(f => f.status === 'ok' && f.id);
  if (!text && !ready.length) return;
  if (!S.signedIn) { toast(t('needToken')); openAuth('ds'); return; }
  if (S.att.some(f => f.status !== 'ok')) { toast(t('fileWait')); return; }
  if (S.editing) return submitEdit(text);
  const files = ready.map(f => ({ id: f.id, name: f.name, size: f.size, isImage: !!f.isImage, thumb: f.thumb || '' }));
  // Модель зараз працює або стискає контекст — не втрачаємо текст,
  // а ставимо його в чергу.
  if (S.busy || S.compactBusy) {
    enqueueMsg({ text, files, thinking: S.thinking, search: S.search });
    inp.value = '';
    inp.style.height = 'auto';
    try { localStorage.removeItem(draftKey()); } catch (e) {}
    clearAtt();
    syncSend();
    return;
  }
  if (!skipCompact && estTokens() > ctxLimit()) {
    S.pendingSend = { text, files: ready.length };
    startCompact(true);
    return;
  }
  deliver(text, files);
}

/** Возвращает неотправленное сообщение в поле ввода. */
function restoreSend() {
  const b = S.pendingMsg;
  S.pendingMsg = null;
  if (!b) return;
  const m = b.msg;
  const i = S.msgs.indexOf(m);
  if (i >= 0) S.msgs.splice(i, 1);
  if (m._el) m._el.remove();
  // a_start уже мог создать пустой пузырь ответа — его тоже убираем.
  if (S.cur && !S.cur.id && !S.cur.text && !S.cur.think) {
    const ci = S.msgs.indexOf(S.cur);
    if (ci >= 0) S.msgs.splice(ci, 1);
    if (S.cur._el) S.cur._el.remove();
    S.cur = null;
  }
  S.pendingUser = null;
  if (b.fromQueue) {
    // Сообщение из черги не надіслалося — ставимо його назад у початок
    // черги й зупиняємо автозапуск, щоб не крутитися в циклі помилок.
    S.queueSeq = (S.queueSeq || 0) + 1;
    S.queue.unshift({ id: 'q' + S.queueSeq, text: b.text || '', files: b.files || [] });
    S.queueHold = true;
    renderQueue();
    toast(t('queueRestored'));
  } else {
    const inp = $('input');
    if (inp) {
      inp.value = b.text || '';
      inp.style.height = 'auto';
      try { inp.style.height = Math.min(150, inp.scrollHeight) + 'px'; } catch (e) {}
      syncSend();
      saveDraft();
    }
    S.att = b.files || [];
    renderAtt();
  }
  // Конспект уже был снят для этого запроса: без него повторная отправка
  // ушла бы без сжатой истории. parent тоже возвращаем, если сервер не
  // успел назначить новый.
  if (b.summary) S.compact = b.summary;
  if (typeof b.parent === 'number') S.parent = b.parent;
  updateWelcome();
  updateCtxChip();
  if (S.find) runFind(S.find);
}

function submitEdit(text) {
  const ed = S.editing;
  if (!ed) return;
  const inp = $('input');
  inp.value = '';
  inp.style.height = 'auto';
  $('editBar').classList.remove('on');
  S.editing = null;
  try { localStorage.removeItem(draftKey()); } catch (e) {}
  vibe(10);
  const m = ed.m || { role: 'user' };
  const mid = m.id || 0;
  const files = (m.files || []).slice();
  for (const f of S.att) {
    if (f.status === 'ok' && f.id && !files.some(x => x.id === f.id)) {
      files.push({ id: f.id, name: f.name, size: f.size, isImage: !!f.isImage, thumb: f.thumb || '' });
    }
  }
  const cut = S.msgs.indexOf(m);
  // Правка рубає хвіст переписки локально ще до відповіді сервера. Якщо
  // запит упаде (401/429/мережа), хвіст і старий текст мають повернутися.
  // Заодно фіксуємо стару версію разом з її хвостом: до неї можна буде
  // повернутися кнопками ‹ ›.
  let vers = m.vers;
  const oldVers = m.vers ? m.vers.slice() : null;
  const oldVi = m.vi || 0;
  if (vers && vers.length) {
    if (m.vi === undefined) m.vi = 0;
    verSync(m, true);
  } else {
    vers = [verSnapshot(m, cut >= 0 ? S.msgs.slice(cut + 1) : [])];
    m.vi = 0;
  }
  S.editBackup = { m, text: m.text || '', files: (m.files || []).slice(), id: m.id || 0,
    ts: m.ts || 0, ms: m.ms || 0, cut, tail: cut >= 0 ? S.msgs.slice(cut + 1) : [],
    vers: oldVers, vi: oldVi };
  m.text = text;
  m.files = files;
  m.id = 0;
  m.ts = Date.now();
  m.ms = 0;
  vers.push(verSnapshot(m, []));
  m.vers = vers;
  m.vi = vers.length - 1;
  S.editTarget = m;
  if (cut >= 0) S.msgs = S.msgs.slice(0, cut + 1);
  else S.msgs.push(m);
  renderAll();
  S.cur = null;
  S.pendingUser = null;
  S.mode = 'edit';
  S.parent = ed.parent || 0;
  setBusy(true);
  clearAtt();
  statsAdd({ edit: 1 });
  api({ cmd: 'edit', text, id: mid, files: files.map(f => f.id), thinking: S.thinking, search: S.search });
}

function lastMsgId() {
  for (let i = S.msgs.length - 1; i >= 0; i--) {
    const id = S.msgs[i].id;
    if (id) return id;
  }
  return 0;
}

/** Повертає чат до стану до невдалої правки. */
function restoreEdit() {
  const b = S.editBackup;
  S.editBackup = null;
  S.editTarget = null;
  if (!b) return;
  const m = b.m;
  const idx = S.msgs.indexOf(m);
  if (b.cut >= 0) S.msgs = S.msgs.slice(0, b.cut);
  else if (idx >= 0) S.msgs.splice(idx, 1);
  m.text = b.text;
  m.files = b.files;
  m.id = b.id;
  m.ts = b.ts || 0;
  m.ms = b.ms || 0;
  if (b.vers) { m.vers = b.vers; m.vi = b.vi || 0; }
  else { m.vers = null; m.vi = 0; }
  m._el = null;
  m._md = null;
  m._foot = null;
  if (b.cut >= 0) S.msgs.push(m);
  for (const x of b.tail) { resetUiMsg(x); S.msgs.push(x); }
  S.parent = lastMsgId();
  renderAll();
  if (S.find) runFind(S.find);
}

function regenMsg(m) {
  if (!m || m.role !== 'assistant' || !m.id || S.busy) return;
  const i = S.msgs.indexOf(m);
  if (i < 0) return;
  if (archivedMsg(m)) { toast(t('versionArchived')); return; }
  // Стара відповідь не зникає назавжди: вона стає версією, до якої можна
  // повернутися разом із хвостом, що йшов під нею. Нова відповідь
  // отримає цей самий масив versions і стане останньою версією.
  let vers;
  if (m.vers && m.vers.length) {
    if (m.vi === undefined) m.vi = 0;
    verSync(m, true);
    vers = m.vers;
  } else {
    vers = [verSnapshot(m, captureTail(m))];
    m.vi = 0;
  }
  S.regenBackup = { m, i, tail: S.msgs.slice(i + 1) };
  S.regenPending = { vers: vers };
  S.msgs.splice(i);
  if (m._el && m._el.parentNode) m._el.parentNode.removeChild(m._el);
  for (const x of S.regenBackup.tail) {
    if (x._el && x._el.parentNode) x._el.parentNode.removeChild(x._el);
  }
  renderAll();
  S.mode = 'regen';
  S.cur = null;
  S.parent = m.id;
  vibe(10);
  setBusy(true);
  statsAdd({ regen: 1 });
  api({ cmd: 'regen', child: m.id, thinking: S.thinking, search: S.search });
}

/** Возвращает удалённое сообщение, если regen так и не начался. */
function restoreRegen() {
  const b = S.regenBackup;
  S.regenBackup = null;
  S.regenPending = null;
  if (!b || !b.m) return;
  const at = Math.max(0, Math.min(b.i, S.msgs.length));
  resetUiMsg(b.m);
  const tail = (b.tail || []).map(resetUiMsg);
  S.msgs = S.msgs.slice(0, at).concat([b.m]).concat(tail);
  S.parent = lastMsgId();
  renderAll();
  if (S.find) runFind(S.find);
}

function contMsg(m) {
  if (!m || m.role !== 'assistant' || !m.id || S.busy) return;
  if (archivedMsg(m)) { toast(t('versionArchived')); return; }
  S.mode = 'cont';
  S.contTarget = m;
  m.streaming = true;
  m.status = 'WIP';
  paintAssistant(m);
  vibe(8);
  setBusy(true);
  api({ cmd: 'cont', id: m.id });
}

function chatMarkdown() {
  let out = '# ' + (S.title || 'FunaChat') + '\n\n';
  const meta = (m) => {
    const bits = [];
    if (m.ts) bits.push(fmtFull(m.ts));
    if (m.role === 'assistant' && m.ms) bits.push(t('aiWork') + ' ' + fmtMs(m.ms));
    return bits.length ? '_' + bits.join(' · ') + '_\n\n' : '';
  };
  for (const m of S.msgs) {
    if (m.role === 'user') {
      out += '## ' + t('you') + '\n\n' + meta(m) + m.text + '\n\n';
      if (m.files && m.files.length) out += m.files.map(f => '- 📎 ' + (f.name || 'file')).join('\n') + '\n\n';
    }
    else if (m.role === 'assistant') {
      out += meta(m);
      if (m.think) out += '<details><summary>' + t('thinking') + '</summary>\n\n' + m.think + '\n\n</details>\n\n';
      out += m.text + '\n\n';
    } else if (m.role === 'tool') {
      out += '> ' + (m.name || 'tool') + '\n\n```\n' + (m.output || '') + '\n```\n\n';
    }
  }
  return out;
}

/* ---------------- find in chat ---------------- */

function clearFind() {
  document.querySelectorAll('#msgs mark.srch').forEach(m => {
    const p = m.parentNode;
    if (!p) return;
    p.replaceChild(document.createTextNode(m.textContent), m);
    p.normalize();
  });
  S.findHits = [];
  S.findIdx = -1;
}

function focusHit() {
  S.findHits.forEach(h => h.classList.remove('cur'));
  const h = S.findHits[S.findIdx];
  if (h) {
    h.classList.add('cur');
    try { h.scrollIntoView({ block: 'center', behavior: 'smooth' }); } catch (e) {}
  }
  const n = S.findHits.length;
  $('findCount').textContent = n ? (S.findIdx + 1) + '/' + n : (S.find ? t('noFind') : '');
}

function runFind(q) {
  clearFind();
  S.find = q || '';
  if (!S.find.trim()) { focusHit(); return; }
  // Регулярка з 'i' замість toLowerCase/indexOf: регістр у деяких мовах
  // (напр. «İ») змінює довжину рядка, і позиції збігів розʼїжджалися б із
  // самим текстом — підсвічувався не той фрагмент.
  let re;
  try {
    re = new RegExp(S.find.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'gi');
  } catch (e) { focusHit(); return; }
  const nodes = Array.from(document.querySelectorAll('#msgs .md'));
  for (const root of nodes) {
    const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, null);
    const texts = [];
    let n;
    while ((n = walker.nextNode())) texts.push(n);
    for (const tn of texts) {
      const val = tn.nodeValue || '';
      re.lastIndex = 0;
      if (!re.test(val)) continue;
      re.lastIndex = 0;
      const frag = document.createDocumentFragment();
      let i = 0, m;
      while ((m = re.exec(val))) {
        if (m.index > i) frag.appendChild(document.createTextNode(val.slice(i, m.index)));
        const mk = document.createElement('mark');
        mk.className = 'srch';
        mk.textContent = m[0];
        frag.appendChild(mk);
        S.findHits.push(mk);
        i = m.index + m[0].length;
        if (!m[0].length) re.lastIndex++;
      }
      if (i < val.length) frag.appendChild(document.createTextNode(val.slice(i)));
      if (tn.parentNode) tn.parentNode.replaceChild(frag, tn);
    }
  }
  S.findIdx = S.findHits.length ? 0 : -1;
  focusHit();
}

function openFind() {
  $('findBar').classList.add('on');
  const inp = $('findInput');
  inp.value = S.find || '';
  setTimeout(() => { try { inp.focus(); } catch (e) {} }, 120);
}

function stepFind(dir) {
  const n = S.findHits.length;
  if (!n) return;
  S.findIdx = (S.findIdx + dir + n) % n;
  focusHit();
}

/* Включение замка должно не только показать оверлей, но и убрать данные
   аккаунта из DOM: запрос sessions/mem/folders, отправленный за миг до
   блокировки, иначе дорисовал бы чужие строки под экраном пароля. */
function purgeAccountDom() {
  S.session = null; S.parent = 0; S.first = true;
  S.cur = null; S.pendingUser = null; S.editing = null;
  S.att = []; S.accounts = []; S.contacts = {}; S.apiKey = ''; S.keys = [];
  S.auditMask = ''; S.auditContact = ''; S.auditHook = ''; S.auditLog = [];
  renderKeys(); renderAuditLog();
  S.compact = ''; S.compactBusy = false; S.ctxFrom = 0; S.subs = {};
  S.pendingSend = null; S.pendingResume = false;
  S.regenBackup = null; S.editBackup = null; S.pendingMsg = null;
  S.editTarget = null; S.regenPending = null;
  S.sessions = []; S.q = ''; S.folders = []; S.fmap = {}; S.mem = []; S.mq = '';
  S.sheetId = null; S.msgTarget = null; S.fileTarget = null;
  S.view = null; S.viewSeq = (S.viewSeq || 0) + 1;
  S.artName = ''; S.pages = []; S.pagesOpen = false;
  if ($('input')) $('input').value = '';
  if ($('fApiKey')) $('fApiKey').value = '';
  if ($('sSearch')) $('sSearch').value = '';
  if ($('mSearch')) $('mSearch').value = '';
  clearAtt();
  clearMessages();
  renderSessions(); renderFolders(); renderMem(); renderPages(); renderAccounts();
  if ($('viewer').classList.contains('on')) closeViewer();
  if ($('fileSheet').classList.contains('on')) closeFileSheet();
  if ($('artFull').classList.contains('on')) { $('artFull').classList.remove('on'); $('artFullFrame').srcdoc = ''; }
  if ($('ctxSheet').classList.contains('on')) closeCtx();
  if ($('msgSheet').classList.contains('on')) $('msgSheet').classList.remove('on');
  if ($('subSheet').classList.contains('on')) closeSub();
  if ($('memory').classList.contains('on')) closeMemory();
  if ($('pages').classList.contains('on')) $('pages').classList.remove('on');
  closeDrawer();
  closeSettings();
  if ($('findBar').classList.contains('on')) closeFind();
  // Ответ старого аккаунта мог оставить «Стоп» на кнопке отправки.
  setBusy(false);
  S.contTarget = null;
  setTitle('FunaChat');
}

function openPages() {
  S.pagesOpen = true;
  $('pages').classList.add('on');
  api({ cmd: 'pages_list' });
}

function closeFind() {
  $('findBar').classList.remove('on');
  $('findInput').value = '';
  S.find = '';
  clearFind();
  $('findCount').textContent = '';
}

/* ---------------- saved pages ---------------- */

function renderPages() {
  const box = $('pageList');
  if (!box) return;
  box.innerHTML = '';
  $('pageEmpty').classList.toggle('on', !S.pages.length);
  for (const p of S.pages) {
    const row = el('div', 'pagerow');
    const kb = p.size > 1024 ? (p.size / 1024).toFixed(1) + ' KB' : (p.size || 0) + ' B';
    row.innerHTML = '<div class="picon">' + ic('i-page') + '</div>' +
      '<div class="pinfo"><b>' + esc(p.name || '') + '</b><span>' + esc(kb) + ' · ' + esc(relTime(p.time)) + '</span></div>' +
      '<button class="pdel" data-pdel="' + esc(p.name || '') + '">' + ic('i-trash') + '</button>';
    row.querySelector('.pinfo').onclick = () => { vibe(6); api({ cmd: 'page_read', name: p.name }); };
    row.querySelector('.pdel').onclick = (ev) => {
      ev.stopPropagation();
      if (confirm(t('deletePage'))) api({ cmd: 'page_del', name: p.name });
    };
    box.appendChild(row);
  }
}

/* ---------------- bridge events ---------------- */

/**
 * Перетворює повідомлення історії (з сервера або зі снапшота версії)
 * у внутрішній формат чату. Результати інструментів показуємо карткою,
 * а не звичайним бульбашкою користувача.
 */
function histMsg(m, tsmap) {
  if (!m) return null;
  const rec = (tsmap && m.id) ? tsmap[m.id] : null;
  if (m.role === 'user' && /^(Результат інструмента|Tool result)/.test(m.text || '')) {
    const mm = (m.text || '').match(/^(?:Результат інструмента|Tool result) «([^»]+)»/);
    return { role: 'tool', name: mm ? mm[1] : 'tool', args: '', status: 'done',
      output: m.text || '', ts: m.ts || (rec && rec.ts) || 0 };
  }
  const it = { role: m.role, text: m.text || '', think: m.think || '', streaming: false,
    status: m.pending ? 'WIP' : 'FINISHED', id: m.id, files: m.files || [],
    ts: m.ts || (rec && rec.ts) || 0, ms: m.ms || (rec && rec.ms) || 0 };
  if (m.vers && m.vers.length > 1) {
    it.vers = m.vers.map(v => {
      const r = (tsmap && v.id) ? tsmap[v.id] : null;
      return Object.assign({}, v, {
        ts: v.ts || (r && r.ts) || 0,
        ms: v.ms || (r && r.ms) || 0
      });
    });
    it.vi = Math.max(0, Math.min(m.vi || 0, m.vers.length - 1));
  }
  return it;
}

window.__funa = function (raw) {
  let o;
  try { o = JSON.parse(raw); } catch (e) { return; }
  switch (o.e) {
    case 'init': {
      S.signedIn = !!o.signedIn; S.activeId = o.activeId || '';
      S.base = o.base || ''; S.baseAuto = !!o.baseAuto;
      S.thinking = !!o.thinking; S.search = !!o.search; S.model = o.model || 'default';
      S.ctxLimit = +o.ctxLimit || 1000000;
      S.storage = !!o.storage; S.version = o.version || '1.1';
      LANG = o.lang || 'uk'; S.lang = LANG;
      S.accounts = o.accounts || [];
      S.apiRunning = !!o.apiRunning; S.apiPort = o.apiPort || 8787;
      S.apiKey = o.apiKey || ''; S.apiAuto = !!o.apiAuto;
      S.apiLan = !!o.apiLan; S.apiDel = o.apiDel !== false;
      S.code = +o.code || 0; S.pubUrl = o.pubUrl || ''; S.updUrl = o.updUrl || '';
      S.apiPublic = o.apiPublic !== false; S.keys = [];
      S.auditMask = o.auditMask || ''; S.auditContact = o.auditContact || '';
      S.auditHook = o.auditHook || ''; S.auditLog = [];
      S.telemetryOn = o.telemetryOn !== false;
      S.telemetryLast = +o.telemetryLast || 0; S.telemetryPending = +o.telemetryPending || 0;
      S.guardOn = o.guardOn !== false; S.guardGap = o.guardGap || 0;
      S.guardPerMin = o.guardPerMin || 12; S.guardPerDay = o.guardPerDay || 400;
      S.guardCount = o.guardCount || 0; S.guardCool = o.guardCool || 0;
      S.guardCoolAt = Date.now() + S.guardCool;
      S.guardNote = o.guardNote || '';
      S.foldAll = !!o.foldAll;
      S.contacts = o.contacts || S.contacts || {};
      S.crash = !!o.crash;
      applyI18n();
      if (o.crashNew) { toast(t('crashToast')); api({ cmd: 'crash_seen' }); }
      renderAccounts();
      S.auth = o.auth || {};
      // Локальне блокування: до розблокування не показуємо навіть чернетку
      // й не просимо список чатів/памʼять — вони належать іншому акаунту.
      const lockedNow = !!(S.auth.has && S.auth.lock && !S.auth.unlocked);
      // Захист у глибину: навіть якщо нативна сторона колись поверне ці
      // поля під замком, у DOM вони не потраплять.
      if (lockedNow) {
        S.accounts = []; S.contacts = {}; S.apiKey = ''; S.keys = [];
        $('fApiKey').value = '';
        renderKeys();
        renderAccounts();
      }
      if (!lockedNow) loadDraft();
      renderProfile();
      if (S.signedIn && !lockedNow) { api({ cmd: 'sessions' }); api({ cmd: 'mem_list' }); api({ cmd: 'folders' }); }
      if (!lockedNow) { api({ cmd: 'keys_list' }); api({ cmd: 'audit_log' }); }
      // Тиха телеметрія: нативна сторона сама перевіряє «раз на добу».
      if (!lockedNow) setTimeout(() => api({ cmd: 'telemetry_auto', stats: statsGet() }), 3000);
      renderTelemetry();
      if (lockedNow) openAuth('unlock', true);
      else if (!S.signedIn && !S.auth.skipped) openAuth('ds');
      // Онбординг не должен перекрывать экран разблокировки.
      if (o.intro && !lockedNow) openIntro();
      // Тихо перевіряємо канал оновлень, якщо він налаштований.
      if (!lockedNow && (S.pubUrl || S.updUrl)) setTimeout(() => checkUpdates(true), 4000);
      break;
    }
    case 'auth': {
      S.auth = o.state || S.auth;
      if (o.signedIn !== undefined) S.signedIn = !!o.signedIn;
      renderProfile();
      $('unlockBtn').disabled = false;
      $('lockSave').disabled = false;
      const act = o.action || '';
      if (!o.ok) {
        const msg = o.msg || t('error');
        if (act === 'unlock') $('unlockMsg').textContent = msg;
        else if (act === 'lock') {
          if (o.code === 'need_pass' || o.code === 'bad_pass') openLockSheet(o.code === 'bad_pass' ? msg : '');
          else { $('lockMsg').textContent = msg; toast(msg); }
        }
        else $('loginMsg').textContent = msg;
        break;
      }
      if (act === 'ds_login') {
        S.signedIn = true;
        S.fromUnlock = false;
        closeAuth();
        toast(t('authLoginOk'));
        const c = S.contacts || {};
        if (!c.email && !c.phone) {
          S.contacts = { email: S.auth.email || '', phone: S.auth.phone || '', google: '' };
        }
        api({ cmd: 'accounts' });
        api({ cmd: 'sessions' });
        api({ cmd: 'mem_list' });
        api({ cmd: 'folders' });
        whoamiSoon();
      } else if (act === 'unlock') {
        closeAuth();
        toast(t('authUnlocked'));
        if (!S.signedIn) { openAuth('ds'); toast(t('needToken')); }
        else {
          // Дані акаунта вантажимо лише тепер — після пароля. «new» потрібен,
          // якщо під замком був холодний старт або зміна акаунта; відкритий
          // чат після блокування з налаштувань не чіпаємо.
          if (!S.msgs.length) api({ cmd: 'new' });
          api({ cmd: 'sessions' });
          api({ cmd: 'mem_list' });
          api({ cmd: 'folders' });
          // Під замком список акаунтів не віддавався; після пароля його
          // треба завантажити, інакше «Акаунти» в налаштуваннях порожні.
          api({ cmd: 'accounts' });
          whoamiSoon();
        }
      } else if (act === 'lock') {
        closeLockSheet();
        renderProfile();
        toast(S.auth.lock ? t('lockOn') : t('lockOff'));
        if (S.auth.lock && !S.auth.unlocked) {
          purgeAccountDom();
          openAuth('unlock', true);
        }
      } else if (act === 'switch') {
        closeAuth();
        renderProfile();
        if (!S.signedIn) { openAuth('ds'); toast(t('needToken')); }
        else if (S.auth.lock && !S.auth.unlocked) openAuth('unlock', true);
        else toast(t('accSwitched'));
      } else if (act === 'logout') {
        S.signedIn = false;
        S.auth = o.state || S.auth;
        closeSettings();
        renderProfile();
        toast(t('authLoggedOut'));
        openAuth('ds', !!S.auth.lock);
      } else if (act === 'skip') {
        if (S.auth.has && S.auth.lock && !S.auth.unlocked) {
          openAuth('unlock', true);
          break;
        }
        closeAuth();
        if (!S.signedIn) { openSettings(); toast(t('needToken')); }
      }
      break;
    }
    case 'diag': S.crash = !!o.crash; renderDiag(o.items || []); break;
    case 'report': if ($('repSheet').classList.contains('on')) $('repText').textContent = o.text || ''; break;
    case 'imported':
      S.signedIn = true;
      closeAuth();
      toast(t('authTokenSaved'));
      api({ cmd: 'accounts' });
      break;
    case 'acc_result':
      if (o.ok) {
        $('loginMsg').textContent = '';
        closeAuth();
        toast(t('authTokenSaved'));
        if (o.reset) toast(t('lockReset'));
        api({ cmd: 'accounts' });
        api({ cmd: 'auth_state' });
      } else {
        $('loginMsg').textContent = o.msg || t('error');
      }
      break;
    case 'acc_check':
      if (o.ok) toast(t('accOk'));
      else toast(o.msg || t('error'));
      break;
    case 'whoami':
      // Статус видно на картці акаунта; тост тут лише заважав би.
      break;
    case 'sessions': S.sessions = o.items || []; renderSessions(); break;
    case 'history': {
      S.session = o.id; S.parent = 0; S.first = !!o.first;
      S.compact = ''; S.compactBusy = false; S.pendingSend = null; S.ctxFrom = 0;
      queueClear();
      S.reqLive = false; S.reqBaseOut = 0;
      S.regenBackup = null; S.editBackup = null; S.pendingMsg = null;
      S.editTarget = null; S.regenPending = null;
      S.editing = null; S.msgTarget = null;
      $('editBar').classList.remove('on');
      $('msgSheet').classList.remove('on');
      clearAtt();
      S.msgs = [];
      S.subs = {};
      if ($('subSheet').classList.contains('on')) closeSub();
      const tsmap = msgTsLoad();
      for (const m of (o.items || [])) {
        const it = histMsg(m, tsmap);
        if (!it) continue;
        S.msgs.push(it);
        if (it.id) {
          S.parent = it.id;
          msgTsPut(tsmap, it.id, it.ts, it.ms);
        }
        if (it.vers) for (const v of it.vers) msgTsPut(tsmap, v.id, v.ts, v.ms);
      }
      msgTsSave(tsmap);
      if (o.older) S.msgs.unshift({ role: 'divider', text: t('olderHidden').replace('{n}', o.older) });
      if (S.msgs.length) S.first = false;
      const known = S.sessions.find(x => x.id === o.id);
      setTitle(known ? known.title : '');
      renderAll();
      loadDraft();
      api({ cmd: 'sub_list' });
      break;
    }
    case 'session':
      S.session = o.id || null;
      if (o.title) setTitle(o.title);
      pruneSubs();
      break;
    case 'clean': {
      S.session = null; S.parent = 0; S.first = true;
      S.reqLive = false; S.reqBaseOut = 0;
      S.regenBackup = null; S.editBackup = null; S.pendingMsg = null;
      S.editTarget = null; S.regenPending = null;
      S.editing = null; S.msgTarget = null;
      $('editBar').classList.remove('on');
      S.cur = null;
      if (o.keep) {
        const resumed = !!S.pendingResume;
        if (resumed) {
          S.pendingResume = false;
          const p = S.pendingSend;
          S.pendingSend = null;
          if (p && p.text) {
            const box = $('input');
            box.value = p.text;
            box.dispatchEvent(new Event('input'));
          }
          setTimeout(() => submit(true), 20);
        } else {
          // Ручне стиснення закінчилось — можна продовжити чергу.
          setTimeout(pumpQueue, 30);
        }
      } else {
        S.compact = '';
        S.ctxFrom = 0;
        S.subs = {};
        if ($('subSheet').classList.contains('on')) closeSub();
        clearAtt();
        setTitle('FunaChat');
        clearMessages();
        loadDraft();
      }
      break;
    }
    case 'user': {
      if (S.pendingUser === o.text) { S.pendingUser = null; break; }
      appendMsg({ role: 'user', text: o.text, ts: Date.now() });
      break;
    }
    case 'a_start': {
      // Відповідь справді почалась: відновлювати видалений при regen текст
      // уже не потрібно.
      const rp = S.regenPending;
      S.regenPending = null;
      S.regenBackup = null; S.editBackup = null; S.pendingMsg = null;
      if (S.contTarget) {
        S.cur = S.contTarget;
        S.contTarget = null;
        S.cur.streaming = true;
        S.cur._adding = true;
        paintAssistant(S.cur);
        paintMeta(S.cur);
        break;
      }
      const node = { role: 'assistant', text: '', think: '', streaming: true, status: 'WIP', id: 0,
        ts: Date.now(), ms: 0 };
      if (rp && rp.vers) {
        const vers = rp.vers;
        vers.push(verSnapshot(node, []));
        node.vers = vers;
        node.vi = vers.length - 1;
      }
      S.cur = node;
      appendMsg(S.cur);
      break;
    }
    case 'a_id':
      if (S.cur) { S.cur.id = o.id; S.parent = o.id; verSync(S.cur, false); rememberTs(S.cur); }
      S.pendingMsg = null;
      break;
    case 'think': S.pendingMsg = null; addDelta(S.cur, 'think', o.v); break;
    case 'answer': S.pendingMsg = null; addDelta(S.cur, 'text', o.v); break;
    case 'a_end': {
      // Ответ пришёл целиком: сообщение пользователя точно сохранено.
      S.pendingMsg = null;
      if (S.cur) {
        S.cur.streaming = false;
        S.cur.status = o.status;
        S.cur.thinkOpen = false;
        if (o.id) S.cur.id = o.id;
        if (o.ts) S.cur.ts = o.ts;
        S.cur.ms = (S.cur._adding ? (S.cur.ms || 0) : 0) + Math.max(0, o.ms || 0);
        S.cur._adding = false;
        verSync(S.cur, true);
        paintAssistant(S.cur);
        paintMeta(S.cur);
        rememberTs(S.cur);
      }
      if (S.find) runFind(S.find);
      break;
    }
    case 'req_end': {
      // Один запит користувача = один рядок статистики, навіть якщо модель
      // встигла зробити кілька раундів з інструментами.
      if (!S.reqLive) break;
      // Считаем прирост всех ответов, а не только новых сообщений:
      // продолжение (continue) дописывается в существующий пузырь.
      const out = Math.max(0, totalOutTokens() - (S.reqBaseOut || 0));
      const patch = { ms: Math.max(0, o.ms || 0), tout: out };
      if (o.ok) patch.done = 1; else patch.stop = 1;
      statsAdd(patch);
      break;
    }
    case 'sub_start': {
      // Картка належить своїй сесії. Поки «session» не прийшов (перше
      // повідомлення), S.session ще null — тоді приймаємо картку, а
      // pruneSubs() прибере її, якщо сесія виявиться іншою.
      if (o.sid && S.session && o.sid !== S.session) break;
      S.subs[o.id] = { id: o.id, sid: o.sid || '', title: o.title || o.id, task: o.task || '',
        status: 'run', t0: o.t0 || Date.now(), live: '', result: '', ms: 0 };
      if (!S.msgs.some(m => m.role === 'sub' && m.id === o.id)) appendMsg({ role: 'sub', id: o.id });
      break;
    }
    case 'sub_list': {
      for (const s of (o.items || [])) {
        S.subs[s.id] = Object.assign({}, S.subs[s.id] || {}, s);
        if (!S.msgs.some(m => m.role === 'sub' && m.id === s.id)) appendMsg({ role: 'sub', id: s.id });
        else repaintSub(s.id);
      }
      break;
    }
    case 'sub_live': {
      const s = S.subs[o.id];
      if (s) { s.live = o.text || ''; repaintSub(o.id); }
      const lt = $('subLiveText');
      if (lt && S.subView && S.subView.id === o.id) lt.textContent = o.text || '';
      break;
    }
    case 'sub_log': {
      if (S.subView && S.subView.id === o.id && $('subSheet').classList.contains('on')) {
        api({ cmd: 'sub_view', id: o.id });
      }
      break;
    }
    case 'sub_end': {
      const s = S.subs[o.id];
      if (s) {
        s.status = o.status || 'done';
        s.result = o.result || '';
        s.error = o.error || '';
        s.ms = o.ms || 0;
        repaintSub(o.id);
        toast(t('subFinishedToast') + (s.title ? ': ' + s.title : ''));
      }
      if (S.subView && S.subView.id === o.id) api({ cmd: 'sub_view', id: o.id });
      break;
    }
    case 'sub_view': {
      if (o.missing) { toast(o.error || t('error')); break; }
      renderSub(o);
      break;
    }
    case 'tool': {
      if (o.name === 'subagent') {
        if (S.cur && S.cur.text) {
          const cleaned = S.cur.text.replace(/```funa[\s\S]*?```/g, '').trim();
          if (cleaned !== S.cur.text) { S.cur.text = cleaned; paintAssistant(S.cur); }
        }
        break;
      }
      if (o.status === 'run') {
        if (S.cur && S.cur.text) {
          const cleaned = S.cur.text.replace(/```funa[\s\S]*?```/g, '').trim();
          if (cleaned !== S.cur.text) { S.cur.text = cleaned; paintAssistant(S.cur); }
        }
        // прогрес уже наявної картки (субагент/довгий інструмент) — оновлюємо, не дублюємо
        if (o.output) {
          let upd = null;
          for (let i = S.msgs.length - 1; i >= 0; i--) {
            const m = S.msgs[i];
            if (m.role === 'tool' && m.status === 'run' && m.name === o.name) { upd = m; break; }
            if (m.role !== 'tool') break;
          }
          if (upd) {
            upd.output = o.output;
            const nn = msgNode(upd);
            if (upd._el) upd._el.replaceWith(nn); else $('msgs').appendChild(nn);
            upd._el = nn;
            break;
          }
        }
        appendMsg({ role: 'tool', name: o.name, args: o.args, status: 'run', output: o.output || '' });
      } else {
        statsAdd({ tool: 1 });
        for (let i = S.msgs.length - 1; i >= 0; i--) {
          const m = S.msgs[i];
          if (m.role === 'tool' && m.name === o.name && m.status === 'run') {
            m.status = o.status;
            m.output = o.output || '';
            const nn = msgNode(m);
            if (m._el) m._el.replaceWith(nn); else $('msgs').appendChild(nn);
            m._el = nn;
            break;
          }
        }
      }
      break;
    }
    case 'u_id': {
      // Сервер принял сообщение — откатывать его из чата уже нельзя.
      S.pendingMsg = null;
      const tgt = S.editTarget;
      S.editTarget = null;
      if (tgt) {
        tgt.id = o.id;
        tgt.ts = tgt.ts || Date.now();
        verSync(tgt, true);
        paintMeta(tgt);
        rememberTs(tgt);
      } else {
        for (let i = S.msgs.length - 1; i >= 0; i--) {
          const m = S.msgs[i];
          if (m.role === 'user') {
            m.id = o.id;
            m.ts = m.ts || Date.now();
            paintMeta(m);
            rememberTs(m);
            break;
          }
        }
      }
      break;
    }
    case 'accounts': {
      const prevId = S.activeId;
      S.accounts = o.items || [];
      S.activeId = o.activeId || '';
      if (o.signedIn !== undefined) S.signedIn = !!o.signedIn;
      // інший акаунт — інша чернетка (ключ містить activeId)
      if (prevId && prevId !== S.activeId) loadDraft();
      renderAccounts();
      renderProfile();
      renderStats();
      break;
    }
    case 'account_deleted': {
      // Видалений акаунт більше ніколи не стане активним, але його
      // чернетки лишались би в localStorage назавжди.
      try {
        if (o.id) {
          localStorage.removeItem('funa.stats.' + o.id);
          localStorage.removeItem('funa.ts.' + o.id);
        }
        const pfx = 'funa.draft.' + (o.id || '') + '.';
        if (o.id) {
          for (let i = localStorage.length - 1; i >= 0; i--) {
            const k = localStorage.key(i);
            if (k && k.indexOf(pfx) === 0) localStorage.removeItem(k);
          }
        }
      } catch (e) {}
      break;
    }
    case 'account_switched': {
      S.signedIn = !!o.signedIn;
      if (o.activeId) S.activeId = o.activeId;
      S.session = null; S.parent = 0; S.first = true; S.folder = 'all';
      S.reqLive = false; S.reqBaseOut = 0;
      S.cur = null; S.pendingUser = null; S.editing = null;
      // всё, что относится к старому аккаунту, не должно уехать в новый:
      // вложения с чужими file_id, конспект контекста и субагенты
      S.att = [];
      S.accounts = [];
      S.compact = '';
      S.compactBusy = false;
      S.ctxFrom = 0;
      S.subs = {};
      S.pendingSend = null;
      S.pendingResume = false;
      S.regenBackup = null; S.editBackup = null; S.pendingMsg = null;
      S.editTarget = null; S.regenPending = null;
      // Списки прошлого аккаунта нельзя оставлять в DOM: при блокировке
      // новый аккаунт их не перезапросит, и чужие чаты/память/папки
      // остались бы видны в шторке под оверлеем.
      S.sessions = []; S.q = '';
      S.folders = []; S.fmap = {};
      S.mem = []; S.mq = '';
      S.sheetId = null; S.msgTarget = null; S.fileTarget = null;
      S.view = null; S.viewSeq = (S.viewSeq || 0) + 1;
      S.artName = ''; S.pages = [];
      // Экран «Сохранённые страницы» — это данные аккаунта: закрываем его
      // и чистим строки, иначе старые имена страниц остались бы в DOM.
      if ($('pages').classList.contains('on')) {
        $('pages').classList.remove('on');
        S.pagesOpen = false;
      }
      renderPages();
      const keepInput = $('input');
      if (keepInput) keepInput.value = '';
      // поля поиска тоже помнят строку прошлого аккаунта
      if ($('sSearch')) $('sSearch').value = '';
      if ($('mSearch')) $('mSearch').value = '';
      clearAtt();
      clearMessages();
      closeDrawer();
      // открытые просмотрщики могли показывать файл прошлого чата
      if ($('viewer').classList.contains('on')) closeViewer();
      if ($('fileSheet').classList.contains('on')) closeFileSheet();
      if ($('artFull').classList.contains('on')) {
        $('artFull').classList.remove('on');
        $('artFullFrame').srcdoc = '';
      }
      if ($('ctxSheet').classList.contains('on')) closeCtx();
      if ($('msgSheet').classList.contains('on')) $('msgSheet').classList.remove('on');
      renderSessions();
      renderFolders();
      renderMem();
      renderStats();
      if ($('folderSheet').classList.contains('on')) renderFolderPick();
      if ($('memory').classList.contains('on')) closeMemory();
      updateCtxChip();
      setTitle('FunaChat');
      if ($('subSheet').classList.contains('on')) closeSub();
      // Переключение на аккаунт со своим паролем: показать экран
      // разблокировки, иначе данные не загружены, а пароль никто не спросил.
      if (o.locked) {
        closeSettings();
        closeDrawer();
        openAuth('unlock', true);
      }
      api({ cmd: 'accounts' });
      break;
    }
    case 'folders': {
      S.folders = o.items || [];
      S.fmap = o.map || {};
      renderFolders();
      renderSessions();
      if ($('folderSheet').classList.contains('on')) renderFolderPick();
      break;
    }
    case 'file': {
      let f = S.att.find(x => x.local === o.local);
      if (!f) {
        // больше 40 сервер всё равно не примет — не держим их в UI
        if (S.att.length >= 40) { toast(t('fileLimit')); break; }
        f = { local: o.local };
        S.att.push(f);
      }
      f.status = o.status || f.status || 'uploading';
      if (o.name) f.name = o.name;
      if (o.size != null) f.size = o.size;
      if (o.mime) f.mime = o.mime;
      if (o.isImage != null) f.isImage = !!o.isImage;
      if (o.thumb) f.thumb = o.thumb;
      if (o.progress != null) f.progress = o.progress;
      if (o.msg) f.msg = o.msg;
      if (o.id) f.id = o.id;
      renderAtt();
      if (o.status === 'error') toast((o.name || 'file') + ': ' + (o.msg || t('fileError')));
      break;
    }
    case 'file_thumb': {
      const path = o.path || '';
      if (o.data) {
        thumbPut(path, o.data);
        document.querySelectorAll('img.aimg[data-fpath]').forEach(img => {
          if (img.getAttribute('data-fpath') === path) img.src = o.data;
        });
      } else {
        document.querySelectorAll('.imglink[data-fpath]').forEach(box => {
          if (box.getAttribute('data-fpath') === path) box.classList.add('broken');
        });
      }
      THUMB_PEND[path] = false;
      break;
    }
    case 'file_info': {
      if (o.path) updateCards(o.path, o);
      if (S.fileTarget && S.fileTarget.path === o.path) {
        $('fileSheetTitle').textContent = o.name || S.fileTarget.name;
        $('fileSheetMeta').textContent = o.exists === false ? t('fileMissing') :
          [o.kind || '', o.size != null ? fmtSize(o.size) : '', o.mime || ''].filter(Boolean).join(' · ');
        $('filePreview').innerHTML = o.thumb ? '<img src="' + o.thumb + '" alt="">' : '';
      }
      INFO_PEND[o.path] = false;
      break;
    }
    case 'view': {
      if (!S.view) break;
      if (o.seq && S.view.seq && o.seq !== S.view.seq) break;
      renderViewer(o);
      break;
    }
    case 'pdf_pages': {
      if (!S.view) break;
      // ответ мог прийти уже после открытия другого PDF
      if (o.seq && S.view.seq && o.seq !== S.view.seq) break;
      if (o.path && S.view.path && o.path !== S.view.path) break;
      S.view.pages = S.view.pages.concat(o.pages || []);
      if (o.total) S.view.total = o.total;
      paintPdfPages();
      break;
    }
    case 'view_error': {
      if (o.seq && S.view && S.view.seq && o.seq !== S.view.seq) break;
      toast(o.msg || t('fileMissing'));
      if (S.view) $('viewerBody').innerHTML = '<div class="vload">' + esc(o.msg || t('fileMissing')) + '</div>';
      break;
    }
    case 'file_saved': {
      toast(t('savedTo') + ' ' + (o.path || ''));
      break;
    }
    case 'compacted': {
      // Конспект прийшов для чату, який уже закрили: інакше його текст
      // приклеївся б до зовсім іншої розмови.
      if ((o.sid || '') !== (S.session || '')) { S.compactBusy = false; setHint(''); break; }
      S.compact = o.summary || '';
      S.compactBusy = false;
      setHint('');
      addDivider(t('compacted'));
      S.ctxFrom = S.msgs.length;
      // флаг ставим до запиту: відповідь «new» може прийти раніше, ніж
      // ми встигнемо його виставити, і тоді відкладене повідомлення загубиться
      if (S.pendingSend) S.pendingResume = true;
      api({ cmd: 'new', keep: true });
      toast(t('compacted'));
      break;
    }
    case 'compact_error': {
      S.compactBusy = false;
      setHint('');
      if ((o.sid || '') !== (S.session || '')) break;
      toast(t('compactError') + (o.msg ? ': ' + o.msg : ''));
      if (S.pendingSend) {
        const p = S.pendingSend;
        S.pendingSend = null;
        if (p && p.text) {
          const box = $('input');
          box.value = p.text;
          box.dispatchEvent(new Event('input'));
        }
        submit(true);
      }
      break;
    }
    case 'saved_file': {
      toast(t('savedTo') + ' ' + (o.path || ''));
      break;
    }
    case 'chat_exported': {
      toast(t('chatExported') + ' · ' + (o.path || ''));
      break;
    }
    case 'pages': {
      S.pages = o.items || [];
      renderPages();
      break;
    }
    case 'page': {
      S.artName = (o.name || 'page').replace(/\.html$/i, '');
      // Ссылки внутри сохранённой страницы тоже должны открываться в
      // браузере: iframe без allow-same-origin сам этого не умеет.
      $('artFullFrame').srcdoc = withLinkHook(o.content || '');
      $('artFull').classList.add('on');
      break;
    }
    case 'title': if (o.text) setTitle(o.text); break;
    case 'busy': {
      // UI блокує поле ще до відповіді нативу, тому S.busy тут уже true.
      // Окремий прапорець не дає порахувати один запит двічі, якщо натив
      // повторить подію, і водночас рахує запити, стартовані з нативу.
      if (o.on) {
        if (!S.reqLive) {
          S.reqLive = true;
          S.reqBaseOut = totalOutTokens();
          statsAdd({ q: 1, tin: ctxStats().tokens });
        }
      } else {
        S.reqLive = false;
      }
      setBusy(!!o.on);
      if (!o.on) {
        // Regen не стартував (помилка/стоп до першої дельти) — повертаємо
        // стару відповідь на місце.
        if (S.regenBackup) restoreRegen();
        if (S.editBackup) restoreEdit();
        if (S.pendingMsg) restoreSend();
        // Продовження не встигло початися (помилка/стоп до a_start):
        // повертаємо кнопку «Продовжити» і знімаємо курсор стріму.
        if (S.contTarget) {
          const cm = S.contTarget;
          cm.streaming = false;
          if (cm.status !== 'FINISHED') cm.status = 'STOPPED';
          verSync(cm, true);
          paintAssistant(cm);
          paintMeta(cm);
        }
        if (S.cur) {
          S.cur.streaming = false;
          S.cur.thinkOpen = false;
          S.cur._adding = false;
          // Обрыв без a_end (мережа/ліміт): не лишаємо «в роботі».
          if (S.cur.status === 'WIP') S.cur.status = 'STOPPED';
          verSync(S.cur, true);
          paintAssistant(S.cur);
          paintMeta(S.cur);
        }
        S.cur = null; S.pendingUser = null; S.contTarget = null;
        S.editTarget = null;
        // Кнопки версій ховаються під час запиту — повертаємо їх лише тим
        // повідомленням, у яких ці кнопки є.
        for (const mm of S.msgs) {
          if (mm.vers && mm.vers.length > 1) paintMeta(mm);
        }
        if (S.find) runFind(S.find);
        if (S.session) api({ cmd: 'sessions' });
        // Черга: після природного завершення одразу беремо наступне
        // повідомлення. Явний «Стоп» ставить чергу на паузу.
        const stopped = !!S.stopByUser;
        S.stopByUser = false;
        if (stopped && S.queue.length) S.queueHold = true;
        renderQueue();
        if (!stopped) pumpQueue();
      }
      break;
    }
    case 'error': {
      if (S.busy) statsAdd({ err: 1 });
      // Помилка (401/429/мережа) не має автоматично ганяти решту черги.
      if (S.queue.length) { S.queueHold = true; renderQueue(); }
      S.editTarget = null;
      const emsg = o.msg || t('error');
      toast(emsg);
      let b = $('msgs').querySelector('.banner');
      if (!b) { b = el('div', 'banner'); $('msgs').prepend(b); }
      b.textContent = '';
      const bt = el('span', 'bannertxt');
      bt.textContent = emsg;
      b.appendChild(bt);
      if (o.expired || o.verify) {
        const btn = el('button', 'bannerbtn');
        btn.textContent = t(o.expired ? 'refreshSite' : 'verifyAccount');
        btn.onclick = () => {
          b.classList.remove('on');
          b.textContent = '';
          closeSettings();
          closeDocs();
          closeDrawer();
          // Перевірка hCaptcha: сайт відкривається на чаті, а не на вході,
          // щоб користувач одразу надіслав повідомлення й пройшов капчу.
          if (o.verify) api({ cmd: 'auth_verify' });
          else openAuth('ds');
        };
        b.appendChild(document.createElement('br'));
        b.appendChild(btn);
      }
      b.classList.add('on');
      if (S.cur && !S.cur.text) { S.cur.status = 'ERROR'; S.cur.streaming = false; paintAssistant(S.cur); }
      break;
    }
    case 'storage': S.storage = !!o.ok; syncSettings(); break;
    case 'guard': {
      S.guardOn = o.guardOn !== false; S.guardGap = o.guardGap || 0;
      S.guardPerMin = o.guardPerMin || 12; S.guardPerDay = o.guardPerDay || 400;
      S.guardCount = o.guardCount || 0; S.guardCool = o.guardCool || 0;
      S.guardCoolAt = Date.now() + S.guardCool;
      S.guardNote = o.guardNote || '';
      syncGuard();
      break;
    }
    case 'api': {
      S.apiRunning = !!o.running; S.apiPort = o.port || S.apiPort || 8787;
      S.apiKey = o.key || S.apiKey; S.apiAuto = !!o.auto;
      S.apiLan = !!o.lan; S.apiDel = o.del !== false;
      if (o.code) S.code = +o.code;
      if (o.pub != null) S.apiPublic = !!o.pub;
      if (o.auditMask != null) S.auditMask = o.auditMask;
      if (o.auditContact != null) S.auditContact = o.auditContact;
      if (o.auditHook != null) S.auditHook = o.auditHook;
      if (o.pubUrl != null) S.pubUrl = o.pubUrl;
      if (o.updUrl != null) S.updUrl = o.updUrl;
      syncApi();
      if (o.err) toast(o.err);
      break;
    }
    case 'keys': {
      S.keys = o.items || [];
      renderKeys();
      break;
    }
    case 'telemetry': {
      if (o.on != null) S.telemetryOn = !!o.on;
      if (o.last != null) S.telemetryLast = +o.last || 0;
      if (o.pending != null) S.telemetryPending = +o.pending || 0;
      if (o.sent === true) toast(t('teleSent'));
      if (o.preview) {
        $('teleText').textContent = o.text || '';
        $('teleSheet').classList.add('on');
      }
      renderTelemetry();
      break;
    }
    case 'audit_log': {
      S.auditLog = o.items || [];
      renderAuditLog();
      break;
    }
    case 'key_new': {
      $('akForm').hidden = true;
      $('akDone').hidden = false;
      $('akValue').value = o.key || '';
      $('apiKeySheet').classList.add('on');
      break;
    }
    case 'deleted': {
      S.sessions = S.sessions.filter(x => x.id !== o.id);
      renderSessions();
      if (o.id === S.session) { S.session = null; clearMessages(); }
      break;
    }
    case 'renamed': {
      const s = S.sessions.find(x => x.id === o.id);
      if (s) s.title = o.title;
      renderSessions();
      break;
    }
    case 'saved': {
      if (o.base != null) { S.base = o.base; S.baseAuto = !!o.baseAuto; }
      if (o.lang && o.lang !== LANG) { LANG = o.lang; S.lang = LANG; applyI18n(); }
      else { syncSettings(); }
      break;
    }
    case 'mem': S.mem = o.items || []; renderMem(); break;
    case 'mem_saved':
      if (o.ok === false) toast(o.msg || t('memFail'));
      else toast(t('cardSaved'));
      break;
    case 'mem_deleted': break;
    case 'pinned': {
      const ps = S.sessions.find(x => x.id === o.id);
      if (ps) ps.pinned = !!o.on;
      renderSessions();
      break;
    }
    case 'mem_exported': toast(t('exported') + ' · ' + (o.count || 0)); break;
    case 'mem_imported': toast(t('imported') + ' · ' + (o.count || 0)); break;
    case 'mem_toggled': {
      const c = S.mem.find(x => x.id === o.id);
      if (c) { c.enabled = !!o.on; renderMem(); }
      break;
    }
    case 'back': handleBack(); break;
    default: break;
  }
};

function setTitle(txt) {
  S.title = txt || '';
  $('htitle').textContent = txt || 'FunaChat';
  $('hsub').textContent = txt ? '' : 'DeepSeek';
  const s = S.sessions.find(x => x.id === S.session);
  if (s && txt) s.title = txt;
}

function handleBack() {
  // підтвердження для нативної сторони: якщо міст мертвий, вона закриє
  // застосунок сама, а не чекатиме на відповідь, якої не буде
  api({ cmd: 'back_ack' });
  if ($('intro').classList.contains('on')) { closeIntro(true); return; }
  if ($('updates').classList.contains('on')) { closeUpdates(); return; }
  if ($('docs').classList.contains('on')) { closeDocs(); return; }
  if ($('subSheet').classList.contains('on')) { closeSub(); return; }
  if ($('queueSheet').classList.contains('on')) {
    $('queueSheet').classList.remove('on'); S.queueEdit = null; return;
  }
  if ($('findBar').classList.contains('on')) { closeFind(); return; }
  if ($('nameSheet').classList.contains('on')) { closeNameSheet(); return; }
  if ($('artFull').classList.contains('on')) { $('artFull').classList.remove('on'); $('artFullFrame').srcdoc = ''; return; }
  if ($('pickSheet').classList.contains('on')) { closePick(); return; }
  if ($('ctxSheet').classList.contains('on')) { closeCtx(); return; }
  if ($('viewer').classList.contains('on')) { closeViewer(); return; }
  if ($('fileSheet').classList.contains('on')) { closeFileSheet(); return; }
  if ($('msgSheet').classList.contains('on')) { $('msgSheet').classList.remove('on'); S.msgTarget = null; return; }
  if ($('folderSheet').classList.contains('on')) { $('folderSheet').classList.remove('on'); return; }
  if ($('keySheet').classList.contains('on')) { closeKeySheet(); return; }
  if ($('lockSheet').classList.contains('on')) { closeLockSheet(); return; }
  if ($('cardSheet').classList.contains('on')) { closeCard(); return; }
  if ($('sheet').classList.contains('on')) { $('sheet').classList.remove('on'); return; }
  if ($('pages').classList.contains('on')) { $('pages').classList.remove('on'); return; }
  if ($('repSheet').classList.contains('on')) { closeReport(); return; }
  if ($('help').classList.contains('on')) { closeHelp(); return; }
  if ($('memory').classList.contains('on')) { closeMemory(); return; }
  if ($('settings').classList.contains('on')) { settingsBackOrClose(); return; }
  if ($('drawer').classList.contains('on')) { closeDrawer(); return; }
  if ($('auth').classList.contains('on')) {
    if (S.authForce) { api({ cmd: 'exit' }); return; }
    api({ cmd: 'auth_skip' });
    return;
  }
  api({ cmd: 'exit' });
}

/* ---------------- single-choice pickers ---------------- */

let pickTarget = null;

function enhanceSelect(sel) {
  if (sel._picked) return;
  sel._picked = true;
  sel.classList.add('nativehide');
  const btn = el('button', 'pickerfield');
  btn.type = 'button';
  btn.innerHTML = '<span class="pv"></span>' + ic('i-chevron', 'chev');
  sel.parentNode.insertBefore(btn, sel.nextSibling);
  const label = () => {
    const o = sel.options[sel.selectedIndex];
    btn.querySelector('.pv').textContent = o ? o.textContent : '';
  };
  label();
  btn.onclick = () => openPick(sel);
  sel._btn = btn;
  sel._label = label;
  sel.addEventListener('change', label);
}

function enhanceSelects(root) {
  const scope = root || document;
  scope.querySelectorAll('select.field').forEach(enhanceSelect);
}

function openPick(sel) {
  pickTarget = sel;
  const sect = sel.closest('.sect');
  const lab = sect ? sect.querySelector('.slabel') : null;
  $('pickTitle').textContent = lab ? lab.textContent : (sel.getAttribute('aria-label') || '');
  const box = $('pickList');
  box.innerHTML = '';
  Array.prototype.forEach.call(sel.options, (o, i) => {
    const b = el('button', 'pickopt' + (i === sel.selectedIndex ? ' on' : ''));
    b.type = 'button';
    b.innerHTML = '<span class="po">' + esc(o.textContent) + '</span>' + ic('i-check');
    b.onclick = () => {
      sel.selectedIndex = i;
      sel.dispatchEvent(new Event('change', { bubbles: true }));
      if (sel._label) sel._label();
      if (sel.id === 'fModel') S.model = sel.value;
      if (sel.id === 'fCtx') S.ctxLimit = +sel.value || 1000000;
      closePick();
    };
    box.appendChild(b);
  });
  $('pickSheet').classList.add('on');
}

function closePick() {
  $('pickSheet').classList.remove('on');
  pickTarget = null;
}

/* ---------------- context info ---------------- */

function fmtNum(n) {
  return String(Math.max(0, Math.round(n || 0))).replace(/\B(?=(\d{3})+(?!\d))/g, ' ');
}

function fmtTok(n) {
  n = Math.max(0, n || 0);
  if (n < 1000) return String(Math.round(n));
  if (n < 10000) return (n / 1000).toFixed(1) + 'k';
  return Math.round(n / 1000) + 'k';
}

function ctxStats() {
  const from = Math.max(0, S.ctxFrom || 0);
  let chars = 0, think = 0, count = 0;
  for (let i = from; i < S.msgs.length; i++) {
    const m = S.msgs[i];
    if (m.role !== 'user' && m.role !== 'assistant') continue;
    count++;
    chars += (m.text || '').length;
    think += (m.think || '').length;
  }
  return { tokens: Math.ceil((chars + think) / 3.2), chars, think, count };
}

let ctxTickAt = 0;

function updateCtxChip(force) {
  const lab = $('ctxLabel');
  if (!lab) return;
  const now = Date.now();
  if (!force && now - ctxTickAt < 350) return;
  ctxTickAt = now;
  const s = ctxStats();
  lab.textContent = '~' + fmtTok(s.tokens);
  const chip = $('chipCtx');
  const ratio = s.tokens / ctxLimit();
  chip.classList.toggle('warn', ratio > 0.8 && ratio <= 1);
  chip.classList.toggle('bad', ratio > 1);
  chip.title = '~' + fmtNum(s.tokens) + ' / ' + fmtNum(ctxLimit());
  if ($('ctxSheet').classList.contains('on')) fillCtx(s);
}

function fillCtx(s) {
  $('ctxUsed').textContent = '~' + fmtNum(s.tokens) + ' / ' + fmtNum(ctxLimit());
  $('ctxMsgs').textContent = fmtNum(s.count);
  $('ctxThink').textContent = fmtNum(s.think);
  $('ctxChars').textContent = fmtNum(s.chars + s.think);
  const ratio = Math.max(0, Math.min(1, s.tokens / ctxLimit()));
  const bar = $('ctxBar');
  bar.style.width = (ratio * 100).toFixed(1) + '%';
  bar.className = ratio > 1 ? 'bad' : (ratio > 0.8 ? 'warn' : '');
}

function openCtx() {
  fillCtx(ctxStats());
  $('ctxSheet').classList.add('on');
}

function closeCtx() {
  $('ctxSheet').classList.remove('on');
}

/* ---------------- DeepSeek accounts ---------------- */

function accountTitle(a) {
  if (!a) return 'DeepSeek';
  if (a.name) return a.name;
  if (a.email) return a.email;
  if (a.phone) return a.phone;
  if (a.uid) return 'ID ' + a.uid;
  return 'DeepSeek';
}

function accountSub(a) {
  if (!a) return '';
  const title = accountTitle(a);
  if (a.email && a.email !== title) return a.email;
  if (a.phone && a.phone !== title) return a.phone;
  return '';
}

function accountLetter(a) {
  const t0 = (accountTitle(a) || 'D').trim();
  return (t0.charAt(0) || 'D').toUpperCase();
}

function activeAccount() {
  const items = S.accounts || [];
  for (const a of items) if (a.active) return a;
  return null;
}

function renderAccounts() {
  const box = $('accList');
  if (!box) return;
  const items = S.accounts || [];
  box.innerHTML = '';
  if (!items.length) {
    const e = el('div', 'accempty');
    e.textContent = t('accEmpty');
    box.appendChild(e);
    return;
  }
  for (const a of items) {
    const bad = a.status === 'expired' || a.status === 'challenge';
    const row = el('div', 'accrow' + (a.active ? ' on' : '') + (bad ? ' bad' : ''));
    const nm = accountTitle(a);
    const sub = accountSub(a);
    row.innerHTML = '<div class="pavatar">' + esc(accountLetter(a)) + '</div>' +
      '<div class="accmeta"><b>' + esc(nm) + '</b>' +
      (sub ? '<small>' + esc(sub) + '</small>' : '') +
      (a.status === 'expired' ? '<em>' + esc(t('accExpiredHint')) + '</em>' : '') +
      (a.status === 'challenge' ? '<em>' + esc(t('accChallengeHint')) + '</em>' : '') +
      '</div>' +
      (a.active ? '<span class="accchip on">' + esc(t('accActive')) + '</span>' : '') +
      '<button class="accact" data-check="1" aria-label="check">' + ic('i-check') + '</button>' +
      '<button class="accact danger" data-del="1" aria-label="delete">' + ic('i-trash') + '</button>';
    row.onclick = (ev) => {
      if (ev.target.closest('[data-check]')) {
        ev.stopPropagation();
        toast(t('checking'));
        api({ cmd: 'acc_check', id: a.id });
        return;
      }
      if (ev.target.closest('[data-del]')) {
        ev.stopPropagation();
        if (confirm(t('accDeleteAsk') + '\n' + nm)) { vibe(12); api({ cmd: 'acc_del', id: a.id }); }
        return;
      }
      if (a.active) {
        if (a.status === 'expired') { closeSettings(); openAuth('ds'); }
        else if (a.status === 'challenge') { closeSettings(); api({ cmd: 'auth_verify' }); }
        return;
      }
      vibe(8);
      // Чернетка належить поточному акаунту: зберігаємо її до перемикання.
      flushDraft();
      api({ cmd: 'acc_use', id: a.id });
    };
    box.appendChild(row);
  }
}

function renderProfile() {
  const card = $('profCard');
  if (!card) return;
  const a = activeAccount();
  const st = S.auth || {};
  if (!a && !S.signedIn) {
    card.innerHTML = '<div class="pavatar dim">D</div><div class="pmeta"><b>' +
      esc(t('authNoProfile')) + '</b><small>' + esc(t('authNoProfileHint')) + '</small></div>';
  } else {
    const nm = (a && accountTitle(a)) || st.name || st.email || st.phone || 'DeepSeek';
    const sub = (a && accountSub(a)) || st.email || st.phone || '';
    const chip = a && (a.status === 'expired' || a.status === 'challenge')
      ? '<span class="accchip bad">' + esc(t(a.status === 'challenge' ? 'accChallenge' : 'accExpired')) + '</span>'
      : '<span class="accchip on">' + esc(t('accActive')) + '</span>';
    card.innerHTML = '<div class="pavatar">' + esc(accountLetter(a)) + '</div>' +
      '<div class="pmeta"><b>' + esc(nm) + '</b>' +
      (sub ? '<small>' + esc(sub) + '</small>' : '') + '</div>' + chip;
  }
  const has = !!a || !!S.signedIn;
  $('profCheck').disabled = !has;
  $('fAuthLock').checked = !!st.lock;
  $('fAuthLock').disabled = !has;
  $('fAuthLock').title = has ? (st.hasPass ? '' : t('authNoPass')) : '';
}

function openLockSheet(err) {
  $('lockMsg').textContent = err || '';
  $('lockPass').value = '';
  $('lockSave').disabled = false;
  $('lockSheet').classList.add('on');
  setTimeout(() => { try { $('lockPass').focus(); } catch (e) {} }, 120);
}

function closeLockSheet() {
  $('lockSheet').classList.remove('on');
}

function openAuth(mode, force) {
  S.authMode = mode === 'unlock' ? 'unlock' : 'ds';
  S.authForce = !!force;
  const unlock = S.authMode === 'unlock';
  $('auth').classList.add('on');
  $('authLogin').style.display = unlock ? 'none' : '';
  $('authUnlock').style.display = unlock ? '' : 'none';
  $('authHead').textContent = t(unlock ? 'authUnlockTitle' : 'dsLoginTitle');
  $('authSub').textContent = t(unlock ? 'authUnlockSub' : 'dsSub');
  $('authBack').style.visibility = force ? 'hidden' : 'visible';
  $('authSkip').style.display = force ? 'none' : '';
  $('loginMsg').textContent = '';
  $('unlockMsg').textContent = '';
  $('unlockBtn').disabled = false;
  $('uPass').value = '';
}

function whoamiSoon() {
  api({ cmd: 'whoami' });
  setTimeout(() => { if (S.signedIn) api({ cmd: 'whoami' }); }, 1200);
}

function closeAuth() {
  $('auth').classList.remove('on');
  S.authForce = false;
}

function openKeySheet() {
  $('keyValue').value = '';
  $('keySheet').classList.add('on');
  setTimeout(() => { try { $('keyValue').focus(); } catch (e) {} }, 120);
}

function closeKeySheet() {
  $('keySheet').classList.remove('on');
}

function saveKey() {
  const v = $('keyValue').value.trim();
  if (!v) return;
  closeKeySheet();
  $('loginMsg').textContent = t('authChecking');
  api({ cmd: 'auth_key', key: v });
}

function submitUnlock() {
  const pass = $('uPass').value;
  if (!pass) { $('unlockMsg').textContent = t('authNeedPass'); return; }
  $('unlockBtn').disabled = true;
  api({ cmd: 'auth_unlock', password: pass });
}

/* ---------------- help, diagnostics, reports ---------------- */

const DOCS = {"uk": [{"t": "Швидкий старт", "p": "П'ять кроків від установки до першого чату.", "li": ["1. На екрані входу натисни «Увійти через сайт DeepSeek» або встав «Ключ сесії».", "2. Дозволь «Керування всіма файлами» — інакше доступні лише Download/FunaFiles.", "3. Налаштування → «Акаунт і безпека» → «Перевірити» — має бути статус «активний».", "4. Напиши перше повідомлення у полі внизу.", "5. Увімкни блокування застосунку в Налаштуваннях, якщо телефон не завжди під рукою."], "a": {"act": "login", "label": "Увійти"}}, {"t": "Вхід і акаунти", "p": "Ідентичність застосунку — активний акаунт DeepSeek. Локальних профілів більше немає.", "li": ["Основний шлях — «Увійти через сайт DeepSeek»: пароль, капча й код обробляються на сайті, у застосунок потрапляє лише токен сесії.", "«Ключ сесії» — вставити готовий Bearer-токен або рядок cookie від chat.deepseek.com.", "«Імпорт session.json» — старий формат експорту (токен + uid + контакти).", "Кілька акаунтів: Налаштування → «Акаунт і безпека» → «Додати акаунт»; перемикання змінює, від чийого імені йдуть запити.", "Пам'ять, папки й сесії ізольовані за акаунтом: різні акаунти не бачать дані один одного."], "a": {"act": "login", "label": "До входу"}}, {"t": "Чат: основи", "p": "Звичайний чат із Markdown, таблицями, кодом і формулами KaTeX — усе рендериться офлайн.", "li": ["Enter у полі введення — надіслати (Shift+Enter — новий рядок).", "Під час відповіді Enter або кнопка надсилання ставлять повідомлення в чергу. Чергу можна переставляти, редагувати й чистити; після відповіді вона запускається сама, а «Стоп» ставить її на паузу.", "Кнопка «Стоп» зупиняє генерацію, вже отриманий текст лишається.", "Повідомлення ШІ не редагуються — історія завжди відповідає тому, що реально прийшло.", "Тап по повідомленню ШІ або кнопка ⋯ — копіювати, перегенерувати чи продовжити; тап по своєму — редагувати.", "Кнопка внизу справа повертає до останнього повідомлення; якщо прокрутити чат угору під час відповіді, автопрокрутка вимикається й текст більше не смикає екран."], "a": {"act": "newchat", "label": "Новий чат"}}, {"t": "Час, версії та статистика", "p": "Під кожним повідомленням видно дату, а біля відповіді ШІ — скільки часу він працював. Редаговані повідомлення й перегенерації мають перемикач версій.", "li": ["Час показується за годинником телефона: сьогодні — лише години й хвилини, давніші — дата.", "Час біля відповіді — тривалість цього раунду; повний час запиту з інструментами видно в статистиці.", "Змінив перше повідомлення? Під ним зʼявиться ‹ 1/2 › — повернення до старої версії відновлює і хвіст чату, що йшов під нею.", "Те саме для «Згенерувати ще раз»: кожна регенерація стає окремою версією.", "Версії зберігаються на сервері DeepSeek і відновлюються після перезапуску (для останніх 16 повідомлень).", "Налаштування → «Статистика ШІ»: запити, відповіді, зупинки, загальний і середній час, оцінка токенів, правки, регенерації, інструменти й помилки.", "Статистика рахується для активного акаунта; «Скинути» очищає лише її."]}, {"t": "Файли й вкладення", "p": "Скріпка в полі введення додає файл до повідомлення.", "li": ["Обмеження на один файл — 50 МБ; великі зображення стискаються перед відправкою.", "Підтримуються тексти, PDF, DOCX, таблиці, зображення; вміст читається й передається моделі.", "Робоча тека інструментів — /sdcard/Download/FunaFiles.", "Експорт документів і пам'яті — /sdcard/Download/FunaFiles/exports.", "Файли поза Download доступні лише з дозволом «Керування всіма файлами»."]}, {"t": "Інструменти моделі", "p": "Модель може працювати з файлами й командами, а не лише відповідати текстом.", "li": ["read — прочитати файл; write/append — створити або дописати; edit — точкова заміна.", "list/ls — вміст теки; sh/bash/termux — команда через міст термінала (Termux/ReTerminal).", "doc/document — створити PDF або DOCX із заголовком і текстом.", "Вбудовані: time, calc, convert, random, base64, hash, skill — працюють без мережі; skill віддає інструкції docx/pdf/design/docs.", "Кожен виклик показується окремою карткою; її можна згорнути або розгорнути."], "a": {"act": "help", "label": "Діагностика"}}, {"t": "funa-bridge: як підключити", "p": "funa-bridge — невеликий скрипт у терміналі, який виконує команди FunaChat і повертає вивід. Працює у Termux і ReTerminal. Без нього доступні лише файлові інструменти.", "li": ["1. Постав термінал з F-Droid: Termux або ReTerminal. Версія Termux із Google Play застаріла й не працює з Android 12+.", "2. Відкрий цей розділ і натисни «Зберегти скрипт» — файл зʼявиться у /sdcard/Download/FunaFiles/funa-bridge.sh.", "3. Termux: termux-setup-storage, потім bash ~/storage/downloads/FunaFiles/funa-bridge.sh", "4. ReTerminal: sh /sdcard/Download/FunaFiles/funa-bridge.sh — termux-setup-storage там не потрібен.", "5. Залиш сесію термінала відкритою: у Termux додай termux-wake-lock, у ReTerminal вимкни для нього оптимізацію батареї.", "6. Перевірка: «Допомога й логи» → «Черга Termux доступна» (або ReTerminal), а в чаті — команда sh echo ok.", "Черга команд лежить у /sdcard/Download/FunaQueue — її мають бачити і FunaChat, і термінал.", "Якщо /sdcard не видно: у Termux повтори termux-setup-storage, у ReTerminal дозволь доступ до файлів у налаштуваннях Android.", "Скрипт v11 портативний: сам знаходить sh, обходиться без flock і timeout — працює і в Alpine, і в системній оболонці. Вивід команди публікується атомарно, тому фонові процеси не ламають маркер завершення."], "a": {"act": "bridge", "label": "Зберегти скрипт"}}, {"t": "Пам'ять", "p": "Картки знань додаються до системного промту в кожній новій сесії.", "li": ["Меню → «Пам'ять» → «+» → заголовок, текст, теги → «Зберегти».", "Картку можна вимкнути перемикачем, не видаляючи.", "У промт потрапляє до 3000 символів — найважливіше пиши коротко.", "Експорт/імпорт — JSON, зручно переносити між пристроями.", "Картки належать активному акаунту; старі без власника забирає перший акаунт."], "a": {"act": "memory", "label": "Відкрити пам'ять"}}, {"t": "Папки, пошук і сесії", "p": "Чатів може бути багато — їх можна групувати й шукати.", "li": ["Бокова шухляда: пошук по назві, фільтр «Всі / Без папки / папка».", "Довге натискання на папці — перейменувати; «+» — створити.", "Меню сесії (⋮) — закріпити, перейменувати, перемістити в папку, видалити.", "Закріплені сесії завжди зверху списку.", "Пошук всередині чату — кнопка лупи у верхній панелі."]}, {"t": "Субагенти", "p": "Для складних задач модель запускає помічників паралельно.", "li": ["Одночасно працює до 4 субагентів, глибина вкладеності — 2.", "Кожен субагент має власний блок із кроком і кнопкою зупинки.", "Результат повертається окремим повідомленням у чат.", "У локальному API субагент виконується синхронно в межах запиту."]}, {"t": "Артефакти, PDF і DOCX", "p": "Великі результати можна отримати як файл або інтерактивний артефакт.", "li": ["HTML-артефакт відкривається на весь екран і виконується офлайн.", "Артефакт працює в пісочниці: без доступу до файлів, буфера й містка застосунку.", "PDF і DOCX генеруються на телефоні без зовнішніх сервісів.", "Документ можна одразу поділитися або зберегти в Download/FunaFiles.", "Готові файли видно у файловому менеджері застосунку."]}, {"t": "Навички ШІ", "p": "Перед створенням документа модель читає вбудовану інструкцію — так PDF, DOCX і сторінки виходять охайними.", "li": ["Навички: docx (Word), pdf, design (HTML-артефакт) і docs (документація).", "У чаті достатньо попросити «зроби PDF» — агент сам викличе skill і дотримається правил.", "У локальному API є інструмент skill із тими самими інструкціями.", "Навички лежать в assets/skills/<назва>.<мова>.md і не потребують мережі.", "Якщо модель забула — повторіть прохання або згадайте назву навички."], "a": {"act": "newchat", "label": "До чату"}}, {"t": "Локальний API", "p": "OpenAI-сумісний сервер працює просто на телефоні — для інших застосунків і агентів.", "li": ["Адреса за замовчуванням: http://127.0.0.1:8787, ключ виду fk-… (Налаштування → «Мережа та API» → API key).", "Ендпоінти: GET /health, GET /v1/models, GET /v1/models/{id}, GET /v1/tools, POST /v1/chat/completions, POST /v1/completions. Ключ потрібен для всіх, навіть для /health.", "Приклад: curl -s http://127.0.0.1:8787/v1/chat/completions -H \"Authorization: Bearer fk-…\" -H \"Content-Type: application/json\" -d '{\"model\":\"default\",\"messages\":[{\"role\":\"user\",\"content\":\"Привіт\"}]}'", "Моделі: deepseek-chat, deepseek-reasoner, deepseek-expert, deepseek-search. Невідомий id мапиться на модель із налаштувань; thinking/search можна передати в тілі запиту.", "Для іншого пристрою в мережі увімкни LAN і відкрий порт у фаєрволі.", "Сервер поважає блокування акаунта: поки застосунок заблокований, /v1/* віддає 503, а /health — locked:true.", "Ліміти API: тіло до 8 МБ, 8 одночасних з’єднань, 120 запитів/хв з однієї IP, промт до 1 000 000 символів. Деталі — в API.md."], "a": {"act": "settings", "label": "Налаштувати API"}}, {"t": "Публічний доступ", "p": "API можна відкрити в інтернет через тунель — без відкритих портів і без картки.", "li": ["У Termux: cd ~/downloads/funachat/tunnel && ./start.sh — Cloudflare quick tunnel, резервний канал — zrok.", "Скрипт пише адресу в Download/FunaFiles/tunnel.txt, застосунок читає її кнопкою «З файлу» на сторінці API.", "Один origin: /v1/* — API, /updates/latest.json — маніфест оновлень, /updates/FunaChat.apk — підписаний APK.", "Ключ fk-… обовʼязковий; ліміт 120 запитів/хв на IP, за тунелем рахується справжній IP клієнта.", "Історія оновлень → «Перевірити оновлення» покаже нову версію й кнопку «Завантажити».", "Публічний API не виконує файлових інструментів, funa-bridge і ADB.", "Деталі та стабільний домен — tunnel/README.md."]}, {"t": "Контекст і стиснення", "p": "Довгі чати не обриваються — старі частини стискаються автоматично.", "li": ["Налаштування → «Чат і ШІ» → «Ліміт контексту»: 60K, 128K, 256K, 512K або 1M токенів; типовий — 1M (DeepSeek V4.1 Flash).", "У налаштуваннях видно, скільки місця зайнято.", "Стиснення зберігає суть і відкидає повтори, а не обрізає навмання.", "Новий чат завжди починається з чистим контекстом і картками пам'яті.", "Якщо модель «забула» деталь — повтори її або додай картку пам'яті."]}, {"t": "Безпека й приватність", "p": "Токени шифруються AES-256-GCM ключем Android Keystore.", "li": ["Пароль DeepSeek не зберігається і не передається застосунку — його обробляє сайт.", "Токени й cookie не потрапляють у логи та звіти.", "Блокування застосунку — окремий локальний пароль (PBKDF2); пароль DeepSeek для нього не потрібен.", "Пароль блокування належить акаунту: додавання іншого акаунта не вимикає захист першого.", "Розблокування теж окреме для кожного акаунта; холодний старт закриває всі сесії.", "Дані пам'яті, папок і сесій живуть лише на телефоні.", "Видалення акаунта прибирає його токен, пароль, памʼять, папки й чернетки."]}, {"t": "Ліміти й помилки DeepSeek", "p": "DeepSeek обмежує часті запити й підозрілі входи — це його захист, не баг застосунку.", "li": ["401 — токен прострочений: увійди через сайт знову.", "Коди 40303/40304 — DeepSeek просить підтвердити, що ти людина: відкрий сайт кнопкою «Пройти перевірку», надішли будь-яке повідомлення й пройди капчу, потім натисни «Імпортувати» й повтори запит.", "429 — занадто часто: зачекай, поки сервер відпустить ліміт.", "«Новий пристрій» / ризик — саме тому вхід іде через сайт, а не нативним запитом.", "Сайт може вимагати капчу або код із листа — це нормально.", "Акаунти без пароля (Google) підтримуються лише через сесію сайту."]}, {"t": "Діагностика й звіти", "p": "Якщо щось не працює — спершу глянь «Допомога й логи».", "li": ["Кожен пункт має стан: ✓ добре, ! увага, × проблема.", "Кнопка дії поруч веде одразу до потрібного налаштування.", "«Звіт» містить версію, пристрій, діагностику й останні 300 рядків логу.", "Логи можна переглянути перед надсиланням; токенів там немає.", "«Діагностичні дані» — знеособлений знімок раз на добу (типово увімкнений); відправляє Termux-релей у Telegram, токен у APK не входить."], "a": {"act": "help", "label": "Відкрити діагностику"}}, {"t": "Поради", "p": "Дрібниці, які економлять час.", "li": ["Формулюй задачу конкретно: мета, формат, обмеження.", "Проси файл («зроби PDF») — отримаєш готовий документ, а не простирадло тексту.", "Для повторюваних фактів — картка пам'яті, а не новий чат щоразу.", "Довгі дослідження краще розбивати на чати з окремими папками.", "Закріплюй важливі сесії, щоб не шукати їх у списку.", "Якщо відповідь обірвалась — «Продовж», модель пам'ятає контекст."]}], "en": [{"t": "Quick start", "p": "Five steps from install to the first chat.", "li": ["1. On the sign-in screen tap “Sign in via the DeepSeek site” or paste a “Session key”.", "2. Grant “All files access” — otherwise only Download/FunaFiles are available.", "3. Settings → “Account & security” → “Verify” — the status should be active.", "4. Type your first message in the composer.", "5. Turn on app lock in Settings if the phone is not always with you."], "a": {"act": "login", "label": "Sign in"}}, {"t": "Sign-in and accounts", "p": "The app's identity is the active DeepSeek account. Local profiles are gone.", "li": ["The main path is “Sign in via the DeepSeek site”: the site handles the password, captcha and code; only the session token reaches the app.", "“Session key” — paste a ready Bearer token or a cookie string from chat.deepseek.com.", "“Import session.json” — the old export format (token + uid + contacts).", "Several accounts: Settings → “Account & security” → “Add account”; switching changes whose identity requests use.", "Memory, folders and sessions are isolated per account: accounts never see each other's data."], "a": {"act": "login", "label": "Open sign-in"}}, {"t": "Chat basics", "p": "A normal chat with Markdown, tables, code and KaTeX formulas — all rendered offline.", "li": ["Enter in the composer sends (Shift+Enter inserts a newline).", "While the model is answering, Enter or the send button queues the message. Reorder, edit or remove queued messages; the queue starts automatically after an answer, and Stop pauses it.", "“Stop” halts generation; the text received so far stays.", "Assistant messages are never edited — history always matches what really arrived.", "Tap an AI message or its ⋯ button to copy, regenerate or continue; tap your own message to edit it.", "The bottom-right button jumps to the latest message; scrolling up while an answer is streaming turns auto-scroll off, so the text no longer pulls the screen down."], "a": {"act": "newchat", "label": "New chat"}}, {"t": "Time, versions and statistics", "p": "Every message shows its date, and an AI answer shows how long the model worked. Edited messages and regenerations get a version switcher.", "li": ["Time follows the phone clock: today shows hours and minutes only, older messages show the date.", "The time next to an answer is that round's duration; the full request time with tools is in the statistics.", "Edited an earlier message? A ‹ 1/2 › switcher appears under it — going back restores the chat tail that followed that version.", "The same applies to “Regenerate”: every regeneration becomes a separate version.", "Versions live on the DeepSeek server and come back after a restart (for the latest 16 messages).", "Settings → “AI usage”: requests, answers, stops, total and average time, token estimate, edits, regenerations, tool calls and errors.", "Statistics are kept per active account; “Reset” clears only them."]}, {"t": "Files and attachments", "p": "The paperclip in the composer attaches a file to the message.", "li": ["One file up to 50 MB; large images are compressed before sending.", "Texts, PDF, DOCX, spreadsheets and images are supported and passed to the model.", "The tools working folder is /sdcard/Download/FunaFiles.", "Document and memory exports go to /sdcard/Download/FunaFiles/exports.", "Files outside Download need the “All files access” permission."]}, {"t": "Model tools", "p": "The model can work with files and commands, not just answer with text.", "li": ["read — read a file; write/append — create or extend; edit — exact replacement.", "list/ls — folder contents; sh/bash/termux — run a command through the terminal bridge (Termux/ReTerminal).", "doc/document — build a PDF or DOCX from a title and text.", "Built-ins: time, calc, convert, random, base64, hash, skill — all offline; skill returns the docx/pdf/design/docs instructions.", "Every call appears as its own card that can be collapsed or expanded."], "a": {"act": "help", "label": "Diagnostics"}}, {"t": "funa-bridge: how to connect", "p": "funa-bridge is a small terminal script that runs FunaChat commands and returns their output. It works in Termux and ReTerminal. Without it only file tools work.", "li": ["1. Install a terminal from F-Droid: Termux or ReTerminal. The Google Play Termux build is outdated and does not work on Android 12+.", "2. Open this section and tap “Save script” — the file appears at /sdcard/Download/FunaFiles/funa-bridge.sh.", "3. Termux: termux-setup-storage, then bash ~/storage/downloads/FunaFiles/funa-bridge.sh", "4. ReTerminal: sh /sdcard/Download/FunaFiles/funa-bridge.sh — no termux-setup-storage needed there.", "5. Keep the terminal session open: run termux-wake-lock in Termux, and disable battery optimisation for ReTerminal.", "6. Check: “Help & logs” → “Termux queue is writable” (or ReTerminal), and in chat run sh echo ok.", "The command queue lives in /sdcard/Download/FunaQueue — both FunaChat and the terminal must see it.", "If /sdcard is not visible: rerun termux-setup-storage in Termux, or grant file access to ReTerminal in Android settings.", "Script v11 is portable: it finds sh itself and works without flock or timeout — in Alpine and in the system shell alike. Output is published atomically, so background processes cannot break the completion marker."], "a": {"act": "bridge", "label": "Save script"}}, {"t": "Memory", "p": "Knowledge cards are added to the system prompt in every new session.", "li": ["Menu → “Memory” → “+” → title, text, tags → Save.", "A card can be switched off without deleting it.", "Up to 3000 characters reach the prompt — keep the important parts short.", "Export/import is JSON, handy for moving between devices.", "Cards belong to the active account; ownerless old cards go to the first account."], "a": {"act": "memory", "label": "Open memory"}}, {"t": "Folders, search and sessions", "p": "Many chats can be grouped and searched.", "li": ["Side drawer: search by title, filter “All / Unfiled / folder”.", "Long-press a folder to rename; “+” creates one.", "Session menu (⋮) — pin, rename, move to a folder, delete.", "Pinned sessions always sit on top.", "Search inside a chat — the magnifier in the top bar."]}, {"t": "Subagents", "p": "For complex tasks the model runs helpers in parallel.", "li": ["Up to 4 subagents at once, nesting depth 2.", "Each subagent has its own block with progress and a stop button.", "The result arrives as a separate chat message.", "In the local API a subagent runs synchronously within the request."]}, {"t": "Artifacts, PDF and DOCX", "p": "Big results can come back as a file or an interactive artifact.", "li": ["An HTML artifact opens full screen and runs offline.", "Artifacts run sandboxed: no access to files, the clipboard or the app bridge.", "PDF and DOCX are generated on the phone with no external services.", "A document can be shared right away or saved to Download/FunaFiles.", "Finished files are visible in the app's file manager."]}, {"t": "AI skills", "p": "Before creating an artifact the model reads built-in instructions — that is why PDFs, DOCX files and pages come out clean.", "li": ["Skills: docx (Word), pdf, design (HTML artifact) and docs (documentation).", "In chat just ask for a PDF — the agent calls skill itself and follows the rules.", "The local API exposes a skill tool with the same instructions.", "Skills live in assets/skills/<name>.<lang>.md and need no network.", "If the model forgot, repeat the request or mention the skill name."], "a": {"act": "newchat", "label": "Open chat"}}, {"t": "Local API", "p": "An OpenAI-compatible server runs right on the phone — for other apps and agents.", "li": ["Default address: http://127.0.0.1:8787, key looks like fk-… (Settings → “Network & API” → API key).", "Endpoints: GET /health, GET /v1/models, GET /v1/models/{id}, GET /v1/tools, POST /v1/chat/completions, POST /v1/completions. The key is required for every one, including /health.", "Example: curl -s http://127.0.0.1:8787/v1/chat/completions -H \"Authorization: Bearer fk-…\" -H \"Content-Type: application/json\" -d '{\"model\":\"default\",\"messages\":[{\"role\":\"user\",\"content\":\"Hi\"}]}'", "Models: deepseek-chat, deepseek-reasoner, deepseek-expert, deepseek-search. An unknown id falls back to the model from Settings; thinking/search can be passed in the request body.", "For another device on the network enable LAN and open the port in the firewall.", "The server respects the account lock: while the app is locked, /v1/* returns 503 and /health reports locked:true.", "API limits: 8 MB body, 8 concurrent connections, 120 requests/min per IP, prompt up to 1,000,000 characters. Details in API.md."], "a": {"act": "settings", "label": "Configure API"}}, {"t": "Public access", "p": "The API can be exposed to the internet through a tunnel — no open ports, no card.", "li": ["In Termux: cd ~/downloads/funachat/tunnel && ./start.sh — Cloudflare quick tunnel, zrok as the backup channel.", "The script writes the URL to Download/FunaFiles/tunnel.txt; the app reads it with the From file button on the API page.", "One origin: /v1/* is the API, /updates/latest.json is the manifest, /updates/FunaChat.apk is the signed APK.", "The fk-… key is required; the limit is 120 requests/min per IP, and behind a tunnel the real client IP is used.", "Update log → Check for updates shows a new version and a Download button.", "The public API cannot run file tools, funa-bridge or ADB.", "Details and the stable domain — tunnel/README.md."]}, {"t": "Context and compaction", "p": "Long chats do not break — older parts are compacted automatically.", "li": ["Settings → “Chat & AI” → “Context limit”: 60K, 128K, 256K, 512K or 1M tokens; the default is 1M (DeepSeek V4.1 Flash).", "Settings show how much space is used.", "Compaction keeps the essence and drops repetition instead of cutting blindly.", "A new chat always starts with a clean context plus memory cards.", "If the model “forgot” a detail, repeat it or save it as a memory card."]}, {"t": "Security and privacy", "p": "Tokens are encrypted with AES-256-GCM backed by the Android Keystore.", "li": ["The DeepSeek password is never stored and never sent to the app — the site handles it.", "Tokens and cookies never reach the logs or reports.", "App lock uses its own local password (PBKDF2); no DeepSeek password is needed for it.", "The lock password belongs to the account: adding another account does not disable the first one's protection.", "Unlocking is per account too; a cold start closes every unlocked session.", "Memory, folders and sessions live only on the phone.", "Deleting an account removes its token, password, memory, folders and drafts."]}, {"t": "DeepSeek limits and errors", "p": "DeepSeek throttles frequent requests and suspicious sign-ins — that is its protection, not an app bug.", "li": ["401 — the token expired: sign in via the site again.", "Codes 40303/40304 — DeepSeek asks you to confirm you are human: open the site with the “Complete the check” button, send any message and pass the captcha, then tap “Import” and retry.", "429 — too frequent: wait until the server releases the limit.", "“New device” / risk — exactly why sign-in goes through the site, not a native request.", "The site may ask for a captcha or an email code — that is normal.", "Accounts without a password (Google) work only through a site session."]}, {"t": "Diagnostics and reports", "p": "If something breaks, open “Help & logs” first.", "li": ["Each item has a state: ✓ fine, ! attention, × problem.", "The action button next to it jumps straight to the right setting.", "A “Report” contains the version, device, diagnostics and the last 300 log lines.", "Logs can be previewed before sending; they contain no tokens.", "“Diagnostic data” is an anonymised daily snapshot (on by default); a Termux relay sends it to Telegram, the token is not in the APK."], "a": {"act": "help", "label": "Open diagnostics"}}, {"t": "Tips", "p": "Small things that save time.", "li": ["State the task concretely: goal, format, constraints.", "Ask for a file (“make a PDF”) and you get a document, not a wall of text.", "For repeating facts use a memory card instead of a new chat every time.", "Split long research into chats grouped in folders.", "Pin important sessions so you do not hunt for them.", "If an answer is cut off, say “continue” — the model keeps the context."]}], "ru": [{"t": "Быстрый старт", "p": "Пять шагов от установки до первого чата.", "li": ["1. На экране входа нажми «Войти через сайт DeepSeek» или вставь «Ключ сессии».", "2. Разреши «Управление всеми файлами» — иначе доступны только Download/FunaFiles.", "3. Настройки → «Аккаунт и безопасность» → «Проверить» — статус должен быть активным.", "4. Напиши первое сообщение в поле внизу.", "5. Включи блокировку приложения в Настройках, если телефон не всегда под рукой."], "a": {"act": "login", "label": "Войти"}}, {"t": "Вход и аккаунты", "p": "Идентичность приложения — активный аккаунт DeepSeek. Локальных профилей больше нет.", "li": ["Основной путь — «Войти через сайт DeepSeek»: пароль, капчу и код обрабатывает сайт, в приложение попадает только токен сессии.", "«Ключ сессии» — вставить готовый Bearer-токен или строку cookie от chat.deepseek.com.", "«Импорт session.json» — старый формат экспорта (токен + uid + контакты).", "Несколько аккаунтов: Настройки → «Аккаунт и безопасность» → «Добавить аккаунт»; переключение меняет, от чьего имени идут запросы.", "Память, папки и сессии изолированы по аккаунту: разные аккаунты не видят данные друг друга."], "a": {"act": "login", "label": "Ко входу"}}, {"t": "Чат: основы", "p": "Обычный чат с Markdown, таблицами, кодом и формулами KaTeX — всё рендерится офлайн.", "li": ["Enter в поле ввода — отправить (Shift+Enter — новая строка).", "Во время ответа Enter или кнопка отправки ставят сообщение в очередь. Очередь можно переставлять, редактировать и очищать; после ответа она запускается сама, а «Стоп» ставит её на паузу.", "Кнопка «Стоп» останавливает генерацию, полученный текст остаётся.", "Сообщения ИИ не редактируются — история всегда соответствует тому, что реально пришло.", "Тап по сообщению ИИ или кнопка ⋯ — копировать, перегенерировать или продолжить; тап по своему — редактировать.", "Кнопка справа внизу возвращает к последнему сообщению; если прокрутить чат вверх во время ответа, автопрокрутка выключается и текст больше не дёргает экран."], "a": {"act": "newchat", "label": "Новый чат"}}, {"t": "Время, версии и статистика", "p": "Под каждым сообщением видна дата, а у ответа ИИ — сколько времени он работал. У правок и перегенераций есть переключатель версий.", "li": ["Время по часам телефона: сегодня — только часы и минуты, у старых — дата.", "Время у ответа — длительность этого раунда; полное время запроса с инструментами видно в статистике.", "Изменил раннее сообщение? Под ним появится ‹ 1/2 › — возврат к старой версии восстанавливает и хвост чата под ней.", "То же для «Сгенерировать ещё раз»: каждая регенерация становится отдельной версией.", "Версии хранятся на сервере DeepSeek и восстанавливаются после перезапуска (для последних 16 сообщений).", "Настройки → «Статистика ИИ»: запросы, ответы, остановки, общее и среднее время, оценка токенов, правки, регенерации, инструменты и ошибки.", "Статистика считается для активного аккаунта; «Сбросить» очищает только её."]}, {"t": "Файлы и вложения", "p": "Скрепка в поле ввода добавляет файл к сообщению.", "li": ["Ограничение на один файл — 50 МБ; большие изображения сжимаются перед отправкой.", "Поддерживаются тексты, PDF, DOCX, таблицы, изображения; содержимое читается и передаётся модели.", "Рабочая папка инструментов — /sdcard/Download/FunaFiles.", "Экспорт документов и памяти — /sdcard/Download/FunaFiles/exports.", "Файлы вне Download доступны только с разрешением «Управление всеми файлами»."]}, {"t": "Инструменты модели", "p": "Модель может работать с файлами и командами, а не только отвечать текстом.", "li": ["read — прочитать файл; write/append — создать или дописать; edit — точечная замена.", "list/ls — содержимое папки; sh/bash/termux — команда через мост терминала (Termux/ReTerminal).", "doc/document — создать PDF или DOCX из заголовка и текста.", "Встроенные: time, calc, convert, random, base64, hash, skill — работают без сети; skill отдаёт инструкции docx/pdf/design/docs.", "Каждый вызов показывается отдельной карточкой; её можно свернуть или развернуть."], "a": {"act": "help", "label": "Диагностика"}}, {"t": "funa-bridge: как подключить", "p": "funa-bridge — небольшой скрипт в терминале, который выполняет команды FunaChat и возвращает вывод. Работает в Termux и ReTerminal. Без него доступны только файловые инструменты.", "li": ["1. Установи терминал из F-Droid: Termux или ReTerminal. Версия Termux из Google Play устарела и не работает на Android 12+.", "2. Открой этот раздел и нажми «Сохранить скрипт» — файл появится в /sdcard/Download/FunaFiles/funa-bridge.sh.", "3. Termux: termux-setup-storage, затем bash ~/storage/downloads/FunaFiles/funa-bridge.sh", "4. ReTerminal: sh /sdcard/Download/FunaFiles/funa-bridge.sh — termux-setup-storage там не нужен.", "5. Оставь сессию терминала открытой: в Termux добавь termux-wake-lock, в ReTerminal отключи для него оптимизацию батареи.", "6. Проверка: «Помощь и логи» → «Очередь Termux доступна» (или ReTerminal), а в чате — команда sh echo ok.", "Очередь команд лежит в /sdcard/Download/FunaQueue — её должны видеть и FunaChat, и терминал.", "Если /sdcard не видно: в Termux повтори termux-setup-storage, в ReTerminal разреши доступ к файлам в настройках Android.", "Скрипт v11 портативный: сам находит sh, обходится без flock и timeout — работает и в Alpine, и в системной оболочке. Вывод команды публикуется атомарно, поэтому фоновые процессы не ломают маркер завершения."], "a": {"act": "bridge", "label": "Сохранить скрипт"}}, {"t": "Память", "p": "Карточки знаний добавляются в системный промт в каждой новой сессии.", "li": ["Меню → «Память» → «+» → заголовок, текст, теги → «Сохранить».", "Карточку можно выключить переключателем, не удаляя.", "В промт попадает до 3000 символов — важное пиши коротко.", "Экспорт/импорт — JSON, удобно переносить между устройствами.", "Карточки принадлежат активному аккаунту; старые без владельца забирает первый аккаунт."], "a": {"act": "memory", "label": "Открыть память"}}, {"t": "Папки, поиск и сессии", "p": "Чатов может быть много — их можно группировать и искать.", "li": ["Боковое меню: поиск по названию, фильтр «Все / Без папки / папка».", "Долгое нажатие на папке — переименовать; «+» — создать.", "Меню сессии (⋮) — закрепить, переименовать, переместить в папку, удалить.", "Закреплённые сессии всегда сверху списка.", "Поиск внутри чата — кнопка лупы в верхней панели."]}, {"t": "Субагенты", "p": "Для сложных задач модель запускает помощников параллельно.", "li": ["Одновременно работает до 4 субагентов, глубина вложенности — 2.", "У каждого субагента свой блок с прогрессом и кнопкой остановки.", "Результат приходит отдельным сообщением в чат.", "В локальном API субагент выполняется синхронно в рамках запроса."]}, {"t": "Артефакты, PDF и DOCX", "p": "Большие результаты можно получить файлом или интерактивным артефактом.", "li": ["HTML-артефакт открывается на весь экран и работает офлайн.", "Артефакт работает в песочнице: без доступа к файлам, буферу и мосту приложения.", "PDF и DOCX генерируются на телефоне без внешних сервисов.", "Документом можно сразу поделиться или сохранить в Download/FunaFiles.", "Готовые файлы видны в файловом менеджере приложения."]}, {"t": "Навыки ИИ", "p": "Перед созданием документа модель читает встроенную инструкцию — поэтому PDF, DOCX и страницы получаются аккуратными.", "li": ["Навыки: docx (Word), pdf, design (HTML-артефакт) и docs (документация).", "В чате достаточно попросить «сделай PDF» — агент сам вызовет skill и соблюдёт правила.", "В локальном API есть инструмент skill с теми же инструкциями.", "Навыки лежат в assets/skills/<название>.<язык>.md и не требуют сети.", "Если модель забыла — повтори просьбу или назови навык."], "a": {"act": "newchat", "label": "К чату"}}, {"t": "Локальный API", "p": "OpenAI-совместимый сервер работает прямо на телефоне — для других приложений и агентов.", "li": ["Адрес по умолчанию: http://127.0.0.1:8787, ключ вида fk-… (Настройки → «Сеть и API» → API key).", "Эндпоинты: GET /health, GET /v1/models, GET /v1/models/{id}, GET /v1/tools, POST /v1/chat/completions, POST /v1/completions. Ключ нужен для всех, даже для /health.", "Пример: curl -s http://127.0.0.1:8787/v1/chat/completions -H \"Authorization: Bearer fk-…\" -H \"Content-Type: application/json\" -d '{\"model\":\"default\",\"messages\":[{\"role\":\"user\",\"content\":\"Привет\"}]}'", "Модели: deepseek-chat, deepseek-reasoner, deepseek-expert, deepseek-search. Неизвестный id мапится на модель из настроек; thinking/search можно передать в теле запроса.", "Для другого устройства в сети включи LAN и открой порт в фаерволе.", "Сервер соблюдает блокировку аккаунта: пока приложение заблокировано, /v1/* отдаёт 503, а /health — locked:true.", "Лимиты API: тело до 8 МБ, 8 одновременных соединений, 120 запросов/мин с одного IP, промт до 1 000 000 символов. Детали — в API.md."], "a": {"act": "settings", "label": "Настроить API"}}, {"t": "Публичный доступ", "p": "API можно открыть в интернет через туннель — без открытых портов и без карты.", "li": ["В Termux: cd ~/downloads/funachat/tunnel && ./start.sh — Cloudflare quick tunnel, резервный канал — zrok.", "Скрипт пишет адрес в Download/FunaFiles/tunnel.txt, приложение читает его кнопкой «Из файла» на странице API.", "Один origin: /v1/* — API, /updates/latest.json — манифест обновлений, /updates/FunaChat.apk — подписанный APK.", "Ключ fk-… обязателен; лимит 120 запросов/мин на IP, за туннелем считается реальный IP клиента.", "История обновлений → «Проверить обновления» покажет новую версию и кнопку «Скачать».", "Публичный API не выполняет файловых инструментов, funa-bridge и ADB.", "Детали и стабильный домен — tunnel/README.md."]}, {"t": "Контекст и сжатие", "p": "Длинные чаты не обрываются — старые части сжимаются автоматически.", "li": ["Настройки → «Чат и ИИ» → «Лимит контекста»: 60K, 128K, 256K, 512K или 1M токенов; по умолчанию — 1M (DeepSeek V4.1 Flash).", "В настройках видно, сколько места занято.", "Сжатие сохраняет суть и отбрасывает повторы, а не режет наугад.", "Новый чат всегда начинается с чистым контекстом и карточками памяти.", "Если модель «забыла» деталь — повтори её или добавь карточку памяти."]}, {"t": "Безопасность и приватность", "p": "Токены шифруются AES-256-GCM ключом Android Keystore.", "li": ["Пароль DeepSeek не сохраняется и не передаётся приложению — его обрабатывает сайт.", "Токены и cookie не попадают в логи и отчёты.", "Блокировка приложения — отдельный локальный пароль (PBKDF2); пароль DeepSeek для неё не нужен.", "Пароль блокировки принадлежит аккаунту: добавление другого аккаунта не отключает защиту первого.", "Разблокировка тоже отдельная для каждого аккаунта; холодный старт закрывает все сессии.", "Данные памяти, папок и сессий живут только на телефоне.", "Удаление аккаунта убирает его токен, пароль, память, папки и черновики."]}, {"t": "Лимиты и ошибки DeepSeek", "p": "DeepSeek ограничивает частые запросы и подозрительные входы — это его защита, не баг приложения.", "li": ["401 — токен просрочен: войди через сайт снова.", "Коды 40303/40304 — DeepSeek просит подтвердить, что ты человек: открой сайт кнопкой «Пройти проверку», отправь любое сообщение и пройди капчу, потом нажми «Импортировать» и повтори запрос.", "429 — слишком часто: подожди, пока сервер отпустит лимит.", "«Новое устройство» / риск — именно поэтому вход идёт через сайт, а не нативным запросом.", "Сайт может требовать капчу или код из письма — это нормально.", "Аккаунты без пароля (Google) поддерживаются только через сессию сайта."]}, {"t": "Диагностика и отчёты", "p": "Если что-то не работает — сначала открой «Помощь и логи».", "li": ["У каждого пункта есть состояние: ✓ хорошо, ! внимание, × проблема.", "Кнопка действия рядом ведёт сразу к нужной настройке.", "«Отчёт» содержит версию, устройство, диагностику и последние 300 строк лога.", "Логи можно посмотреть перед отправкой; токенов там нет.", "«Диагностические данные» — обезличенный снимок раз в сутки (по умолчанию включён); отправляет Termux-релей в Telegram, токен в APK не входит."], "a": {"act": "help", "label": "Открыть диагностику"}}, {"t": "Советы", "p": "Мелочи, которые экономят время.", "li": ["Формулируй задачу конкретно: цель, формат, ограничения.", "Проси файл («сделай PDF») — получишь документ, а не простыню текста.", "Для повторяющихся фактов — карточка памяти, а не новый чат каждый раз.", "Долгие исследования лучше разбивать на чаты с отдельными папками.", "Закрепляй важные сессии, чтобы не искать их в списке.", "Если ответ оборвался — «Продолжай», модель помнит контекст."]}]};

/* История обновлений. Порядок — от новых к старым; текущая версия
   подсвечивается по номеру из нативного слоя (S.version). */
const UPDATES = {
  uk: [
    {
      code: "v108", name: "2.42.33", title: "Чат не стрибає вниз під час відповіді",
      p: "Якщо прокрутити історію вгору, поки модель пише, екран більше не смикається до низу.",
      li: [
        "Автопрокрутка вимикається, щойно палець тягне чат угору.",
        "Нові шматки відповіді не змінюють позицію, поки ти читаєш вище.",
        "Кнопка внизу повертає до останнього повідомлення й знову вмикає автопрокрутку.",
        "Надсилання нового повідомлення теж повертає до низу."
      ]
    },
    {
      code: "v107", name: "2.42.32", title: "Довгі відповіді не розсувають чат",
      p: "HTML-сторінка під час генерації показується компактною карткою, а не простирадлом тегів.",
      li: [
        "Поки HTML тече, видно картку «Сторінка збирається…», після завершення — готовий артефакт.",
        "HTML без ```-огорожі теж стає інтерактивною карткою.",
        "Код довший за 4 КБ згорнутий, його можна розгорнути кнопкою.",
        "Відповідь довша за 24 КБ показується початком, повний текст — за кнопкою."
      ]
    },
    {
      code: "v106", name: "2.42.31", title: "Перевірка акаунта DeepSeek",
      p: "Коли DeepSeek просить hCaptcha, застосунок більше не вважає це успіхом і показує кнопку перевірки.",
      li: [
        "Коди 40303/40304 зберігаються й показуються як «потрібна перевірка».",
        "Кнопка «Пройти перевірку» відкриває сайт на чаті: надішли повідомлення й пройди капчу.",
        "Після «Імпортувати» сесія оновлюється, а статус акаунта знову стає активним."
      ]
    },
    {
      code: "v105", name: "2.42.30", title: "Адреса приймача закодована",
      p: "URL телеметрії більше не лежить в APK відкритим рядком.",
      li: [
        "Адреса збирається з закодованих байтів під час роботи.",
        "Токен бота, chat_id і GitHub-токен в APK відсутні.",
        "allowBackup вимкнено, підпис V3; локальні секрети не входять у бекапи."
      ]
    },
    {
      code: "v104", name: "2.42.29", title: "Повний звіт документом",
      p: "У чат іде короткий підсумок, а повний JSON із логами — файлом, без обрізання.",
      li: [
        "Повідомлення містить підсумок і хвіст логу; повний звіт прикріплюється документом.",
        "Кількість рядків логу рахується за реальним вмістом, а не за памʼяттю запуску.",
        "Логи беруться з файлу, тому історія зберігається між запусками."
      ]
    },
    {
      code: "v103", name: "2.42.28", title: "Історія логів у звіті",
      p: "Діагностика бере логи з файлу, тому після перезапуску історія не губиться.",
      li: [
        "Останні 150 рядків логу, включно з попередніми запусками.",
        "Токени, ключі, cookie й підписи вирізаються ще до запису.",
        "Розмір звіту автоматично підганяється під ліміт приймача."
      ]
    },
    {
      code: "v102", name: "2.42.27", title: "Повний звіт і закрита адреса",
      p: "Діагностика тепер містить логи й текст збою, а адреса приймача більше не показується.",
      li: [
        "У звіті останні 150 рядків логу (разом з історією до перезапуску) та повний текст збою; токени й ключі вирізаються.",
        "Виправлено час звіту: раніше через обмеження Worker він показувався як 1970 рік.",
        "Адреса HTTPS-приймача прибрана з інтерфейсу й не повертається застосунком.",
        "Повідомлення в Telegram автоматично обрізається до ліміту, останні рядки логу зберігаються."
      ]
    },
    {
      code: "v101", name: "2.42.26", title: "Телеметрія від усіх інсталяцій",
      p: "Діагностика йде напряму на Cloudflare Worker; токен бота не входить в APK.",
      li: [
        "Звіт надсилається на HTTPS-приймач одразу; при помилці лишається в локальній черзі.",
        "URL приймача видно й можна змінити в Довідка → «Діагностичні дані».",
        "Токен бота зберігається лише в секретах Cloudflare, не в застосунку.",
        "Склад звіту не змінився: версія, пристрій, дозволи й лічильники, без токенів і тексту чатів."
      ]
    },
    {
      code: "v100", name: "2.42.25", title: "Діагностична телеметрія",
      p: "Знеособлений звіт раз на добу допомагає швидше знаходити збої; надсилається через Termux-релей у Telegram.",
      li: [
        "Довідка → «Діагностичні дані»: перемикач (типово увімкнений), статус черги, «Показати звіт» і «Надіслати зараз».",
        "У звіті лише версія, модель/Android, локаль, дозволи й лічильники; без токенів, ключів, пошти та тексту чатів.",
        "Файли лежать у Download/FunaFiles/telemetry; tunnel/telemetry.sh надсилає їх через бота в Telegram.",
        "Токен бота зберігається в ~/.config/funachat і не входить в APK."
      ]
    },
    {
      code: "v99", name: "2.42.24", title: "Надійніший канал оновлень",
      p: "Перевірка оновлень перебирає кілька джерел, якщо основне віддає старий маніфест.",
      li: [
        "Порядок: заданий канал → адреса тунелю → GitHub raw → jsDelivr.",
        "Якщо raw віддає закешований latest.json, застосунок візьме свіжий із jsDelivr.",
        "Невалідний маніфест більше не зупиняє перевірку — пробується наступне джерело."
      ]
    },
    {
      code: "v98", name: "2.42.23", title: "Аудит злитих ключів",
      p: "Секретний endpoint показує власника злитого API-ключа, дозволяє його відкликати й повідомити власника.",
      li: [
        "POST /v1/audit/key: дії info (довідка), revoke (відкликати), notify (повідомити власника).",
        "Доступ — секрет аудиту пристрою fa-… або підпис приватним ключем розробника (ECDSA P-256).",
        "Відповідь містить власника, пристрій, назву ключа, термін дії, останню активність та IP.",
        "Журнал аудиту й webhook у Налаштуваннях → API → Аудит ключів; події також показуються Toast-повідомленням.",
        "CLI: tunnel/audit-key.sh <base-url> <key> [info|revoke|notify] [note].",
        "Коли публічний доступ вимкнено, аудит ззовні теж закритий (403); локально працює."
      ]
    },
    {
      code: "v97", name: "2.42.22", title: "Керування API",
      p: "Публічний доступ можна вимкнути, а ключів стало багато — з іменами й терміном дії.",
      li: [
        "«Дозволити доступ із публічної мережі»: вимкнено — запити з тунелю отримують 403, сам тунель можна не зупиняти.",
        "API-ключі: fk- + 48 hex (192 біти). Старі короткі ключі лишаються чинними.",
        "Кілька ключів одночасно: ім'я, термін дії, окреме відкликання й копіювання.",
        "Прострочений ключ дає 401 expired_api_key, невідомий — 401 invalid_api_key.",
        "Адреса тунелю тепер лише для читання; «Забути адресу» очищає її.",
        "Новий ключ показується повністю один раз — одразу після створення."
      ]
    },
    {
      code: "v96", name: "2.42.21", title: "Канал оновлень на GitHub",
      p: "Перевірка оновлень працює навіть без тунелю — маніфест лежить у репозиторії Funachat.",
      li: [
        "Типовий канал — latest.json у Sqwid-member/Funachat; якщо raw віддає кеш, використовується jsDelivr.",
        "Публічна адреса тунелю, якщо задана, має пріоритет над GitHub.",
        "Релізи публікуються скриптом tunnel/release-gh.sh: APK + latest.json, тег v<code>.",
        "v95 лишається внутрішньою збіркою: її канал оновлень не мав CORS."
      ]
    },
    {
      code: "v95", name: "2.42.20", title: "Публічний доступ і перевірка оновлень",
      p: "API можна відкрити в інтернет через тунель, а застосунок сам перевіряє, чи є новіша версія.",
      li: [
        "Налаштування API → «Публічний доступ (тунель)»: адреса, кнопка «З файлу» (читає Download/FunaFiles/tunnel.txt), перевірка доступності.",
        "Кнопка «Копіювати конфіг клієнта» дає Base URL, ключ і модель для будь-якого OpenAI-клієнта.",
        "Тиха перевірка оновлень під час запуску і ручна — на екрані «Історія оновлень».",
        "Якщо є новіша версія, показується її номер і кнопка «Завантажити».",
        "Ліміт запитів рахується за справжнім IP клієнта за тунелем, а не за 127.0.0.1.",
        "Додано edge-проксі та скрипти тунелів: Cloudflare (основний) і zrok (резерв)."
      ]
    },
    {
      code: "v94", name: "2.42.19", title: "Журнал оновлень",
      p: "У застосунку з'явився екран з історією версій — що саме змінилося в кожному оновленні.",
      li: [
        "Новий екран «Історія оновлень» зі списком версій: новіші — зверху.",
        "Поточна версія підсвічена рамкою й позначкою «Поточна версія».",
        "Відкривається з «Довідки й логів» та з налаштувань → «Довідка й застосунок».",
        "Для v90–v93 додано детальні описи; ранні v21–v89 зібрано в зведений блок без вигаданих дат.",
        "Журнал перекладається українською, англійською та російською.",
        "Додано CHANGELOG.md із тим самим вмістом для репозиторію."
      ]
    },
    {
      code: "v93", name: "2.42.18", title: "Черга повідомлень",
      p: "Можна писати далі, поки модель відповідає — повідомлення не губляться, а стають у чергу.",
      li: [
        "Під час відповіді Enter або кнопка надсилання ставлять повідомлення в чергу, а не блокуються.",
        "Після завершення відповіді черга автоматично надсилає наступне повідомлення.",
        "Порядок змінюється кнопками ↑ і ↓, текст редагується в окремому вікні, зайве видаляється кошиком.",
        "Черга зберігає вкладення; редагування тексту не втрачає файли.",
        "Окрема червона кнопка «Стоп» зупиняє генерацію. Явна зупинка ставить чергу на паузу, кнопка ▶ у панелі черги відновлює її.",
        "Поле введення не очищується, коли черга надсилає своє повідомлення.",
        "Перехід в інший чат із непорожньою чергою просить підтвердження.",
        "Оновлено довідку та UI-тести."
      ]
    },
    {
      code: "v92", name: "2.42.17", title: "Контекст до 1M токенів",
      p: "Ліміт контексту став налаштовуваним, а типовий пресет відповідає DeepSeek V4.1 Flash.",
      li: [
        "Пресети ліміту: 60K, 128K, 256K, 512K або 1M токенів; типовий — 1M.",
        "Вибір зберігається в налаштуваннях і діє для активного акаунта.",
        "Локальний API приймає промт до 1 000 000 символів.",
        "Обмеження введення в застосунку — 1 000 000 символів.",
        "Стиснення контексту витримує транскрипт до 400 000 символів, субагенти — до 200 000.",
        "Оновлено API.md, документацію в застосунку й тести."
      ]
    },
    {
      code: "v91", name: "2.42.16", title: "Полірування налаштувань і чату",
      p: "Виправлення після великого оновлення налаштувань і кілька зручностей у чаті.",
      li: [
        "Виправлено горизонтальне переповнення категорій налаштувань.",
        "Додано анімації переходу між головним екраном і сторінками категорій.",
        "Кнопки в «Довідці» вирівняно, відступ між ними став однаковий.",
        "Час повідомлень кешується: після повторного відкриття сесії він не зникає.",
        "Перемикачі версій отримали видимі SVG-кнопки.",
        "Для повідомлень ШІ додано контекстне меню: копіювати, перегенерувати, продовжити."
      ]
    },
    {
      code: "v90", name: "2.42.15", title: "Сім категорій налаштувань",
      p: "Велике меню налаштувань розібрано на зрозумілі розділи.",
      li: [
        "Категорії: «Акаунт і безпека», «Чат і ШІ», «Мережа та API», «Файли й дані», «Мова й вигляд», «Статистика ШІ», «Довідка й застосунок».",
        "Головний екран показує всі категорії з коротким описом поточного стану.",
        "Кожна категорія відкривається окремою сторінкою з плавним переходом.",
        "Кнопка «Назад» повертає зі сторінки до списку, а зі списку — до чату.",
        "Кнопка «Зберегти» закріплена внизу й доступна на будь-якій сторінці."
      ]
    },
    {
      code: "v21–v89", name: "", title: "Ранні версії",
      p: "Фундамент застосунку. Точні дати й окремі зміни цих збірок не збереглися, тому тут зведено те, що з'явилося за цей період.",
      li: [
        "Акаунти DeepSeek: вхід через сайт у WebView, ключ сесії, імпорт session.json, cookie-режим і кілька акаунтів.",
        "Безпека: Vault, блокування застосунку, PBKDF2, AES-256-GCM і Android Keystore.",
        "Чат: Markdown, KaTeX, підсвітка коду, історія, пошук, папки, закріплення, версії повідомлень і статистика.",
        "Файли: вкладення до 50 МБ, PDF/DOCX, HTML-артефакти, стиснення зображень.",
        "Модель: інструменти read/write/edit/list/sh, навички docx/pdf/design/docs, субагенти й картки пам'яті.",
        "Локальний API, сумісний з OpenAI, і міст funa-bridge для Termux/ReTerminal.",
        "Інтерфейс: три мови (uk/en/ru), онбординг першого запуску, документація та діагностика."
      ]
    }
  ],
  en: [
    {
      code: "v108", name: "2.42.33", title: "Chat no longer jumps down while answering",
      p: "If you scroll up while the model is writing, the screen no longer snaps back to the bottom.",
      li: [
        "Auto-scroll stops as soon as you drag the chat upwards.",
        "New pieces of the answer do not move the view while you read above.",
        "The bottom button returns to the latest message and turns auto-scroll back on.",
        "Sending a new message also returns to the bottom."
      ]
    },
    {
      code: "v107", name: "2.42.32", title: "Long answers no longer flood the chat",
      p: "An HTML page is shown as a compact card while it is being generated, not as a wall of tags.",
      li: [
        "While HTML streams, a “Building the page…” card is shown; it becomes the finished artifact afterwards.",
        "HTML without a ``` fence also becomes an interactive card.",
        "Code longer than 4 KB is folded and can be expanded with a button.",
        "Answers longer than 24 KB show their beginning, with the full text behind a button."
      ]
    },
    {
      code: "v106", name: "2.42.31", title: "DeepSeek account check",
      p: "When DeepSeek asks for hCaptcha, the app no longer treats it as success and offers a check button.",
      li: [
        "Codes 40303/40304 are preserved and shown as “check required”.",
        "“Complete the check” opens the site on the chat: send a message and pass the captcha.",
        "After “Import” the session is refreshed and the account status becomes active again."
      ]
    },
    {
      code: "v105", name: "2.42.30", title: "Receiver address encoded",
      p: "The telemetry URL is no longer a readable string in the APK.",
      li: [
        "The address is assembled from encoded bytes at runtime.",
        "The bot token, chat_id and GitHub token are absent from the APK.",
        "allowBackup is off, signature V3; local secrets stay out of backups."
      ]
    },
    {
      code: "v104", name: "2.42.29", title: "Full report as a file",
      p: "The chat gets a short summary; the full JSON with logs is attached as a document, untrimmed.",
      li: [
        "The message shows a summary and the log tail; the full report is attached as a file.",
        "Log line count comes from the actual report, not from the current session buffer.",
        "Logs are read from disk, so history survives restarts."
      ]
    },
    {
      code: "v103", name: "2.42.28", title: "Log history in reports",
      p: "Diagnostics read logs from disk, so history survives restarts.",
      li: [
        "The last 150 log lines, including previous launches.",
        "Tokens, keys, cookies and signatures are stripped before writing.",
        "The report size is fitted to the receiver limit automatically."
      ]
    },
    {
      code: "v102", name: "2.42.27", title: "Full report, hidden endpoint",
      p: "Diagnostics now include logs and crash text; the receiver address is no longer shown.",
      li: [
        "The report carries the last 150 log lines (including history across restarts) and the full crash text; tokens and keys are stripped.",
        "Fixed the report timestamp: a Worker cap used to show it as 1970.",
        "The HTTPS receiver address is removed from the UI and no longer returned by the app.",
        "Telegram messages are trimmed to the limit, keeping the newest log lines."
      ]
    },
    {
      code: "v101", name: "2.42.26", title: "Telemetry from every install",
      p: "Diagnostics go straight to a Cloudflare Worker; the bot token is not in the APK.",
      li: [
        "The report is posted to the HTTPS receiver immediately; on failure it stays in the local queue.",
        "The receiver URL is visible and editable in Help → “Diagnostic data”.",
        "The bot token lives only in Cloudflare secrets, not in the app.",
        "The report contents are unchanged: version, device, permissions and counters, no tokens or chat text."
      ]
    },
    {
      code: "v100", name: "2.42.25", title: "Diagnostic telemetry",
      p: "An anonymised daily report helps find crashes faster; a Termux relay sends it to Telegram.",
      li: [
        "Help → “Diagnostic data”: toggle (on by default), queue status, “Show report” and “Send now”.",
        "The report has only version, device/Android, locale, permissions and counters; no tokens, keys, email or chat text.",
        "Files live in Download/FunaFiles/telemetry; tunnel/telemetry.sh sends them through the bot.",
        "The bot token stays in ~/.config/funachat and is not bundled in the APK."
      ]
    },
    {
      code: "v99", name: "2.42.24", title: "More reliable update channel",
      p: "The update check walks several sources when the primary one serves a stale manifest.",
      li: [
        "Order: configured channel → tunnel URL → GitHub raw → jsDelivr.",
        "If raw serves a cached latest.json, the app takes a fresh one from jsDelivr.",
        "An invalid manifest no longer stops the check — the next source is tried."
      ]
    },
    {
      code: "v98", name: "2.42.23", title: "Leaked key audit",
      p: "A secret endpoint shows the owner of a leaked API key and can revoke it and notify the owner.",
      li: [
        "POST /v1/audit/key: actions info, revoke and notify.",
        "Access via the device audit secret fa-… or a developer signature (ECDSA P-256).",
        "The response includes the owner, device, key name, expiry, last activity and IP.",
        "Audit log and webhook in Settings → API → Key audit; events also appear as a Toast.",
        "CLI: tunnel/audit-key.sh <base-url> <key> [info|revoke|notify] [note].",
        "When public access is off, remote audit is closed too (403); local audit still works."
      ]
    },
    {
      code: "v97", name: "2.42.22", title: "API management",
      p: "Public access can be switched off, and you can keep many keys with names and expiry dates.",
      li: [
        "“Allow access from the public network”: when off, tunnel requests get 403 while the tunnel itself may keep running.",
        "API keys are now fk- + 48 hex (192 bits). Older short keys stay valid.",
        "Multiple keys at once: name, expiry, individual revoke and copy.",
        "An expired key returns 401 expired_api_key; an unknown one returns 401 invalid_api_key.",
        "The tunnel address is now read-only; “Forget address” clears it.",
        "A new key is shown in full only once, right after creation."
      ]
    },
    {
      code: "v96", name: "2.42.21", title: "Update channel on GitHub",
      p: "Update checks work even without the tunnel — the manifest lives in the Funachat repository.",
      li: [
        "Default channel: latest.json in Sqwid-member/Funachat; jsDelivr is used when raw serves a cached copy.",
        "A configured tunnel URL takes priority over GitHub.",
        "Releases are published by tunnel/release-gh.sh: APK + latest.json, tag v<code>.",
        "v95 remains an internal build: its update channel had no CORS."
      ]
    },
    {
      code: "v95", name: "2.42.20", title: "Public access and update checks",
      p: "The API can be exposed to the internet through a tunnel, and the app now checks for newer versions itself.",
      li: [
        "API settings → Public access (tunnel): URL, From file button (reads Download/FunaFiles/tunnel.txt), reachability check.",
        "Copy client config gives the Base URL, key and model for any OpenAI client.",
        "Silent update check on launch and a manual one on the Update log screen.",
        "If a newer version exists, its number and a Download button are shown.",
        "Rate limits now use the real client IP behind a tunnel instead of 127.0.0.1.",
        "Added an edge proxy and tunnel scripts: Cloudflare (primary) and zrok (backup)."
      ]
    },
    {
      code: "v94", name: "2.42.19", title: "Update log",
      p: "The app now has a version history screen showing exactly what changed in each release.",
      li: [
        "A new Update log screen lists releases with the newest first.",
        "The current version is highlighted with a border and a Current version badge.",
        "Open it from Help & logs or Settings → Help & app.",
        "v90–v93 have detailed notes; earlier v21–v89 builds are summarised without invented dates.",
        "The log is translated into Ukrainian, English and Russian.",
        "Added CHANGELOG.md with the same content for the repository."
      ]
    },
    {
      code: "v93", name: "2.42.18", title: "Message queue",
      p: "Keep writing while the model answers — messages are not lost, they wait in a queue.",
      li: [
        "While an answer is streaming, Enter or the send button queues the message instead of blocking.",
        "When the answer finishes, the queue automatically sends the next message.",
        "Reorder with ↑ and ↓, edit the text in a sheet, remove with the bin button.",
        "The queue keeps attachments; editing the text does not drop the files.",
        "A separate red Stop button halts generation. An explicit stop pauses the queue; the ▶ button in the queue bar resumes it.",
        "The composer is not cleared when the queue sends its own message.",
        "Leaving a chat with a non-empty queue asks for confirmation.",
        "Help and UI tests were updated."
      ]
    },
    {
      code: "v92", name: "2.42.17", title: "1M-token context",
      p: "The context limit is configurable, and the default matches DeepSeek V4.1 Flash.",
      li: [
        "Presets: 60K, 128K, 256K, 512K or 1M tokens; the default is 1M.",
        "The choice is saved in Settings and applies to the active account.",
        "The local API accepts prompts up to 1,000,000 characters.",
        "The app input limit is 1,000,000 characters.",
        "Context compaction handles transcripts up to 400,000 characters; subagents up to 200,000.",
        "API.md, in-app docs and tests were updated."
      ]
    },
    {
      code: "v91", name: "2.42.16", title: "Settings and chat polish",
      p: "Fixes after the big settings update plus a few chat conveniences.",
      li: [
        "Fixed horizontal overflow in the settings categories.",
        "Added transitions between the settings home screen and category pages.",
        "Buttons in Help are aligned with consistent spacing.",
        "Message timestamps are cached, so they survive reopening a session.",
        "Version switchers now have visible SVG buttons.",
        "Assistant messages got a context menu: copy, regenerate, continue."
      ]
    },
    {
      code: "v90", name: "2.42.15", title: "Seven settings categories",
      p: "The large settings menu was split into clear sections.",
      li: [
        "Categories: Account & security, Chat & AI, Network & API, Files & data, Language & look, AI usage, Help & app.",
        "The home screen lists every category with a short status summary.",
        "Each category opens on its own page with a smooth transition.",
        "Back returns from a category to the list, and from the list to the chat.",
        "The Save button is pinned at the bottom on every page."
      ]
    },
    {
      code: "v21–v89", name: "", title: "Early versions",
      p: "The foundation of the app. Exact dates and individual changes of those builds were not preserved, so this is a summary of what appeared during that period.",
      li: [
        "DeepSeek accounts: site login in a WebView, session key, session.json import, cookie mode and multiple accounts.",
        "Security: Vault, app lock, PBKDF2, AES-256-GCM and the Android Keystore.",
        "Chat: Markdown, KaTeX, code highlighting, history, search, folders, pinning, message versions and statistics.",
        "Files: attachments up to 50 MB, PDF/DOCX, HTML artifacts, image compression.",
        "Model: read/write/edit/list/sh tools, docx/pdf/design/docs skills, subagents and memory cards.",
        "An OpenAI-compatible local API and the funa-bridge for Termux/ReTerminal.",
        "Interface: three languages (uk/en/ru), first-run onboarding, docs and diagnostics."
      ]
    }
  ],
  ru: [
    {
      code: "v108", name: "2.42.33", title: "Чат не прыгает вниз во время ответа",
      p: "Если прокрутить историю вверх, пока модель пишет, экран больше не дёргается вниз.",
      li: [
        "Автопрокрутка выключается, как только палец тянет чат вверх.",
        "Новые куски ответа не меняют позицию, пока ты читаешь выше.",
        "Кнопка внизу возвращает к последнему сообщению и снова включает автопрокрутку.",
        "Отправка нового сообщения тоже возвращает к низу."
      ]
    },
    {
      code: "v107", name: "2.42.32", title: "Длинные ответы не растягивают чат",
      p: "HTML-страница во время генерации показывается компактной карточкой, а не простынёй тегов.",
      li: [
        "Пока HTML течёт, видна карточка «Страница собирается…», после завершения — готовый артефакт.",
        "HTML без ```-обёртки тоже становится интерактивной карточкой.",
        "Код длиннее 4 КБ свёрнут, его можно развернуть кнопкой.",
        "Ответ длиннее 24 КБ показывается началом, полный текст — за кнопкой."
      ]
    },
    {
      code: "v106", name: "2.42.31", title: "Проверка аккаунта DeepSeek",
      p: "Когда DeepSeek просит hCaptcha, приложение больше не считает это успехом и показывает кнопку проверки.",
      li: [
        "Коды 40303/40304 сохраняются и показываются как «нужна проверка».",
        "«Пройти проверку» открывает сайт на чате: отправь сообщение и пройди капчу.",
        "После «Импортировать» сессия обновляется, а статус аккаунта снова становится активным."
      ]
    },
    {
      code: "v105", name: "2.42.30", title: "Адрес приёмника закодирован",
      p: "URL телеметрии больше не лежит в APK открытой строкой.",
      li: [
        "Адрес собирается из закодированных байтов во время работы.",
        "Токен бота, chat_id и GitHub-токен в APK отсутствуют.",
        "allowBackup отключён, подпись V3; локальные секреты не попадают в бэкапы."
      ]
    },
    {
      code: "v104", name: "2.42.29", title: "Полный отчёт документом",
      p: "В чат идёт краткая сводка, а полный JSON с логами — файлом, без обрезки.",
      li: [
        "Сообщение содержит сводку и хвост лога; полный отчёт прикрепляется документом.",
        "Количество строк лога считается по реальному содержимому, а не по памяти запуска.",
        "Логи берутся из файла, поэтому история сохраняется между запусками."
      ]
    },
    {
      code: "v103", name: "2.42.28", title: "История логов в отчёте",
      p: "Диагностика берёт логи из файла, поэтому после перезапуска история не теряется.",
      li: [
        "Последние 150 строк лога, включая предыдущие запуски.",
        "Токены, ключи, cookie и подписи вырезаются ещё до записи.",
        "Размер отчёта автоматически подгоняется под лимит приёмника."
      ]
    },
    {
      code: "v102", name: "2.42.27", title: "Полный отчёт и скрытый адрес",
      p: "Диагностика теперь содержит логи и текст сбоя, а адрес приёмника больше не показывается.",
      li: [
        "В отчёте последние 150 строк лога (включая историю до перезапуска) и полный текст сбоя; токены и ключи вырезаются.",
        "Исправлено время отчёта: раньше из-за ограничения Worker оно показывалось как 1970 год.",
        "Адрес HTTPS-приёмника убран из интерфейса и не возвращается приложением.",
        "Сообщение в Telegram автоматически обрезается до лимита, последние строки лога сохраняются."
      ]
    },
    {
      code: "v101", name: "2.42.26", title: "Телеметрия от всех установок",
      p: "Диагностика идёт напрямую на Cloudflare Worker; токен бота не входит в APK.",
      li: [
        "Отчёт отправляется на HTTPS-приёмник сразу; при ошибке остаётся в локальной очереди.",
        "URL приёмника виден и редактируется в Помощь → «Диагностические данные».",
        "Токен бота хранится только в секретах Cloudflare, не в приложении.",
        "Состав отчёта не изменился: версия, устройство, разрешения и счётчики, без токенов и текста чатов."
      ]
    },
    {
      code: "v100", name: "2.42.25", title: "Диагностическая телеметрия",
      p: "Обезличенный отчёт раз в сутки помогает быстрее находить сбои; отправляется через Termux-релей в Telegram.",
      li: [
        "Помощь → «Диагностические данные»: переключатель (по умолчанию включён), статус очереди, «Показать отчёт» и «Отправить сейчас».",
        "В отчёте только версия, модель/Android, локаль, разрешения и счётчики; без токенов, ключей, почты и текста чатов.",
        "Файлы лежат в Download/FunaFiles/telemetry; tunnel/telemetry.sh отправляет их через бота.",
        "Токен бота хранится в ~/.config/funachat и не входит в APK."
      ]
    },
    {
      code: "v99", name: "2.42.24", title: "Надёжнее канал обновлений",
      p: "Проверка обновлений перебирает несколько источников, если основной отдаёт старый манифест.",
      li: [
        "Порядок: заданный канал → адрес туннеля → GitHub raw → jsDelivr.",
        "Если raw отдаёт закешированный latest.json, приложение возьмёт свежий из jsDelivr.",
        "Невалидный манифест больше не останавливает проверку — пробуется следующий источник."
      ]
    },
    {
      code: "v98", name: "2.42.23", title: "Аудит утёкших ключей",
      p: "Секретный endpoint показывает владельца утёкшего API-ключа и позволяет отозвать его и уведомить владельца.",
      li: [
        "POST /v1/audit/key: действия info, revoke и notify.",
        "Доступ — секрет аудита устройства fa-… или подпись приватным ключом разработчика (ECDSA P-256).",
        "Ответ содержит владельца, устройство, имя ключа, срок действия, последнюю активность и IP.",
        "Журнал аудита и webhook в Настройках → API → Аудит ключей; события также показываются Toast.",
        "CLI: tunnel/audit-key.sh <base-url> <key> [info|revoke|notify] [note].",
        "Когда публичный доступ выключен, удалённый аудит тоже закрыт (403); локально работает."
      ]
    },
    {
      code: "v97", name: "2.42.22", title: "Управление API",
      p: "Публичный доступ можно выключить, а ключей стало много — с именами и сроком действия.",
      li: [
        "«Разрешить доступ из публичной сети»: выключено — запросы из туннеля получают 403, сам туннель можно не останавливать.",
        "API-ключи: fk- + 48 hex (192 бита). Старые короткие ключи остаются действительными.",
        "Несколько ключей одновременно: имя, срок действия, отдельный отзыв и копирование.",
        "Просроченный ключ даёт 401 expired_api_key, неизвестный — 401 invalid_api_key.",
        "Адрес туннеля теперь только для чтения; «Забыть адрес» очищает его.",
        "Новый ключ показывается полностью один раз — сразу после создания."
      ]
    },
    {
      code: "v96", name: "2.42.21", title: "Канал обновлений на GitHub",
      p: "Проверка обновлений работает даже без туннеля — манифест лежит в репозитории Funachat.",
      li: [
        "Канал по умолчанию — latest.json в Sqwid-member/Funachat; если raw отдаёт кеш, используется jsDelivr.",
        "Публичный адрес туннеля, если задан, имеет приоритет над GitHub.",
        "Релизы публикуются скриптом tunnel/release-gh.sh: APK + latest.json, тег v<code>.",
        "v95 остаётся внутренней сборкой: у её канала обновлений не было CORS."
      ]
    },
    {
      code: "v95", name: "2.42.20", title: "Публичный доступ и проверка обновлений",
      p: "API можно открыть в интернет через туннель, а приложение само проверяет наличие новой версии.",
      li: [
        "Настройки API → «Публичный доступ (туннель)»: адрес, кнопка «Из файла» (читает Download/FunaFiles/tunnel.txt), проверка доступности.",
        "Кнопка «Копировать конфиг клиента» даёт Base URL, ключ и модель для любого OpenAI-клиента.",
        "Тихая проверка обновлений при запуске и ручная — на экране «История обновлений».",
        "Если есть новая версия, показываются её номер и кнопка «Скачать».",
        "Лимит запросов считается по реальному IP клиента за туннелем, а не по 127.0.0.1.",
        "Добавлены edge-прокси и скрипты туннелей: Cloudflare (основной) и zrok (резерв)."
      ]
    },
    {
      code: "v94", name: "2.42.19", title: "История обновлений",
      p: "В приложении появился экран с историей версий — что именно изменилось в каждом обновлении.",
      li: [
        "Новый экран «История обновлений» со списком версий: новые — сверху.",
        "Текущая версия подсвечена рамкой и меткой «Текущая версия».",
        "Открывается из «Справки и логов» и из настроек → «Справка и приложение».",
        "Для v90–v93 добавлены подробные описания; ранние v21–v89 собраны в сводный блок без выдуманных дат.",
        "История переводится на украинский, английский и русский.",
        "Добавлен CHANGELOG.md с тем же содержимым для репозитория."
      ]
    },
    {
      code: "v93", name: "2.42.18", title: "Очередь сообщений",
      p: "Можно писать дальше, пока модель отвечает — сообщения не теряются, а встают в очередь.",
      li: [
        "Во время ответа Enter или кнопка отправки ставят сообщение в очередь, а не блокируются.",
        "После завершения ответа очередь автоматически отправляет следующее сообщение.",
        "Порядок меняется кнопками ↑ и ↓, текст редактируется в отдельном окне, лишнее удаляется корзиной.",
        "Очередь сохраняет вложения; редактирование текста не теряет файлы.",
        "Отдельная красная кнопка «Стоп» останавливает генерацию. Явная остановка ставит очередь на паузу, кнопка ▶ в панели очереди возобновляет её.",
        "Поле ввода не очищается, когда очередь отправляет своё сообщение.",
        "Переход в другой чат с непустой очередью просит подтверждение.",
        "Обновлены справка и UI-тесты."
      ]
    },
    {
      code: "v92", name: "2.42.17", title: "Контекст до 1M токенов",
      p: "Лимит контекста стал настраиваемым, а пресет по умолчанию соответствует DeepSeek V4.1 Flash.",
      li: [
        "Пресеты лимита: 60K, 128K, 256K, 512K или 1M токенов; по умолчанию — 1M.",
        "Выбор сохраняется в настройках и действует для активного аккаунта.",
        "Локальный API принимает промт до 1 000 000 символов.",
        "Ограничение ввода в приложении — 1 000 000 символов.",
        "Сжатие контекста выдерживает транскрипт до 400 000 символов, субагенты — до 200 000.",
        "Обновлены API.md, документация в приложении и тесты."
      ]
    },
    {
      code: "v91", name: "2.42.16", title: "Полировка настроек и чата",
      p: "Исправления после большого обновления настроек и несколько удобств в чате.",
      li: [
        "Исправлено горизонтальное переполнение категорий настроек.",
        "Добавлены анимации перехода между главным экраном и страницами категорий.",
        "Кнопки в «Справке» выровнены, отступ между ними стал одинаковым.",
        "Время сообщений кешируется: после повторного открытия сессии оно не исчезает.",
        "Переключатели версий получили видимые SVG-кнопки.",
        "Для сообщений ИИ добавлено контекстное меню: копировать, перегенерировать, продолжить."
      ]
    },
    {
      code: "v90", name: "2.42.15", title: "Семь категорий настроек",
      p: "Большое меню настроек разобрано на понятные разделы.",
      li: [
        "Категории: «Аккаунт и безопасность», «Чат и ИИ», «Сеть и API», «Файлы и данные», «Язык и вид», «Статистика ИИ», «Справка и приложение».",
        "Главный экран показывает все категории с кратким описанием текущего состояния.",
        "Каждая категория открывается отдельной страницей с плавным переходом.",
        "Кнопка «Назад» возвращает со страницы к списку, а из списка — в чат.",
        "Кнопка «Сохранить» закреплена внизу и доступна на любой странице."
      ]
    },
    {
      code: "v21–v89", name: "", title: "Ранние версии",
      p: "Фундамент приложения. Точные даты и отдельные изменения этих сборок не сохранились, поэтому здесь собрано то, что появилось за этот период.",
      li: [
        "Аккаунты DeepSeek: вход через сайт в WebView, ключ сессии, импорт session.json, cookie-режим и несколько аккаунтов.",
        "Безопасность: Vault, блокировка приложения, PBKDF2, AES-256-GCM и Android Keystore.",
        "Чат: Markdown, KaTeX, подсветка кода, история, поиск, папки, закрепление, версии сообщений и статистика.",
        "Файлы: вложения до 50 МБ, PDF/DOCX, HTML-артефакты, сжатие изображений.",
        "Модель: инструменты read/write/edit/list/sh, навыки docx/pdf/design/docs, субагенты и карточки памяти.",
        "Локальный API, совместимый с OpenAI, и мост funa-bridge для Termux/ReTerminal.",
        "Интерфейс: три языка (uk/en/ru), онбординг первого запуска, документация и диагностика."
      ]
    }
  ]
};

const DOC_ACTIONS = {
  login: () => { closeDocs(); openAuth('ds'); },
  bridge: () => { api({ cmd: 'bridge_save' }); },
  memory: () => { closeDocs(); openMemory(); },
  settings: () => { closeDocs(); openSettings(); },
  help: () => { closeDocs(); openHelp(); },
  newchat: () => { closeDocs(); if (S.busy) { toast(t('stopFirst')); return; } if (!leaveQueueOk()) return; flushDraft(); api({ cmd: 'new' }); }
};

function renderDocs() {
  const box = $('docsBody');
  if (!box) return;
  const list = DOCS[LANG] || DOCS.uk;
  box.innerHTML = list.map((d, i) => {
    const li = (d.li || []).map(x => '<li>' + esc(x) + '</li>').join('');
    const a = d.a ? '<button class="btn tonal" data-doc="' + d.a.act + '">' +
      esc(d.a.label) + '</button>' : '';
    return '<div class="docsec"><h3>' + (i + 1) + '. ' + esc(d.t) + '</h3><p>' + esc(d.p) +
      '</p>' + (li ? '<ul>' + li + '</ul>' : '') + a + '</div>';
  }).join('');
  box.querySelectorAll('[data-doc]').forEach(b => {
    b.onclick = () => { const f = DOC_ACTIONS[b.dataset.doc]; if (f) f(); };
  });
}

function openIntro() {
  const el = $('intro');
  if (!el) return;
  el.classList.add('on');
  el.scrollTop = 0;
  const body = el.querySelector('.introbody');
  if (body) body.scrollTop = 0;
}

function closeIntro(markSeen) {
  $('intro').classList.remove('on');
  if (markSeen) api({ cmd: 'intro_seen' });
}

function openDocs() { renderDocs(); $('docs').classList.add('on'); }
function closeDocs() { $('docs').classList.remove('on'); }

/* ---------------- update log ---------------- */

function renderUpdates() {
  const box = $('updatesBody');
  if (!box) return;
  const cur = (S.version || '').trim();
  const list = UPDATES[LANG] || UPDATES.uk;
  box.innerHTML = list.map(u => {
    const isCur = !!u.name && u.name === cur;
    const li = (u.li || []).map(x => '<li>' + esc(x) + '</li>').join('');
    return '<article class="upd' + (isCur ? ' cur' : '') + '">' +
      '<div class="updhead"><span class="updver">' + esc(u.code) + '</span>' +
      (u.name ? '<span class="updname">' + esc(u.name) + '</span>' : '') +
      (isCur ? '<span class="updnow">' + esc(t('updatesNow')) + '</span>' : '') +
      '</div>' +
      '<h3>' + esc(u.title) + '</h3>' +
      '<p>' + esc(u.p) + '</p>' +
      (li ? '<ul>' + li + '</ul>' : '') +
      '</article>';
  }).join('');
}

function openUpdates() { renderUpdates(); $('updates').classList.add('on'); checkUpdates(true); }
function closeUpdates() { $('updates').classList.remove('on'); }

const QUICK_ACTS = [
  { k: 'qaLogin', act: 'login' },
  { k: 'qaChat', act: 'newchat' },
  { k: 'qaMemory', act: 'memory' },
  { k: 'qaApi', act: 'settings' },
  { k: 'qaDocs', act: 'docs' }
];

function renderQuickActs() {
  const box = $('quickActs');
  if (!box) return;
  box.innerHTML = '';
  for (const q of QUICK_ACTS) {
    const b = el('button', 'btn tonal', esc(t(q.k)));
    b.onclick = () => {
      if (q.act === 'docs') { closeHelp(); openDocs(); return; }
      closeHelp();
      (DOC_ACTIONS[q.act] || (() => {}))();
    };
    box.appendChild(b);
  }
}

function openHelp() {
  renderQuickActs();
  $('help').classList.add('on');
  $('diagList').innerHTML = '<div class="muted small">…</div>';
  renderTelemetry();
  api({ cmd: 'diag' });
  api({ cmd: 'telemetry_state' });
}

function closeHelp() { $('help').classList.remove('on'); }

function diagIcon(state) {
  if (state === 'ok') return '✓';
  if (state === 'warn') return '!';
  if (state === 'bad') return '×';
  return 'i';
}

function renderDiag(items) {
  const box = $('diagList');
  if (!box) return;
  if (!items || !items.length) {
    box.innerHTML = '<div class="muted small">' + esc(t('helpEmpty')) + '</div>';
    return;
  }
  box.innerHTML = '';
  for (const it of items) {
    const d = el('div', 'dgitem ' + (it.state || 'info'));
    d.innerHTML = '<span class="dgdot">' + diagIcon(it.state) + '</span>' +
      '<div class="dgbody"><b>' + esc(it.title || '') + '</b>' +
      (it.hint ? '<small>' + esc(it.hint) + '</small>' : '') + '</div>';
    if (it.action && it.act) {
      const b = el('button', 'dgact', esc(it.act));
      b.type = 'button';
      b.onclick = () => diagAction(it.action);
      d.appendChild(b);
    }
    box.appendChild(d);
  }
}

function diagAction(id) {
  if (id === 'settings') { closeHelp(); openSettings(); return; }
  if (id === 'report') { previewReport(); return; }
  if (id === 'account') { closeHelp(); openSettings(); openSetCat('account'); return; }
  if (id === 'verify') { closeHelp(); api({ cmd: 'auth_verify' }); return; }
  api({ cmd: 'diag_action', id });
}

function previewReport() {
  $('repText').textContent = t('reportLoading');
  $('repSheet').classList.add('on');
  api({ cmd: 'report_preview' });
}

function closeReport() { $('repSheet').classList.remove('on'); }

/* ---------------- collapse all ---------------- */

function applyFold() {
  const dirty = [];
  for (const m of S.msgs) {
    let ch = false;
    if (S.foldAll) {
      if (m.thinkOpen !== undefined && m.thinkOpen) { m.thinkOpen = false; ch = true; }
      if (m.role === 'tool' && m.open) { m.open = false; ch = true; }
      if (m.role === 'sub') ch = true;
    } else {
      if (m.thinkOpen === false && m.think) { m.thinkOpen = true; ch = true; }
      if (m.role === 'sub') ch = true;
    }
    if (ch && m._el) dirty.push(m);
  }
  for (const m of dirty) {
    const n = msgNode(m);
    m._el.replaceWith(n);
    m._el = n;
  }
  maybeScroll();
}

/* ---------------- wiring ---------------- */


/* Горизонтальний свайп не має зсувати інтерфейс: блокуємо пан по X,
   лишаючи прокрутку всередині код-блоків і широких таблиць. */
(function () {
  function hScrollable(node) {
    while (node && node.nodeType === 1 && node !== document.body) {
      try {
        const st = getComputedStyle(node);
        if (node.scrollWidth > node.clientWidth + 2 &&
            st.overflowX !== 'visible' && st.overflowX !== 'hidden') return true;
      } catch (e) {}
      node = node.parentElement;
    }
    return false;
  }
  let sx = 0, sy = 0, free = false;
  document.addEventListener('touchstart', (e) => {
    free = false;
    if (e.touches.length !== 1) { free = true; return; }
    sx = e.touches[0].clientX;
    sy = e.touches[0].clientY;
    free = hScrollable(e.target);
  }, { passive: true });
  document.addEventListener('touchmove', (e) => {
    if (free || e.touches.length !== 1) return;
    const dx = Math.abs(e.touches[0].clientX - sx);
    const dy = Math.abs(e.touches[0].clientY - sy);
    if (dx > 4 && dx > dy) e.preventDefault();
  }, { passive: false });
})();

document.addEventListener('DOMContentLoaded', () => {
  try {
    api({ cmd: 'log', level: 'info',
      msg: 'viewport css=' + window.innerWidth + 'x' + window.innerHeight +
        ' dpr=' + window.devicePixelRatio + ' scrollW=' + document.documentElement.scrollWidth });
  } catch (e) {}
  $('menuBtn').onclick = openDrawer;
  $('drawerClose').onclick = closeDrawer;
  $('scrim').onclick = closeDrawer;
  $('newBtn').onclick = () => { if (S.busy) { toast(t('stopFirst')); return; } if (!leaveQueueOk()) return; closeDrawer(); flushDraft(); api({ cmd: 'new' }); };
  $('drawerNew').onclick = () => { if (S.busy) { toast(t('stopFirst')); return; } if (!leaveQueueOk()) return; closeDrawer(); flushDraft(); api({ cmd: 'new' }); };
  $('setBtn').onclick = openSettings;
  $('setClose').onclick = settingsBackOrClose;
  $('memBtn').onclick = openMemory;
  $('chipMem').onclick = openMemory;
  $('drawerMem').onclick = () => { closeDrawer(); openMemory(); };
  $('drawerLang').onclick = () => { closeDrawer(); openSettings(); openSetCat('look'); };
  $('drawerHelp').onclick = () => { closeDrawer(); openHelp(); };

  $('memBack').onclick = closeMemory;
  $('memAdd').onclick = () => openCard(null);
  $('memEmptyAdd').onclick = () => openCard(null);
  $('docsBack').onclick = () => closeDocs();
  $('btnDocs').onclick = () => { closeSettings(); openDocs(); };
  $('btnUpdates').onclick = () => { closeSettings(); openUpdates(); };
  $('introStart').onclick = () => closeIntro(true);
  $('introSkip').onclick = () => closeIntro(true);
  $('introShow').onclick = () => openIntro();
  $('authDocs').onclick = () => openDocs();
  $('authSite').onclick = () => {
    $('loginMsg').textContent = '';
    api({ cmd: 'auth_site' });
  };
  $('authKey').onclick = openKeySheet;
  $('keyClose').onclick = closeKeySheet;
  $('keySave').onclick = saveKey;
  $('keySheet').onclick = (ev) => { if (ev.target === $('keySheet')) closeKeySheet(); };
  $('cardClose').onclick = closeCard;
  $('cardSave').onclick = saveCard;
  $('cardDelete').onclick = () => {
    if (!S.memEdit) return;
    if (confirm(t('delCard'))) {
      api({ cmd: 'mem_del', id: S.memEdit.id });
      closeCard();
    }
  };
  $('cardSheet').onclick = (ev) => { if (ev.target === $('cardSheet')) closeCard(); };

  $('send').onclick = () => submit();
  $('stopBtn').onclick = () => {
    if (!S.busy) return;
    // Явна зупинка не має нескінченно тягнути чергу: ставимо її на паузу,
    // доки користувач сам не натисне «Запустити чергу».
    S.stopByUser = true;
    vibe(14);
    api({ cmd: 'stop' });
  };

  const qbar = $('queueBar');
  qbar.addEventListener('click', (ev) => {
    const row = ev.target.closest('.qrow');
    if (ev.target.closest('[data-qplay]')) {
      S.queueHold = false;
      renderQueue();
      pumpQueue();
      return;
    }
    if (!row) return;
    const i = +row.getAttribute('data-qi');
    if (!(i >= 0) || i >= S.queue.length) return;
    if (ev.target.closest('[data-qup]')) {
      if (i > 0) {
        const a = S.queue[i - 1];
        S.queue[i - 1] = S.queue[i];
        S.queue[i] = a;
        renderQueue();
      }
      return;
    }
    if (ev.target.closest('[data-qdown]')) {
      if (i < S.queue.length - 1) {
        const a = S.queue[i + 1];
        S.queue[i + 1] = S.queue[i];
        S.queue[i] = a;
        renderQueue();
      }
      return;
    }
    if (ev.target.closest('[data-qedit]')) { openQueueEdit(i); return; }
    if (ev.target.closest('[data-qdel]')) {
      S.queue.splice(i, 1);
      if (!S.queue.length) S.queueHold = false;
      renderQueue();
      return;
    }
  });

  $('queueSave').onclick = () => {
    const it = S.queueEdit;
    const v = $('queueText').value.trim();
    if (it && (v || (it.files && it.files.length))) it.text = v;
    $('queueSheet').classList.remove('on');
    S.queueEdit = null;
    renderQueue();
  };
  $('queueCancel').onclick = () => {
    $('queueSheet').classList.remove('on');
    S.queueEdit = null;
  };
  $('queueSheet').onclick = (ev) => {
    if (ev.target === $('queueSheet')) {
      $('queueSheet').classList.remove('on');
      S.queueEdit = null;
    }
  };

  const inp = $('input');
  inp.addEventListener('input', () => {
    inp.style.height = 'auto';
    inp.style.height = Math.min(150, inp.scrollHeight) + 'px';
    syncSend();
    clearTimeout(inp._dh);
    inp._dh = setTimeout(saveDraft, 300);
  });
  inp.addEventListener('keydown', (ev) => {
    if (ev.key === 'Enter' && !ev.shiftKey) { ev.preventDefault(); submit(); }
  });

  $('chipThink').onclick = () => {
    S.thinking = !S.thinking;
    syncChips();
    api({ cmd: 'save', base: S.baseAuto ? '' : S.base, thinking: S.thinking, search: S.search, model: S.model, lang: LANG });
  };
  $('chipSearch').onclick = () => {
    S.search = !S.search;
    syncChips();
    api({ cmd: 'save', base: S.baseAuto ? '' : S.base, thinking: S.thinking, search: S.search, model: S.model, lang: LANG });
  };

  $('msgs').addEventListener('click', (ev) => {
    const hp = ev.target.closest('[data-help]');
    if (hp) { openHelp(); return; }
    const th = ev.target.closest('[data-think]');
    if (th) {
      const w = th.closest('.think');
      w.classList.toggle('open');
      const node = th.closest('.msg');
      if (node && node._m) node._m.thinkOpen = w.classList.contains('open');
      return;
    }
    const im = ev.target.closest('.imglink');
    if (im) {
      const path = im.getAttribute('data-fpath');
      if (path) openViewer(path, im.querySelector('em') ? im.querySelector('em').textContent : '');
      else {
        const pic = im.querySelector('img');
        if (pic && pic.getAttribute('src')) openRemoteImage(pic.getAttribute('src'), pic.getAttribute('alt'));
      }
      return;
    }
    const fl = ev.target.closest('.flink');
    if (fl) { openViewer(fl.getAttribute('data-fpath'), fl.textContent); return; }
    const ac = ev.target.closest('.acard');
    if (ac) { openFileSheet(ac.getAttribute('data-fpath'), ac.getAttribute('data-fname')); return; }
    const af = ev.target.closest('[data-art-full]');
    if (af) { openArtFull(af.closest('.artifact')); return; }
    const ad = ev.target.closest('[data-art-dl]');
    if (ad) { downloadArt(ad.closest('.artifact')); return; }
    const rg = ev.target.closest('[data-mregen]');
    if (rg) { const n = rg.closest('.msg'); if (n && n._m) regenMsg(n._m); return; }
    const ct = ev.target.closest('[data-mcont]');
    if (ct) { const n = ct.closest('.msg'); if (n && n._m) contMsg(n._m); return; }
    const vp = ev.target.closest('[data-vprev]');
    if (vp) { ev.stopPropagation(); const n = vp.closest('.msg'); if (n && n._m) switchVersion(n._m, -1); return; }
    const vn = ev.target.closest('[data-vnext]');
    if (vn) { ev.stopPropagation(); const n = vn.closest('.msg'); if (n && n._m) switchVersion(n._m, 1); return; }
    const mm = ev.target.closest('[data-mmenu]');
    if (mm) { const n = mm.closest('.msg'); if (n && n._m) openMsgSheet(n._m); return; }
    const um = ev.target.closest('.msg.user');
    if (um && um._m) { openMsgSheet(um._m); return; }
    const lb = ev.target.closest('[data-longmsg]');
    if (lb) {
      const n = lb.closest('.msg');
      if (n && n._m) { n._m.expanded = !n._m.expanded; paintAssistant(n._m); }
      return;
    }
    const fb = ev.target.closest('[data-fold]');
    if (fb) {
      const pre = fb.closest('.code').querySelector('pre');
      if (pre) {
        const closed = pre.hasAttribute('hidden');
        if (closed) pre.removeAttribute('hidden');
        else pre.setAttribute('hidden', '');
        fb.textContent = t(closed ? 'hideCode' : 'showCode');
      }
      return;
    }
    const b = ev.target.closest('[data-copy]');
    if (b) {
      const pre = b.closest('.code').querySelector('pre');
      if (pre) { api({ cmd: 'copy', text: pre.textContent }); b.textContent = '✓'; setTimeout(() => b.textContent = t('copy'), 1200); }
      return;
    }
    const card = ev.target.closest('.toolcard');
    if (card) {
      const sub = card.closest('.subcard');
      if (sub) {
        const n = sub.closest('.msg');
        if (n && n._m) { openSub(n._m.id); return; }
      }
      card.classList.toggle('open');
      const tn = card.closest('.msg');
      if (tn && tn._m && tn._m.role === 'tool') tn._m.open = card.classList.contains('open');
      return;
    }
    const cp = ev.target.closest('[data-mcopy]');
    if (cp) {
      const n = cp.closest('.msg').querySelector('.md');
      api({ cmd: 'copy', text: n ? n.innerText : '' });
      toast(t('copied'));
      return;
    }
    const ed = ev.target.closest('[data-medit]');
    if (ed) {
      const n = ed.closest('.msg').querySelector('.md');
      const inp2 = $('input');
      inp2.value = n ? n.innerText : '';
      inp2.dispatchEvent(new Event('input'));
      inp2.focus();
      inp2.scrollIntoView({ block: 'end' });
      return;
    }
    // Контекстне меню відповіді ШІ: тап по тексту, якщо це не посилання,
    // картка інструмента чи інший інтерактивний елемент.
    const am = ev.target.closest('.msg:not(.user)');
    if (am && am._m && !ev.target.closest('a,button,.think,.code,.artifact,.toolcard,.subcard,.imglink,.acard,.flink')) {
      openMsgSheet(am._m);
      return;
    }
  });

  $('btnImport').onclick = () => api({ cmd: 'import' });
  $('btnBaseReset').onclick = () => {
    $('fBase').value = '';
    S.base = '';
    S.baseAuto = true;
    $('baseCount').textContent = '0';
    api({ cmd: 'save', base: '', thinking: S.thinking, search: S.search, model: S.model, lang: LANG });
  };
  $('btnPerm').onclick = () => api({ cmd: 'perm' });

  const apiApply = () => {
    const port = parseInt($('fApiPort').value, 10) || S.apiPort || 8787;
    S.apiAuto = $('fApiOn').checked;
    S.apiLan = $('fApiLan').checked;
    S.apiDel = $('fApiDel').checked;
    S.apiPort = port;
    api({ cmd: 'api_apply', port, auto: S.apiAuto, lan: S.apiLan, del: S.apiDel });
  };
  $('fApiOn').onchange = apiApply;
  $('fApiLan').onchange = apiApply;
  $('fApiDel').onchange = apiApply;
  $('fApiPort').onchange = apiApply;
  $('apiToggle').onclick = () => api({ cmd: S.apiRunning ? 'api_stop' : 'api_start' });
  $('keyAddBtn').onclick = openApiKeySheet;
  $('auditCopy').onclick = () => api({ cmd: 'audit_copy' });
  $('auditRotate').onclick = () => {
    if (confirm(t('auditRotateAsk'))) api({ cmd: 'audit_rotate' });
  };
  const auditApply = () => api({
    cmd: 'audit_apply',
    contact: ($('fAuditContact').value || '').trim(),
    hook: ($('fAuditHook').value || '').trim()
  });
  $('fAuditContact').onchange = auditApply;
  $('fAuditHook').onchange = auditApply;
  $('akCreate').onclick = () => api({
    cmd: 'key_add', name: ($('akName').value || '').trim(), ttl: +$('akTtl').value || 0
  });
  $('akCopy').onclick = () => api({ cmd: 'copy', text: $('akValue').value });
  $('akOk').onclick = closeApiKeySheet;
  $('akClose').onclick = closeApiKeySheet;
  if ($('pubFile')) {
    $('pubFile').onclick = () => api({ cmd: 'pub_file' });
    $('pubCheck').onclick = checkPublic;
    $('pubCopy').onclick = () => {
      const b = pubBase();
      if (!b) { toast(t('pubNone')); return; }
      api({ cmd: 'copy', text: b });
    };
    $('pubClient').onclick = copyClientConfig;
    $('pubForget').onclick = () => api({ cmd: 'pub_apply', url: '' });
    $('fPubOn').onchange = () => {
      S.apiPublic = $('fPubOn').checked;
      api({ cmd: 'api_apply', pub: S.apiPublic });
    };
    $('updCheck').onclick = () => checkUpdates(false);
    $('updGet').onclick = () => { if (UPD) api({ cmd: 'open_url', url: UPD.url }); };
  }
  const guardApply = () => {
    S.guardOn = $('fGuardOn').checked;
    S.guardGap = parseInt($('fGuardGap').value, 10) || 0;
    S.guardPerMin = parseInt($('fGuardMin').value, 10) || 12;
    S.guardPerDay = parseInt($('fGuardDay').value, 10) || 400;
    api({ cmd: 'guard_apply', on: S.guardOn, gap: S.guardGap, perMin: S.guardPerMin, perDay: S.guardPerDay });
  };
  $('fGuardOn').onchange = guardApply;
  $('fGuardGap').onchange = guardApply;
  $('fGuardMin').onchange = guardApply;
  $('fGuardDay').onchange = guardApply;
  $('guardReset').onclick = () => api({ cmd: 'guard_reset' });
  $('statReset').onclick = () => {
    if (confirm(t('statResetAsk'))) { statsReset(); vibe(8); }
  };
  $('fFoldAll').onchange = () => {
    S.foldAll = $('fFoldAll').checked;
    applyFold();
    api({ cmd: 'fold_apply', on: S.foldAll });
  };
  $('chipCtx').onclick = openCtx;
  $('ctxClose').onclick = closeCtx;
  $('ctxCompact').onclick = () => { closeCtx(); startCompact(false); };
  $('ctxSheet').onclick = (ev) => { if (ev.target === $('ctxSheet')) closeCtx(); };
  $('pickCancel').onclick = closePick;
  $('pickSheet').onclick = (ev) => { if (ev.target === $('pickSheet')) closePick(); };
  enhanceSelects();
  $('fBase').addEventListener('input', () => { $('baseCount').textContent = $('fBase').value.length; });
  $('btnSave').onclick = () => {
    S.base = $('fBase').value;
    S.baseAuto = S.base.trim() === '';
    S.thinking = $('fThink').checked;
    S.search = $('fSearch').checked;
    S.model = $('fModel').value;
    S.ctxLimit = +($('fCtx') ? $('fCtx').value : S.ctxLimit) || 1000000;
    api({ cmd: 'save', base: S.baseAuto ? '' : S.base, thinking: S.thinking, search: S.search,
      model: S.model, lang: LANG, ctxLimit: S.ctxLimit });
    closeSettings();
    updateCtxChip(true);
    if (S.signedIn) { api({ cmd: 'sessions' }); api({ cmd: 'mem_list' }); }
  };

  document.querySelectorAll('#langSeg button').forEach(b => {
    b.onclick = () => {
      const lang = b.dataset.lang;
      if (lang === LANG) return;
      LANG = lang;
      S.lang = lang;
      S.baseAuto = true;
      S.base = '';
      applyI18n();
      api({ cmd: 'save', base: '', thinking: S.thinking, search: S.search, model: S.model, lang });
    };
  });

  $('sSearch').addEventListener('input', (e) => { S.q = e.target.value; renderSessions(); });
  $('mSearch').addEventListener('input', (e) => { S.mq = e.target.value; renderMem(); });
  $('memExport').onclick = () => api({ cmd: 'mem_export' });
  $('memImport').onclick = () => api({ cmd: 'mem_import' });
  $('sheetPin').onclick = () => {
    $('sheet').classList.remove('on');
    if (!S.sheetId) return;
    vibe(6);
    api({ cmd: 'pin', id: S.sheetId, on: !S.sheetPin });
  };

  const sc = $('scroll');
  let lastTop = sc.scrollTop, touchY = 0, touching = false;
  sc.addEventListener('scroll', () => {
    const top = sc.scrollTop;
    const d = sc.scrollHeight - top - sc.clientHeight;
    // Рух угору — користувач читає історію. Вимикаємо автопрокрутку
    // одразу, а не після 240 px, інакше чат виривається з-під пальця.
    if (top < lastTop - 2 && d > 24) S.follow = false;
    else if (d < 24) S.follow = true;
    lastTop = top;
    $('toBottom').classList.toggle('on', !S.follow || d > 420);
  }, { passive: true });
  sc.addEventListener('touchstart', (e) => {
    touching = true;
    touchY = e.touches.length ? e.touches[0].clientY : 0;
  }, { passive: true });
  sc.addEventListener('touchmove', (e) => {
    // Палець тягне вниз — контент іде вгору (до старих повідомлень).
    if (touching && e.touches.length && e.touches[0].clientY - touchY > 8) S.follow = false;
  }, { passive: true });
  sc.addEventListener('touchend', () => { touching = false; }, { passive: true });
  $('toBottom').onclick = () => {
    S.follow = true;
    try {
      if (typeof sc.scrollTo === 'function') sc.scrollTo({ top: sc.scrollHeight, behavior: 'smooth' });
      else sc.scrollTop = sc.scrollHeight;
    } catch (e) { sc.scrollTop = sc.scrollHeight; }
    $('toBottom').classList.remove('on');
  };

  $('sheetCancel').onclick = () => $('sheet').classList.remove('on');
  $('sheet').onclick = (ev) => { if (ev.target === $('sheet')) $('sheet').classList.remove('on'); };
  $('sheetRename').onclick = () => {
    $('sheet').classList.remove('on');
    const s = S.sessions.find(x => x.id === S.sheetId);
    const v = prompt(t('renamePrompt'), s ? (s.title || '') : '');
    if (v != null && v.trim()) api({ cmd: 'rename', id: S.sheetId, title: v.trim() });
  };
  $('sheetDelete').onclick = () => {
    $('sheet').classList.remove('on');
    if (!S.sheetId) return;
    if (confirm(t('delSession'))) api({ cmd: 'delete', id: S.sheetId });
  };

  document.addEventListener('click', (ev) => {
    const a = ev.target.closest('a[href^="http"]');
    if (a) { ev.preventDefault(); api({ cmd: 'open_url', url: a.href }); }
  });

  $('accAdd').onclick = () => { closeSettings(); openAuth('ds'); };
  $('profCheck').onclick = () => {
    if (!S.signedIn) { openAuth('ds'); return; }
    toast(t('checking'));
    api({ cmd: 'acc_check' });
  };
  $('fAuthLock').onchange = () => {
    const on = $('fAuthLock').checked;
    if (on && !(S.auth && S.auth.hasPass)) { openLockSheet(''); return; }
    api({ cmd: 'auth_lock', on });
  };
  $('lockClose').onclick = () => { closeLockSheet(); $('fAuthLock').checked = !!(S.auth && S.auth.lock); };
  $('lockCancel').onclick = $('lockClose').onclick;
  $('lockSheet').onclick = (ev) => { if (ev.target === $('lockSheet')) $('lockClose').onclick(); };
  $('lockSave').onclick = () => {
    const pass = $('lockPass').value;
    if (pass.length < 6) { $('lockMsg').textContent = t('lockShort'); return; }
    $('lockSave').disabled = true;
    $('lockMsg').textContent = t('authChecking');
    api({ cmd: 'auth_lock', on: true, password: pass });
  };
  $('lockPass').addEventListener('keydown', (e) => { if (e.key === 'Enter') $('lockSave').click(); });
  $('lockForget').onclick = () => {
    closeLockSheet();
    closeSettings();
    api({ cmd: 'auth_forgot' });
    toast(t('lockForgetHint'));
  };
  $('editCancel').onclick = () => { vibe(6); cancelEdit(); };
  $('msgCancel').onclick = () => { $('msgSheet').classList.remove('on'); S.msgTarget = null; };
  $('msgSheet').onclick = (ev) => { if (ev.target === $('msgSheet')) { $('msgSheet').classList.remove('on'); S.msgTarget = null; } };
  $('msgCopy').onclick = () => {
    const m = S.msgTarget;
    $('msgSheet').classList.remove('on');
    if (!m) return;
    api({ cmd: 'copy', text: m.text || '' });
    toast(t('copied'));
  };
  $('msgEdit').onclick = () => {
    const m = S.msgTarget;
    $('msgSheet').classList.remove('on');
    S.msgTarget = null;
    if (m) startEdit(m);
  };
  $('msgRegen').onclick = () => {
    const m = S.msgTarget;
    $('msgSheet').classList.remove('on');
    S.msgTarget = null;
    if (m) regenMsg(m);
  };
  $('msgCont').onclick = () => {
    const m = S.msgTarget;
    $('msgSheet').classList.remove('on');
    S.msgTarget = null;
    if (m) contMsg(m);
  };

  $('sheetFolder').onclick = () => { $('sheet').classList.remove('on'); renderFolderPick(); $('folderSheet').classList.add('on'); };
  $('folderCancel').onclick = () => $('folderSheet').classList.remove('on');
  $('folderSheet').onclick = (ev) => { if (ev.target === $('folderSheet')) $('folderSheet').classList.remove('on'); };
  $('folderNew').onclick = () => {
    $('folderSheet').classList.remove('on');
    openNameSheet({ title: t('createFolder'), value: '', id: null });
  };
  $('nameClose').onclick = closeNameSheet;
  $('nameCancel').onclick = closeNameSheet;
  $('nameSave').onclick = saveNameSheet;
  $('nameSheet').onclick = (ev) => { if (ev.target === $('nameSheet')) closeNameSheet(); };
  $('nameDelete').onclick = () => {
    if (!S.nameId) return;
    if (!confirm(t('folderDelete'))) return;
    api({ cmd: 'folder_del', id: S.nameId });
    S.folder = 'all';
    closeNameSheet();
  };
  $('nameInput').addEventListener('keydown', (ev) => {
    if (ev.key === 'Enter') { ev.preventDefault(); saveNameSheet(); }
  });

  $('artFullClose').onclick = () => { $('artFull').classList.remove('on'); $('artFullFrame').srcdoc = ''; };

  // посилання з артефакту: лише http(s), решту ігноруємо
  window.addEventListener('message', (ev) => {
    const d = ev && ev.data;
    if (!d || typeof d !== 'object' || d.funa !== 'open') return;
    // postMessage может прислать любая песочница (включая саму страницу
    // артефакта при загрузке): открываем браузер только по клику внутри
    // нашего iframe, а не по сообщению неизвестного источника.
    const frames = [];
    const af = $('artFullFrame');
    if (af && af.contentWindow) frames.push(af.contentWindow);
    document.querySelectorAll('.artifact iframe').forEach((f) => {
      if (f.contentWindow) frames.push(f.contentWindow);
    });
    if (!frames.includes(ev.source)) return;
    const u = typeof d.url === 'string' ? d.url : '';
    if (!/^https?:\/\//i.test(u) || u.length > 8000) return;
    api({ cmd: 'open_url', url: u });
  });
  $('artFullDownload').onclick = () => {
    const f = $('artFullFrame');
    api({ cmd: 'save_html', name: S.artName || 'page', content: stripLinkHook(f.srcdoc || '') });
  };

  $('attach').onclick = () => { if (S.busy) return; api({ cmd: 'pick_file' }); };

  $('sheetCompact').onclick = () => { $('sheet').classList.remove('on'); startCompact(false); };

  const fileAction = (cmd) => {
    const f = S.fileTarget;
    if (!f) return;
    vibe(8);
    api({ cmd, path: f.path });
  };
  $('fileClose').onclick = closeFileSheet;
  $('fileCancel').onclick = closeFileSheet;
  $('subBack').onclick = closeSub;
  $('subStop').onclick = () => { if (S.subView && S.subView.status === 'run') api({ cmd: 'sub_stop', id: S.subView.id }); };
  $('fileSheet').onclick = (ev) => { if (ev.target === $('fileSheet')) closeFileSheet(); };
  $('fileView').onclick = () => {
    const f = S.fileTarget;
    if (!f) return;
    closeFileSheet();
    openViewer(f.path, f.name);
  };
  $('fileOpen').onclick = () => { closeFileSheet(); fileAction('open_with'); };
  $('fileShare').onclick = () => fileAction('share_file');
  $('fileSave').onclick = () => fileAction('save_file');

  $('viewerBack').onclick = closeViewer;
  $('viewerOpen').onclick = () => { if (S.view && S.view.path) { vibe(8); api({ cmd: 'open_with', path: S.view.path }); } };
  $('viewerShare').onclick = () => { if (S.view && S.view.path) { vibe(8); api({ cmd: 'share_file', path: S.view.path }); } };
  $('viewerSave').onclick = () => { if (S.view && S.view.path) { vibe(8); api({ cmd: 'save_file', path: S.view.path }); } };
  $('attachBar').addEventListener('click', (ev) => {
    const b = ev.target.closest('[data-adel]');
    if (!b) return;
    const i = +b.getAttribute('data-adel');
    if (!isNaN(i)) { S.att.splice(i, 1); renderAtt(); }
  });

  $('findBtn').onclick = () => {
    if ($('findBar').classList.contains('on')) closeFind();
    else openFind();
  };
  $('findClose').onclick = closeFind;
  $('findInput').addEventListener('input', (e) => runFind(e.target.value));
  $('findInput').addEventListener('keydown', (ev) => {
    if (ev.key === 'Enter') { ev.preventDefault(); stepFind(ev.shiftKey ? -1 : 1); }
    if (ev.key === 'Escape') closeFind();
  });
  $('findPrev').onclick = () => stepFind(-1);
  $('findNext').onclick = () => stepFind(1);

  $('sheetExport').onclick = () => {
    $('sheet').classList.remove('on');
    if (!S.msgs.length) return;
    api({ cmd: 'export_chat', name: S.title || 'chat', md: chatMarkdown() });
  };

  $('btnPages').onclick = () => openPages();
  $('btnHelp').onclick = () => { closeSettings(); openHelp(); };
  $('helpBack').onclick = closeHelp;
  $('helpRefresh').onclick = () => { api({ cmd: 'diag' }); api({ cmd: 'telemetry_state' }); };
  $('fTelemetry').onchange = () => {
    S.telemetryOn = $('fTelemetry').checked;
    api({ cmd: 'telemetry_apply', on: S.telemetryOn });
    renderTelemetry();
  };
  $('telePreview').onclick = () => api({ cmd: 'telemetry_preview', stats: statsGet() });
  $('teleSend').onclick = () => api({ cmd: 'telemetry_send', stats: statsGet() });
  $('teleCopy').onclick = () => api({ cmd: 'copy', text: $('teleText').textContent });
  $('teleClose').onclick = () => $('teleSheet').classList.remove('on');
  $('teleSheet').onclick = (ev) => { if (ev.target === $('teleSheet')) $('teleSheet').classList.remove('on'); };
  $('updBack').onclick = closeUpdates;
  $('helpUpdates').onclick = () => { closeHelp(); openUpdates(); };
  $('logEmail').onclick = () => api({ cmd: 'report_email' });
  $('logShare').onclick = () => api({ cmd: 'report_share' });
  $('logPreview').onclick = previewReport;
  $('logClear').onclick = () => { if (confirm(t('logClearAsk'))) api({ cmd: 'log_clear' }); };
  $('repEmail').onclick = () => { closeReport(); api({ cmd: 'report_email' }); };
  $('repShare').onclick = () => { closeReport(); api({ cmd: 'report_share' }); };
  $('repClose').onclick = closeReport;
  $('repSheet').onclick = (ev) => { if (ev.target === $('repSheet')) closeReport(); };
  $('authBack').onclick = () => {
    if (S.fromUnlock) { S.fromUnlock = false; openAuth('unlock', true); return; }
    if (S.authForce) { api({ cmd: 'exit' }); return; }
    if (S.signedIn) closeAuth(); else api({ cmd: 'auth_skip' });
  };
  $('authSkip').onclick = () => api({ cmd: 'auth_skip' });
  $('unlockBtn').onclick = submitUnlock;
  $('authSwitch').onclick = () => { S.fromUnlock = true; openAuth('ds', false); };
  $('unlockForgot').onclick = () => { api({ cmd: 'auth_forgot' }); toast(t('lockForgetHint')); };
  $('uPass').addEventListener('keydown', (e) => { if (e.key === 'Enter') submitUnlock(); });
  $('pagesBack').onclick = () => $('pages').classList.remove('on');
  $('pagesRefresh').onclick = () => api({ cmd: 'pages_list' });

  updateWelcome();
  api({ cmd: 'boot' });
});
})();
