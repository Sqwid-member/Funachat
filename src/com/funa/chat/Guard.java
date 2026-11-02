package com.funa.chat;

import java.io.IOException;
import java.util.Locale;

/**
 * Захист акаунта: обмежувач темпу запитів до DeepSeek.
 *
 * Навіщо: акаунт, який бачить десятки сесій і запитів за хвилину з одного
 * токена, виглядає як автоматизація — це головна причина блокувань.
 * Guard тримає людський темп, денний ліміт і паузу після ознак ризику.
 *
 * Стан зберігається у Store (SharedPreferences), тому ліміти переживають
 * перезапуск застосунку.
 */
public final class Guard {
    private Guard() {}

    private static final long DAY = 86400000L;
    private static final long MIN = 60000L;

    /** Час, зарезервований під наступний старт запиту (щоб паралельні не злипались). */
    private static long reservedAt;

    /** Паузи після різних ознак. */
    private static final long COOL_AUTH = 30 * MIN;
    private static final long COOL_RATE = 5 * MIN;
    private static final long COOL_RISK = 60 * MIN;
    private static final long COOL_MAX = 30 * MIN;

    /**
     * Ознаки перевірки акаунта в тексті помилки. Навмисно без «част»/«ліміт»:
     * вони трапляються у звичайних повідомленнях і давали хибну паузу на годину.
     * Код 429 обробляється окремо, ще до цього списку.
     */
    private static final String[] RISK_WORDS = {
            "risk", "banned", "ban ", "blocked", "abnormal", "frequen", "too many",
            "ризик", "підозр", "подозр", "заблок"
    };

    public interface State {
        boolean guardOn();
        int guardGapMs();
        int guardPerMin();
        int guardPerDay();
        int guardCount();
        long guardDay();
        long guardLast();
        long guardCooldown();
        String guardNote();
        long guardMinStart();
        int guardMinCount();

        void guardSave(int count, long day, long last, long cooldown, String note,
                       long minStart, int minCount);
    }

    /** kind: cool | min | day. */
    public static class Limited extends IOException {
        public final String kind;
        public final long until;
        public final int limit;
        public final long seconds;
        public final String noteKey;

        public Limited(String kind, long until, int limit, long seconds, String noteKey) {
            super(kind);
            this.kind = kind;
            this.until = until;
            this.limit = limit;
            this.seconds = seconds;
            this.noteKey = noteKey == null ? "" : noteKey;
        }
    }

    /** Викликати перед кожним запитом completion. */
    public static void gate(State s) throws Limited, InterruptedException {
        long wait;
        synchronized (Guard.class) {
            wait = reserve(s);
        }
        // Паузу тримаємо ПОЗА замком: інакше один субагент, який чекає
        // мінімальний інтервал, блокував би чат та інших субагентів.
        if (wait > 0) Thread.sleep(wait);
    }

    /**
     * Резервує час старту запиту й повертає, скільки ще чекати.
     * Викликається під замком; лічильники оновлюються одразу, щоб
     * паралельні запити не «проскочили» ліміт.
     */
    private static long reserve(State s) throws Limited {
        long now = System.currentTimeMillis();
        long day = s.guardDay();
        int count = s.guardCount();
        long last = s.guardLast();
        long cool = s.guardCooldown();
        long minStart = s.guardMinStart();
        int minCount = s.guardMinCount();

        if (day != now / DAY) {
            day = now / DAY;
            count = 0;
            minStart = 0;
            minCount = 0;
        }

        int perDay = Math.max(1, s.guardPerDay());
        int perMin = Math.max(1, s.guardPerMin());
        int gap = Math.max(0, s.guardGapMs());

        if (cool > now) {
            long left = (cool - now + 999) / 1000;
            throw new Limited("cool", cool, 0, left, s.guardNote());
        }

        if (s.guardOn() && count >= perDay) {
            long nextDay = (now / DAY + 1) * DAY;
            throw new Limited("day", nextDay, perDay, 0, "");
        }

        long start = now;
        if (s.guardOn()) {
            if (minStart == 0 || now - minStart >= MIN) {
                minStart = now;
                minCount = 0;
            }
            if (minCount >= perMin) {
                throw new Limited("min", minStart + MIN, perMin, 0, "");
            }
            long base = now;
            if (gap > 0) base = Math.max(now, Math.max(last, reservedAt) + gap);
            // легкий джитер, щоб запити не йшли рівним автоматним тактом
            int jitter = Math.abs((int) (System.nanoTime() % 400L));
            start = base + jitter;
            reservedAt = start;
        }

        count++;
        minCount++;
        s.guardSave(count, day, start, 0, "", minStart, minCount);
        return Math.max(0, start - now);
    }

    /** Успішна відповідь: знімаємо паузу. */
    public static synchronized void ok(State s) {
        if (s.guardCooldown() == 0 && (s.guardNote() == null || s.guardNote().isEmpty())) return;
        s.guardSave(s.guardCount(), s.guardDay(), s.guardLast(), 0, "",
                s.guardMinStart(), s.guardMinCount());
    }

    /** Помилка запиту: визначаємо, чи це ознака ризику, і ставимо паузу. */
    public static synchronized void fail(State s, int code, String msg) {
        long now = System.currentTimeMillis();
        String text = msg == null ? "" : msg.toLowerCase(Locale.ROOT);
        long prev = Math.max(0, s.guardCooldown() - now);
        long cool;
        String note;

        if (code == 401) {
            cool = COOL_AUTH;
            note = "auth";
        } else if (code == 403) {
            cool = COOL_AUTH;
            note = "forbidden";
        } else if (code == 429 || has(text, "too many", "frequen", "rate limit")) {
            cool = Math.min(COOL_MAX, Math.max(COOL_RATE, prev * 2));
            note = "rate";
        } else if (has(text, RISK_WORDS)) {
            cool = COOL_RISK;
            note = "risk";
        } else {
            // решта (5xx, мережа) — не ознака ризику, паузу не ставимо
            return;
        }

        s.guardSave(s.guardCount(), s.guardDay(), s.guardLast(), now + cool, note,
                s.guardMinStart(), s.guardMinCount());
        Log.add("guard", "pause " + (cool / 60000) + " min (" + note + ")"
                + (msg == null || msg.isEmpty() ? "" : ": " + msg));
    }

    /** Ручне зняття паузи (після дій користувача). */
    public static synchronized void reset(State s) {
        s.guardSave(s.guardCount(), s.guardDay(), s.guardLast(), 0, "", 0, 0);
    }

    /* --------------------------- локалізація --------------------------- */

    public static String message(Limited e, String lang) {
        boolean en = "en".equals(lang);
        boolean ru = "ru".equals(lang);
        if ("cool".equals(e.kind)) {
            String base = en ? "Paused for " + e.seconds + " s"
                    : ru ? "Пауза ещё " + e.seconds + " с"
                    : "Пауза ще " + e.seconds + " с";
            String note = note(e.noteKey, lang);
            return note.isEmpty() ? base : base + " (" + note + ")";
        }
        if ("min".equals(e.kind)) {
            return en ? "Rate limit: " + e.limit + " requests per minute"
                    : ru ? "Слишком часто: лимит " + e.limit + " в минуту"
                    : "Забагато запитів: ліміт " + e.limit + " на хвилину";
        }
        return en ? "Daily limit of " + e.limit + " requests reached"
                : ru ? "Дневной лимит " + e.limit + " запросов исчерпан"
                : "Денний ліміт " + e.limit + " запитів вичерпано";
    }

    /** Локалізований опис причини паузи (для UI й діагностики). */
    public static String noteText(String key, String lang) {
        return note(key, lang);
    }

    private static String note(String key, String lang) {
        boolean en = "en".equals(lang);
        boolean ru = "ru".equals(lang);
        if ("auth".equals(key)) {
            return en ? "token invalid or session closed"
                    : ru ? "токен недействителен или сессия закрыта"
                    : "токен недійсний або сесію завершено";
        }
        if ("forbidden".equals(key)) {
            return en ? "access denied — account check possible"
                    : ru ? "доступ запрещён — возможна проверка аккаунта"
                    : "доступ заборонено — можлива перевірка акаунта";
        }
        if ("rate".equals(key)) {
            return en ? "server is throttling requests"
                    : ru ? "сервер ограничивает частоту"
                    : "сервер обмежує частоту";
        }
        if ("risk".equals(key)) {
            return en ? "looks like an account check"
                    : ru ? "похоже на проверку аккаунта"
                    : "схоже на перевірку акаунта";
        }
        return "";
    }

    private static boolean has(String text, String... words) {
        for (String w : words) if (text.contains(w)) return true;
        return false;
    }
}
