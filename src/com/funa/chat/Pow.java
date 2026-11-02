package com.funa.chat;

import java.nio.charset.StandardCharsets;

/**
 * DeepSeek PoW: SHA3-256 sponge with Keccak-p[1600, 23] (last 23 round constants).
 * answer = smallest i in [0, difficulty) such that hash(salt + "_" + expire_at + "_" + i) == challenge.
 */
public final class Pow {
    private static final long[] RC = {
        0x0000000000000001L, 0x0000000000008082L, 0x800000000000808AL, 0x8000000080008000L,
        0x000000000000808BL, 0x0000000080000001L, 0x8000000080008081L, 0x8000000000008009L,
        0x000000000000008AL, 0x0000000000000088L, 0x0000000080008009L, 0x000000008000000AL,
        0x000000008000808BL, 0x800000000000008BL, 0x8000000000008089L, 0x8000000000008003L,
        0x8000000000008002L, 0x8000000000000080L, 0x000000000000800AL, 0x800000008000000AL,
        0x8000000080008081L, 0x8000000000008080L, 0x0000000080000001L, 0x8000000080008008L
    };
    private static final int[] ROT = {
        0, 36, 3, 41, 18,
        1, 44, 10, 45, 2,
        62, 6, 43, 15, 61,
        28, 55, 25, 21, 56,
        27, 20, 39, 8, 14
    };

    private Pow() {}

    private static long rol(long x, int n) {
        return n == 0 ? x : (x << n) | (x >>> (64 - n));
    }

    /** Keccak-p[1600, 23]: rounds 1..23 of the standard sequence (drop round 0). */
    private static void permute(long[] a) {
        permute(a, new long[5], new long[25]);
    }

    private static void permute(long[] a, long[] c, long[] b) {
        for (int round = 1; round < 24; round++) {
            for (int x = 0; x < 5; x++) {
                c[x] = a[x] ^ a[x + 5] ^ a[x + 10] ^ a[x + 15] ^ a[x + 20];
            }
            for (int x = 0; x < 5; x++) {
                long d = c[(x + 4) % 5] ^ rol(c[(x + 1) % 5], 1);
                for (int y = 0; y < 25; y += 5) a[x + y] ^= d;
            }
            for (int x = 0; x < 5; x++) {
                for (int y = 0; y < 5; y++) {
                    b[y + 5 * ((2 * x + 3 * y) % 5)] = rol(a[x + 5 * y], ROT[x * 5 + y]);
                }
            }
            for (int y = 0; y < 25; y += 5) {
                for (int x = 0; x < 5; x++) {
                    a[x + y] = b[x + y] ^ ((~b[(x + 1) % 5 + y]) & b[(x + 2) % 5 + y]);
                }
            }
            a[0] ^= RC[round];
        }
    }

    /** Hash of a short message (single 136-byte block) into 32-byte target. */
    public static byte[] hash(byte[] msg) {
        if (msg == null || msg.length > 135) throw new IllegalArgumentException("повідомлення довше за блок");
        byte[] block = new byte[136];
        System.arraycopy(msg, 0, block, 0, msg.length);
        block[msg.length] = 0x06;
        block[135] |= (byte) 0x80;
        long[] a = new long[25];
        for (int i = 0; i < 17; i++) a[i] = le(block, i * 8);
        permute(a);
        byte[] out = new byte[32];
        for (int i = 0; i < 4; i++) {
            long v = a[i];
            for (int j = 0; j < 8; j++) out[i * 8 + j] = (byte) (v >>> (8 * j));
        }
        return out;
    }

    private static long le(byte[] b, int off) {
        long v = 0;
        for (int i = 7; i >= 0; i--) v = (v << 8) | (b[off + i] & 0xffL);
        return v;
    }

    /** Brute force answer for the challenge. Returns -1 if not found. */
    public static long solve(String salt, long expireAt, String challengeHex, long difficulty) {
        byte[] target = unhex(challengeHex);
        if (target == null) return -1;
        byte[] prefix = (salt + "_" + expireAt + "_").getBytes(StandardCharsets.UTF_8);
        // сіль приходить від сервера; довга сіль + 20 цифр не влізе в блок
        if (prefix.length > 110) return -1;
        byte[] block = new byte[136];
        long[] a = new long[25];
        long[] c = new long[5];
        long[] b = new long[25];
        byte[] digits = new byte[20];
        byte[] rev = new byte[20];
        for (long i = 0; i < difficulty; i++) {
            // «Стоп» перериває потік агента, але solve() інакше доводив би
            // пошук до кінця (до 3 млн хешів) — кнопка не реагувала б.
            if ((i & 8191L) == 0 && Thread.currentThread().isInterrupted()) return -1;
            int n = digits(i, digits, rev);
            int len = prefix.length + n;
            System.arraycopy(prefix, 0, block, 0, prefix.length);
            System.arraycopy(digits, 0, block, prefix.length, n);
            block[len] = 0x06;
            block[135] |= (byte) 0x80;
            for (int k = 0; k < 25; k++) a[k] = 0;
            for (int k = 0; k < 17; k++) a[k] = le(block, k * 8);
            permute(a, c, b);
            int ok = 0;
            for (int k = 0; k < 32; k++) {
                if ((byte) (a[k >> 3] >>> (8 * (k & 7))) != target[k]) break;
                ok++;
            }
            if (ok == 32) return i;
            block[len] = 0;
        }
        return -1;
    }

    private static int digits(long v, byte[] out, byte[] rev) {
        if (v == 0) { out[0] = '0'; return 1; }
        int n = 0;
        while (v > 0 && n < out.length) { rev[n++] = (byte) ('0' + (int) (v % 10)); v /= 10; }
        for (int i = 0; i < n; i++) out[i] = rev[n - 1 - i];
        return n;
    }

    public static byte[] unhex(String s) {
        if (s == null || (s.length() & 1) != 0) return null;
        byte[] r = new byte[s.length() / 2];
        for (int i = 0; i < r.length; i++) {
            int hi = Character.digit(s.charAt(2 * i), 16);
            int lo = Character.digit(s.charAt(2 * i + 1), 16);
            if (hi < 0 || lo < 0) return null;
            r[i] = (byte) ((hi << 4) | lo);
        }
        return r;
    }

    public static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) sb.append(Character.forDigit((x >> 4) & 15, 16)).append(Character.forDigit(x & 15, 16));
        return sb.toString();
    }
}
