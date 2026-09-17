#!/usr/bin/env bash
set -euo pipefail

# Creates a local Play upload keystore. Never commit the .jks or keystore.properties.

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ -f upload-keystore.jks ]]; then
  echo "upload-keystore.jks already exists. Refusing to overwrite." >&2
  exit 1
fi

if [[ -f keystore.properties ]]; then
  if grep -q 'ci-keystore' keystore.properties || grep -q 'keyAlias=ci' keystore.properties; then
    echo "keystore.properties currently points at the CI throwaway key (2-day validity)." >&2
    echo "Play Console rejects that certificate. Remove the leftover files, then re-run:" >&2
    echo "  rm -f keystore.properties ci-keystore/ci.jks" >&2
    echo "  ./scripts/create-upload-keystore.sh" >&2
    exit 1
  fi
  echo "keystore.properties already exists. Refusing to overwrite." >&2
  exit 1
fi

read -r -s -p "Keystore / key password: " PASSWORD
echo
if [[ -z "$PASSWORD" ]]; then
  echo "Password required." >&2
  exit 1
fi

keytool -genkeypair -v \
  -keystore upload-keystore.jks \
  -alias upload \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass "$PASSWORD" \
  -keypass "$PASSWORD" \
  -dname "CN=PinAlarmLock upload, OU=PinAlarmLock, O=PinAlarmLock, L=Unknown, ST=Unknown, C=US"

cat > keystore.properties <<EOF
storeFile=upload-keystore.jks
storePassword=${PASSWORD}
keyAlias=upload
keyPassword=${PASSWORD}
EOF

echo
echo "Created upload-keystore.jks and keystore.properties (gitignored)."
echo "Back this keystore up; losing it blocks updates if Play App Signing is not yet enrolled."
echo
echo "GitHub Actions secrets:"
echo "  ANDROID_KEYSTORE_BASE64=$(base64 < upload-keystore.jks | tr -d '\n' | head -c 32)..."
echo "  ANDROID_KEYSTORE_PASSWORD=<the password you entered>"
echo "  ANDROID_KEY_ALIAS=upload"
echo "  ANDROID_KEY_PASSWORD=<the password you entered>"
