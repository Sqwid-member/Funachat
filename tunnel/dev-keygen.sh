#!/data/data/com.termux/files/usr/bin/bash
# Створює пару ECDSA P-256 для глобального аудиту ключів розробником.
# Приватний ключ лишається в ~/.config/funachat, публічний друкується
# для константи DEV_AUDIT_PUB в ApiServer.java.
set -eu
DIR="${HOME}/.config/funachat"
mkdir -p "$DIR"
chmod 700 "$DIR"
KEY="$DIR/dev_audit_key.pem"
if [ -f "$KEY" ] && [ "${1:-}" != "--force" ]; then
  echo "уже є: $KEY (--force — перестворити)" >&2
else
  umask 077
  openssl ecparam -name prime256v1 -genkey -noout -out "$KEY"
  chmod 600 "$KEY"
  echo "private: $KEY" >&2
fi
openssl ec -in "$KEY" -pubout -out "$DIR/dev_audit_pub.pem" 2>/dev/null
PUB=$(openssl pkey -pubin -in "$DIR/dev_audit_pub.pem" -outform DER | base64 -w0)
echo "$PUB" > "$DIR/dev_audit_pub.b64"
echo "public:  $DIR/dev_audit_pub.pem" >&2
echo
echo "DEV_AUDIT_PUB = \"$PUB\""
