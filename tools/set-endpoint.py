#!/usr/bin/env python3
"""Переписує XOR-масив адреси телеметрії у Telemetry.java.

Використання:
    tools/set-endpoint.py src/com/funa/chat/Telemetry.java https://example.org/v1/telemetry

Адреса не потрапляє в DEX відкритим рядком: байти зберігаються
закодованими XOR-ом 0x5A, як і було раніше. Це не криптографія, лише
прибирає адресу з `strings`.
"""
import re
import sys

KEY = 0x5A
MARK = "private static final int[] ENDPOINT = {"


def encode(url: str) -> str:
    raw = url.encode("utf-8")
    nums = [b ^ KEY for b in raw]
    lines = []
    for i in range(0, len(nums), 12):
        lines.append("            " + ", ".join(str(n) for n in nums[i:i + 12]))
    return MARK + "\n" + ",\n".join(lines) + "\n    };"


def main() -> int:
    if len(sys.argv) != 3:
        print(__doc__.strip(), file=sys.stderr)
        return 2
    path, url = sys.argv[1], sys.argv[2].strip()
    if not re.match(r"^https?://[^\s]+$", url):
        print("endpoint must be an http(s) URL", file=sys.stderr)
        return 2
    src = open(path, encoding="utf-8").read()
    start = src.find(MARK)
    if start < 0:
        print("ENDPOINT array not found", file=sys.stderr)
        return 1
    end = src.find("};", start)
    if end < 0:
        print("end of ENDPOINT array not found", file=sys.stderr)
        return 1
    src = src[:start] + encode(url) + src[end + 2:]
    open(path, "w", encoding="utf-8").write(src)
    print("endpoint updated:", url)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
