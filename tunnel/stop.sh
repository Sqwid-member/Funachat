#!/data/data/com.termux/files/usr/bin/bash
# Зупиняє публічний доступ (edge + обидва тунелі).
set -u
cd "$(dirname "$0")"
pkill -f "[c]loudflared tunnel --url http://127.0.0.1:8790" 2>/dev/null
pkill -f "[z]rok2 share public" 2>/dev/null
pkill -f "[n]ode proxy.js" 2>/dev/null
rm -f url.txt url-zrok.txt
echo "public access stopped"
