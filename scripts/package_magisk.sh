#!/usr/bin/env bash
set -euo pipefail

PKG="com.mora.gamespace"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${OUT_DIR:-$ROOT_DIR/dist}"
MODULE_DIR="$OUT_DIR/mora_perf_deamon"
ZIP_NAME="${ZIP_NAME:-mora_perf_deamon.zip}"
RUST_BIN="${RUST_BIN:-$ROOT_DIR/target/aarch64-linux-android/release/perf_daemon}"
APK_PATH="${APK_PATH:-}"

# Try to locate the GameSpace APK if not provided explicitly.
if [[ -z "$APK_PATH" ]]; then
  for c in \
    "$ROOT_DIR/../GameSpace/app/build/outputs/apk/release/app-release.apk" \
    "$ROOT_DIR/../GameSpace/app/build/outputs/apk/release/app-release-unsigned.apk" \
    "$ROOT_DIR/../GameSpace/app/build/outputs/apk/debug/app-debug.apk" \
    "$ROOT_DIR/app/build/outputs/apk/release/app-release.apk" ; do
    [[ -f "$c" ]] && { APK_PATH="$c"; break; }
  done
fi

if [[ ! -f "$RUST_BIN" ]]; then
  echo "ERROR: Rust daemon binary not found: $RUST_BIN" >&2
  echo "Build it first: cargo build --release --target aarch64-linux-android" >&2
  exit 1
fi
if [[ -z "$APK_PATH" || ! -f "$APK_PATH" ]]; then
  echo "ERROR: GameSpace APK not found. Set APK_PATH or build the app first." >&2
  exit 1
fi

rm -rf "$MODULE_DIR"
mkdir -p "$MODULE_DIR"
cp -a "$ROOT_DIR/magisk/." "$MODULE_DIR/"

# Drop repo placeholders.
find "$MODULE_DIR" -name .gitkeep -delete
find "$MODULE_DIR" -name 'PUT_APK_HERE.txt' -delete

# Daemon binary.
mkdir -p "$MODULE_DIR/system/bin"
cp "$RUST_BIN" "$MODULE_DIR/system/bin/perf_daemon"
chmod 0755 "$MODULE_DIR/system/bin/perf_daemon"

# App APK as a privileged system app.
mkdir -p "$MODULE_DIR/system/priv-app/$PKG"
cp "$APK_PATH" "$MODULE_DIR/system/priv-app/$PKG/$PKG.apk"
chmod 0644 "$MODULE_DIR/system/priv-app/$PKG/$PKG.apk"

# Sane permissions.
chmod 0755 "$MODULE_DIR/service.sh" "$MODULE_DIR/META-INF/com/google/android/update-binary"
find "$MODULE_DIR" -type d -exec chmod 0755 {} +
find "$MODULE_DIR" -type f ! -path '*/system/bin/perf_daemon' ! -path '*/service.sh' ! -path '*/META-INF/com/google/android/update-binary' -exec chmod 0644 {} +

mkdir -p "$OUT_DIR"
( cd "$MODULE_DIR" && zip -qr9 "$OUT_DIR/$ZIP_NAME" . )
echo "$OUT_DIR/$ZIP_NAME"
