#!/usr/bin/env bash
# 手工构建 APK（不使用 Gradle）
#
# 用法：
#   export TVGUARD_KEYSTORE=/path/to/your.jks
#   export TVGUARD_KS_PASS=yourpassword
#   bash build/build.sh
#
# 可选环境变量：
#   TVGUARD_TOOLCHAIN  工具链目录（默认 build/toolchain）
#   TVGUARD_KS_ALIAS   签名别名（默认 tvguard）
set -e

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TC="${TVGUARD_TOOLCHAIN:-$ROOT/build/toolchain}"
APP="$ROOT/app"
OUT="$ROOT/build/out"

JDK8="$TC/jdk8"
JDK17="$TC/jdk17"
BT="$TC/build-tools"
ANDROID_JAR="$TC/platform/android.jar"

KEYSTORE="${TVGUARD_KEYSTORE:-}"
KS_PASS="${TVGUARD_KS_PASS:-}"
KS_ALIAS="${TVGUARD_KS_ALIAS:-tvguard}"

# 跨平台可执行文件后缀
if [ -f "$BT/aapt.exe" ]; then
    EXE=".exe"
else
    EXE=""
fi

AAPT="$BT/aapt$EXE"
ZIPALIGN="$BT/zipalign$EXE"
JAVAC="$JDK8/bin/javac$EXE"
JARSIGNER="$JDK8/bin/jarsigner$EXE"
JAVA="$JDK17/bin/java$EXE"
if [ -f "$BT/d8.bat" ]; then
    D8="$BT/d8.bat"
else
    D8="$BT/d8"
fi

# 前置检查
missing=0
for f in "$JAVAC" "$JAVA" "$AAPT" "$ZIPALIGN" "$ANDROID_JAR"; do
    if [ ! -f "$f" ]; then
        echo "缺少: $f"
        missing=1
    fi
done
if [ "$missing" -ne 0 ]; then
    echo
    echo "工具链不完整。请按 README「编译」一节准备 build/toolchain："
    echo "  jdk8/  jdk17/  build-tools/  platform/"
    exit 1
fi
if [ -z "$KEYSTORE" ] || [ ! -f "$KEYSTORE" ]; then
    echo "未指定签名文件。请设置 TVGUARD_KEYSTORE 指向 .jks"
    exit 1
fi
if [ -z "$KS_PASS" ]; then
    echo "未指定签名密码。请设置 TVGUARD_KS_PASS"
    exit 1
fi

# MSYS/Git Bash 下把 /c/foo 转成 C:/foo，供 Windows 原生程序使用
simple_winpath() { sed 's|^/\([a-zA-Z]\)/|\1:/|'; }

rm -rf "$OUT"
mkdir -p "$OUT/gen" "$OUT/obj" "$OUT/dex"

echo "[1/6] aapt: 生成 R.java + 资源"
"$AAPT" package -f -m \
    -J "$OUT/gen" \
    -M "$APP/AndroidManifest.xml" \
    -S "$APP/res" \
    -I "$ANDROID_JAR" \
    -F "$OUT/app-unsigned.apk"

echo "[2/6] javac: 编译 Java 源码"
find "$APP/src" "$OUT/gen" -name "*.java" | simple_winpath > "$OUT/sources.txt"
echo "      源文件数: $(wc -l < "$OUT/sources.txt")"
"$JAVAC" -source 1.7 -target 1.7 -encoding UTF-8 \
    -bootclasspath "$ANDROID_JAR" \
    -d "$OUT/obj" "@$OUT/sources.txt" 2>&1 | grep -v "bootstrap class path" || true

echo "[3/6] d8: class -> dex"
if command -v cygpath >/dev/null 2>&1; then
    export JAVA_HOME="$(cygpath -w "$JDK17")"
else
    export JAVA_HOME="$JDK17"
fi
export PATH="$JDK17/bin:$PATH"
cd "$OUT"
"$D8" --lib "$ANDROID_JAR" --min-api 19 --output "$OUT/dex" \
    $(find "$OUT/obj" -name "*.class" | simple_winpath)

echo "[4/6] aapt: 把 classes.dex 打进 APK"
cd "$OUT/dex"
"$AAPT" add "$OUT/app-unsigned.apk" classes.dex

echo "[5/6] zipalign"
cd "$ROOT"
"$ZIPALIGN" -f 4 "$OUT/app-unsigned.apk" "$OUT/app-aligned.apk"

echo "[6/6] jarsigner: v1 签名（Android 4.4 只认 v1）"
"$JARSIGNER" \
    -keystore "$KEYSTORE" -storepass "$KS_PASS" -keypass "$KS_PASS" \
    -sigalg SHA256withRSA -digestalg SHA-256 \
    -signedjar "$ROOT/build/QuizGate.apk" "$OUT/app-aligned.apk" "$KS_ALIAS" >/dev/null 2>&1

echo
echo "=== 产物 ==="
ls -lh "$ROOT/build/QuizGate.apk"
echo
echo "=== 校验签名 ==="
"$JARSIGNER" -verify -verbose -certs "$ROOT/build/QuizGate.apk" 2>&1 | head -4
