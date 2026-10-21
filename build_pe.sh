#!/system/bin/sh
set -e
cd "$(dirname "$0")"
echo "=== Yongzhe Local Server + GM / PE Build ==="
java -version
if command -v gradle >/dev/null 2>&1; then
  gradle --no-daemon --stacktrace :app:assembleDebug
else
  echo "ERROR: PE 中没有找到 gradle，请先执行: rkb setup ide"
  exit 1
fi
echo "APK: app/build/outputs/apk/debug/app-debug.apk"
