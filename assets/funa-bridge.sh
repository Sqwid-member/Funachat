#!/system/bin/sh
# Funa bridge v11 — portable: Termux, ReTerminal, any Android sh.
#
# Протокол: один файл funa_X.cmd.txt — демон пише результат у нього ж.
# Старий протокол (funa_X.cmd + funa_X.out) теж підтримується.
#
# Команди виконуються у фоні: довга збірка в одному каналі не блокує інші.
# Канал зайнятий, поки у файлі немає [[DONE]] — апка в нього нове не пише.
#
# Запуск:
#   Termux:      bash ~/storage/downloads/FunaFiles/funa-bridge.sh
#   ReTerminal:  sh /sdcard/Download/FunaFiles/funa-bridge.sh
# Зупинити: Ctrl+C або закрити сесію.

# ── тека черги ─────────────────────────────────────────────────────────────
# Можна перевизначити: FUNA_QUEUE=/sdcard/Download/FunaQueue sh funa-bridge.sh
# Шлях мусить збігатися з тим, куди пише FunaChat, інакше міст «мовчить».
Q=${FUNA_QUEUE:-/sdcard/Download/FunaQueue}
export FUNA_QUEUE=$Q

# ── оточення ───────────────────────────────────────────────────────────────
# PATH збираємо з того, що реально існує: Termux, ReTerminal/Alpine, система.
P=
for d in "$PREFIX/bin" /data/data/com.termux/files/usr/bin /usr/local/bin /usr/bin /bin \
         /system/bin /system/xbin /vendor/bin; do
  [ -d "$d" ] || continue
  case ":$P:" in *":$d:"*) ;; *) P=${P:+$P:}$d ;; esac
done
[ -n "$P" ] && PATH=$P:$PATH
export PATH

# Виконавець команд: bash, якщо є (Termux), інакше системний sh (ReTerminal).
if command -v bash >/dev/null 2>&1; then
  SH=$(command -v bash)
elif [ -x /system/bin/sh ]; then
  SH=/system/bin/sh
else
  SH=sh
fi

LIMIT=${FUNA_LIMIT:-85}
LONG=${FUNA_LONG:-600}
case "$LIMIT" in ''|*[!0-9]*) LIMIT=85 ;; esac
case "$LONG" in ''|*[!0-9]*) LONG=600 ;; esac
if ! mkdir -p "$Q" 2>/dev/null || [ ! -w "$Q" ]; then
  echo "funa-bridge: не можу писати в $Q" >&2
  echo "Дай терміналу доступ до файлів: Termux — termux-setup-storage; ReTerminal — дозвіл «Файли» в налаштуваннях Android." >&2
  echo "Або вкажи іншу теку: FUNA_QUEUE=/шлях sh funa-bridge.sh" >&2
  exit 1
fi

# ── один демон ─────────────────────────────────────────────────────────────
# Тільки атомарний mkdir + PID. flock навмисно не використовуємо: на
# /sdcard (FUSE) він є в PATH, але падає з «Function not implemented» —
# і стара версія через `flock -n 9 || exit 0` тихо завершувалась, так і
# не запустившись. mkdir працює і в Termux, і в ReTerminal/Alpine.
# Замок тримаємо в черзі: $Q уже перевірена на запис, а TMPDIR у
# ReTerminal/Alpine може вказувати в нікуди.
LOCKBASE=$Q
LOCKD="$LOCKBASE/funa-bridge.lock.d"
if ! mkdir "$LOCKD" 2>/dev/null; then
  old=$(cat "$LOCKD/pid" 2>/dev/null)
  if [ -z "$old" ]; then
    # Замок щойно створив інший екземпляр, але ще не встиг записати pid.
    # Без паузи ми б видалили живий замок і запустили другий демон.
    sleep 1
    old=$(cat "$LOCKD/pid" 2>/dev/null)
  fi
  if [ -n "$old" ] && kill -0 "$old" 2>/dev/null; then
    echo "funa-bridge: уже працює (pid $old)" >&2
    exit 0
  fi
  rm -rf "$LOCKD" 2>/dev/null
  mkdir "$LOCKD" 2>/dev/null || exit 0
fi
echo $$ > "$LOCKD/pid" 2>/dev/null
# Перевірка володіння: якщо два старти перетнулися, pid у замку буде
# чужий. Тоді виходимо ДО встановлення trap, щоб не знести чужий замок.
if [ "$(cat "$LOCKD/pid" 2>/dev/null)" != "$$" ]; then
  echo "funa-bridge: інший екземпляр уже працює" >&2
  exit 0
fi
# По сигналу застосунок/користувач чекає завершення: trap на TERM без
# exit лише прибирав би замок, а демон жив далі — і другий екземпляр
# почав би виконувати ті самі команди паралельно.
trap 'rm -rf "$LOCKD" 2>/dev/null; exit 0' INT TERM HUP
trap 'rm -rf "$LOCKD" 2>/dev/null' EXIT

# прибираємо залишки старих версій (q*.cmd.txt) — новий демон їх не виконує
rm -f "$Q"/q*.cmd.txt "$Q"/*.out "$Q"/*.out.txt "$Q"/*.cmd.tmp "$Q"/*.cmd.txt.tmp 2>/dev/null

echo "funa-bridge v11: $Q (shell: $SH)" >&2

# ── обмеження розміру файлу каналу ─────────────────────────────────────────
# Файл не має рости без межі (find /, yes, dd). Лишаємо початок і хвіст:
# у початку [[ID]]/[[RUN]], у хвості [[DONE]] і текст помилки.
cap_file() {
  f=$1
  [ -f "$f" ] || return 0
  sz=$(wc -c < "$f" 2>/dev/null) || return 0
  [ -n "$sz" ] || return 0
  [ "$sz" -gt 524288 ] || return 0
  h=$(head -c 200000 "$f" 2>/dev/null)
  t=$(tail -c 300000 "$f" 2>/dev/null)
  printf '%s\n… (вивід обрізано у файлі: %s байт) …\n%s\n' "$h" "$sz" "$t" > "$f" 2>/dev/null
}

# ── запуск із лімітом часу ─────────────────────────────────────────────────
run_limited() {
  body=$1
  lim=$2
  if command -v timeout >/dev/null 2>&1; then
    timeout "$lim" "$SH" -c "$body" < /dev/null 2>&1
    return $?
  fi

  # немає timeout (мінімальний Android/Alpine) — сторожовий таймер
  mark="$LOCKBASE/funa-timeout.$$.$(date +%s 2>/dev/null || echo 0)"
  "$SH" -c "$body" < /dev/null 2>&1 &
  pid=$!
  ( sleep "$lim"; : > "$mark" 2>/dev/null; kill -9 "$pid" 2>/dev/null ) &
  wd=$!
  wait "$pid"
  code=$?
  kill "$wd" 2>/dev/null
  wait "$wd" 2>/dev/null
  if [ -f "$mark" ]; then
    code=124
  fi
  rm -f "$mark" 2>/dev/null
  return $code
}

exec_out() {
  dest=$1
  body=$2
  lim=$3
  # Вивід іде в окремий tmp, а не одразу в канал. Інакше фонові діти
  # команди (сервер, nohup, sleep &) тримають fd каналу й дописують у
  # нього вже ПІСЛЯ [[DONE]] — маркер перестає бути останнім рядком, і
  # апка вважає команду незавершеною до 30-хвилинного таймауту.
  tmp="$dest.tmp"
  : > "$tmp" 2>/dev/null
  run_limited "$body" "$lim" >> "$tmp" 2>&1
  code=$?
  if [ "$code" = "124" ]; then
    printf '\n[перервано: ліміт %s с]\n' "$lim" >> "$tmp"
  fi
  # Спершу обрізаємо величезний вивід і лише потім публікуємо його в
  # канал разом із [[DONE]]: апка не має побачити маркер раніше за текст.
  cap_file "$tmp"
  cat "$tmp" >> "$dest" 2>/dev/null
  rm -f "$tmp" 2>/dev/null
  printf '\n[[DONE:%s]]\n' "$code" >> "$dest"
}

# ── основний цикл ──────────────────────────────────────────────────────────
# дробовий sleep є не скрізь — перевіряємо один раз
SLEEP=0.2
sleep 0.2 2>/dev/null || SLEEP=1

NL='
'
while :; do
  for f in "$Q"/funa_?.cmd "$Q"/funa_?.cmd.txt; do
    [ -s "$f" ] || continue
    raw=$(cat "$f" 2>/dev/null) || continue
    [ -n "$raw" ] || continue

    # Взяту команду впізнаємо за першим рядком: міст пише туди [[ID:…]],
    # тому файл без #funa: у першому рядку вже виконано або виконується.
    # Раніше тут шукали "[[DONE" по всьому файлу — і команда, у тексті якої
    # траплявся цей рядок, мовчки пропускалась.
    first=${raw%%"$NL"*}
    case "$first" in
      "#funa:"*) tok=${first#\#funa:} ;;
      *) continue ;;
    esac
    # токен — лише hex, інакше це не команда
    case "$tok" in
      ""|*[!0-9a-f]*) continue ;;
    esac
    [ ${#tok} -ge 4 ] || continue

    body=${raw#*"$NL"}
    if [ "$body" = "$raw" ]; then body=""; fi

    name=${f##*/}
    case "$name" in
      *.cmd.txt) base=${f%.cmd.txt} ;;
      *.cmd)     base=${f%.cmd} ;;
      *)         base="$f" ;;
    esac

    # старий протокол: апка вже створила .out і чекає результат там
    out=""
    [ -f "$base.out" ] && out="$base.out"
    [ -z "$out" ] && [ -f "$base.out.txt" ] && out="$base.out.txt"

    # Директиви в перших рядках (для sh це просто коментарі, тому старі
    # версії моста їх не ламають):
    #   #long        — довга команда, ліміт LONG (600 с)
    #   #limit:N     — точний ліміт у секундах (5..900)
    lim=$LIMIT
    while :; do
      head=${body%%"$NL"*}
      case "$head" in
        "#limit:"*)
          n=${head#\#limit:}
          case "$n" in ''|*[!0-9]*) n=$LIMIT ;; esac
          lim=$n
          case "$body" in *"$NL"*) body=${body#*"$NL"} ;; *) body="" ;; esac
          ;;
        "#long"*|"# long"*|"#довго"*)
          [ "$lim" -ge "$LONG" ] || lim=$LONG
          case "$body" in *"$NL"*) body=${body#*"$NL"} ;; *) body="" ;; esac
          ;;
        *) break ;;
      esac
    done
    [ "$lim" -ge 5 ] 2>/dev/null || lim=5
    [ "$lim" -le 900 ] 2>/dev/null || lim=900

    if [ -n "$out" ]; then
      : > "$f" 2>/dev/null
      # позначаємо «взято» одразу, щоб апка не пішла в локальний shell
      printf '[[ID:%s]]\n[[RUN]]\n' "$tok" > "$out" 2>/dev/null
      # 9>&- : дитина не тримає зайвих дескрипторів демона (fd 9 міг
      # лишитися від старих версій з flock)
      exec_out "$out" "$body" "$lim" 2>/dev/null 9>&- &
    else
      # усе в тому самому файлі; спершу «забираємо» команду, інакше під час
      # truncate апка бачить порожній файл і запускає команду локально
      printf '[[ID:%s]]\n[[RUN]]\n' "$tok" > "$f" 2>/dev/null
      exec_out "$f" "$body" "$lim" 2>/dev/null 9>&- &
    fi
  done
  sleep "$SLEEP"
done
